/*
   Parts of this class are based on code from google:
   https://github.com/android/location-samples
 */

package tu.tk3.g31.tk3_project;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;

import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

public class GeofenceTransitionsJobIntentService extends JobIntentService {
    private static final int JOB_ID = 573;

    private static final String TAG = "GeofenceTransitionsIS";

    private static final String CHANNEL_ID = "channel_01";

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, GeofenceTransitionsJobIntentService.class, JOB_ID, intent);
    }

    /**
     * Handles incoming intents.
     * @param intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleWork(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        Log.w(TAG, "Wörk");

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        /* === RMV === */
        boolean rmvEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(".RmvEnabled", false);
        if (rmvEnabled && geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.w(TAG, "ENTER RMV");

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            String geofenceTransitionString = getTransitionString(geofenceTransition);

            // Get the Ids of each geofence that was triggered.
            ArrayList<String> triggeringGeofencesIdsList = new ArrayList<>();
            boolean rmv = false;
            for (Geofence geofence : triggeringGeofences) {
                triggeringGeofencesIdsList.add(geofence.getRequestId());
                if (geofence.getRequestId().contains("RMV"))
                    rmv = true;
            }

            if (rmv) {
                ArrayList<DetectedActivity> detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
                        PreferenceManager.getDefaultSharedPreferences(this)
                                .getString(MainActivity.DETECTED_ACTIVITY, ""));

                for (DetectedActivity d : detectedActivities) {
                    if (d.getType() == DetectedActivity.ON_FOOT && d.getConfidence() >= 50 &&
                        d.getType() == DetectedActivity.RUNNING && d.getConfidence() >= 30) {
                        //Trigger RMV
                        openRMVDepartures();
                        break;
                    }
                }
            }
        }

        boolean mensaEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(".MensaEnabled", false);
        if (mensaEnabled && geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.w(TAG, "ENTER Mensa");
            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            String geofenceTransitionString = getTransitionString(geofenceTransition);

            // Get the Ids of each geofence that was triggered.
            ArrayList<String> triggeringGeofencesIdsList = new ArrayList<>();
            boolean mensa = false;
            for (Geofence geofence : triggeringGeofences) {
                triggeringGeofencesIdsList.add(geofence.getRequestId());
                if (geofence.getRequestId().contains("MENSA"))
                    mensa = true;
            }

            if (mensa) {
                ArrayList<DetectedActivity> detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
                        PreferenceManager.getDefaultSharedPreferences(this)
                                .getString(MainActivity.DETECTED_ACTIVITY, ""));

                for (DetectedActivity d : detectedActivities) {
                    if (d.getType() == DetectedActivity.ON_FOOT && d.getConfidence() >= 50 &&
                            d.getType() == DetectedActivity.STILL && d.getConfidence() <= 20) {
                        //Trigger MENSA
                        openMensaApp();
                        break;
                    }
                }
            }
        }
    }

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param geofenceTransition    The ID of the geofence transition.
     * @param triggeringGeofences   The geofence(s) triggered.
     * @return                      The transition details formatted as String.
     */
    private String getGeofenceTransitionDetails(
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Get the Ids of each geofence that was triggered.
        ArrayList<String> triggeringGeofencesIdsList = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ",  triggeringGeofencesIdsList);

        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the MainActivity.
     */
    private void sendNotification(String notificationDetails) {
        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);
        }

        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Define the notification settings.
        builder.setSmallIcon(R.drawable.ic_launcher_foreground)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_launcher_foreground))
                .setColor(Color.RED)
                .setContentTitle(notificationDetails)
                .setContentText(getString(R.string.geofence_transition_notification_text))
                .setContentIntent(notificationPendingIntent);

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }

    private void checkActivity() {
        ArrayList<DetectedActivity> detectedActivities = ActivityIntentService.detectedActivitiesFromJson(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString(MainActivity.DETECTED_ACTIVITY, ""));

        for (DetectedActivity d : detectedActivities) {

        }
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType    A transition type constant defined in Geofence
     * @return                  A String indicating the type of transition
     */
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);
            default:
                return getString(R.string.unknown_geofence_transition);
        }
    }

    /**
     * Opens the Tu-Darmstadt Mensa App
     */
    public void openMensaApp(){
        Intent intent = getPackageManager().getLaunchIntentForPackage("de.incloud.mensaapp");

        // Checks if Mensa app installed
        if (intent == null){
            intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=de.incloud.mensaapp"));
        }
        startActivity(intent);
    }

    /**
     * Open RMV Departure View
     */
    public void openRMVDepartures(){

        Intent intent = new Intent(this, RMVActivity.class);
        //Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.rmv.de/auskunft/bin/jp/stboard.exe/dn?L=vs_anzeigetafel&cfgfile=DarmstadtA_3024011_1707051365&start=1"));
        startActivity(intent);


    }
}
