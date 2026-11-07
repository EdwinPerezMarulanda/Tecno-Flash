package com.tecnoflash.app

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var root: FrameLayout
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

    data class Producto(val nombre: String, val descripcion: String, val precio: Int)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        root = findViewById(R.id.contenedorPantalla)
        mostrarBienvenida()
    }

    data class Usuario(
        var nombre: String,
        var correo: String,
        var rol: String
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

        campo(l, "Correo electrónico")
        campo(l, "Contraseña", true)

        boton(l, "Ingresar") {
            if (preferencias.getBoolean(claveBiometria, false)) {
                entrarAlSistema()
            } else {
                preguntarActivarBiometria()
            }
        }

        if (preferencias.getBoolean(claveBiometria, false)) {
            botonSecundario(l, "Ingresar con huella digital") {
                verificarBiometria(false)
            }
        }

        botonSecundario(l, "¿Olvidaste tu contraseña?") {
            mostrarRecuperarPassword()
        }

        botonSecundario(l, "Volver") {
            mostrarBienvenida()
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

        campo(l, "Correo electrónico")

        boton(l, "Enviar instrucciones") {
            mostrarLogin()
        }

        botonSecundario(l, "Volver al inicio de sesión") {
            mostrarLogin()
        }
    }

    private fun mostrarInicioComprador() {
        val l = pantallaBase()
        encabezado(l, "Bienvenido a Tecno-Flash")
        texto(l, "Productos destacados", 16)
        productos.take(2).forEach { tarjetaProducto(l, it, false) }
        boton(l, "Ver productos") { mostrarProductos() }
        botonSecundario(l, "Mi perfil") { mostrarPerfil() }
        navegacion(
            l,
            listOf("Inicio", "Productos", "Carrito", "Perfil"),
            listOf(::mostrarInicioComprador, ::mostrarProductos, ::mostrarCarrito, ::mostrarPerfil)
        )
    }

    private fun mostrarProductos() {
        val l = pantallaBase()
        encabezado(l, "Productos")
        campo(l, "Buscar producto")
        productos.forEach { tarjetaProducto(l, it, true) }
        botonSecundario(l, "Volver al inicio") { mostrarInicioComprador() }
        navegacion(
            l,
            listOf("Inicio", "Productos", "Carrito", "Perfil"),
            listOf(::mostrarInicioComprador, ::mostrarProductos, ::mostrarCarrito, ::mostrarPerfil)
        )
    }

    private fun mostrarDetalle(p: Producto) {
        val l = pantallaBase()
        encabezado(l, "Detalle del producto")
        caja(l, "Imagen del producto", 110)
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
            aviso("Método seleccionado: Tarjeta")
        }

        botonSecundario(l, "PSE") {
            aviso("Método seleccionado: PSE")
        }

        boton(l, "Confirmar compra (demostración)") {
            if (carrito.isEmpty()) {
                aviso("El carrito está vacío")
            } else {
                val nombresProductos =
                    carrito.joinToString(", ") { it.nombre }

                val totalCompra = carrito.sumOf { it.precio }

                compras.add(
                    Compra(
                        "Usuario de ejemplo",
                        nombresProductos,
                        totalCompra,
                        "Pendiente"
                    )
                )

                carrito.clear()
                aviso("Compra registrada correctamente")
                mostrarInicioComprador()
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
        botonSecundario(l, "Mis compras") { aviso("Historial de compras de ejemplo") }
        botonSecundario(l, "Cerrar sesión") { mostrarBienvenida() }
        navegacion(
            l,
            listOf("Inicio", "Productos", "Carrito", "Perfil"),
            listOf(::mostrarInicioComprador, ::mostrarProductos, ::mostrarCarrito, ::mostrarPerfil)
        )
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
                mostrarLista(
                    "Productos",
                    productos.map { "${it.nombre} · ${dinero(it.precio)}" })
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

    private fun mostrarUsuarios() {
        val l = pantallaBase()
        encabezado(l, "Administrar usuarios")

        usuarios.forEach { usuario ->
            val fila = tarjeta(l)

            texto(fila, usuario.nombre, 16)
            texto(fila, usuario.correo, 13)
            texto(fila, usuario.rol, 13)

            botonPequeno(fila, "Editar") {
                mostrarFormularioUsuario(usuario)
            }

            botonPequeno(fila, "Eliminar") {
                usuarios.remove(usuario)
                mostrarUsuarios()
            }
        }

        botonSecundario(l, "Volver") {
            mostrarInicioGestion()
        }
    }

    private fun mostrarFormularioUsuario(usuario: Usuario) {
        val l = pantallaBase()
        encabezado(l, "Editar usuario")

        val nombre = campo(l, "Nombre completo")
        val correo = campo(l, "Correo electrónico")

        nombre.setText(usuario.nombre)
        correo.setText(usuario.correo)

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
            val nuevoCorreo = correo.text.toString().trim()

            if (nuevoNombre.isBlank() || nuevoCorreo.isBlank()) {
                aviso("Completa todos los campos")
            } else {
                usuario.nombre = nuevoNombre
                usuario.correo = nuevoCorreo
                usuario.rol = selectorRol.selectedItem.toString()

                aviso("Usuario actualizado")
                mostrarUsuarios()
            }
        }

        botonSecundario(l, "Cancelar") {
            mostrarUsuarios()
        }
    }

    private fun mostrarCompras() {
        val l = pantallaBase()
        encabezado(l, "Administrar compras")

        if (compras.isEmpty()) {
            texto(l, "No hay compras registradas.", 15)
        }

        compras.toList().forEach { compra ->
            val fila = tarjeta(l)

            texto(fila, compra.cliente, 16)
            texto(fila, "Productos: ${compra.productos}", 14)
            texto(fila, "Total: ${dinero(compra.total)}", 14)
            texto(fila, "Estado: ${compra.estado}", 14)

            botonPequeno(fila, "Cambiar estado") {
                compra.estado = when (compra.estado) {
                    "Pendiente" -> "En proceso"
                    "En proceso" -> "Entregada"
                    else -> "Pendiente"
                }
                mostrarCompras()
            }

            botonPequeno(fila, "Eliminar") {
                compras.remove(compra)
                mostrarCompras()
            }
        }

        botonSecundario(l, "Volver") {
            mostrarInicioGestion()
        }
    }

    private fun mostrarMisProductos() {
        val l = pantallaBase()
        encabezado(l, "Mis productos")
        boton(l, "+ Registrar") { mostrarFormularioProducto(null) }
        productos.forEach { p ->
            val fila = tarjeta(l)
            texto(fila, "${p.nombre} · ${dinero(p.precio)}", 15)
            botonPequeno(fila, "Editar") { mostrarFormularioProducto(p) }
            botonPequeno(fila, "Eliminar") { productos.remove(p); mostrarMisProductos() }
        }
        botonSecundario(l, "Volver") { mostrarInicioGestion() }
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
        caja(l, "Seleccionar una imagen (demostración)", 70)
        boton(l, if (producto == null) "Publicar producto" else "Guardar cambios") {
            val n = nombre.text.toString().ifBlank { "Nuevo producto" }
            val d = descripcion.text.toString().ifBlank { "Producto tecnológico." }
            val v = precio.text.toString().toIntOrNull() ?: 50000
            if (producto == null) productos.add(Producto(n, d, v))
            else {
                val i = productos.indexOf(producto); if (i >= 0) productos[i] = Producto(n, d, v)
            }
            aviso("Información guardada"); mostrarMisProductos()
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
        botonSecundario(l, "Cerrar sesión") { mostrarBienvenida() }
    }

    private fun tarjetaProducto(l: LinearLayout, p: Producto, agregar: Boolean) {
        val c = tarjeta(l)
        texto(c, "▣  ${p.nombre}", 16)
        texto(c, p.descripcion, 13)
        texto(c, dinero(p.precio), 15)
        botonPequeno(c, if (agregar) "Ver detalle" else "Ver") { mostrarDetalle(p) }
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
