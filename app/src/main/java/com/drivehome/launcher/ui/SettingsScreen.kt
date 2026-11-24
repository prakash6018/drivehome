package com.drivehome.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivehome.launcher.R
import com.drivehome.launcher.data.DriverSide
import com.drivehome.launcher.data.LauncherSettings
import com.drivehome.launcher.data.ThemeMode
import com.drivehome.launcher.ui.theme.Accents
import com.drivehome.launcher.ui.theme.LocalPalette
import com.drivehome.launcher.ui.theme.Orbitron
import com.drivehome.launcher.ui.theme.labelStyle

class SettingsActions(
    val onUpdate: ((LauncherSettings) -> LauncherSettings) -> Unit,
    val onMediaAccess: () -> Unit,
    val onLocation: () -> Unit,
    val onUsageAccess: () -> Unit,
    val onChooseHomeApp: () -> Unit,
    val onOpenOriginalLauncher: () -> Unit,
    val onAndroidSettings: () -> Unit,
    val onDiagnostics: () -> Unit,
    val onBack: () -> Unit,
)

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    mediaAccess: Boolean,
    usageAccess: Boolean,
    hasLocation: Boolean,
    actions: SettingsActions,
) {
    val p = LocalPalette.current
    BackHandler(onBack = actions.onBack)

    AmbientBackdrop(art = null, glowFromRight = settings.driverSide == DriverSide.RIGHT, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            ScreenHeader("Settings", onBack = actions.onBack)
            LazyColumn(
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { Section("Layout") }
                item {
                    SettingRow("Driver side", "Big buttons move to the side closest to you. India: Right.") {
                        Segmented(
                            listOf("Left", "Right"),
                            if (settings.driverSide == DriverSide.LEFT) 0 else 1,
                        ) { i -> actions.onUpdate { it.copy(driverSide = if (i == 0) DriverSide.LEFT else DriverSide.RIGHT) } }
                    }
                }
                item {
                    SettingRow("Clock", null) {
                        Segmented(listOf("12-hour", "24-hour"), if (settings.use24h) 1 else 0) { i ->
                            actions.onUpdate { it.copy(use24h = i == 1) }
                        }
                    }
                }
                item {
                    SettingRow("Car name", "Shown on the trip widget.") {
                        SettingTextField(
                            settings.carName,
                            onValueChange = { v -> actions.onUpdate { it.copy(carName = v.take(24)) } },
                            textStyle = TextStyle(fontFamily = Orbitron, fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 2.sp, color = p.text),
                        )
                    }
                }

                item { Section("Navigation") }
                item {
                    SettingRow("Home address", "One tap on the map widget starts navigation here.") {
                        SettingTextField(
                            settings.homeAddress,
                            placeholder = "e.g. 12 MG Road, Bengaluru",
                            onValueChange = { v -> actions.onUpdate { it.copy(homeAddress = v.take(120)) } },
                        )
                    }
                }
                item {
                    SettingRow("Work address", null) {
                        SettingTextField(
                            settings.workAddress,
                            placeholder = "Office address",
                            onValueChange = { v -> actions.onUpdate { it.copy(workAddress = v.take(120)) } },
                        )
                    }
                }

                item { Section("Look") }
                item {
                    SettingRow("Theme", "Auto switches to the dimmer night look from 6:30 PM to 6:30 AM.") {
                        Segmented(listOf("Auto", "Day", "Night"), settings.themeMode.ordinal) { i ->
                            actions.onUpdate { it.copy(themeMode = ThemeMode.entries[i]) }
                        }
                    }
                }
                item {
                    SettingRow("Accent colour", Accents.getOrElse(settings.accentIndex) { Accents[0] }.name) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Accents.forEachIndexed { i, accent ->
                                val selected = i == settings.accentIndex
                                Box(
                                    Modifier
                                        .size(MinTouch)
                                        .clip(CircleShape)
                                        .border(if (selected) 4.dp else 0.dp, if (selected) p.text else Color.Transparent, CircleShape)
                                        .padding(8.dp)
                                        .clip(CircleShape)
                                        .background(accent.color)
                                        .clickable { actions.onUpdate { it.copy(accentIndex = i) } }
                                )
                            }
                        }
                    }
                }

                item { Section("Permissions") }
                item {
                    SettingRow(
                        "Song details",
                        if (mediaAccess) "Allowed" else "Needed to show song name and artwork. If Android says " +
                            "\"Restricted setting\", open App info → ⋮ → Allow restricted settings first.",
                    ) {
                        PillButton(if (mediaAccess) "Allowed" else "Allow", primary = !mediaAccess, onClick = actions.onMediaAccess)
                    }
                }
                item {
                    SettingRow("GPS speed", if (hasLocation) "Allowed" else "Needed for the speedometer.") {
                        PillButton(if (hasLocation) "Allowed" else "Allow", primary = !hasLocation, onClick = actions.onLocation)
                    }
                }

                item {
                    SettingRow(
                        "Follow source switches",
                        if (usageAccess) "Allowed" else "Optional. Lets the media card follow Radio / BT / USB when you " +
                            "switch with the steering MODE button. Find DriveHome under Usage access.",
                    ) {
                        PillButton(if (usageAccess) "Allowed" else "Allow", primary = !usageAccess, onClick = actions.onUsageAccess)
                    }
                }

                item { Section("System") }
                item {
                    SettingRow("Default home app", "Pick DriveHome, or switch back to the original launcher.") {
                        PillButton("Choose", onClick = actions.onChooseHomeApp)
                    }
                }
                item {
                    SettingRow("Original launcher", "Opens the head unit's own home screen once.") {
                        PillButton("Open", onClick = actions.onOpenOriginalLauncher)
                    }
                }
                item {
                    SettingRow("Android settings", null) {
                        PillButton("Open", onClick = actions.onAndroidSettings)
                    }
                }
                item {
                    SettingRow("Diagnostics", "Device info and crash log. Also: hold the car on the home screen.") {
                        PillButton("Open", onClick = actions.onDiagnostics)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String? = null,
    textStyle: TextStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = LocalPalette.current.text),
) {
    val p = LocalPalette.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.width(380.dp).heightIn(min = 64.dp),
        placeholder = placeholder?.let { { Text(it, fontSize = 18.sp) } },
        singleLine = true,
        textStyle = textStyle,
        shape = ControlShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = p.accent,
            unfocusedBorderColor = p.outline,
            cursorColor = p.accent,
        ),
    )
}

@Composable
private fun Section(title: String) {
    Row(Modifier.padding(start = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title.uppercase(), color = LocalPalette.current.accent, style = labelStyle(13.sp))
        Spacer(Modifier.width(10.dp))
        LiveryStripes()
    }
}

@Composable
private fun SettingRow(title: String, subtitle: String?, trailing: @Composable () -> Unit) {
    Panel(Modifier.fillMaxWidth().heightIn(min = 96.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 16.dp)) {
                Text(title, color = LocalPalette.current.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (subtitle != null) Text(subtitle, color = LocalPalette.current.textDim, fontSize = 17.sp)
            }
            trailing()
        }
    }
}
