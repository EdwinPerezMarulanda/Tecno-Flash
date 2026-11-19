
package com.tecnoflash.backend.controller;

import com.tecnoflash.backend.dto.CrearOrdenRequest;
import com.tecnoflash.backend.service.MercadoPagoService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mercadopago")
public class MercadoPagoController {

    private final MercadoPagoService mercadoPagoService;

    public MercadoPagoController(MercadoPagoService mercadoPagoService) {
        this.mercadoPagoService = mercadoPagoService;
    }

    @GetMapping("/prueba")
    public String probarMercadoPago() {
        return mercadoPagoService.probarConfiguracion();
    }

    @PostMapping("/crear-orden")
    public Map<String, Object> crearOrden(
        @RequestBody CrearOrdenRequest solicitud) throws Exception {

        return mercadoPagoService.crearOrden(solicitud);
    }
}
