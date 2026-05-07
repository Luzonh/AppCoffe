package com.henryuide.pruebacoffe

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.crashlytics.buildtools.reloc.org.apache.commons.io.output.ByteArrayOutputStream
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.henryuide.pruebacoffe
.camera.FrameMetadata
import java.nio.ByteBuffer
import java.util.UUID

interface InputInfo {
    fun getBitmap(): Bitmap
}

class CameraInputInfo(
    private val frameByteBuffer: ByteBuffer,
    private val frameMetadata: FrameMetadata
) : InputInfo {

    private var bitmap: Bitmap? = null

    @Synchronized
    override fun getBitmap(): Bitmap {
        return bitmap ?: let {
            bitmap = Utils.convertToBitmap(
                frameByteBuffer, frameMetadata.width, frameMetadata.height, frameMetadata.rotation
            )
            bitmap!!
        }
    }
}

class BitmapInputInfo(private val bitmap: Bitmap) : InputInfo {
    override fun getBitmap(): Bitmap {
        return bitmap
    }
}
fun uploadToFirebase(inputInfo: InputInfo, userName: String, detectedLabel: String, description: String, scientificName: String, temperature: String, moreInformationUrl: String) {
    val bitmap = inputInfo.getBitmap()

    // Convertir el Bitmap a ByteArray
    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
    val data = baos.toByteArray()

    // Referencia al almacenamiento en Firebase
    val storageRef = FirebaseStorage.getInstance().reference
    val imageRef = storageRef.child("images/${UUID.randomUUID()}.jpg")

    // Subir el archivo a Firebase Storage
    val uploadTask = imageRef.putBytes(data)
    uploadTask.addOnFailureListener {
        // Manejar el fallo de la subida
        Log.e("FirebaseUpload", "Error al subir la imagen", it)
    }.addOnSuccessListener { taskSnapshot ->
        // Obtener la URL de descarga de la imagen subida
        imageRef.downloadUrl.addOnSuccessListener { uri ->
            val downloadUrl = uri.toString()

            // Guardar la URL en Firestore junto con los demás datos
            val db = FirebaseFirestore.getInstance()
            val detectionData = hashMapOf(
                "user" to userName,
                "timestamp" to FieldValue.serverTimestamp(),
                "detectedLabel" to detectedLabel,
                "description" to description,
                "scientificName" to scientificName,
                "temperature" to temperature,
                "urlInformation" to moreInformationUrl,
                "imageUrl" to downloadUrl // URL de la imagen en Firebase Storage
            )

            db.collection("detections").add(detectionData)
                .addOnSuccessListener {
                    Log.d("Firestore", "Datos guardados exitosamente")
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error al guardar datos", e)
                }
        }
    }
}
