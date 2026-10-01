# kotlinx.serialization: keep generated serializers for our DTOs and navigation routes.
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations, AnnotationDefault
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class com.aura.dashboard.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.aura.dashboard.**$$serializer { *; }

# Retrofit service interfaces are accessed reflectively.
-keep,allowobfuscation,allowshrinking interface com.aura.dashboard.data.news.api.** { *; }
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# kxml/XmlPullParser references that are provided by the platform.
-dontwarn org.xmlpull.v1.**
-dontwarn org.kxml2.**
