package sabbir.apk.Reminder;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_CLASS_REMINDER = "sabbir.apk.ACTION_CLASS_REMINDER";
    public static final String ACTION_FIRST_CLASS_ALARM = "sabbir.apk.ACTION_FIRST_CLASS_ALARM";
    public static final String EXTRA_SUBJECT = "extra_subject";
    public static final String EXTRA_INSTRUCTOR = "extra_instructor";
    public static final String EXTRA_START_TIME = "extra_start_time";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }

        Intent serviceIntent = new Intent(context, ReminderService.class);
        serviceIntent.setAction(intent.getAction());
        serviceIntent.putExtras(intent);
        context.startService(serviceIntent);
    }
}
