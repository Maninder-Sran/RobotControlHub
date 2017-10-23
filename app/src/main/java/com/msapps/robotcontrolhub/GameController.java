package com.msapps.robotcontrolhub;

import android.app.Activity;
import android.content.Context;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;

public class GameController extends View {

    final static int UP = 0;
    final static int LEFT = 1;
    final static int RIGHT = 2;
    final static int DOWN = 3;
    final static int CENTER = 4;
    int directionPressed = -1; // initialized to -1
    private double joystick_left_x = 0;
    private double joystick_left_y = 0;
    private double joystick_right_x = 0;
    private double joystick_right_y = 0;
    private Context context;
    private MainActivity mainActivity;
    private ToggleButton controller_data_button;
    private TextView left_x;
    private TextView left_y;
    private TextView right_x;
    private TextView right_y;
    private TextView dpad_key;
    private String dpad_value = "---";
    public GameController(Context context, MainActivity mainActivity) {
        super(context);
        this.context = context;
        this.mainActivity = mainActivity;

        this.controller_data_button = (ToggleButton)((Activity)context).findViewById(R.id.controller_data_button);

        this.left_x = (TextView)((Activity)context).findViewById(R.id.joystick_left_x);
        this.left_y = (TextView)((Activity)context).findViewById(R.id.joystick_left_y);
        this.right_x = (TextView)((Activity)context).findViewById(R.id.joystick_right_x);
        this.right_y = (TextView)((Activity)context).findViewById(R.id.joystick_right_y);
        this.dpad_key = (TextView)((Activity)context).findViewById(R.id.dpad_key_value);
    }

    private static float getCenteredAxis(MotionEvent event,
                                         InputDevice device, int axis, int historyPos) {
        final InputDevice.MotionRange range =
                device.getMotionRange(axis, event.getSource());

        // A joystick at rest does not always report an absolute position of
        // (0,0). Use the getFlat() method to determine the range of values
        // bounding the joystick axis center.
        if (range != null) {
            final float flat = range.getFlat();
            final float value =
                    historyPos < 0 ? event.getAxisValue(axis) :
                            event.getHistoricalAxisValue(axis, historyPos);

            // Ignore axis values that are within the 'flat' region of the
            // joystick axis center.
            if (Math.abs(value) > flat) {
                return value;
            }
        }
        return 0;
    }

    public boolean isConnected() {
        return (getGameControllerIds().size() == 1);
    }

    public double getJoystick(Joysticks joystick, final int iteration) {

        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (iteration == 1) {
                    if (!controller_data_button.isChecked()) {

                        left_x.setVisibility(VISIBLE);
                        left_x.setText("Joystick Left X = " + ((int) ((joystick_left_x * -127) + 127)));

                        left_y.setVisibility(VISIBLE);
                        left_y.setText("Joystick Left Y = " + ((int) ((joystick_left_y * -127) + 127)));

                        right_x.setVisibility(VISIBLE);
                        right_x.setText("Joystick Right X = " + ((int) ((joystick_right_x * -127) + 127)));

                        right_y.setVisibility(VISIBLE);
                        right_y.setText("Joystick Right Y = " + ((int) ((joystick_right_y * -127) + 127)));

                        dpad_key.setVisibility(VISIBLE);
                        dpad_key.setText("DPad Key Pressed = " + dpad_value);
                    } else {
                        left_x.setVisibility(GONE);
                        left_y.setVisibility(GONE);
                        right_x.setVisibility(GONE);
                        right_y.setVisibility(GONE);
                        dpad_key.setVisibility(GONE);
                    }
                }
            }
        });
        switch (joystick) {
            case JOYSTICK_LEFT_X:
                return joystick_left_x;
            case JOYSTICK_LEFT_Y:
                return joystick_left_y;
            case JOYSTICK_RIGHT_X:
                return joystick_right_x;
            case JOYSTICK_RIGHT_Y:
                return joystick_right_y;
        }
        return 0;
    }

    private ArrayList getGameControllerIds() {
        ArrayList gameControllerDeviceIds = new ArrayList();
        int[] deviceIds = InputDevice.getDeviceIds();
        for (int deviceId : deviceIds) {
            InputDevice dev = InputDevice.getDevice(deviceId);
            int sources = dev.getSources();

            // Verify that the device has gamepad buttons, control sticks, or both.
            if (((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD)
                    || ((sources & InputDevice.SOURCE_JOYSTICK)
                    == InputDevice.SOURCE_JOYSTICK)) {
                // This device is a game controller. Store its device ID.
                if (!gameControllerDeviceIds.contains(deviceId)) {
                    gameControllerDeviceIds.add(deviceId);
                }
            }
        }
        return gameControllerDeviceIds;
    }

    public void processJoystickInput(MotionEvent event,
                                      int historyPos) {

        InputDevice mInputDevice = event.getDevice();

        joystick_left_x = getCenteredAxis(event,mInputDevice,MotionEvent.AXIS_X,historyPos);
        joystick_left_y = getCenteredAxis(event,mInputDevice,MotionEvent.AXIS_Y,historyPos);
        joystick_right_x = getCenteredAxis(event,mInputDevice,MotionEvent.AXIS_Z,historyPos);
        joystick_right_y = getCenteredAxis(event,mInputDevice,MotionEvent.AXIS_RZ,historyPos);
    }
    public void setDPadKeyPressed(String keyPressed){
        dpad_value = keyPressed;
    }

public enum Joysticks {JOYSTICK_LEFT_X, JOYSTICK_LEFT_Y, JOYSTICK_RIGHT_X, JOYSTICK_RIGHT_Y}
}
