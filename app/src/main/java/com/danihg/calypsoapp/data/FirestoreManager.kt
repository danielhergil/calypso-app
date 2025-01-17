package com.danihg.calypsoapp.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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
}
