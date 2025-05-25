package com.kolee.composemusicexoplayer.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume

class LocationUtils(private val context: Context) {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val geocoder = Geocoder(context, Locale.getDefault())

    companion object {
        const val LOCATION_PICKER_REQUEST_CODE = 1001
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_ADDRESS = "address"
    }

    suspend fun getCurrentLocation(): LocationResult = withContext(Dispatchers.IO) {
        try {
            // Check permissions
            val fineLocationPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            )
            val coarseLocationPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            )

            if (fineLocationPermission != PackageManager.PERMISSION_GRANTED &&
                coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
                return@withContext LocationResult.Error("Location permissions not granted")
            }

            // Check if location services are enabled
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                return@withContext LocationResult.Error("Location services are disabled")
            }

            // Get last known location first (faster)
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            var lastKnownLocation: Location? = null

            for (provider in providers) {
                try {
                    val location = locationManager.getLastKnownLocation(provider)
                    if (location != null && (lastKnownLocation == null ||
                                location.time > lastKnownLocation.time)) {
                        lastKnownLocation = location
                    }
                } catch (e: SecurityException) {
                    Log.e("LocationUtils", "Security exception getting last known location", e)
                }
            }

            if (lastKnownLocation != null) {
                val countryCode = getCountryCodeFromLocation(lastKnownLocation.latitude, lastKnownLocation.longitude)
                if (countryCode != null) {
                    return@withContext LocationResult.Success(countryCode)
                }
            }

            return@withContext LocationResult.Error("Unable to determine location")

        } catch (e: Exception) {
            Log.e("LocationUtils", "Error getting current location", e)
            return@withContext LocationResult.Error("Error: ${e.message}")
        }
    }


    suspend fun getCountryCodeFromCoordinates(latitude: Double, longitude: Double): LocationDetailResult {
        return try {
            val addresses = withContext(Dispatchers.IO) {
                geocoder.getFromLocation(latitude, longitude, 1)
            }

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                Log.d("LocationUtils", "Address found: ${address.countryName} (${address.countryCode})")

                LocationDetailResult.Success(
                    countryCode = address.countryCode ?: "",
                    countryName = address.countryName ?: "",
                    city = address.locality ?: address.subAdminArea ?: "",
                    fullAddress = address.getAddressLine(0) ?: ""
                )
            } else {
                Log.e("LocationUtils", "No addresses found for coordinates")
                LocationDetailResult.Error("No address found for the selected location")
            }
        } catch (e: Exception) {
            Log.e("LocationUtils", "Error geocoding location", e)
            LocationDetailResult.Error("Error getting location details: ${e.message}")
        }
    }

    // ENHANCED: Create better location picker intent with multiple fallbacks
    fun createLocationPickerIntent(): Intent {
        return try {
            // Method 1: Try Google Maps Place Picker (if available)
            val placePickerIntent = Intent().apply {
                action = "com.google.android.gms.location.places.ui.PICK_PLACE"
                setPackage("com.google.android.apps.maps")
            }

            if (placePickerIntent.resolveActivity(context.packageManager) != null) {
                return placePickerIntent
            }

            // Method 2: Try Google Maps with search query
            val mapsSearchIntent = Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("geo:0,0?q=location")
                setPackage("com.google.android.apps.maps")
            }

            if (mapsSearchIntent.resolveActivity(context.packageManager) != null) {
                return mapsSearchIntent
            }

            // Method 3: Generic geo intent
            val geoIntent = Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("geo:0,0?q=")
            }

            if (geoIntent.resolveActivity(context.packageManager) != null) {
                return geoIntent
            }

            // Method 4: Web fallback
            return createWebLocationPickerIntent()

        } catch (e: Exception) {
            Log.e("LocationUtils", "Error creating location picker intent", e)
            createWebLocationPickerIntent()
        }
    }

    // ENHANCED: Web-based location picker as ultimate fallback
    private fun createWebLocationPickerIntent(): Intent {
        return Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://www.google.com/maps/search/?api=1&query=location")
        }
    }


    private suspend fun getCountryCodeFromLocation(latitude: Double, longitude: Double): String? {
        return try {
            // Validate coordinates
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                Log.e("LocationUtils", "Invalid coordinates: $latitude, $longitude")
                return null
            }

            val addresses = withContext(Dispatchers.IO) {
                try {
                    geocoder.getFromLocation(latitude, longitude, 1)
                } catch (e: Exception) {
                    Log.e("LocationUtils", "Geocoder error", e)
                    null
                }
            }

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val countryCode = address.countryCode
                val countryName = address.countryName

                Log.d("LocationUtils", "Geocoding successful: $countryName ($countryCode)")

                if (!countryCode.isNullOrBlank()) {
                    countryCode
                } else {
                    Log.w("LocationUtils", "Country code is null or blank")
                    null
                }
            } else {
                Log.w("LocationUtils", "No addresses found for coordinates: $latitude, $longitude")
                null
            }
        } catch (e: Exception) {
            Log.e("LocationUtils", "Error getting country code from location", e)
            null
        }
    }


    fun getCountryName(countryCode: String): String {
        return try {
            val locale = Locale("", countryCode)
            locale.displayCountry
        } catch (e: Exception) {
            countryCode
        }
    }

    fun getCommonCountries(): List<CountryInfo> {
        return listOf(
            CountryInfo("US", "United States"),
            CountryInfo("ID", "Indonesia"),
            CountryInfo("GB", "United Kingdom"),
            CountryInfo("CA", "Canada"),
            CountryInfo("AU", "Australia"),
            CountryInfo("DE", "Germany"),
            CountryInfo("FR", "France"),
            CountryInfo("JP", "Japan"),
            CountryInfo("KR", "South Korea"),
            CountryInfo("SG", "Singapore"),
            CountryInfo("MY", "Malaysia"),
            CountryInfo("TH", "Thailand"),
            CountryInfo("PH", "Philippines"),
            CountryInfo("VN", "Vietnam"),
            CountryInfo("IN", "India"),
            CountryInfo("CN", "China"),
            CountryInfo("BR", "Brazil"),
            CountryInfo("MX", "Mexico"),
            CountryInfo("ES", "Spain"),
            CountryInfo("IT", "Italy"),
            CountryInfo("NL", "Netherlands"),
            CountryInfo("SE", "Sweden"),
            CountryInfo("NO", "Norway"),
            CountryInfo("DK", "Denmark"),
            CountryInfo("FI", "Finland"),
            CountryInfo("BE", "Belgium"),
            CountryInfo("CH", "Switzerland"),
            CountryInfo("AT", "Austria"),
            CountryInfo("PT", "Portugal"),
            CountryInfo("IE", "Ireland")
        ).sortedBy { it.name }
    }
}

sealed class LocationResult {
    data class Success(val countryCode: String) : LocationResult()
    data class Error(val message: String) : LocationResult()
}

sealed class LocationDetailResult {
    data class Success(
        val countryCode: String,
        val countryName: String,
        val city: String,
        val fullAddress: String
    ) : LocationDetailResult()
    data class Error(val message: String) : LocationDetailResult()
}

data class CountryInfo(
    val code: String,
    val name: String
) {
    override fun toString(): String = name
}


data class LocationCoordinates(
    val latitude: Double,
    val longitude: Double,
    val address: String? = null
)


fun LocationResult.onSuccess(action: (String) -> Unit): LocationResult {
    if (this is LocationResult.Success) {
        action(countryCode)
    }
    return this
}

fun LocationResult.onError(action: (String) -> Unit): LocationResult {
    if (this is LocationResult.Error) {
        action(message)
    }
    return this
}


fun LocationDetailResult.onSuccess(action: (String, String, String, String) -> Unit): LocationDetailResult {
    if (this is LocationDetailResult.Success) {
        action(countryCode, countryName, city, fullAddress)
    }
    return this
}

fun LocationDetailResult.onError(action: (String) -> Unit): LocationDetailResult {
    if (this is LocationDetailResult.Error) {
        action(message)
    }
    return this
}