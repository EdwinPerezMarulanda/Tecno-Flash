package com.tecnoflash.backend.controller;

import com.tecnoflash.backend.service.FirestoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FirestoreController {

    private final FirestoreService firestoreService;

    public FirestoreController(FirestoreService firestoreService) {
        this.firestoreService = firestoreService;
    }

    @GetMapping("/api/firestore/prueba")
    public String probarFirestore() {
        try {
            return firestoreService.probarConexion();
        } catch (Exception e) {
            return "Error al consultar Firestore: " + e.getMessage();
        }
    }
}
