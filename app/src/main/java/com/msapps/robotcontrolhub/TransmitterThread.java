package com.msapps.robotcontrolhub;

import android.os.Handler;
import android.util.Log;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class TransmitterThread extends Thread {
    private static final byte[] data = new byte[6];

    private int period;
    private boolean stop = false;
    private String ipAddress;
    private int port;
    private int speed;
    private int buttons;

    private EventCallback<String> errorCallback;
    private Handler handler;
    private GameController gameController;
    private Dpad mDpad = new Dpad();
    private RelativeLayout layout;
    private boolean isDisabled;
    ToggleButton[] toggleButtons;

    public TransmitterThread(String ipAddress,
                             int port,
                             int frequency,
                             EventCallback<String> errorCallback,
                             GameController gameController,
                             int speed,
                             int buttons, RelativeLayout layout, ToggleButton[] toggleButtons) {
        this.period = 1000 / frequency;
        this.ipAddress = ipAddress;
        this.port = port;
        this.errorCallback = errorCallback;
        this.handler = new Handler();
        this.gameController = gameController;
        this.speed = speed;
        this.buttons = buttons;
        this.layout = layout;
        this.toggleButtons = toggleButtons;
    }

    @Override
    public void run() {
        layout.setOnGenericMotionListener(new View.OnGenericMotionListener() {
            @Override
            public boolean onGenericMotion(View v, MotionEvent event) {

               /* if(Dpad.isDpadDevice(event)){
                    int keyPressed = mDpad.getDirectionPressed(event);
                    keyPressed = keyPressed >=0 ? keyPressed : 5;
                    gameController.setDPadKeyPressed(Dpad.KEY_PRESS.values()[keyPressed].name());
                    return true;
                }*/
                // Check that the event came from a game controller
                if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) ==
                        InputDevice.SOURCE_JOYSTICK &&
                        event.getAction() == MotionEvent.ACTION_MOVE) {

                    // Process all historical movement samples in the batch
                    final int historySize = event.getHistorySize();

                    // Process the movements starting from the
                    // earliest historical position in the batch
                    for (int i = 0; i < historySize; i++) {
                        // Process the event at historical position i
                        gameController.processJoystickInput(event, i);
                    }
                    // Process the current movement sample in the batch (position -1)
                    gameController.processJoystickInput(event, -1);
                    return true;
                }
                return false;
            }
        });
        toggleButtons[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setRobotState(!isDisabled);
            }
        });
        OutputStream outputStream;
        InputStream inputStream;
        Socket socket;
        String returnData;
        byte[] bytes = new byte[50];
        try {
            socket = new Socket(ipAddress, port);
            outputStream = socket.getOutputStream();
            inputStream = socket.getInputStream();
        } catch (IOException e) {
            raiseError("Could not connect to robot");
            return;
        }
        while (!isInterrupted() && !stop) {
            data[0] = (!isDisabled) ? (byte) ((int) (gameController.getJoystick(GameController.Joysticks.JOYSTICK_LEFT_X, 1) * -127 + 127) & 0xFF) : 127;
            data[1] = (!isDisabled) ? (byte) ((int) (gameController.getJoystick(GameController.Joysticks.JOYSTICK_RIGHT_Y, 0) * 127 + 127) & 0xFF) : 127;
            data[2] = (!isDisabled) ? (byte) ((int) (gameController.getJoystick(GameController.Joysticks.JOYSTICK_RIGHT_X, 0) * -127 + 127) & 0xFF) : 127;
            data[3] = (!isDisabled) ? (byte) ((int) (gameController.getJoystick(GameController.Joysticks.JOYSTICK_LEFT_Y, 0) * 127 + 127) & 0xFF) : 127;
            data[4] = (byte) (speed & 0xFF);
            data[5] = (byte) (buttons & 0xFF);
            buttons = 0;
            try {
                outputStream.write(data);
                outputStream.flush();
             //   inputStream.read(bytes);
              //  returnData = new String(bytes);
                Log.d("Text",String.valueOf(inputStream.available()));

                sleep(period);
            } catch (IOException | InterruptedException e) {
                raiseError("Connection ended");
                stopTransmitting();
            }
        }
        try {
            socket.getOutputStream().close();
            socket.getInputStream().close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setButtons(int buttons) {
        this.buttons |= (byte) buttons;
    }

    private void raiseError(final String message) {
        if (!stop) {
            Log.e("Transmitter", message);
            if (handler != null && errorCallback != null) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        errorCallback.callback(message);
                    }
                });
            }
        }
    }

    public void setRobotState(boolean isEnabled) {
        isDisabled = isEnabled;
    }

    public void stopTransmitting() {
        stop = true;
        handler = null;
        interrupt();
    }
}