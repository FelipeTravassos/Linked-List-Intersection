package com.fstravassos.emailprocessorservice;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ResultReceiver;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;

public class RemoveDuplicateIntentService extends IntentService {

    public static final String CHANNEL_ID = "com.fstravassos.emailprocessorservice";
    public static final String CHANNEL_NAME = "RemoveDuplicateIntentService";
    public static final int NOTIFICATION_ID = 1;

    public static final int RECEIVER_RESULT_SUCCESS = 1;
    public static final String RESULT_RECEIVER_KEY = "RESULT_RECEIVER";
    public static final String PARAMS_KEY = "PARAMS";
    public static final String LINKED_LIST_KEY = "EMAILS";
    public static final String PACKAGE_KEY = "PACKAGE";
    public static final String REMOVE_DUPLICATE_ACTION = "REMOVE_DUPLICATE";

    private static LinkedList<String> mLastList = new LinkedList<>();

    public RemoveDuplicateIntentService() {
        super("RemoveDuplicateIntentService");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground();
        } else {
            startForeground(NOTIFICATION_ID, new Notification());
        }
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        try {
            if (intent != null) {
                final String action = intent.getAction();
                if (REMOVE_DUPLICATE_ACTION.equals(action)) {
                    Context context = createPackageContext(intent.getStringExtra(PACKAGE_KEY),
                            Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
                    ClassLoader cl = context.getClassLoader();

                    Bundle bundle = intent.getBundleExtra(PARAMS_KEY);
                    bundle.setClassLoader(cl);
                    ResultReceiver receiver = bundle.getParcelable(RESULT_RECEIVER_KEY);
                    ArrayList<String> list = (ArrayList<String>) bundle.getSerializable(LINKED_LIST_KEY);

                    if (list != null && receiver != null) {
                        removeDuplicates(new LinkedList<>(list), Objects.requireNonNull(receiver));
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove duplicated items
     *
     * @param list     List with items
     * @param receiver Listener to send result
     */
    private void removeDuplicates(LinkedList<String> list, ResultReceiver receiver) {
        LinkedList<String> aux = new LinkedList<>();
        for (int j, i = 0; i < list.size(); i++) {
            String item = list.get(i);
            if (mLastList.size() > 0 && item.equals(mLastList.get(0))) {
                aux.addAll(mLastList);
                break;
            } else {
                for (j = i + 1; j < list.size(); j++) {
                    if (item.equals(list.get(j))) list.remove(j--);
                }
                aux.add(item);
            }
        }

        mLastList = aux;
        Bundle bundle = new Bundle();
        bundle.putSerializable(LINKED_LIST_KEY, aux);
        receiver.send(RECEIVER_RESULT_SUCCESS, bundle);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void startForeground() {
        NotificationChannel chan = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME,
                NotificationManager.IMPORTANCE_NONE);
        NotificationManager manager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        Objects.requireNonNull(manager).createNotificationChannel(chan);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        Notification notification = builder.setOngoing(true)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }
}