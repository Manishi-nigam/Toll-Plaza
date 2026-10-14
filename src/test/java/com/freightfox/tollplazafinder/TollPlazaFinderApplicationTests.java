package com.freightfox.tollplazafinder;

import com.freightfox.tollplazafinder.dto.TollPlazaFinderResponse;
import com.freightfox.tollplazafinder.dto.TollPlazaRequest;
import com.freightfox.tollplazafinder.repository.TollPlazaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private com.freightfox.tollplazafinder.controller.TollPlazaController tollPlazaController;

    @Autowired
    private com.freightfox.tollplazafinder.service.RoutingService routingService;

    @Autowired
    private com.freightfox.tollplazafinder.service.RouteTollService routeTollService;

    @Test
    void testExistingDatabaseFunctionality() {
        long count = tollPlazaRepository.count();
        assertTrue(count > 0, "Toll plazas should exist in DB");
    }

    @Test
    void testInputValidationMalformedPincodes() {
        assertThrows(IllegalArgumentException.class, () -> tollPlazaController.findTollPlazasAlongRoute(null));
        assertThrows(IllegalArgumentException.class, () -> tollPlazaController.findTollPlazasAlongRoute(new TollPlazaRequest(null, "226001")));
        assertThrows(IllegalArgumentException.class, () -> tollPlazaController.findTollPlazasAlongRoute(new TollPlazaRequest("   ", "226001")));
        assertThrows(IllegalArgumentException.class, () -> tollPlazaController.findTollPlazasAlongRoute(new TollPlazaRequest("110001", null)));
        assertThrows(IllegalArgumentException.class, () -> tollPlazaController.findTollPlazasAlongRoute(new TollPlazaRequest("123", "226001")));
        assertThrows(IllegalArgumentException.class, () -> tollPlazaController.findTollPlazasAlongRoute(new TollPlazaRequest("011001", "226001")));
        assertThrows(IllegalArgumentException.class, () -> tollPlazaController.findTollPlazasAlongRoute(new TollPlazaRequest("ABCDEF", "226001")));
        assertThrows(IllegalArgumentException.class, () -> tollPlazaController.findTollPlazasAlongRoute(new TollPlazaRequest("1100010", "226001")));
    }

    @Test
    void testGeocodingCoordinatesAndUnresolvedPincode() {
        double[] delhiCoords = routingService.geocodePincode("110001");
        assertNotNull(delhiCoords);
        assertEquals(2, delhiCoords.length);
        assertEquals(77.2, delhiCoords[0], 0.5, "Delhi longitude mismatch");
        assertEquals(28.6, delhiCoords[1], 0.5, "Delhi latitude mismatch");

        double[] lucknowCoords = routingService.geocodePincode("226001");
        assertNotNull(lucknowCoords);
        assertEquals(2, lucknowCoords.length);
        assertEquals(80.9, lucknowCoords[0], 0.5, "Lucknow longitude mismatch");
        assertEquals(26.8, lucknowCoords[1], 0.5, "Lucknow latitude mismatch");

        var ex = assertThrows(org.springframework.web.server.ResponseStatusException.class, () ->
                routingService.geocodePincode("999999")
        );
        assertEquals(org.springframework.http.HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void testSegmentProjectionGeometry() {
        double[] closestMid = com.freightfox.tollplazafinder.util.DistanceCalculator.findClosestPointOnSegment(
                28.0, 77.0,
                28.0, 78.0,
                28.01, 77.5
        );
        assertEquals(28.0, closestMid[0], 0.001);
        assertEquals(77.5, closestMid[1], 0.001);
        assertEquals(0.5, closestMid[2], 0.01);

        double[] closestBefore = com.freightfox.tollplazafinder.util.DistanceCalculator.findClosestPointOnSegment(
                28.0, 77.0,
                28.0, 78.0,
                28.0, 76.5
        );
        assertEquals(28.0, closestBefore[0], 0.001);
        assertEquals(77.0, closestBefore[1], 0.001);
        assertEquals(0.0, closestBefore[2], 0.001);

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
        var response = tollPlazaController.findTollPlazasAlongRoute(new TollPlazaRequest("110001", "226001"));
        assertNotNull(response);
        assertNotNull(response.route());
        assertEquals("110001", response.route().sourcePincode());
        assertEquals("226001", response.route().destinationPincode());
        assertTrue(response.route().distanceInKm() > 400.0, "Actual calculated route distance should be > 400 km");

        assertNotNull(response.tollPlazas());
        assertFalse(response.tollPlazas().isEmpty(), "Should find toll plazas between Delhi and Lucknow");

        double prevDistance = -1.0;
        for (var toll : response.tollPlazas()) {
            assertNotNull(toll.name());
            assertTrue(toll.latitude() > 0);
            assertTrue(toll.longitude() > 0);
            assertTrue(toll.distanceFromSource() >= 0, "Distance must be non-negative");
            assertTrue(toll.distanceFromSource() >= prevDistance, "Results must be sorted ascending by distance from source");
            prevDistance = toll.distanceFromSource();
        }
    }

    @Test
    void testRouteWithNoMatchingTollsReturnsEmptyList() {
        var response = routeTollService.findTollPlazasAlongRoute("110001", "110001");
        assertNotNull(response);
        assertNotNull(response.route());
        assertEquals("110001", response.route().sourcePincode());
        assertEquals("110001", response.route().destinationPincode());
        assertNotNull(response.tollPlazas());
        assertTrue(response.tollPlazas().isEmpty(), "Route within same central area should return empty tollPlazas list []");
    }

    @Test
    void testHttpEndpointPostTollPlazasValidationAndSuccess() throws Exception {
        // Missing body -> 400 Bad Request
        mockMvc.perform(post("/api/v1/toll-plazas")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());

        // Invalid source format -> 400 Bad Request
        mockMvc.perform(post("/api/v1/toll-plazas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourcePincode": "012345",
                                  "destinationPincode": "226001"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Invalid sourcePincode")));

        // Non-existent pincode -> 404 Not Found
        mockMvc.perform(post("/api/v1/toll-plazas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourcePincode": "999999",
                                  "destinationPincode": "226001"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("999999")));

        // Assessment required request: Delhi to Bengaluru (110001 -> 560001) -> 200 OK
        mockMvc.perform(post("/api/v1/toll-plazas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sourcePincode": "110001",
                                  "destinationPincode": "560001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.route.sourcePincode").value("110001"))
                .andExpect(jsonPath("$.route.destinationPincode").value("560001"))
                .andExpect(jsonPath("$.route.distanceInKm").isNumber())
                .andExpect(jsonPath("$.tollPlazas").isArray())
                .andExpect(jsonPath("$.tollPlazas[0].name").exists())
                .andExpect(jsonPath("$.tollPlazas[0].latitude").isNumber())
                .andExpect(jsonPath("$.tollPlazas[0].longitude").isNumber())
                .andExpect(jsonPath("$.tollPlazas[0].distanceFromSource").isNumber());
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
        var cachedResponse = (TollPlazaFinderResponse) cachedWrapper.get();
        assertNotNull(cachedResponse);
        assertEquals(firstResult.route().distanceInKm(), cachedResponse.route().distanceInKm());
        assertEquals(firstResult.tollPlazas().size(), cachedResponse.tollPlazas().size());
        assertEquals(firstResult.tollPlazas().get(0).name(), cachedResponse.tollPlazas().get(0).name());

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
        assertTrue(emptyResult.tollPlazas().isEmpty());
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

        Object nativeCache = cache.getNativeCache();
        assertTrue(nativeCache instanceof com.github.benmanes.caffeine.cache.Cache<?, ?>);

        var caffeineCache = (com.github.benmanes.caffeine.cache.Cache<?, ?>) nativeCache;
        var policy = caffeineCache.policy();
        assertTrue(policy.expireAfterWrite().isPresent(), "expireAfterWrite policy should be present");

        long minutes = policy.expireAfterWrite().get().getExpiresAfter(java.util.concurrent.TimeUnit.MINUTES);
        assertEquals(30, minutes, "Cache TTL must be configured for 30 minutes");
    }
}
