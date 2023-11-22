package com.example.image_uplod


import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageSwitcher
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.FirebaseApp
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class MainActivity : AppCompatActivity() {

    private lateinit var imageSwitcher: ImageSwitcher
    private lateinit var pickImagesBtn: Button
    private lateinit var nextBtn: Button
    private lateinit var previousBtn: Button
    private lateinit var deleteBtn: Button

    private var images: ArrayList<Uri?> = ArrayList()
    private var position = 0
    private val PICK_IMAGES_CODE = 0
    private val storage = FirebaseStorage.getInstance()
    private val storageRef: StorageReference = storage.reference.child("images")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        FirebaseApp.initializeApp(this)

        imageSwitcher = findViewById(R.id.imageSwitcher)
        pickImagesBtn = findViewById(R.id.pickImagesBtn)
        nextBtn = findViewById(R.id.nextBtn)
        previousBtn = findViewById(R.id.previousBtn)
        deleteBtn = findViewById(R.id.deleteBtn)


        imageSwitcher.setFactory {
            ImageView(applicationContext)
        }

        pickImagesBtn.setOnClickListener {
            pickImagesIntent()
        }

        nextBtn.setOnClickListener {
            if (position < images.size - 1) {
                position++
                loadImage(images[position])
            } else {
                Toast.makeText(this, "No more images...", Toast.LENGTH_SHORT).show()
            }
        }

        previousBtn.setOnClickListener {
            if (position > 0) {
                position--
                loadImage(images[position])
            } else {
                Toast.makeText(this, "No more images...", Toast.LENGTH_SHORT).show()
            }
        }

        deleteBtn.setOnClickListener {
            deleteCurrentImage()
        }

        // Fetch images from Firebase Storage and populate the 'images' list
        fetchImagesFromFirebaseStorage()
    }

    private fun loadImage(imageUri: Uri?) {
        Glide.with(this)
            .load(imageUri)
            .into(imageSwitcher.currentView as ImageView)
    }

    private fun fetchImagesFromFirebaseStorage() {
        storageRef.listAll()
            .addOnSuccessListener { result ->
                images.clear()
                for (imageRef in result.items) {
                    // Download URLs for each image and add them to the 'images' list
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        images.add(uri)
                        // Set the first image in the ImageSwitcher
                        if (images.isNotEmpty()) {
                            loadImage(images[0])
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to fetch images: $exception", Toast.LENGTH_SHORT).show()
            }
    }

    private fun pickImagesIntent() {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Image(s)"), PICK_IMAGES_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGES_CODE && resultCode == Activity.RESULT_OK) {
            if (data!!.clipData != null) {
                val count = data.clipData!!.itemCount
                for (i in 0 until count) {
                    val imageUri = data.clipData!!.getItemAt(i).uri
                    uploadImageToFirebaseStorage(imageUri) // Upload each image to Firebase Storage
                }
            } else {
                val imageUri = data.data
                uploadImageToFirebaseStorage(imageUri) // Upload the single selected image to Firebase Storage
            }
        }
    }

    private fun uploadImageToFirebaseStorage(imageUri: Uri?) {
        val imageName = "${System.currentTimeMillis()}_${imageUri?.lastPathSegment}"
        val imagesRef = storageRef.child(imageName)

        val uploadTask = imagesRef.putFile(imageUri!!)

        uploadTask.addOnSuccessListener {
            Toast.makeText(this, "Image uploaded successfully", Toast.LENGTH_SHORT).show()
            // Refresh the image list after uploading
            fetchImagesFromFirebaseStorage()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteCurrentImage() {
        if (images.isNotEmpty() && position < images.size) {
            val currentImageRef = storage.getReferenceFromUrl(images[position].toString())

            currentImageRef.delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Image deleted successfully", Toast.LENGTH_SHORT).show()
                    // Refresh the image list after deletion
                    fetchImagesFromFirebaseStorage()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to delete image", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "No image to delete", Toast.LENGTH_SHORT).show()
        }
    }
}