package com.example.wol

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisation des éléments d'interface
        val etMacAddress: EditText = findViewById(R.id.etMacAddress)
        val etBroadcastIP: EditText = findViewById(R.id.etBroadcastIP)
        val etPort: EditText = findViewById(R.id.etPort)
        val btnWakePC: Button = findViewById(R.id.btnWakePC)
        val btnUpdateInfo: Button = findViewById(R.id.btnUpdateInfo)
        val btnDeleteInfo: Button = findViewById(R.id.btnDeleteInfo)

        // Initialisation de SharedPreferences
        sharedPreferences = getSharedPreferences("PC_Info", MODE_PRIVATE)

        // Charger les infos sauvegardées
        etMacAddress.setText(sharedPreferences.getString("MAC_ADDRESS", ""))
        etBroadcastIP.setText(sharedPreferences.getString("BROADCAST_IP", ""))
        etPort.setText(sharedPreferences.getInt("PORT", 9).toString())

        // Action pour réveiller l'ordinateur
        btnWakePC.setOnClickListener {
            val macAddress = etMacAddress.text.toString()
            val broadcastIP = etBroadcastIP.text.toString()
            val port = etPort.text.toString().toIntOrNull() ?: 9

            if (macAddress.isNotBlank() && broadcastIP.isNotBlank()) {
                sendWakeOnLanPacket(macAddress, broadcastIP, port)
            } else {
                Toast.makeText(this, "Veuillez remplir toutes les informations.", Toast.LENGTH_SHORT).show()
            }
        }

        // Action pour mettre à jour les infos
        btnUpdateInfo.setOnClickListener {
            val macAddress = etMacAddress.text.toString()
            val broadcastIP = etBroadcastIP.text.toString()
            val port = etPort.text.toString().toIntOrNull() ?: 9

            with(sharedPreferences.edit()) {
                putString("MAC_ADDRESS", macAddress)
                putString("BROADCAST_IP", broadcastIP)
                putInt("PORT", port)
                apply()
            }

            Toast.makeText(this, "Informations mises à jour.", Toast.LENGTH_SHORT).show()
        }

        // Action pour supprimer les infos
        btnDeleteInfo.setOnClickListener {
            with(sharedPreferences.edit()) {
                clear()
                apply()
            }

            etMacAddress.text.clear()
            etBroadcastIP.text.clear()
            etPort.text.clear()

            Toast.makeText(this, "Informations supprimées.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendWakeOnLanPacket(macAddress: String, broadcastIP: String, port: Int) {
        Thread {
            try {
                val macBytes = macAddress.split(":").map { it.toInt(16).toByte() }.toByteArray()

                // Construction du paquet magique
                val magicPacket = ByteArray(102)
                for (i in 0 until 6) {
                    magicPacket[i] = 0xFF.toByte()
                }
                for (i in 1..16) {
                    System.arraycopy(macBytes, 0, magicPacket, i * 6, macBytes.size)
                }

                // Envoi du paquet
                val address = InetAddress.getByName(broadcastIP)
                val packet = DatagramPacket(magicPacket, magicPacket.size, address, port)
                DatagramSocket().use { socket ->
                    socket.send(packet)
                }

                runOnUiThread {
                    Toast.makeText(this, "Paquet envoyé avec succès.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Erreur lors de l'envoi du paquet.", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}
