package com.example.calcmot.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.calcmot.hasOverlayPermission
import com.example.calcmot.isAccessibilityServiceEnabled

@Composable
fun OnboardingScreen(onPermissionsGranted: () -> Unit) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Ative o Métrica", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Conceda as permissões para analisar suas corridas em tempo real.", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(32.dp))

        PermissionCheckItem(
            text = "Permissão de Sobreposição",
            isChecked = hasOverlayPermission(context),
            onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        PermissionCheckItem(
            text = "Serviço de Acessibilidade",
            isChecked = isAccessibilityServiceEnabled(context),
            onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        )

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onPermissionsGranted,
            enabled = hasOverlayPermission(context) && isAccessibilityServiceEnabled(context)
        ) {
            Text("CONCLUIR")
        }
    }
}

@Composable
fun PermissionCheckItem(text: String, isChecked: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, modifier = Modifier.weight(1f))
            Checkbox(checked = isChecked, onCheckedChange = null)
        }
    }
}