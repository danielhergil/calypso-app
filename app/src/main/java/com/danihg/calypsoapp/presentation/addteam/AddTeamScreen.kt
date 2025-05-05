package com.danihg.calypsoapp.presentation.addteam

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.danihg.calypsoapp.data.FirestoreManager
import com.danihg.calypsoapp.data.Team
import com.danihg.calypsoapp.model.AddTeamViewModel
import com.danihg.calypsoapp.ui.theme.Black
import com.danihg.calypsoapp.ui.theme.CalypsoRed
import com.danihg.calypsoapp.ui.theme.Gray
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTeamScreen(
    firestoreManager: FirestoreManager,
    viewModel: AddTeamViewModel = viewModel()
) {
    val teams by viewModel.teams.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadTeams(firestoreManager)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Manage Teams",
                        color = Color.White,
                        style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Black)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 40.dp),
                onClick = { viewModel.showAddTeamDialog = true },
                containerColor = CalypsoRed,
                contentColor = Color.White,
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Team", tint = Color.White)
            }
        },
        containerColor = Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(teams) { team ->
                    TeamItem(
                        firestoreManager = firestoreManager,
                        team = team,
                        onTeamChanged = { viewModel.loadTeams(firestoreManager) }
                    )
                }
            }
        }
    }

    if (viewModel.showAddTeamDialog) {
        AddTeamDialog(
            firestoreManager = firestoreManager,
            onDismiss = { viewModel.showAddTeamDialog = false },
            onTeamAdded = {
                coroutineScope.launch {
                    viewModel.loadTeams(firestoreManager)
                }
            }
        )
    }
}

@Composable
fun TeamItem(
    firestoreManager: FirestoreManager,
    team: Team,
    onTeamChanged: () -> Unit // Callback to reload teams after edit or delete.
) {
    var expanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Gray),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    AsyncImage(
                        model = team.logo,
                        contentDescription = "Team Logo",
                        modifier = Modifier
                            .width(32.dp)
                            .height(24.dp)
                            .padding(end = 12.dp),
                        contentScale = ContentScale.Fit
                    )
                    Text(
                        text = team.name,
                        style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    )
                }
                Text(
                    text = team.alias,
                    style = TextStyle(fontSize = 16.sp, color = CalypsoRed)
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text("Players:", color = Color.LightGray, fontWeight = FontWeight.Medium)
                    team.players.forEach { player ->
                        Row(
                            modifier = Modifier
                                .padding(start = 16.dp, top = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(color = CalypsoRed, shape = CircleShape)
                            )
                            Text(
                                text = player,
                                color = Color.White,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    // Add Edit and Delete buttons.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { showEditDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = CalypsoRed)
                        ) {
                            Text("Edit", color = Color.White)
                        }
                        Button(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Delete", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditTeamDialog(
            firestoreManager = firestoreManager,
            team = team,
            onDismiss = { showEditDialog = false },
            onTeamUpdated = {
                showEditDialog = false
                onTeamChanged()
            }
        )
    }
    if (showDeleteDialog) {
        DeleteTeamDialog(
            firestoreManager = firestoreManager,
            team = team,
            onDismiss = { showDeleteDialog = false },
            onTeamDeleted = {
                showDeleteDialog = false
                onTeamChanged()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteTeamDialog(
    firestoreManager: FirestoreManager,
    team: Team,
    onDismiss: () -> Unit,
    onTeamDeleted: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Team", color = Color.White) },
        text = { Text("Are you sure you want to delete the team \"${team.name}\"?", color = Color.White) },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        val success = firestoreManager.deleteTeam(team)
                        if (success) {
                            onTeamDeleted()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = Gray,
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTeamDialog(
    firestoreManager: FirestoreManager,
    team: Team,
    onDismiss: () -> Unit,
    onTeamUpdated: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf(team.name) }
    var alias by remember { mutableStateOf(team.alias) }
    // For adding new players in edit mode.
    var newPlayerNameInput by remember { mutableStateOf("") }
    var newPlayerNumberInput by remember { mutableStateOf("") }
    var logoUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    // A simple data class for splitting player info.
    data class PlayerData(var name: String, var number: String)

    // Build a mutable state list from the existing team players.
    val playersData = remember {
        mutableStateListOf<PlayerData>().apply {
            team.players.forEach { playerStr ->
                val parts = playerStr.split(",")
                if (parts.size == 2)
                    add(PlayerData(parts[0], parts[1]))
                else
                    add(PlayerData(playerStr, ""))
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> logoUri = uri }
    )

    // FocusRequester for the new player's name input.
    val nameFocusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Team", color = Color.White) },
        text = {
            // Use a Box with fixed height and vertical scroll.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 20) name = it },
                        label = { Text("Team Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CalypsoRed,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = CalypsoRed,
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = alias,
                        onValueChange = { if (it.length <= 3) alias = it.uppercase() },
                        label = { Text("Alias (3 characters)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CalypsoRed,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = CalypsoRed,
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Players:", color = Color.LightGray, fontWeight = FontWeight.Medium)
                    // Existing players list.
                    playersData.forEachIndexed { index, playerData ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            OutlinedTextField(
                                value = playerData.name,
                                onValueChange = { newName ->
                                    playersData[index] = playerData.copy(name = newName)
                                },
                                label = { Text("Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CalypsoRed,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedLabelColor = CalypsoRed,
                                    unfocusedLabelColor = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = playerData.number,
                                onValueChange = { newNumber ->
                                    playersData[index] = playerData.copy(number = newNumber)
                                },
                                label = { Text("Number") },
                                singleLine = true,
                                modifier = Modifier.width(80.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CalypsoRed,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedLabelColor = CalypsoRed,
                                    unfocusedLabelColor = Color.Gray
                                )
                            )
                        }
                    }
                    // Show the row to add a new player only if under the limit.
                    if (playersData.size < 12) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newPlayerNameInput,
                                onValueChange = { if (it.length <= 20) newPlayerNameInput = it },
                                label = { Text("Player Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(nameFocusRequester),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CalypsoRed,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedLabelColor = CalypsoRed,
                                    unfocusedLabelColor = Color.Gray
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = newPlayerNumberInput,
                                onValueChange = { if (it.length <= 4) newPlayerNumberInput = it },
                                label = { Text("Number") },
                                singleLine = true,
                                modifier = Modifier.width(80.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = CalypsoRed,
                                    unfocusedBorderColor = Color.Gray,
                                    focusedLabelColor = CalypsoRed,
                                    unfocusedLabelColor = Color.Gray
                                )
                            )
                            IconButton(
                                onClick = {
                                    if (newPlayerNameInput.isNotBlank() && newPlayerNumberInput.isNotBlank()) {
                                        playersData.add(
                                            PlayerData(
                                                newPlayerNameInput.trim(),
                                                newPlayerNumberInput.trim()
                                            )
                                        )
                                        newPlayerNameInput = ""
                                        newPlayerNumberInput = ""
                                        // Request focus again if still under the limit.
                                        if (playersData.size < 12) {
                                            nameFocusRequester.requestFocus()
                                        }
                                    }
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Player", tint = CalypsoRed)
                            }
                        }
                    }
                    if (playersData.size >= 12) {
                        Text("Maximum players reached", color = Color.Red, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CalypsoRed)
                    ) {
                        Text("Change Logo")
                    }
                    logoUri?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = it,
                            contentDescription = "Logo Preview",
                            modifier = Modifier
                                .size(100.dp)
                                .padding(8.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (isUploading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(color = CalypsoRed)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && alias.isNotBlank()) {
                        coroutineScope.launch {
                            isUploading = true
                            // Convert playersData to a list of "name,number" strings.
                            val updatedPlayers = playersData
                                .filter{ it.name.isNotBlank() && it.number.isNotBlank() }
                                .map{ "${it.name.trim()},${it.number.trim()}" }
                            val success = firestoreManager.updateTeam(
                                context = context,
                                team = team,
                                newTeamName = name,
                                newTeamAlias = alias,
                                newPlayers = updatedPlayers,
                                newLogoUri = logoUri
                            )
                            isUploading = false
                            if (success) {
                                onTeamUpdated()
                            } else {
                                // Handle failure (e.g., show a toast)
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CalypsoRed)
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = Gray,
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTeamDialog(
    firestoreManager: FirestoreManager,
    onDismiss: () -> Unit,
    onTeamAdded: () -> Unit // Notify parent when a team is added
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var alias by remember { mutableStateOf("") }
    // Instead of a single input, use two inputs: one for player name and one for number.
    var playerNameInput by remember { mutableStateOf("") }
    var playerNumberInput by remember { mutableStateOf("") }
    var players by remember { mutableStateOf(listOf<String>()) }
    var logoUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            logoUri = uri
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Team", color = Color.White) },
        text = {
            // Make the dialog content scrollable:
            Box(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .heightIn(max = 400.dp)
            ) {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 20) name = it },
                        label = { Text("Team Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CalypsoRed,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = CalypsoRed,
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = alias,
                        onValueChange = { if (it.length <= 3) alias = it.uppercase() },
                        label = { Text("Alias (3 characters)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CalypsoRed,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = CalypsoRed,
                            unfocusedLabelColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    // Row for adding a player (name and number)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = playerNameInput,
                            onValueChange = { if (it.length <= 20) playerNameInput = it },
                            label = { Text("Player Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CalypsoRed,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = CalypsoRed,
                                unfocusedLabelColor = Color.Gray
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = playerNumberInput,
                            onValueChange = { if (it.length <= 4) playerNumberInput = it },
                            label = { Text("Number") },
                            singleLine = true,
                            modifier = Modifier.width(80.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CalypsoRed,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = CalypsoRed,
                                unfocusedLabelColor = Color.Gray
                            )
                        )
                        IconButton(
                            onClick = {
                                if (playerNameInput.isNotBlank() && playerNumberInput.isNotBlank() && players.size < 12) {
                                    // Save the player as "name,number"
                                    players = players + "${playerNameInput.trim()},${playerNumberInput.trim()}"
                                    playerNameInput = ""
                                    playerNumberInput = ""
                                }
                            },
                            modifier = Modifier.padding(start = 8.dp),
                            enabled = players.size < 12 // disable if already 12 players
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Player", tint = CalypsoRed)
                        }
                    }
                    // Optionally, show a message when maximum is reached:
                    if (players.size >= 12) {
                        Text("Maximum players reached", color = Color.Red, fontSize = 12.sp)
                    }
                    // Display added players in a friendly format.
                    LazyColumn(
                        modifier = Modifier.height(100.dp)
                    ) {
                        items(players) { player ->
                            val parts = player.split(",")
                            val displayText = if (parts.size == 2) {
                                "Name: ${parts[0]} – Number: ${parts[1]}"
                            } else player
                            Text(displayText, color = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CalypsoRed)
                    ) {
                        Text("Upload Logo")
                    }
                    logoUri?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = it,
                            contentDescription = "Logo Preview",
                            modifier = Modifier
                                .size(100.dp)
                                .padding(8.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    if (isUploading) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(color = CalypsoRed)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && alias.isNotBlank() && logoUri != null) {
                        coroutineScope.launch {
                            isUploading = true
                            val success = firestoreManager.addTeamWithLogo(context, name, alias, players, logoUri!!)
                            isUploading = false
                            if (success) {
                                onTeamAdded()
                                onDismiss()
                            } else {
                                // Handle upload failure (e.g., show a toast)
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CalypsoRed)
            ) {
                Text("Add Team")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = Gray,
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}
