package com.example.lostandfoundapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.*

val PrimaryBlue = Color(0xFF2563EB)
val LightBlue = Color(0xFFEFF6FF)
val SoftBackground = Color(0xFFFFFBF3)
val DarkText = Color(0xFF1E293B)
val DangerRed = Color(0xFFDC2626)

data class Advert(
    val id: Int,
    val postType: String,
    val category: String,
    val name: String,
    val phone: String,
    val description: String,
    val date: String,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    val imageUri: String,
    val timestamp: String
)

class LostFoundDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "lost_found_db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE adverts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                postType TEXT,
                category TEXT,
                name TEXT,
                phone TEXT,
                description TEXT,
                date TEXT,
                location TEXT,
                latitude REAL,
                longitude REAL,
                imageUri TEXT,
                timestamp TEXT
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS adverts")
        onCreate(db)
    }

    fun insertAdvert(advert: Advert): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("postType", advert.postType)
            put("category", advert.category)
            put("name", advert.name)
            put("phone", advert.phone)
            put("description", advert.description)
            put("date", advert.date)
            put("location", advert.location)
            put("latitude", advert.latitude)
            put("longitude", advert.longitude)
            put("imageUri", advert.imageUri)
            put("timestamp", advert.timestamp)
        }

        val result = db.insert("adverts", null, values)
        db.close()
        return result != -1L
    }

    fun getAllAdverts(): List<Advert> {
        val list = mutableListOf<Advert>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM adverts ORDER BY id DESC", null)

        if (cursor.moveToFirst()) {
            do {
                list.add(
                    Advert(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                        postType = cursor.getString(cursor.getColumnIndexOrThrow("postType")),
                        category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                        name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                        phone = cursor.getString(cursor.getColumnIndexOrThrow("phone")),
                        description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                        date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                        location = cursor.getString(cursor.getColumnIndexOrThrow("location")),
                        latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude")),
                        longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude")),
                        imageUri = cursor.getString(cursor.getColumnIndexOrThrow("imageUri")),
                        timestamp = cursor.getString(cursor.getColumnIndexOrThrow("timestamp"))
                    )
                )
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()
        return list
    }

    fun deleteAdvert(id: Int): Boolean {
        val db = writableDatabase
        val result = db.delete("adverts", "id=?", arrayOf(id.toString()))
        db.close()
        return result > 0
    }
}

class MainActivity : ComponentActivity() {

    private lateinit var dbHelper: LostFoundDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dbHelper = LostFoundDatabaseHelper(this)

        setContent {
            MaterialTheme {
                LostFoundApp(dbHelper)
            }
        }
    }
}

@Composable
fun LostFoundApp(dbHelper: LostFoundDatabaseHelper) {
    var screen by remember { mutableStateOf("home") }
    var adverts by remember { mutableStateOf(dbHelper.getAllAdverts()) }
    var selectedAdvert by remember { mutableStateOf<Advert?>(null) }

    when (screen) {
        "home" -> HomeScreen(
            onCreateClick = { screen = "create" },
            onShowClick = {
                adverts = dbHelper.getAllAdverts()
                screen = "list"
            },
            onMapClick = {
                adverts = dbHelper.getAllAdverts()
                screen = "map"
            }
        )

        "create" -> CreateAdvertScreen(
            dbHelper = dbHelper,
            onSaveComplete = {
                adverts = dbHelper.getAllAdverts()
                screen = "list"
            },
            onBack = { screen = "home" }
        )

        "list" -> AdvertListScreen(
            adverts = adverts,
            onAdvertClick = {
                selectedAdvert = it
                screen = "detail"
            },
            onBack = { screen = "home" }
        )

        "detail" -> selectedAdvert?.let {
            AdvertDetailScreen(
                advert = it,
                dbHelper = dbHelper,
                onDelete = {
                    adverts = dbHelper.getAllAdverts()
                    screen = "list"
                },
                onBack = { screen = "list" }
            )
        }

        "map" -> MapScreen(
            adverts = adverts,
            onBack = { screen = "home" }
        )
    }
}

@Composable
fun HomeScreen(
    onCreateClick: () -> Unit,
    onShowClick: () -> Unit,
    onMapClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.White, LightBlue, PrimaryBlue)
                )
            )
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Lost & Found",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Helping lost items find their owners.",
                color = DarkText
            )

            Spacer(modifier = Modifier.height(50.dp))

            HomeCard(
                title = "Create New Advert",
                subtitle = "Post a lost or found item",
                onClick = onCreateClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            HomeCard(
                title = "Show Lost & Found Items",
                subtitle = "Browse all adverts",
                onClick = onShowClick
            )

            Spacer(modifier = Modifier.height(16.dp))

            HomeCard(
                title = "Show On Map",
                subtitle = "View items on Google Map",
                onClick = onMapClick
            )
        }
    }
}

@Composable
fun HomeCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun CreateAdvertScreen(
    dbHelper: LostFoundDatabaseHelper,
    onSaveComplete: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var postType by remember { mutableStateOf("Lost") }
    var category by remember { mutableStateOf("Electronics") }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf(0.0) }
    var longitude by remember { mutableStateOf(0.0) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val categories = listOf("Electronics", "Pets", "Wallets", "Bags", "Keys", "Documents")

    val imagePickerLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri ->
            imageUri = uri
        }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                val loc = getCurrentLocation(context)
                if (loc != null) {
                    latitude = loc.latitude
                    longitude = loc.longitude
                    location = getAddressFromLatLng(context, latitude, longitude)
                    Toast.makeText(context, "Current location added", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    val calendar = Calendar.getInstance()

    val datePicker = DatePickerDialog(
        context,
        { _, year, month, day ->
            date = "$day/${month + 1}/$year"
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBackground)
            .verticalScroll(rememberScrollState())
    ) {
        TopBar(title = "Create Advert", onBack = onBack)

        Column(modifier = Modifier.padding(18.dp)) {
            Row {
                Button(
                    onClick = { postType = "Lost" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (postType == "Lost") PrimaryBlue else Color.LightGray
                    )
                ) {
                    Text("Lost")
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = { postType = "Found" },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (postType == "Found") PrimaryBlue else Color.LightGray
                    )
                ) {
                    Text("Found")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(text = "Category", fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                categories.forEach {
                    FilterChip(
                        selected = category == it,
                        onClick = { category = it },
                        label = { Text(it) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location / Address") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val result = getLatLngFromAddress(context, location)
                    if (result != null) {
                        latitude = result.first
                        longitude = result.second
                        Toast.makeText(context, "Location found", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Please enter a valid location", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Find Location Coordinates")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val loc = getCurrentLocation(context)
                        if (loc != null) {
                            latitude = loc.latitude
                            longitude = loc.longitude
                            location = getAddressFromLatLng(context, latitude, longitude)
                            Toast.makeText(context, "Current location added", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Get Current Location")
            }

            if (latitude != 0.0 && longitude != 0.0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Lat: $latitude, Lng: $longitude",
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { datePicker.show() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(if (date.isBlank()) "Select Date" else date)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Upload Image")
            }

            imageUri?.let {
                Spacer(modifier = Modifier.height(12.dp))

                Image(
                    painter = rememberAsyncImagePainter(it),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (
                        name.isBlank() ||
                        phone.isBlank() ||
                        description.isBlank() ||
                        date.isBlank() ||
                        location.isBlank() ||
                        imageUri == null ||
                        latitude == 0.0 ||
                        longitude == 0.0
                    ) {
                        Toast.makeText(
                            context,
                            "Please fill all fields and add location",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val timestamp =
                            SimpleDateFormat(
                                "dd/MM/yyyy hh:mm a",
                                Locale.getDefault()
                            ).format(Date())

                        val advert = Advert(
                            id = 0,
                            postType = postType,
                            category = category,
                            name = name,
                            phone = phone,
                            description = description,
                            date = date,
                            location = location,
                            latitude = latitude,
                            longitude = longitude,
                            imageUri = imageUri.toString(),
                            timestamp = timestamp
                        )

                        if (dbHelper.insertAdvert(advert)) {
                            Toast.makeText(context, "Advert Saved", Toast.LENGTH_SHORT).show()
                            onSaveComplete()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Save Advert")
            }
        }
    }
}

@Composable
fun AdvertListScreen(
    adverts: List<Advert>,
    onAdvertClick: (Advert) -> Unit,
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }

    val categories = listOf("All", "Electronics", "Pets", "Wallets", "Bags", "Keys", "Documents")

    val filteredAdverts =
        if (selectedCategory == "All") adverts
        else adverts.filter { it.category == selectedCategory }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBackground)
    ) {
        TopBar(title = "Lost & Found Items", onBack = onBack)

        LazyColumn(modifier = Modifier.padding(16.dp)) {
            item {
                Text(
                    text = "Filter by Category",
                    fontWeight = FontWeight.Bold,
                    color = DarkText
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    categories.forEach {
                        FilterChip(
                            selected = selectedCategory == it,
                            onClick = { selectedCategory = it },
                            label = { Text(it) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            items(filteredAdverts) { advert ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onAdvertClick(advert) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Image(
                            painter = rememberAsyncImagePainter(advert.imageUri),
                            contentDescription = null,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = advert.postType,
                                color = PrimaryBlue,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = advert.description,
                                fontWeight = FontWeight.Bold,
                                color = DarkText
                            )

                            Text(text = "Category: ${advert.category}", color = Color.Gray)
                            Text(text = "Location: ${advert.location}", color = Color.Gray)
                            Text(text = "Posted: ${advert.timestamp}", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdvertDetailScreen(
    advert: Advert,
    dbHelper: LostFoundDatabaseHelper,
    onDelete: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBackground)
            .verticalScroll(rememberScrollState())
    ) {
        TopBar(title = "Advert Details", onBack = onBack)

        Column(modifier = Modifier.padding(18.dp)) {
            Image(
                painter = rememberAsyncImagePainter(advert.imageUri),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            DetailRow("Type", advert.postType)
            DetailRow("Category", advert.category)
            DetailRow("Name", advert.name)
            DetailRow("Phone", advert.phone)
            DetailRow("Description", advert.description)
            DetailRow("Date", advert.date)
            DetailRow("Location", advert.location)
            DetailRow("Latitude", advert.latitude.toString())
            DetailRow("Longitude", advert.longitude.toString())
            DetailRow("Posted", advert.timestamp)

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (dbHelper.deleteAdvert(advert.id)) {
                        Toast.makeText(context, "Advert Removed", Toast.LENGTH_SHORT).show()
                        onDelete()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
            ) {
                Text("Remove Advert")
            }
        }
    }
}

@Composable
fun MapScreen(
    adverts: List<Advert>,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var radiusText by remember { mutableStateOf("10") }
    var userLat by remember { mutableStateOf(0.0) }
    var userLng by remember { mutableStateOf(0.0) }
    var filteredAdverts by remember { mutableStateOf(adverts) }

    val defaultLocation = LatLng(-37.8136, 144.9631)

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                val loc = getCurrentLocation(context)

                if (loc != null) {
                    userLat = loc.latitude
                    userLng = loc.longitude

                    val radius = radiusText.toDoubleOrNull() ?: 10.0

                    filteredAdverts = adverts.filter {
                        calculateDistanceKm(
                            userLat,
                            userLng,
                            it.latitude,
                            it.longitude
                        ) <= radius
                    }

                    cameraPositionState.position =
                        CameraPosition.fromLatLngZoom(
                            LatLng(userLat, userLng),
                            12f
                        )

                    Toast.makeText(context, "Radius search applied", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBackground)
    ) {
        TopBar(title = "Map View", onBack = onBack)

        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            OutlinedTextField(
                value = radiusText,
                onValueChange = { radiusText = it },
                label = { Text("Radius in KM") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val loc = getCurrentLocation(context)

                        if (loc != null) {
                            userLat = loc.latitude
                            userLng = loc.longitude

                            val radius = radiusText.toDoubleOrNull() ?: 10.0

                            filteredAdverts = adverts.filter {
                                calculateDistanceKm(
                                    userLat,
                                    userLng,
                                    it.latitude,
                                    it.longitude
                                ) <= radius
                            }

                            cameraPositionState.position =
                                CameraPosition.fromLatLngZoom(
                                    LatLng(userLat, userLng),
                                    12f
                                )

                            Toast.makeText(context, "Showing nearby items", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Unable to get current location", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Show Items Within Radius")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Showing ${filteredAdverts.size} item(s) within $radiusText km",
                fontWeight = FontWeight.Bold,
                color = DarkText
            )

            Spacer(modifier = Modifier.height(8.dp))

            GoogleMap(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
                    .clip(RoundedCornerShape(18.dp)),
                cameraPositionState = cameraPositionState
            ) {
                if (userLat != 0.0 && userLng != 0.0) {
                    Marker(
                        state = remember {
                            MarkerState(
                                position = LatLng(userLat, userLng)
                            )
                        },
                        title = "Your Current Location"
                    )
                }

                filteredAdverts.forEach { advert ->
                    Marker(
                        state = remember(advert.id) {
                            MarkerState(
                                position = LatLng(
                                    advert.latitude,
                                    advert.longitude
                                )
                            )
                        },
                        title = "${advert.postType}: ${advert.name}",
                        snippet = advert.location
                    )
                }
            }
        }
    }
}

@Composable
fun TopBar(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryBlue)
            .statusBarsPadding()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "< Back",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onBack() }
        )

        Spacer(modifier = Modifier.width(20.dp))

        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String
) {
    Row(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = "$label:",
            modifier = Modifier.width(110.dp),
            fontWeight = FontWeight.Bold,
            color = DarkText
        )

        Text(
            text = value,
            color = DarkText
        )
    }
}

fun getLatLngFromAddress(context: Context, address: String): Pair<Double, Double>? {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val result = geocoder.getFromLocationName(address, 1)
        if (!result.isNullOrEmpty()) {
            Pair(result[0].latitude, result[0].longitude)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

fun getAddressFromLatLng(context: Context, latitude: Double, longitude: Double): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val result = geocoder.getFromLocation(latitude, longitude, 1)
        if (!result.isNullOrEmpty()) {
            result[0].getAddressLine(0)
        } else {
            "$latitude, $longitude"
        }
    } catch (e: Exception) {
        "$latitude, $longitude"
    }
}

@SuppressLint("MissingPermission")
fun getCurrentLocation(context: Context): Location? {
    val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val gpsLocation =
        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

    val networkLocation =
        locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

    return gpsLocation ?: networkLocation
}

fun calculateDistanceKm(
    userLat: Double,
    userLng: Double,
    itemLat: Double,
    itemLng: Double
): Double {
    val earthRadius = 6371.0

    val dLat = Math.toRadians(itemLat - userLat)
    val dLng = Math.toRadians(itemLng - userLng)

    val a =
        sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(userLat)) *
                cos(Math.toRadians(itemLat)) *
                sin(dLng / 2).pow(2.0)

    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadius * c
}