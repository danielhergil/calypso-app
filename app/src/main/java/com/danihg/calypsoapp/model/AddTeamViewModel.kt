package com.danihg.calypsoapp.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.danihg.calypsoapp.data.FirestoreManager
import com.danihg.calypsoapp.data.Team
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddTeamViewModel : ViewModel() {
    private val _teams = MutableStateFlow<List<Team>>(emptyList())
    val teams = _teams.asStateFlow()

    var showAddTeamDialog by mutableStateOf(false)

    fun loadTeams(firestoreManager: FirestoreManager) {
        viewModelScope.launch {
            _teams.value = firestoreManager.getTeams()
        }
    }
}