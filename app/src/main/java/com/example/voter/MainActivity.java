package com.example.voter;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_SEND_SMS = 1;
    private static final String SENT = "com.example.voter.SMS_SENT";
    private static final String DELIVERED = "com.example.voter.SMS_DELIVERED";

    private EditText editTextMessageCount;
    private EditText editTextMessage;
    private EditText editTextPhoneNumber;
    private EditText editTextSleepTime;
    private Button buttonSendMessages;
    private ProgressBar progressBar;
    private TextView textViewProgress;

    private int totalMessages;
    private int sentMessages;
    private int failedMessages;

    private BroadcastReceiver sentReceiver;
    private BroadcastReceiver deliveredReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextMessageCount = findViewById(R.id.editTextMessageCount);
        editTextMessage = findViewById(R.id.editTextMessage);
        editTextPhoneNumber = findViewById(R.id.editTextPhoneNumber);
        editTextSleepTime = findViewById(R.id.editTextSleepTime);
        buttonSendMessages = findViewById(R.id.buttonSendMessages);
        progressBar = findViewById(R.id.progressBar);
        textViewProgress = findViewById(R.id.textViewProgress);

        buttonSendMessages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermission()) {
                    sendMessages();
                } else {
                    requestPermission();
                }
            }
        });
    }

    private boolean checkPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSION_REQUEST_SEND_SMS);
    }

    private void sendMessages() {
        totalMessages = Integer.parseInt(editTextMessageCount.getText().toString());
        String message = editTextMessage.getText().toString();
        String phoneNumber = editTextPhoneNumber.getText().toString();
        long sleepTime = Long.parseLong(editTextSleepTime.getText().toString());

        sentMessages = 0;
        failedMessages = 0;
        progressBar.setMax(totalMessages);
        progressBar.setProgress(0);
        textViewProgress.setText("Progress: 0/" + totalMessages);

        registerReceivers();

        new Thread(new Runnable() {
            @Override
            public void run() {
                SmsManager smsManager = SmsManager.getDefault();
                for (int i = 0; i < totalMessages; i++) {
                    PendingIntent sentPI = PendingIntent.getBroadcast(MainActivity.this, 0, new Intent(SENT), PendingIntent.FLAG_IMMUTABLE);
                    PendingIntent deliveredPI = PendingIntent.getBroadcast(MainActivity.this, 0, new Intent(DELIVERED), PendingIntent.FLAG_IMMUTABLE);

                    smsManager.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);

                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void registerReceivers() {
        sentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        sentMessages++;
                        break;
                    default:
                        failedMessages++;
                        break;
                }
                updateProgress();
            }
        };
        registerReceiver(sentReceiver, new IntentFilter(SENT), Context.RECEIVER_EXPORTED);

        deliveredReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                switch (getResultCode()) {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered", Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        };
        registerReceiver(deliveredReceiver, new IntentFilter(DELIVERED), Context.RECEIVER_EXPORTED);
    }

    private void updateProgress() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int progress = sentMessages + failedMessages;
                progressBar.setProgress(progress);
                textViewProgress.setText("Progress: " + progress + "/" + totalMessages +
                        " (Sent: " + sentMessages + ", Failed: " + failedMessages + ")");

                if (progress == totalMessages) {
                    Toast.makeText(MainActivity.this, "All messages processed", Toast.LENGTH_SHORT).show();
                    unregisterReceiver(sentReceiver);
                    unregisterReceiver(deliveredReceiver);
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sentReceiver != null) {
            unregisterReceiver(sentReceiver);
        }
        if (deliveredReceiver != null) {
            unregisterReceiver(deliveredReceiver);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendMessages();
            } else {
                Toast.makeText(this, "Permission denied. Cannot send SMS.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}