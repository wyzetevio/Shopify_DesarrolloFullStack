package com.shopcloud.inventario.repository;

import com.shopcloud.inventario.entity.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InventarioRepository
        extends JpaRepository<Inventario, Long> {

    List<Inventario> findByTiendaId(
            Long tiendaId
    );

    Optional<Inventario> findByIdAndTiendaId(
            Long id,
            Long tiendaId
    );

    boolean existsByTiendaIdAndProductoIdAndVarianteIsNull(
            Long tiendaId,
            Long productoId
    );

    boolean existsByTiendaIdAndProductoIdAndVarianteId(
            Long tiendaId,
            Long productoId,
            Long varianteId
    );
}