package com.freightfox.tollplazafinder.controller;

import com.freightfox.tollplazafinder.dto.TollPlazaFinderResponse;
import com.freightfox.tollplazafinder.dto.TollPlazaRequest;
import com.freightfox.tollplazafinder.service.RouteTollService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/toll-plazas")
public class TollPlazaController {
    private static final Pattern PINCODE_PATTERN = Pattern.compile("^[1-9][0-9]{5}$");

    private final RouteTollService routeTollService;

    public TollPlazaController(RouteTollService routeTollService) {
        this.routeTollService = routeTollService;
    }

    @PostMapping
    public TollPlazaFinderResponse findTollPlazasAlongRoute(@RequestBody(required = false) TollPlazaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required.");
        }

        validatePincode(request.sourcePincode(), "sourcePincode");
        validatePincode(request.destinationPincode(), "destinationPincode");

        return routeTollService.findTollPlazasAlongRoute(
                request.sourcePincode().trim(),
                request.destinationPincode().trim()
        );
    }

    private void validatePincode(String pincode, String paramName) {
        if (pincode == null || pincode.isBlank()) {
            throw new IllegalArgumentException("Field '" + paramName + "' is required and cannot be blank.");
        }
        String trimmed = pincode.trim();
        if (!PINCODE_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("Invalid " + paramName + ": '" + pincode + "'. Pincode must be a 6-digit Indian postal code starting with digits 1-9.");
        }
    }
}

