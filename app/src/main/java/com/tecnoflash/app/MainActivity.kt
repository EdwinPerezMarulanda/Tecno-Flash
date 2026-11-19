package com.tecnoflash.app

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.content.Intent
import android.net.Uri

class MainActivity : AppCompatActivity() {
    private lateinit var root: FrameLayout

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var latitud: Double? = null
    private var longitud: Double? = null

    private var metodoPagoSeleccionado = ""
    private var rol = "Comprador"
    private val preferencias by lazy {
        getSharedPreferences("tecnoflash_prefs", MODE_PRIVATE)
    }

    private val claveBiometria = "biometria_activa"
    private val productos = mutableListOf(
        Producto("Audífonos", "Audífonos inalámbricos para uso diario.", 89900),
        Producto("Mouse", "Mouse óptico para computador.", 45900),
        Producto("Teclado", "Teclado USB de diseño compacto.", 120000),
        Producto("Parlante", "Parlante portátil.", 150000)
    )
    private val carrito = mutableListOf<Producto>()

    private val compras = mutableListOf(
        Compra("Juan Pérez", "Audífonos", 89900, "Pendiente"),
        Compra("Carlos López", "Mouse y teclado", 165900, "En proceso"),
        Compra("Laura Díaz", "Parlante", 150000, "Entregada")
    )

    private val usuarios = mutableListOf(
        Usuario("Juan Pérez", "juan@gmail.com", "Comprador"),
        Usuario("Ana Gómez", "ana@gmail.com", "Vendedor"),
        Usuario("Carlos López", "carlos@gmail.com", "Comprador"),
        Usuario("Laura Díaz", "laura@gmail.com", "Vendedor")
    )

    data class Compra(
        val cliente: String,
        val productos: String,
        val total: Int,
        var estado: String
    )

    data class Producto(
        val nombre: String,
        val descripcion: String,
        val precio: Int,
        val id: String = "",
        val imagen: String = "prodcuto_audifonos"
    )


    private fun obtenerImagenProducto(nombre: String): Int {
        return when (nombre) {
            "producto_audifonos" -> R.drawable.producto_audifonos
            "producto_mouse" -> R.drawable.producto_mouse
            "producto_teclado" -> R.drawable.producto_teclado
            "producto_parlante" -> R.drawable.producto_parlante
            "producto_macbook" -> R.drawable.producto_macbook
            "producto_apple_watch" -> R.drawable.producto_apple_watch
            else -> R.drawable.producto_audifonos
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        root = findViewById(R.id.contenedorPantalla)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mostrarBienvenida()
    }

    private fun solicitarUbicacion() {

        val permisoPreciso = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val permisoAproximado = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (permisoPreciso != PackageManager.PERMISSION_GRANTED &&
            permisoAproximado != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                100
            )
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { ubicacion: Location? ->

                if (ubicacion != null) {
                    latitud = ubicacion.latitude
                    longitud = ubicacion.longitude

                    aviso(
                        "Ubicación obtenida correctamente: " +
                                "$latitud, $longitud"
                    )
                } else {
                    aviso("No se pudo obtener la ubicación. Activa el GPS.")
                }
            }
            .addOnFailureListener {
                aviso("Error al obtener la ubicación")
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == 100) {
            if (grantResults.any {
                    it == PackageManager.PERMISSION_GRANTED
                }
            ) {
                solicitarUbicacion()
            } else {
                aviso("No se concedió el permiso de ubicación")
            }
        }
    }

    data class Usuario(
        var nombre: String,
        var correo: String,
        var rol: String,
        val uid: String = ""
    )


    private fun mostrarBienvenida() {
        val layout = pantallaBase()
        titulo(layout, "Tecno-Flash")
        texto(layout, "Tecnología al alcance de todos", 16, centrado = true)
        espacio(layout, 24)
        texto(layout, "Selecciona un perfil para continuar.", 14, centrado = true)

        listOf("Comprador", "Vendedor", "Administrador").forEach { perfil ->
            boton(layout, "Continuar como $perfil") {
                rol = perfil
                mostrarLogin()
            }
        }

        botonSecundario(layout, "Iniciar sesión") {
            mostrarLogin()
        }
    }


    private fun mostrarLogin() {
        val l = pantallaBase()
        encabezado(l, "Iniciar sesión")

        val correo = campo(l, "Correo electrónico")
        val contrasena = campo(l, "Contraseña", true)

        boton(l, "Ingresar") {

            val email = correo.text.toString().trim()
            val password = contrasena.text.toString()

            if (email.isBlank() || password.isBlank()) {
                aviso("Ingresa el correo y la contraseña")
                return@boton
            }

            FirebaseAuth.getInstance()
                .signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { tarea ->

                    if (tarea.isSuccessful) {

                        val usuarioActual = FirebaseAuth.getInstance().currentUser

                        if (usuarioActual == null) {
                            aviso("No se pudo identificar al usuario")
                            return@addOnCompleteListener
                        }

                        FirebaseFirestore.getInstance()
                            .collection("usuarios")
                            .document(usuarioActual.uid)
                            .get()
                            .addOnSuccessListener { documento ->

                                if (documento.exists()) {

                                    val rolFirebase =
                                        documento.getString("rol")

                                    if (rolFirebase in listOf(
                                            "Comprador",
                                            "Vendedor",
                                            "Administrador"
                                        )
                                    ) {
                                        rol = rolFirebase!!

                                        if (preferencias.getBoolean(
                                                claveBiometria,
                                                false
                                            )
                                        ) {
                                            verificarBiometria(false)
                                        } else {
                                            preguntarActivarBiometria()
                                        }

                                    } else {
                                        FirebaseAuth.getInstance().signOut()
                                        aviso("El rol del usuario no es válido")
                                    }

                                } else {
                                    FirebaseAuth.getInstance().signOut()
                                    aviso("El usuario no tiene un perfil registrado")
                                }
                            }
                            .addOnFailureListener {
                                aviso("No se pudo consultar el perfil en Firestore")
                            }

                    } else {
                        val error = tarea.exception?.localizedMessage
                        aviso(error ?: "No se pudo iniciar sesión")
                    }
                }
        }

        botonSecundario(l, "Crear cuenta") {
            mostrarRegistro()
        }

        botonSecundario(l, "¿Olvidaste tu contraseña?") {
            mostrarRecuperarPassword()
        }

        botonSecundario(l, "Volver") {
            mostrarBienvenida()
        }
    }

    private fun mostrarRegistro() {
        val l = pantallaBase()
        encabezado(l, "Crear cuenta")

        val nombre = campo(l, "Nombre completo")
        val correo = campo(l, "Correo electrónico")
        val password = campo(l, "Contraseña", true)
        val confirmarPassword = campo(l, "Confirmar contraseña", true)

        boton(l, "Registrarme") {

            val nombreUsuario = nombre.text.toString().trim()
            val email = correo.text.toString().trim()
            val clave = password.text.toString()
            val confirmar = confirmarPassword.text.toString()

            if (nombreUsuario.isBlank() ||
                email.isBlank() ||
                clave.isBlank() ||
                confirmar.isBlank()
            ) {
                aviso("Completa todos los campos")
                return@boton
            }

            if (clave.length < 6) {
                aviso("La contraseña debe tener mínimo 6 caracteres")
                return@boton
            }

            if (clave != confirmar) {
                aviso("Las contraseñas no coinciden")
                return@boton
            }

            FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, clave)
                .addOnCompleteListener(this) { tarea ->

                    if (tarea.isSuccessful) {

                        val usuario = FirebaseAuth.getInstance().currentUser

                        if (usuario == null) {
                            aviso("No se pudo crear el usuario")
                            return@addOnCompleteListener
                        }

                        val datos = hashMapOf(
                            "nombre" to nombreUsuario,
                            "correo" to email,
                            "rol" to "Comprador"
                        )

                        FirebaseFirestore.getInstance()
                            .collection("usuarios")
                            .document(usuario.uid)
                            .set(datos)
                            .addOnSuccessListener {

                                rol = "Comprador"
                                aviso("Cuenta creada correctamente")
                                mostrarInicioComprador()
                            }
                            .addOnFailureListener {
                                aviso("La cuenta se creó, pero no se pudo guardar el perfil")
                            }

                    } else {
                        aviso(
                            tarea.exception?.localizedMessage
                                ?: "No se pudo crear la cuenta"
                        )
                    }
                }
        }

        botonSecundario(l, "Volver al inicio de sesión") {
            mostrarLogin()
        }
    }

    private fun entrarAlSistema() {
        if (rol == "Comprador") {
            mostrarInicioComprador()
        } else {
            mostrarInicioGestion()
        }
    }

    private fun preguntarActivarBiometria() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Acceso con huella digital")
            .setMessage("¿Deseas activar el acceso con huella para tus próximos ingresos?")
            .setPositiveButton("Activar") { _, _ ->
                verificarBiometria(true)
            }
            .setNegativeButton("Ahora no") { _, _ ->
                entrarAlSistema()
            }
            .show()
    }

    private fun verificarBiometria(activar: Boolean) {

        val biometricManager = BiometricManager.from(this)

        val resultado = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )

        if (resultado != BiometricManager.BIOMETRIC_SUCCESS) {
            aviso("No hay huella digital disponible o configurada en este dispositivo.")

            if (activar) entrarAlSistema()
            return
        }

        val executor = ContextCompat.getMainExecutor(this)

        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)

                    if (activar) {
                        preferencias.edit()
                            .putBoolean(claveBiometria, true)
                            .apply()

                        aviso("Acceso con huella activado correctamente.")
                    }

                    entrarAlSistema()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)

                    aviso(errString.toString())

                    if (activar) {
                        entrarAlSistema()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    aviso("Huella no reconocida. Inténtalo nuevamente.")
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Acceso a Tecno-Flash")
            .setSubtitle("Verifica tu identidad con tu huella digital")
            .setNegativeButtonText("Cancelar")
            .build()

        prompt.authenticate(info)
    }

    private fun mostrarRecuperarPassword() {
        val l = pantallaBase()
        encabezado(l, "Recuperar contraseña")

        val correo = campo(l, "Correo electrónico")

        boton(l, "Enviar enlace") {
            val email = correo.text.toString().trim()

            if (email.isBlank()) {
                aviso("Ingresa tu correo electrónico")
                return@boton
            }

            FirebaseAuth.getInstance()
                .sendPasswordResetEmail(email)
                .addOnCompleteListener(this) { tarea ->
                    if (tarea.isSuccessful) {
                        aviso("Revisa tu correo para restablecer la contraseña")
                        mostrarLogin()
                    } else {
                        aviso(
                            tarea.exception?.localizedMessage
                                ?: "No se pudo enviar el enlace"
                        )
                    }
                }
        }

        botonSecundario(l, "Volver al inicio de sesión") {
            mostrarLogin()
        }
    }


    private fun mostrarInicioComprador() {
        val l = pantallaBase()
        encabezado(l, "Bienvenido a Tecno-Flash")

        texto(l, "Productos destacados", 16)

        FirebaseFirestore.getInstance()
            .collection("productos")
            .limit(2)
            .get()
            .addOnSuccessListener { resultado ->

                if (resultado.isEmpty) {
                    texto(l, "No hay productos disponibles.", 15)
                }

                resultado.documents.forEach { documento ->

                    val producto = Producto(
                        nombre = documento.getString("nombre") ?: "",
                        descripcion = documento.getString("descripcion") ?: "",
                        precio = (documento.getLong("precio") ?: 0L).toInt(),
                        id = documento.id,
                        imagen = documento.getString("imagen")
                            ?: "producto_audifonos"
                    )

                    tarjetaProducto(l, producto, false)
                }
            }
            .addOnFailureListener { error ->
                aviso("Error al cargar productos: ${error.localizedMessage}")
            }

        boton(l, "Ver productos") {
            mostrarProductos()
        }

        botonSecundario(l, "Mi perfil") {
            mostrarPerfil()
        }

        navegacion(
            l,
            listOf("Inicio", "Productos", "Carrito", "Perfil"),
            listOf(
                ::mostrarInicioComprador,
                ::mostrarProductos,
                ::mostrarCarrito,
                ::mostrarPerfil
            )
        )
    }


    private fun mostrarProductos() {
        val l = pantallaBase()
        encabezado(l, "Productos")

        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario == null) {
            aviso("Debes iniciar sesión")
            mostrarLogin()
            return
        }

        FirebaseFirestore.getInstance()
            .collection("productos")
            .get()
            .addOnSuccessListener { resultado ->

                if (resultado.isEmpty) {
                    texto(l, "No hay productos disponibles.", 15)
                }

                resultado.documents.forEach { documento ->

                    val producto = Producto(
                        nombre = documento.getString("nombre") ?: "",
                        descripcion = documento.getString("descripcion") ?: "",
                        precio = (documento.getLong("precio") ?: 0L).toInt(),
                        id = documento.id,
                        imagen = documento.getString("imagen")
                            ?: "producto_audifonos"
                    )

                    tarjetaProducto(l, producto, true)
                }
            }
            .addOnFailureListener { error ->
                aviso("Error al consultar productos: ${error.localizedMessage}")
            }

        botonSecundario(l, "Volver al inicio") {
            mostrarInicioComprador()
        }

        navegacion(
            l,
            listOf("Inicio", "Productos", "Carrito", "Perfil"),
            listOf(
                ::mostrarInicioComprador,
                ::mostrarProductos,
                ::mostrarCarrito,
                ::mostrarPerfil
            )
        )
    }


    private fun mostrarDetalle(p: Producto) {
        val l = pantallaBase()
        encabezado(l, "Detalle del producto")
        val imagen = ImageView(this).apply {
            setImageResource(obtenerImagenProducto(p.imagen))
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        l.addView(imagen, LinearLayout.LayoutParams(-1, dp(220)))
        texto(l, p.nombre, 21)
        texto(l, p.descripcion, 14)
        texto(l, dinero(p.precio), 18)
        boton(l, "Agregar al carrito") { carrito.add(p); aviso("Producto agregado al carrito") }
        botonSecundario(l, "Volver a productos") { mostrarProductos() }
    }

    private fun mostrarCarrito() {
        val l = pantallaBase()
        encabezado(l, "Carrito")
        if (carrito.isEmpty()) texto(l, "Tu carrito está vacío.", 15)
        carrito.forEach { texto(l, "${it.nombre} · ${dinero(it.precio)}", 15) }
        val total = carrito.sumOf { it.precio }
        texto(l, "Subtotal: ${dinero(total)}", 18)
        boton(l, "Continuar pago") { mostrarPago() }
        botonSecundario(l, "Seguir comprando") { mostrarProductos() }
        navegacion(
            l,
            listOf("Inicio", "Productos", "Carrito", "Perfil"),
            listOf(::mostrarInicioComprador, ::mostrarProductos, ::mostrarCarrito, ::mostrarPerfil)
        )
    }


    private fun mostrarPago() {
        val l = pantallaBase()
        encabezado(l, "Pago")

        texto(l, "Resumen de compra", 17)
        texto(l, "Productos: ${carrito.size}", 14)
        texto(l, "Total: ${dinero(carrito.sumOf { it.precio })}", 18)

        texto(l, "Método de pago", 16)

        botonSecundario(l, "Tarjeta") {
            metodoPagoSeleccionado = "Tarjeta"
            aviso("Método seleccionado: Tarjeta")
        }

        botonSecundario(l, "PSE") {
            metodoPagoSeleccionado = "PSE"
            aviso("Método seleccionado: PSE")
        }

        botonSecundario(l, "Obtener ubicación de entrega") {
            solicitarUbicacion()
        }


        boton(l, "Confirmar compra") {

            if (carrito.isEmpty()) {
                aviso("El carrito está vacío")
                return@boton
            }

            if (metodoPagoSeleccionado.isEmpty()) {
                aviso("Selecciona un método de pago")
                return@boton
            }

            val usuario = FirebaseAuth.getInstance().currentUser

            if (latitud == null || longitud == null) {
                aviso("Primero debes obtener tu ubicación de entrega")
                return@boton
            }

            if (usuario == null || usuario.email.isNullOrBlank()) {
                aviso("Debes iniciar sesión")
                return@boton
            }

            val productosPago = carrito.map { producto ->
                Pair(producto.id, 1)
            }

            aviso("Conectando con Mercado Pago...")

            java.util.concurrent.Executors.newSingleThreadExecutor().execute {

                try {
                    val api = MercadoPagoApi()

                    val checkoutUrl = api.crearOrden(
                        usuario.email!!,
                        productosPago
                    )

                    runOnUiThread {

                        val intent = android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse(checkoutUrl)
                        )

                        startActivity(intent)
                    }

                } catch (error: Exception) {

                    runOnUiThread {
                        aviso(
                            "Error al iniciar el pago: ${error.localizedMessage}"
                        )
                    }
                }
            }
        }


        texto(l, "Esta pantalla es una demostración. No realiza cobros.", 12)
    }


    private fun mostrarPerfil() {
        val l = pantallaBase()
        encabezado(l, "Mi perfil")
        texto(l, "Usuario de ejemplo", 16, centrado = true)
        texto(l, "usuario@tecnoflash.com", 13, centrado = true)
        botonSecundario(l, "Editar perfil") { aviso("Edición de perfil: demostración") }
        botonSecundario(l, "Mis compras") {
            mostrarMisCompras()
        }
        botonSecundario(l, "Cerrar sesión") {
            FirebaseAuth.getInstance().signOut()
            rol = "Comprador"
            carrito.clear()
            mostrarBienvenida()
        }
        navegacion(
            l,
            listOf("Inicio", "Productos", "Carrito", "Perfil"),
            listOf(::mostrarInicioComprador, ::mostrarProductos, ::mostrarCarrito, ::mostrarPerfil)
        )
    }

    private fun mostrarMisCompras() {
        val l = pantallaBase()
        encabezado(l, "Mis compras")

        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario == null) {
            aviso("Debes iniciar sesión")
            mostrarLogin()
            return
        }

        FirebaseFirestore.getInstance()
            .collection("ordenes")
            .whereEqualTo("compradorUid", usuario.uid)
            .get()
            .addOnSuccessListener { resultado ->

                if (resultado.isEmpty) {
                    texto(l, "Todavía no tienes compras registradas.", 15)
                }

                resultado.documents.forEach { documento ->

                    val total = (documento.getLong("total") ?: 0L).toInt()

                    val estado = documento.getString("estado")
                        ?: "Pendiente"

                    val metodoPago = documento.getString("metodoPago")
                        ?: "No especificado"

                    val fecha = documento.getLong("fecha") ?: 0L

                    val fechaTexto = if (fecha > 0) {
                        java.text.SimpleDateFormat(
                            "dd/MM/yyyy HH:mm",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date(fecha))
                    } else {
                        "No disponible"
                    }

                    val listaProductos = documento.get("productos") as? List<*>

                    val nombresProductos = listaProductos
                        ?.mapNotNull { producto ->
                            (producto as? Map<*, *>)?.get("nombre") as? String
                        }
                        ?.joinToString(", ")
                        ?: "Sin productos"

                    val fila = tarjeta(l)

                    texto(fila, "Productos: $nombresProductos", 14)
                    texto(fila, "Total: ${dinero(total)}", 15)
                    texto(fila, "Estado: $estado", 15)
                    texto(fila, "Método de pago: $metodoPago", 14)
                    texto(fila, "Fecha: $fechaTexto", 13)
                }
            }
            .addOnFailureListener { error ->
                aviso("Error al consultar compras: ${error.localizedMessage}")
            }

        botonSecundario(l, "Volver al perfil") {
            mostrarPerfil()
        }
    }

    private fun mostrarInicioGestion() {
        val admin = rol == "Administrador"
        val l = pantallaBase()
        encabezado(l, "Bienvenido, $rol")
        texto(
            l,
            if (admin) "Administra la información de Tecno-Flash." else "Gestiona tus productos de forma fácil.",
            14
        )
        if (admin) {
            boton(l, "Usuarios") { mostrarUsuarios() }
            boton(l, "Productos") {
                mostrarProductosAdmin()
            }
            boton(l, "Compras") {
                mostrarCompras()
            }
        } else {
            boton(l, "Mis productos") { mostrarMisProductos() }
            boton(l, "Registrar producto") { mostrarFormularioProducto(null) }
        }
        botonSecundario(l, "Mi perfil") { mostrarPerfilGestion() }
        botonSecundario(l, "Cambiar perfil") { mostrarBienvenida() }
    }


    private fun mostrarProductosAdmin() {
        val l = pantallaBase()
        encabezado(l, "Administrar productos")

        FirebaseFirestore.getInstance()
            .collection("productos")
            .get()
            .addOnSuccessListener { resultado ->

                if (resultado.isEmpty) {
                    texto(l, "No hay productos registrados.", 15)
                }

                resultado.documents.forEach { documento ->

                    val producto = Producto(
                        nombre = documento.getString("nombre") ?: "",
                        descripcion = documento.getString("descripcion") ?: "",
                        precio = (documento.getLong("precio") ?: 0L).toInt(),
                        id = documento.id,
                        imagen = documento.getString("imagen")
                            ?: "producto_audifonos"
                    )

                    tarjetaProducto(l, producto, false)
                }
            }
            .addOnFailureListener { error ->
                aviso("Error al consultar productos: ${error.localizedMessage}")
            }

        botonSecundario(l, "Volver") {
            mostrarInicioGestion()
        }
    }


    private fun mostrarUsuarios() {
        val l = pantallaBase()
        encabezado(l, "Administrar usuarios")

        FirebaseFirestore.getInstance()
            .collection("usuarios")
            .get()
            .addOnSuccessListener { resultado ->

                if (resultado.isEmpty) {
                    texto(l, "No hay usuarios registrados.", 15)
                }

                resultado.documents.forEach { documento ->

                    val usuario = Usuario(
                        nombre = documento.getString("nombre") ?: "",
                        correo = documento.getString("correo") ?: "",
                        rol = documento.getString("rol") ?: "",
                        uid = documento.id
                    )

                    val fila = tarjeta(l)

                    texto(fila, usuario.nombre, 16)
                    texto(fila, usuario.correo, 13)
                    texto(fila, usuario.rol, 13)

                    botonPequeno(fila, "Editar") {
                        mostrarFormularioUsuario(usuario)
                    }

                    botonPequeno(fila, "Eliminar") {
                        val usuarioActual = FirebaseAuth.getInstance().currentUser

                        if (usuario.uid == usuarioActual?.uid) {
                            aviso("No puedes eliminar tu propia cuenta")
                            return@botonPequeno
                        }

                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Eliminar usuario")
                            .setMessage("¿Deseas eliminar el perfil de ${usuario.nombre}?")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("Eliminar") { _, _ ->

                                FirebaseFirestore.getInstance()
                                    .collection("usuarios")
                                    .document(usuario.uid)
                                    .delete()
                                    .addOnSuccessListener {
                                        aviso("Perfil eliminado de Firestore")
                                        mostrarUsuarios()
                                    }
                                    .addOnFailureListener { error ->
                                        aviso("Error: ${error.localizedMessage}")
                                    }
                            }
                            .show()
                    }
                }
            }
            .addOnFailureListener { error ->
                aviso("Error al consultar usuarios: ${error.localizedMessage}")
            }

        botonSecundario(l, "Volver") {
            mostrarInicioGestion()
        }
    }

    private fun mostrarFormularioUsuario(usuario: Usuario) {
        val l = pantallaBase()
        encabezado(l, "Editar usuario")

        val nombre = campo(l, "Nombre completo")
        nombre.setText(usuario.nombre)

        texto(l, "Correo: ${usuario.correo}", 14)

        texto(l, "Tipo de usuario", 14)

        val selectorRol = Spinner(this)
        val roles = listOf("Comprador", "Vendedor", "Administrador")

        selectorRol.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            roles
        )

        selectorRol.setSelection(roles.indexOf(usuario.rol))
        l.addView(selectorRol, params())

        boton(l, "Guardar cambios") {
            val nuevoNombre = nombre.text.toString().trim()
            val nuevoRol = selectorRol.selectedItem.toString()

            if (nuevoNombre.isBlank()) {
                aviso("Ingresa el nombre del usuario")
                return@boton
            }

            FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(usuario.uid)
                .update(
                    mapOf(
                        "nombre" to nuevoNombre,
                        "rol" to nuevoRol
                    )
                )
                .addOnSuccessListener {
                    aviso("Usuario actualizado correctamente")
                    mostrarUsuarios()
                }
                .addOnFailureListener { error ->
                    aviso("Error al actualizar: ${error.localizedMessage}")
                }
        }

        botonSecundario(l, "Cancelar") {
            mostrarUsuarios()
        }
    }

    private fun mostrarCompras() {
        val l = pantallaBase()
        encabezado(l, "Administrar compras")

        FirebaseFirestore.getInstance()
            .collection("ordenes")
            .get()
            .addOnSuccessListener { resultado ->

                if (resultado.isEmpty) {
                    texto(l, "No hay compras registradas.", 15)
                }

                resultado.documents.forEach { documento ->

                    val correo = documento.getString("compradorCorreo")
                        ?: "Cliente"

                    val total = (documento.getLong("total") ?: 0L).toInt()

                    val estado = documento.getString("estado")
                        ?: "Pendiente"

                    val metodoPago = documento.getString("metodoPago")
                        ?: "No especificado"

                    val latitud = documento.getDouble("latitud")
                    val longitud = documento.getDouble("longitud")

                    val listaProductos = documento.get("productos") as? List<*>

                    val nombresProductos = listaProductos
                        ?.mapNotNull { producto ->
                            (producto as? Map<*, *>)?.get("nombre") as? String
                        }
                        ?.joinToString(", ")
                        ?: "Sin productos"

                    val fila = tarjeta(l)

                    texto(fila, "Cliente: $correo", 15)
                    texto(fila, "Productos: $nombresProductos", 14)
                    texto(fila, "Total: ${dinero(total)}", 14)
                    texto(fila, "Estado: $estado", 14)
                    texto(fila, "Método de pago: $metodoPago", 14)

                    if (latitud != null && longitud != null) {
                        texto(fila, "Ubicación de entrega:", 14)
                        texto(fila, "Latitud: $latitud", 13)
                        texto(fila, "Longitud: $longitud", 13)
                    } else {
                        texto(fila, "Ubicación no registrada", 13)
                    }

                    botonPequeno(fila, "Ver ubicación en Google Maps") {
                        if (latitud != null && longitud != null) {
                            val uri = android.net.Uri.parse(
                                "https://www.google.com/maps/search/?api=1&query=$latitud,$longitud"
                            )

                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                uri
                            )

                            startActivity(intent)
                        } else {
                            aviso("Este pedido no tiene ubicación registrada")
                        }
                    }

                    botonPequeno(fila, "Cambiar estado") {

                        val nuevoEstado = when (estado) {
                            "Pendiente" -> "En proceso"
                            "En proceso" -> "Entregada"
                            else -> "Pendiente"
                        }

                        FirebaseFirestore.getInstance()
                            .collection("ordenes")
                            .document(documento.id)
                            .update("estado", nuevoEstado)
                            .addOnSuccessListener {
                                aviso("Estado actualizado")
                                mostrarCompras()
                            }
                            .addOnFailureListener { error ->
                                aviso("Error: ${error.localizedMessage}")
                            }
                    }

                    botonPequeno(fila, "Eliminar") {

                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Eliminar compra")
                            .setMessage("¿Deseas eliminar este pedido?")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("Eliminar") { _, _ ->

                                FirebaseFirestore.getInstance()
                                    .collection("ordenes")
                                    .document(documento.id)
                                    .delete()
                                    .addOnSuccessListener {
                                        aviso("Compra eliminada")
                                        mostrarCompras()
                                    }
                                    .addOnFailureListener { error ->
                                        aviso("Error: ${error.localizedMessage}")
                                    }
                            }
                            .show()
                    }
                }
            }
            .addOnFailureListener { error ->
                aviso("Error al consultar compras: ${error.localizedMessage}")
            }

        botonSecundario(l, "Volver") {
            mostrarInicioGestion()
        }
    }


    private fun mostrarMisProductos() {
        val l = pantallaBase()
        encabezado(l, "Mis productos")

        boton(l, "+ Registrar") {
            mostrarFormularioProducto(null)
        }

        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario == null) {
            aviso("Debes iniciar sesión")
            mostrarLogin()
            return
        }

        FirebaseFirestore.getInstance()
            .collection("productos")
            .whereEqualTo("vendedorUid", usuario.uid)
            .get()
            .addOnSuccessListener { resultado ->

                if (resultado.isEmpty) {
                    texto(l, "Todavía no tienes productos registrados.", 15)
                }

                resultado.documents.forEach { documento ->

                    val producto = Producto(
                        nombre = documento.getString("nombre") ?: "",
                        descripcion = documento.getString("descripcion") ?: "",
                        precio = (documento.getLong("precio") ?: 0L).toInt(),
                        id = documento.id,
                        imagen = documento.getString("imagen") ?: "producto_audifonos"
                    )

                    val fila = tarjeta(l)

                    val imagen = ImageView(this).apply {
                        setImageResource(obtenerImagenProducto(producto.imagen))
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }

                    fila.addView(
                        imagen,
                        LinearLayout.LayoutParams(-1, dp(160))
                    )

                    texto(fila, producto.nombre, 16)
                    texto(fila, producto.descripcion, 13)
                    texto(fila, dinero(producto.precio), 15)

                    botonPequeno(fila, "Editar") {
                        mostrarFormularioProducto(producto)
                    }


                    botonPequeno(fila, "Eliminar") {
                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Eliminar producto")
                            .setMessage("¿Deseas eliminar ${producto.nombre}?")
                            .setNegativeButton("Cancelar", null)
                            .setPositiveButton("Eliminar") { _, _ ->

                                FirebaseFirestore.getInstance()
                                    .collection("productos")
                                    .document(producto.id)
                                    .delete()
                                    .addOnSuccessListener {
                                        aviso("Producto eliminado correctamente")
                                        mostrarMisProductos()
                                    }
                                    .addOnFailureListener { error ->
                                        aviso("Error al eliminar: ${error.localizedMessage}")
                                    }
                            }
                            .show()
                    }

                }
            }
            .addOnFailureListener { error ->
                aviso("Error al consultar productos: ${error.localizedMessage}")
            }

        botonSecundario(l, "Volver") {
            mostrarInicioGestion()
        }
    }

    private fun mostrarFormularioProducto(producto: Producto?) {
        val l = pantallaBase()
        encabezado(l, if (producto == null) "Registrar producto" else "Editar producto")
        val nombre = campo(l, "Nombre del producto")
        val descripcion = campo(l, "Descripción")
        val precio = campo(l, "Precio")
        nombre.setText(producto?.nombre ?: "")
        descripcion.setText(producto?.descripcion ?: "")
        precio.setText(producto?.precio?.toString() ?: "")

        texto(l, "Imagen del producto", 14)

        val imagenes = listOf(
            "Audífonos" to "producto_audifonos",
            "Mouse" to "producto_mouse",
            "Teclado" to "producto_teclado",
            "Parlante" to "producto_parlante",
            "MacBook" to "producto_macbook",
            "Apple Watch" to "producto_apple_watch"
        )

        val selectorImagen = Spinner(this)

        selectorImagen.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            imagenes.map { it.first }
        )

        selectorImagen.setSelection(
            imagenes.indexOfFirst { it.second == producto?.imagen }
                .takeIf { it >= 0 } ?: 0
        )

        l.addView(selectorImagen, params())

        caja(l, "Seleccionar una imagen (demostración)", 70)
        boton(l, if (producto == null) "Publicar producto" else "Guardar cambios") {
            val n = nombre.text.toString().trim()
            val d = descripcion.text.toString().trim()
            val v = precio.text.toString().toIntOrNull()

            val imagenElegida =
                imagenes[selectorImagen.selectedItemPosition].second

            if (n.isBlank() || d.isBlank() || v == null || v <= 0) {
                aviso("Completa los campos correctamente")
                return@boton
            }

            if (producto == null) {
                val usuario = FirebaseAuth.getInstance().currentUser

                if (usuario == null) {
                    aviso("Debes iniciar sesión")
                    return@boton
                }

                val datos = hashMapOf(
                    "nombre" to n,
                    "descripcion" to d,
                    "precio" to v,
                    "vendedorUid" to usuario.uid,
                    "imagen" to imagenElegida
                )

                FirebaseFirestore.getInstance()
                    .collection("productos")
                    .add(datos)
                    .addOnSuccessListener {
                        aviso("Producto registrado correctamente")
                        mostrarMisProductos()
                    }
                    .addOnFailureListener { error ->
                        aviso("Error al guardar: ${error.localizedMessage}")
                    }


            } else {
                val datosActualizados = hashMapOf(
                    "nombre" to n,
                    "descripcion" to d,
                    "precio" to v,
                    "imagen" to imagenElegida
                )

                FirebaseFirestore.getInstance()
                    .collection("productos")
                    .document(producto.id)
                    .update(datosActualizados as Map<String, Any>)
                    .addOnSuccessListener {
                        aviso("Producto actualizado correctamente")
                        mostrarMisProductos()
                    }
                    .addOnFailureListener { error ->
                        aviso("Error al actualizar: ${error.localizedMessage}")
                    }
            }

        }
        botonSecundario(l, "Cancelar") { mostrarMisProductos() }
    }

    private fun mostrarLista(titulo: String, elementos: List<String>) {
        val l = pantallaBase()
        encabezado(l, titulo)
        elementos.forEach { tarjeta(l, it) }
        botonSecundario(l, "Volver al panel") { mostrarInicioGestion() }
    }

    private fun mostrarPerfilGestion() {
        val l = pantallaBase()
        encabezado(l, "Mi perfil")
        texto(l, "$rol · usuario@tecnoflash.com", 15)
        botonSecundario(l, "Cerrar sesión") {
            FirebaseAuth.getInstance().signOut()
            rol = "Comprador"
            carrito.clear()
            mostrarBienvenida()
        }
    }


    private fun tarjetaProducto(l: LinearLayout, p: Producto, agregar: Boolean) {
        val c = tarjeta(l)

        val imagen = ImageView(this).apply {
            setImageResource(obtenerImagenProducto(p.imagen))
            scaleType = ImageView.ScaleType.CENTER_CROP
        }

        c.addView(
            imagen,
            LinearLayout.LayoutParams(-1, dp(180))
        )

        texto(c, p.nombre, 16)
        texto(c, p.descripcion, 13)
        texto(c, dinero(p.precio), 15)

        botonPequeno(c, if (agregar) "Ver detalle" else "Ver") {
            mostrarDetalle(p)
        }
    }


    private fun pantallaBase(): LinearLayout {
        val scroll = ScrollView(this)
        val l = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(24))
        }
        scroll.addView(l)
        root.removeAllViews()
        root.addView(scroll)
        return l
    }

    private fun encabezado(l: LinearLayout, t: String) {
        val bar = TextView(this).apply {
            text = "Tecno-Flash"
            textSize = 19f
            setTextColor(getColor(android.R.color.white))
            setBackgroundColor(getColor(R.color.primary))
            setPadding(dp(16), dp(15), dp(16), dp(15))
        }
        l.addView(bar, params())
        espacio(l, 18)
        titulo(l, t)
    }

    private fun titulo(l: LinearLayout, t: String) {
        val v = TextView(this).apply {
            text = t; textSize = 24f; setTextColor(getColor(R.color.text_primary))
            gravity = Gravity.CENTER
        }
        l.addView(v, params())
        espacio(l, 10)
    }

    private fun texto(l: LinearLayout, t: String, size: Int, centrado: Boolean = false) {
        val v = TextView(this).apply {
            text = t; textSize = size.toFloat()
            setTextColor(getColor(R.color.text_primary))
            if (centrado) gravity = Gravity.CENTER
            setPadding(dp(3), dp(6), dp(3), dp(6))
        }
        l.addView(v, params())
    }

    private fun campo(l: LinearLayout, hint: String, password: Boolean = false): EditText {
        val e = EditText(this).apply {
            this.hint = hint
            textSize = 14f
            setSingleLine(!password)
            if (password) inputType = 129
            setPadding(dp(12), dp(8), dp(12), dp(8))
            setBackgroundResource(R.drawable.card_background)
        }
        val p = params()
        p.setMargins(0, dp(6), 0, dp(8))
        l.addView(e, p)
        return e
    }

    private fun boton(l: LinearLayout, t: String, accion: () -> Unit) {
        val b = Button(this).apply {
            text = t; isAllCaps = false; setTextColor(getColor(android.R.color.white))
            setBackgroundResource(R.drawable.button_background)
            setOnClickListener { accion() }
        }
        val p = params(); p.setMargins(0, dp(6), 0, dp(6))
        l.addView(b, p)
    }

    private fun botonSecundario(l: LinearLayout, t: String, accion: () -> Unit) {
        val b = Button(this).apply { text = t; isAllCaps = false; setOnClickListener { accion() } }
        val p = params(); p.setMargins(0, dp(4), 0, dp(4)); l.addView(b, p)
    }

    private fun botonPequeno(l: LinearLayout, t: String, accion: () -> Unit) {
        val b = Button(this).apply {
            text = t; isAllCaps = false; textSize = 12f; setOnClickListener { accion() }
        }
        l.addView(b, params())
    }

    private fun tarjeta(l: LinearLayout, contenido: String? = null): LinearLayout {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setBackgroundResource(R.drawable.card_background)
        }
        val p = params(); p.setMargins(0, dp(6), 0, dp(6)); l.addView(c, p)
        if (contenido != null) texto(c, contenido, 14)
        return c
    }

    private fun caja(l: LinearLayout, t: String, alto: Int) {
        val v = TextView(this).apply {
            text = t; gravity = Gravity.CENTER; textSize = 14f
            setTextColor(getColor(R.color.text_secondary))
            setBackgroundResource(R.drawable.card_background)
        }
        val p = LinearLayout.LayoutParams(-1, dp(alto)); p.setMargins(0, dp(8), 0, dp(8))
        l.addView(v, p)
    }

    private fun navegacion(l: LinearLayout, etiquetas: List<String>, acciones: List<() -> Unit>) {
        espacio(l, 14)
        val fila = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        etiquetas.forEachIndexed { i, s ->
            val b = Button(this).apply {
                text = s; textSize = 10f; isAllCaps = false
                setPadding(dp(2), 0, dp(2), 0)
                setOnClickListener { acciones[i]() }
            }
            fila.addView(b, LinearLayout.LayoutParams(0, dp(48), 1f))
        }
        l.addView(fila, params())
    }

    private fun espacio(l: LinearLayout, alto: Int) {
        l.addView(View(this), LinearLayout.LayoutParams(1, dp(alto)))
    }

    private fun params() = LinearLayout.LayoutParams(-1, -2)
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun dinero(v: Int) = "$" + String.format("%,d", v).replace(",", ".")
    private fun aviso(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()
}
