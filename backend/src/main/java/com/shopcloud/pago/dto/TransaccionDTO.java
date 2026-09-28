package com.shopcloud.pago.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Builder
public class TransaccionDTO {
    private Long id;
    private Long pedidoId;
    private Long metodoPagoId;
    private String metodoPagoNombre;
    private BigDecimal monto;
    private String estado;
    private String referenciaTransaccion;
    private LocalDateTime creadoEn;
}
