package com.danihg.calypsoapp.utils

// CameraComponents.kt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danihg.calypsoapp.R

@Composable
fun <T> ModernDropdown(
    items: List<T>,
    enabled: Boolean = true,
    selectedValue: T,
    displayMapper: (T) -> String,
    onValueChange: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier
        .fillMaxWidth()
        .background(Color.Gray.copy(alpha = 0.2f), CircleShape)
        .padding(4.dp)) {
        Row(
            modifier = Modifier
                .then(
                    if (enabled) Modifier.clickable { expanded = true }
                    else Modifier // Prevent clicks when disabled
                )
                .padding(4.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayMapper(selectedValue),
                color = if (enabled) Color.White else Color.Gray,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = if (enabled) Color.White else Color.Gray
            )
        }

        DropdownMenu(
            expanded = enabled && expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.Gray)
        ) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = displayMapper(item),
                            color = if (item == selectedValue) Color.Red else Color.White,
                            fontSize = 16.sp
                        )
                    },
                    onClick = {
                        onValueChange(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SectionSubtitle(title: String) {
    Text(title, style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Color.LightGray), modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
fun ScoreboardActionButtons(
    onLeftButtonClick: () -> Unit,
    onRightButtonClick: () -> Unit,
    onLeftDecrement: () -> Unit,
    onRightDecrement: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 120.dp, top = 50.dp),
            horizontalArrangement = Arrangement.spacedBy(55.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left group: decrease button to the left of add button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp) // adjust spacing as needed
            ) {
                AuxButton(
                    modifier = Modifier.size(30.dp),
                    painter = painterResource(id = R.drawable.ic_decrease),
                    onClick = onLeftDecrement
                )
                AuxButton(
                    modifier = Modifier.size(30.dp),
                    painter = painterResource(id = R.drawable.ic_add),
                    onClick = onLeftButtonClick
                )
            }

            // Right group: add button with decrease button to its right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp) // adjust spacing as needed
            ) {
                AuxButton(
                    modifier = Modifier.size(30.dp),
                    painter = painterResource(id = R.drawable.ic_add),
                    onClick = onRightButtonClick
                )
                AuxButton(
                    modifier = Modifier.size(30.dp),
                    painter = painterResource(id = R.drawable.ic_decrease),
                    onClick = onRightDecrement
                )
            }
        }
    }
}