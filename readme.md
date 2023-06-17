## OpenCV-Repackaged

This is the OpenCV Java API, repackaged into an AAR that you can add as a Gradle dependency to your Android Studio project. This is exactly the same thing that is in the `/sdk/java` folder of the OpenCV Android bundle.

### Why would I want to use this?

To avoid the pain of having to download the OpenCV Android bundle, extract the Java library, and import it as a module in Android Studio. This bloats your git repository and makes it a pain to upgrade to a newer OpenCV version.

### Installation Instructions

1. Open your FTC SDK Android Studio project

2. Open the `build.gradle` file for the TeamCode module:

    ![img-her](doc/images/teamcode-gradle.png)

3. At the bottom, add this:

        dependencies {
            implementation 'org.openftc:opencv-repackaged-bundled-dylibs:4.7.0-A'
         }

4. You can now use OpenCV just as you would as if you had manually imported the module, with one minor difference being you do **not** need to call any methods such as `OpenCVLoader.initDebug()` because the native library is automatically loaded in the background when the SDK boots.

## Changelog:

#### v4.7.0-A

- OpenCV Android SDK updated to v4.7.0

#### v4.5.3-C
 - **Artifact name changed to `opencv-repackaged-bundled-dylibs`**
 - **Native library is bundled with the AAR instead of being loaded dynamically.**
     - The original reason for not bundling the native library was to reduce APK size for wireless deploy time, but that was something that it seems I cared about more than anyone else, and people often seem to have difficulty with setting it up properly for some reason
     - There have been multiple requests for 64-bit support, which would have made the dynamic loading from external storage even more complicated
 - 64 bit support added

#### v4.5.3-B

 - Don't use deprecated `setGlobalWarningMessage(String msg)` method

#### v4.5.3-A

 - OpenCV Android SDK updated to v4.5.3

#### v4.1.0-C

 - Specifically handle error case of failure to load 32-bit library when FTC Robot Controller app has already loaded another native library as 64-bit

#### v4.1.0-B

 - Drastically improve error handling when loading native library

#### v4.1.0-A

 - Initial release, based on OpenCV Android SDK v4.1.0
