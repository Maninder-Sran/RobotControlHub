package com.msapps.robotcontrolhub;

import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity implements EventCallback<String> {
    private TransmitterThread transmitterThread;
    private GameController gameController;
    private final int REQUEST_ENABLE_BT = 2;
    private BluetoothAdapter mBluetoothAdapter;
    private ToggleButton[] toggleButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toggleButtons = new ToggleButton[]{(ToggleButton)findViewById(R.id.enable_button),
        (ToggleButton)findViewById(R.id.controller_data_button)};

        gameController  = new GameController(MainActivity.this,this);

    }

    @Override
    protected void onResume() {
        checkBluetooth();
        beginConnection();
        super.onResume();
    }

    private void beginConnection() {

        TextView textView = (TextView)findViewById(R.id.controller_status);
        textView.setText(String.valueOf(gameController.isConnected()));

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        int speed = (int) (sp.getInt("speed", 1) * 2.55);
        int frequency = sp.getInt("frequency", 1);
        int port = Integer.valueOf(sp.getString("port", "0"));
        String ipAddress = sp.getString("ipAddress", "");

        transmitterThread = new TransmitterThread(ipAddress, port, frequency, this, gameController, speed, 0,(RelativeLayout)findViewById(R.id.main_layout),toggleButtons);
        transmitterThread.start();
    }

    private void checkBluetooth() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }


    @Override
    protected void onPause() {
        transmitterThread.stopTransmitting();
        transmitterThread = null;
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_activity_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings_menu_button:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivity(i);
                return true;
            case R.id.action_1_menu_button:
                transmitterThread.setButtons(0b00000001);
                return true;
            case R.id.action_2_menu_button:
                transmitterThread.setButtons(0b00000010);
                return true;
            case R.id.action_3_menu_button:
                transmitterThread.setButtons(0b00000100);
                return true;
            case R.id.action_4_menu_button:
                transmitterThread.setButtons(0b00001000);
                return true;
            default:
                return false;
        }
    }

    @Override
    public void callback(String s) {
        new AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage(s)
                .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(MainActivity.this, SettingsActivity.class);
                        startActivity(i);
                    }
                })
                .setNegativeButton("Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        beginConnection();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        beginConnection();
                    }
                })
                .create()
                .show();
    }
}