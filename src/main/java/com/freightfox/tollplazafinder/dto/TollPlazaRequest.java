package com.freightfox.tollplazafinder.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request payload containing source and destination Indian postal codes")
public record TollPlazaRequest(
        @Schema(description = "6-digit Indian postal code for route origin", example = "110001", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonAlias("source") String sourcePincode,

        @Schema(description = "6-digit Indian postal code for route destination", example = "560001", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonAlias("destination") String destinationPincode
) {
}

