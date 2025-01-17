package com.danihg.calypsoapp.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

data class Team(
    val id: String = "",          // Firestore document ID
    val alias: String = "",       // Team alias
    val createdAt: Timestamp? = null,     // Timestamp for when the team was created
    val logo: String = "",        // URL for team logo
    val name: String = "",        // Team name
    val players: List<String> = emptyList() // List of players
)

class FirestoreManager {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun initializeUserData() {
        val user = auth.currentUser ?: return
        val userDocRef = db.collection("users").document(user.uid)

        try {
            val userDoc = userDocRef.get().await()
            if (!userDoc.exists()) {
                // Create user document if it doesn't exist
                val userData = hashMapOf(
                    "email" to user.email,
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                userDocRef.set(userData).await()

                // Create empty teams subcollection
                val teamsCollectionRef = userDocRef.collection("teams")
                val placeholderTeam = hashMapOf(
                    "name" to "Placeholder Team",
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                teamsCollectionRef.add(placeholderTeam).await()
            }
        } catch (e: Exception) {
            // Handle any errors here
            e.printStackTrace()
        }
    }

    suspend fun getTeams(): List<Team> {
        val user = auth.currentUser ?: return emptyList()
        val teamsCollectionRef = db.collection("users").document(user.uid).collection("teams")

        return try {
            val snapshot = teamsCollectionRef.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Team::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addTeam(teamName: String, teamAlias: String, players: List<String>, logoUrl: String) {
        val user = auth.currentUser ?: return
        val teamsCollectionRef = db.collection("users").document(user.uid).collection("teams")

        val newTeam = hashMapOf(
            "name" to teamName,
            "alias" to teamAlias,
            "players" to players,
            "logo" to logoUrl,
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        try {
            teamsCollectionRef.add(newTeam).await()
        } catch (e: Exception) {
            // Handle any errors here
            e.printStackTrace()
        }
    }

    // Add more functions for editing and removing teams as needed


    /**
     * Uploads a logo to Firebase Storage, resizes it to 50x50 pixels, and returns the URL.
     */
    suspend fun uploadTeamLogo(context: Context, uri: Uri, teamName: String): String? {
        val user = auth.currentUser ?: return null
        val storageRef = storage.reference
        val logoRef = storageRef.child("${user.uid}/${teamName}.png")

        return try {
            // Open InputStream using the provided Context
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 50, 50, true)

            // Convert the resized bitmap to bytes
            val baos = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val data = baos.toByteArray()

            // Upload the bytes to Firebase Storage
            logoRef.putBytes(data).await()
            logoRef.downloadUrl.await().toString() // Return the download URL
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Adds a new team with the provided details and uploads the logo.
     */
    suspend fun addTeamWithLogo(
        context: Context,
        teamName: String,
        teamAlias: String,
        players: List<String>,
        logoUri: Uri
    ): Boolean {
        val logoUrl = uploadTeamLogo(context, logoUri, teamName) ?: return false
        addTeam(teamName, teamAlias, players, logoUrl)
        return true
    }
}
