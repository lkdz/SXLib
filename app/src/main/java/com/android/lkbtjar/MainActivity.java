package com.android.lkbtjar;

import android.os.Debug;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.android.bluetooth.BluetoothService;
import com.android.comm.RX_MT_Comm;
import com.android.comm.RX_ReadData;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final RX_MT_Comm rx = RX_MT_Comm.getInstance();
        rx.setBTService(new BluetoothService());


        rx.setReadData(0x68);
        rx.setReadData(0x01);
        rx.setReadData(0x0E);
        rx.setReadData(0x18);
        rx.setReadData(0x0C);
        rx.setReadData(0x80);
        rx.setReadData(0x01);
        rx.setReadData(0x05);
        rx.setReadData(0x09);
        rx.setReadData(0x99);
        rx.setReadData(0x99);
        rx.setReadData(0x00);
        rx.setReadData(0x00);
        rx.setReadData(0x00);
        rx.setReadData(0x36);
        rx.setReadData(0x00);
        rx.setReadData(0x03);
        rx.setReadData(0x16);


        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                boolean ret = rx.readSingleMeter("2576980377", null);
                Log.i("mydebug", String.valueOf(ret));
                if (ret) {
                    RX_ReadData d = rx.recvSingleMeter();
                    Log.i("mydebug", d.getMeterNo());
                }
            }


        });
        thread.start();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
