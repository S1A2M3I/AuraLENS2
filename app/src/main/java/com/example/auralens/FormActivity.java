package com.example.auralens;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.util.SparseArray;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class FormActivity extends AppCompatActivity {

    private static final int CAMERA_REQUEST = 100;
    private static final int VOICE_REQUEST = 200;
    private static final int PERMISSION_REQUEST_CODE = 300;

    private EditText currentActiveField;
    private EditText etTemperature, etPressure, etFlow,
            etInTime, etOutTime, etTankerDetails, etWeight, resultArea;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);

        checkAndRequestPermissions();
        initializeViews();
        setupInputFields();
        setupSubmitButton();
    }

    private void initializeViews() {
        etTemperature = findViewById(R.id.etTemperature);
        etPressure = findViewById(R.id.etPressure);
        etFlow = findViewById(R.id.etFlow);
        etInTime = findViewById(R.id.etInTime);
        etOutTime = findViewById(R.id.etOutTime);
        etTankerDetails = findViewById(R.id.etTankerDetails);
        etWeight = findViewById(R.id.etWeight);
        resultArea = findViewById(R.id.result_area);
    }

    private void setupInputFields() {
        setupInputField(R.id.etTemperature, R.id.btnTempCamera, R.id.btnTempVoice);
        setupInputField(R.id.etPressure, R.id.btnPressureCamera, R.id.btnPressureVoice);
        setupInputField(R.id.etFlow, R.id.btnFlowCamera, R.id.btnFlowVoice);
        setupInputField(R.id.etInTime, R.id.btnInTimeCamera, R.id.btnInTimeVoice);
        setupInputField(R.id.etOutTime, R.id.btnOutTimeCamera, R.id.btnOutTimeVoice);
        setupInputField(R.id.etTankerDetails, R.id.btnTankerCamera, R.id.btnTankerVoice);
        setupInputField(R.id.etWeight, R.id.btnWeightCamera, R.id.btnWeightVoice);
    }

    private void setupInputField(int editTextId, int cameraBtnId, int voiceBtnId) {
        EditText et = findViewById(editTextId);
        ImageButton btnCamera = findViewById(cameraBtnId);
        ImageButton btnVoice = findViewById(voiceBtnId);

        btnCamera.setOnClickListener(v -> handleCameraClick(et));
        btnVoice.setOnClickListener(v -> handleVoiceClick(et));
    }

    private void handleCameraClick(EditText targetField) {
        currentActiveField = targetField;
        if (checkCameraPermission()) {
            openCamera();
        }
    }

    private void handleVoiceClick(EditText targetField) {
        currentActiveField = targetField;
        if (checkAudioPermission()) {
            startVoiceInput();
        }
    }

    private void setupSubmitButton() {
        Button btnSubmit = findViewById(R.id.btnSubmit);
        btnSubmit.setOnClickListener(v -> showFormData());
    }

    private void checkAndRequestPermissions() {
        if (!checkCameraPermission() || !checkAudioPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    PERMISSION_REQUEST_CODE);
        }
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkAudioPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions are required for full functionality", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        }
    }

    private void startVoiceInput() {
        Intent voiceIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        voiceIntent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...");
        if (voiceIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(voiceIntent, VOICE_REQUEST);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST && data != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                extractTextFromImage(photo);
            } else if (requestCode == VOICE_REQUEST && data != null) {
                ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (matches != null && !matches.isEmpty()) {
                    currentActiveField.setText(matches.get(0));
                }
            }
        }
    }

    private void extractTextFromImage(Bitmap bitmap) {
        try {
            TextRecognizer recognizer = new TextRecognizer.Builder(this).build();
            if (!recognizer.isOperational()) {
                Toast.makeText(this, "Text recognition failed", Toast.LENGTH_SHORT).show();
                return;
            }

            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<TextBlock> textBlocks = recognizer.detect(frame);

            StringBuilder extractedText = new StringBuilder();
            for (int i = 0; i < textBlocks.size(); i++) {
                TextBlock textBlock = textBlocks.valueAt(i);
                extractedText.append(textBlock.getValue()).append(" ");
            }

            String result = extractedText.toString().trim();
            if (!result.isEmpty()) {
                currentActiveField.setText(result);
            } else {
                Toast.makeText(this, "No text detected", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFormData() {
        Intent reportIntent = new Intent(FormActivity.this, ReportActivity.class);

        reportIntent.putExtra("PRESSURE", etPressure.getText().toString().trim());
        reportIntent.putExtra("FLOW", etFlow.getText().toString().trim());
        reportIntent.putExtra("IN_TIME", etInTime.getText().toString().trim());
        reportIntent.putExtra("OUT_TIME", etOutTime.getText().toString().trim());
        reportIntent.putExtra("TANKER_DETAILS", etTankerDetails.getText().toString().trim());
        reportIntent.putExtra("WEIGHT", etWeight.getText().toString().trim());

        startActivity(reportIntent);
    }
}