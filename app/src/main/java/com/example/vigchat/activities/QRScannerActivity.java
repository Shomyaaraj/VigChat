package com.example.vigchat.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.vigchat.utils.QRCodeHelper;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanIntentResult;
import com.journeyapps.barcodescanner.ScanOptions;

public class QRScannerActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    launchScanner();
                } else {
                    Toast.makeText(this, "Camera permission is only needed when you scan a room QR.", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    private final ActivityResultLauncher<ScanOptions> scanLauncher =
            registerForActivityResult(new ScanContract(), this::handleScanResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchScanner();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan a VigChat room QR code");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        scanLauncher.launch(options);
    }

    private void handleScanResult(ScanIntentResult result) {
        if (result == null || result.getContents() == null) {
            finish();
            return;
        }

        String roomId = QRCodeHelper.extractRoomId(result.getContents());
        if (roomId == null) {
            Toast.makeText(this, "Invalid VigChat QR code", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("ROOM_ID", roomId);
        startActivity(intent);
        finish();
    }
}
