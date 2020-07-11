package tu.tk3.g31.tk3_project;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

public class Constants {
    private Constants() {
    }

    static final long ACTIVITY_RECOGNITION_INTERVAL = 3000;

    private static final String PACKAGE_NAME = "com.google.android.gms.location.Geofence";
    static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";

    static final float GEOFENCE_RADIUS_IN_METERS = 150;

    /**
     * Map for storing information about airports in the San Francisco bay area.
     */
    static final HashMap<String, LatLng> MENSA_LANDMARKS = new HashMap<>();
    static final HashMap<String, LatLng> RMV_LANDMARKS = new HashMap<>();

    static {
        // Schloss
        RMV_LANDMARKS.put("RMV_Schloss", new LatLng(49.872886,8.655337));

        // Mensa
        MENSA_LANDMARKS.put("MENSA_Stadtmitte", new LatLng(49.8758, 8.6578));
    }
}
