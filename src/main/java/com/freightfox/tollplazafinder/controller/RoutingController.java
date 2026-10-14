package com.freightfox.tollplazafinder.controller;


import com.freightfox.tollplazafinder.dto.RouteResponse;
import com.freightfox.tollplazafinder.dto.RouteTollResponse;
import com.freightfox.tollplazafinder.entity.TollPlaza;
import com.freightfox.tollplazafinder.service.RouteTollService;
import com.freightfox.tollplazafinder.service.RoutingService;
import com.freightfox.tollplazafinder.util.PolyLineDecoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/routes")
public class RoutingController {
    private static final Pattern PINCODE_PATTERN = Pattern.compile("^[1-9][0-9]{5}$");

    private final RoutingService routingService;
    private final RouteTollService routeTollService;

    public RoutingController(RoutingService routingService,
                             RouteTollService routeTollService){
        this.routingService= routingService;
        this.routeTollService=routeTollService;
    }

    @GetMapping("/test")
    public RouteResponse testRoute(){
        return routingService.getRoute();
    }

    @GetMapping("/test-geometry")
    public Map<String, Object> testGeometry() {

        RouteResponse response = routingService.getRoute();

        String geometry = response.routes().get(0).geometry();

        List<double[]> coordinates = PolyLineDecoder.decode(geometry);

        return Map.of(
                "totalCoordinates", coordinates.size(),
                "firstCoordinate", coordinates.get(0),
                "lastCoordinate", coordinates.get(coordinates.size() - 1)
        );
    }

    @GetMapping("/toll-plazas")
    public List<RouteTollResponse> getTollPlazasAlongRoute(
            @RequestParam(name = "source", required = false) String source,
            @RequestParam(name = "destination", required = false) String destination) {

        validatePincode(source, "source");
        validatePincode(destination, "destination");

        return routeTollService.findTollPlazasAlongRoute(source.trim(), destination.trim());
    }

    private void validatePincode(String pincode, String paramName) {
        if (pincode == null || pincode.isBlank()) {
            throw new IllegalArgumentException("Parameter '" + paramName + "' is required and cannot be blank.");
        }
        String trimmed = pincode.trim();
        if (!PINCODE_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid " + paramName + " pincode: '" + pincode + "'. Pincode must be a 6-digit Indian postal code starting with digits 1-9.");
        }
    }
}
