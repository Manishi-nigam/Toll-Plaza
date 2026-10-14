package com.freightfox.tollplazafinder.controller;

import com.freightfox.tollplazafinder.dto.TollPlazaFinderResponse;
import com.freightfox.tollplazafinder.dto.TollPlazaRequest;
import com.freightfox.tollplazafinder.service.RouteTollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/toll-plazas")
@Tag(name = "Toll Plazas", description = "Endpoints for route calculation and toll plaza matching along Indian highways")
public class TollPlazaController {
    private static final Pattern PINCODE_PATTERN = Pattern.compile("^[1-9][0-9]{5}$");

    private final RouteTollService routeTollService;

    public TollPlazaController(RouteTollService routeTollService) {
        this.routeTollService = routeTollService;
    }

    @PostMapping
    @Operation(
            summary = "Find toll plazas along a route",
            description = "Calculates driving route between source and destination Indian pincodes and returns route details with all toll plazas along the route."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Route and toll plazas successfully calculated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = TollPlazaFinderResponse.class))),
            @ApiResponse(responseCode = "400", description = "Bad Request - Invalid or missing pincode parameters"),
            @ApiResponse(responseCode = "404", description = "Not Found - Geographic coordinates could not be resolved for a pincode"),
            @ApiResponse(responseCode = "502", description = "Bad Gateway - External routing or geocoding service unavailable")
    })
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

