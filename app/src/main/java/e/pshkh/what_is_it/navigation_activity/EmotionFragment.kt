package e.pshkh.what_is_it.navigation_activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.storage.FirebaseStorage
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraLogger
import com.otaliastudios.cameraview.CameraView
import e.pshkh.what_is_it.EmotionResultActivity
import e.pshkh.what_is_it.R
import kotlinx.android.synthetic.main.fragment_emotion.view.*
import java.io.File
import java.io.FileOutputStream


class EmotionFragment : Fragment() {
    var storage: FirebaseStorage? = null
    var auth: FirebaseAuth? = null
    var firestore: FirebaseFirestore? = null

    private lateinit var cameraView: CameraView
    private lateinit var camearaImageView: ImageView
    private lateinit var faceDetector: FirebaseVisionFaceDetector
    private lateinit var captureBtn: ImageButton
    private lateinit var canvas: Canvas
    private lateinit var bitmap: Bitmap
    private lateinit var emotion: String
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        storage = FirebaseStorage.getInstance()
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val emotionView = inflater!!.inflate(R.layout.fragment_emotion, container, false)

        cameraView = emotionView.emotion_camera_view
        camearaImageView = emotionView.emotion_camera_image_view
        cameraView.scaleX = -1f // 전면카메라 좌우반전
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE)
        cameraView.setLifecycleOwner(this)
        emotion = ""
        cameraView.addFrameProcessor { frame ->
            val width = frame.size.width
            val height = frame.size.height
            val metadata = FirebaseVisionImageMetadata.Builder()
                .setWidth(width)
                .setHeight(height)
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                .setRotation(FirebaseVisionImageMetadata.ROTATION_270)
                .build()

            val firebaseVisionImage = FirebaseVisionImage.fromByteArray(frame.data, metadata)
            val options = FirebaseVisionFaceDetectorOptions.Builder()
                .setContourMode(FirebaseVisionFaceDetectorOptions.ALL_CONTOURS)
                .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
                .build()

            faceDetector = FirebaseVision.getInstance().getVisionFaceDetector(options)
            faceDetector.detectInImage(firebaseVisionImage).addOnSuccessListener {
                camearaImageView.setImageBitmap(null)

                bitmap = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888)
                canvas = Canvas(bitmap) // 카메라 위에 도형 그리기 위함
                val boxPaint = Paint()
                boxPaint.color = Color.GREEN
                boxPaint.style = Paint.Style.STROKE
                boxPaint.strokeWidth = 4F
                val textPaint = Paint()
                textPaint.isAntiAlias = true
                textPaint.color = Color.GREEN
                textPaint.textSize = 40.0f
                for (face in it) {
                    Log.d("Emotion", face.smilingProbability.toString())
                    emotion = "행복"
                    if (face.smilingProbability < 0.20f) {
                        emotion = "화남"
                        textPaint.color = Color.RED
                        boxPaint.color = Color.RED
                    } else if (face.smilingProbability < 0.80f) {
                        emotion = "보통"
                        textPaint.color = Color.YELLOW
                        boxPaint.color = Color.YELLOW
                    }
                    canvas.drawRect(face.boundingBox, boxPaint)
                    canvas.drawText(
                        emotion,
                        face.boundingBox.right.toFloat(),
                        face.boundingBox.bottom.toFloat(),
                        textPaint
                    )
                }

                camearaImageView.setImageBitmap(bitmap)

            }.addOnFailureListener {
                camearaImageView.setImageBitmap(null)
            }

        }

        captureBtn = emotionView.captureBtn
        captureBtn.setOnClickListener {
            cameraView.capturePicture()

        }


        cameraView.addCameraListener(object : CameraListener() {

            override fun onPictureTaken(jpeg: ByteArray?) {
                super.onPictureTaken(jpeg)
                val resultBitmap =
                    BitmapFactory.decodeByteArray(jpeg, 0, jpeg!!.size).copy(Bitmap.Config.ARGB_8888, true)
                val resultCanvas = Canvas(resultBitmap)
                resultCanvas.drawBitmap(bitmap, Matrix(), null)
                SaveImage(context).execute(resultBitmap)
            }
        })

        return emotionView
    }


    inner class SaveImage(context: Context?) : AsyncTask<Bitmap, Void, String>() {

        private var context: Context? = context

        override fun doInBackground(vararg params: Bitmap?): String {
            val path = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/whatisit")
            if (!path.exists()) {
                path.mkdirs()
            }
            val fileName = String.format("%d.jpg", System.currentTimeMillis())
            val outputFile = File(path, fileName)

            outputFile.createNewFile()
            val out = FileOutputStream(outputFile)
            val data = params.get(0) as Bitmap
            data.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()

            val mediaScan = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScan.data = Uri.fromFile(outputFile)
            context!!.sendBroadcast(mediaScan)
            return outputFile.absolutePath
            return "failed"
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            val resultIntent = Intent(context, EmotionResultActivity::class.java)

            resultIntent.putExtra("jpeg", result)
            resultIntent.putExtra("emotion", emotion)
            startActivityForResult(resultIntent,1000)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == 1000){
                val diary_id = data!!.getStringExtra("diary_id")
                val uri = data!!.getParcelableExtra<Uri?>("uri")
                firestore!!.collection("DiaryBook").document(auth?.currentUser?.uid!!).collection("diary")
                    .document(diary_id!!).update("question", uri)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


}
