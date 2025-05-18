//package com.example.auralens;
//
//import android.Manifest;
//import android.content.Context;
//import android.content.pm.PackageManager;
//import android.media.AudioFormat;
//import android.media.AudioRecord;
//import android.media.MediaRecorder;
//
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//
//import org.tensorflow.lite.task.audio.classifier.AudioClassifier;
//import org.tensorflow.lite.support.audio.TensorAudio;
//import org.tensorflow.lite.task.audio.classifier.Classifications;
//
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class SoundCapture extends AppCompatActivity {
//
//    public interface DetectionListener {
//        void onSoundDetected(String label, float confidence, String timestamp);
//        void onError(String errorMessage);
//    }
//
//    private static final int SAMPLE_RATE = 16000;
//    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
//    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
//
//    private final Context context;
//    private final DetectionListener detectionListener;
//
//    private AudioClassifier classifier;
//    private TensorAudio tensorAudio;
//    private AudioRecord audioRecord;
//    private boolean isRecording = false;
//    private int bufferSize;
//
//    private final ExecutorService executor = Executors.newSingleThreadExecutor();
//
//    public SoundCapture(Context context, DetectionListener listener) {
//        this.context = context;
//        this.detectionListener = listener;
//
//        try {
//            classifier = AudioClassifier.createFromFile(context, "yamnet.tflite");
//            tensorAudio = classifier.createInputTensorAudio();
//        } catch (Exception e) {
//            e.printStackTrace();
//            detectionListener.onError("Failed to load model: " + e.getMessage());
//        }
//    }
//
//    public boolean hasAudioPermission() {
//        return ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
//    }
//
//    public void startRecording() {
//        if (!hasAudioPermission()) {
//            detectionListener.onError("Audio permission not granted.");
//            return;
//        }
//
//        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);
//
//        try {
//            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
//                    sampleRate,
//                    channelConfig,
//                    audioFormat,
//                    bufferSize);
//        } catch (SecurityException e) {
//            e.printStackTrace();
//            resultText.setText("SecurityException: Permission denied.");
//            return;
//        }
//
//        audioRecord.startRecording();
//        isRecording = true;
//
//        executor.submit(this::recordAndClassify);
//    }
//
//    private void recordAndClassify() {
//        short[] audioBuffer = new short[bufferSize / 2];
//
//        while (isRecording && !Thread.currentThread().isInterrupted()) {
//            int readResult = audioRecord.read(audioBuffer, 0, audioBuffer.length);
//            if (readResult > 0) {
//                tensorAudio.load(audioBuffer);
//                List<Classifications> results = classifier.classify(tensorAudio);
//
//                if (!results.isEmpty() && !results.get(0).getCategories().isEmpty()) {
//                    String topLabel = results.get(0).getCategories().get(0).getLabel();
//                    float confidence = results.get(0).getCategories().get(0).getScore();
//
//                    String timeString = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
//                            .format(new java.util.Date());
//
//                    detectionListener.onSoundDetected(topLabel, confidence, timeString);
//                }
//            }
//        }
//    }
//
//    public void stopRecording() {
//        isRecording = false;
//        if (audioRecord != null) {
//            try {
//                audioRecord.stop();
//                audioRecord.release();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            audioRecord = null;
//        }
//        executor.shutdownNow();
//    }
//}





package com.example.auralens;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.auralens.databinding.ActivitySoundCaptureBinding;

import org.tensorflow.lite.task.audio.classifier.AudioClassifier;
import org.tensorflow.lite.support.audio.TensorAudio;
import org.tensorflow.lite.task.audio.classifier.Classifications;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SoundCaptureActivity extends AppCompatActivity {

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean permissionGranted = false;

    private ActivitySoundCaptureBinding binding;
    private Button startStopButton;
    private TextView resultText;
    private RecyclerView recyclerView;
    private DetectionAdapter detectionAdapter;
    private final List<DetectionEntry> detectionEntries = new ArrayList<>();

    private AudioClassifier classifier;
    private TensorAudio tensorAudio;
    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRecording = false;

    private final int sampleRate = 16000;
    private final int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferSize;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySoundCaptureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Audio Classification");
        }

        initializeViews();
        setupRecyclerView();
        requestAudioPermission();
        initializeAudioClassifier();
    }

    private void initializeViews() {
        startStopButton = binding.buttonStartStop;
        resultText = binding.textResult;
        recyclerView = binding.recyclerDetectionHistory;
        mainHandler = new Handler(Looper.getMainLooper());

        startStopButton.setOnClickListener(v -> toggleRecording());
    }

    private void toggleRecording() {
        if (!isRecording) {
            startRecording();
            startStopButton.setText("Stop Listening");
        } else {
            stopRecording();
            startStopButton.setText("Start Listening");
        }
    }

    private void setupRecyclerView() {
        detectionAdapter = new DetectionAdapter(detectionEntries);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(detectionAdapter);
    }

    private void initializeAudioClassifier() {
        try {
            classifier = AudioClassifier.createFromFile(this, "yamnet.tflite");
            tensorAudio = classifier.createInputTensorAudio();
        } catch (Exception e) {
            e.printStackTrace();
            resultText.setText("Model loading failed");
            startStopButton.setEnabled(false);
            Toast.makeText(this, "Error loading TensorFlow Lite model", Toast.LENGTH_LONG).show();
        }
    }

    private void requestAudioPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
        } else {
            permissionGranted = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionGranted = grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!permissionGranted) {
                resultText.setText("Permission denied to record audio");
                Toast.makeText(this, "Microphone permission required", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startRecording() {
        if (!permissionGranted) {
            resultText.setText("Audio permission not granted");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            resultText.setText("Audio permission missing");
            return;
        }

        bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat);

        try {
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize);
        } catch (SecurityException e) {
            e.printStackTrace();
            resultText.setText("SecurityException: Permission denied.");
            return;
        }

        audioRecord.startRecording();
        isRecording = true;

        recordingThread = new Thread(() -> {
            short[] audioBuffer = new short[bufferSize / 2];
            while (isRecording && !Thread.currentThread().isInterrupted()) {
                int readResult = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                if (readResult > 0) {
                    runInference(audioBuffer);
                }
            }
        });
        recordingThread.start();
    }

    private void stopRecording() {
        if (audioRecord != null) {
            isRecording = false;
            try {
                recordingThread.interrupt();
                recordingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    private void runInference(short[] audioBuffer) {
        try {
            tensorAudio.load(audioBuffer);
            List<Classifications> results = classifier.classify(tensorAudio);

            if (!results.isEmpty() && !results.get(0).getCategories().isEmpty()) {
                String topLabel = results.get(0).getCategories().get(0).getLabel();
                float confidence = results.get(0).getCategories().get(0).getScore();
                String timeString = new SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        .format(new Date());

                DetectionEntry newEntry = new DetectionEntry(topLabel, timeString);
                String resultString = String.format("Detected: %s (%.2f%%)\nAt: %s",
                        topLabel, confidence * 100, timeString);

                mainHandler.post(() -> {
                    resultText.setText(resultString);
                    detectionEntries.add(0, newEntry);
                    detectionAdapter.notifyItemInserted(0);
                    recyclerView.smoothScrollToPosition(0);
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            mainHandler.post(() -> Toast.makeText(this,
                    "Error during audio processing",
                    Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isRecording) {
            stopRecording();
        }
    }
}