
package com.tecnoflash.app

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MercadoPagoApi {

    fun crearOrden(
        correo: String,
        productos: List<Pair<String, Int>>
    ): String {

        val url = URL("http://10.0.2.2:8080/api/mercadopago/crear-orden")

        val conexion = url.openConnection() as HttpURLConnection

        try {
            conexion.requestMethod = "POST"
            conexion.setRequestProperty("Content-Type", "application/json")
            conexion.doOutput = true
            conexion.connectTimeout = 10000
            conexion.readTimeout = 15000

            val listaProductos = JSONArray()

            productos.forEach { (productoId, cantidad) ->
                val producto = JSONObject()
                producto.put("productoId", productoId)
                producto.put("cantidad", cantidad)
                listaProductos.put(producto)
            }

            val solicitud = JSONObject()
            solicitud.put("correo", correo)
            solicitud.put("productos", listaProductos)

            conexion.outputStream.use {
                it.write(solicitud.toString().toByteArray(Charsets.UTF_8))
            }

            if (conexion.responseCode !in 200..299) {
                throw Exception("Error del servidor: ${conexion.responseCode}")
            }

            val respuesta = conexion.inputStream.bufferedReader().use {
                it.readText()
            }

            val json = JSONObject(respuesta)

            return json.getString("checkoutUrl")

        } finally {
            conexion.disconnect()
        }
    }
}
