package net.maxbraun.mirror;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * A helper class to detected faces with the front-facing camera.
 */
@ExperimentalGetImage
@RequiresApi(api = Build.VERSION_CODES.N)
public class Faces implements DefaultLifecycleObserver {
  private static final String TAG = Faces.class.getSimpleName();

  /**
   * The with of images requested from the camera.
   */
  private static final int TARGET_RESOLUTION_WIDTH = 480;

  /**
   * The height of images requested from the camera.
   */
  private static final int TARGET_RESOLUTION_HEIGHT = 360;

  /**
   * The number of frames in the face detection result buffer.
   */
  private static final int FACE_DETECTION_BUFFER_SIZE = 10;

  /**
   * The minimum width of a face to count as a detection.
   */
  private static final int MIN_FACE_WIDTH = 200;

  /**
   * The fraction of face detections needed to trigger {@link FaceCallback#onFaceEnter()}.
   */
  private static final float FACE_DETECTION_ENTER_THRESHOLD = 0.2f;

  /**
   * The fraction of missing face detections needed to trigger {@link FaceCallback#onFaceExit()}.
   */
  private static final float FACE_DETECTION_EXIT_THRESHOLD = 0.8f;

  private final ExecutorService cameraExecutor = Executors.newSingleThreadExecutor();
  private final Context context;
  private final LifecycleOwner lifecycleOwner;
  private final FaceCallback faceCallback;
  private FaceDetector faceDetector;
  private ImageAnalysis imageAnalysis;

  /**
   * A fixed-size FIFO buffer of face detection results. Used to manage hysteresis and delay.
   */
  private Queue<Boolean> faceDetectionBuffer = new LinkedBlockingQueue(FACE_DETECTION_BUFFER_SIZE);

  /**
   * A callback to notify listeners of faces entering and exiting the camera.
   */
  public interface FaceCallback {

    /**
     * Called when one more more faces become visible.
     */
    void onFaceEnter();

    /**
     * Called when no faces are visible anymore.
     */
    void onFaceExit();
  }

  public Faces(Context context, LifecycleOwner lifecycleOwner, FaceCallback faceCallback) {
    this.context = context;
    this.lifecycleOwner = lifecycleOwner;
    this.faceCallback = faceCallback;

    // Bind the camera to the provided lifecycle.
    lifecycleOwner.getLifecycle().addObserver(this);
  }

  @Override
  public void onResume(@NonNull LifecycleOwner owner) {
    // Start analyzing camera frames for faces.
    faceDetector = getFaceDetector();
    imageAnalysis = getImageAnalysis();
    getCameraProvider(cameraProvider -> {
      cameraProvider.unbindAll();
      cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA,
          imageAnalysis);
    });
  }

  @Override
  public void onPause(@NonNull LifecycleOwner owner) {
    // Stop analyzing camera frames for faces.
    getCameraProvider(cameraProvider -> {
      cameraProvider.unbind(imageAnalysis);
      imageAnalysis = null;
      faceDetector.close();
      faceDetector = null;
    });
  }

  /**
   * Gets a {@link ProcessCameraProvider} instance via a callback.
   */
  private void getCameraProvider(Consumer<ProcessCameraProvider> cameraProviderConsumer) {
    ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
        ProcessCameraProvider.getInstance(context);
    cameraProviderFuture.addListener(() -> {
      try {
        ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
        cameraProviderConsumer.accept(cameraProvider);
      } catch (ExecutionException | InterruptedException e) {
        Log.e(TAG, "Failed to access camera", e);
      }
    }, ContextCompat.getMainExecutor(context));
  }

  /**
   * Gets a configured {@link FaceDetector} instance.
   */
  private FaceDetector getFaceDetector() {
    FaceDetectorOptions faceDetectorOptions = new FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .build();
    return FaceDetection.getClient(faceDetectorOptions);
  }

  /**
   * Gets a configured {@link ImageAnalysis} instance.
   */
  private ImageAnalysis getImageAnalysis() {
    ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
        .setTargetResolution(new Size(TARGET_RESOLUTION_WIDTH, TARGET_RESOLUTION_HEIGHT))
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build();
    imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);
    return imageAnalysis;
  }

  /**
   * Analyzes one camera frame for faces.
   */
  private void analyzeImage(ImageProxy imageProxy) {
    // Extract the image in the correct orientation.
    int imageRotation = imageProxy.getImageInfo().getRotationDegrees();
    InputImage image = InputImage.fromMediaImage(imageProxy.getImage(), imageRotation);

    // Detect faces.
    faceDetector.process(image).addOnSuccessListener(faces -> {
      boolean faceDetected = false;
      for (Face face : faces) {
        Rect boundingBox = face.getBoundingBox();
        if (boundingBox.width() >= MIN_FACE_WIDTH) {
          // One large enough face is enough.
          faceDetected = true;
          break;
        }
      }

      // Update the buffer with this frame's detection result.
      updateFaceDetectionBuffer(faceDetected);

      // Process the whole buffer and call callbacks.
      processFaceDetectionBuffer();
    }).addOnFailureListener(e -> {
      Log.e(TAG, "Failed to detect faces", e);
    }).addOnCompleteListener(task -> {
      imageProxy.close();
    });
  }

  /**
   * Adds a new face detection state to the end of the buffer, making room at the start if needed.
   */
  private void updateFaceDetectionBuffer(boolean faceDetected) {
    if (!faceDetectionBuffer.offer(faceDetected)) {
      faceDetectionBuffer.poll();
      faceDetectionBuffer.add(faceDetected);
    }
  }

  /**
   * Processes the face detections in the buffer to determine which callbacks need to be called.
   */
  private void processFaceDetectionBuffer() {
    // Calculate statistics about the detection states in the buffer.
    int bufferSize = faceDetectionBuffer.size();
    long detectionCount = faceDetectionBuffer.stream().filter(faceDetected -> faceDetected).count();
    long noDetectionCount = bufferSize - detectionCount;
    float enterFraction = (float) detectionCount / bufferSize;
    float exitFraction = (float) noDetectionCount / bufferSize;

    // Call the appropriate callbacks.
    // TODO: Only call callbacks on state changes.
    if (enterFraction > FACE_DETECTION_ENTER_THRESHOLD) {
      faceCallback.onFaceEnter();
    } else if (exitFraction > FACE_DETECTION_EXIT_THRESHOLD) {
      faceCallback.onFaceExit();
    }
  }
}
