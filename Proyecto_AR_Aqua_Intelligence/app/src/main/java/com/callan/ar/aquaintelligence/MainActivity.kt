package com.callan.ar.aquaintelligence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.database.*
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private val db: DatabaseReference by lazy {
        FirebaseDatabase.getInstance().reference.child("piscina1")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                RealtimeScreen(db)
            }
        }
    }
}

@Composable
fun RealtimeScreen(piscinaRef: DatabaseReference) {
    // Estados para mostrar en UI
    var oxigeno by remember { mutableStateOf(0.0) }
    var temperatura by remember { mutableStateOf(0.0) }
    var estadoRele by remember { mutableStateOf(false) }

    // Listener en tiempo real (lee de Firebase)
    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                oxigeno = snapshot.child("oxigeno").getValue(Double::class.java) ?: 0.0
                temperatura = snapshot.child("temperatura").getValue(Double::class.java) ?: 0.0
                estadoRele = snapshot.child("estado_rele").getValue(Boolean::class.java) ?: false
            }

            override fun onCancelled(error: DatabaseError) {
                // Si quieres, aquí puedes loguear el error
            }
        }

        piscinaRef.addValueEventListener(listener)
        onDispose { piscinaRef.removeEventListener(listener) }
    }

    // ✅ Simulación automática (escribe oxígeno cada 2 segundos)
    LaunchedEffect(Unit) {
        while (true) {
            val nuevoOx = (0..80).random() / 10.0 // 0.0 a 8.0
            piscinaRef.child("oxigeno").setValue(nuevoOx)
            piscinaRef.child("ultima_actualizacion").setValue(ServerValue.TIMESTAMP)
            delay(2000) // cada 2 segundos
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Piscina 1", style = MaterialTheme.typography.headlineMedium)

        Text("Oxígeno: $oxigeno")
        Text("Temperatura: $temperatura")

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Oxigenador (Relé): ")
            Switch(
                checked = estadoRele,
                onCheckedChange = { nuevo ->
                    piscinaRef.child("estado_rele").setValue(nuevo)
                    piscinaRef.child("ultima_actualizacion").setValue(ServerValue.TIMESTAMP)
                }
            )
        }

        // Este botón ya no es necesario, pero lo dejo por si quieres probar manual
        Button(onClick = {
            val nuevoOx = (0..80).random() / 10.0
            piscinaRef.child("oxigeno").setValue(nuevoOx)
            piscinaRef.child("ultima_actualizacion").setValue(ServerValue.TIMESTAMP)
        }) {
            Text("Simular oxígeno (manual)")
        }
    }
}