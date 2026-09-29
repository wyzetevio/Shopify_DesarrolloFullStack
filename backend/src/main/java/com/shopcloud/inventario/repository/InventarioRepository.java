package com.shopcloud.inventario.repository;

import com.shopcloud.inventario.entity.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

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

    Optional<Inventario> findByTiendaIdAndProductoIdAndVarianteIsNull(
            Long tiendaId,
            Long productoId
    );

    boolean existsByTiendaIdAndProductoIdAndVarianteId(
            Long tiendaId,
            Long productoId,
            Long varianteId
    );

    Optional<Inventario> findByTiendaIdAndProductoIdAndVarianteId(
            Long tiendaId,
            Long productoId,
            Long varianteId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventario> findWithLockByTiendaIdAndProductoIdAndVarianteIsNull(
            Long tiendaId,
            Long productoId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventario> findWithLockByTiendaIdAndProductoIdAndVarianteId(
            Long tiendaId,
            Long productoId,
            Long varianteId
    );
}
