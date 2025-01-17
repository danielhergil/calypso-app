package com.danihg.calypsoapp.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
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
     * Adds a new team with the provided details and uploads the logo.
     */
    suspend fun addTeamWithLogo(
        context: Context,
        teamName: String,
        teamAlias: String,
        players: List<String>,
        logoUri: Uri
    ): Boolean {
        val user = auth.currentUser ?: return false
        val userId = user.uid

        // Extract the file extension, fallback to "png" if missing
        val extension = context.contentResolver.getType(logoUri)?.split("/")?.last() ?: "png"
        val logoPath = "$userId/$teamName.$extension"
        val storageRef = storage.reference.child(logoPath)

        return try {
            // Upload the logo to Firebase Storage
            val inputStream = context.contentResolver.openInputStream(logoUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, true)

            val baos = ByteArrayOutputStream()
            resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val data = baos.toByteArray()

            storageRef.putBytes(data).await() // Upload the resized image

            val downloadUrl = storageRef.downloadUrl.await().toString()

            // Add team with the logo path
            addTeam(teamName, teamAlias, players, downloadUrl)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getLogoDownloadUrl(logoPath: String): String? {
        return logoPath // Just return the URL since we're now storing the full URL
    }

//    suspend fun getLogoDownloadUrl(logoPath: String): String? {
//        Log.d("FirestoreManager", "Fetching logo at path: $logoPath")
//        return try {
//            val storageRef = storage.reference.child(logoPath)
//            storageRef.downloadUrl.await().toString()
//        } catch (e: Exception) {
//            Log.e("FirestoreManager", "Error fetching logo: ${e.message}")
//            null
//        }
//    }
}
