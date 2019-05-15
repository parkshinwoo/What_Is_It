package e.pshkh.what_is_it.navigation_activity

import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.otaliastudios.cameraview.CameraLogger
import com.otaliastudios.cameraview.CameraView
import e.pshkh.what_is_it.R
import kotlinx.android.synthetic.main.fragment_emotion.view.*

class EmotionFragment : Fragment() {
    private lateinit var cameraView: CameraView
    private lateinit var camearaImageView: ImageView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val emotionView = inflater!!.inflate(R.layout.fragment_emotion, container, false)

        cameraView = emotionView.emotion_camera_view
        camearaImageView = emotionView.emotion_camera_image_view
        cameraView.scaleX = -1f // 전면카메라 좌우반전
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE)
        cameraView.setLifecycleOwner(this@EmotionFragment)
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

            val faceDetector = FirebaseVision.getInstance().getVisionFaceDetector(options)
            faceDetector.detectInImage(firebaseVisionImage).addOnSuccessListener {
                camearaImageView.setImageBitmap(null)

                val bitmap = Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888) // 카메라 영상
                val canvas = Canvas(bitmap) // 카메라 위에 도형 그리기 위함
                val boxPaint = Paint()
                boxPaint.color = Color.RED
                boxPaint.style = Paint.Style.STROKE
                boxPaint.strokeWidth = 4F
                val textPaint = Paint()
                textPaint.isAntiAlias = true
                textPaint.color = Color.RED
                textPaint.textSize = 40.0f
                for (face in it) {
                    canvas.drawRect(face.boundingBox, boxPaint)
                    canvas.drawText(
                        "행복: ${face.smilingProbability}",
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
        return emotionView
    }


}
