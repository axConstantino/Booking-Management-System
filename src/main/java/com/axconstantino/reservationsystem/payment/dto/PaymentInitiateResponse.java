package com.axconstantino.reservationsystem.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentInitiateResponse {
    private String url;
    private String sessionId;
    private String error;


    public PaymentInitiateResponse(String url, String sessionId) {
        this.url = url;
        this.sessionId = sessionId;
        this.error = null;
    }

    public PaymentInitiateResponse(String error) {
        this.error = error;
    }
}
