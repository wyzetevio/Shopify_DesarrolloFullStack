package com.shopcloud.pago.service;

import com.shopcloud.pago.entity.MetodoPago;
import com.shopcloud.pedido.entity.Pedido;
import org.springframework.stereotype.Service;

@Service
public class PagoSimuladoService {

    public String procesar(Pedido pedido, MetodoPago metodoPago) {
        return "APROBADO";
    }
}
