package com.shopcloud.cliente.service;

import com.shopcloud.cliente.dto.ClienteDTO;
import com.shopcloud.cliente.entity.Cliente;
import com.shopcloud.cliente.repository.ClienteRepository;

import com.shopcloud.exception.RecursoNoEncontradoException;
import com.shopcloud.exception.AccesoDenegadoException;

import com.shopcloud.security.UsuarioPrincipal;

import com.shopcloud.tenant.TenantService;
import com.shopcloud.tienda.entity.Tienda;
import com.shopcloud.usuario.entity.Usuario;
import com.shopcloud.usuario.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final TenantService tenantService;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public Cliente obtenerOCrearClienteActual() {

        Long tiendaId = tenantService.obtenerTenantId();
        Long usuarioId = obtenerUsuarioIdAutenticado();

        return clienteRepository
                .findByUsuarioIdAndTiendaId(usuarioId, tiendaId)
                .orElseGet(() -> crearClienteActual(usuarioId));
    }

    @Transactional(readOnly = true)
    public List<ClienteDTO> listar() {

        Long tiendaId =
                tenantService.obtenerTenantId();

        return clienteRepository
                .findByTiendaId(tiendaId)
                .stream()
                .map(this::convertirDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteDTO obtener(Long id) {

        return convertirDTO(
                buscarCliente(id)
        );
    }

    @Transactional(readOnly = true)
    public Cliente buscarCliente(Long id) {

        Long tiendaId =
                tenantService.obtenerTenantId();

        return clienteRepository
                .findByIdAndTiendaId(
                        id,
                        tiendaId
                )
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Cliente no encontrado"
                        )
                );
    }

    private Cliente crearClienteActual(Long usuarioId) {

        Usuario usuario = usuarioRepository
                .findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Usuario autenticado no encontrado"
                ));

        Tienda tienda = tenantService.obtenerTiendaActual();

        return clienteRepository.save(
                Cliente.builder()
                        .tienda(tienda)
                        .usuario(usuario)
                        .nombre(usuario.getNombre())
                        .correoElectronico(usuario.getCorreoElectronico())
                        .build()
        );
    }

    private Long obtenerUsuarioIdAutenticado() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UsuarioPrincipal principal)) {
            throw new AccesoDenegadoException("Usuario no autenticado");
        }

        return principal.getId();
    }

    private ClienteDTO convertirDTO(
            Cliente cliente
    ) {

        return ClienteDTO.builder()
                .id(cliente.getId())
                .nombre(cliente.getNombre())
                .correoElectronico(
                        cliente.getCorreoElectronico()
                )
                .telefono(cliente.getTelefono())
                .build();
    }

}
