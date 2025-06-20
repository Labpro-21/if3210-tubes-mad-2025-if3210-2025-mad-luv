package com.kolee.composemusicexoplayer.presentation.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCodeScanner

data class BottomNavItem(
    val name: String,
    val route: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem("Home", "home", Icons.Outlined.Home),
    BottomNavItem("Library", "library", Icons.Outlined.LibraryMusic),
    BottomNavItem("Scan QR", "scan_qr", Icons.Outlined.QrCodeScanner),
    BottomNavItem("Profile", "profile", Icons.Outlined.Person)
)
