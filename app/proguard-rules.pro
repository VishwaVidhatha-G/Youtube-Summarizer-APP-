# Proguard/R8 rules to optimize and shrink the APK securely.

# Retrofit keeps
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature, InnerClasses, EnclosingMethod

# Gson / Json serialization keeps
-keepattributes *Annotation*,Signature,EnclosingMethod,InnerClasses
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep domain & data models from obfuscation (required for JSON reflection)
-keep class com.summarizer.app.data.model.** { *; }
-keep class com.summarizer.app.domain.model.** { *; }
-keep class com.summarizer.app.data.database.** { *; }
