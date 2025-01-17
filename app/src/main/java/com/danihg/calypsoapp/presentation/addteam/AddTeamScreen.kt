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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                    TeamItem(firestoreManager, team)
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
fun TeamItem(firestoreManager: FirestoreManager, team: Team) {
    var expanded by remember { mutableStateOf(false) }
//    var logoUrl by remember { mutableStateOf<String?>(null) }
//    var isLoading by remember { mutableStateOf(true) }
//    val coroutineScope = rememberCoroutineScope()
//
//    LaunchedEffect(team.logo) {
//        coroutineScope.launch {
//            isLoading = true
//            try {
//                logoUrl = firestoreManager.getLogoDownloadUrl(team.logo)
//            } catch (e: Exception) {
//                // Handle error if needed
//            } finally {
//                isLoading = false
//            }
//        }
//    }

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
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(team.logo)
                            .crossfade(true)
                            .memoryCacheKey(team.logo)
                            .diskCacheKey(team.logo)
                            .build(),
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
                            // Bullet point
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(
                                        color = CalypsoRed,
                                        shape = CircleShape
                                    )
                            )
                            // Player name
                            Text(
                                text = player,
                                color = Color.White,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
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
    var playerInput by remember { mutableStateOf("") }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = playerInput,
                        onValueChange = { if (it.length <= 20) playerInput = it },
                        label = { Text("Add Player") },
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
                    IconButton(
                        onClick = {
                            if (playerInput.isNotBlank()) {
                                players = players + playerInput
                                playerInput = ""
                            }
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Player", tint = CalypsoRed)
                    }
                }
                LazyColumn(
                    modifier = Modifier.height(100.dp)
                ) {
                    items(players) { player ->
                        Text(player, color = Color.White)
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