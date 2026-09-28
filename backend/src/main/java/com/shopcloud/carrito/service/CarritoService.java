package com.shopcloud.carrito.service;

import com.shopcloud.carrito.dto.*;
import com.shopcloud.carrito.entity.Carrito;
import com.shopcloud.carrito.entity.ElementoCarrito;
import com.shopcloud.carrito.repository.CarritoRepository;
import com.shopcloud.carrito.repository.ElementoCarritoRepository;
import com.shopcloud.catalogo.entity.Producto;
import com.shopcloud.catalogo.entity.Variante;
import com.shopcloud.catalogo.repository.ProductoRepository;
import com.shopcloud.catalogo.repository.VarianteRepository;
import com.shopcloud.cliente.entity.Cliente;
import com.shopcloud.cliente.service.ClienteService;
import com.shopcloud.exception.RecursoNoEncontradoException;
import com.shopcloud.exception.ReglaNegocioException;
import com.shopcloud.inventario.entity.Inventario;
import com.shopcloud.inventario.repository.InventarioRepository;
import com.shopcloud.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CarritoService {

    private static final String ESTADO_ACTIVO = "ACTIVO";

    private final CarritoRepository carritoRepository;
    private final ElementoCarritoRepository elementoRepository;
    private final ClienteService clienteService;
    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final InventarioRepository inventarioRepository;
    private final TenantService tenantService;

    @Transactional(readOnly = true)
    public CarritoDTO obtenerActual() {
        return buscarCarritoActualSiExiste()
                .map(this::convertirDTO)
                .orElseGet(this::carritoVacio);
    }

    @Transactional(readOnly = true)
    public Carrito obtenerCarritoActualParaCheckout() {
        return obtenerCarritoActual();
    }

    @Transactional(readOnly = true)
    public List<ElementoCarrito> obtenerElementosParaCheckout(Carrito carrito) {
        return elementoRepository.findByCarritoIdOrderById(carrito.getId());
    }

    @Transactional
    public void vaciarDespuesDeCheckout(Carrito carrito) {
        elementoRepository.deleteByCarritoId(carrito.getId());
        tocar(carrito);
    }

    @Transactional
    public CarritoDTO agregarProducto(AgregarProductoDTO dto) {
        Long tiendaId = tenantService.obtenerTenantId();
        Cliente cliente = clienteService.obtenerOCrearClienteActual();
        Producto producto = buscarProductoActivo(dto.getProductoId(), tiendaId);
        Variante variante = buscarVarianteActiva(producto, dto.getVarianteId());
        Inventario inventario = buscarInventario(tiendaId, producto.getId(), variante);
        Carrito carrito = obtenerOCrearCarritoActivo(cliente, tiendaId);

        Optional<ElementoCarrito> existente = buscarElementoPorProducto(
                carrito.getId(), producto.getId(), variante);
        int cantidadResultante = dto.getCantidad()
                + existente.map(ElementoCarrito::getCantidad).orElse(0);

        validarStock(inventario, cantidadResultante);

        BigDecimal precio = variante != null && variante.getPrecio() != null
                ? variante.getPrecio()
                : producto.getPrecio();

        ElementoCarrito elemento = existente.orElseGet(() ->
                ElementoCarrito.builder()
                        .carrito(carrito)
                        .producto(producto)
                        .variante(variante)
                        .build());

        elemento.setCantidad(cantidadResultante);
        elemento.setPrecioUnitario(precio);
        elementoRepository.save(elemento);
        tocar(carrito);

        return convertirDTO(carrito);
    }

    @Transactional
    public CarritoDTO actualizarCantidad(Long elementoId, ActualizarCantidadDTO dto) {
        Long tiendaId = tenantService.obtenerTenantId();
        Carrito carrito = obtenerCarritoActual();
        ElementoCarrito elemento = buscarElementoPropio(elementoId, carrito.getId());
        Inventario inventario = buscarInventario(
                tiendaId,
                elemento.getProducto().getId(),
                elemento.getVariante()
        );

        validarStock(inventario, dto.getCantidad());
        elemento.setCantidad(dto.getCantidad());
        elementoRepository.save(elemento);
        tocar(carrito);

        return convertirDTO(carrito);
    }

    @Transactional
    public void eliminarElemento(Long elementoId) {
        Carrito carrito = obtenerCarritoActual();
        ElementoCarrito elemento = buscarElementoPropio(elementoId, carrito.getId());
        elementoRepository.delete(elemento);
        tocar(carrito);
    }

    @Transactional
    public void vaciar() {
        buscarCarritoActualSiExiste().ifPresent(carrito -> {
            elementoRepository.deleteByCarritoId(carrito.getId());
            tocar(carrito);
        });
    }

    private Optional<Carrito> buscarCarritoActualSiExiste() {
        Long tiendaId = tenantService.obtenerTenantId();
        return clienteService.obtenerClienteActualSiExiste()
                .flatMap(cliente -> carritoRepository
                        .findFirstByClienteIdAndTiendaIdAndEstadoOrderByCreadoEnDesc(
                                cliente.getId(), tiendaId, ESTADO_ACTIVO));
    }

    private Carrito obtenerCarritoActual() {
        return buscarCarritoActualSiExiste()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Carrito activo no encontrado"));
    }

    private Carrito obtenerOCrearCarritoActivo(Cliente cliente, Long tiendaId) {
        return carritoRepository
                .findFirstByClienteIdAndTiendaIdAndEstadoOrderByCreadoEnDesc(
                        cliente.getId(), tiendaId, ESTADO_ACTIVO)
                .orElseGet(() -> carritoRepository.save(
                        Carrito.builder()
                                .tienda(cliente.getTienda())
                                .cliente(cliente)
                                .estado(ESTADO_ACTIVO)
                                .build()));
    }

    private Producto buscarProductoActivo(Long productoId, Long tiendaId) {
        return productoRepository
                .findByIdAndTiendaIdAndActivoTrue(productoId, tiendaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Producto activo no encontrado en la tienda"));
    }

    private Variante buscarVarianteActiva(Producto producto, Long varianteId) {
        if (varianteId == null) return null;
        return varianteRepository
                .findByIdAndProductoIdAndActivoTrue(varianteId, producto.getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Variante activa no encontrada para el producto"));
    }

    private Inventario buscarInventario(
            Long tiendaId,
            Long productoId,
            Variante variante
    ) {
        Optional<Inventario> inventario = variante == null
                ? inventarioRepository.findByTiendaIdAndProductoIdAndVarianteIsNull(
                        tiendaId, productoId)
                : inventarioRepository.findByTiendaIdAndProductoIdAndVarianteId(
                        tiendaId, productoId, variante.getId());

        return inventario.orElseThrow(() -> new RecursoNoEncontradoException(
                "Inventario no configurado para el producto o variante"));
    }

    private Optional<ElementoCarrito> buscarElementoPorProducto(
            Long carritoId,
            Long productoId,
            Variante variante
    ) {
        return variante == null
                ? elementoRepository.findByCarritoIdAndProductoIdAndVarianteIsNull(
                        carritoId, productoId)
                : elementoRepository.findByCarritoIdAndProductoIdAndVarianteId(
                        carritoId, productoId, variante.getId());
    }

    private ElementoCarrito buscarElementoPropio(Long elementoId, Long carritoId) {
        return elementoRepository.findByIdAndCarritoId(elementoId, carritoId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Elemento no encontrado en el carrito actual"));
    }

    private void validarStock(Inventario inventario, int cantidadSolicitada) {
        if (cantidadSolicitada > inventario.getCantidad()) {
            throw new ReglaNegocioException(
                    "Stock insuficiente. Disponible: " + inventario.getCantidad());
        }
    }

    private void tocar(Carrito carrito) {
        carrito.setActualizadoEn(LocalDateTime.now());
        carritoRepository.save(carrito);
    }

    private CarritoDTO convertirDTO(Carrito carrito) {
        List<ElementoCarritoDTO> elementos = elementoRepository
                .findByCarritoIdOrderById(carrito.getId())
                .stream()
                .map(this::convertirElementoDTO)
                .toList();

        int cantidadTotal = elementos.stream()
                .mapToInt(ElementoCarritoDTO::getCantidad)
                .sum();
        BigDecimal subtotal = elementos.stream()
                .map(ElementoCarritoDTO::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CarritoDTO.builder()
                .id(carrito.getId())
                .elementos(elementos)
                .cantidadTotal(cantidadTotal)
                .subtotal(subtotal)
                .build();
    }

    private ElementoCarritoDTO convertirElementoDTO(ElementoCarrito elemento) {
        Variante variante = elemento.getVariante();
        BigDecimal subtotal = elemento.getPrecioUnitario()
                .multiply(BigDecimal.valueOf(elemento.getCantidad()));

        return ElementoCarritoDTO.builder()
                .id(elemento.getId())
                .productoId(elemento.getProducto().getId())
                .nombreProducto(elemento.getProducto().getNombre())
                .varianteId(variante != null ? variante.getId() : null)
                .nombreVariante(variante != null ? variante.getNombre() : null)
                .cantidad(elemento.getCantidad())
                .precioUnitario(elemento.getPrecioUnitario())
                .subtotal(subtotal)
                .build();
    }

    private CarritoDTO carritoVacio() {
        return CarritoDTO.builder()
                .id(null)
                .elementos(List.of())
                .cantidadTotal(0)
                .subtotal(BigDecimal.ZERO)
                .build();
    }
}
