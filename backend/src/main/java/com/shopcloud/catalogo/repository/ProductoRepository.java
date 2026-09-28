package com.shopcloud.catalogo.repository;

import com.shopcloud.catalogo.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository
        extends JpaRepository<Producto, Long> {

    List<Producto> findByTiendaIdAndActivoTrue(
            Long tiendaId
    );

    Optional<Producto> findByIdAndTiendaId(
            Long productoId,
            Long tiendaId
    );

    boolean existsByTiendaIdAndSkuIgnoreCase(
            Long tiendaId,
            String sku
    );

    boolean existsByTiendaIdAndSkuIgnoreCaseAndIdNot(
            Long tiendaId,
            String sku,
            Long productoId
    );
}