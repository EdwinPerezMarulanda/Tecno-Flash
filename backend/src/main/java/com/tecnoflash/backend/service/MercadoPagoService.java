
package com.tecnoflash.backend.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.order.OrderClient;
import com.mercadopago.client.order.OrderCreateRequest;
import com.mercadopago.client.order.OrderItemRequest;
import com.mercadopago.client.order.OrderPayerRequest;
import com.mercadopago.core.MPRequestOptions;
import com.mercadopago.resources.order.Order;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.tecnoflash.backend.dto.CrearOrdenRequest;

import java.util.ArrayList;
import java.util.List;

@Service
public class MercadoPagoService {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    private final FirestoreService firestoreService;

    public MercadoPagoService(FirestoreService firestoreService) {
        this.firestoreService = firestoreService;
    }

    public String probarConfiguracion() {

        if (accessToken == null || accessToken.isBlank()) {
            return "Falta configurar el Access Token";
        }

        return "Mercado Pago está configurado correctamente";
    }


    public Map<String, Object> crearOrden(CrearOrdenRequest solicitud)
        throws Exception {

        if (solicitud.getCorreo() == null || solicitud.getCorreo().isBlank()) {
            throw new RuntimeException("El correo es obligatorio");
        }

        if (solicitud.getProductos() == null || solicitud.getProductos().isEmpty()) {
            throw new RuntimeException("El carrito está vacío");
        }

        List<OrderItemRequest> productos = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CrearOrdenRequest.ProductoRequest item : solicitud.getProductos()) {

            if (item.getProductoId() == null || item.getCantidad() == null
                || item.getCantidad() <= 0) {
                throw new RuntimeException("Producto o cantidad inválida");
            }

            Double precio = firestoreService.obtenerPrecioProducto(item.getProductoId());

            if (precio == null || precio <= 0) {
                throw new RuntimeException("Precio del producto inválido");
            }

            BigDecimal precioUnitario = BigDecimal.valueOf(precio);
            BigDecimal subtotal = precioUnitario.multiply(
                BigDecimal.valueOf(item.getCantidad())
            );

            total = total.add(subtotal);

            productos.add(
                OrderItemRequest.builder()
                    .externalCode(item.getProductoId())
                    .title("Producto Tecno-Flash")
                    .quantity(item.getCantidad())
                    .unitPrice(
                        precioUnitario.stripTrailingZeros().toPlainString()
                    )
                    .build()
            );
        }

        String totalTexto = total.stripTrailingZeros().toPlainString();

        MercadoPagoConfig.setAccessToken(accessToken);

        OrderClient cliente = new OrderClient();

        OrderCreateRequest ordenSolicitud = OrderCreateRequest.builder()
            .type("online")
            .processingMode("manual")
            .captureMode("automatic")
            .totalAmount(totalTexto)
            .externalReference("COMPRA-TECNOFLASH-" + UUID.randomUUID())
            .description("Compra en Tecno-Flash")
            .payer(OrderPayerRequest.builder()
                .email(solicitud.getCorreo())
                .build())
            .items(productos)
            .build();

        Map<String, String> encabezados = new HashMap<>();
        encabezados.put("X-Idempotency-Key", UUID.randomUUID().toString());

        MPRequestOptions opciones = MPRequestOptions.builder()
            .customHeaders(encabezados)
            .build();

        Order orden = cliente.create(ordenSolicitud, opciones);

        Map<String, Object> respuesta = new HashMap<>();
        respuesta.put("idOrden", orden.getId());
        respuesta.put("estado", orden.getStatus());
        respuesta.put("checkoutUrl", orden.getCheckoutUrl());
        respuesta.put("total", totalTexto);

        return respuesta;
    }

}
