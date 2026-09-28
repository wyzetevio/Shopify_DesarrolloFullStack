package com.shopcloud.carrito.service;

import com.shopcloud.carrito.dto.ActualizarCantidadDTO;
import com.shopcloud.carrito.dto.AgregarProductoDTO;
import com.shopcloud.carrito.dto.CarritoDTO;
import com.shopcloud.carrito.entity.Carrito;
import com.shopcloud.carrito.entity.ElementoCarrito;
import com.shopcloud.carrito.repository.CarritoRepository;
import com.shopcloud.carrito.repository.ElementoCarritoRepository;
import com.shopcloud.catalogo.entity.Producto;
import com.shopcloud.catalogo.repository.ProductoRepository;
import com.shopcloud.catalogo.repository.VarianteRepository;
import com.shopcloud.cliente.entity.Cliente;
import com.shopcloud.cliente.service.ClienteService;
import com.shopcloud.exception.RecursoNoEncontradoException;
import com.shopcloud.inventario.entity.Inventario;
import com.shopcloud.inventario.repository.InventarioRepository;
import com.shopcloud.tenant.TenantService;
import com.shopcloud.tienda.entity.Tienda;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    @Mock private CarritoRepository carritoRepository;
    @Mock private ElementoCarritoRepository elementoRepository;
    @Mock private ClienteService clienteService;
    @Mock private ProductoRepository productoRepository;
    @Mock private VarianteRepository varianteRepository;
    @Mock private InventarioRepository inventarioRepository;
    @Mock private TenantService tenantService;
    @InjectMocks private CarritoService carritoService;

    @Test
    void noPermiteModificarElementoDeOtroCarrito() {
        Cliente cliente = cliente(100L, 1L);
        Carrito carrito = carrito(20L, cliente);
        ActualizarCantidadDTO dto = new ActualizarCantidadDTO();
        dto.setCantidad(2);

        when(tenantService.obtenerTenantId()).thenReturn(1L);
        when(clienteService.obtenerClienteActualSiExiste()).thenReturn(Optional.of(cliente));
        when(carritoRepository.findFirstByClienteIdAndTiendaIdAndEstadoOrderByCreadoEnDesc(
                100L, 1L, "ACTIVO")).thenReturn(Optional.of(carrito));
        when(elementoRepository.findByIdAndCarritoId(999L, 20L))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> carritoService.actualizarCantidad(999L, dto));
        verify(elementoRepository, never()).save(any());
    }

    @Test
    void noPermiteAgregarProductoDeOtroTenant() {
        AgregarProductoDTO dto = agregar(77L, null, 1);
        Cliente cliente = cliente(100L, 1L);

        when(tenantService.obtenerTenantId()).thenReturn(1L);
        when(clienteService.obtenerOCrearClienteActual()).thenReturn(cliente);
        when(productoRepository.findByIdAndTiendaIdAndActivoTrue(77L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> carritoService.agregarProducto(dto));
        verify(elementoRepository, never()).save(any());
    }

    @Test
    void acumulaCantidadDelMismoProductoSinVariante() {
        Cliente cliente = cliente(100L, 1L);
        Carrito carrito = carrito(20L, cliente);
        Producto producto = Producto.builder()
                .id(7L).nombre("Laptop").precio(new BigDecimal("100.00"))
                .activo(true).build();
        Inventario inventario = Inventario.builder().cantidad(10).build();
        ElementoCarrito existente = ElementoCarrito.builder()
                .id(50L).carrito(carrito).producto(producto)
                .cantidad(3).precioUnitario(new BigDecimal("100.00")).build();

        when(tenantService.obtenerTenantId()).thenReturn(1L);
        when(clienteService.obtenerOCrearClienteActual()).thenReturn(cliente);
        when(productoRepository.findByIdAndTiendaIdAndActivoTrue(7L, 1L))
                .thenReturn(Optional.of(producto));
        when(inventarioRepository.findByTiendaIdAndProductoIdAndVarianteIsNull(1L, 7L))
                .thenReturn(Optional.of(inventario));
        when(carritoRepository.findFirstByClienteIdAndTiendaIdAndEstadoOrderByCreadoEnDesc(
                100L, 1L, "ACTIVO")).thenReturn(Optional.of(carrito));
        when(elementoRepository.findByCarritoIdAndProductoIdAndVarianteIsNull(20L, 7L))
                .thenReturn(Optional.of(existente));
        when(elementoRepository.findByCarritoIdOrderById(20L))
                .thenReturn(List.of(existente));

        CarritoDTO resultado = carritoService.agregarProducto(agregar(7L, null, 2));

        assertEquals(5, resultado.getCantidadTotal());
        assertEquals(new BigDecimal("500.00"), resultado.getSubtotal());
        assertEquals(5, existente.getCantidad());
        verify(elementoRepository).save(existente);
    }

    private AgregarProductoDTO agregar(Long productoId, Long varianteId, int cantidad) {
        AgregarProductoDTO dto = new AgregarProductoDTO();
        dto.setProductoId(productoId);
        dto.setVarianteId(varianteId);
        dto.setCantidad(cantidad);
        return dto;
    }

    private Cliente cliente(Long clienteId, Long tiendaId) {
        Tienda tienda = Tienda.builder().id(tiendaId).activo(true).build();
        return Cliente.builder().id(clienteId).tienda(tienda).build();
    }

    private Carrito carrito(Long carritoId, Cliente cliente) {
        return Carrito.builder()
                .id(carritoId).cliente(cliente).tienda(cliente.getTienda())
                .estado("ACTIVO").build();
    }
}
