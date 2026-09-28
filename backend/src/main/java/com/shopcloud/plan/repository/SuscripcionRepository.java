package com.shopcloud.plan.repository;

import com.shopcloud.plan.entity.Suscripcion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SuscripcionRepository
        extends JpaRepository<Suscripcion, Long> {

    List<Suscripcion> findByTiendaId(
            Long tiendaId
    );

    Optional<Suscripcion> findByIdAndTiendaId(
            Long id,
            Long tiendaId
    );

    Optional<Suscripcion>
    findFirstByTiendaIdAndEstadoOrderByCreadoEnDesc(
            Long tiendaId,
            String estado
    );

    boolean existsByTiendaIdAndEstado(
            Long tiendaId,
            String estado
    );
}