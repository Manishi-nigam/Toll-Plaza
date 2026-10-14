package com.freightfox.tollplazafinder.dto;

public record RouteTollResponse(
        Long id,
        String tollName,
        double latitude,
        double longitude,
        String geoState,
        double distanceFromSourceKm
) {
}