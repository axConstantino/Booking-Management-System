package com.axconstantino.reservationsystem.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TokenResponse(
        @JsonProperty("accessToken")
        String accessToken,
        @JsonProperty("refreshToken")
        String refreshToken
) {
}
