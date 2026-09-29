package com.shopcloud.pedido.service;

import com.shopcloud.cliente.entity.Cliente;
import com.shopcloud.cliente.service.ClienteService;
import com.shopcloud.exception.RecursoNoEncontradoException;
import com.shopcloud.pedido.repository.ElementoPedidoRepository;
import com.shopcloud.pedido.repository.PedidoRepository;
import com.shopcloud.tenant.TenantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {
    @Mock private PedidoRepository pedidoRepository;
    @Mock private ElementoPedidoRepository elementoPedidoRepository;
    @Mock private ClienteService clienteService;
    @Mock private TenantService tenantService;
    @InjectMocks private PedidoService pedidoService;

    @Test
    void noPermiteConsultarPedidoAjenoONoPertenecienteAlTenant() {
        Cliente cliente = Cliente.builder().id(100L).build();
        when(tenantService.obtenerTenantId()).thenReturn(1L);
        when(clienteService.obtenerClienteActualSiExiste()).thenReturn(Optional.of(cliente));
        when(pedidoRepository.findByIdAndClienteIdAndTiendaId(99L, 100L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class,
                () -> pedidoService.obtenerDelClienteActual(99L));
        verify(pedidoRepository).findByIdAndClienteIdAndTiendaId(99L, 100L, 1L);
    }
}
