package com.tecnoflash.backend.service;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;


@Service
public class FirestoreService {

    public String probarConexion() throws Exception {

        Firestore db = FirestoreClient.getFirestore();

        return db.collection("productos")
            .limit(1)
            .get()
            .get()
            .getDocuments()
            .toString();
    }

    public Double obtenerPrecioProducto(String productoId)
        throws Exception {

        Firestore db = FirestoreClient.getFirestore();

        var documento = db.collection("productos")
            .document(productoId)
            .get()
            .get();

        if (!documento.exists()) {
            throw new RuntimeException("Producto no encontrado");
        }

        return documento.getDouble("precio");
    }

}
