package com.freightfox.tollplazafinder;

import com.freightfox.tollplazafinder.repository.TollPlazaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TollPlazaFinderApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    @Autowired
    private TollPlazaRepository tollPlazaRepository;

    @Autowired
    private com.freightfox.tollplazafinder.controller.RoutingController routingController;

    @Autowired
    private com.freightfox.tollplazafinder.controller.TollPlazaController tollPlazaController;

    @Autowired
    private com.freightfox.tollplazafinder.service.RoutingService routingService;

    @Autowired
    private com.freightfox.tollplazafinder.service.RouteTollService routeTollService;

    @Test
    void testExistingDatabaseAndEndpoints() {
        // 3. Verify database functionality
        long count = tollPlazaRepository.count();
        assertTrue(count > 0, "Toll plazas should exist in DB");

        // 12. Ensure existing endpoints work
        var allPlazas = tollPlazaController.getAllTollPlazas();
        assertNotNull(allPlazas);
        assertFalse(allPlazas.isEmpty());

        var testRoute = routingController.testRoute();
        assertNotNull(testRoute);
        assertFalse(testRoute.routes().isEmpty());

        var testGeometry = routingController.testGeometry();
        assertNotNull(testGeometry);
        assertTrue((int) testGeometry.get("totalCoordinates") > 0);
    }

    @Test
    void testInputValidationMalformedPincodes() {
        // 9. Verify that malformed pincodes are rejected before external API calls
        assertThrows(IllegalArgumentException.class, () -> routingController.getTollPlazasAlongRoute(null, "226001"));
        assertThrows(IllegalArgumentException.class, () -> routingController.getTollPlazasAlongRoute("   ", "226001"));
        assertThrows(IllegalArgumentException.class, () -> routingController.getTollPlazasAlongRoute("110001", null));
        assertThrows(IllegalArgumentException.class, () -> routingController.getTollPlazasAlongRoute("123", "226001"));
        assertThrows(IllegalArgumentException.class, () -> routingController.getTollPlazasAlongRoute("011001", "226001"));
        assertThrows(IllegalArgumentException.class, () -> routingController.getTollPlazasAlongRoute("ABCDEF", "226001"));
        assertThrows(IllegalArgumentException.class, () -> routingController.getTollPlazasAlongRoute("1100010", "226001"));
    }

    @Test
    void testGeocodingCoordinatesAndUnresolvedPincode() {
        // 5. Verify coordinates correspond to requested pincodes
        double[] delhiCoords = routingService.geocodePincode("110001");
        assertNotNull(delhiCoords);
        assertEquals(2, delhiCoords.length);
        // Delhi: lon ~77.2, lat ~28.6
        assertEquals(77.2, delhiCoords[0], 0.5, "Delhi longitude mismatch");
        assertEquals(28.6, delhiCoords[1], 0.5, "Delhi latitude mismatch");

        double[] lucknowCoords = routingService.geocodePincode("226001");
        assertNotNull(lucknowCoords);
        assertEquals(2, lucknowCoords.length);
        // Lucknow: lon ~80.9, lat ~26.8
        assertEquals(80.9, lucknowCoords[0], 0.5, "Lucknow longitude mismatch");
        assertEquals(26.8, lucknowCoords[1], 0.5, "Lucknow latitude mismatch");

        // 10. Verify unresolved pincodes produce 404 ResponseStatusException
        var ex = assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
                routingService.geocodePincode("999999")
        );
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testSegmentProjectionGeometry() {
        // 7. Verify segment projection math
        // Segment from (28.0, 77.0) to (28.0, 78.0)
        // Point at (28.01, 77.5) should project to midpoint (~77.5, 28.0) with t ~ 0.5
        double[] closestMid = com.freightfox.tollplazafinder.util.DistanceCalculator.findClosestPointOnSegment(
                28.0, 77.0,
                28.0, 78.0,
                28.01, 77.5
        );
        assertEquals(28.0, closestMid[0], 0.001);
        assertEquals(77.5, closestMid[1], 0.001);
        assertEquals(0.5, closestMid[2], 0.01);

        // Point before start -> clamped to start t=0
        double[] closestBefore = com.freightfox.tollplazafinder.util.DistanceCalculator.findClosestPointOnSegment(
                28.0, 77.0,
                28.0, 78.0,
                28.0, 76.5
        );
        assertEquals(28.0, closestBefore[0], 0.001);
        assertEquals(77.0, closestBefore[1], 0.001);
        assertEquals(0.0, closestBefore[2], 0.001);

        // Point after end -> clamped to end t=1
        double[] closestAfter = com.freightfox.tollplazafinder.util.DistanceCalculator.findClosestPointOnSegment(
                28.0, 77.0,
                28.0, 78.0,
                28.0, 78.5
        );
        assertEquals(28.0, closestAfter[0], 0.001);
        assertEquals(78.0, closestAfter[1], 0.001);
        assertEquals(1.0, closestAfter[2], 0.001);
    }

    @Test
    void testTollPlazasAlongRouteWithValidPincodes() {
        // 4, 6, 7, 8: Accept source & destination, process geometry, match segments, calculate distance & sort
        var results = routingController.getTollPlazasAlongRoute("110001", "226001");
        assertNotNull(results);
        assertFalse(results.isEmpty(), "Should find toll plazas between Delhi and Lucknow");

        double prevDistance = -1.0;
        for (var toll : results) {
            assertNotNull(toll.id());
            assertNotNull(toll.tollName());
            assertNotNull(toll.geoState());
            assertTrue(toll.distanceFromSourceKm() >= 0, "Distance must be non-negative");
            assertTrue(toll.distanceFromSourceKm() >= prevDistance, "Results must be sorted ascending by distance from source");
            prevDistance = toll.distanceFromSourceKm();
        }
    }

    @Test
    void testRouteWithNoMatchingTollsReturnsEmptyList() {
        // 11. Valid route with no matching toll plazas returns empty result (not error)
        // Two points very close to each other in Connaught Place (110001 to 110001)
        var results = routeTollService.findTollPlazasAlongRoute("110001", "110001");
        assertNotNull(results);
        assertTrue(results.isEmpty(), "Route within same central area should have 0 toll plazas and return empty list");
    }

    @Test
    void testHttpEndpointValidationAndErrorResponses() throws Exception {
        // 15 & 16. Missing params -> 400 Bad Request
        mockMvc.perform(get("/api/v1/routes/toll-plazas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());

        // Invalid source format -> 400 Bad Request
        mockMvc.perform(get("/api/v1/routes/toll-plazas")
                        .param("source", "012345")
                        .param("destination", "226001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid source pincode")));

        // Non-existent pincode -> 404 Not Found
        mockMvc.perform(get("/api/v1/routes/toll-plazas")
                        .param("source", "999999")
                        .param("destination", "226001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("999999")));

        // Valid route -> 200 OK
        mockMvc.perform(get("/api/v1/routes/toll-plazas")
                        .param("source", "110001")
                        .param("destination", "226001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].tollName").exists())
                .andExpect(jsonPath("$[0].distanceFromSourceKm").exists());
    }

    @Test
    void testCachingSuccessReplayAndDistinctKeys() {
        var cache = cacheManager.getCache("route-toll-plazas");
        assertNotNull(cache, "Cache 'route-toll-plazas' must be registered");
        cache.clear();

        String key = "110001-226001";
        assertNull(cache.get(key), "Cache must be empty initially for key " + key);

        // 1. First request calculates and populates cache
        var firstResult = routeTollService.findTollPlazasAlongRoute("110001", "226001");
        assertNotNull(firstResult);
        assertNotNull(cache.get(key), "Cache must be populated after first call");

        // 2. Repeated request reuses cached results
        var cachedWrapper = cache.get(key);
        assertNotNull(cachedWrapper);
        @SuppressWarnings("unchecked")
        var cachedList = (java.util.List<com.freightfox.tollplazafinder.dto.RouteTollResponse>) cachedWrapper.get();
        assertEquals(firstResult.size(), cachedList.size());
        assertEquals(firstResult.get(0).tollName(), cachedList.get(0).tollName());

        // 3. Different / reversed pair uses a distinct cache entry
        String reversedKey = "226001-110001";
        assertNull(cache.get(reversedKey), "Reversed route must not reuse forward route cache");
        var reversedResult = routeTollService.findTollPlazasAlongRoute("226001", "110001");
        assertNotNull(reversedResult);
        assertNotNull(cache.get(reversedKey), "Reversed route must have its own cache entry");

        // 4. Empty result route is also cached
        String emptyKey = "110001-110001";
        assertNull(cache.get(emptyKey));
        var emptyResult = routeTollService.findTollPlazasAlongRoute("110001", "110001");
        assertTrue(emptyResult.isEmpty());
        assertNotNull(cache.get(emptyKey), "Empty result route must be cached");

        // 5. Failed requests are NOT cached
        String failedKey = "999999-226001";
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
                routeTollService.findTollPlazasAlongRoute("999999", "226001")
        );
        assertNull(cache.get(failedKey), "Failed request must not be stored in cache");
    }

    @Test
    void testCacheTtlExpirationConfiguration() {
        var cache = cacheManager.getCache("route-toll-plazas");
        assertNotNull(cache);

        // Verify underlying Caffeine cache expiration policy is configured for 30 minutes
        Object nativeCache = cache.getNativeCache();
        assertTrue(nativeCache instanceof com.github.benmanes.caffeine.cache.Cache<?, ?>);

        var caffeineCache = (com.github.benmanes.caffeine.cache.Cache<?, ?>) nativeCache;
        var policy = caffeineCache.policy();
        assertTrue(policy.expireAfterWrite().isPresent(), "expireAfterWrite policy should be present");

        long nanos = policy.expireAfterWrite().get().getExpiresAfter(java.util.concurrent.TimeUnit.MINUTES);
        assertEquals(30, nanos, "Cache TTL must be configured for 30 minutes");
    }
}
