package com.shopcloud.carrito.entity;

import com.shopcloud.cliente.entity.Cliente;
import com.shopcloud.tienda.entity.Tienda;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "carritos")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Carrito {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    private Cliente cliente;

    @Column(name = "sesion_id", length = 100)
    private String sesionId;

    @Column(nullable = false, length = 30)
    private String estado;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    protected void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (estado == null) estado = "ACTIVO";
        if (creadoEn == null) creadoEn = ahora;
        actualizadoEn = ahora;
    }

    @PreUpdate
    protected void preUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}
