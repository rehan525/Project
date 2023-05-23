package com.example.newproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.BreakIterator;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;

    private SeekBar mSeekBar;
    private Button mConnectButton;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;
    private OutputStream mOutputStream;
    private boolean mConnected = false;
    private TextView mDataTextView;

    private TextView textView;

    private Button mResetButton;

    private EditText editText;
    private Button startButton;
    private String data;

    private int intValue;

    private String result1;
    private String result2="0";

    int value;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSeekBar = findViewById(R.id.seekBar);
        mConnectButton = findViewById(R.id.connectButton);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Spinner speedSpinner = findViewById(R.id.speed_spinner);
        mDataTextView=findViewById(R.id.dataTextView);
        mResetButton = findViewById(R.id.resetButton);
        editText = findViewById(R.id.editText);
        startButton = findViewById(R.id.startButton);
        TextView textView = findViewById(R.id.textView);

        requestBluetoothPermissions();

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mConnected) {
                    String data = String.valueOf(progress) + "\n";
                    textView.setText(data);
                    try {
                        mOutputStream.write(data.getBytes()); // write the data to the output stream
                    } catch (IOException e) {
                        Toast.makeText(getApplicationContext(), "Error sending data", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        speedSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Get the selected speed option
                String selectedOption = parent.getItemAtPosition(position).toString();
                // Parse the selected speed value from the option string
                int speedValue = Integer.parseInt(selectedOption.split(" ")[0]);
                // Set the seekbar progress to the selected speed value
                mSeekBar.setProgress(speedValue);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mConnected) {
                    connectBluetooth();
                } else {
                    disconnectBluetooth();
                }
            }
        });
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSeekBar.setProgress(25);
                String start="t";
                sendBluetoothMessage(start);
            }
        });

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    editText.clearFocus();
                    // Hide the keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);

                    // Get the text from the EditText
                    String value = editText.getText().toString();
                    result2=value;

                    // Copy the value to a variable
                    intValue = Integer.parseInt(value);

                    return true;
                }
                return false;
            }
        });

    }

    private void requestBluetoothPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, REQUEST_BLUETOOTH_PERMISSIONS);
        } else {
            enableBluetooth();
        }
    }

    @SuppressLint("MissingPermission")
    private void enableBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            connectBluetooth();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == RESULT_OK) {
                connectBluetooth();
            } else {
                Toast.makeText(this, "Bluetooth activation canceled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void receiveBluetoothData() {
        final InputStream inputStream;
        try {
            inputStream = mBluetoothSocket.getInputStream();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Error getting input stream", Toast.LENGTH_SHORT).show();
            return;
        }

        Thread receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                StringBuilder receivedData = new StringBuilder(); // StringBuilder to accumulate received data

                while (mConnected) {
                    try {
                        byte[] buffer = new byte[1024];
                        int bytes = inputStream.read(buffer);

                        if (bytes > 0) {
                            String data = new String(buffer, 0, bytes);
                            receivedData.append(data);

                            while (receivedData.toString().contains("\n")) {
                                int newlineIndex = receivedData.indexOf("\n");
                                String valueString = receivedData.substring(0, newlineIndex).trim();
                                receivedData.delete(0, newlineIndex + 1);

                                try {
                                    value = Integer.parseInt(valueString);

                                    // Update the TextView with the individual value
                                    if(value>intValue){
                                        mSeekBar.setProgress(0);
                                        String stop="s";
                                        sendBluetoothMessage(stop);
                                        break;
                                    }
                                    final String finalValue = String.valueOf(value); // Declare final variable for access in the inner class
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            mDataTextView.setText(finalValue + "\n");
                                        }
                                    });
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            }
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        });

        receiveThread.start();
    }



    public void onForwardButtonClick(View view) {
        // Send a "forward" command to the Arduino over Bluetooth
        String message = "f";
        sendBluetoothMessage(message);
    }

    public void onBackwardButtonClick(View view) {
        // Send a "backward" command to the Arduino over Bluetooth
        String message = "b";
        sendBluetoothMessage(message);
        Log.d("TAG", "intValue: " + intValue);
        Log.d("TAG", "data : "+value);
    }

    public void onResetButtonClick(View view) {
        if (mConnected) {
            // Send a reset command to the Arduino over Bluetooth
            String message = "r";
            sendBluetoothMessage(message);
            String start="t";
            sendBluetoothMessage(start);
            // Reset the received data TextView
            mDataTextView.setText("0");
        } else {
            Toast.makeText(getApplicationContext(), "Not connected to Bluetooth", Toast.LENGTH_SHORT).show();
        }
    }


    private void sendBluetoothMessage(String message) {
        if (mOutputStream != null) {
            try {
                mOutputStream.write(message.getBytes());
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Error sending data", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @SuppressLint("MissingPermission")
    private void connectBluetooth() {
        if (!mBluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice("00:18:91:D7:5B:5C"); // Replace with your HC-05 MAC address
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard SerialPortService ID
            mBluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(uuid);
            mBluetoothSocket.connect();
            mOutputStream = mBluetoothSocket.getOutputStream();
            mConnected = true;
            mConnectButton.setText("Disconnect From Winding Machine");
            receiveBluetoothData();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Connection failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void disconnectBluetooth() {
        if (mConnected) {
            try {
                mBluetoothSocket.close();
                mConnected = false;
                mConnectButton.setText("Connect");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}