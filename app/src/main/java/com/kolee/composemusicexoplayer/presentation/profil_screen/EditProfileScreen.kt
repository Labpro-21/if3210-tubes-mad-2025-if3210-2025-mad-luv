package com.kolee.composemusicexoplayer.presentation.profile_screen

import android.Manifest
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Map
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.kolee.composemusicexoplayer.R
import com.kolee.composemusicexoplayer.data.profile.ProfileViewModel
import com.kolee.composemusicexoplayer.data.profile.UpdateStatus
import com.kolee.composemusicexoplayer.utils.LocationUtils
import com.kolee.composemusicexoplayer.utils.CountryInfo
import com.kolee.composemusicexoplayer.utils.LocationDetailResult
import com.kolee.composemusicexoplayer.utils.onSuccess
import com.kolee.composemusicexoplayer.utils.onError
import kotlinx.coroutines.launch
//import androidx.compose.foundation.rotate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.LaunchedEffect
import java.util.concurrent.TimeUnit
import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.compose.ui.draw.rotate
import coil.request.CachePolicy
import com.kolee.composemusicexoplayer.utils.LocationCoordinates

@Composable
fun EditProfileScreen(
    profileViewModel: ProfileViewModel,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val profileState by profileViewModel.profile.collectAsState()
    val updateStatus by profileViewModel.updateStatus.collectAsState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) } // Track camera URI
    var currentLocation by remember { mutableStateOf("") }
    var currentLocationCode by remember { mutableStateOf("") }
    var showImagePickerDialog by remember { mutableStateOf(false) }
    var showLocationDialog by remember { mutableStateOf(false) }
    var showLocationOptions by remember { mutableStateOf(false) }
    var isGettingLocation by remember { mutableStateOf(false) }
    var isProcessingMapLocation by remember { mutableStateOf(false) }

    val locationUtils = remember { LocationUtils(context) }

    // Initialize current location from profile with null safety
    LaunchedEffect(profileState) {
        profileState?.let { profile ->
            try {
                currentLocationCode = profile.location?.takeIf { !it.isNullOrBlank() } ?: ""
                currentLocation = if (!profile.location.isNullOrBlank()) {
                    locationUtils.getCountryName(profile.location!!)?.takeIf { !it.isNullOrBlank() } ?: ""
                } else {
                    ""
                }
            } catch (e: Exception) {
                currentLocationCode = ""
                currentLocation = ""
            }
        }
    }

    // Handle update status
    LaunchedEffect(updateStatus) {
        when (updateStatus) {
            is UpdateStatus.Success -> {
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                onBackClick()
                profileViewModel.resetUpdateStatus()
            }
            is UpdateStatus.Error -> {
                val errorMessage = (updateStatus as UpdateStatus.Error).message ?: "Unknown error"
                Toast.makeText(context, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                profileViewModel.resetUpdateStatus()
            }
            else -> { /* Do nothing */ }
        }
    }

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            isGettingLocation = true
            coroutineScope.launch {
                locationUtils.getCurrentLocation()
                    .onSuccess { countryCode ->
                        try {
                            currentLocationCode = countryCode?.takeIf { it.isNotBlank() } ?: ""
                            currentLocation = if (!countryCode.isNullOrBlank()) {
                                locationUtils.getCountryName(countryCode)?.takeIf { it.isNotBlank() } ?: ""
                            } else {
                                ""
                            }
                        } catch (e: Exception) {
                            currentLocationCode = ""
                            currentLocation = ""
                        }
                        isGettingLocation = false
                    }
                    .onError { error ->
                        Toast.makeText(context, "Error getting location: ${error ?: "Unknown error"}", Toast.LENGTH_LONG).show()
                        isGettingLocation = false
                    }
            }
        } else {
            Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Image picker launcher for gallery
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        pendingCameraUri = null // Clear camera URI when gallery is used
    }

    // Camera launcher - FIXED
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null) {
            // Set the selected URI to the camera URI when photo is taken successfully
            selectedImageUri = pendingCameraUri
        } else {
            // Clear both URIs if camera failed
            selectedImageUri = null
            pendingCameraUri = null
        }
    }

    // Permission launcher for camera - FIXED
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val imageUri = createImageUri(context)
            pendingCameraUri = imageUri // Store camera URI separately
            cameraLauncher.launch(imageUri)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF015871), Color.Black)
                )
            )
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "Edit Profile",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            backgroundColor = Color.Transparent,
            elevation = 0.dp
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Profile Picture Section - IMPROVED IMAGE HANDLING
            Box(
                contentAlignment = Alignment.BottomEnd
            ) {
                val imageUrl = when {
                    selectedImageUri != null -> selectedImageUri.toString()
                    !profileState?.profilePhoto.isNullOrBlank() ->
                        "http://34.101.226.132:3000/uploads/profile-picture/${profileState?.profilePhoto}"
                    else -> null
                }

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color.Gray)
                        .clickable { showImagePickerDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .memoryCachePolicy(CachePolicy.DISABLED) // Disable cache for immediate updates
                                .diskCachePolicy(CachePolicy.DISABLED)   // Disable disk cache
                                .build(),
                            contentDescription = "Profile Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            onError = {
                                Log.e("ImageLoad", "Failed to load image: $imageUrl")
                            }
                        )
                    }

                    if (imageUrl == null) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Profile",
                            tint = Color.White,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { showImagePickerDialog = true },
                    modifier = Modifier
                        .offset(x = (-8).dp, y = (-8).dp)
                        .background(Color.White, CircleShape)
                        .size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile Picture",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Safe username display with fallback
            if (profileState != null) {
                Text(
                    text = profileState!!.username?.takeIf { !it.isNullOrBlank() } ?: "Unknown User",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            } else {
                Text(
                    text = "Loading...",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Location Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                backgroundColor = Color(0xFF1E1E1E)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Location",
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Location",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = currentLocation.takeIf { it.isNotBlank() } ?: "Not set",
                        fontSize = 16.sp,
                        color = if (currentLocation.isNotBlank()) Color.White else Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Choose Manually Button
                        Button(
                            onClick = { showLocationOptions = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF2A2A2A)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Manual",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Manual",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }

                        // Current Location Button
                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                ))
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF4A9EFF)
                            ),
                            enabled = !isGettingLocation && !isProcessingMapLocation
                        ) {
                            if (isGettingLocation) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "GPS",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "GPS",
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Save Button
            val isLoading = updateStatus is UpdateStatus.Loading || isProcessingMapLocation
            Button(
                onClick = {
                    val locationToSave = currentLocationCode.takeIf { it.isNotBlank() }
                    try {
                        profileViewModel.updateProfile(
                            location = locationToSave,
                            imageUri = selectedImageUri,
                            context = context
                        )
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error updating profile: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF1DB954)
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Text(
                        text = "Save Changes",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Image Picker Dialog
    if (showImagePickerDialog) {
        ImagePickerDialog(
            onGalleryClick = {
                showImagePickerDialog = false
                imagePickerLauncher.launch("image/*")
            },
            onCameraClick = {
                showImagePickerDialog = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            onDismiss = { showImagePickerDialog = false }
        )
    }

    if (showLocationOptions) {
        LocationOptionsDialogWithManualInput(
            locationUtils = locationUtils,
            onManualSelect = { countryCode, countryName ->
                currentLocationCode = countryCode
                currentLocation = countryName
                showLocationOptions = false
            },
            onCoordinatesInput = { countryCode, countryName ->
                currentLocationCode = countryCode
                currentLocation = countryName
                showLocationOptions = false
            },
            onDismiss = { showLocationOptions = false }
        )
    }
}

// Enhanced Location Options Dialog with Maps Integration
@Composable
fun LocationOptionsDialogWithManualInput(
    locationUtils: LocationUtils,
    onManualSelect: (String, String) -> Unit,
    onCoordinatesInput: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val commonCountries = remember { locationUtils.getCommonCountries() }
    var searchQuery by remember { mutableStateOf("") }
    var showManualSelection by remember { mutableStateOf(false) }
    var showCoordinateInput by remember { mutableStateOf(false) }
    var coordinateInput by remember { mutableStateOf("") }
    var isProcessingCoordinates by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Function to process coordinate input
    fun processCoordinateInput() {
        if (coordinateInput.isBlank()) return

        isProcessingCoordinates = true
        coroutineScope.launch {
            try {
                val coordinates = parseCoordinateString(coordinateInput)
                if (coordinates != null) {
                    // Get country from coordinates
                    locationUtils.getCountryCodeFromCoordinates(coordinates.latitude, coordinates.longitude)
                        .onSuccess { countryCode, countryName, city, fullAddress ->
                            onCoordinatesInput(countryCode, countryName)
                            Toast.makeText(
                                context,
                                "Location set: $countryName${if (city.isNotBlank()) ", $city" else ""}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .onError { error ->
                            Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(context, "Invalid coordinate format", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error processing coordinates: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isProcessingCoordinates = false
            }
        }
    }

    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            commonCountries
        } else {
            commonCountries.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.code.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = Color(0xFF2A2A2A),
        title = {
            Text(
                text = when {
                    showCoordinateInput -> "Enter Location"
                    showManualSelection -> "Select Country"
                    else -> "Set Location"
                },
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            when {
                showCoordinateInput -> {
                    // Manual coordinate/link input
                    Column {
                        Text(
                            text = "Paste coordinates or Google Maps link:",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        TextField(
                            value = coordinateInput,
                            onValueChange = { coordinateInput = it },
                            placeholder = {
                                Text(
                                    text = "e.g., -6.2088, 106.8456",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            },
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color(0xFF1E1E1E),
                                textColor = Color.White,
                                cursorColor = Color(0xFF1DB954),
                                focusedIndicatorColor = Color(0xFF1DB954),
                                unfocusedIndicatorColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Instructions
                        Card(
                            backgroundColor = Color(0xFF1E1E1E),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "How to get coordinates from Google Maps:",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "1. Buka Google Maps\n" +
                                            "2. Cari atau tap lokasi yang diinginkan\n" +
                                            "3. Tap dan hold pada lokasi tersebut\n" +
                                            "4. Copy koordinat yang muncul (e.g., -6.2088, 106.8456)\n" +
                                            "5. Paste di field ini",
                                    color = Color.Gray,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        if (isProcessingCoordinates) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFF1DB954),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Processing location...",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                showManualSelection -> {
                    // Country selection
                    Column {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = "Search countries...",
                                    color = Color.Gray
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.Gray
                                )
                            },
                            colors = TextFieldDefaults.textFieldColors(
                                backgroundColor = Color(0xFF1E1E1E),
                                textColor = Color.White,
                                cursorColor = Color(0xFF1DB954),
                                focusedIndicatorColor = Color(0xFF1DB954),
                                unfocusedIndicatorColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        ) {
                            items(filteredCountries) { country ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clickable {
                                            onManualSelect(country.code, country.name)
                                        },
                                    backgroundColor = Color(0xFF1E1E1E),
                                    elevation = 2.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = country.name,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = country.code,
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Main options
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Choose how to set your location:",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Coordinate input option
                        LocationMethodCard(
                            icon = Icons.Default.LocationOn,
                            title = "Enter Coordinates",
                            description = "Paste from Google Maps",
                            onClick = { showCoordinateInput = true }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Manual selection option
                        LocationMethodCard(
                            icon = Icons.Default.Search,
                            title = "Choose Country",
                            description = "Select from list",
                            onClick = { showManualSelection = true }
                        )
                    }
                }
            }
        },
        confirmButton = {
            when {
                showCoordinateInput -> {
                    Button(
                        onClick = { processCoordinateInput() },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF1DB954)
                        ),
                        enabled = coordinateInput.isNotBlank() && !isProcessingCoordinates
                    ) {
                        Text(
                            text = "Set Location",
                            color = Color.White
                        )
                    }
                }
                showManualSelection -> {
                    TextButton(
                        onClick = { showManualSelection = false }
                    ) {
                        Text(
                            text = "Back",
                            color = Color(0xFF1DB954)
                        )
                    }
                }
                showCoordinateInput -> {
                    TextButton(
                        onClick = { showCoordinateInput = false }
                    ) {
                        Text(
                            text = "Back",
                            color = Color(0xFF1DB954)
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = Color.Gray
                )
            }
        }
    )
}

// ===== HELPER FUNCTION =====
fun parseCoordinateString(input: String): LocationCoordinates? {
    return try {
        val cleanInput = input.trim()

        when {
            // Format: "lat, lng" atau "lat,lng"
            cleanInput.contains(",") -> {
                val parts = cleanInput.split(",")
                if (parts.size >= 2) {
                    val lat = parts[0].trim().toDoubleOrNull()
                    val lng = parts[1].trim().toDoubleOrNull()

                    if (lat != null && lng != null &&
                        lat >= -90 && lat <= 90 &&
                        lng >= -180 && lng <= 180) {
                        LocationCoordinates(lat, lng)
                    } else null
                } else null
            }

            // Format: Google Maps URL
            cleanInput.contains("google.com/maps") || cleanInput.contains("goo.gl/maps") -> {
                parseGoogleMapsUrl(cleanInput)
            }

            // Format: "@lat,lng" dari Maps
            cleanInput.contains("@") -> {
                val atIndex = cleanInput.indexOf("@")
                val coordPart = cleanInput.substring(atIndex + 1).split(",")
                if (coordPart.size >= 2) {
                    val lat = coordPart[0].toDoubleOrNull()
                    val lng = coordPart[1].toDoubleOrNull()

                    if (lat != null && lng != null &&
                        lat >= -90 && lat <= 90 &&
                        lng >= -180 && lng <= 180) {
                        LocationCoordinates(lat, lng)
                    } else null
                } else null
            }

            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

fun parseGoogleMapsUrl(url: String): LocationCoordinates? {
    return try {
        // Extract coordinates from various Google Maps URL formats
        val patterns = listOf(
            Regex("""@(-?\d+\.?\d*),(-?\d+\.?\d*)"""),
            Regex("""q=(-?\d+\.?\d*),(-?\d+\.?\d*)"""),
            Regex("""ll=(-?\d+\.?\d*),(-?\d+\.?\d*)""")
        )

        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) {
                val lat = match.groupValues[1].toDoubleOrNull()
                val lng = match.groupValues[2].toDoubleOrNull()

                if (lat != null && lng != null &&
                    lat >= -90 && lat <= 90 &&
                    lng >= -180 && lng <= 180) {
                    return LocationCoordinates(lat, lng)
                }
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}

@Composable
fun LocationMethodCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        backgroundColor = Color(0xFF1E1E1E),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF1DB954),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Go",
                tint = Color.Gray,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(180f)
            )
        }
    }
}


@Composable
fun ImagePickerDialog(
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = Color(0xFF2A2A2A),
        title = {
            Text(
                text = "Select Profile Picture",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        },
        text = {
            Column(
                modifier = Modifier.padding(top = 12.dp)
            ){
                // Gallery option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGalleryClick() },
                    backgroundColor = Color(0xFF1E1E1E),
                    elevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Choose from Gallery",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Camera option
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCameraClick() },
                    backgroundColor = Color(0xFF1E1E1E),
                    elevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Take Photo",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    color = Color.Gray
                )
            }
        }
    )
}

private fun createImageUri(context: Context): Uri {
    return context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "profile_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }
    ) ?: Uri.EMPTY
}
