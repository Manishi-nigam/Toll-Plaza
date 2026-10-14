package com.freightfox.tollplazafinder.service;

import com.freightfox.tollplazafinder.dto.RouteResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
public class RoutingService {

    @Value("${ors.api.key}")
    private String orsApiKey;

    private final RestClient restClient = RestClient.create();

    public RouteResponse getRoute() {
        return getRoute(77.2090, 28.6139, 80.9462, 26.8467);
    }

    public RouteResponse getRoute(double startLon, double startLat, double endLon, double endLat) {
        try {
            Map<String, Object> body = Map.of(
                    "coordinates", List.of(
                            List.of(startLon, startLat),
                            List.of(endLon, endLat)
                    )
            );

            RouteResponse response = restClient.post()
                    .uri("https://api.heigit.org/openrouteservice/v2/directions/driving-car")
                    .header("Authorization", orsApiKey)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .retrieve()
                    .body(RouteResponse.class);

            if (response == null || response.routes() == null || response.routes().isEmpty()
                    || response.routes().get(0).geometry() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Routing service returned an empty or malformed route.");
            }

            return response;
        } catch (HttpStatusCodeException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "External routing service error: " + ex.getStatusCode());
        } catch (ResourceAccessException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "External routing service is currently unavailable.");
        }
    }

    public double[] geocodePincode(String pincode) {
        boolean upstreamFailed = false;

        try {
            List<Map<String, Object>> response = restClient.get()
                    .uri("https://nominatim.openstreetmap.org/search?postalcode={pincode}&country=India&format=json", pincode)
                    .header("User-Agent", "FreightFoxTollPlazaFinder/1.0")
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<Map<String, Object>>>() {});

            if (response != null && !response.isEmpty()) {
                Map<String, Object> first = response.get(0);
                double lat = Double.parseDouble(String.valueOf(first.get("lat")));
                double lon = Double.parseDouble(String.valueOf(first.get("lon")));
                return new double[]{lon, lat};
            }
        } catch (RestClientException ex) {
            upstreamFailed = true;
        }

        try {
            Map<String, Object> zippoResponse = restClient.get()
                    .uri("https://api.zippopotam.us/in/{pincode}", pincode)
                    .header("User-Agent", "FreightFoxTollPlazaFinder/1.0")
                    .header("Accept", "application/json")
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (zippoResponse != null && zippoResponse.get("places") instanceof List<?> places && !places.isEmpty()) {
                if (places.get(0) instanceof Map<?, ?> place) {
                    double lat = Double.parseDouble(String.valueOf(place.get("latitude")));
                    double lon = Double.parseDouble(String.valueOf(place.get("longitude")));
                    return new double[]{lon, lat};
                }
            }
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode() != HttpStatus.NOT_FOUND) {
                upstreamFailed = true;
            }
        } catch (RestClientException ex) {
            upstreamFailed = true;
        }

        if (upstreamFailed) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "External geocoding service is unavailable.");
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not resolve geographic coordinates for pincode: " + pincode);
    }
}
