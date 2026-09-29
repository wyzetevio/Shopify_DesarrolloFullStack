package com.shopcloud.pedido.controller;

import com.shopcloud.pedido.dto.CheckoutDTO;
import com.shopcloud.pedido.dto.PedidoRespuestaDTO;
import com.shopcloud.pedido.service.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENTE')")
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping
    public ResponseEntity<PedidoRespuestaDTO> checkout(
            @Valid @RequestBody CheckoutDTO dto
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(checkoutService.realizarCheckout(dto));
    }
}
