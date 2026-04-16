package com.example.vigchat.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.vigchat.R;
import com.example.vigchat.adapters.MessageAdapter;
import com.example.vigchat.data.LocalChatRepository;
import com.example.vigchat.models.ChatRoom;
import com.example.vigchat.models.Message;
import com.example.vigchat.utils.QRCodeHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private String roomId;
    private String currentUserId;
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private final List<Message> messageList = new ArrayList<>();
    private EditText messageInput;
    private Button sendBtn;
    private Button fileBtn;
    private Button voiceBtn;
    private Button deleteBtn;
    private TextView roomInfoText;

    private MediaRecorder recorder;
    private String filePath;
    private boolean isAdmin = false;
    private LocalChatRepository.Subscription messageSubscription;

    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    attachFile(uri);
                }
            });

    private final ActivityResultLauncher<String> audioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    startRecording();
                } else {
                    Toast.makeText(this, "Microphone permission is only needed when you record voice.", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        roomId = QRCodeHelper.extractRoomId(getIntent().getStringExtra("ROOM_ID"));
        if (roomId == null && getIntent().getDataString() != null) {
            roomId = QRCodeHelper.extractRoomId(getIntent().getDataString());
        }

        if (roomId == null) {
            Toast.makeText(this, "Invalid room", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        currentUserId = LocalChatRepository.getOrCreateCurrentUserId(this);

        initUI();
        setupRecyclerView();
        setComposerEnabled(false);
        loadRoom();
    }

    private void initUI() {
        roomInfoText = findViewById(R.id.roomInfoText);
        recyclerView = findViewById(R.id.recyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendBtn = findViewById(R.id.sendBtn);
        fileBtn = findViewById(R.id.fileBtn);
        voiceBtn = findViewById(R.id.voiceBtn);
        deleteBtn = findViewById(R.id.deleteBtn);

        deleteBtn.setVisibility(View.GONE);
        roomInfoText.setText("Room ID: " + roomId);

        sendBtn.setOnClickListener(v -> sendMessage());
        fileBtn.setOnClickListener(v -> pickFile());
        voiceBtn.setOnClickListener(v -> toggleRecording());
        deleteBtn.setOnClickListener(v -> {
            if (isAdmin) {
                confirmDeleteRoom();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter(messageList, currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void loadRoom() {
        ChatRoom room = LocalChatRepository.getRoom(this, roomId);
        if (room == null) {
            Toast.makeText(
                    this,
                    "Room not found in frontend mode. Create the room first on this app, then join using its link or QR.",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        isAdmin = currentUserId.equals(room.getAdminId());
        deleteBtn.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        roomInfoText.setText(isAdmin
                ? "Room ID: " + roomId + "  |  Frontend admin"
                : "Room ID: " + roomId + "  |  Frontend participant");
        setComposerEnabled(true);
        observeMessages();
    }

    private void observeMessages() {
        if (messageSubscription != null) {
            messageSubscription.unsubscribe();
        }

        messageSubscription = LocalChatRepository.observeMessages(this, roomId, messages -> {
            messageList.clear();
            messageList.addAll(messages);
            adapter.notifyDataSetChanged();
            if (!messageList.isEmpty()) {
                recyclerView.scrollToPosition(messageList.size() - 1);
            }
        });
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }

        Message msg = new Message(
                currentUserId,
                text,
                null,
                null,
                null,
                null,
                "text",
                System.currentTimeMillis()
        );

        LocalChatRepository.addMessage(this, roomId, msg);
        messageInput.setText("");
    }

    private void confirmDeleteRoom() {
        new AlertDialog.Builder(this)
                .setTitle("Delete this room?")
                .setMessage("This removes the room and all locally stored messages from the frontend app.")
                .setPositiveButton("Delete", (dialog, which) -> deleteRoom())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRoom() {
        deleteBtn.setEnabled(false);
        LocalChatRepository.deleteRoom(this, roomId);
        Toast.makeText(this, "Room deleted", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void toggleRecording() {
        if (recorder == null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                startRecording();
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
        } else {
            stopRecording();
        }
    }

    private void startRecording() {
        File cacheDirectory = getExternalCacheDir() != null ? getExternalCacheDir() : getCacheDir();
        filePath = new File(cacheDirectory, "voice_" + System.currentTimeMillis() + ".3gp").getAbsolutePath();

        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(filePath);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
            recorder.start();
            voiceBtn.setText(getString(R.string.stop_recording));
        } catch (Exception e) {
            cleanupRecorder(false);
            Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (recorder == null) {
            return;
        }

        try {
            recorder.stop();
        } catch (Exception e) {
            cleanupRecorder(false);
            Toast.makeText(this, "Unable to finish the recording.", Toast.LENGTH_SHORT).show();
            return;
        }

        cleanupRecorder(true);
        addVoiceMessage();
    }

    private void addVoiceMessage() {
        if (TextUtils.isEmpty(filePath)) {
            return;
        }

        File voiceFile = new File(filePath);
        Uri voiceUri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".fileprovider",
                voiceFile
        );

        Message msg = new Message(
                currentUserId,
                "",
                voiceUri.toString(),
                voiceFile.getName(),
                "audio/3gpp",
                filePath,
                "voice",
                System.currentTimeMillis()
        );

        LocalChatRepository.addMessage(this, roomId, msg);
        filePath = null;
    }

    private void pickFile() {
        filePickerLauncher.launch(new String[]{"*/*"});
    }

    private void attachFile(Uri fileUri) {
        try {
            getContentResolver().takePersistableUriPermission(
                    fileUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
            );
        } catch (SecurityException ignored) {
        }

        Message msg = new Message(
                currentUserId,
                "",
                fileUri.toString(),
                queryDisplayName(fileUri),
                getContentResolver().getType(fileUri),
                fileUri.toString(),
                "file",
                System.currentTimeMillis()
        );

        LocalChatRepository.addMessage(this, roomId, msg);
    }

    @NonNull
    private String queryDisplayName(@NonNull Uri fileUri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(
                    fileUri,
                    new String[]{OpenableColumns.DISPLAY_NAME},
                    null,
                    null,
                    null
            );
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String name = cursor.getString(index);
                    if (name != null && !name.trim().isEmpty()) {
                        return name.trim();
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return "attachment_" + System.currentTimeMillis();
    }

    private void setComposerEnabled(boolean enabled) {
        messageInput.setEnabled(enabled);
        sendBtn.setEnabled(enabled);
        fileBtn.setEnabled(enabled);
        voiceBtn.setEnabled(enabled);
    }

    private void cleanupRecorder(boolean keepFile) {
        if (recorder != null) {
            recorder.release();
            recorder = null;
        }
        voiceBtn.setText(getString(R.string.voice));
        if (!keepFile) {
            deleteLocalRecording();
        }
    }

    private void deleteLocalRecording() {
        if (!TextUtils.isEmpty(filePath)) {
            File localFile = new File(filePath);
            if (localFile.exists()) {
                localFile.delete();
            }
        }
        filePath = null;
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (recorder != null) {
            cleanupRecorder(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageSubscription != null) {
            messageSubscription.unsubscribe();
            messageSubscription = null;
        }
    }
}
