package com.example.shake;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Matrix;
import android.os.Bundle;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * MainActivity to detect device orientation and shaking.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometerSensor, magnetometerSensor;
    private TextView xValueTextView, yValueTextView, zValueTextView;
    private ImageView empireImageView;

    // Variables for accelerometer data and shake detection.
    private static final float SHAKE_THRESHOLD = 400.0f;
    private long lastUpdate;
    private float last_x, last_y, last_z;
    float[] mGravity;
    float[] mGeomagnetic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeSensors();
    }

    /**
     * Initialize UI elements.
     */
    private void initializeViews() {
        xValueTextView = findViewById(R.id.xValue);
        yValueTextView = findViewById(R.id.yValue);
        zValueTextView = findViewById(R.id.zValue);
        empireImageView = findViewById(R.id.empire);
    }

    /**
     * Initialize sensors.
     */
    private void initializeSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register listeners for the sensors.
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, magnetometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the sensor listeners when the activity is paused.
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // You can handle accuracy changes here if needed.
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            detectShake(event);
        }

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            mGeomagnetic = event.values;
        }

        // Compute orientation based on sensor data and update the UI.
        computeOrientationAndUpdateUI();
    }

    /**
     * Detect shake based on accelerometer data.
     */
    private void detectShake(SensorEvent event) {
        mGravity = event.values;

        float x = mGravity[0];
        float y = mGravity[1];
        float z = mGravity[2];

        long currentTime = System.currentTimeMillis();
        if ((currentTime - lastUpdate) > 100) { // Check every 100ms
            long diffTime = (currentTime - lastUpdate);
            lastUpdate = currentTime;

            float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

            if (speed > SHAKE_THRESHOLD) {
                makeToast();
            }

            last_x = x;
            last_y = y;
            last_z = z;
        }
    }

    /**
     * Compute the device's orientation and update UI accordingly.
     */
    private void computeOrientationAndUpdateUI() {
        if (mGravity != null && mGeomagnetic != null) {
            float[] R = new float[9];
            float[] I = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
            if (success) {
                updateOrientationDisplay(R);
            }
        }
    }

    /**
     * Update the display based on the orientation matrix.
     */
    private void updateOrientationDisplay(float[] R) {
        float[] orientation = new float[3];
        SensorManager.getOrientation(R, orientation);

        float azimuth = (float) Math.toDegrees(orientation[0]); // Yaw
        float pitch = (float) Math.toDegrees(orientation[1]); // Pitch
        float roll = (float) Math.toDegrees(orientation[2]); // Roll

        Matrix matrix = new Matrix();
        empireImageView.setScaleType(ImageView.ScaleType.MATRIX);
        matrix.postRotate(-azimuth, (float) empireImageView.getDrawable().getBounds().width() / 2, (float) empireImageView.getDrawable().getBounds().height() / 2);
        empireImageView.setImageMatrix(matrix);

        int color = orientationToColor(azimuth, pitch, roll);

        RainbowFragment rainbowFragment = (RainbowFragment) getSupportFragmentManager().findFragmentById(com.example.shake.R.id.fragmentContainerView);
        if (rainbowFragment != null && rainbowFragment.getView() != null) {
            rainbowFragment.getView().setBackgroundColor(color);
        }

        xValueTextView.setText(String.format(getResources().getString(com.example.shake.R.string.format_azimuth), azimuth));
        yValueTextView.setText(String.format(getResources().getString(com.example.shake.R.string.format_pitch), pitch));
        zValueTextView.setText(String.format(getResources().getString(com.example.shake.R.string.format_roll), roll));
    }

    /**
     * Convert orientation to a color.
     */
    private int orientationToColor(float azimuth, float pitch, float roll) {
        int red = (int) ((azimuth + 180) / 360 * 255);
        int green = (int) ((pitch + 90) / 180 * 255);
        int blue = (int) ((roll + 90) / 180 * 255);

        return android.graphics.Color.rgb(red, green, blue);
    }

    /**
     * Show a toast with accelerometer sensor values when a shake is detected.
     */
    private void makeToast() {
        String message = String.format("Shake detected with accelerometer values: X=%s, Y=%s, Z=%s", last_x, last_y, last_z);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
