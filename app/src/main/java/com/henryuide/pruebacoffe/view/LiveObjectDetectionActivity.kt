package com.henryuide.pruebacoffe.view

import android.Manifest
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.Camera
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.gms.common.internal.Objects
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.henryuide.pruebacoffe.R
import com.henryuide.pruebacoffe.camera.CameraSource
import com.henryuide.pruebacoffe.camera.CameraSourcePreview
import com.henryuide.pruebacoffe.camera.GraphicOverlay
import com.henryuide.pruebacoffe.camera.WorkflowModel
import com.henryuide.pruebacoffe.camera.WorkflowModel.WorkflowState
import com.henryuide.pruebacoffe.objectdetection.DetectedObjectInfo
import com.henryuide.pruebacoffe.objectdetection.MultiObjectProcessor
import com.henryuide.pruebacoffe.objectdetection.ProminentObjectProcessor
import com.henryuide.pruebacoffe.pestsearch.BottomSheetScrimView
import com.henryuide.pruebacoffe.pestsearch.Pest
import com.henryuide.pruebacoffe.settings.AboutActivity
import com.henryuide.pruebacoffe.settings.PreferenceUtils
import com.henryuide.pruebacoffe.settings.SettingsActivity
import java.io.IOException
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.henryuide.pruebacoffe.GoogleAuthHelper
import com.henryuide.pruebacoffe.view.onboarding.OnBoardingActivity
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import com.google.android.gms.location.FusedLocationProviderClient


private const val REQUEST_CODE_PERMISSIONS = 999 // Return code after asking for permission
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
class LiveObjectDetectionActivity : AppCompatActivity(), View.OnClickListener {

    private var cameraSource: CameraSource? = null
    private var preview: CameraSourcePreview? = null
    private var graphicOverlay: GraphicOverlay? = null
    private var settingsButton: View? = null
    private var aboutButton: View? = null
    private var exitButton: View? = null
    private var flashButton: View? = null
    private var promptChip: Chip? = null
    private var promptChipAnimator: AnimatorSet? = null
    private var searchButton: ExtendedFloatingActionButton? = null
    private var searchButtonAnimator: AnimatorSet? = null
    private var workflowModel: WorkflowModel? = null
    private var currentWorkflowState: WorkflowState? = null

    private var bottomSheetBehavior: BottomSheetBehavior<View>? = null
    private var bottomSheetScrimView: BottomSheetScrimView? = null
    private var bottomSheetTitleView: TextView? = null
    private var bottomSheetTitlePest: TextView? = null
    private var bottomSheetDescriptionsPest: TextView? = null
    private var bottomSheetTemperaturePest: TextView? = null
    private var bottomSheetSeeMore: TextView? = null
    private var bottomSheetScientificNamePest: TextView? = null
    private var bottomSheetImagePest: ImageView? = null
    private var objectThumbnailForBottomSheet: Bitmap? = null
    private var slidingSheetUpFromHiddenState: Boolean = false
    private lateinit var googleAuthHelper: GoogleAuthHelper
    private val REQ_ONE_TAP = 2
    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient
    private lateinit var signInRequest: BeginSignInRequest
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var btnSignOut: Button
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_object_detection)

        // Inicializar el FusedLocationProviderClient
        //fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Solicitar permiso de ubicación
        //requestLocationPermission()

        // Inicializa Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Configura Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))  // Usa tu ID de cliente de OAuth 2.0
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleAuthHelper = GoogleAuthHelper(this)

        //Salir de google
        // Inicializa el botón de cerrar sesión



        //checkAuthAndPermissions()


        // Inicializar componentes de la vista
        promptChip = findViewById(R.id.bottom_prompt_chip)
        promptChipAnimator = (AnimatorInflater.loadAnimator(
            this,
            R.animator.bottom_prompt_chip_enter
        ) as AnimatorSet).apply { setTarget(promptChip) }
        searchButton =
            findViewById<ExtendedFloatingActionButton>(R.id.product_search_button).apply {
                setOnClickListener(this@LiveObjectDetectionActivity)
            }
        searchButtonAnimator = (AnimatorInflater.loadAnimator(
            this,
            R.animator.search_button_enter
        ) as AnimatorSet).apply { setTarget(searchButton) }
        setUpBottomSheet()
        findViewById<View>(R.id.close_button).setOnClickListener(this)
        flashButton = findViewById<View>(R.id.flash_button).apply {
            setOnClickListener(this@LiveObjectDetectionActivity)
        }
        settingsButton = findViewById<View>(R.id.settings_button).apply {
            setOnClickListener(this@LiveObjectDetectionActivity)
        }
        aboutButton = findViewById<View>(R.id.about_button).apply {
            setOnClickListener(this@LiveObjectDetectionActivity)
        }
        exitButton = findViewById<View>(R.id.exit_button).apply {
            setOnClickListener {this@LiveObjectDetectionActivity}
        }
        // Verificar permisos y autenticación

        if (allPermissionsGranted()) {
             if (FirebaseAuth.getInstance().currentUser != null) {
                 initCameraComponents() // Usuario autenticado, iniciar cámara
             } else {
                 googleAuthHelper.signInWithGoogle() // Pedir autenticación
             }

        /*if (allPermissionsGranted()) {
            initCameraComponents()*/
        }else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    // nuevo codigo
    @RequiresApi(Build.VERSION_CODES.O)
   /*private fun signOut() {
        // Primero, cerramos sesión en Firebase
        auth.signOut()

        // Luego, cerramos sesión en Google
        googleSignInClient.signOut().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                Log.d("SignOut", "Google sign out successful")
                // Revocar el acceso
                googleSignInClient.revokeAccess().addOnCompleteListener(this) { revokeTask ->
                    if (revokeTask.isSuccessful) {
                        Log.d("SignOut", "Google access revoked")
                        // Reiniciar la actividad para volver al estado inicial
                        restartActivity()
                    } else {
                        Log.e("SignOut", "Failed to revoke access", revokeTask.exception)
                        showErrorMessage("Error al revocar el acceso. Intente de nuevo.")
                    }
                }
            } else {
                Log.e("SignOut", "Google sign out failed", task.exception)
                showErrorMessage("Error al cerrar sesión. Intente de nuevo.")
            }
        }
    }
    */
    private fun signOut() {
        // Cerrar sesión en Firebase
        FirebaseAuth.getInstance().signOut()

        // Cerrar sesión en Google
        googleSignInClient.signOut().addOnCompleteListener {
            // Verificar si el cierre fue exitoso
            Log.d(TAG, "Sesión cerrada en Google")
            // Redirigir al usuario a la pantalla de inicio de sesión (o volver a pedir autenticación)
            googleAuthHelper.signInWithGoogle()

        }.addOnFailureListener { e ->
            // Si ocurre un error al cerrar sesión
            Log.e(TAG, "Error al cerrar sesión en Google", e)
            showErrorMessage("Error al cerrar sesión. Inténtalo de nuevo.")
        }
    }
    private fun restartActivity() {
        val intent = Intent(this, OnBoardingActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkAuthAndPermissions() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            Log.d("Auth", "Usuario autenticado: ${currentUser.email}")
            if (allPermissionsGranted()) {
                initCameraComponents()
            } else {
                requestPermissions()
            }
        } else {
            Log.d("Auth", "Usuario no autenticado, iniciando autenticación")
            startAuthentication()
        }
    }

    private fun startAuthentication() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d("GoogleSignIn", "Google sign in successful")
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w("GoogleSignIn", "Google sign in failed", e)
                showErrorMessage("Error en la autenticación con Google. Código: ${e.statusCode}")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseAuth", "signInWithCredential:success")
                    checkAuthAndPermissions()
                } else {
                    Log.w("FirebaseAuth", "signInWithCredential:failure", task.exception)
                    showErrorMessage("Error en la autenticación con Firebase.")
                }
            }
    }



    private fun initViewComponents() {
    // Configura todos los componentes de la vista aquí
    promptChip = findViewById(R.id.bottom_prompt_chip)
    promptChipAnimator = (AnimatorInflater.loadAnimator(
        this,
        R.animator.bottom_prompt_chip_enter
    ) as AnimatorSet).apply { setTarget(promptChip) }
    searchButton =
        findViewById<ExtendedFloatingActionButton>(R.id.product_search_button).apply {
            setOnClickListener(this@LiveObjectDetectionActivity)
        }
    searchButtonAnimator = (AnimatorInflater.loadAnimator(
        this,
        R.animator.search_button_enter
    ) as AnimatorSet).apply { setTarget(searchButton) }
    setUpBottomSheet()
    findViewById<View>(R.id.close_button).setOnClickListener(this)
    flashButton = findViewById<View>(R.id.flash_button).apply {
        setOnClickListener(this@LiveObjectDetectionActivity)
    }
    settingsButton = findViewById<View>(R.id.settings_button).apply {
        setOnClickListener(this@LiveObjectDetectionActivity)
    }
    aboutButton = findViewById<View>(R.id.about_button).apply {
        setOnClickListener(this@LiveObjectDetectionActivity)
    }
    exitButton = findViewById<View>(R.id.exit_button).apply {
        setOnClickListener(this@LiveObjectDetectionActivity)
    }
}

    //nuevo codigo


    @RequiresApi(Build.VERSION_CODES.O)
    fun initCameraComponents() {
        preview = findViewById(R.id.camera_preview)
        graphicOverlay = findViewById<GraphicOverlay>(R.id.camera_preview_graphic_overlay).apply {
            setOnClickListener(this@LiveObjectDetectionActivity)
            cameraSource = CameraSource(this)
        }
        try {
            //graphicOverlay.cameraSource.start()
        } catch (e: Exception) {
            Log.e("CameraError", "Error al iniciar la cámara: ${e.message}")
            showErrorMessage("Error al iniciar la cámara. Por favor, intente de nuevo.")
        }
        setUpWorkflowModel()
    }

    private fun allPermissionsGranted(): Boolean = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    initCameraComponents()
                    initViewComponents()
                    Log.d("AuthStatus", "Current user: ${currentUser.email}")
                    //showErrorMessage("Ingreso con éxito.")
                } else {
                    Log.d(
                        "AuthStatus",
                        "Usuario no autenticado. Iniciando autenticación con Google."
                    )
                    googleAuthHelper.signInWithGoogle()
                    showErrorMessage("Error de Ingreso")
                }
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.permission_deny_text),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }


    override fun onResume() {
        super.onResume()
        workflowModel?.markCameraFrozen()
        settingsButton?.isEnabled = true
        aboutButton?.isEnabled = true
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        currentWorkflowState = WorkflowState.NOT_STARTED
        cameraSource?.setFrameProcessor(
            if (PreferenceUtils.isMultipleObjectsMode(this)) {
                MultiObjectProcessor(
                    graphicOverlay!!, workflowModel!!,
                    CUSTOM_MODEL_PATH,
                )
            } else {
                ProminentObjectProcessor(
                    graphicOverlay!!, workflowModel!!,
                    CUSTOM_MODEL_PATH,
                )
            }
        )
        workflowModel?.setWorkflowState(WorkflowState.DETECTING)
    }

    override fun onPause() {
        super.onPause()
        currentWorkflowState = WorkflowState.NOT_STARTED
        stopCameraPreview()
        errorSnackbar?.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        errorSnackbar?.dismiss()
        cameraSource?.release()
        cameraSource = null
    }

    override fun onBackPressed() {
        if (bottomSheetBehavior?.state != BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior?.setState(BottomSheetBehavior.STATE_HIDDEN)
        } else {
            super.onBackPressed()
        }
    }

    override fun onClick(view: View) {
        when (view.id) {

            R.id.product_search_button -> {
                searchButton?.isEnabled = false
                workflowModel?.onSearchButtonClicked()
            }

            R.id.bottom_sheet_scrim_view -> bottomSheetBehavior?.setState(BottomSheetBehavior.STATE_HIDDEN)
            R.id.close_button -> onBackPressed()
            R.id.close_button -> finish()
            R.id.flash_button -> {
                if (flashButton?.isSelected == true) {
                    flashButton?.isSelected = false
                    cameraSource?.updateFlashMode(Camera.Parameters.FLASH_MODE_OFF)
                } else {
                    flashButton?.isSelected = true
                    cameraSource?.updateFlashMode(Camera.Parameters.FLASH_MODE_TORCH)
                }
            }

            R.id.settings_button -> {
                settingsButton?.isEnabled = false
                startActivity(Intent(this, SettingsActivity::class.java))
            }

            R.id.about_button -> {
                aboutButton?.isEnabled = false
                startActivity(Intent(this, AboutActivity::class.java))
            }

            R.id.exit_button -> {
                exitButton?.isEnabled = false
                startActivity(Intent(this, SettingsActivity::class.java))
            }

        }
    }

    private fun startCameraPreview() {
        val cameraSource = this.cameraSource ?: return
        val workflowModel = this.workflowModel ?: return
        if (!workflowModel.isCameraLive) {
            try {
                workflowModel.markCameraLive()
                preview?.start(cameraSource)
            } catch (e: IOException) {
                Log.e(TAG, "Failed to start camera preview!", e)
                cameraSource.release()
                this.cameraSource = null
            }
        }
    }

    private fun stopCameraPreview() {
        if (workflowModel?.isCameraLive == true) {
            workflowModel!!.markCameraFrozen()
            flashButton?.isSelected = false
            preview?.stop()
        }
    }

    private fun setUpBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet))

        bottomSheetBehavior?.setBottomSheetCallback(
            object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    Log.d(TAG, "Bottom sheet new state: $newState")
                    bottomSheetScrimView?.visibility =
                        if (newState == BottomSheetBehavior.STATE_HIDDEN) View.GONE else View.VISIBLE
                    graphicOverlay?.clear()

                    when (newState) {
                        BottomSheetBehavior.STATE_HIDDEN -> workflowModel?.setWorkflowState(
                            WorkflowState.DETECTING
                        )

                        BottomSheetBehavior.STATE_COLLAPSED,
                        BottomSheetBehavior.STATE_EXPANDED,
                        BottomSheetBehavior.STATE_HALF_EXPANDED,
                        -> slidingSheetUpFromHiddenState =
                            false

                        BottomSheetBehavior.STATE_DRAGGING, BottomSheetBehavior.STATE_SETTLING -> {
                        }
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    val searchedObject = workflowModel!!.searchedObject.value
                    if (searchedObject == null || java.lang.Float.isNaN(slideOffset)) {
                        return
                    }

                    val graphicOverlay = graphicOverlay ?: return
                    val bottomSheetBehavior = bottomSheetBehavior ?: return
                    val collapsedStateHeight =
                        bottomSheetBehavior.peekHeight.coerceAtMost(bottomSheet.height)
                    val bottomBitmap = objectThumbnailForBottomSheet ?: return
                    if (slidingSheetUpFromHiddenState) {
                        val thumbnailSrcRect =
                            graphicOverlay.translateRect(searchedObject.boundingBox)
                        bottomSheetScrimView?.updateWithThumbnailTranslateAndScale(
                            bottomBitmap,
                            collapsedStateHeight,
                            slideOffset,
                            thumbnailSrcRect
                        )
                    } else {
                        bottomSheetScrimView?.updateWithThumbnailTranslate(
                            bottomBitmap, collapsedStateHeight, slideOffset, bottomSheet
                        )
                    }
                }
            })

        bottomSheetScrimView =
            findViewById<BottomSheetScrimView>(R.id.bottom_sheet_scrim_view).apply {
                setOnClickListener(this@LiveObjectDetectionActivity)
            }

        bottomSheetTitleView = findViewById(R.id.bottom_sheet_title)
        bottomSheetTitlePest = findViewById(R.id.tvTitle)
        bottomSheetImagePest = findViewById(R.id.imageView)
        bottomSheetDescriptionsPest = findViewById(R.id.tvDescriptions)
        bottomSheetTemperaturePest = findViewById(R.id.tvTemperature)
        bottomSheetScientificNamePest = findViewById(R.id.tvScientificName)
        bottomSheetSeeMore = findViewById(R.id.tvSeeMore)
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun setUpWorkflowModel() {
        workflowModel = ViewModelProviders.of(this).get(WorkflowModel::class.java).apply {

            // Observes the workflow state changes, if happens, update the overlay view indicators and
            // camera preview state.
            workflowState.observe(this@LiveObjectDetectionActivity, Observer { workflowState ->
                if (workflowState == null || Objects.equal(currentWorkflowState, workflowState)) {
                    return@Observer
                }
                currentWorkflowState = workflowState
                Log.d(TAG, "Current workflow state: ${workflowState.name}")

                if (PreferenceUtils.isAutoSearchEnabled(this@LiveObjectDetectionActivity)) {
                    stateChangeInAutoSearchMode(workflowState)
                } else {
                    stateChangeInManualSearchMode(workflowState)
                }
            })

            // product search results.

            objectToSearch.observe(this@LiveObjectDetectionActivity) { detectObject ->

                // Filtrar objetos que no sean hojas de café

                val validLabels = listOf(
                    getString(R.string.id_antracnosis),
                    getString(R.string.id_ojo_de_gallo),
                    getString(R.string.id_roya),
                    getString(R.string.id_miner)
                )
                val isCoffeeLeaf = isCoffeeLeafDetected(detectObject)
                val detectedLabel = detectObject.labels.firstOrNull()?.text ?: ""

                // Check if the detected object is a coffee leaf and has a valid disease/pest label
                if (isCoffeeLeaf && detectedLabel in validLabels) {
                    val pestList: List<Pest> = detectObject.labels.map { label ->
                        //data class Info(val mainInfo: String, val extraInfo: String)
                        val information = when (label.text) {
                            getString(R.string.id_antracnosis) -> getString(R.string.msg_antracnosis)
                            getString(R.string.id_ojo_de_gallo) -> getString(R.string.msg_ojo_de_gallo)
                            getString(R.string.id_roya) -> getString(R.string.msg_roya)


                            getString(R.string.id_miner) -> getString(R.string.msg_miner)
                            else -> getString(R.string.msg_default)
                        }
                        val scientificName = when (label.text) {
                            getString(R.string.id_antracnosis) -> getString(R.string.scientific_name_antracnosis)
                            getString(R.string.id_ojo_de_gallo) -> getString(R.string.scientific_name_ojo_de_gallo)
                            getString(R.string.id_roya) -> getString(R.string.scientific_name_roya)
                            getString(R.string.id_miner) -> getString(R.string.scientific_name_miner)
                            else -> getString(R.string.msg_default)
                        }
                        val imgPreview = when (label.text) {
                            getString(R.string.id_antracnosis) -> R.drawable.antraconosis
                            getString(R.string.id_ojo_de_gallo) -> R.drawable.ojo_de_gallo
                            getString(R.string.id_roya) -> R.drawable.roya
                            getString(R.string.id_miner) -> R.drawable.minador
                            else -> R.drawable.default_leaf
                        }
                        val temperature = when (label.text) {
                            getString(R.string.id_antracnosis) -> getString(R.string.temperature_antracnosis)
                            getString(R.string.id_ojo_de_gallo) -> getString(R.string.temperature_ojo_de_gallo)
                            getString(R.string.id_roya) -> getString(R.string.temperature_roya)
                            getString(R.string.id_miner) -> getString(R.string.temperature_roya)
                            else -> getString(R.string.msg_default)
                        }
                        val moreInformation = when (label.text) {
                            getString(R.string.id_antracnosis) -> "https://perfectdailygrind.com/es/2021/04/27/una-enfermedad-silenciosa-que-es-la-antracnosis-del-cafe/"
                            getString(R.string.id_ojo_de_gallo) -> "http://royacafe.lanref.org.mx/Documentos/FTNo49Mycenacitricolor.pdf"
                            getString(R.string.id_roya) -> "https://cropaia.com/es/blog/la-roya/"
                            getString(R.string.id_miner) -> "https://www.agronegocios.co/agricultura/consejos-del-profesor-yarumo-el-minador-de-la-hoja-de-cafe-2975523"
                            else -> getString(R.string.msg_default)
                        }
                        Pest(
                            label.text,
                            scientificName,
                            information,
                            imgPreview,
                            temperature,
                            moreInformation
                        )
                    }
                    workflowModel?.onSearchCompleted(detectObject, pestList)

                    // Guarda la información en Firestore y Storage
                    saveDetectionData(detectObject, pestList)
                } else if (!isCoffeeLeaf) {
                    // Show error message if no coffee leaf is detected
                    showErrorMessage("No se ha detectado una hoja de café. Por favor, enfoque correctamente la hoja de café.")

                } else {
                    // Show error message for invalid object
                    showErrorMessage("No se ha detectado una hoja de café válida. Por favor, enfoque correctamente el objeto.")
                }
            }

            // to present search result.
            searchedObject.observe(this@LiveObjectDetectionActivity) { searchedObject ->

                val productList = searchedObject.pestList
                objectThumbnailForBottomSheet = searchedObject.getObjectThumbnail()
                getString(R.string.detected)
                /*
                bottomSheetTitleView?.text = resources.getQuantityString(
                    R.plurals.bottom_sheet_title,
                    //R.string.detected,

                    productList.size,
                    productList.size
                )*/

                if (productList.isEmpty() || !productList.any {
                        it.title.toLowerCase().contains("")
                    }) {
                    // No se detectó ninguna hoja o la lista está vacía
                    showErrorMessage(getString(R.string.error_no_coffee_leaf))
                } else {
                    objectThumbnailForBottomSheet = searchedObject.getObjectThumbnail()
                    getString(R.string.detected)
                    /*
                    bottomSheetTitleView?.text = resources.getQuantityString(
                        R.plurals.bottom_sheet_title,
                        productList.size,
                        productList.size
                    )*/

                    bottomSheetTitlePest?.text = productList.firstOrNull()?.title
                        ?: getString(R.string.msg_default) //Load Title
                    bottomSheetImagePest?.setImageResource(
                        productList.firstOrNull()?.imageUrl ?: R.drawable.tfl2_logo
                    ) //Load ImagePreview
                    bottomSheetDescriptionsPest?.text = (productList.firstOrNull()?.description
                        ?: getString(R.string.msg_default)) //Load Description

                    bottomSheetScientificNamePest?.text = (productList.firstOrNull()?.subtitle
                        ?: getString(R.string.msg_default)) //Load Scientific Name
                    bottomSheetTemperaturePest?.text = (productList.firstOrNull()?.temperature
                        ?: getString(R.string.msg_default)) //Load Scientific Name
                    bottomSheetSeeMore?.setOnClickListener {
                        openBrowser(
                            productList.firstOrNull()?.urlInformation
                                ?: getString(R.string.msg_default)
                        )
                    }
                    slidingSheetUpFromHiddenState = true
                    bottomSheetBehavior?.peekHeight =
                        preview?.height?.div(1) ?: BottomSheetBehavior.PEEK_HEIGHT_AUTO
                    bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                }
            }
        }
    }

    // Función para guardar los datos en Firestore y la imagen en Firebase Storage
    @RequiresApi(Build.VERSION_CODES.O)

    private fun saveDetectionData(detectObjectInfo: DetectedObjectInfo, pestList: List<Pest>) {
        val db = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance().reference

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            val userEmail = currentUser.email ?: "Correo Desconocido"
            val userName = currentUser.displayName ?: "Nombre Desconocido"
            val timestamp = System.currentTimeMillis()
            val currentDateTime = LocalDateTime.now()
            val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

            // Obtener ubicación actual
            val location = getCurrentLocation() // Método que debes implementar

            val detectionData = hashMapOf(
                "user" to userEmail,
                "userName" to userName,
                "detectedLabel" to pestList.firstOrNull()?.title,
                "temperature" to pestList.firstOrNull()?.temperature,
                "location" to location, // Añadir ubicación
                "date" to currentDateTime.format(dateFormatter),
                "time" to currentDateTime.format(timeFormatter)
            )

            // Obtener el Bitmap desde detectObjectInfo
            val bitmap = detectObjectInfo.getBitmap()

            if (bitmap != null) {
                // Guardar imagen en Firebase Storage
                val imageRef = storage.child("detections/${timestamp}.jpg")
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val imageData = baos.toByteArray()

                imageRef.putBytes(imageData)
                    .addOnSuccessListener {
                        // Obtener URL de descarga de la imagen
                        imageRef.downloadUrl.addOnSuccessListener { uri ->
                            detectionData["imageUrl"] = uri.toString()

                            // Guardar los datos en Firestore
                            db.collection("detections")
                                .add(detectionData)
                                .addOnSuccessListener {
                                    Log.d(TAG, "Detección guardada con éxito")
                                    showRatingDialog(detectionData) // Mostrar el diálogo de calificación
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error al guardar detección", e)
                                    showErrorMessage("Error al guardar detección.")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error al subir la imagen", e)
                    }
            } else {
                showErrorMessage("No se pudo obtener el Bitmap para la imagen.")
            }
        } else {
            showErrorMessage("Usuario no autenticado.")
        }
    }

    // Método para mostrar el diálogo de calificación
    private fun showRatingDialog(detectionData: HashMap<String, String?>) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Calificación")
        builder.setMessage("¿Te gusta esta detección?")

        builder.setPositiveButton("Like") { _, _ ->
            detectionData["rating"] = "Like"
            saveRating(detectionData)
        }

        builder.setNegativeButton("No Like") { _, _ ->
            detectionData["rating"] = "No Like"
            saveRating(detectionData)
        }

        builder.show()
    }

    // Método para guardar la calificación en Firestore
    private fun saveRating(detectionData: HashMap<String, String?>) {
        // Aquí puedes guardar la calificación en Firestore si lo deseas
        // Por ejemplo:
        FirebaseFirestore.getInstance().collection("detections")
            .document(detectionData["documentId"].toString())
            .update("rating", detectionData["rating"])
            .addOnSuccessListener {
                Log.d(TAG, "Calificación guardada con éxito")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al guardar calificación", e)
            }
    }

    // Implementar método para obtener la ubicación actual
    /*private fun requestLocationPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // Permiso concedido, puedes obtener la ubicación
            getCurrentLocation()
        }
    }*/
    private fun getCurrentLocation(): String {
        // Aquí debes implementar la lógica para obtener la ubicación actual
        // Esto puede implicar el uso de FusedLocationProviderClient para obtener la ubicación
        return "Latitud, Longitud" // Retorna una cadena con la ubicación
    }

    private var errorSnackbar: Snackbar? = null
    @RequiresApi(Build.VERSION_CODES.O)
    fun showErrorMessage(message: String) {
        // Ocultar el bottom sheet si está visible
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        errorSnackbar?.dismiss()
        // Mostrar un mensaje de error, por ejemplo, usando un Snackbar
        errorSnackbar = Snackbar.make(findViewById(android.R.id.content),message, Snackbar.LENGTH_INDEFINITE)
            .setAction(getString(R.string.action_retry)) {
                // Acción para reintentar la detección
                retryDetection()
                // Descartar el Snackbar inmediatamente al presionar "Reintentar"
                errorSnackbar?.dismiss()

            }
        errorSnackbar?.show()

    }


    private fun isCoffeeLeafDetected(detectObject: DetectedObjectInfo): Boolean {

        return detectObject.labels.any { label ->
            //label.text in listOf((R.string.id_roya)
            label.text == getString(R.string.id_roya) ||
                    label.text == getString(R.string.id_antracnosis)
            //label.text == getString(R.string.id_ojo_de_gallo)
            //label.text == getString(R.string.id_miner) // Assume this is a valid label for coffee leaf

        }
    }

    private fun retryDetection() {
        // Descartar el Snackbar de error si está visible
        errorSnackbar?.dismiss()
        errorSnackbar = null

        // Reiniciar el proceso de detección
        workflowModel?.markCameraFrozen()
        settingsButton?.isEnabled = true
        aboutButton?.isEnabled = true
        bottomSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        currentWorkflowState = WorkflowState.NOT_STARTED
        cameraSource?.setFrameProcessor(
            if (PreferenceUtils.isMultipleObjectsMode(this)) {
                MultiObjectProcessor(
                    graphicOverlay!!, workflowModel!!,
                    CUSTOM_MODEL_PATH,

                    )
            } else {
                ProminentObjectProcessor(
                    graphicOverlay!!, workflowModel!!,
                    CUSTOM_MODEL_PATH,

                    )
            }
        )
        workflowModel?.setWorkflowState(WorkflowState.DETECTING)
    }


    private fun openBrowser(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun stateChangeInAutoSearchMode(workflowState: WorkflowState) {
        val wasPromptChipGone = promptChip!!.visibility == View.GONE

        searchButton?.visibility = View.GONE
        when (workflowState) {
            WorkflowState.DETECTING, WorkflowState.DETECTED, WorkflowState.CONFIRMING -> {
                promptChip?.visibility = View.VISIBLE
                promptChip?.setText(
                    if (workflowState == WorkflowState.CONFIRMING)
                        R.string.prompt_hold_camera_steady
                    else
                        R.string.prompt_point_at_a_bird
                )
                startCameraPreview()
            }

            WorkflowState.CONFIRMED -> {
                promptChip?.visibility = View.VISIBLE
                promptChip?.setText(R.string.prompt_searching)
                stopCameraPreview()
            }

            WorkflowState.SEARCHING -> {
                promptChip?.visibility = View.GONE
                stopCameraPreview()
            }

            WorkflowState.SEARCHED -> {
                stopCameraPreview()
            }

            else -> promptChip?.visibility = View.GONE
        }

        val shouldPlayPromptChipEnteringAnimation =
            wasPromptChipGone && promptChip?.visibility == View.VISIBLE
        if (shouldPlayPromptChipEnteringAnimation && promptChipAnimator?.isRunning == false) {
            promptChipAnimator?.start()
        }
    }

    private fun stateChangeInManualSearchMode(workflowState: WorkflowState) {
        val wasPromptChipGone = promptChip?.visibility == View.GONE
        val wasSearchButtonGone = searchButton?.visibility == View.GONE

        when (workflowState) {
            WorkflowState.DETECTING, WorkflowState.DETECTED, WorkflowState.CONFIRMING -> {
                promptChip?.visibility = View.VISIBLE
                promptChip?.setText(R.string.prompt_point_at_an_object)
                searchButton?.visibility = View.GONE
                startCameraPreview()
            }

            WorkflowState.CONFIRMED -> {
                promptChip?.visibility = View.GONE
                searchButton?.visibility = View.VISIBLE
                searchButton?.isEnabled = true
                searchButton?.setBackgroundColor(Color.WHITE)
                startCameraPreview()
            }

            WorkflowState.SEARCHING -> {
                promptChip?.visibility = View.GONE
                searchButton?.visibility = View.VISIBLE
                searchButton?.isEnabled = false
                searchButton?.setBackgroundColor(Color.GRAY)
                stopCameraPreview()
            }

            WorkflowState.SEARCHED -> {
                promptChip?.visibility = View.GONE
                searchButton?.visibility = View.GONE
                stopCameraPreview()
            }

            else -> {
                promptChip?.visibility = View.GONE
                searchButton?.visibility = View.GONE
            }
        }

        val shouldPlayPromptChipEnteringAnimation =
            wasPromptChipGone && promptChip?.visibility == View.VISIBLE
        promptChipAnimator?.let {
            if (shouldPlayPromptChipEnteringAnimation && !it.isRunning) it.start()
        }

        val shouldPlaySearchButtonEnteringAnimation =
            wasSearchButtonGone && searchButton?.visibility == View.VISIBLE
        searchButtonAnimator?.let {
            if (shouldPlaySearchButtonEnteringAnimation && !it.isRunning) it.start()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onGoogleSignInSuccess() {
        initCameraComponents()
    }
      companion object {
         // private const val LOCATION_PERMISSION_REQUEST_CODE = 1
          private const val TAG = "LiveObjectDetection"
          private const val RC_SIGN_IN = 9001
          //private const val CUSTOM_MODEL_PATH = "plagas_detector_v1.tflite"
          private const val CUSTOM_MODEL_PATH = "enfermedades_cafe.tflite"


    }
}