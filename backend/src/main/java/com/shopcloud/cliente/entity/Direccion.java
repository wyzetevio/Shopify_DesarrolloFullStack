package com.shopcloud.cliente.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "direcciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Direccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(
            name = "linea_direccion",
            nullable = false,
            length = 255
    )
    private String lineaDireccion;

    @Column(nullable = false, length = 100)
    private String ciudad;

    @Column(length = 100)
    private String distrito;

    @Column(length = 100)
    private String departamento;

    @Column(name = "codigo_postal", length = 20)
    private String codigoPostal;

    @Column(length = 255)
    private String referencia;
}