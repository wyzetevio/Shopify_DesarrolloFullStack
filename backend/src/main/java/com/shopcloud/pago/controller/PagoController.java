package com.shopcloud.pago.controller;

import com.shopcloud.pago.dto.MetodoPagoDTO;
import com.shopcloud.pago.dto.PagoDTO;
import com.shopcloud.pago.dto.TransaccionDTO;
import com.shopcloud.pago.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENTE')")
public class PagoController {

    private final PagoService pagoService;

    @GetMapping("/metodos")
    public ResponseEntity<List<MetodoPagoDTO>> listarMetodos() {
        return ResponseEntity.ok(pagoService.listarMetodosActivos());
    }

    @PostMapping
    public ResponseEntity<TransaccionDTO> pagar(
            @Valid @RequestBody PagoDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pagoService.procesar(dto));
    }

    @GetMapping("/transacciones")
    public ResponseEntity<List<TransaccionDTO>> listarTransacciones() {
        return ResponseEntity.ok(pagoService.listarTransaccionesPropias());
    }

    @GetMapping("/transacciones/{id}")
    public ResponseEntity<TransaccionDTO> obtenerTransaccion(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(pagoService.obtenerTransaccionPropia(id));
    }
}
