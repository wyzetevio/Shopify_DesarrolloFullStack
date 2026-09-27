package com.shopcloud.tienda.repository;

import com.shopcloud.tienda.entity.Tienda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TiendaRepository
        extends JpaRepository<Tienda, Long> {

    boolean existsBySlug(String slug);

    Optional<Tienda> findBySlug(String slug);

    List<Tienda> findByPropietarioId(Long propietarioId);

    Optional<Tienda> findByIdAndPropietarioId(
            Long tiendaId,
            Long propietarioId
    );

    boolean existsByIdAndPropietarioIdAndActivoTrue(
            Long tiendaId,
            Long propietarioId
    );

    boolean existsByIdAndActivoTrue(Long tiendaId);
}