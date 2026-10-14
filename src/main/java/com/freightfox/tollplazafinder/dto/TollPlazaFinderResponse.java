package com.freightfox.tollplazafinder.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Response containing route overview and matched toll plazas")
public record TollPlazaFinderResponse(
        @Schema(description = "Route metadata including origin, destination, and calculated distance")
        RouteInfo route,

        @Schema(description = "List of toll plazas located along the route, ordered ascending by distance from source")
        List<TollPlazaItem> tollPlazas
) {
    @Schema(description = "Route metadata")
    public record RouteInfo(
            @Schema(description = "Source Indian postal code", example = "110001")
            String sourcePincode,

            @Schema(description = "Destination Indian postal code", example = "560001")
            String destinationPincode,

            @Schema(description = "Total route driving distance in kilometers", example = "2174.52")
            double distanceInKm
    ) {}

    @Schema(description = "Toll plaza details and distance along the route")
    public record TollPlazaItem(
            @Schema(description = "Name of the toll plaza", example = "Chhayan Toll Plaza")
            String name,

            @Schema(description = "Latitude of the toll plaza", example = "27.818817")
            double latitude,

            @Schema(description = "Longitude of the toll plaza", example = "77.56847")
            double longitude,

            @Schema(description = "Distance from route origin along the driving path in kilometers", example = "92.15")
            double distanceFromSource
    ) {}
}

