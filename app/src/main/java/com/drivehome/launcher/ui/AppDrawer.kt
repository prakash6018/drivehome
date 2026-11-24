package com.drivehome.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivehome.launcher.R
import com.drivehome.launcher.data.AppEntry
import com.drivehome.launcher.ui.theme.LocalPalette
import com.drivehome.launcher.ui.theme.Rajdhani

/**
 * Full app list (a parked-use screen). With [pickingFor] set it becomes the tile picker,
 * and [onReset] puts the tile back on automatic.
 */
@Composable
fun AppDrawer(
    apps: List<AppEntry>,
    pickingFor: String?,
    onSelect: (AppEntry) -> Unit,
    onReset: (() -> Unit)?,
    onClose: () -> Unit,
) {
    val p = LocalPalette.current
    BackHandler(onBack = onClose)
    var query by rememberSaveable { mutableStateOf("") }
    val shown = remember(apps, query) {
        val q = query.trim()
        if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
    }

    AmbientBackdrop(art = null, glowFromRight = true, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            ScreenHeader(if (pickingFor != null) "Choose: $pickingFor" else "All apps", onBack = onClose) {
                if (onReset != null) {
                    PillButton("Automatic", onClick = onReset)
                    Spacer(Modifier.width(12.dp))
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.width(380.dp).heightIn(min = 64.dp),
                    placeholder = { Text("Search apps", fontSize = 20.sp) },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_search), null, Modifier.size(28.dp)) },
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = Rajdhani, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = p.text),
                    shape = ControlShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = p.accent,
                        unfocusedBorderColor = p.outline,
                        focusedContainerColor = p.surface,
                        unfocusedContainerColor = p.surface,
                        cursorColor = p.accent,
                    ),
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(shown, key = { "${it.packageName}/${it.activityName}" }) { app ->
                    AppCell(app) { onSelect(app) }
                }
            }
        }
    }
}

@Composable
private fun AppCell(app: AppEntry, onClick: () -> Unit) {
    val p = LocalPalette.current
    Panel(Modifier.fillMaxWidth().heightIn(min = 150.dp), onClick = onClick) {
        Column(
            Modifier.fillMaxWidth().padding(12.dp).align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(app.icon, null, Modifier.size(72.dp))
            Spacer(Modifier.height(10.dp))
            Text(
                app.label,
                color = p.text,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
