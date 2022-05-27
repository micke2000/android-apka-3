package com.example.watki;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int KOD_WRITE_EXTERNAL_STORAGE = 4200;

    private EditText mTextUrl;
    private Button mButtonGetInformation;
    private TextView mFileSizeTextView;
    private TextView mFileTypeTextView;
    private Button mDownloadButton;
    private ProgressBar mProgressBar;
    private TextView mDownloadedBytes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextUrl = findViewById(R.id.adres_edit);
        mTextUrl.setText(R.string.exampleUrl);
        mButtonGetInformation = findViewById(R.id.getInformationButton);
        mFileSizeTextView = findViewById(R.id.rozmiar_texview);
        mFileTypeTextView = findViewById(R.id.type_textview);
        mDownloadButton = findViewById(R.id.downloadButton);
        mDownloadedBytes = findViewById(R.id.downloadedBytesTextView);
        mProgressBar = findViewById(R.id.mProgressBar);
        mButtonGetInformation.setOnClickListener(view -> {
            DownloadFileDetails fileDetailsTask = new DownloadFileDetails();
            fileDetailsTask.execute(mTextUrl.getText().toString());
        });
        mDownloadButton.setOnClickListener(view -> {
            Log.d(TAG, "Prepare for starting download file service");
            boolean appHasWriteExternalStoragePermission =
                    ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED;

            if (appHasWriteExternalStoragePermission) {
                startFileDownloadService();
            } else {
                askForPermissions();
            }
        });
    }

    private void startFileDownloadService() {
        DownloadFileService.startService(MainActivity.this, mTextUrl.getText().toString());
        Log.d(TAG, "Download file service started");

    }

    private void askForPermissions() {
        boolean shouldAppAskForExternalStoragePermission = ActivityCompat.shouldShowRequestPermissionRationale(
                MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (shouldAppAskForExternalStoragePermission) {
            CharSequence message = getResources().getText(R.string.askExternalStoragePermissionMessage);
            Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
        }
        askForWriteExternalStoragePermissions();
    }

    private void askForWriteExternalStoragePermissions() {
        ActivityCompat.requestPermissions(MainActivity.this,
                new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                KOD_WRITE_EXTERNAL_STORAGE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == KOD_WRITE_EXTERNAL_STORAGE) {
            boolean doesAppHasExternalStoragePermissions = permissions.length > 0
                    && permissions[0].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (doesAppHasExternalStoragePermissions) {
                startDownloadFileService();
                return;
            }
            throw new UnsupportedOperationException("Permission not granted...");
        }
        throw new UnsupportedOperationException("Unknown request code...");
    }

    private void startDownloadFileService() {
        DownloadFileService.startService(MainActivity.this, mTextUrl.getText().toString());
        Log.d(TAG, "Download file service started");
    }

    class DownloadFileDetails extends AsyncTask<String, Void, FileDetails> {
        @Override
        protected FileDetails doInBackground(String... strings) {
            FileDetails result = null;
            URLConnection connection = null;
            try {
                URL url = new URL(strings[0]);
                connection = (HttpsURLConnection) url.openConnection();
                return new FileDetails(connection.getContentLength(), connection.getContentType());
            } catch (MalformedURLException e) {
                result = handleException("URL error", "Bad URL provided to address field", e);
            }
            catch (SecurityException e) {
                result = handleException("Permission error", "Application doesn't have permission to connect with the Internet", e);
            } catch (IOException e) {
                result = handleException("Task error", "Cannot receive file size", e);
            }
            return result;
        }

        private FileDetails handleException(String messageTag, String message, Throwable exception) {
            Log.e(messageTag, message, exception);
            exception.printStackTrace();
            return new FileDetails(0, "none", true, message);
        }

        @Override
        protected void onPostExecute(FileDetails result) {
            super.onPostExecute(result);

            if (result.hasError()) {
                Toast.makeText(MainActivity.this, result.getErrorMessage(), Toast.LENGTH_LONG).show();
            }

            mFileSizeTextView.setText(String.valueOf(result.getSize()));
            mFileTypeTextView.setText(result.getType());
        }
    }
    class TimeBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            PostepInfo postep = intent.getParcelableExtra("downloadProgress");
            Log.d(TAG, "onReceive() - time received by broadcast: "
                    + postep.mStatus + "," + postep.mPobranychBajtow +","+postep.mRozmiar);
           mProgressBar.setMax(postep.mRozmiar);
           mProgressBar.setProgress(postep.mPobranychBajtow);
           mDownloadedBytes.setText(String.valueOf(postep.mPobranychBajtow));
           if("done".equals(postep.mStatus)){
               mDownloadedBytes.setText("Done");
               Toast.makeText(MainActivity.this, "Downloading finished", Toast.LENGTH_SHORT).show();
           }
        }
    }
    private TimeBroadcastReceiver mTimeBroadcastReceiver = new TimeBroadcastReceiver();
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mTimeBroadcastReceiver, new IntentFilter(DownloadFileService.ACTION_BROADCAST));
    }
    @Override
    protected void onStop() {
        unregisterReceiver(mTimeBroadcastReceiver);
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("FileSize",mFileSizeTextView.getText().toString());
        outState.putString("FileType",mFileTypeTextView.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String fileSize = savedInstanceState.getString("FileSize");
        String fileType = savedInstanceState.getString("FileType");

        mFileSizeTextView.setText(fileSize);
        mFileTypeTextView.setText(fileType);
    }
}
