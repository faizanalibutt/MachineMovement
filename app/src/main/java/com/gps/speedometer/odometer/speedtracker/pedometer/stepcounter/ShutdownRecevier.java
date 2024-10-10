/*
 * Copyright 2013 Thomas Hoffmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.API26Wrapper;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.AppUtils;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.Logger;
import com.gps.speedometer.odometer.speedtracker.pedometer.stepcounter.util.Util;

public class ShutdownRecevier extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (BuildConfig.DEBUG) Logger.log("shutting down");

        String service = AppUtils.INSTANCE.getDefaultPreferences
                (context).getString("pedo_state", "start");
        if (service != null && service.equals("stop"))
            if (Build.VERSION.SDK_INT >= 26) {
                API26Wrapper.startForegroundService(context,
                        new Intent(context, SensorListener.class));
            } else {
                context.startService(new Intent(context, SensorListener.class));
            }

        // if the user used a root script for shutdown, the DEVICE_SHUTDOWN
        // broadcast might not be send. Therefore, the app will check this
        // setting on the next boot and displays an error message if it's not
        // set to true
        context.getSharedPreferences("pedometer", Context.MODE_PRIVATE).edit()
                .putBoolean("correctShutdown", true).apply();

        Database db = Database.getInstance(context);
        // if it's already a new day, add the temp. steps to the last one
        if (db.getSteps(Util.getToday()) == Integer.MIN_VALUE) {
            int steps = db.getCurrentSteps();
            db.insertNewDay(Util.getToday(), steps);
        } else {
            db.addToLastEntry(db.getCurrentSteps());
        }
        // current steps will be reset on boot @see BootReceiver
        db.close();
    }

}
