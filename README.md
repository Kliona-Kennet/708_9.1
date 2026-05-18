# Lost & Found Map Mobile App

## Overview
The Lost & Found Map Mobile App is an Android application developed using Kotlin and Jetpack Compose. The application allows users to create lost or found item adverts, upload images, save item details, capture geo-location information, and display items on Google Maps with radius-based search functionality.

This project was developed for SIT708 Task 9.1P – Lost and Found Map Mobile App.

---

# Features

- Create lost or found item adverts
- Upload item images
- Save advert details using SQLite database
- Add location using:
  - Current GPS location
  - Address to coordinate conversion
- Google Maps integration
- Display markers for lost and found items
- Radius-based search using latitude and longitude
- View advert details
- Delete adverts
- Category filtering

---

# Technologies Used

- Kotlin
- Android Studio
- Jetpack Compose
- SQLite Database
- Google Maps Compose
- Android Geocoder API
- Location Services

---

# Project Structure

- `MainActivity.kt` → Main application logic and UI screens
- `AndroidManifest.xml` → Permissions and app configuration
- SQLite Database → Local advert storage
- Google Maps API → Map and geo-location functionality

---

# Geo Features

The application supports:
- Current location detection
- Address to latitude/longitude conversion
- Radius-based search
- Google Map markers for adverts

---

# How to Run the Project

1. Open the project in Android Studio
2. Sync Gradle files
3. Add your Google Maps API key
4. Run the app on:
   - Android Emulator
   - Physical Android Device

---

# Permissions Used

- ACCESS_FINE_LOCATION
- INTERNET

---

# Radius Search

Users can enter a radius value in kilometres.  
The app calculates distances using the Haversine formula and displays only nearby lost or found items within the selected radius.

---

# Future Improvements

- Firebase cloud database integration
- User authentication
- Push notifications
- Real-time item updates
- AI image matching for lost items
- Chat functionality between users

---

# Author

Kliona Kennet
