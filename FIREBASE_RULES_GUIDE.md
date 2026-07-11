# 🛡️ UniMove Firebase Security Rules Guide

Your project currently lacks server-side security rules. This means anyone with your Project ID can read or delete your database. Follow these steps to secure it.

## 1. Firestore Security Rules

1.  Go to the [Firebase Console](https://console.firebase.google.com/).
2.  Select your project: **UniMove**.
3.  In the left menu, click **Firestore Database**.
4.  Click the **Rules** tab.
5.  Delete the existing rules and **paste the following code**:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper function: check if user is logged in
    function isSignedIn() {
      return request.auth != null;
    }

    // Helper function: check if user owns the document
    function isOwner(userId) {
      return isSignedIn() && request.auth.uid == userId;
    }

    // 1. Users collection: Only the user can read/write their own profile
    match /users/{userId} {
      allow read, write: if isOwner(userId);
      
      // Sub-collections (sessions, logs, etc) inherit this or can be specific
      match /{allSubcollections=**} {
        allow read, write: if isOwner(userId);
      }
    }

    // 2. Challenges: Everyone logged in can read, only admin (backend) can write
    match /challenges/{challengeId} {
      allow read: if isSignedIn();
      allow write: if false; // Disallow client-side writes for security
      
      match /participants/{userId} {
        allow read: if isSignedIn();
        allow write: if isOwner(userId);
      }
    }

    // 3. Community Feed: Everyone logged in can read, authors can write
    match /community_feed/{postId} {
      allow read: if isSignedIn();
      allow create: if isSignedIn() && request.resource.data.authorId == request.auth.uid;
      allow update, delete: if isSignedIn() && resource.data.authorId == request.auth.uid;
    }

    // 4. Weekly Leaderboard: Read-only for users
    match /weekly_leaderboard/{docId} {
      allow read: if isSignedIn();
      allow write: if false;
    }
  }
}
```

6.  Click **Publish**.

---

## 2. API Key Restrictions (Google Cloud Console)

To prevent theft of your Google Maps quota:

1.  Go to the [Google Cloud Console Credentials page](https://console.cloud.google.com/apis/credentials).
2.  Find your **Maps API Key**.
3.  Under **Application restrictions**, select **Android apps**.
4.  Add your package name: `com.example.unimove`.
5.  Add your **SHA-1 fingerprint** (you can get this by running `./gradlew signingReport`).
6.  Under **API restrictions**, select **Restrict key** and check **Maps SDK for Android**.
7.  Save.

---

## 3. Storage Security Rules (If using Firebase Storage)

If you use Firebase Storage for user photos, apply these rules in the **Storage > Rules** tab:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /users/{userId}/{allPaths=**} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```
