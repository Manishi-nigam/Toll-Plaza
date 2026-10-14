package com.freightfox.tollplazafinder.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TollPlazaRequest(
        @JsonAlias("source") String sourcePincode,
        @JsonAlias("destination") String destinationPincode
) {
}
