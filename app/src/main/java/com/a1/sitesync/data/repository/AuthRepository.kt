package com.a1.sitesync.data.repository

import com.a1.sitesync.data.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// A simple sealed class to represent authentication results
sealed class AuthResult {
    data class Success(val user: FirebaseUser) : AuthResult()
    data class Error(val exception: Exception) : AuthResult()
}

class AuthRepository(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private val usersCollection = firestore.collection("users")

    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success(authResult.user!!)
        } catch (e: Exception) {
            AuthResult.Error(e)
        }
    }

    suspend fun signUp(email: String, password: String, displayName: String, phoneNumber: String?, profilePictureUrl: String?): AuthResult {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user!!
            // Now save additional user data to Firestore
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                displayName = displayName,
                phoneNumber = phoneNumber,
                profilePictureUrl = profilePictureUrl
            )
            saveUserData(user)
            AuthResult.Success(firebaseUser)
        } catch (e: Exception) {
            AuthResult.Error(e)
        }
    }

    private suspend fun saveUserData(user: User) {
        usersCollection.document(user.uid).set(user).await()
    }

    suspend fun getUserData(uid: String): User? {
        return try {
            usersCollection.document(uid).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            // Handle exceptions (e.g., logging)
            null
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }
}