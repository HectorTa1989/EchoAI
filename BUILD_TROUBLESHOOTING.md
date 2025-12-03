# APK Build Troubleshooting Guide

This document explains the issues encountered during APK build and how they were resolved.

## Issues Encountered & Solutions

### 1. Missing Gradle Wrapper Scripts

**Problem:**
```
./gradlew : The term './gradlew' is not recognized
```

**Root Cause:**
- The `gradlew` and `gradlew.bat` scripts were missing from the `whisper_java` directory
- User tried to run build from root directory instead of `whisper_java` subdirectory

**Solution:**
1. Navigate to the correct directory:
   ```bash
   cd whisper_java
   ```

2. Regenerate Gradle wrapper using Java directly:
   ```bash
   java -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain wrapper
   ```

3. Run build:
   ```bash
   ./gradlew.bat assembleDebug  # Windows
   ./gradlew assembleDebug       # Linux/Mac
   ```

---

### 2. Android SDK Not Found

**Problem:**
```
SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable 
or by setting the sdk.dir path in your project's local properties file
```

**Root Cause:**
- `local.properties` file was missing (this file is gitignored by default)
- Android SDK path not configured

**Solution:**
Create `whisper_java/local.properties` with your SDK path:
```properties
sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
```

For Windows PowerShell:
```powershell
@"
sdk.dir=C\:\\Users\\HLC\\AppData\\Local\\Android\\Sdk
"@ | Out-File -FilePath "local.properties" -Encoding UTF8
```

---

### 3. DEX Merge Error (65K Method Limit)

**Problem:**
```
Error while merging dex archives:
com.android.builder.dexing.DexArchiveMergerException
```

**Root Cause:**
- Android apps have a 65,536 method reference limit (64K limit)
- Large libraries (Apache POI for DOCX, iTextPDF for PDF) exceeded this limit
- Total method count exceeded 65K due to:
  - Apache POI: ~40K methods
  - iTextPDF: ~25K methods
  - TensorFlow Lite: ~15K methods
  - Android framework: ~10K methods

**Solution:**
Enable **MultiDex** support in `app/build.gradle`:

```gradle
android {
    defaultConfig {
        // Enable multidex support for large libraries
        multiDexEnabled true
    }
}

dependencies {
    // Multidex support for apps with 65K+ methods
    implementation 'androidx.multidex:multidex:2.0.1'
}
```

**What is MultiDex?**
- MultiDex allows apps to use multiple DEX files instead of a single one
- Each DEX file can contain up to 65K methods
- Android 5.0+ (API 21+) has native MultiDex support
- For older devices, the MultiDex library provides compatibility

---

### 4. Gradle Out of Memory (JVM Heap Space)

**Problem:**
```
Gradle build daemon has been stopped: since the JVM garbage collector is thrashing
```

**Root Cause:**
- Default Gradle heap size (typically 512MB-1GB) insufficient for compiling large libraries
- Apache POI and iTextPDF require significant memory during compilation
- Multiple parallel build tasks exhausted available memory

**Solution:**
Increase Gradle heap size in `gradle.properties`:

```properties
android.useAndroidX=true
android.enableJetifier=true

# Increase Gradle heap size for large libraries (Apache POI, iTextPDF)
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError
org.gradle.daemon=true
org.gradle.parallel=true
```

**Explanation:**
- `-Xmx4096m`: Maximum heap size of 4GB
- `-XX:MaxMetaspaceSize=512m`: Metaspace limit of 512MB
- `-XX:+HeapDumpOnOutOfMemoryError`: Create heap dump on OOM for debugging
- `org.gradle.daemon=true`: Enable Gradle daemon for faster builds
- `org.gradle.parallel=true`: Enable parallel execution

**After updating gradle.properties:**
```bash
# Stop all running Gradle daemons to apply new settings
./gradlew --stop

# Clean and rebuild
./gradlew clean assembleDebug
```

---

## Complete Build Process

### Prerequisites
1. **Java Development Kit (JDK) 11 or higher**
   - Check: `java -version`
   - Set JAVA_HOME if needed

2. **Android SDK**
   - Install via Android Studio
   - Typical location: `C:\Users\<Username>\AppData\Local\Android\Sdk`

3. **Minimum 6GB RAM** available for build process

### Step-by-Step Build Instructions

1. **Navigate to project directory:**
   ```bash
   cd whisper_java
   ```

2. **Configure SDK location** (if not exists):
   ```bash
   # Create local.properties
   echo "sdk.dir=C\:\\Users\\HLC\\AppData\\Local\\Android\\Sdk" > local.properties
   ```

3. **Configure Gradle memory** (create/update gradle.properties):
   ```properties
   android.useAndroidX=true
   android.enableJetifier=true
   org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
   org.gradle.daemon=true
   org.gradle.parallel=true
   ```

4. **Stop any running Gradle daemons:**
   ```bash
   ./gradlew --stop
   ```

5. **Build APK:**
   ```bash
   # Debug build
   cd whisper_java
   ./gradlew --stop
   ./gradlew clean assembleDebug
   
   # Release build (requires signing configuration)
   ./gradlew clean assembleRelease
   ```

6. **Locate the APK:**
   ```
   whisper_java/app/build/outputs/apk/debug/app-debug.apk
   whisper_java/app/build/outputs/apk/release/app-release.apk
   ```

---

## Performance Optimization Tips

### Reduce Build Time

1. **Increase Gradle heap further** (if you have 16GB+ RAM):
   ```properties
   org.gradle.jvmargs=-Xmx6144m -XX:MaxMetaspaceSize=1024m
   ```

2. **Use Gradle build cache:**
   ```properties
   org.gradle.caching=true
   ```

3. **Avoid clean builds** unless necessary:
   ```bash
   ./gradlew assembleDebug  # Instead of clean assembleDebug
   ```

### Reduce APK Size

1. **Enable ProGuard/R8** for release builds:
   ```gradle
   buildTypes {
       release {
           minifyEnabled true
           shrinkResources true
           proguardFiles getDefaultProguardFile('proguard-android-optimize.txt')
       }
   }
   ```

2. **Use APK splits** for different architectures:
   ```gradle
   android {
       splits {
           abi {
               enable true
               reset()
               include 'armeabi-v7a', 'arm64-v8a'
           }
       }
   }
   ```

---

## Common Build Errors & Quick Fixes

| Error | Quick Fix |
|-------|-----------|
| `gradle: command not found` | Use `./gradlew` instead of `gradle` |
| `Permission denied` | Run `chmod +x gradlew` on Linux/Mac |
| `Build failed with exit code 137` | Increase heap size in gradle.properties |
| `Could not resolve dependencies` | Check internet connection, sync Gradle |
| `Unsupported class file version` | Update JDK to version 11+ |
| `Manifest merger failed` | Check for conflicting permissions/activities |

---

## Verification

After successful build, verify the APK:

```bash
# Check APK exists
ls app/build/outputs/apk/debug/

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk

# Check APK method count
./gradlew app:methodCount
```

---

## Additional Resources

- [Android MultiDex Documentation](https://developer.android.com/studio/build/multidex)
- [Gradle Performance Guide](https://docs.gradle.org/current/userguide/performance.html)
- [Android Build Configuration](https://developer.android.com/studio/build)

---

**Author:** HectorTa1989  
**Last Updated:** 2025-12-01
