package com.shopcloud.pedido.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Builder
public class PedidoRespuestaDTO {
    private Long id;
    private String estado;
    private BigDecimal subtotal;
    private BigDecimal montoDescuento;
    private BigDecimal total;
    private LocalDateTime creadoEn;
    private Long direccionId;
    private String lineaDireccion;
    private String ciudad;
    private List<ElementoPedidoDTO> elementos;
}
