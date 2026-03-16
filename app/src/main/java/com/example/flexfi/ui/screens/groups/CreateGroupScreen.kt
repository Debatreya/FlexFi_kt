package com.example.flexfi.ui.screens.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flexfi.ui.components.*
import com.example.flexfi.ui.screens.contacts.ContactViewModel
import com.example.flexfi.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    groupViewModel: GroupViewModel,
    contactViewModel: ContactViewModel,
    onGroupCreated: () -> Unit,
    onBack: () -> Unit = {}
) {
    var groupName by remember { mutableStateOf("") }
    val contacts by contactViewModel.contacts.collectAsState()
    val selectedMembers = remember { mutableStateListOf<String>() }

    Scaffold(
        containerColor = FlexFiGreySurface,
        topBar = {
            FlexFiTopBar(
                title = "Create Group",
                showBackButton = true,
                onBackClick = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Group details card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FlexFiWhite),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("GROUP DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = FlexFiBodyText, letterSpacing = 1.sp)

                    FlexFiTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = "Group Name",
                        placeholder = "e.g. Trip to Goa, Roommates",
                        leadingIcon = Icons.Default.Group
                    )
                }
            }

            // Members selection card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FlexFiWhite),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "SELECT MEMBERS (OPTIONAL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = FlexFiBodyText,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(8.dp))

                    if (contacts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No contacts yet. Add contacts first!", fontSize = 13.sp, color = FlexFiBodyText)
                        }
                    } else {
                        LazyColumn {
                            items(contacts) { contact ->
                                val isSelected = selectedMembers.contains(contact.phone)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clickable {
                                            if (isSelected) selectedMembers.remove(contact.phone)
                                            else selectedMembers.add(contact.phone)
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) FlexFiLightBlue else FlexFiGreySurface
                                    ),
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        FlexFiAvatar(
                                            name = contact.name,
                                            size = AvatarSize.SMALL,
                                            isGhost = contact.isGhost
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                contact.name,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 14.sp,
                                                color = FlexFiDarkText
                                            )
                                            Text(
                                                contact.phone,
                                                fontSize = 12.sp,
                                                color = FlexFiBodyText
                                            )
                                        }
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.CheckCircle,
                                                "Selected",
                                                tint = FlexFiBlue,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            FlexFiPrimaryButton(
                text = "Create Group",
                onClick = {
                    val selectedEntities = contacts.filter { selectedMembers.contains(it.phone) }
                    groupViewModel.createGroup(
                        name = groupName,
                        selectedContacts = selectedEntities,
                        onComplete = onGroupCreated
                    )
                },
                enabled = groupName.isNotBlank(),
                trailingIcon = {
                    Icon(Icons.Default.Check, null, tint = FlexFiWhite, modifier = Modifier.size(18.dp))
                }
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}
