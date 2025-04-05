package com.kolee.composemusicexoplayer.presentation.navigation

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration

@Composable
fun ResponsiveNavigationBar(
    items: List<BottomNavItem>,
    currentRoute: String,
    onItemClick: (String) -> Unit
) {
    val configuration = LocalConfiguration.current

    if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        NavigationRail(
            backgroundColor = Color.Black,
            contentColor = Color.White,
            modifier = Modifier.fillMaxHeight()
        ) {
            items.forEach { item ->
                NavigationRailItem(
                    selected = currentRoute == item.route,
                    onClick = { onItemClick(item.route) },
                    icon = {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.name,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(100.dp))
                            Text(
                                text = item.name,
                                color = Color.White
                            )
                        }
                    }
                )
            }
        }
    } else {
        BottomNavigation(
            backgroundColor = Color.Black,
            contentColor = Color.White,
            elevation = 120.dp
        ) {
            items.forEach { item ->
                BottomNavigationItem(
                    selected = currentRoute == item.route,
                    onClick = { onItemClick(item.route) },
                    icon = { Icon(imageVector = item.icon, contentDescription = item.name) },
                    label = { Text(text = item.name) },
                    selectedContentColor = Color.White,
                    unselectedContentColor = Color.Gray
                )
            }
        }
    }
}
