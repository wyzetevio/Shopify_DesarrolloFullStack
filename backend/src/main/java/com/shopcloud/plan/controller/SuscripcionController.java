package com.shopcloud.plan.controller;

import com.shopcloud.plan.dto.CrearSuscripcionDTO;
import com.shopcloud.plan.dto.SuscripcionRespuestaDTO;
import com.shopcloud.plan.service.SuscripcionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suscripciones")
@RequiredArgsConstructor
@PreAuthorize(
    "hasAnyRole('PROPIETARIO_TIENDA', 'SUPER_ADMINISTRADOR')"
)
public class SuscripcionController {

    private final SuscripcionService suscripcionService;

    @PostMapping
    public ResponseEntity<SuscripcionRespuestaDTO> crear(
            @Valid
            @RequestBody CrearSuscripcionDTO dto
    ) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        suscripcionService.crear(dto)
                );
    }

    @GetMapping
    public ResponseEntity<List<SuscripcionRespuestaDTO>>
    listar() {

        return ResponseEntity.ok(
                suscripcionService.listar()
        );
    }

    @GetMapping("/activa")
    public ResponseEntity<SuscripcionRespuestaDTO>
    obtenerActiva() {

        return ResponseEntity.ok(
                suscripcionService.obtenerActiva()
        );
    }

    @PutMapping("/cambiar-plan")
    public ResponseEntity<SuscripcionRespuestaDTO>
    cambiarPlan(
            @Valid
            @RequestBody CrearSuscripcionDTO dto
    ) {

        return ResponseEntity.ok(
                suscripcionService.cambiarPlan(dto)
        );
    }

    @DeleteMapping("/activa")
    public ResponseEntity<Void> cancelar() {

        suscripcionService.cancelar();

        return ResponseEntity.noContent().build();
    }
}