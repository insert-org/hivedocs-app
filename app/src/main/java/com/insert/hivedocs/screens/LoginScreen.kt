package com.insert.hivedocs.screens

import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.insert.hivedocs.R
import com.insert.hivedocs.data.UserProfile

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun handleEmailLogin() {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Preencha e-mail e senha.", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    Toast.makeText(context, "Login bem-sucedido!", Toast.LENGTH_SHORT).show()
                    onLoginSuccess()
                } else {
                    val error = (task.exception as? FirebaseAuthException)?.errorCode
                    val message = when (error) {
                        "ERROR_WRONG_PASSWORD" -> "Senha incorreta."
                        "ERROR_USER_NOT_FOUND" -> "Usuário não encontrado."
                        "ERROR_INVALID_EMAIL" -> "Formato de e-mail inválido."
                        else -> "Falha no login."
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
    }

    fun handleEmailSignUp() {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Preencha e-mail e senha para registrar.", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = task.result?.user
                    if (firebaseUser != null) {
                        val newUserProfile = UserProfile(isAdmin = false)
                        FirebaseFirestore.getInstance().collection("users").document(firebaseUser.uid)
                            .set(newUserProfile)
                            .addOnSuccessListener {
                                isLoading = false
                                Toast.makeText(context, "Registro bem-sucedido!", Toast.LENGTH_SHORT).show()
                                onLoginSuccess()
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                Log.e("SignUp", "Erro ao criar perfil de usuário.", e)
                                Toast.makeText(context, "Erro ao finalizar registro.", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    isLoading = false
                    val error = (task.exception as? FirebaseAuthException)?.errorCode
                    val message = when (error) {
                        "ERROR_EMAIL_ALREADY_IN_USE" -> "Este e-mail já está em uso."
                        "ERROR_WEAK_PASSWORD" -> "A senha deve ter no mínimo 6 caracteres."
                        "ERROR_INVALID_EMAIL" -> "Formato de e-mail inválido."
                        else -> "Falha no registro."
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        isLoading = true
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
            auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                isLoading = false
                if (authTask.isSuccessful) {
                    onLoginSuccess()
                } else {
                    Toast.makeText(context, "Erro ao autenticar com Google.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: ApiException) {
            isLoading = false
            Toast.makeText(context, "Login com Google cancelado ou falhou.", Toast.LENGTH_SHORT).show()
            Log.e("LoginScreen", "Google sign in failed", e)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bem-vindo ao HiveDocs", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Senha") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { handleEmailLogin() },
                enabled = !isLoading,
                modifier = Modifier.weight(1f)
            ) {
                Text("Entrar")
            }
            Button(
                onClick = { handleEmailSignUp() },
                enabled = !isLoading,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Registrar")
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 24.dp)
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text("OU", modifier = Modifier.padding(horizontal = 8.dp))
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Button(
            onClick = {
                isLoading = true
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                launcher.launch(googleSignInClient.signInIntent)
            },
            enabled = !isLoading
        ) {
            Text("Login com Google")
        }
    }
}