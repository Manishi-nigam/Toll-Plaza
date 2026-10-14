package com.freightfox.tollplazafinder.dto;

import java.util.List;

public record TollPlazaFinderResponse(
        RouteInfo route,
        List<TollPlazaItem> tollPlazas
) {
    public record RouteInfo(
            String sourcePincode,
            String destinationPincode,
            double distanceInKm
    ) {}

    public record TollPlazaItem(
            String name,
            double latitude,
            double longitude,
            double distanceFromSource
    ) {}
}
