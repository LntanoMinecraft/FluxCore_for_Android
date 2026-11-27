-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

# Keep Android Manifest components
-keep class com.mouajmd.fluxcore.FluxCoreApplication { *; }
-keep class com.mouajmd.fluxcore.MainActivity { *; }
-keep class com.mouajmd.fluxcore.service.FluxService { *; }
-keep class com.mouajmd.fluxcore.service.BootReceiver { *; }
-keep class com.mouajmd.fluxcore.service.FluxCoreTileSlot* { *; }
-keep class com.mouajmd.fluxcore.shortcut.ShortcutTriggerActivity { *; }

# Keep kotlinx.serialization navigation routes & models
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.mouajmd.fluxcore.ui.navigation.**$$serializer { *; }
-keepclassmembers class com.mouajmd.fluxcore.ui.navigation.** {
    *** Companion;
}
-keepclasseswithmembers class com.mouajmd.fluxcore.ui.navigation.** {
    kotlinx.serialization.KSerializer serializer(...);
}
