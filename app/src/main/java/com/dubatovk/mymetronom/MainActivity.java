package com.dubatovk.mymetronom;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.dubatovk.mymetronom.R;
import com.dubatovk.mymetronom.ServiceCallbacks;

import static com.dubatovk.mymetronom.R.drawable.shape_oval_green;
import static com.dubatovk.mymetronom.R.drawable.shape_oval_white;


public class MainActivity extends AppCompatActivity implements ServiceCallbacks {

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int DEFAULT_PROGRESS_VALUE = 100;

    private MyService service;
    private ServiceConnection sConn;
    private boolean bound = false;

    private ToggleButton start;
    SeekBar seekBar;

    private TextView enterNumber;
    private EditText input;
    private int bpmValue;
    private boolean isVibration = true;
    private boolean isTone = true;
    private boolean isGreenColor;
    private SharedPreferences prefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        isVibration = prefs.getBoolean("isVibration", true);
        isTone = prefs.getBoolean("isTone", true);
        bpmValue = prefs.getInt("getValue", DEFAULT_PROGRESS_VALUE);

        setUpEnterNumber();
        setUpSeekBar();
        setUpVibration();
        setUpSound();
        setUpServiceConnection();
        setUpStart();
    }


    private void setUpVibration() {
        ToggleButton vibration = (ToggleButton) findViewById(R.id.vibration_btn);
        vibration.setChecked(isVibration);

        vibration.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isVibration = isChecked;
                if (bound) {
                    service.setIsVibration(isVibration);
                }
                prefs.edit().putBoolean("isVibration", isVibration).apply();
            }
        });
    }

    private void setUpSound() {
        ToggleButton sound = (ToggleButton) findViewById(R.id.sound_btn);
        sound.setChecked(isTone);

        sound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isTone = isChecked;
                if (bound) {
                    service.setIsTone(isTone);
                }
                prefs.edit().putBoolean("isTone", isTone).apply();
            }
        });
    }

    private void setUpEnterNumber() {
        enterNumber = (TextView) findViewById(R.id.enter_number);
        enterNumber.setText(String.valueOf(bpmValue));

        enterNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
                View promptView = layoutInflater.inflate(R.layout.prompts, null);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);

                alertDialogBuilder.setView(promptView);
                input = (EditText) promptView.findViewById(R.id.userInput);

                alertDialogBuilder
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Editable inputText = input.getText();
                                bpmValue = Integer.parseInt(String.valueOf(inputText));

                                if (bpmValue >= 30 && bpmValue <= 220) {
                                    enterNumber.setText(inputText);
                                    seekBar.setProgress(bpmValue);

                                    if (bound) {
                                        service.setInterval(bpmValue);
                                    }
                                } else {

                                    int duration = Toast.LENGTH_LONG;
                                    Toast toast = Toast.makeText(getApplicationContext(),
                                            R.string.incorrect_input,
                                            duration);
                                    toast.setGravity(Gravity.CENTER_HORIZONTAL, 0, 0);
                                    toast.show();
                                }
                                prefs.edit().putInt("getValue", bpmValue).apply();
                            }
                        })
                        .setNegativeButton(R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                alertDialogBuilder.create().show();
            }
        });
    }

    private void setUpSeekBar() {
        seekBar = (SeekBar) findViewById(R.id.seek_bar);
        seekBar.setProgress(bpmValue);

        seekBar.setOnSeekBarChangeListener(new MyOnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                final int minValue = 30;
                bpmValue = minValue + seekBar.getProgress();
                enterNumber.setText(String.valueOf(bpmValue));

                if (bound) {
                    service.setInterval(bpmValue);
                }
                prefs.edit().putInt("getValue", bpmValue).apply();
            }
        });
    }

    private void setUpServiceConnection() {
        sConn = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) {
                Log.d(LOG_TAG, "onServiceConnected");
                service = ((MyService.MyBinder) binder).getService();
                bound = true;
                service.setCallbacks(MainActivity.this);
            }

            public void onServiceDisconnected(ComponentName name) {
                Log.d(LOG_TAG, "onServiceDisconnected");
                bound = false;
            }
        };
    }

    private void setUpStart() {
        start = (ToggleButton) findViewById(R.id.start);
        start.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Intent intent = new Intent(MainActivity.this, MyService.class);

                if (isChecked) {
                    bound = false;
                    stopService(intent);
                    start.setText(R.string.start);
                } else {
                    intent.putExtra(MyService.ARGS_GET_VALUE, bpmValue);
                    intent.putExtra(MyService.ARGS_IS_VIBRATION, isVibration);
                    intent.putExtra(MyService.ARGS_IS_TONE, isTone);

                    bindService(intent, sConn, 0);
                    startService(intent);
                    start.setText(R.string.stop);
                }
            }
        });
    }

    @Override
    public void changeColorIndicator() {

        ImageView imageView = (ImageView) findViewById(R.id.indicator);

        if (isGreenColor) {
            imageView.setBackgroundResource(shape_oval_white);
        } else {
            imageView.setBackgroundResource(shape_oval_green);
        }
        isGreenColor = !isGreenColor;
    }
}
