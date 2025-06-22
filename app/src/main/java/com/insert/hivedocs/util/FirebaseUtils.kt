package com.insert.hivedocs.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.insert.hivedocs.data.UserProfile

fun checkUserProfile(uid: String, onResult: (UserProfile?) -> Unit) {
    Log.d("checkUserProfile", "Verificando perfil para UID: $uid")
    FirebaseFirestore.getInstance().collection("users").document(uid).get()
        .addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                try {
                    val isAdminFromDb = document.getBoolean("isAdmin") ?: false
                    val isBannedFromDb = document.getBoolean("isBanned") ?: false

                    val userProfile = UserProfile(isAdmin = isAdminFromDb, isBanned = isBannedFromDb)

                    Log.d("checkUserProfile", "Perfil MAPEADO MANUALMENTE: isAdmin=${userProfile.isAdmin}, isBanned=${userProfile.isBanned}")
                    onResult(userProfile)

                } catch (e: Exception) {
                    Log.e("checkUserProfile", "Erro no mapeamento manual dos campos!", e)
                    onResult(UserProfile())
                }
            } else {
                Log.w("checkUserProfile", "Documento de usuário não encontrado para o UID: $uid")
                onResult(UserProfile())
            }
        }
        .addOnFailureListener { exception ->
            Log.e("checkUserProfile", "FALHA AO BUSCAR PERFIL! (Erro de Rede/Permissão)", exception)
            onResult(null)
        }
}