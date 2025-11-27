package com.example.weatherapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weather_table")
data class WeatherEntity(
    @PrimaryKey val city: String,
    val temp: Double,
    val description: String,
    val icon: String,
    val timestamp: Long = System.currentTimeMillis()
)