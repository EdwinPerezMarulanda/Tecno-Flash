package com.tecnoflash.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PruebaController {

    @GetMapping("/api/prueba")
    public String probarServidor() {
        return "Tecno-Flash Backend funcionando correctamente";
    }
}
