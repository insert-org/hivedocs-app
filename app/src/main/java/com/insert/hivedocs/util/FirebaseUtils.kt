package com.insert.hivedocs.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.insert.hivedocs.data.UserProfile

fun checkUserRole(uid: String, onResult: (Boolean) -> Unit) {
    Log.d("checkUserRole", "Iniciando verificação para UID: $uid")
    val db = FirebaseFirestore.getInstance()
    db.collection("users").document(uid).get()
        .addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val isAdminResult = document.getBoolean("isAdmin") ?: false
                Log.d("checkUserRole", "SUCESSO na leitura direta! Valor de 'isAdmin': $isAdminResult")
                onResult(isAdminResult)
            } else {
                Log.w("checkUserRole", "Documento não encontrado para o UID: $uid")
                onResult(false)
            }
        }
        .addOnFailureListener { exception ->
            Log.e("checkUserRole", "FALHA AO BUSCAR DOCUMENTO!", exception)
            onResult(false)
        }
}