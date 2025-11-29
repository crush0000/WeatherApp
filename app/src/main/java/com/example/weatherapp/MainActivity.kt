package com.example.weatherapp

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var weatherCard: CardView
    private lateinit var cityEditText: EditText
    private lateinit var getWeatherButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var tempTextView: TextView
    private lateinit var descTextView: TextView
    private lateinit var iconImageView: ImageView

    private lateinit var apiService: ApiService
    private lateinit var database: AppDatabase

    private val API_KEY = "ff669304884fd1a34f0e28c7cf293c10"

    private val PREFS_NAME = "WeatherAppPrefs"
    private val LAST_CITY_KEY = "last_city"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRetrofit()
        database = AppDatabase.getInstance(this)


        loadLastCachedWeather()

        getWeatherButton.setOnClickListener {
            val city = cityEditText.text.toString().trim()
            if (city.isNotEmpty()) {
                fetchWeather(city)
            }
        }
    }

    private fun initViews() {
        weatherCard = findViewById(R.id.weatherCard)
        cityEditText = findViewById(R.id.cityEditText)
        getWeatherButton = findViewById(R.id.getWeatherButton)
        progressBar = findViewById(R.id.progressBar)
        errorTextView = findViewById(R.id.errorTextView)
        tempTextView = findViewById(R.id.tempTextView)
        descTextView = findViewById(R.id.descTextView)
        iconImageView = findViewById(R.id.iconImageView)
    }

    private fun setupRetrofit() {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        apiService = retrofit.create(ApiService::class.java)
    }

    private fun loadLastCachedWeather() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCity = prefs.getString(LAST_CITY_KEY, null) ?: return

        cityEditText.setText(lastCity)

        lifecycleScope.launch {
            val cached = withContext(Dispatchers.IO) {
                database.weatherDao().getWeatherByCity(lastCity)
            }
            cached?.let {
                displayWeather(it.temp, it.description, it.icon)
            }
        }
    }

    private fun saveLastCity(city: String) {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(LAST_CITY_KEY, city)
            .apply()
    }

    private fun fetchWeather(city: String) {
        lifecycleScope.launch {
            showLoading()
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getWeather(city, API_KEY)
                }


                val entity = WeatherEntity(
                    city = city,
                    temp = response.main.temp,
                    description = response.weather[0].description,
                    icon = response.weather[0].icon
                )
                withContext(Dispatchers.IO) {
                    database.weatherDao().insert(entity)
                }

                saveLastCity(city)
                displayWeather(response.main.temp, response.weather[0].description, response.weather[0].icon)

            } catch (e: IOException) {
                val cached = withContext(Dispatchers.IO) {
                    database.weatherDao().getWeatherByCity(city)
                }
                if (cached != null) {
                    saveLastCity(city)
                    displayWeather(cached.temp, cached.description, cached.icon)
                } else {
                    showError("Нет подключения к интернету")
                }
            } catch (e: Exception) {
                showError(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    private fun displayWeather(temp: Double, rawDescription: String, icon: String) {
        progressBar.visibility = View.GONE
        errorTextView.visibility = View.GONE
        weatherCard.visibility = View.VISIBLE
        tempTextView.visibility = View.VISIBLE
        descTextView.visibility = View.VISIBLE
        iconImageView.visibility = View.VISIBLE
        getWeatherButton.isEnabled = true

        tempTextView.text = "${temp}°F"
        descTextView.text = rawDescription.split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

        val url = "https://openweathermap.org/img/wn/${icon}@4x.png"
        Glide.with(this).load(url).into(iconImageView)
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        errorTextView.visibility = View.GONE
        weatherCard.visibility = View.GONE
        tempTextView.visibility = View.GONE
        descTextView.visibility = View.GONE
        iconImageView.visibility = View.GONE
        getWeatherButton.isEnabled = false
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        errorTextView.visibility = View.VISIBLE
        errorTextView.text = "Ошибка: $message"
        weatherCard.visibility = View.GONE
        getWeatherButton.isEnabled = true
    }
}