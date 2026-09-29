package com.shopcloud.analitica.entity;

import com.shopcloud.tienda.entity.Tienda;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "analiticas_ventas", uniqueConstraints =
        @UniqueConstraint(name = "uq_analiticas_ventas_tienda_fecha",
                columnNames = {"tienda_id", "fecha"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AnaliticaVenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(name = "cantidad_pedidos", nullable = false)
    private Integer cantidadPedidos;

    @Column(name = "productos_vendidos", nullable = false)
    private Integer productosVendidos;

    @Column(name = "cantidad_clientes", nullable = false)
    private Integer cantidadClientes;

    @Column(name = "monto_ventas", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoVentas;
}
