package com.freightfox.tollplazafinder.service;

import com.freightfox.tollplazafinder.dto.RouteResponse;
import com.freightfox.tollplazafinder.dto.RouteTollResponse;
import com.freightfox.tollplazafinder.entity.TollPlaza;
import com.freightfox.tollplazafinder.repository.TollPlazaRepository;
import com.freightfox.tollplazafinder.util.DistanceCalculator;
import com.freightfox.tollplazafinder.util.PolyLineDecoder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class RouteTollService {

    private final RoutingService routingService;
    private final TollPlazaRepository tollPlazaRepository;

    public RouteTollService(RoutingService routingService,
                            TollPlazaRepository tollPlazaRepository){
        this.routingService= routingService;
        this.tollPlazaRepository=tollPlazaRepository;
    }

    public List<RouteTollResponse> findTollPlazasAlongRoute() {
        return findTollPlazasAlongRoute("110001", "226001");
    }

    @Cacheable(value = "route-toll-plazas", key = "#sourcePincode + '-' + #destinationPincode")
    public List<RouteTollResponse> findTollPlazasAlongRoute(String sourcePincode, String destinationPincode) {
        double[] startCoords = routingService.geocodePincode(sourcePincode);
        double[] endCoords = routingService.geocodePincode(destinationPincode);

        RouteResponse response = routingService.getRoute(startCoords[0], startCoords[1], endCoords[0], endCoords[1]);

        if (response == null || response.routes() == null || response.routes().isEmpty()
                || response.routes().get(0).geometry() == null) {
            return Collections.emptyList();
        }

        String geometry = response.routes().get(0).geometry();
        List<double[]> routeCoordinates = PolyLineDecoder.decode(geometry);

        if (routeCoordinates.size() < 2) {
            return Collections.emptyList();
        }

        int numSegments = routeCoordinates.size() - 1;
        double[] segmentLengths = new double[numSegments];
        double[] cumulativeDistances = new double[numSegments + 1];
        cumulativeDistances[0] = 0;

        for (int i = 0; i < numSegments; i++) {
            double[] c1 = routeCoordinates.get(i);
            double[] c2 = routeCoordinates.get(i + 1);
            segmentLengths[i] = DistanceCalculator.calculateDistance(c1[0], c1[1], c2[0], c2[1]);
            cumulativeDistances[i + 1] = cumulativeDistances[i] + segmentLengths[i];
        }

        List<TollPlaza> allTollPlazas = tollPlazaRepository.findAll();
        List<RouteTollResponse> results = new ArrayList<>();
        Set<String> seenTollPlazas = new HashSet<>();

        double thresholdMeters = 500.0;

        for (TollPlaza tollPlaza : allTollPlazas) {
            double minDistanceToRoute = Double.MAX_VALUE;
            double tollDistanceFromSourceMeters = 0;

            for (int i = 0; i < numSegments; i++) {
                double[] pA = routeCoordinates.get(i);
                double[] pB = routeCoordinates.get(i + 1);

                double[] closest = DistanceCalculator.findClosestPointOnSegment(
                        pA[0], pA[1],
                        pB[0], pB[1],
                        tollPlaza.getLatitude(), tollPlaza.getLongitude()
                );

                double distanceToSegment = DistanceCalculator.calculateDistance(
                        tollPlaza.getLatitude(),
                        tollPlaza.getLongitude(),
                        closest[0],
                        closest[1]
                );

                if (distanceToSegment < minDistanceToRoute) {
                    minDistanceToRoute = distanceToSegment;
                    double t = closest[2];
                    tollDistanceFromSourceMeters = cumulativeDistances[i] + (t * segmentLengths[i]);
                }
            }

            if (minDistanceToRoute <= thresholdMeters) {
                String uniqueKey = tollPlaza.getTollName().trim().toLowerCase()
                        + "|" + tollPlaza.getLatitude()
                        + "|" + tollPlaza.getLongitude();

                if (seenTollPlazas.add(uniqueKey)) {
                    results.add(new RouteTollResponse(
                            tollPlaza.getId(),
                            tollPlaza.getTollName(),
                            tollPlaza.getLatitude(),
                            tollPlaza.getLongitude(),
                            tollPlaza.getGeoState(),
                            Math.round((tollDistanceFromSourceMeters / 1000.0) * 100.0) / 100.0
                    ));
                }
            }
        }

        results.sort(Comparator.comparingDouble(RouteTollResponse::distanceFromSourceKm));

        return results;
    }
}
