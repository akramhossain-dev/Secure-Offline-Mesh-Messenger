# ProGuard / R8 Rules — Debug Build
# Preserves everything for debuggability

-dontobfuscate
-dontoptimize
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keep class ** { *; }
