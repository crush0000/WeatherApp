package com.example.weatherapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var weatherCard: androidx.cardview.widget.CardView
    private lateinit var cityEditText: EditText
    private lateinit var getWeatherButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var tempTextView: TextView
    private lateinit var descTextView: TextView
    private lateinit var iconImageView: ImageView
    private lateinit var apiService: ApiService
    private val API_KEY = "ff669304884fd1a34f0e28c7cf293c10"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        weatherCard=findViewById(R.id.weatherCard)
        cityEditText = findViewById(R.id.cityEditText)
        getWeatherButton = findViewById(R.id.getWeatherButton)
        progressBar = findViewById(R.id.progressBar)
        errorTextView = findViewById(R.id.errorTextView)
        tempTextView = findViewById(R.id.tempTextView)
        descTextView = findViewById(R.id.descTextView)
        iconImageView = findViewById(R.id.iconImageView)


        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val client = OkHttpClient.Builder().addInterceptor(logging).build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.openweathermap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        apiService = retrofit.create(ApiService::class.java)

        getWeatherButton.setOnClickListener {
            val city = cityEditText.text.toString().trim()
            if (city.isNotEmpty()) {
                fetchWeather(city)
            }
        }
    }

    private fun fetchWeather(city: String) {
        lifecycleScope.launch {
            showLoading()
            try {
                val response = withContext(Dispatchers.IO) {
                    apiService.getWeather(city, API_KEY)
                }
                showWeather(response)
            } catch (e: Exception) {
                showError(e.message ?: "Неизвестная ошибка")
            }
        }
    }

    private fun showWeather(data: WeatherResponse) {
        progressBar.visibility = View.GONE
        errorTextView.visibility = View.GONE

        weatherCard.visibility = View.VISIBLE
        tempTextView.visibility = View.VISIBLE
        descTextView.visibility = View.VISIBLE
        iconImageView.visibility = View.VISIBLE


        tempTextView.text = "${data.main.temp}°F"

        descTextView.text = data.weather[0].description.capitalize()

        val icon = data.weather[0].icon
        val url = "https://openweathermap.org/img/wn/${icon}@4x.png"

        Glide.with(this)
            .load(url)
            .into(iconImageView)
    }


    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        errorTextView.visibility = View.GONE
        tempTextView.visibility = View.GONE
        descTextView.visibility = View.GONE
        iconImageView.visibility = View.GONE
        getWeatherButton.isEnabled = false
    }

    private fun showError(message: String) {
        progressBar.visibility = View.GONE
        errorTextView.visibility = View.VISIBLE
        errorTextView.text = "Ошибка: $message"
        tempTextView.visibility = View.GONE
        descTextView.visibility = View.GONE
        iconImageView.visibility = View.GONE
        getWeatherButton.isEnabled = true
    }

//    private fun showContent(response: WeatherResponse) {
//        progressBar.visibility = View.GONE
//        errorTextView.visibility = View.GONE
//        tempTextView.visibility = View.VISIBLE
//        descTextView.visibility = View.VISIBLE
//        iconImageView.visibility = View.VISIBLE
//
//        tempTextView.text = "${response.main.temp} °C"
//        descTextView.text = response.weather.firstOrNull()?.description ?: "Нет описания"
//
//        val iconCode = response.weather.firstOrNull()?.icon ?: ""
//        val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
//        Glide.with(this).load(iconUrl).into(iconImageView)
//
//        getWeatherButton.isEnabled = false
//    }
}