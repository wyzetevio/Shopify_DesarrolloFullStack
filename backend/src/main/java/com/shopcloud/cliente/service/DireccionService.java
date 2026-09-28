package com.shopcloud.cliente.service;

import com.shopcloud.cliente.dto.DireccionDTO;
import com.shopcloud.cliente.entity.Cliente;
import com.shopcloud.cliente.entity.Direccion;
import com.shopcloud.cliente.repository.DireccionRepository;

import com.shopcloud.exception.RecursoNoEncontradoException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DireccionService {

    private final DireccionRepository direccionRepository;
    private final ClienteService clienteService;

    @Transactional
    public DireccionDTO crearParaClienteActual(DireccionDTO dto) {

        Cliente cliente =
                clienteService.obtenerOCrearClienteActual();

        Direccion direccion =
                Direccion.builder()
                        .cliente(cliente)
                        .lineaDireccion(
                                dto.getLineaDireccion().trim()
                        )
                        .ciudad(
                                dto.getCiudad().trim()
                        )
                        .distrito(
                                limpiar(dto.getDistrito())
                        )
                        .departamento(
                                limpiar(dto.getDepartamento())
                        )
                        .codigoPostal(
                                limpiar(dto.getCodigoPostal())
                        )
                        .referencia(
                                limpiar(dto.getReferencia())
                        )
                        .build();

        return convertirDTO(
                direccionRepository.save(direccion)
        );
    }

    @Transactional
    public List<DireccionDTO> listarDelClienteActual() {

        Cliente cliente = clienteService.obtenerOCrearClienteActual();

        return direccionRepository
                .findByClienteId(cliente.getId())
                .stream()
                .map(this::convertirDTO)
                .toList();
    }

    @Transactional
    public DireccionDTO actualizarDelClienteActual(
            Long direccionId,
            DireccionDTO dto
    ) {

        Cliente cliente = clienteService.obtenerOCrearClienteActual();
        Direccion direccion = buscarDireccion(cliente.getId(), direccionId);

        copiarDatos(dto, direccion);

        return convertirDTO(direccionRepository.save(direccion));
    }

    @Transactional
    public void eliminarDelClienteActual(Long direccionId) {

        Cliente cliente = clienteService.obtenerOCrearClienteActual();
        Direccion direccion = buscarDireccion(cliente.getId(), direccionId);

        direccionRepository.delete(direccion);
    }

    @Transactional(readOnly = true)
    public List<DireccionDTO> listar(
            Long clienteId
    ) {

        /*
         * Antes de consultar las direcciones,
         * comprobamos que el cliente realmente
         * pertenece al tenant actual.
         */
        clienteService.buscarCliente(clienteId);

        return direccionRepository
                .findByClienteId(clienteId)
                .stream()
                .map(this::convertirDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public DireccionDTO obtener(
            Long clienteId,
            Long direccionId
    ) {

        clienteService.buscarCliente(clienteId);

        return convertirDTO(
                buscarDireccion(
                        clienteId,
                        direccionId
                )
        );
    }

    private Direccion buscarDireccion(
            Long clienteId,
            Long direccionId
    ) {

        return direccionRepository
                .findByIdAndClienteId(
                        direccionId,
                        clienteId
                )
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Dirección no encontrada"
                        )
                );
    }

    private DireccionDTO convertirDTO(
            Direccion direccion
    ) {

        return DireccionDTO.builder()
                .id(direccion.getId())
                .lineaDireccion(
                        direccion.getLineaDireccion()
                )
                .ciudad(direccion.getCiudad())
                .distrito(direccion.getDistrito())
                .departamento(
                        direccion.getDepartamento()
                )
                .codigoPostal(
                        direccion.getCodigoPostal()
                )
                .referencia(
                        direccion.getReferencia()
                )
                .build();
    }

    private void copiarDatos(
            DireccionDTO dto,
            Direccion direccion
    ) {

        direccion.setLineaDireccion(dto.getLineaDireccion().trim());
        direccion.setCiudad(dto.getCiudad().trim());
        direccion.setDistrito(limpiar(dto.getDistrito()));
        direccion.setDepartamento(limpiar(dto.getDepartamento()));
        direccion.setCodigoPostal(limpiar(dto.getCodigoPostal()));
        direccion.setReferencia(limpiar(dto.getReferencia()));
    }

    private String limpiar(String valor) {

        if (valor == null) {
            return null;
        }

        String resultado = valor.trim();

        return resultado.isEmpty()
                ? null
                : resultado;
    }
}
