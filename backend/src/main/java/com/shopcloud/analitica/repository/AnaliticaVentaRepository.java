package com.shopcloud.analitica.repository;

import com.shopcloud.analitica.entity.AnaliticaVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnaliticaVentaRepository extends JpaRepository<AnaliticaVenta, Long> {
    Optional<AnaliticaVenta> findByTiendaIdAndFecha(Long tiendaId, LocalDate fecha);

    List<AnaliticaVenta> findByTiendaIdAndFechaBetweenOrderByFechaAsc(
            Long tiendaId, LocalDate desde, LocalDate hasta);

    void deleteByTiendaIdAndFechaBetween(Long tiendaId, LocalDate desde, LocalDate hasta);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO analiticas_ventas
                (tienda_id, fecha, cantidad_pedidos, productos_vendidos,
                 cantidad_clientes, monto_ventas)
            VALUES
                (:tiendaId, :fecha, :cantidadPedidos, :productosVendidos,
                 :cantidadClientes, :montoVentas)
            ON CONFLICT (tienda_id, fecha) DO UPDATE SET
                cantidad_pedidos = EXCLUDED.cantidad_pedidos,
                productos_vendidos = EXCLUDED.productos_vendidos,
                cantidad_clientes = EXCLUDED.cantidad_clientes,
                monto_ventas = EXCLUDED.monto_ventas
            """, nativeQuery = true)
    int guardarResumen(
            @Param("tiendaId") Long tiendaId,
            @Param("fecha") LocalDate fecha,
            @Param("cantidadPedidos") int cantidadPedidos,
            @Param("productosVendidos") int productosVendidos,
            @Param("cantidadClientes") int cantidadClientes,
            @Param("montoVentas") BigDecimal montoVentas);
}
