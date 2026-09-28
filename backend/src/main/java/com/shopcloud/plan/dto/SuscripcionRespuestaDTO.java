package com.shopcloud.plan.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class SuscripcionRespuestaDTO {

    private Long id;

    private Long tiendaId;

    private Long planId;
    private String planNombre;
    private BigDecimal planPrecio;

    private String estado;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    private LocalDateTime creadoEn;
}