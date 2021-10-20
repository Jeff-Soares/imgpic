package dev.jx.imgpic

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import dev.jx.imgpic.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var internalPhotoAdapter: InternalPhotoAdapter
    private lateinit var externalPhotoAdapter: ExternalPhotoAdapter

    private val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    private lateinit var permissionsLauncher: ActivityResultLauncher<String>
    private var writePermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAdapters()
        setupRecyclerViews()

        permissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                writePermission = isGranted ?: writePermission

                if (!isGranted) {
                    val snackbar =
                        Snackbar.make(binding.root, "Permission denied.", Snackbar.LENGTH_LONG)
                    snackbar.setAction("Settings") {
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        intent.data = Uri.fromParts("package", packageName, null)
                        startActivity(intent)
                    }.show()
                }
            }

        val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) {
            if (it == null) return@registerForActivityResult
            val isPrivate = binding.switchPrivate.isChecked

            val isSavedSuccessfully = when {
                isPrivate -> savePhotoToInternalStorage(UUID.randomUUID().toString(), it)
                writePermission -> savePhotoToExternalStorage(UUID.randomUUID().toString(), it)
                else -> false
            }

            if (isSavedSuccessfully) {
                setInternalPhotosList()
                Toast.makeText(this, "Photo saved successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
            }

        }

        binding.cameraBtn.setOnClickListener {
            verifyPermissions()
            takePhoto.launch()
        }

        setInternalPhotosList()
    }

    private fun setupAdapters() {
        internalPhotoAdapter = InternalPhotoAdapter {
            val isDeleteSuccess = deletePhotoFromInternalStorage(it.name)
            if (isDeleteSuccess) {
                setInternalPhotosList()
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

    private fun setInternalPhotosList() {
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

    @SuppressLint("InlinedApi")
    private fun savePhotoToExternalStorage(filename: String, bmp: Bitmap): Boolean {
        val imageCollection = if (minSdk29) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
            put(MediaStore.Images.Media.WIDTH, bmp.width)
            put(MediaStore.Images.Media.HEIGHT, bmp.height)
            if (minSdk29) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + File.separator + getString(R.string.app_name)
                )
            }
        }

        return try {
            contentResolver.insert(imageCollection, contentValues)?.also { uri ->
                contentResolver.openOutputStream(uri).use { outputStream ->
                    if (!bmp.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                        throw IOException("Compress image failed")
                    }
                }
            } ?: throw IOException("Uri is null")
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun verifyPermissions() {
        writePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED || minSdk29

        if (!writePermission) {
            permissionsLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

}
