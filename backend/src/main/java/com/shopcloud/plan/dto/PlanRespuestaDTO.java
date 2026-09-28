package com.shopcloud.plan.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PlanRespuestaDTO {

    private Long id;
    private String nombre;
    private String descripcion;

    private BigDecimal precio;

    private Integer maxProductos;
    private Integer maxUsuarios;

    private Boolean activo;
}