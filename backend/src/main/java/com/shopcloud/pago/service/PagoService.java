package com.shopcloud.pago.service;

import com.shopcloud.cliente.entity.Cliente;
import com.shopcloud.cliente.service.ClienteService;
import com.shopcloud.exception.RecursoNoEncontradoException;
import com.shopcloud.exception.ReglaNegocioException;
import com.shopcloud.pago.dto.MetodoPagoDTO;
import com.shopcloud.pago.dto.PagoDTO;
import com.shopcloud.pago.dto.TransaccionDTO;
import com.shopcloud.pago.entity.MetodoPago;
import com.shopcloud.pago.entity.Transaccion;
import com.shopcloud.pago.repository.MetodoPagoRepository;
import com.shopcloud.pago.repository.TransaccionRepository;
import com.shopcloud.pedido.entity.Pedido;
import com.shopcloud.pedido.repository.PedidoRepository;
import com.shopcloud.tenant.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PagoService {

    private static final String PENDIENTE = "PENDIENTE";
    private static final String PAGADO = "PAGADO";
    private static final String APROBADO = "APROBADO";

    private final MetodoPagoRepository metodoPagoRepository;
    private final TransaccionRepository transaccionRepository;
    private final PedidoRepository pedidoRepository;
    private final ClienteService clienteService;
    private final TenantService tenantService;
    private final PagoSimuladoService pagoSimuladoService;

    @Transactional(readOnly = true)
    public List<MetodoPagoDTO> listarMetodosActivos() {
        return metodoPagoRepository
                .findByTiendaIdAndActivoTrueOrderByNombre(
                        tenantService.obtenerTenantId())
                .stream()
                .map(this::convertirMetodoDTO)
                .toList();
    }

    @Transactional
    public TransaccionDTO procesar(PagoDTO dto) {
        Long tiendaId = tenantService.obtenerTenantId();
        Cliente cliente = clienteService.obtenerClienteActualSiExiste()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Pedido no encontrado"));

        Pedido pedido = pedidoRepository
                .findWithLockByIdAndClienteIdAndTiendaId(
                        dto.getPedidoId(), cliente.getId(), tiendaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Pedido no encontrado"));

        if (!PENDIENTE.equals(pedido.getEstado())) {
            throw new ReglaNegocioException(
                    "Solamente un pedido PENDIENTE puede pagarse");
        }
        if (transaccionRepository.existsByPedidoIdAndEstado(
                pedido.getId(), APROBADO)) {
            throw new ReglaNegocioException("El pedido ya tiene un pago aprobado");
        }

        MetodoPago metodoPago = metodoPagoRepository
                .findByIdAndTiendaIdAndActivoTrue(dto.getMetodoPagoId(), tiendaId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Método de pago activo no encontrado en la tienda"));

        String resultado = pagoSimuladoService.procesar(pedido, metodoPago);
        if (!APROBADO.equals(resultado) && !"RECHAZADO".equals(resultado)) {
            throw new ReglaNegocioException(
                    "Resultado inválido del procesador de pago simulado");
        }

        Transaccion transaccion = transaccionRepository.save(
                Transaccion.builder()
                        .pedido(pedido)
                        .metodoPago(metodoPago)
                        .monto(pedido.getTotal())
                        .estado(resultado)
                        .referenciaTransaccion(generarReferencia())
                        .build());

        if (APROBADO.equals(resultado)) {
            pedido.setEstado(PAGADO);
            pedidoRepository.save(pedido);
        }

        return convertirTransaccionDTO(transaccion);
    }

    @Transactional(readOnly = true)
    public List<TransaccionDTO> listarTransaccionesPropias() {
        Long tiendaId = tenantService.obtenerTenantId();
        Optional<Cliente> cliente = clienteService.obtenerClienteActualSiExiste();
        if (cliente.isEmpty()) return List.of();

        return transaccionRepository
                .findByPedidoClienteIdAndPedidoTiendaIdOrderByCreadoEnDesc(
                        cliente.get().getId(), tiendaId)
                .stream()
                .map(this::convertirTransaccionDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public TransaccionDTO obtenerTransaccionPropia(Long transaccionId) {
        Long tiendaId = tenantService.obtenerTenantId();
        Cliente cliente = clienteService.obtenerClienteActualSiExiste()
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Transacción no encontrada"));

        return transaccionRepository
                .findByIdAndPedidoClienteIdAndPedidoTiendaId(
                        transaccionId, cliente.getId(), tiendaId)
                .map(this::convertirTransaccionDTO)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Transacción no encontrada"));
    }

    private String generarReferencia() {
        return "SIM-" + UUID.randomUUID().toString().toUpperCase();
    }

    private MetodoPagoDTO convertirMetodoDTO(MetodoPago metodo) {
        return MetodoPagoDTO.builder()
                .id(metodo.getId())
                .nombre(metodo.getNombre())
                .tipo(metodo.getTipo())
                .build();
    }

    private TransaccionDTO convertirTransaccionDTO(Transaccion transaccion) {
        return TransaccionDTO.builder()
                .id(transaccion.getId())
                .pedidoId(transaccion.getPedido().getId())
                .metodoPagoId(transaccion.getMetodoPago().getId())
                .metodoPagoNombre(transaccion.getMetodoPago().getNombre())
                .monto(transaccion.getMonto())
                .estado(transaccion.getEstado())
                .referenciaTransaccion(transaccion.getReferenciaTransaccion())
                .creadoEn(transaccion.getCreadoEn())
                .build();
    }
}
