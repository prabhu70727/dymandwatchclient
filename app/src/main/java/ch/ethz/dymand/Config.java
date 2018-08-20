package ch.ethz.dymand;

import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;

import ch.ethz.dymand.Sensors.SensorRecorder;

import java.util.UUID;

public class Config {
    public static final int NOTIFICATION_ID = 71193;
    public static final String CHANNEL_ID = "DynamdNotificationServiceChannel";


    public static final int MINUTE = 60000; // number of milliseconds in a minute
    public static final int HOUR = MINUTE * 60;
    public static final int SERVICE_LIFE = 1 * HOUR; // needs to be 7 days

    // For Demo
    public static final int RECORD_TIME = MINUTE * 1; // min of recording per hour

    public static final int SCAN_MODE_BLUETOOTH = ScanSettings.SCAN_MODE_LOW_POWER;
    public static final int RECORDER_AUDIO_CHANNELS = 2;
    public static final int SENSOR_DELAY = SensorManager.SENSOR_DELAY_FASTEST;
    public static String SERVICE_STRING = "7D2EA28A-F7BD-485A-BD9D-92AD6ECFE93E";
    public static UUID SERVICE_UUID = UUID.fromString(SERVICE_STRING);
    public static String CHARACTERISTIC_STRING = "7D2EBAAD-F7BD-485A-BD9D-92AD6ECFE93E";
    public static UUID CHARACTERISTIC_UUID = UUID.fromString(CHARACTERISTIC_STRING);

    public static String audioFileTag = "watchRecordAudio";
    public static final int RECORDER_SAMPLE_RATE = 96000;
    public static final int RECORDER_ENCODING_BIT_RATE = 200000;

    public static boolean isCentral = true;
    public static final int SYNC_BUFFER = 5 * (MINUTE/60); //To sync between central and peripheral
    public static final int threshold = 100;

    public static int[] sensorList = new int[]{
            Sensor.TYPE_HEART_RATE,
            Sensor.TYPE_ACCELEROMETER,
            Sensor.TYPE_GYROSCOPE,
            Sensor.TYPE_LIGHT
    };
    public static final String SENSOR_FILE_EXTENSION = ".csv";

    public static final int INTENT_EXPIRY = 5 * (MINUTE); // Time for the intent to expiry

}

/* The Sensors in Polar M600 are
android.sensor.accelerometer
android.sensor.linear_acceleration
android.sensor.gravity
android.sensor.gyroscope
android.sensor.significant_motion
android.sensor.step_detector
android.sensor.step_counter
android.sensor.wrist_tilt_gesture
android.sensor.light
android.sensor.heart_rate
com.polar.sensor.activity.met
com.polar.sensor.hr.ppg
com.polar.sensor.anymotion
com.polar.sensor.hr.ppg.gain
com.polar.sensor.sleep.std
com.polar.sensor.hr.247
com.polar.sensor.hr.ppInterval
android.sensor.heart_beat
com.polar.sensor.activity.acckcal
com.polar.sensor.activity.fusion
android.sensor.game_rotation_vector
 */