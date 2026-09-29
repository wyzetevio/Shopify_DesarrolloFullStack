package com.shopcloud.pago.service;

import com.shopcloud.cliente.entity.Cliente;
import com.shopcloud.cliente.service.ClienteService;
import com.shopcloud.exception.RecursoNoEncontradoException;
import com.shopcloud.exception.ReglaNegocioException;
import com.shopcloud.pago.dto.PagoDTO;
import com.shopcloud.pago.dto.TransaccionDTO;
import com.shopcloud.pago.entity.MetodoPago;
import com.shopcloud.pago.entity.Transaccion;
import com.shopcloud.pago.repository.MetodoPagoRepository;
import com.shopcloud.pago.repository.TransaccionRepository;
import com.shopcloud.pedido.entity.Pedido;
import com.shopcloud.pedido.repository.PedidoRepository;
import com.shopcloud.tenant.TenantService;
import com.shopcloud.tienda.entity.Tienda;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class PagoServiceTest {

    @Mock private MetodoPagoRepository metodoPagoRepository;
    @Mock private TransaccionRepository transaccionRepository;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private ClienteService clienteService;
    @Mock private TenantService tenantService;
    @Mock private PagoSimuladoService pagoSimuladoService;
    @InjectMocks private PagoService pagoService;

    private Cliente cliente;
    private Pedido pedido;
    private MetodoPago metodo;
    private PagoDTO dto;

    @BeforeEach
    void preparar() {
        Tienda tienda = Tienda.builder().id(1L).activo(true).build();
        cliente = Cliente.builder().id(100L).tienda(tienda).build();
        pedido = Pedido.builder().id(15L).tienda(tienda).cliente(cliente)
                .estado("PENDIENTE").total(new BigDecimal("179.80")).build();
        metodo = MetodoPago.builder().id(3L).tienda(tienda)
                .nombre("Método de prueba").tipo("SIMULADO").activo(true).build();
        dto = new PagoDTO();
        dto.setPedidoId(15L);
        dto.setMetodoPagoId(3L);
    }

    @Test
    void pagoAprobadoUsaTotalDelPedidoYLoMarcaPagado() {
        prepararPedidoPendiente();
        when(metodoPagoRepository.findByIdAndTiendaIdAndActivoTrue(3L, 1L))
                .thenReturn(Optional.of(metodo));
        when(pagoSimuladoService.procesar(pedido, metodo)).thenReturn("APROBADO");
        when(transaccionRepository.save(any(Transaccion.class))).thenAnswer(invocacion -> {
            Transaccion transaccion = invocacion.getArgument(0);
            transaccion.setId(8L);
            return transaccion;
        });

        TransaccionDTO resultado = pagoService.procesar(dto);

        assertEquals("APROBADO", resultado.getEstado());
        assertEquals(new BigDecimal("179.80"), resultado.getMonto());
        assertEquals("PAGADO", pedido.getEstado());

        ArgumentCaptor<Transaccion> captor = ArgumentCaptor.forClass(Transaccion.class);
        verify(transaccionRepository).save(captor.capture());
        assertEquals(pedido.getTotal(), captor.getValue().getMonto());
        verify(pedidoRepository).save(pedido);
    }

    @Test
    void segundoPagoEsRechazado() {
        pedido.setEstado("PAGADO");
        when(tenantService.obtenerTenantId()).thenReturn(1L);
        when(clienteService.obtenerClienteActualSiExiste()).thenReturn(Optional.of(cliente));
        when(pedidoRepository.findWithLockByIdAndClienteIdAndTiendaId(15L, 100L, 1L))
                .thenReturn(Optional.of(pedido));

        assertThrows(ReglaNegocioException.class, () -> pagoService.procesar(dto));
        verify(transaccionRepository, never()).save(any());
    }

    @Test
    void pedidoAjenoUOtroTenantNoPuedePagarse() {
        when(tenantService.obtenerTenantId()).thenReturn(1L);
        when(clienteService.obtenerClienteActualSiExiste()).thenReturn(Optional.of(cliente));
        when(pedidoRepository.findWithLockByIdAndClienteIdAndTiendaId(15L, 100L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> pagoService.procesar(dto));
        verify(transaccionRepository, never()).save(any());
    }

    @Test
    void metodoInvalidoOInactivoEsRechazado() {
        prepararPedidoPendiente();
        when(metodoPagoRepository.findByIdAndTiendaIdAndActivoTrue(3L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> pagoService.procesar(dto));
        verify(transaccionRepository, never()).save(any());
    }

    @Test
    void pagoRechazadoMantienePedidoPendiente() {
        prepararPedidoPendiente();
        when(metodoPagoRepository.findByIdAndTiendaIdAndActivoTrue(3L, 1L))
                .thenReturn(Optional.of(metodo));
        when(pagoSimuladoService.procesar(pedido, metodo)).thenReturn("RECHAZADO");
        when(transaccionRepository.save(any(Transaccion.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        TransaccionDTO resultado = pagoService.procesar(dto);

        assertEquals("RECHAZADO", resultado.getEstado());
        assertEquals("PENDIENTE", pedido.getEstado());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    void listaSolamenteTransaccionesPropiasDelTenant() {
        Transaccion transaccion = transaccion("APROBADO");
        when(tenantService.obtenerTenantId()).thenReturn(1L);
        when(clienteService.obtenerClienteActualSiExiste()).thenReturn(Optional.of(cliente));
        when(transaccionRepository
                .findByPedidoClienteIdAndPedidoTiendaIdOrderByCreadoEnDesc(100L, 1L))
                .thenReturn(List.of(transaccion));

        List<TransaccionDTO> resultado = pagoService.listarTransaccionesPropias();

        assertEquals(1, resultado.size());
        assertEquals(8L, resultado.getFirst().getId());
    }

    @Test
    void transaccionAjenaUOtroTenantNoPuedeConsultarse() {
        when(tenantService.obtenerTenantId()).thenReturn(1L);
        when(clienteService.obtenerClienteActualSiExiste()).thenReturn(Optional.of(cliente));
        when(transaccionRepository
                .findByIdAndPedidoClienteIdAndPedidoTiendaId(8L, 100L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> pagoService.obtenerTransaccionPropia(8L));
    }

    private void prepararPedidoPendiente() {
        when(tenantService.obtenerTenantId()).thenReturn(1L);
        when(clienteService.obtenerClienteActualSiExiste()).thenReturn(Optional.of(cliente));
        when(pedidoRepository.findWithLockByIdAndClienteIdAndTiendaId(15L, 100L, 1L))
                .thenReturn(Optional.of(pedido));
        when(transaccionRepository.existsByPedidoIdAndEstado(15L, "APROBADO"))
                .thenReturn(false);
    }

    private Transaccion transaccion(String estado) {
        return Transaccion.builder().id(8L).pedido(pedido).metodoPago(metodo)
                .monto(pedido.getTotal()).estado(estado)
                .referenciaTransaccion("SIM-PRUEBA").build();
    }
}
