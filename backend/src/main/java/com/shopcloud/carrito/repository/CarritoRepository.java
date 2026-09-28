package com.shopcloud.carrito.repository;

import com.shopcloud.carrito.entity.Carrito;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CarritoRepository extends JpaRepository<Carrito, Long> {
    Optional<Carrito> findFirstByClienteIdAndTiendaIdAndEstadoOrderByCreadoEnDesc(
            Long clienteId, Long tiendaId, String estado);
}
