package dev.jx.imgpic

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.lifecycle.lifecycleScope
import dev.jx.imgpic.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var internalPhotoAdapter: InternalPhotoAdapter
    private lateinit var externalPhotoAdapter: ExternalPhotoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdapters()
        setupRecyclerViews()

        val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) {
            if (it == null) return@registerForActivityResult
            val isSavedSuccessfully = savePhotoToInternalStorage(UUID.randomUUID().toString(), it)
            if (isSavedSuccessfully) {
                loadInternalPhotosIntoRecyclerView()
                Toast.makeText(this, "Photo saved successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cameraBtn.setOnClickListener {
            takePhoto.launch()
        }

        loadInternalPhotosIntoRecyclerView()
    }

    private fun setupAdapters() {
        internalPhotoAdapter = InternalPhotoAdapter {
            val isDeleteSuccess = deletePhotoFromInternalStorage(it.name)
            if (isDeleteSuccess) {
                loadInternalPhotosIntoRecyclerView()
                Toast.makeText(this, "Photo deleted successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Delete failed", Toast.LENGTH_SHORT).show()
            }
        }
        externalPhotoAdapter = ExternalPhotoAdapter()
    }

    private fun setupRecyclerViews() {
        binding.internalPhotosList.adapter = internalPhotoAdapter
        binding.externalPhotosList.adapter = externalPhotoAdapter
    }

    private fun loadInternalPhotosIntoRecyclerView() {
        lifecycleScope.launch {
            val photos = loadPhotosFromInternalStorage()
            internalPhotoAdapter.submitList(photos)
        }
    }

    private suspend fun loadPhotosFromInternalStorage(): List<Photo> {
        return withContext(Dispatchers.IO) {
            val files = filesDir.listFiles()
            files
                ?.filter { it.canRead() && it.isFile && it.name.endsWith(".jpg") }
                ?.map {
                    val bytes = it.readBytes()
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    Photo(name = it.name, bmp = bmp)
                } ?: listOf()
        }
    }

    private fun savePhotoToInternalStorage(filename: String, bmp: Bitmap): Boolean {
        return try {
            openFileOutput("$filename.jpg", MODE_PRIVATE).use {
                bmp.compress(Bitmap.CompressFormat.JPEG, 95, it)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun deletePhotoFromInternalStorage(filename: String): Boolean {
        return try {
            deleteFile(filename)
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

}
