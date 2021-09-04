/*
 * Copyright (c) 2019 FTC team 4634 FROGbots
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.openftc.opencvrepackaged;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Environment;

import com.qualcomm.robotcore.eventloop.opmode.AnnotatedOpModeManager;
import com.qualcomm.robotcore.eventloop.opmode.OpModeRegistrar;
import com.qualcomm.robotcore.hardware.configuration.LynxConstants;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.internal.network.NetworkConnectionHandler;
import org.firstinspires.ftc.robotcore.internal.network.PeerStatusCallback;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;
import org.firstinspires.ftc.robotcore.internal.ui.UILocation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;

public class DynamicOpenCvNativeLibLoader
{
    private static final String NATIVE_LIB_FILENAME = "libOpenCvAndroid453.so";
    private static final String NATIVE_LIB_MD5 = "9d9a9ed11665dc92c91c475aad54ef94";
    private static final String TAG = "OpenFTC-OpenCV-Repackaged-Loader";
    private static boolean alreadyLoaded = false;
    private static Runnable onPeerConnectedRunnable = null;

    private File libInProtectedStorage;
    private File protectedExtraFolder;
    private File libOnSdcard;
    private Activity rcActivity;

    static
    {
        if(LynxConstants.isRevControlHub())
        {
            NetworkConnectionHandler.getInstance().registerPeerStatusCallback(new PeerStatusCallback()
            {
                @Override
                public void onPeerConnected()
                {
                    if(onPeerConnectedRunnable != null)
                    {
                        onPeerConnectedRunnable.run();
                    }
                }

                @Override
                public void onPeerDisconnected()
                {

                }
            });
        }
    }

    /*
     * By annotating this method with @OpModeRegistrar, it will be called
     * automatically by the SDK as it is scanning all the classes in the app
     * (for @Teleop, etc.) while it is "starting" the robot.
     */
    @OpModeRegistrar
    public static void loadNativeLibOnStartRobot(Context context, AnnotatedOpModeManager manager)
    {
        /*
         * Because this is called every time the robot is "restarted" we
         * check to see whether we've already previously done our job here.
         */
        if(alreadyLoaded)
        {
            /*
             * Get out of dodge
             */
            return;
        }

        DynamicOpenCvNativeLibLoader loader = new DynamicOpenCvNativeLibLoader();
        loader.setupOpenCVNativeLib();
    }

    @SuppressLint("UnsafeDynamicallyLoadedCode")
    private void setupOpenCVNativeLib()
    {
        onPeerConnectedRunnable = null;

        rcActivity = AppUtil.getInstance().getRootActivity();

        /*
         * OpenCV 4.x is not compatible with Android Versions below Lollipop
         */
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
        {
            // No period at the end, since a semicolon may be appended by the system
            String globalWarningMessage = "Unfortunately, OpenCV 4.x is not compatible with Android versions below 5.0 Lollipop. Any OpenCV-enabled OpModes will crash";
            RobotLog.ee(TAG, globalWarningMessage);
            RobotLog.setGlobalWarningMessage(globalWarningMessage);

            String dialogTitle = "This device is not compatible with OpenCV 4.x";
            String dialogMsg = "Unfortunately, OpenCV 4.x is not compatible with Android versions below 5.0 Lollipop. Any OpenCV-enabled OpModes will crash.";

            showErrorDialog(dialogTitle, dialogMsg);
            return;
        }

        try
        {
            /*
             * Attempt to set up the OpenCV library for loading in
             * the next statement
             */
            setupOpenCvFiles(false);

            /*
             * We've been given the go-ahead! Load up the native lib!
             */
            System.load(libInProtectedStorage.getAbsolutePath());
            alreadyLoaded = true;
        }
        catch (OpenCvNativeLibNotFoundException e)
        {
            // No period at the end, since a semicolon may be appended by the system
            String globalWarningMessage = String.format("%s was not found, any OpenCV-enabled OpModes will crash. Please copy it to the FIRST folder on the internal storage", NATIVE_LIB_FILENAME);
            RobotLog.ee(TAG, e, globalWarningMessage);
            RobotLog.setGlobalWarningMessage(globalWarningMessage);

            String dialogTitle = String.format("%s not found", NATIVE_LIB_FILENAME);
            String dialogMsg = String.format("%s was not found, any OpenCV-enabled OpModes will crash. Please copy it to the FIRST folder on the internal storage.", NATIVE_LIB_FILENAME);

            showErrorDialog(dialogTitle, dialogMsg);
        }
        catch (OpenCvNativeLibCorruptedException e)
        {
            // No period at the end, since a semicolon may be appended by the system.
            String globalWarningMessage = String.format("%s is present in the FIRST on the internal storage. However, the MD5 " +
                    "checksum does not match what is expected. Any OpenCV-enabled OpModes will likely crash. Delete and re-download the file", NATIVE_LIB_FILENAME);
            RobotLog.ee(TAG, e, globalWarningMessage);
            RobotLog.setGlobalWarningMessage(globalWarningMessage);

            String dialogTitle = String.format("%s corrupted", NATIVE_LIB_FILENAME);
            String dialogMsg = String.format("%s is present in the FIRST on the internal storage. However, the MD5 " +
                    "checksum does not match what is expected. Any OpenCV-enabled OpModes will likely crash. Delete and re-download the file.", NATIVE_LIB_FILENAME);

            showErrorDialog(dialogTitle, dialogMsg);
        }
        catch (Exception | Error e)
        {
            if(e instanceof UnsatisfiedLinkError && ((UnsatisfiedLinkError) e).getMessage().contains("32-bit instead of 64-bit"))
            {
                // No period at the end, since a semicolon may be appended by the system.
                String globalWarningMessage = "Could not load OpenCV native library because app is running in 64-bit mode. Please remove the arm64-v8a entries from build.common.gradle.";
                RobotLog.ee(TAG, e, globalWarningMessage);
                RobotLog.setGlobalWarningMessage(globalWarningMessage);

                String dialogTitle = "Failed to load OpenCV native library";
                String dialogMsg = "The OpenCV native library could not be loaded because the app is running in 64-bit mode. Please remove the arm64-v8a entries from build.common.gradle. Any OpenCV-enabled OpModes will crash.";

                showErrorDialog(dialogTitle, dialogMsg);
            }
            else
            {
                // No period at the end, since a semicolon may be appended by the system.
                String globalWarningMessage = "Error occurred while loading OpenCV native library! Any OpenCV-enabled OpModes will crash";
                RobotLog.ee(TAG, e, globalWarningMessage);
                RobotLog.setGlobalWarningMessage(globalWarningMessage);

                String dialogTitle = "Error loading OpenCV native library";
                String dialogMsg = "An error occurred while loading the OpenCV native library. Any OpenCV-enabled OpModes will crash.";

                showErrorDialog(dialogTitle, dialogMsg);
            }
        }
    }

    private void setupOpenCvFiles(boolean forceCopy) throws OpenCvNativeLibNotFoundException, OpenCvNativeLibCorruptedException, CopyOpenCvNativeLibToProtectedStorageException
    {
        libInProtectedStorage = new File(String.format("%s/extra/%s", rcActivity.getFilesDir(), NATIVE_LIB_FILENAME));
        protectedExtraFolder = new File(String.format("%s/extra/", rcActivity.getFilesDir()));
        libOnSdcard = new File(String.format("%s/FIRST/%s", Environment.getExternalStorageDirectory(), NATIVE_LIB_FILENAME));

        /*
         * First, check to see if it exists in the protected storage
         */
        if((!libInProtectedStorage.exists()) || forceCopy)
        {
            /*
             * Ok, so it's not in the protected storage. Check if it exists
             * in the FIRST folder on the SDcard
             */
            if(libOnSdcard.exists())
            {
                /*
                 * Yup, it exists, but we need to verify the integrity of the file
                 * with the MD5 hash before we copy it, otherwise bad things might
                 * happen when we try to load a corrupted lib...
                 */
                if(MD5.checkMD5(NATIVE_LIB_MD5, libOnSdcard))
                {
                    /*
                     * Alright, everything checks out, so copy it to the protected
                     * storage and continue with the app launch!
                     */
                    copyLibFromSdcardToProtectedStorage();
                }
                else
                {
                    /*
                     * Oooh, not good - it's corrupted.
                     * Show the user a dialog explaining the situation
                     */
                    throw new OpenCvNativeLibCorruptedException();
                }
            }
            else
            {
                /*
                 * Welp, it doesn't exist on the SDcard either :(
                 * Show the user a dialog explaining the situation
                 */
                throw new OpenCvNativeLibNotFoundException();
            }
        }
        else
        {
            /*
             * Ok so it does exist in the protected storage. This means that we ourselves
             * copied it in here earlier. Ordinarily this would mean that we do not need
             * to do an MD5 check on it, since it would have been checked before it was
             * copied in. However, in the case that the user upgrades the OpenCV Java SDK
             * version and a previous version of the native lib is already in the protected
             * storage, then things would likely explode. So, we check the MD5 here anyway.
             * It doesn't really cost *that* much CPU time anyway; <200ms
             */
            if(!MD5.checkMD5(NATIVE_LIB_MD5, libInProtectedStorage))
            {
                setupOpenCvFiles(true);
            }
        }
    }

    private void showErrorDialog(final String title, final String message)
    {
        if(LynxConstants.isRevControlHub())
        {
            //If robocol isn't linked yet, register the dialog for later
            if(!NetworkConnectionHandler.getInstance().isPeerConnected())
            {
                onPeerConnectedRunnable = new Runnable()
                {
                    @Override
                    public void run()
                    {
                        AppUtil.getInstance().showAlertDialog(UILocation.BOTH, title, message);
                    }
                };
            }

            //Robocol is linked, show dialog now
            else
            {
                AppUtil.getInstance().showAlertDialog(UILocation.BOTH, title, message);
            }
        }
        else
        {
            rcActivity.runOnUiThread(new Runnable()
            {
                @Override
                public void run()
                {
                    AlertDialog dialog = new AlertDialog.Builder(rcActivity)
                            .setTitle(title)
                            .setMessage(message)
                            .setCancelable(false)
                            .setPositiveButton("OK", new DialogInterface.OnClickListener()
                            {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i)
                                {
                                    System.exit(1);
                                }
                            }).create();
                    dialog.show();
                }
            });
        }
    }

    private void copyLibFromSdcardToProtectedStorage() throws CopyOpenCvNativeLibToProtectedStorageException
    {
        try
        {
            /*
             * Check if the 'extra' folder exists. If it doesn't,
             * then create it now or else the copy code will crash
             */
            if(!protectedExtraFolder.exists())
            {
                protectedExtraFolder.mkdir();
            }

            /*
             * Copy the file with a 1MiB buffer
             */
            InputStream is = new FileInputStream(libOnSdcard);
            OutputStream os = new FileOutputStream(libInProtectedStorage);
            byte[] buff = new byte[1024];
            int len;
            while ((len = is.read(buff)) > 0)
            {
                os.write(buff, 0, len);
            }
            is.close();
            os.close();
        }
        catch (Exception e)
        {
            throw new CopyOpenCvNativeLibToProtectedStorageException();
        }
    }
}