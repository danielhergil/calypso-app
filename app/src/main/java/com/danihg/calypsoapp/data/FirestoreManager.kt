package com.danihg.calypsoapp.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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

// Data class for RTMP configurations.
// The constructedUrl property provides the final URL by combining rtmpUrl and streamKey.
data class RTMPConfig(
    val alias: String = "",
    val rtmpUrl: String = "",
    val streamKey: String = ""
) {
    val constructedUrl: String
        get() = rtmpUrl.trim().removeSuffix("/") + "/" + streamKey.trim()
}

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

    suspend fun updateTeam(
        context: Context,
        team: Team,
        newTeamName: String,
        newTeamAlias: String,
        newPlayers: List<String>,
        newLogoUri: Uri? = null
    ): Boolean {
        val user = auth.currentUser ?: return false
        val teamDocRef = db.collection("users").document(user.uid).collection("teams").document(team.id)
        // Prepare the update data.
        val updateData = hashMapOf<String, Any>(
            "name" to newTeamName,
            "alias" to newTeamAlias,
            "players" to newPlayers
        )
        // If a new logo is provided, upload it and update the logo field.
        if (newLogoUri != null) {
            val extension = context.contentResolver.getType(newLogoUri)?.split("/")?.last() ?: "png"
            val logoPath = "$user.uid/$newTeamName.$extension"
            val storageRef = storage.reference.child(logoPath)
            try {
                val inputStream = context.contentResolver.openInputStream(newLogoUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 50, 50, true)
                val baos = ByteArrayOutputStream()
                resizedBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                val data = baos.toByteArray()
                storageRef.putBytes(data).await() // Upload the new logo.
                val downloadUrl = storageRef.downloadUrl.await().toString()
                updateData["logo"] = downloadUrl
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
        return try {
            teamDocRef.update(updateData).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteTeam(team: Team): Boolean {
        val user = auth.currentUser ?: return false
        val teamDocRef = db.collection("users").document(user.uid).collection("teams").document(team.id)
        // Delete the logo file from Firebase Storage.
        try {
            val storageRef = storage.getReferenceFromUrl(team.logo)
            storageRef.delete().await()
        } catch (e: Exception) {
            e.printStackTrace()
            // Continue even if deletion of the image fails.
        }
        return try {
            teamDocRef.delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getLogoDownloadUrl(logoPath: String): String? {
        return logoPath // Just return the URL since we're now storing the full URL
    }

    /**
     * Saves the provided RTMP configuration to Firestore under the current user's subcollection "rtmp_configs".
     * Here the document ID is set to the alias so that the RTMP configuration is unique per alias.
     */
    suspend fun saveRTMPConfig(config: RTMPConfig): Boolean {
        val user = auth.currentUser ?: return false
        // Use the alias as the document ID (you may choose another unique field if preferred)
        val docRef = db.collection("users").document(user.uid)
            .collection("rtmp_configs").document(config.alias)
        val data = hashMapOf(
            "alias" to config.alias,
            "rtmpUrl" to config.rtmpUrl,
            "streamKey" to config.streamKey,
            "constructedUrl" to config.constructedUrl,
            "createdAt" to Timestamp.now()
        )

        return try {
            docRef.set(data).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Retrieves all saved RTMP configurations for the current user.
     */
    suspend fun getRTMPConfigs(): List<RTMPConfig> {
        val user = auth.currentUser ?: return emptyList()
        return try {
            val snapshot = db.collection("users").document(user.uid)
                .collection("rtmp_configs").get().await()
            snapshot.documents.mapNotNull { doc ->
                val alias = doc.getString("alias")
                val rtmpUrl = doc.getString("rtmpUrl")
                val streamKey = doc.getString("streamKey")
                if (!alias.isNullOrBlank() && !rtmpUrl.isNullOrBlank() && !streamKey.isNullOrBlank()) {
                    RTMPConfig(alias, rtmpUrl, streamKey)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Deletes the RTMP configuration identified by the provided alias.
     */
    suspend fun deleteRTMPConfig(alias: String): Boolean {
        val user = auth.currentUser ?: return false
        return try {
            db.collection("users").document(user.uid)
                .collection("rtmp_configs").document(alias).delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
