# Keep attributes for debugging, annotations, and reflection used by libraries.
-keepattributes Signature, *Annotation*, EnclosingMethod

# --- Firebase/Firestore Data Model Keep Rules ---
# This is the most critical section. It prevents ProGuard from removing or renaming
# classes, their empty constructors, and their public members. This is essential
# for Firestore's .toObject() calls to work correctly in release builds.

-keep public class com.meow.utaract.utils.Event { public <init>(); public *; }
-keep public class com.meow.utaract.utils.GuestProfile { public <init>(); public *; }
-keep public class com.meow.utaract.utils.Applicant { public <init>(); public *; }
-keep public class com.meow.utaract.utils.JoinedEvent { public <init>(); public *; }
-keep public class com.meow.utaract.utils.Notification { public <init>(); public *; }
-keep public class com.meow.utaract.utils.News { public <init>(); public *; }
-keep public class com.meow.utaract.utils.ManagedEventItem { public <init>(); public *; }
-keep public class com.meow.utaract.activities.GuestFormActivity { public <init>(); public *; }

# --- General Firebase & Google Play Services Rules ---
-keep public class com.google.firebase.** { public *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-dontwarn com.google.firebase.**

# --- Library Rules (Glide, ZXing, Gson, OkHttp) ---
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class com.meow.utaract.activities.services.MyAppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$ImageType { *; }
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.zxing.android.embedded.** { *; }
-keep class com.google.gson.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# --- General Android Best Practices ---
-keep public class * extends android.view.View { public <init>(...); public void set*(...); }
-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}