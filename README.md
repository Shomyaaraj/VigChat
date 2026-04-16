# VigChat

VigChat is an Android room-based chat app built around Firebase Authentication, Firestore, and Firebase Storage. It lets users join a room by QR code or link without using phone numbers. The room creator becomes the admin and can delete the room and its stored content.

## Folder structure

```text
VigChat/
|- app/                         Android client
|  |- src/main/java/...         Activities, adapters, models, helpers
|  |- src/main/res/...          Layouts, strings, drawables
|  \- google-services.json      Firebase Android configuration
|- backend/                     Optional Node + PostgreSQL scaffold
|  |- src/config/               Database configuration
|  |- src/controllers/          Route handlers
|  |- src/routes/               Express routes
|  \- .env.example              Backend environment template
|- gradle/                      Gradle version catalog and wrapper files
|- build.gradle.kts             Root Gradle config
\- settings.gradle.kts          Android modules
```

## Requirements before first use

1. Install Android Studio with Android SDK Platform 36 and a working emulator or USB device.
2. Make sure Java is available for Gradle. This project is configured for Java 11 source compatibility.
3. In Firebase Console:
   - Enable Anonymous Authentication.
   - Create a Firestore database.
   - Enable Firebase Storage.
   - Apply the repo rules from `firestore.rules` and `storage.rules`.
   - Keep `app/google-services.json` aligned with your Firebase project.
4. Optional for backend work:
   - Install Node.js 18+.
   - Install PostgreSQL 14+.
   - Copy `backend/.env.example` to `backend/.env` and update `DATABASE_URL`.

## Current Android flow

1. Open the app and let anonymous sign-in complete.
2. Create a room from the home screen.
3. Share the generated room QR code or link.
4. Join the room by scanning the QR or pasting the room link or ID.
5. Send text, voice, and files such as PDF, DOCX, PPT, audio, or video.
6. Use the admin delete action to remove the room, messages, and uploaded attachments.

## Step-by-step development order

1. Firebase setup:
   Enable Firebase Auth, Firestore, and Storage before testing any chat flow.
2. Room creation:
   Create a room document in Firestore with `roomId`, `adminId`, `joinLink`, and `createdAt`.
3. Room access:
   Parse QR codes and pasted links into `roomId`, then open the room only if it exists.
4. Messaging:
   Store text messages inside `chatrooms/{roomId}/messages`.
5. Attachments:
   Upload files and voice recordings to Firebase Storage, then save the download URL and storage path in each message.
6. Admin controls:
   Let only the room creator delete the room and associated stored content.
7. Optional PostgreSQL backend:
   Use the backend scaffold for future audit logs, admin dashboards, analytics, or exports. The live chat flow in this project currently runs on Firebase.

## Backend quick start

```bash
cd backend
npm install
copy .env.example .env
npm start
```

Check `GET /api/health` after setting `DATABASE_URL`.
