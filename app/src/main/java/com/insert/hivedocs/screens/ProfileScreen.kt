package com.insert.hivedocs.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.insert.hivedocs.R

@Composable
fun ProfileScreen(auth: FirebaseAuth, isAdmin: Boolean) {
    val context = LocalContext.current
    val currentUser = auth.currentUser

    var showPasswordDialog by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordLoading by remember { mutableStateOf(false) }

    val isEmailPasswordUser = currentUser?.providerData?.any {
        it.providerId == EmailAuthProvider.PROVIDER_ID
    } ?: false

    fun handleChangePassword() {
        if (newPassword.isBlank() || confirmPassword.isBlank()) {
            Toast.makeText(context, "Por favor, preencha ambos os campos.", Toast.LENGTH_SHORT).show()
            return
        }
        if (newPassword != confirmPassword) {
            Toast.makeText(context, "As senhas não coincidem.", Toast.LENGTH_SHORT).show()
            return
        }

        isPasswordLoading = true
        currentUser?.updatePassword(newPassword)?.addOnCompleteListener { task ->
            isPasswordLoading = false
            if (task.isSuccessful) {
                Toast.makeText(context, "Senha alterada com sucesso!", Toast.LENGTH_SHORT).show()
                showPasswordDialog = false
                newPassword = ""
                confirmPassword = ""
            } else {
                val errorMessage = when (task.exception) {
                    is FirebaseAuthWeakPasswordException -> "A senha é muito fraca. Use pelo menos 6 caracteres."
                    else -> "Falha ao alterar a senha. Tente fazer login novamente."
                }
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Perfil", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (currentUser != null) {
            Text("Logado como:", style = MaterialTheme.typography.bodyLarge)
            Text(currentUser.email ?: "Email não disponível", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Função: ${if (isAdmin) "Administrador" else "Usuário"}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(24.dp))

            if (isEmailPasswordUser) {
                Button(onClick = { showPasswordDialog = true }) {
                    Icon(
                        painter = painterResource(R.drawable.lock_solid),
                        contentDescription = "Alterar Senha",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Alterar Senha")
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(onClick = { auth.signOut() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                Icon(
                    painter = painterResource(R.drawable.right_from_bracket_solid),
                    contentDescription = "Sair",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sair da Conta")
            }
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Alterar Senha") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Nova Senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirmar Nova Senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                    if(isPasswordLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            },
            confirmButton = {
                Button(onClick = { handleChangePassword() }, enabled = !isPasswordLoading) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }, enabled = !isPasswordLoading) {
                    Text("Cancelar")
                }
            }
        )
    }
}