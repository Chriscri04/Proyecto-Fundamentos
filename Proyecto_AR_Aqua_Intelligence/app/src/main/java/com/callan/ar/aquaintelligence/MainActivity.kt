package com.callan.ar.aquaintelligence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val db: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().reference.child("piscina1")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PiscinaScreenRealtime(piscinaRef = db)
                }
            }
        }
    }
}

enum class EstadoOxigeno {
    NORMAL, ADVERTENCIA, CRITICO
}

data class PiscinaUiState(
    val oxigenoMgL: Double = 0.0,
    val temperatura: Double = 0.0,
    val voltajeMv: Double = 0.0,
    val rawAdc: Int = 0,
    val wifiRssi: Int = 0,
    val timestamp: Long = 0L
)

@Composable
fun PiscinaScreenRealtime(piscinaRef: DatabaseReference) {
    var uiState by remember { mutableStateOf(PiscinaUiState()) }

    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sensor = snapshot.child("sensor")
                val meta = snapshot.child("meta")

                uiState = PiscinaUiState(
                    oxigenoMgL = sensor.child("oxigeno_mgL").getValue(Double::class.java) ?: 0.0,
                    temperatura = sensor.child("temperatura").getValue(Double::class.java) ?: 0.0,
                    voltajeMv = sensor.child("voltaje_mV").getValue(Double::class.java) ?: 0.0,
                    rawAdc = sensor.child("raw_adc").getValue(Int::class.java) ?: 0,
                    wifiRssi = meta.child("wifi_rssi").getValue(Int::class.java) ?: 0,
                    timestamp = meta.child("timestamp").getValue(Long::class.java) ?: 0L
                )
            }

            override fun onCancelled(error: DatabaseError) {
                // Si quieres, aquí luego mostramos un mensaje de error
            }
        }

        piscinaRef.addValueEventListener(listener)
        onDispose { piscinaRef.removeEventListener(listener) }
    }

    val estado = calcularEstado(uiState.oxigenoMgL)
    val colorEstado = colorPorEstado(estado)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "AquaControl IA",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Piscina 1",
            style = MaterialTheme.typography.titleMedium
        )

        // Tarjeta principal
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Oxígeno disuelto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = String.format(Locale.US, "%.2f mg/L", uiState.oxigenoMgL),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                EstadoBadge(estado = estado, color = colorEstado)

                HorizontalDivider()

                InfoRow("Temperatura", String.format(Locale.US, "%.1f °C", uiState.temperatura))
                InfoRow("Voltaje sensor", String.format(Locale.US, "%.0f mV", uiState.voltajeMv))
                InfoRow("RAW ADC", uiState.rawAdc.toString())
                InfoRow("WiFi RSSI", "${uiState.wifiRssi} dBm")
                InfoRow("Última actualización", formatearTimestamp(uiState.timestamp))
            }
        }

        // Referencia de rangos
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Rangos (demo)", fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("• Normal: ≥ 5.0 mg/L")
                Text("• Advertencia: 3.0 – 4.99 mg/L")
                Text("• Crítico: < 3.0 mg/L")
            }
        }
    }
}

@Composable
fun EstadoBadge(estado: EstadoOxigeno, color: Color) {
    val texto = when (estado) {
        EstadoOxigeno.NORMAL -> "✅ Normal"
        EstadoOxigeno.ADVERTENCIA -> "⚠️ Advertencia"
        EstadoOxigeno.CRITICO -> "🚨 Crítico"
    }

    Box(
        modifier = Modifier
            .background(color = color.copy(alpha = 0.18f), shape = RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = texto,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

fun calcularEstado(oxigenoMgL: Double): EstadoOxigeno {
    return when {
        oxigenoMgL < 3.0 -> EstadoOxigeno.CRITICO
        oxigenoMgL < 5.0 -> EstadoOxigeno.ADVERTENCIA
        else -> EstadoOxigeno.NORMAL
    }
}

fun colorPorEstado(estado: EstadoOxigeno): Color {
    return when (estado) {
        EstadoOxigeno.NORMAL -> Color(0xFF2E7D32)       // verde
        EstadoOxigeno.ADVERTENCIA -> Color(0xFFF57C00)  // naranja
        EstadoOxigeno.CRITICO -> Color(0xFFD32F2F)      // rojo
    }
}

fun formatearTimestamp(ts: Long): String {
    if (ts <= 0L) return "Sin dato"
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(ts * 1000)) // tu ESP32 manda epoch en segundos
}