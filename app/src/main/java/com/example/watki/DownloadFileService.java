package com.example.watki;

import android.app.DownloadManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.util.Locale;

public class DownloadFileService extends IntentService {
    private static final String TAG = DownloadFileService.class.getSimpleName();
    private static final String ACTION_DOWNLOAD_FILE = "com.example.lab4.download_file";
    private static final String NOTIFICATION_CHANNEL_ID = "com.example.lab4.notification_channel";
    private static final String NOTIFICATION_CHANNEL_NAME = "com.example.lab4.notification_channel";
    public static final String ACTION_BROADCAST =
            "com.example.service.broadcast";
    private static final int NOTIFICATION_ID = 1;
    private static final String URL_KEY = "com.example.lab4.url";
    private static final String BYTES_DOWNLOADED_KEY = "com.example.lab4.bytes_downloaded";
    private static final int BLOCK_SIZE = 1024;
    private static final int END_OF_FILE_CODE = -1;
    public final static String POWIADOMIENIE =  "com.example.intent_service.odbiornik";
    public final static String INFO = "info";
    private NotificationManager mNotificationManager;
    private int mBytesDownloaded = 0;
    private int mtotalSize = 0;

    public DownloadFileService() {
        super("DownloadFileService");
    }

    public DownloadFileService(String name) {
        super(name);
    }

    public static void startService(Context context, String url) {
        Intent intent = new Intent(context, DownloadFileService.class);
        intent.setAction(ACTION_DOWNLOAD_FILE);
        intent.putExtra(URL_KEY, url);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        mBytesDownloaded = 0;
        prepareNotificationManager();
        startForeground(NOTIFICATION_ID, createNotification());

        if (intent != null) {
            prepareForFileDownload(intent);
        }
    }

    private void wyslijBroadcast(Parcelable paczka,String tag) {
        //utworzenie intencji powiadomienia i umieszczenie w niej danych
        Intent zamiar = new Intent(ACTION_BROADCAST);
        zamiar.putExtra(tag, paczka);
        //wysłanie komunikatu
        sendBroadcast(zamiar);
    }

    private void prepareForFileDownload(Intent intent) {
        String action = intent.getAction();

        if (ACTION_DOWNLOAD_FILE.equals(action)) {
            String url = intent.getStringExtra(URL_KEY);
            processDownloadingFile(url);
            return;
        }
        throw new UnsupportedOperationException("Wrong action provided");
    }


    private void processDownloadingFile(String urlString) {
        try {
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            int totalSize = connection.getContentLength();
            mtotalSize = totalSize;
            InputStream reader = new DataInputStream(connection.getInputStream());

            File tempFile = new File(url.getFile());
                File outputFile = new File(Environment.getExternalStorageDirectory()+ File.separator + tempFile.getName());

            if (outputFile.exists()) {
                deletePreviousOutputFile(outputFile);
            }

            OutputStream writer =  new FileOutputStream(outputFile.getPath());
            downloadFile(reader, writer,outputFile,totalSize);

        } catch (IOException e) {
            Log.e(TAG, "Exception during file download");
            e.printStackTrace();
        }
    }

    private void downloadFile(InputStream reader, OutputStream writer,File outputFile,int totalSize) throws IOException {
        byte[] buffer = new byte[BLOCK_SIZE];
        int newBytesDownloaded = reader.read(buffer, 0, BLOCK_SIZE);

        while (newBytesDownloaded != END_OF_FILE_CODE) {
            writer.write(buffer, 0, newBytesDownloaded);
            mBytesDownloaded += newBytesDownloaded;
            newBytesDownloaded = reader.read(buffer, 0, BLOCK_SIZE);

            Log.d(TAG, String.format("Downloaded portion of %d bytes. Bytes downloaded: %d", newBytesDownloaded, mBytesDownloaded));
            mNotificationManager.notify(NOTIFICATION_ID, createNotification());
            PostepInfo postep = new PostepInfo(mBytesDownloaded,mtotalSize,"w trakcie");
            wyslijBroadcast(postep,"downloadProgress");
        }
        PostepInfo postep = new PostepInfo(mBytesDownloaded,mtotalSize,"done");
        wyslijBroadcast(postep,"downloadProgress");
        Log.d("Download finished","Your file has been downloaded.");
        writer.flush();
        writer.close();
        reader.close();

        Context mainApp = getApplicationContext();

        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.addCompletedDownload(outputFile.getName(), outputFile.getName(), true, "application/json", outputFile.getAbsolutePath(),outputFile.length(),true);

    }

    private void deletePreviousOutputFile(File outputFile) {
        boolean fileHasBeenDeleted = outputFile.delete();
        if (fileHasBeenDeleted) {
            Log.d(TAG, String.format(Locale.getDefault(), "Previous version of file %s was deleted", outputFile.getName()));
        }
    }

    private Notification createNotification() {
        PendingIntent pendingIntent = prepareNotificationIntents();
        return buildNotification(pendingIntent);
    }

    private PendingIntent prepareNotificationIntents() {

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.putExtra(BYTES_DOWNLOADED_KEY, mBytesDownloaded);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(notificationIntent);
        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

    }

    private Notification buildNotification(PendingIntent pendingIntent) {
        Notification.Builder notificationBuilder = new Notification.Builder(this);

        notificationBuilder
                .setContentTitle("Downloading file")
                .setContentText(String.format(Locale.getDefault(),"%d bytes downloaded", mBytesDownloaded))
                .setProgress(mtotalSize,mBytesDownloaded,false)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_HIGH)
                .setOngoing(true);
//        if ()
//        {
//            notificationBuilder.setOngoing(false);
//        } else {
//            notificationBuilder.setOngoing(true);
//        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationBuilder.setChannelId(NOTIFICATION_CHANNEL_ID);
        }
        //tworzymy i zwracamy powiadomienie
        return notificationBuilder.build();
    }

    private void prepareNotificationManager() {
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String appName = getString(R.string.app_name);
            NotificationChannel notificationChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID, appName, NotificationManager.IMPORTANCE_LOW);
            mNotificationManager.createNotificationChannel(notificationChannel);
        }
    }
}
