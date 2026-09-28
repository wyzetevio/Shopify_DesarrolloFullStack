package com.shopcloud.pago.dto;

import lombok.Builder;
import lombok.Getter;

@Getter @Builder
public class MetodoPagoDTO {
    private Long id;
    private String nombre;
    private String tipo;
}
