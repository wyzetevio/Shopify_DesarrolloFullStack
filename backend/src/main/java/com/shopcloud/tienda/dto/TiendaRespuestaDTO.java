package com.shopcloud.tienda.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class TiendaRespuestaDTO {

    private Long id;

    private String nombre;

    private String slug;

    private String descripcion;

    private String urlLogo;

    private String urlBanner;

    private String colorPrimario;

    private String correoContacto;

    private String telefonoContacto;

    private Boolean activo;

    private LocalDateTime creadoEn;
}