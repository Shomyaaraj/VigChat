package com.example.vigchat.utils;

import androidx.annotation.NonNull;

import com.example.vigchat.models.ChatRoom;
import com.example.vigchat.models.Message;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FirebaseHelper {

    public interface AuthSuccessCallback {
        void onSuccess(@NonNull String userId);
    }

    public interface FailureCallback {
        void onFailure(@NonNull Exception exception);
    }

    public interface RoomCallback {
        void onRoomLoaded(@NonNull ChatRoom room);
    }

    public interface CompletionCallback {
        void onComplete();
    }

    private FirebaseHelper() {
    }

    public static void ensureSignedIn(
            @NonNull AuthSuccessCallback onSuccess,
            @NonNull FailureCallback onFailure
    ) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null && auth.getCurrentUser().getUid() != null) {
            onSuccess.onSuccess(auth.getCurrentUser().getUid());
            return;
        }

        auth.signInAnonymously()
                .addOnSuccessListener(authResult -> {
                    if (authResult.getUser() != null && authResult.getUser().getUid() != null) {
                        onSuccess.onSuccess(authResult.getUser().getUid());
                    } else {
                        onFailure.onFailure(
                                new IllegalStateException("Anonymous sign-in did not return a user.")
                        );
                    }
                })
                .addOnFailureListener(onFailure::onFailure);
    }

    @NonNull
    public static String toUserMessage(@NonNull Exception exception, @NonNull String action) {
        if (exception instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) exception).getErrorCode();
            String normalizedCode = errorCode == null ? "" : errorCode.toUpperCase(Locale.US);

            if (normalizedCode.contains("CONFIGURATION_NOT_FOUND")) {
                return "Firebase Authentication is not configured for this app. In Firebase Console for project "
                        + "\"claprojectmad\", open Authentication, click Get started, and make sure the app "
                        + "configuration is valid for package com.example.vigchat.";
            }

            if (normalizedCode.contains("OPERATION_NOT_ALLOWED")) {
                return "Anonymous login is disabled in Firebase Console. Enable Authentication > Sign-in method > Anonymous.";
            }

            if (normalizedCode.contains("NETWORK_REQUEST_FAILED")) {
                return "Network error while " + action + ". Check internet access and try again.";
            }

            if (normalizedCode.contains("API_NOT_AVAILABLE")) {
                return "Google Play services or Firebase services are not available on this device.";
            }
        }

        if (exception instanceof FirebaseNetworkException) {
            return "Network error while " + action + ". Check internet access and try again.";
        }

        if (exception instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) exception;
            if (firestoreException.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return "Firestore denied access. Publish the firestore.rules file to your Firebase project before testing CRUD.";
            }
        }

        if (exception instanceof StorageException) {
            int errorCode = ((StorageException) exception).getErrorCode();
            if (errorCode == StorageException.ERROR_NOT_AUTHENTICATED) {
                return "Storage upload failed because the user is not authenticated.";
            }
            if (errorCode == StorageException.ERROR_NOT_AUTHORIZED) {
                return "Storage denied access. Publish the storage.rules file to your Firebase project before testing file and voice uploads.";
            }
        }

        String rawMessage = exception.getMessage();
        if (rawMessage != null && rawMessage.toUpperCase(Locale.US).contains("CONFIGURATION_NOT_FOUND")) {
            return "Firebase Authentication is not configured for this app. In Firebase Console for project "
                    + "\"claprojectmad\", open Authentication, click Get started, and verify package com.example.vigchat.";
        }

        if (rawMessage != null && !rawMessage.trim().isEmpty()) {
            return "Error while " + action + ": " + rawMessage;
        }

        return "Error while " + action + ".";
    }

    public static CollectionReference roomsCollection() {
        return FirebaseFirestore.getInstance().collection("chatrooms");
    }

    public static void createRoom(
            @NonNull String roomId,
            @NonNull String adminId,
            @NonNull String joinLink,
            long createdAt,
            @NonNull RoomCallback onSuccess,
            @NonNull FailureCallback onFailure
    ) {
        ChatRoom room = new ChatRoom(roomId, adminId, joinLink, createdAt);
        roomsCollection()
                .document(roomId)
                .set(room)
                .addOnSuccessListener(unused -> onSuccess.onRoomLoaded(room))
                .addOnFailureListener(onFailure::onFailure);
    }

    public static void loadRoom(
            @NonNull String roomId,
            @NonNull RoomCallback onSuccess,
            @NonNull FailureCallback onFailure
    ) {
        roomsCollection()
                .document(roomId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    ChatRoom room = documentSnapshot.toObject(ChatRoom.class);
                    if (documentSnapshot.exists() && room != null) {
                        onSuccess.onRoomLoaded(room);
                    } else {
                        onFailure.onFailure(new IllegalStateException("Room not found or deleted."));
                    }
                })
                .addOnFailureListener(onFailure::onFailure);
    }

    public static void deleteRoomAndContents(
            @NonNull String roomId,
            @NonNull CompletionCallback onSuccess,
            @NonNull FailureCallback onFailure
    ) {
        DocumentReference roomRef = roomsCollection().document(roomId);
        roomRef.collection("messages")
                .get()
                .addOnSuccessListener(snapshot -> deleteMessagesAndRoom(roomRef, snapshot, onSuccess, onFailure))
                .addOnFailureListener(onFailure::onFailure);
    }

    private static void deleteMessagesAndRoom(
            @NonNull DocumentReference roomRef,
            @NonNull QuerySnapshot snapshot,
            @NonNull CompletionCallback onSuccess,
            @NonNull FailureCallback onFailure
    ) {
        List<Task<?>> deleteTasks = new ArrayList<>();

        for (DocumentSnapshot documentSnapshot : snapshot.getDocuments()) {
            Message message = documentSnapshot.toObject(Message.class);
            if (message != null && message.getStoragePath() != null && !message.getStoragePath().isEmpty()) {
                deleteTasks.add(FirebaseStorage.getInstance().getReference(message.getStoragePath()).delete());
            }
            deleteTasks.add(documentSnapshot.getReference().delete());
        }

        Task<List<Task<?>>> allDeletes = deleteTasks.isEmpty()
                ? Tasks.forResult(new ArrayList<Task<?>>())
                : Tasks.whenAllComplete(deleteTasks);

        allDeletes
                .addOnSuccessListener(results -> roomRef.delete()
                        .addOnSuccessListener(unused -> onSuccess.onComplete())
                        .addOnFailureListener(onFailure::onFailure))
                .addOnFailureListener(onFailure::onFailure);
    }
}
