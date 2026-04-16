package com.example.vigchat.utils;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.Locale;

public class FileUploadHelper {

    public interface UploadCallback {
        void onSuccess(@NonNull UploadedFile uploadedFile);
        void onFailure(@NonNull Exception exception);
    }

    public static class UploadedFile {
        private final String fileName;
        private final String mimeType;
        private final String storagePath;
        private final String downloadUrl;

        public UploadedFile(
                @NonNull String fileName,
                @NonNull String mimeType,
                @NonNull String storagePath,
                @NonNull String downloadUrl
        ) {
            this.fileName = fileName;
            this.mimeType = mimeType;
            this.storagePath = storagePath;
            this.downloadUrl = downloadUrl;
        }

        public String getFileName() {
            return fileName;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getStoragePath() {
            return storagePath;
        }

        public String getDownloadUrl() {
            return downloadUrl;
        }
    }

    private FileUploadHelper() {
    }

    public static void uploadFile(
            @NonNull Context context,
            @NonNull Uri fileUri,
            @NonNull String roomId,
            @NonNull UploadCallback callback
    ) {
        String displayName = getDisplayName(context, fileUri);
        String mimeType = getMimeType(context, fileUri, displayName);
        String storagePath = "chat_files/" + roomId + "/" + System.currentTimeMillis()
                + "_" + sanitizeFileName(displayName);

        uploadToStorage(fileUri, storagePath, displayName, mimeType, callback);
    }

    public static void uploadVoiceRecording(
            @NonNull String localFilePath,
            @NonNull String roomId,
            @NonNull UploadCallback callback
    ) {
        String displayName = "voice_" + System.currentTimeMillis() + ".3gp";
        String mimeType = "audio/3gpp";
        String storagePath = "voice/" + roomId + "/" + displayName;

        uploadToStorage(Uri.fromFile(new File(localFilePath)), storagePath, displayName, mimeType, callback);
    }

    private static void uploadToStorage(
            @NonNull Uri uri,
            @NonNull String storagePath,
            @NonNull String fileName,
            @NonNull String mimeType,
            @NonNull UploadCallback callback
    ) {
        StorageReference reference = FirebaseStorage.getInstance().getReference(storagePath);
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType(mimeType)
                .build();

        reference.putFile(uri, metadata)
                .addOnSuccessListener(taskSnapshot -> reference.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> callback.onSuccess(
                                new UploadedFile(fileName, mimeType, storagePath, downloadUri.toString())
                        ))
                        .addOnFailureListener(callback::onFailure))
                .addOnFailureListener(callback::onFailure);
    }

    @NonNull
    private static String getDisplayName(@NonNull Context context, @NonNull Uri uri) {
        String displayName = queryDisplayName(context, uri);
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName.trim();
        }
        return "attachment_" + System.currentTimeMillis();
    }

    @Nullable
    private static String queryDisplayName(@NonNull Context context, @NonNull Uri uri) {
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    uri,
                    new String[]{OpenableColumns.DISPLAY_NAME},
                    null,
                    null,
                    null
            );
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    return cursor.getString(index);
                }
            }
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    @NonNull
    private static String getMimeType(
            @NonNull Context context,
            @NonNull Uri uri,
            @NonNull String fileName
    ) {
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType != null && !mimeType.isEmpty()) {
            return mimeType;
        }

        String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
        if (extension != null && !extension.isEmpty()) {
            String guessedMime = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(extension.toLowerCase(Locale.US));
            if (guessedMime != null && !guessedMime.isEmpty()) {
                return guessedMime;
            }
        }

        return "application/octet-stream";
    }

    @NonNull
    private static String sanitizeFileName(@NonNull String fileName) {
        return fileName.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
