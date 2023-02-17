package vn.itplus.vinhpv.appchats.notifications;

import static vn.itplus.vinhpv.appchats.Utils.Constant.*;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import vn.itplus.vinhpv.appchats.R;
import vn.itplus.vinhpv.appchats.activity.MessageActivity;

public class FirebaseMessaging extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        sendNormalNotification(remoteMessage);
    }

    /*Vinh PV: Thông báo khi có tin nhắn mới.*/
    private void sendNormalNotification(RemoteMessage remoteMessage) {
        Map<String, String> stringMap = remoteMessage.getData();
        String user = stringMap.get(getString(R.string.key_user_notify));
        String icon = stringMap.get(getString(R.string.key_icon_notify));
        String title = stringMap.get(getString(R.string.key_title_notify));
        String body = stringMap.get(getString(R.string.key_body_notify));

        int i = Integer.parseInt(user.replaceAll("[\\D]", ""));
        Intent intent = new Intent(this, MessageActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(getString(R.string.key_uid), user);
        intent.putExtras(bundle);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pIntent = PendingIntent.getActivity(this, i, intent, PendingIntent.FLAG_ONE_SHOT);

        Uri defSoundsUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,ID)
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

    }

    private void updateToken(String tokenRefresh) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference(getString(R.string.path_tokens));
        Token token = new Token(tokenRefresh);
        reference.child(user.getUid()).setValue(token);
    }
}
