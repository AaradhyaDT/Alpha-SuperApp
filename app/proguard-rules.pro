# Add project-specific ProGuard rules here.

# MediaPipe - Keeping only the necessary entry points and results
-keep class com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult { *; }
-keep class com.google.mediapipe.tasks.components.containers.NormalizedLandmark { *; }
-keep class com.google.mediapipe.tasks.vision.core.RunningMode { *; }
-dontwarn com.google.mediapipe.**

# OKHttp
-keepattributes Signature, AnnotationDefault, EnclosingMethod, InnerClasses
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# Gson - Keep the data classes that are serialized/deserialized
-keepclassmembers class com.alpha.** {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Keep the AI response models if any (represented by anonymous JsonObjects/Arrays in GeminiClient)
-keepattributes Signature, *Annotation*
-dontwarn sun.misc.**

# ViewModel and State Classes - Ensure they aren't stripped or renamed in a way that breaks Compose
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keep class com.alpha.features.**.UiState { *; }

# AutoValue / Shaded libs
-dontwarn com.google.auto.value.**
-dontwarn autovalue.shaded.**
-dontwarn javax.lang.model.**
-dontwarn javax.annotation.processing.**
