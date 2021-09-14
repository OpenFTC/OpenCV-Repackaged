## OpenCV-Repackaged

### What exactly is this?

This is the OpenCV Java API, repackaged into an AAR that you can add as a Gradle dependency to your Android Studio project. This is exactly the same thing that is in the `/sdk/java` folder of the OpenCV Android bundle.

### Why in the world would I want to use this?

To avoid the pain of having to download the OpenCV Android bundle, extract the Java library, and import it as a module in Android Studio. This bloats your git repository and makes it a pain to upgrade to a newer OpenCV version.
**However, that's not even the biggest benefit of using this library!** To decrease the final APK size (and thus decreasing deployment time to your robot each time you make a code change), this library loads the C++ library required by OpenCV (more than 10MB!) dynamically from the internal storage instead of shipping it inside the APK.

### Device compatibility:

Unfortunately, due to a [known bug with OpenCV 4.x](https://github.com/opencv/opencv/issues/15389), OpenCV-Repackaged is only compatible with devices that run Andorid 5.0 or higher. For FTC, this means that it is incompatible with the ZTE Speed. OpenCV-Repackaged will work fine on all other FTC-legal devices (including the new Control Hub).

### So how do I use this?

1. Open your FTC SDK Android Studio project

2. Open the `build.gradle` file for the TeamCode module:

    ![img-her](doc/images/teamcode-gradle.png)

3. At the bottom, add this:

        dependencies {
            implementation 'org.openftc:opencv-repackaged:4.5.3-A'
         }

6. Copy `libOpenCvAndroid453.so` from the `/doc/native_libs` folder of this repo into the `FIRST` folder on the USB storage of the Robot Controller (i.e. connect the Robot Controller to your computer with a USB cable, put it into MTP mode, and drag 'n drop the file).

7. You can now use OpenCV just as you would as if you had manually imported the module, with one minor difference being you do **not** need to call any methods such as `OpenCVLoader.initDebug()` because the native library is automatically loaded in the background when the SDK boots.

## Changelog:

#### v4.5.3-A

 - OpenCV Android SDK updated to v4.5.3

#### v4.1.0-C

 - Specifically handle error case of failure to load 32-bit library when FTC Robot Controller app has already loaded another native library as 64-bit

#### v4.1.0-B

 - Drastically improve error handling when loading native library

#### v4.1.0-A

 - Initial release, based on OpenCV Android SDK v4.1.0
