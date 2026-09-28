package com.shopcloud.catalogo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "variantes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Variante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 100)
    private String sku;

    @Column(precision = 12, scale = 2)
    private BigDecimal precio;

    @Column(nullable = false)
    private Boolean activo;

    @PrePersist
    protected void prePersist() {
        if (activo == null) {
            activo = true;
        }
    }
}