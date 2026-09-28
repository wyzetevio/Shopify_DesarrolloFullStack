package com.shopcloud.cliente.entity;

import com.shopcloud.tienda.entity.Tienda;
import com.shopcloud.usuario.entity.Usuario;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tienda_id", nullable = false)
    private Tienda tienda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(
            name = "correo_electronico",
            nullable = false,
            length = 150
    )
    private String correoElectronico;

    @Column(length = 30)
    private String telefono;

    @Column(
            name = "creado_en",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime creadoEn;
}