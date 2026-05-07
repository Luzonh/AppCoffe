package com.henryuide.pruebacoffe

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.henryuide.pruebacoffe.view.LiveObjectDetectionActivity

class GoogleAuthHelper(private val activity: LiveObjectDetectionActivity) {

    companion object {
        const val RC_SIGN_IN = 9001
    }

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val googleSignInClient: GoogleSignInClient

    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(activity, gso)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        activity.startActivityForResult(signInIntent, RC_SIGN_IN)
        activity.initCameraComponents()

    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    Log.d("GoogleAuthHelper", "signInWithCredential:success")
                    activity.onGoogleSignInSuccess()
                } else {
                    Log.w("GoogleAuthHelper", "signInWithCredential:failure", task.exception)
                    activity.showErrorMessage("Error de autenticación: ${task.exception?.message}")
                    }
                }
    }
}