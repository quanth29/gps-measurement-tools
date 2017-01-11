package com.google.android.apps.location.gps.gnsslogger;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by quan.tran on 01/11/17.
 */

public class NewFileLogger implements GnssListener {
    private static final String TAG = "FileLogger";
    private static final String FILE_PREFIX = "pseudoranges";
    private static final String ERROR_WRITING_FILE = "Problem writing to file.";
    private static final String COMMENT_START = "# ";
    private static final char RECORD_DELIMITER = ',';
    private static final String VERSION_TAG = "Version: ";
    private static final String FILE_VERSION = "1.4.0.0, Platform: N";

    private static final int MAX_FILES_STORED = 100;
    private static final int MINIMUM_USABLE_FILE_SIZE_BYTES = 1000;

    private final Context mContext;

    private final Object mFileLock = new Object();
    private final Object mImuFileLock = new Object();
    private BufferedWriter mGpsFileWriter;
    private File mGpsFile;
    private BufferedWriter mImuFileWriter;
    private File mImuFile;

    private LoggerFragment.UIFragmentComponent mUiComponent;

    public synchronized LoggerFragment.UIFragmentComponent getUiComponent() {
        return mUiComponent;
    }

    public synchronized void setUiComponent(LoggerFragment.UIFragmentComponent value) {
        mUiComponent = value;
    }

    public NewFileLogger(Context context) {
        this.mContext = context;
    }

    public void startNewLog(){
        logGpsFile();
        logImuFile();
    }
    public void stopLogFile() {
        if (mGpsFileWriter != null) {
            try {
                mGpsFileWriter.close();
                mGpsFileWriter = null;
            } catch (IOException e) {
                logException("Unable to close all file streams.", e);
                return;
            }
        }

        if (mImuFileWriter != null) {
            try {
                mImuFileWriter.close();
                mImuFileWriter = null;
            } catch (IOException e) {
                logException("Unable to close all file streams.", e);
                return;
            }
        }
    }

    private void logGpsFile() {
        synchronized (mFileLock) {
            File currentFile = createFile("gps");
            if (currentFile == null)
                return;

            String currentFilePath = currentFile.getAbsolutePath();
            try {
                mGpsFile = currentFile;
                mGpsFileWriter = new BufferedWriter(new FileWriter(currentFile));
            } catch (IOException e) {
                logException("Could not open file: " + currentFilePath, e);
                return;
            }

            Toast.makeText(mContext, "GPS File opened: " + currentFilePath, Toast.LENGTH_SHORT).show();
        }
    }
    private void logImuFile(){
        synchronized (mImuFileLock){
            File currentFile = createFile("imu");
            if (currentFile == null)
                return;

            String currentFilePath = currentFile.getAbsolutePath();
            BufferedWriter currentFileWriter;

            try {
                mImuFile = currentFile;
                mImuFileWriter = new BufferedWriter(new FileWriter(currentFile));
            } catch (IOException e) {
                logException("Could not open file: " + currentFilePath, e);
                return;
            }

            Toast.makeText(mContext, "IMu File opened: " + currentFilePath, Toast.LENGTH_SHORT).show();
        }
    }

    private File createFile(String namePrefix){
        File baseDirectory;
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            baseDirectory = new File(Environment.getExternalStorageDirectory(), FILE_PREFIX);
            baseDirectory.mkdirs();
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            logError("Cannot write to external storage.");
            return null;
        } else {
            logError("Cannot read external storage.");
            return null;
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyy_MM_dd_HH_mm_ss");
        Date now = new Date();
        String fileName = String.format("%s_%s_log_%s.txt", FILE_PREFIX, namePrefix, formatter.format(now));
        File currentFile = new File(baseDirectory, fileName);
        return currentFile;
    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            synchronized (mFileLock) {
                if (mGpsFileWriter == null) {
                    return;
                }
                String locationStream =
                        String.format(
                                Locale.US,
                                "%d,%f,%f,%f,%f",
                                location.getTime(),
                                location.getLatitude(),
                                location.getLongitude(),
                                location.getAltitude(),
                                location.getAccuracy());
                try {
                    mGpsFileWriter.write(locationStream);
                    mGpsFileWriter.newLine();
                } catch (IOException e) {
                    logException(ERROR_WRITING_FILE, e);
                }
            }
        }
    }

    @Override
    public void onLocationStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {

    }

    @Override
    public void onGnssMeasurementsStatusChanged(int status) {

    }

    @Override
    public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {

    }

    @Override
    public void onGnssNavigationMessageStatusChanged(int status) {

    }

    @Override
    public void onGnssStatusChanged(GnssStatus gnssStatus) {

    }

    @Override
    public void onListenerRegistration(String listener, boolean result) {

    }

    @Override
    public void onNmeaReceived(long l, String s) {

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        synchronized (mImuFileLock){
            if(mImuFileWriter == null){
                return;
            }

            switch (sensorEvent.sensor.getType()){
                case Sensor.TYPE_ACCELEROMETER:
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int sensorType) {
        synchronized (mImuFileLock){
            if(mImuFileWriter == null){
                return;
            }
        }
    }

    private void logException(String errorMessage, Exception e) {
        Log.e(GnssContainer.TAG + TAG, errorMessage, e);
        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void logError(String errorMessage) {
        Log.e(GnssContainer.TAG + TAG, errorMessage);
        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
    }
    /**
     * Implements a {@link FileFilter} to delete files that are not in the
     * {@link FileLogger.FileToDeleteFilter#mRetainedFiles}.
     */
    private static class FileToDeleteFilter implements FileFilter {
        private final List<File> mRetainedFiles;

        public FileToDeleteFilter(File... retainedFiles) {
            this.mRetainedFiles = Arrays.asList(retainedFiles);
        }

        /**
         * Returns {@code true} to delete the file, and {@code false} to keep the file.
         *
         * <p>Files are deleted if they are not in the {@link FileLogger.FileToDeleteFilter#mRetainedFiles} list.
         */
        @Override
        public boolean accept(File pathname) {
            if (pathname == null || !pathname.exists()) {
                return false;
            }
            if (mRetainedFiles.contains(pathname)) {
                return false;
            }
            return pathname.length() < MINIMUM_USABLE_FILE_SIZE_BYTES;
        }
    }
}
