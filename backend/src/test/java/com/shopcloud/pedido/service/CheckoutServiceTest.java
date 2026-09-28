package com.shopcloud.pedido.service;

import com.shopcloud.carrito.entity.Carrito;
import com.shopcloud.carrito.entity.ElementoCarrito;
import com.shopcloud.carrito.service.CarritoService;
import com.shopcloud.catalogo.entity.Producto;
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
import com.shopcloud.pedido.entity.Pedido;
import com.shopcloud.pedido.repository.ElementoPedidoRepository;
import com.shopcloud.pedido.repository.PedidoRepository;
import com.shopcloud.tenant.TenantService;
import com.shopcloud.tienda.entity.Tienda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CheckoutServiceTest {

    @Mock private ClienteService clienteService;
    @Mock private CarritoService carritoService;
    @Mock private DireccionRepository direccionRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private VarianteRepository varianteRepository;
    @Mock private InventarioRepository inventarioRepository;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private ElementoPedidoRepository elementoPedidoRepository;
    @Mock private PedidoService pedidoService;
    @Mock private TenantService tenantService;
    @InjectMocks private CheckoutService checkoutService;

    private Cliente cliente;
    private Carrito carrito;
    private Producto producto;
    private ElementoCarrito elemento;
    private Direccion direccion;
    private CheckoutDTO dto;

    @BeforeEach
    void preparar() {
        Tienda tienda = Tienda.builder().id(1L).activo(true).build();
        cliente = Cliente.builder().id(100L).tienda(tienda).build();
        carrito = Carrito.builder().id(20L).tienda(tienda)
                .cliente(cliente).estado("ACTIVO").build();
        producto = Producto.builder().id(7L).tienda(tienda).nombre("Mouse")
                .precio(new BigDecimal("89.90")).activo(true).build();
        elemento = ElementoCarrito.builder().id(50L).carrito(carrito)
                .producto(producto).cantidad(2)
                .precioUnitario(new BigDecimal("80.00")).build();
        direccion = Direccion.builder().id(5L).cliente(cliente)
                .lineaDireccion("Av. Principal 123").ciudad("Lima").build();
        dto = new CheckoutDTO();
        dto.setDireccionId(5L);
    }

    @Test
    void checkoutExitosoRecalculaPrecioDescuentaStockYVaciaCarrito() {
        Inventario inventario = Inventario.builder().cantidad(5).build();
        PedidoRespuestaDTO respuesta = PedidoRespuestaDTO.builder().id(15L).build();

        prepararFlujoHastaDireccion();
        when(productoRepository.findByIdAndTiendaIdAndActivoTrue(7L, 1L))
                .thenReturn(Optional.of(producto));
        when(inventarioRepository.findWithLockByTiendaIdAndProductoIdAndVarianteIsNull(1L, 7L))
                .thenReturn(Optional.of(inventario));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocacion -> {
            Pedido pedido = invocacion.getArgument(0);
            pedido.setId(15L);
            return pedido;
        });
        when(pedidoService.convertirRespuesta(any(Pedido.class))).thenReturn(respuesta);

        PedidoRespuestaDTO resultado = checkoutService.realizarCheckout(dto);

        assertEquals(15L, resultado.getId());
        assertEquals(3, inventario.getCantidad());
        verify(elementoPedidoRepository).saveAll(argThat(elementos -> {
            var iterador = elementos.iterator();
            if (!iterador.hasNext()) return false;
            var primero = iterador.next();
            return !iterador.hasNext()
                    && primero.getPrecioUnitario()
                        .compareTo(new BigDecimal("89.90")) == 0;
        }));
        verify(carritoService).vaciarDespuesDeCheckout(carrito);
    }

    @Test
    void rechazaCarritoVacio() {
        when(tenantService.obtenerTenantId()).thenReturn(1L);
        when(clienteService.obtenerClienteActualSiExiste()).thenReturn(Optional.of(cliente));
        when(carritoService.obtenerCarritoActualParaCheckout()).thenReturn(carrito);
        when(carritoService.obtenerElementosParaCheckout(carrito)).thenReturn(List.of());

        assertThrows(ReglaNegocioException.class,
                () -> checkoutService.realizarCheckout(dto));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void rechazaDireccionAjena() {
        when(tenantService.obtenerTenantId()).thenReturn(1L);
        when(clienteService.obtenerClienteActualSiExiste()).thenReturn(Optional.of(cliente));
        when(carritoService.obtenerCarritoActualParaCheckout()).thenReturn(carrito);
        when(carritoService.obtenerElementosParaCheckout(carrito)).thenReturn(List.of(elemento));
        when(direccionRepository.findByIdAndClienteId(5L, 100L))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> checkoutService.realizarCheckout(dto));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void rechazaStockInsuficienteRevalidado() {
        prepararFlujoHastaDireccion();
        when(productoRepository.findByIdAndTiendaIdAndActivoTrue(7L, 1L))
                .thenReturn(Optional.of(producto));
        when(inventarioRepository.findWithLockByTiendaIdAndProductoIdAndVarianteIsNull(1L, 7L))
                .thenReturn(Optional.of(Inventario.builder().cantidad(1).build()));

        assertThrows(ReglaNegocioException.class,
                () -> checkoutService.realizarCheckout(dto));
        verify(pedidoRepository, never()).save(any());
        verify(carritoService, never()).vaciarDespuesDeCheckout(any());
    }

    private void prepararFlujoHastaDireccion() {
        when(tenantService.obtenerTenantId()).thenReturn(1L);
        when(clienteService.obtenerClienteActualSiExiste()).thenReturn(Optional.of(cliente));
        when(carritoService.obtenerCarritoActualParaCheckout()).thenReturn(carrito);
        when(carritoService.obtenerElementosParaCheckout(carrito)).thenReturn(List.of(elemento));
        when(direccionRepository.findByIdAndClienteId(5L, 100L))
                .thenReturn(Optional.of(direccion));
    }
}
