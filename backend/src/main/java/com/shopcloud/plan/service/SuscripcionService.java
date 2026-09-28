package com.shopcloud.plan.service;

import com.shopcloud.exception.RecursoNoEncontradoException;
import com.shopcloud.exception.ReglaNegocioException;

import com.shopcloud.plan.dto.CrearSuscripcionDTO;
import com.shopcloud.plan.dto.SuscripcionRespuestaDTO;

import com.shopcloud.plan.entity.Plan;
import com.shopcloud.plan.entity.Suscripcion;

import com.shopcloud.plan.repository.PlanRepository;
import com.shopcloud.plan.repository.SuscripcionRepository;

import com.shopcloud.tenant.TenantService;
import com.shopcloud.tienda.entity.Tienda;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SuscripcionService {

    private final SuscripcionRepository suscripcionRepository;
    private final PlanRepository planRepository;
    private final TenantService tenantService;

    @Transactional
    public SuscripcionRespuestaDTO crear(
            CrearSuscripcionDTO dto
    ) {

        Long tiendaId =
                tenantService.obtenerTenantId();

        validarFechas(
                dto.getFechaInicio(),
                dto.getFechaFin()
        );

        Plan plan = planRepository
                .findByIdAndActivoTrue(
                        dto.getPlanId()
                )
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Plan activo no encontrado"
                        )
                );

        if (suscripcionRepository
                .existsByTiendaIdAndEstado(
                        tiendaId,
                        "ACTIVO"
                )) {

            throw new ReglaNegocioException(
                    "La tienda ya tiene una suscripción activa"
            );
        }

        Tienda tienda =
                tenantService.obtenerTiendaActual();

        Suscripcion suscripcion =
                Suscripcion.builder()
                        .tienda(tienda)
                        .plan(plan)
                        .estado("ACTIVO")
                        .fechaInicio(
                                dto.getFechaInicio()
                        )
                        .fechaFin(
                                dto.getFechaFin()
                        )
                        .build();

        return convertirDTO(
                suscripcionRepository.save(
                        suscripcion
                )
        );
    }

    @Transactional(readOnly = true)
    public List<SuscripcionRespuestaDTO> listar() {

        Long tiendaId =
                tenantService.obtenerTenantId();

        return suscripcionRepository
                .findByTiendaId(tiendaId)
                .stream()
                .map(this::convertirDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public SuscripcionRespuestaDTO obtenerActiva() {

        Long tiendaId =
                tenantService.obtenerTenantId();

        Suscripcion suscripcion =
                suscripcionRepository
                        .findFirstByTiendaIdAndEstadoOrderByCreadoEnDesc(
                                tiendaId,
                                "ACTIVO"
                        )
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "La tienda no tiene una suscripción activa"
                                )
                        );

        return convertirDTO(suscripcion);
    }

    @Transactional
    public SuscripcionRespuestaDTO cambiarPlan(
            CrearSuscripcionDTO dto
    ) {

        Long tiendaId =
                tenantService.obtenerTenantId();

        validarFechas(
                dto.getFechaInicio(),
                dto.getFechaFin()
        );

        Plan nuevoPlan =
                planRepository
                        .findByIdAndActivoTrue(
                                dto.getPlanId()
                        )
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "Plan activo no encontrado"
                                )
                        );

        suscripcionRepository
                .findFirstByTiendaIdAndEstadoOrderByCreadoEnDesc(
                        tiendaId,
                        "ACTIVO"
                )
                .ifPresent(actual -> {

                    actual.setEstado("CANCELADO");

                    if (actual.getFechaFin() == null) {
                        actual.setFechaFin(
                                LocalDate.now()
                        );
                    }

                    suscripcionRepository.save(
                            actual
                    );
                });

        Tienda tienda =
                tenantService.obtenerTiendaActual();

        Suscripcion nueva =
                Suscripcion.builder()
                        .tienda(tienda)
                        .plan(nuevoPlan)
                        .estado("ACTIVO")
                        .fechaInicio(
                                dto.getFechaInicio()
                        )
                        .fechaFin(
                                dto.getFechaFin()
                        )
                        .build();

        return convertirDTO(
                suscripcionRepository.save(nueva)
        );
    }

    @Transactional
    public void cancelar() {

        Long tiendaId =
                tenantService.obtenerTenantId();

        Suscripcion suscripcion =
                suscripcionRepository
                        .findFirstByTiendaIdAndEstadoOrderByCreadoEnDesc(
                                tiendaId,
                                "ACTIVO"
                        )
                        .orElseThrow(() ->
                                new RecursoNoEncontradoException(
                                        "La tienda no tiene una suscripción activa"
                                )
                        );

        suscripcion.setEstado("CANCELADO");

        if (suscripcion.getFechaFin() == null) {
            suscripcion.setFechaFin(
                    LocalDate.now()
            );
        }

        suscripcionRepository.save(
                suscripcion
        );
    }

    private void validarFechas(
            LocalDate inicio,
            LocalDate fin
    ) {

        if (fin != null
                && fin.isBefore(inicio)) {

            throw new ReglaNegocioException(
                    "La fecha de fin no puede ser anterior " +
                    "a la fecha de inicio"
            );
        }
    }

    private SuscripcionRespuestaDTO convertirDTO(
            Suscripcion suscripcion
    ) {

        return SuscripcionRespuestaDTO.builder()
                .id(suscripcion.getId())
                .tiendaId(
                        suscripcion.getTienda().getId()
                )
                .planId(
                        suscripcion.getPlan().getId()
                )
                .planNombre(
                        suscripcion.getPlan().getNombre()
                )
                .planPrecio(
                        suscripcion.getPlan().getPrecio()
                )
                .estado(
                        suscripcion.getEstado()
                )
                .fechaInicio(
                        suscripcion.getFechaInicio()
                )
                .fechaFin(
                        suscripcion.getFechaFin()
                )
                .creadoEn(
                        suscripcion.getCreadoEn()
                )
                .build();
    }
}