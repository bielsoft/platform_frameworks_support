/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.car.util;

import android.app.Activity;
import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.uxrestrictions.CarUxRestrictions;
import androidx.car.uxrestrictions.OnUxRestrictionsChangedListener;

/**
 * Helps registering {@link OnUxRestrictionsChangedListener} and managing car connection.
 */
public class CarUxRestrictionsHelper {
    private static final String TAG = "CarUxRestrictionsHelper";

    // mCar is created in the constructor, but can be null if connection to the car is not
    // successful.
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @Nullable
    final Car mCar;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @Nullable
    CarUxRestrictionsManager mCarUxRestrictionsManager;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final OnUxRestrictionsChangedListener mListener;

    public CarUxRestrictionsHelper(@NonNull Context context,
            @NonNull OnUxRestrictionsChangedListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null.");
        }
        mListener = listener;
        mCar = Car.createCar(context, mServiceConnection);
    }

    /**
     * Starts monitoring any changes in {@link CarUxRestrictions}.
     *
     * <p>This method can be called from {@code Activity}'s {@link Activity#onStart()}, or at the
     * time of construction.
     *
     * <p>This method must be accompanied with a matching {@link #stop()} to avoid leak. After
     * {@link #start()} has been called, calling {@link #start()} subsequent times without
     * calling {@link #stop()} will result in a no-op.
     */
    public void start() {
        try {
            if (mCar != null && !mCar.isConnected()) {
                mCar.connect();
            }
        } catch (IllegalStateException e) {
            // Do nothing.
            Log.w(TAG, "start(); cannot connect to Car");
        }
    }

    /**
     * Stops monitoring any changes in {@link CarUxRestrictions}.
     *
     * <p>This method should be called from {@code Activity}'s {@link Activity#onStop()}, or at the
     * time of being discarded.
     *
     * <p>After {@link #stop()} has been called, {@link #start()} can be called again to resume
     * monitoring car ux restrictions change. Calling {@link #stop()} without calling
     * {@link #start()} will result in a no-op.
     */
    public void stop() {
        if (mCarUxRestrictionsManager != null) {
            try {
                mCarUxRestrictionsManager.unregisterListener();
            } catch (CarNotConnectedException e) {
                // Do nothing.
                Log.w(TAG, "stop(); cannot unregister listener.");
            }
        }
        try {
            if (mCar != null && mCar.isConnected()) {
                mCar.disconnect();
            }
        } catch (IllegalStateException e) {
            // Do nothing.
            Log.w(TAG, "stop(); cannot disconnect from Car.");
        }
    }

    /**
     * Gets the current UX restrictions {@link CarUxRestrictions} in place.
     *
     * @return current UX restrictions that is in effect. If the current UX restrictions cannot
     * be obtained, the default of no active restrictions is returned.
     */
    @NonNull
    public CarUxRestrictions getCurrentCarUxRestrictions() {
        try {
            if (mCarUxRestrictionsManager != null) {
                return new CarUxRestrictions(
                        mCarUxRestrictionsManager.getCurrentCarUxRestrictions());
            }
        } catch (CarNotConnectedException e) {
            // Do nothing.
            Log.w(TAG, "getCurrentCarUxRestrictions(); cannot get current UX restrictions.");
        }

        return new CarUxRestrictions.Builder(false,
                CarUxRestrictions.UX_RESTRICTIONS_BASELINE, SystemClock.elapsedRealtimeNanos())
                .build();
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                mCarUxRestrictionsManager = (CarUxRestrictionsManager)
                        mCar.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE);
                // Convert framework UX restrictions to androidx type.
                mCarUxRestrictionsManager.registerListener(restrictions ->
                        mListener.onUxRestrictionsChanged(new CarUxRestrictions(restrictions)));

                mListener.onUxRestrictionsChanged(new CarUxRestrictions(
                        mCarUxRestrictionsManager.getCurrentCarUxRestrictions()));
            } catch (CarNotConnectedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCarUxRestrictionsManager = null;
        }
    };
}
