package it.drone.mesh.advertiser;

import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.Toast;

import it.drone.mesh.R;
import it.drone.mesh.tasks.AcceptBLETask;

/**
 * Allows user to start & stop Bluetooth LE Advertising of their device.
 */
public class AdvertiserFragment extends Fragment implements View.OnClickListener {

    /**
     * Lets user toggle BLE Advertising.
     */
    private static final String TAG = AdvertiserFragment.class.getSimpleName();
    private Switch mSwitch;

    private BluetoothManager mBluetoothManager;

    /**
     * Listens for notifications that the {@code AdvertiserService} has failed to start advertising.
     * This Receiver deals with Fragment UI elements and only needs to be active when the Fragment
     * is on-screen, so it's defined and registered in code instead of the Manifest.
     */
    private BroadcastReceiver advertisingFailureReceiver;

    /**
     * Returns Intent addressed to the {@code AdvertiserService} class.
     */
    private static Intent getServiceIntent(Context c) {
        return new Intent(c, AdvertiserService.class);
    }

    public void setBluetoothManager(BluetoothManager btManager) {
        this.mBluetoothManager = btManager;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        advertisingFailureReceiver = new BroadcastReceiver() {

            /**
             * Receives Advertising error codes from {@code AdvertiserService} and displays error messages
             * to the user. Sets the advertising toggle to 'false.'
             */
            @Override
            public void onReceive(Context context, Intent intent) {

                int errorCode = intent.getIntExtra(AdvertiserService.ADVERTISING_FAILED_EXTRA_CODE, -1);

                mSwitch.setChecked(false);

                String errorMessage = getString(R.string.start_error_prefix);
                switch (errorCode) {
                    case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                        errorMessage += " " + getString(R.string.start_error_already_started);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                        errorMessage += " " + getString(R.string.start_error_too_large);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                        errorMessage += " " + getString(R.string.start_error_unsupported);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                        errorMessage += " " + getString(R.string.start_error_internal);
                        break;
                    case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                        errorMessage += " " + getString(R.string.start_error_too_many);
                        break;
                    case AdvertiserService.ADVERTISING_TIMED_OUT:
                        errorMessage = " " + getString(R.string.advertising_timedout);
                        break;
                    default:
                        errorMessage += " " + getString(R.string.start_error_unknown);
                }

                Toast.makeText(getActivity(), errorMessage, Toast.LENGTH_LONG).show();
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_advertiser, container, false);

        mSwitch = view.findViewById(R.id.advertise_switch);
        mSwitch.setOnClickListener(this);

        return view;
    }

    /**
     * When app comes on screen, check if BLE Advertisements are running, set switch accordingly,
     * and register the Receiver to be notified if Advertising fails.
     */
    @Override
    public void onResume() {
        super.onResume();

        if (AdvertiserService.running) {
            mSwitch.setChecked(true);
        } else {
            mSwitch.setChecked(false);
        }

        IntentFilter failureFilter = new IntentFilter(AdvertiserService.ADVERTISING_FAILED);
        getActivity().registerReceiver(advertisingFailureReceiver, failureFilter);

    }

    /**
     * When app goes off screen, unregister the Advertising failure Receiver to stop memory leaks.
     * (and because the app doesn't care if Advertising fails while the UI isn't active)
     */
    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(advertisingFailureReceiver);
    }

    /**
     * Called when switch is toggled - starts or stops advertising.
     */
    @Override
    public void onClick(View v) {
        // Is the toggle on?
        boolean on = ((Switch) v).isChecked();

        if (on) {
            startAdvertising();
        } else {
            stopAdvertising();
        }
    }

    /**
     * Starts BLE Advertising by starting {@code AdvertiserService}.
     */
    private void startAdvertising() {
        Context c = getActivity();
        c.startService(getServiceIntent(c));
        Log.d(TAG, "OUD: " + "startAdvertising: StART Server");
        AcceptBLETask acceptBLETask = new AcceptBLETask(null, mBluetoothManager, getContext());
        acceptBLETask.startServer();
    }

    /**
     * Stops BLE Advertising by stopping {@code AdvertiserService}.
     */
    private void stopAdvertising() {
        Context c = getActivity();
        c.stopService(getServiceIntent(c));
        mSwitch.setChecked(false);
    }

}