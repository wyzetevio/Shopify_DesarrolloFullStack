package com.shopcloud.pedido.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CheckoutDTO {
    @NotNull(message = "La dirección es obligatoria")
    @Positive(message = "El ID de la dirección debe ser válido")
    private Long direccionId;
}
