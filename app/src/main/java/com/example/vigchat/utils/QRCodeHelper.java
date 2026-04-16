package com.example.vigchat.utils;

import android.graphics.Bitmap;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;

public class QRCodeHelper {

    private static final String HTTPS_LINK_PREFIX = "https://vigchat.app/chat/";
    private static final String APP_LINK_PREFIX = "vigchat://chat/";

    private QRCodeHelper() {
    }

    public static Bitmap generateQR(String text) {
        try {
            BarcodeEncoder encoder = new BarcodeEncoder();
            return encoder.encodeBitmap(text, BarcodeFormat.QR_CODE, 400, 400);
        } catch (Exception e) {
            return null;
        }
    }

    public static String generateRoomLink(String roomId) {
        return HTTPS_LINK_PREFIX + roomId;
    }

    @Nullable
    public static String extractRoomId(@Nullable String rawInput) {
        if (rawInput == null) {
            return null;
        }

        String candidate = rawInput.trim();
        if (candidate.isEmpty()) {
            return null;
        }

        if (candidate.startsWith(HTTPS_LINK_PREFIX) || candidate.startsWith(APP_LINK_PREFIX)) {
            return sanitizeRoomId(candidate.substring(candidate.lastIndexOf('/') + 1));
        }

        try {
            Uri uri = Uri.parse(candidate);
            if (uri != null) {
                String roomIdFromQuery = sanitizeRoomId(uri.getQueryParameter("roomId"));
                if (roomIdFromQuery != null) {
                    return roomIdFromQuery;
                }

                if (uri.getPathSegments() != null && !uri.getPathSegments().isEmpty()) {
                    if ("chat".equalsIgnoreCase(uri.getHost()) && !uri.getPathSegments().isEmpty()) {
                        return sanitizeRoomId(uri.getPathSegments().get(0));
                    }

                    if (uri.getPathSegments().size() >= 2
                            && "chat".equalsIgnoreCase(uri.getPathSegments().get(0))) {
                        return sanitizeRoomId(uri.getPathSegments().get(1));
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return sanitizeRoomId(candidate);
    }

    @Nullable
    private static String sanitizeRoomId(@Nullable String roomId) {
        if (TextUtils.isEmpty(roomId)) {
            return null;
        }

        String trimmed = roomId.trim();
        return trimmed.matches("[A-Za-z0-9\\-]{8,}") ? trimmed : null;
    }
}
