package com.shopcloud.carrito.controller;

import com.shopcloud.carrito.dto.ActualizarCantidadDTO;
import com.shopcloud.carrito.dto.AgregarProductoDTO;
import com.shopcloud.carrito.dto.CarritoDTO;
import com.shopcloud.carrito.service.CarritoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrito")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENTE')")
public class CarritoController {

    private final CarritoService carritoService;

    @GetMapping
    public ResponseEntity<CarritoDTO> obtener() {
        return ResponseEntity.ok(carritoService.obtenerActual());
    }

    @PostMapping("/productos")
    public ResponseEntity<CarritoDTO> agregar(
            @Valid @RequestBody AgregarProductoDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(carritoService.agregarProducto(dto));
    }

    @PutMapping("/productos/{elementoId}")
    public ResponseEntity<CarritoDTO> actualizarCantidad(
            @PathVariable Long elementoId,
            @Valid @RequestBody ActualizarCantidadDTO dto
    ) {
        return ResponseEntity.ok(
                carritoService.actualizarCantidad(elementoId, dto));
    }

    @DeleteMapping("/productos/{elementoId}")
    public ResponseEntity<Void> eliminar(@PathVariable Long elementoId) {
        carritoService.eliminarElemento(elementoId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> vaciar() {
        carritoService.vaciar();
        return ResponseEntity.noContent().build();
    }
}
