package com.insert.hivedocs.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.insert.hivedocs.data.UserProfile
import com.google.firebase.firestore.ktx.toObject

fun checkUserProfile(uid: String, onResult: (UserProfile) -> Unit) {
    Log.d("checkUserProfile", "Verificando perfil para UID: $uid")
    FirebaseFirestore.getInstance().collection("users").document(uid).get()
        .addOnSuccessListener { document ->
            if (document != null && document.exists()) {
                val isAdminFromDb = document.getBoolean("isAdmin") ?: false
                val isBannedFromDb = document.getBoolean("isBanned") ?: false

                val userProfile = UserProfile(isAdmin = isAdminFromDb, isBanned = isBannedFromDb)
                Log.d("checkUserProfile", "Perfil encontrado: isAdmin=${userProfile?.isAdmin}, isBanned=${userProfile?.isBanned}")
                onResult(userProfile ?: UserProfile())
            } else {
                Log.w("checkUserProfile", "Documento de usuário não encontrado, retornando perfil padrão.")
                onResult(UserProfile())
            }
        }
        .addOnFailureListener { exception ->
            Log.e("checkUserProfile", "FALHA AO BUSCAR PERFIL!", exception)
            onResult(UserProfile())
        }
}