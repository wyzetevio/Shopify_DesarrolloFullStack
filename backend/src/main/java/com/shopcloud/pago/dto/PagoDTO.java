package com.shopcloud.pago.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PagoDTO {
    @NotNull(message = "El pedido es obligatorio")
    @Positive(message = "El ID del pedido debe ser válido")
    private Long pedidoId;

    @NotNull(message = "El método de pago es obligatorio")
    @Positive(message = "El ID del método de pago debe ser válido")
    private Long metodoPagoId;
}
