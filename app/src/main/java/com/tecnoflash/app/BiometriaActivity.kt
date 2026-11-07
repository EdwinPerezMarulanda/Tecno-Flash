package com.tecnoflash.app

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class BiometriaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_biometrica)

        val btnVerificar = findViewById<Button>(R.id.btnVerificarHuella)
        val btnVolver = findViewById<Button>(R.id.btnVolver)

        btnVerificar.setOnClickListener {
            verificarBiometria()
        }

        btnVolver.setOnClickListener {
            finish()
        }
    }

    private fun verificarBiometria() {
        val biometricManager = BiometricManager.from(this)

        val resultado = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        )

        if (resultado != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(
                this,
                "El dispositivo no tiene biometría disponible o configurada.",
                Toast.LENGTH_LONG
            ).show()
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

                    Toast.makeText(
                        this@BiometriaActivity,
                        "Autenticación correcta",
                        Toast.LENGTH_SHORT
                    ).show()

                    finish()
                }

                override fun onAuthenticationError(
                    errorCode: Int,
                    errString: CharSequence
                ) {
                    super.onAuthenticationError(errorCode, errString)

                    Toast.makeText(
                        this@BiometriaActivity,
                        errString,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()

                    Toast.makeText(
                        this@BiometriaActivity,
                        "Huella no reconocida. Inténtalo nuevamente.",
                        Toast.LENGTH_SHORT
                    ).show()
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
}
