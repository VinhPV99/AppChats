package vn.itplus.vinhpv.appchats.notifications;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import vn.itplus.vinhpv.appchats.activity.MessageActivity;

public class FirebaseMessaging extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        sendNormalNotification(remoteMessage);
    }

    private void sendNormalNotification(RemoteMessage remoteMessage) {
        Map<String, String> stringMap = remoteMessage.getData();
        String user = stringMap.get("user");
        String icon = stringMap.get("icon");
        String title = stringMap.get("title");
        String body = stringMap.get("body");

        int i = Integer.parseInt(user.replaceAll("[\\D]", ""));
        Intent intent = new Intent(this, MessageActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("uid", user);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pIntent = PendingIntent.getActivity(this, i, intent, PendingIntent.FLAG_ONE_SHOT);

        Uri defSoundsUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,MyApplication.ID)
                .setSmallIcon(Integer.parseInt(icon))
                .setContentText(body)
                .setContentTitle(title)
                .setAutoCancel(true)
                .setSound(defSoundsUri)
                .setContentIntent(pIntent);
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int j = 0;
        if (i > 0) {
            j = i;
        }
        notificationManager.notify(j, builder.build());
    }

    @Override
    public void onNewToken(@NonNull String tokenRefresh) {
        super.onNewToken(tokenRefresh);
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            updateToken(tokenRefresh);
        }

        Log.d("TAG", "tokenRefresh : "+ tokenRefresh);
    }

    private void updateToken(String tokenRefresh) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Tokens");
        Token token = new Token(tokenRefresh);
        reference.child(user.getUid()).setValue(token);
    }
}
