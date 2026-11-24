package com.drivehome.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.drivehome.launcher.R
import com.drivehome.launcher.ui.theme.LocalPalette

/** Stand-in for logcat: everything needed to debug this unit without ADB. */
@Composable
fun DiagnosticsScreen(report: String, onExport: () -> Unit, onClearCrashLog: () -> Unit, onBack: () -> Unit) {
    val p = LocalPalette.current
    BackHandler(onBack = onBack)
    Column(
        Modifier
            .fillMaxSize()
            .background(p.background)
            .padding(16.dp),
    ) {
        ScreenHeader("Diagnostics", onBack = onBack) {
            PillButton("Clear crash log", onClick = onClearCrashLog)
            Spacer(Modifier.width(12.dp))
            PillButton("Save to Downloads", primary = true, onClick = onExport)
        }
        SelectionContainer(
            Modifier
                .padding(top = 16.dp)
                .clip(ControlShape)
                .background(p.surface)
                .padding(16.dp)
        ) {
            Text(
                report,
                Modifier.verticalScroll(rememberScrollState()),
                color = p.text,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}
