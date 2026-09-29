package com.shopcloud.analitica.service;

import com.shopcloud.analitica.dto.*;
import com.shopcloud.analitica.entity.AnaliticaVenta;
import com.shopcloud.analitica.repository.AnaliticaVentaRepository;
import com.shopcloud.pedido.repository.ElementoPedidoRepository;
import com.shopcloud.pedido.repository.PedidoRepository;
import com.shopcloud.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnaliticaService {
    private static final int LIMITE_MAXIMO_PRODUCTOS = 100;

    private final AnaliticaVentaRepository analiticaVentaRepository;
    private final PedidoRepository pedidoRepository;
    private final ElementoPedidoRepository elementoPedidoRepository;
    private final TenantService tenantService;

    @Transactional
    public DashboardDTO obtenerDashboard(LocalDate desde, LocalDate hasta) {
        Rango rango = resolverRango(desde, hasta);
        Long tiendaId = tenantService.obtenerTenantId();
        sincronizar(tiendaId, rango);

        PedidoRepository.ResumenPedidoRangoProjection resumen =
                pedidoRepository.resumirVentasDelRango(
                        tiendaId, rango.inicio(), rango.finExclusivo());
        long pedidos = valor(resumen == null ? null : resumen.getCantidadPedidos());
        long clientes = valor(resumen == null ? null : resumen.getCantidadClientes());
        BigDecimal ventas = dinero(resumen == null ? null : resumen.getMontoVentas());
        long productos = valor(elementoPedidoRepository.sumarUnidadesVendidas(
                tiendaId, rango.inicio(), rango.finExclusivo()));

        return DashboardDTO.builder()
                .desde(rango.desde()).hasta(rango.hasta())
                .ventasTotales(ventas)
                .pedidosPagados(pedidos)
                .productosVendidos(productos)
                .clientesCompradores(clientes)
                .ticketPromedio(calcularTicketPromedio(ventas, pedidos))
                .build();
    }

    @Transactional
    public List<ResumenVentasDTO> obtenerVentasDiarias(LocalDate desde, LocalDate hasta) {
        Rango rango = resolverRango(desde, hasta);
        Long tiendaId = tenantService.obtenerTenantId();
        sincronizar(tiendaId, rango);
        return analiticaVentaRepository
                .findByTiendaIdAndFechaBetweenOrderByFechaAsc(
                        tiendaId, rango.desde(), rango.hasta())
                .stream().map(this::convertirResumen).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductoVendidoDTO> obtenerProductosMasVendidos(
            LocalDate desde, LocalDate hasta, Integer limite) {
        Rango rango = resolverRango(desde, hasta);
        int limiteValidado = limite == null ? 5 : limite;
        if (limiteValidado < 1 || limiteValidado > LIMITE_MAXIMO_PRODUCTOS) {
            throw new IllegalArgumentException("limite debe estar entre 1 y 100");
        }
        return elementoPedidoRepository.listarProductosMasVendidos(
                        tenantService.obtenerTenantId(), rango.inicio(),
                        rango.finExclusivo(), limiteValidado)
                .stream()
                .map(p -> ProductoVendidoDTO.builder()
                        .productoId(p.getProductoId())
                        .nombreProducto(p.getNombreProducto())
                        .cantidadVendida(valor(p.getCantidadVendida()))
                        .montoGenerado(dinero(p.getMontoGenerado()))
                        .build())
                .toList();
    }

    private void sincronizar(Long tiendaId, Rango rango) {
        List<PedidoRepository.ResumenPedidoDiarioProjection> pedidosPorDia =
                pedidoRepository.resumirVentasDiarias(
                        tiendaId, rango.inicio(), rango.finExclusivo());
        Map<LocalDate, Long> unidadesPorDia = new HashMap<>();
        elementoPedidoRepository.resumirUnidadesVendidasDiarias(
                        tiendaId, rango.inicio(), rango.finExclusivo())
                .forEach(r -> unidadesPorDia.put(r.getFecha(), valor(r.getCantidad())));

        analiticaVentaRepository.deleteByTiendaIdAndFechaBetween(
                tiendaId, rango.desde(), rango.hasta());
        pedidosPorDia.forEach(r -> analiticaVentaRepository.guardarResumen(
                tiendaId,
                r.getFecha(),
                Math.toIntExact(valor(r.getCantidadPedidos())),
                Math.toIntExact(unidadesPorDia.getOrDefault(r.getFecha(), 0L)),
                Math.toIntExact(valor(r.getCantidadClientes())),
                dinero(r.getMontoVentas())));
    }

    private Rango resolverRango(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now();
        LocalDate fechaDesde = desde == null
                ? hoy.with(TemporalAdjusters.firstDayOfMonth()) : desde;
        LocalDate fechaHasta = hasta == null
                ? hoy.with(TemporalAdjusters.lastDayOfMonth()) : hasta;
        if (fechaDesde.isAfter(fechaHasta)) {
            throw new IllegalArgumentException("desde no puede ser posterior a hasta");
        }
        return new Rango(fechaDesde, fechaHasta, fechaDesde.atStartOfDay(),
                fechaHasta.plusDays(1).atStartOfDay());
    }

    private BigDecimal calcularTicketPromedio(BigDecimal ventas, long pedidos) {
        return pedidos == 0 ? BigDecimal.ZERO.setScale(2)
                : ventas.divide(BigDecimal.valueOf(pedidos), 2, RoundingMode.HALF_UP);
    }

    private ResumenVentasDTO convertirResumen(AnaliticaVenta resumen) {
        return ResumenVentasDTO.builder()
                .fecha(resumen.getFecha())
                .cantidadPedidos(resumen.getCantidadPedidos())
                .productosVendidos(resumen.getProductosVendidos())
                .cantidadClientes(resumen.getCantidadClientes())
                .montoVentas(resumen.getMontoVentas())
                .build();
    }

    private long valor(Long valor) { return valor == null ? 0L : valor; }
    private BigDecimal dinero(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(2) : valor.setScale(2);
    }

    private record Rango(LocalDate desde, LocalDate hasta,
                         LocalDateTime inicio, LocalDateTime finExclusivo) {}
}
