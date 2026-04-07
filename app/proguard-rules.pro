# 스택 트레이스 디버깅용 줄 번호 유지
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin ────────────────────────────────────────────────────────────────────
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }

# ── Retrofit / OkHttp ─────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Retrofit: interface 메서드 어노테이션 유지
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# ── Gson / JSON 직렬화 ────────────────────────────────────────────────────────
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# 네트워크 DTO 클래스 유지 (Gson이 리플렉션으로 사용)
-keep class com.yoshi0311.sharedledger.network.** { *; }

# ── Room ──────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.**

# ── Hilt / Dagger ────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-dontwarn dagger.**

# ── DataStore / Protobuf ─────────────────────────────────────────────────────
-dontwarn androidx.datastore.**

# ── Coroutines ────────────────────────────────────────────────────────────────
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ── 앱 도메인 모델 / Entity 유지 ─────────────────────────────────────────────
-keep class com.yoshi0311.sharedledger.data.db.entity.** { *; }

# ── Compose (일반적으로 별도 설정 불필요, 아래는 안전망) ──────────────────────
-dontwarn androidx.compose.**
