package com.shopcloud.pedido.service;

import com.shopcloud.catalogo.entity.Variante;
import com.shopcloud.cliente.entity.Cliente;
import com.shopcloud.cliente.entity.Direccion;
import com.shopcloud.cliente.service.ClienteService;
import com.shopcloud.exception.RecursoNoEncontradoException;
import com.shopcloud.pedido.dto.ElementoPedidoDTO;
import com.shopcloud.pedido.dto.PedidoDTO;
import com.shopcloud.pedido.dto.PedidoRespuestaDTO;
import com.shopcloud.pedido.entity.ElementoPedido;
import com.shopcloud.pedido.entity.Pedido;
import com.shopcloud.pedido.repository.ElementoPedidoRepository;
import com.shopcloud.pedido.repository.PedidoRepository;
import com.shopcloud.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ElementoPedidoRepository elementoPedidoRepository;
    private final ClienteService clienteService;
    private final TenantService tenantService;

    @Transactional(readOnly = true)
    public List<PedidoDTO> listarDelClienteActual() {
        Long tiendaId = tenantService.obtenerTenantId();
        Optional<Cliente> cliente = clienteService.obtenerClienteActualSiExiste();
        if (cliente.isEmpty()) return List.of();

        return pedidoRepository
                .findByClienteIdAndTiendaIdOrderByCreadoEnDesc(
                        cliente.get().getId(), tiendaId)
                .stream()
                .map(this::convertirResumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoRespuestaDTO obtenerDelClienteActual(Long pedidoId) {
        Long tiendaId = tenantService.obtenerTenantId();
        Cliente cliente = clienteService.obtenerClienteActualSiExiste()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Pedido no encontrado"));

        Pedido pedido = pedidoRepository
                .findByIdAndClienteIdAndTiendaId(
                        pedidoId, cliente.getId(), tiendaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Pedido no encontrado"));

        return convertirRespuesta(pedido);
    }

    @Transactional(readOnly = true)
    public PedidoRespuestaDTO convertirRespuesta(Pedido pedido) {
        Direccion direccion = pedido.getDireccion();
        List<ElementoPedidoDTO> elementos = elementoPedidoRepository
                .findByPedidoIdOrderById(pedido.getId())
                .stream()
                .map(this::convertirElemento)
                .toList();

        return PedidoRespuestaDTO.builder()
                .id(pedido.getId())
                .estado(pedido.getEstado())
                .subtotal(pedido.getSubtotal())
                .montoDescuento(pedido.getMontoDescuento())
                .total(pedido.getTotal())
                .creadoEn(pedido.getCreadoEn())
                .direccionId(direccion.getId())
                .lineaDireccion(direccion.getLineaDireccion())
                .ciudad(direccion.getCiudad())
                .elementos(elementos)
                .build();
    }

    private PedidoDTO convertirResumen(Pedido pedido) {
        return PedidoDTO.builder()
                .id(pedido.getId())
                .estado(pedido.getEstado())
                .total(pedido.getTotal())
                .creadoEn(pedido.getCreadoEn())
                .build();
    }

    private ElementoPedidoDTO convertirElemento(ElementoPedido elemento) {
        Variante variante = elemento.getVariante();
        return ElementoPedidoDTO.builder()
                .id(elemento.getId())
                .productoId(elemento.getProducto().getId())
                .nombreProducto(elemento.getNombreProducto())
                .varianteId(variante != null ? variante.getId() : null)
                .cantidad(elemento.getCantidad())
                .precioUnitario(elemento.getPrecioUnitario())
                .subtotal(elemento.getSubtotal())
                .build();
    }
}
