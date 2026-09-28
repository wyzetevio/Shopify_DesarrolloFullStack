package com.shopcloud.pago.repository;

import com.shopcloud.pago.entity.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
    boolean existsByPedidoIdAndEstado(Long pedidoId, String estado);
    List<Transaccion> findByPedidoClienteIdAndPedidoTiendaIdOrderByCreadoEnDesc(
            Long clienteId, Long tiendaId);
    Optional<Transaccion> findByIdAndPedidoClienteIdAndPedidoTiendaId(
            Long transaccionId, Long clienteId, Long tiendaId);
}
