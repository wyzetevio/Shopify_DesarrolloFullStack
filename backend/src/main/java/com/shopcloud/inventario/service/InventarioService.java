package com.shopcloud.inventario.service;

import com.shopcloud.catalogo.entity.Producto;
import com.shopcloud.catalogo.entity.Variante;
import com.shopcloud.catalogo.repository.ProductoRepository;
import com.shopcloud.catalogo.repository.VarianteRepository;

import com.shopcloud.exception.RecursoNoEncontradoException;
import com.shopcloud.exception.ReglaNegocioException;

import com.shopcloud.inventario.dto.ActualizarInventarioDTO;
import com.shopcloud.inventario.dto.CrearInventarioDTO;
import com.shopcloud.inventario.dto.InventarioRespuestaDTO;

import com.shopcloud.inventario.entity.Inventario;
import com.shopcloud.inventario.repository.InventarioRepository;

import com.shopcloud.tenant.TenantService;
import com.shopcloud.tienda.entity.Tienda;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventarioService {

    private final InventarioRepository inventarioRepository;
    private final ProductoRepository productoRepository;
    private final VarianteRepository varianteRepository;
    private final TenantService tenantService;

    @Transactional
    public InventarioRespuestaDTO crear(
            CrearInventarioDTO dto
    ) {

        Long tiendaId =
                tenantService.obtenerTenantId();

        Tienda tienda =
                tenantService.obtenerTiendaActual();

        Producto producto =
                buscarProducto(
                        dto.getProductoId(),
                        tiendaId
                );

        Variante variante =
                obtenerVariante(
                        producto,
                        dto.getVarianteId()
                );

        validarDuplicado(
                tiendaId,
                producto.getId(),
                dto.getVarianteId()
        );

        Inventario inventario =
                Inventario.builder()
                        .tienda(tienda)
                        .producto(producto)
                        .variante(variante)
                        .cantidad(dto.getCantidad())
                        .cantidadMinima(
                                dto.getCantidadMinima()
                        )
                        .build();

        return convertirDTO(
                inventarioRepository.save(inventario)
        );
    }

    @Transactional(readOnly = true)
    public List<InventarioRespuestaDTO> listar() {

        Long tiendaId =
                tenantService.obtenerTenantId();

        return inventarioRepository
                .findByTiendaId(tiendaId)
                .stream()
                .map(this::convertirDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public InventarioRespuestaDTO obtener(
            Long id
    ) {

        return convertirDTO(
                buscarInventario(id)
        );
    }

    @Transactional
    public InventarioRespuestaDTO actualizar(
            Long id,
            ActualizarInventarioDTO dto
    ) {

        Inventario inventario =
                buscarInventario(id);

        inventario.setCantidad(
                dto.getCantidad()
        );

        inventario.setCantidadMinima(
                dto.getCantidadMinima()
        );

        return convertirDTO(
                inventarioRepository.save(inventario)
        );
    }

    @Transactional(readOnly = true)
    public List<InventarioRespuestaDTO>
    listarStockBajo() {

        Long tiendaId =
                tenantService.obtenerTenantId();

        return inventarioRepository
                .findByTiendaId(tiendaId)
                .stream()
                .filter(inventario ->
                        inventario.getCantidad()
                                <=
                        inventario.getCantidadMinima()
                )
                .map(this::convertirDTO)
                .toList();
    }

    private Inventario buscarInventario(
            Long id
    ) {

        Long tiendaId =
                tenantService.obtenerTenantId();

        return inventarioRepository
                .findByIdAndTiendaId(
                        id,
                        tiendaId
                )
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Registro de inventario no encontrado"
                        )
                );
    }

    private Producto buscarProducto(
            Long productoId,
            Long tiendaId
    ) {

        return productoRepository
                .findByIdAndTiendaId(
                        productoId,
                        tiendaId
                )
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Producto no encontrado"
                        )
                );
    }

    private Variante obtenerVariante(
            Producto producto,
            Long varianteId
    ) {

        if (varianteId == null) {
            return null;
        }

        return varianteRepository
                .findByIdAndProductoId(
                        varianteId,
                        producto.getId()
                )
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "La variante no pertenece al producto"
                        )
                );
    }

    private void validarDuplicado(
            Long tiendaId,
            Long productoId,
            Long varianteId
    ) {

        boolean existe;

        if (varianteId == null) {

            existe =
                    inventarioRepository
                    .existsByTiendaIdAndProductoIdAndVarianteIsNull(
                            tiendaId,
                            productoId
                    );

        } else {

            existe =
                    inventarioRepository
                    .existsByTiendaIdAndProductoIdAndVarianteId(
                            tiendaId,
                            productoId,
                            varianteId
                    );
        }

        if (existe) {
            throw new ReglaNegocioException(
                    "Ya existe un registro de inventario " +
                    "para este producto o variante"
            );
        }
    }

    private InventarioRespuestaDTO convertirDTO(
            Inventario inventario
    ) {

        Variante variante =
                inventario.getVariante();

        return InventarioRespuestaDTO.builder()
                .id(inventario.getId())
                .productoId(
                        inventario.getProducto().getId()
                )
                .productoNombre(
                        inventario.getProducto().getNombre()
                )
                .varianteId(
                        variante != null
                                ? variante.getId()
                                : null
                )
                .varianteNombre(
                        variante != null
                                ? variante.getNombre()
                                : null
                )
                .cantidad(inventario.getCantidad())
                .cantidadMinima(
                        inventario.getCantidadMinima()
                )
                .stockBajo(
                        inventario.getCantidad()
                                <=
                        inventario.getCantidadMinima()
                )
                .actualizadoEn(
                        inventario.getActualizadoEn()
                )
                .build();
    }
}