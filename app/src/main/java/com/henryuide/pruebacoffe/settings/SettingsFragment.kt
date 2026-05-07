package com.henryuide.pruebacoffe.settings

import android.hardware.Camera
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.henryuide.pruebacoffe.R
import com.henryuide.pruebacoffe.Utils
import com.henryuide.pruebacoffe.camera.CameraSource

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        setUpRearCameraPreviewSizePreference()
    }

    private fun setUpRearCameraPreviewSizePreference() {
        var camera: Camera? = null
        try {
            camera = Camera.open(CameraSource.CAMERA_FACING_BACK)
            val previewSizeList = Utils.generateValidPreviewSizeList(camera!!)
            val previewSizeStringValues = arrayOfNulls<String>(previewSizeList.size)
            val previewToPictureSizeStringMap = HashMap<String, String>()
            for (i in previewSizeList.indices) {
                val sizePair = previewSizeList[i]
                previewSizeStringValues[i] = sizePair.preview.toString()
                if (sizePair.picture != null) {
                    previewToPictureSizeStringMap[sizePair.preview.toString()] =
                        sizePair.picture.toString()
                }
            }
        } catch (e: Exception) {
            // If there's no camera for the given camera id, hide the corresponding preference.
//            previewSizePreference.parent?.removePreference(previewSizePreference)
        } finally {
            camera?.release()
        }
    }
}