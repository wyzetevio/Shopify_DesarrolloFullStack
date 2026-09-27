package com.shopcloud.tienda.service;

import com.shopcloud.tienda.dto.ActualizarTiendaDTO;
import com.shopcloud.tienda.dto.CrearTiendaDTO;
import com.shopcloud.tienda.dto.TiendaRespuestaDTO;

import com.shopcloud.tienda.entity.Tienda;
import com.shopcloud.tienda.repository.TiendaRepository;

import com.shopcloud.usuario.entity.Rol;
import com.shopcloud.usuario.entity.Usuario;
import com.shopcloud.usuario.repository.RolRepository;
import com.shopcloud.usuario.repository.UsuarioRepository;

import com.shopcloud.util.SlugUtil;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TiendaService {

    private final TiendaRepository tiendaRepository;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    @Transactional
    public TiendaRespuestaDTO crear(
            CrearTiendaDTO dto,
            Long usuarioId
    ) {

        Usuario usuario = usuarioRepository
                .findById(usuarioId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Usuario no encontrado"
                        )
                );

        String slugBase;

        if (dto.getSlug() != null &&
                !dto.getSlug().isBlank()) {

            slugBase = SlugUtil.generar(
                    dto.getSlug()
            );

        } else {

            slugBase = SlugUtil.generar(
                    dto.getNombre()
            );
        }

        if (slugBase.isBlank()) {
            throw new IllegalArgumentException(
                    "No se pudo generar un slug válido"
            );
        }

        String slug = generarSlugUnico(slugBase);

        Tienda tienda = Tienda.builder()
                .propietario(usuario)
                .nombre(dto.getNombre().trim())
                .slug(slug)
                .descripcion(dto.getDescripcion())
                .urlLogo(dto.getUrlLogo())
                .urlBanner(dto.getUrlBanner())
                .colorPrimario(dto.getColorPrimario())
                .correoContacto(dto.getCorreoContacto())
                .telefonoContacto(dto.getTelefonoContacto())
                .activo(true)
                .build();

        Tienda tiendaGuardada =
                tiendaRepository.save(tienda);

        asignarRolPropietario(usuario);

        return convertirDTO(tiendaGuardada);
    }

    @Transactional(readOnly = true)
    public List<TiendaRespuestaDTO> listarMisTiendas(
            Long usuarioId
    ) {

        return tiendaRepository
                .findByPropietarioId(usuarioId)
                .stream()
                .map(this::convertirDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public TiendaRespuestaDTO obtenerMiTienda(
            Long tiendaId,
            Long usuarioId
    ) {

        Tienda tienda =
                buscarTiendaDelPropietario(
                        tiendaId,
                        usuarioId
                );

        return convertirDTO(tienda);
    }

    @Transactional
    public TiendaRespuestaDTO actualizar(
            Long tiendaId,
            ActualizarTiendaDTO dto,
            Long usuarioId
    ) {

        Tienda tienda =
                buscarTiendaDelPropietario(
                        tiendaId,
                        usuarioId
                );

        tienda.setNombre(
                dto.getNombre().trim()
        );

        tienda.setDescripcion(
                dto.getDescripcion()
        );

        tienda.setUrlLogo(
                dto.getUrlLogo()
        );

        tienda.setUrlBanner(
                dto.getUrlBanner()
        );

        tienda.setColorPrimario(
                dto.getColorPrimario()
        );

        tienda.setCorreoContacto(
                dto.getCorreoContacto()
        );

        tienda.setTelefonoContacto(
                dto.getTelefonoContacto()
        );

        return convertirDTO(
                tiendaRepository.save(tienda)
        );
    }

    @Transactional
    public void desactivar(
            Long tiendaId,
            Long usuarioId
    ) {

        Tienda tienda =
                buscarTiendaDelPropietario(
                        tiendaId,
                        usuarioId
                );

        tienda.setActivo(false);

        tiendaRepository.save(tienda);
    }

    private Tienda buscarTiendaDelPropietario(
            Long tiendaId,
            Long usuarioId
    ) {

        return tiendaRepository
                .findByIdAndPropietarioId(
                        tiendaId,
                        usuarioId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Tienda no encontrada o no pertenece al usuario"
                        )
                );
    }

    private void asignarRolPropietario(
            Usuario usuario
    ) {

        boolean yaEsPropietario =
                usuario.getRoles()
                        .stream()
                        .anyMatch(rol ->
                                rol.getNombre()
                                        .equals(
                                                "PROPIETARIO_TIENDA"
                                        )
                        );

        if (yaEsPropietario) {
            return;
        }

        Rol rolPropietario =
                rolRepository
                        .findByNombre(
                                "PROPIETARIO_TIENDA"
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "El rol PROPIETARIO_TIENDA no existe"
                                )
                        );

        usuario.getRoles().add(
                rolPropietario
        );

        usuarioRepository.save(usuario);
    }

    private String generarSlugUnico(
            String slugBase
    ) {

        String slug = slugBase;

        int contador = 2;

        while (tiendaRepository.existsBySlug(slug)) {

            slug = slugBase + "-" + contador;

            contador++;
        }

        return slug;
    }

    private TiendaRespuestaDTO convertirDTO(
            Tienda tienda
    ) {

        return TiendaRespuestaDTO.builder()
                .id(tienda.getId())
                .nombre(tienda.getNombre())
                .slug(tienda.getSlug())
                .descripcion(
                        tienda.getDescripcion()
                )
                .urlLogo(
                        tienda.getUrlLogo()
                )
                .urlBanner(
                        tienda.getUrlBanner()
                )
                .colorPrimario(
                        tienda.getColorPrimario()
                )
                .correoContacto(
                        tienda.getCorreoContacto()
                )
                .telefonoContacto(
                        tienda.getTelefonoContacto()
                )
                .activo(
                        tienda.getActivo()
                )
                .creadoEn(
                        tienda.getCreadoEn()
                )
                .build();
    }
}