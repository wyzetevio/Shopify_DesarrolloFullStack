package com.shopcloud.pedido.service;

import com.shopcloud.carrito.entity.Carrito;
import com.shopcloud.carrito.entity.ElementoCarrito;
import com.shopcloud.carrito.service.CarritoService;
import com.shopcloud.catalogo.entity.Producto;
import com.shopcloud.catalogo.entity.Variante;
import com.shopcloud.catalogo.repository.ProductoRepository;
import com.shopcloud.catalogo.repository.VarianteRepository;
import com.shopcloud.cliente.entity.Cliente;
import com.shopcloud.cliente.entity.Direccion;
import com.shopcloud.cliente.repository.DireccionRepository;
import com.shopcloud.cliente.service.ClienteService;
import com.shopcloud.exception.RecursoNoEncontradoException;
import com.shopcloud.exception.ReglaNegocioException;
import com.shopcloud.inventario.entity.Inventario;
import com.shopcloud.inventario.repository.InventarioRepository;
import com.shopcloud.pedido.dto.CheckoutDTO;
import com.shopcloud.pedido.dto.PedidoRespuestaDTO;
import com.shopcloud.pedido.entity.ElementoPedido;
import com.shopcloud.pedido.entity.Pedido;
import com.shopcloud.pedido.repository.ElementoPedidoRepository;
import com.shopcloud.pedido.repository.PedidoRepository;
import com.shopcloud.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final ClienteService clienteService;
    private final CarritoService carritoService;
    private final DireccionRepository direccionRepository;
    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final InventarioRepository inventarioRepository;
    private final PedidoRepository pedidoRepository;
    private final ElementoPedidoRepository elementoPedidoRepository;
    private final PedidoService pedidoService;
    private final TenantService tenantService;

    @Transactional
    public PedidoRespuestaDTO realizarCheckout(CheckoutDTO dto) {
        Long tiendaId = tenantService.obtenerTenantId();
        Cliente cliente = clienteService.obtenerClienteActualSiExiste()
                .orElseThrow(() -> new ReglaNegocioException(
                        "No existe un cliente para realizar el checkout"));

        Carrito carrito = carritoService.obtenerCarritoActualParaCheckout();
        if (!carrito.getCliente().getId().equals(cliente.getId())
                || !carrito.getTienda().getId().equals(tiendaId)) {
            throw new RecursoNoEncontradoException("Carrito activo no encontrado");
        }

        List<ElementoCarrito> elementosCarrito = carritoService
                .obtenerElementosParaCheckout(carrito);
        if (elementosCarrito.isEmpty()) {
            throw new ReglaNegocioException("El carrito está vacío");
        }

        Direccion direccion = direccionRepository
                .findByIdAndClienteId(dto.getDireccionId(), cliente.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Dirección no encontrada para el cliente actual"));

        List<LineaCheckout> lineas = elementosCarrito.stream()
                .sorted(Comparator
                        .comparing((ElementoCarrito e) -> e.getProducto().getId())
                        .thenComparing(e -> e.getVariante() != null
                                ? e.getVariante().getId() : Long.MIN_VALUE))
                .map(elemento -> prepararLinea(elemento, tiendaId))
                .toList();

        BigDecimal subtotal = lineas.stream()
                .map(LineaCheckout::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal montoDescuento = BigDecimal.ZERO;

        Pedido pedido = pedidoRepository.save(
                Pedido.builder()
                        .tienda(cliente.getTienda())
                        .cliente(cliente)
                        .direccion(direccion)
                        .estado("PENDIENTE")
                        .subtotal(subtotal)
                        .montoDescuento(montoDescuento)
                        .total(subtotal.subtract(montoDescuento))
                        .build());

        List<ElementoPedido> elementosPedido = lineas.stream()
                .map(linea -> crearElementoPedido(pedido, linea))
                .toList();
        elementoPedidoRepository.saveAll(elementosPedido);

        lineas.forEach(linea -> {
            Inventario inventario = linea.inventario();
            inventario.setCantidad(inventario.getCantidad() - linea.cantidad());
            inventarioRepository.save(inventario);
        });

        carritoService.vaciarDespuesDeCheckout(carrito);
        return pedidoService.convertirRespuesta(pedido);
    }

    private LineaCheckout prepararLinea(ElementoCarrito elemento, Long tiendaId) {
        Producto producto = productoRepository
                .findByIdAndTiendaIdAndActivoTrue(
                        elemento.getProducto().getId(), tiendaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Producto activo no encontrado durante el checkout"));

        Variante variante = null;
        if (elemento.getVariante() != null) {
            variante = varianteRepository
                    .findByIdAndProductoIdAndActivoTrue(
                            elemento.getVariante().getId(), producto.getId())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "Variante activa no encontrada durante el checkout"));
        }

        Inventario inventario = obtenerInventarioBloqueado(
                tiendaId, producto.getId(), variante);
        if (inventario.getCantidad() < elemento.getCantidad()) {
            throw new ReglaNegocioException(
                    "Stock insuficiente para " + producto.getNombre()
                            + ". Disponible: " + inventario.getCantidad());
        }

        BigDecimal precio = variante != null && variante.getPrecio() != null
                ? variante.getPrecio()
                : producto.getPrecio();
        BigDecimal subtotal = precio.multiply(
                BigDecimal.valueOf(elemento.getCantidad()));

        return new LineaCheckout(
                producto, variante, inventario,
                elemento.getCantidad(), precio, subtotal);
    }

    private Inventario obtenerInventarioBloqueado(
            Long tiendaId, Long productoId, Variante variante) {
        return (variante == null
                ? inventarioRepository
                    .findWithLockByTiendaIdAndProductoIdAndVarianteIsNull(
                            tiendaId, productoId)
                : inventarioRepository
                    .findWithLockByTiendaIdAndProductoIdAndVarianteId(
                            tiendaId, productoId, variante.getId()))
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Inventario no configurado durante el checkout"));
    }

    private ElementoPedido crearElementoPedido(Pedido pedido, LineaCheckout linea) {
        return ElementoPedido.builder()
                .pedido(pedido)
                .producto(linea.producto())
                .variante(linea.variante())
                .nombreProducto(linea.producto().getNombre())
                .cantidad(linea.cantidad())
                .precioUnitario(linea.precioUnitario())
                .subtotal(linea.subtotal())
                .build();
    }

    private record LineaCheckout(
            Producto producto,
            Variante variante,
            Inventario inventario,
            int cantidad,
            BigDecimal precioUnitario,
            BigDecimal subtotal
    ) {}
}
