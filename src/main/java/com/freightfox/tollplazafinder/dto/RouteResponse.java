package com.freightfox.tollplazafinder.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RouteResponse(
        List<Route> routes) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(
            Summary summary,
            String geometry){}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summary(
            double distance,
            double duration){}
}
