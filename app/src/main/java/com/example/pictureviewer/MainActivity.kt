package com.example.pictureviewer

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import android.graphics.BitmapFactory
import java.net.HttpURLConnection
import java.net.URL

import com.bumptech.glide.Glide

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var editTextUrl: EditText
    private lateinit var textViewStatus: TextView

    private var photoFile: File? = null

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val file = photoFile
        if (result.resultCode == RESULT_OK && file != null && file.length() > 0) {
            Glide.with(this).load(file).into(imageView)
            textViewStatus.setText(R.string.status_photo_done)
        } else {
            textViewStatus.setText(R.string.status_photo_cancel)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        imageView = findViewById<ImageView>(R.id.imageView)
        editTextUrl = findViewById<EditText>(R.id.editTextUrl)
        textViewStatus = findViewById<TextView>(R.id.textViewStatus)

        findViewById<Button>(R.id.buttonResource).setOnClickListener { showResourceImage() }

        findViewById<Button>(R.id.buttonInternet).setOnClickListener { loadImageManually() }

        findViewById<Button>(R.id.buttonGlide).setOnClickListener { loadImageWithGlide() }

        savedInstanceState?.getString("photo_path")?.let { photoFile = File(it) }

        findViewById<Button>(R.id.buttonCamera).setOnClickListener { takePhoto(false) }
        findViewById<Button>(R.id.buttonSelfie).setOnClickListener { takePhoto(true) }

    }

    private fun showResourceImage() {
        imageView.setImageResource(R.drawable.ic_launcher_foreground)
        textViewStatus.setText(R.string.status_ready)
    }

    private fun loadImageManually() {
        val address = editTextUrl.text.toString().trim()
        if (address.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_url, Toast.LENGTH_SHORT).show()
            return
        }

        textViewStatus.setText(R.string.status_loading)

        Thread {
            try {
                val connection = URL(address).openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                val bitmap = connection.inputStream.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
                connection.disconnect()

                runOnUiThread {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap)
                        textViewStatus.setText(R.string.status_done)
                    } else {
                        textViewStatus.setText(R.string.status_error)
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    textViewStatus.setText(R.string.status_error)
                }
            }
        }.start()
    }

    private fun loadImageWithGlide() {
        val address = editTextUrl.text.toString().trim()
        if (address.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_url, Toast.LENGTH_SHORT).show()
            return
        }

        textViewStatus.setText(R.string.status_glide)

        Glide.with(this)
            .load(address)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .error(android.R.drawable.ic_dialog_alert)
            .into(imageView)
    }


    private fun takePhoto(front: Boolean) {
        val dir = File(cacheDir, "photos")
        dir.mkdirs()
        val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
        photoFile = file
        val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        if (front) {
            intent.putExtra("android.intent.extras.CAMERA_FACING", 1)
            intent.putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
            intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
        }

        try {
            cameraLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.error_no_camera, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("photo_path", photoFile?.absolutePath)
    }





}