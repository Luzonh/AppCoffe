package com.henryuide.pruebacoffe.camera

import android.graphics.Bitmap
import com.google.mlkit.vision.objects.DetectedObject
import com.henryuide.pruebacoffe.pestsearch.Pest

class SearchedObject (
    val detectedObjects: List<DetectedObject>,
    val pestList: List<Pest>
) {
    fun getObjectThumbnail(): Unit? {
        // Aquí puedes decidir qué thumbnail mostrar si hay múltiples objetos
        // Por ejemplo, podrías mostrar el thumbnail del primer objeto
        return detectedObjects.firstOrNull()?.let { detectedObject ->
            // Código para crear el thumbnail
        }
    }
}