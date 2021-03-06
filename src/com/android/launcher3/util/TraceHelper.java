/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.launcher3.util;

import static android.util.Log.VERBOSE;
import static android.util.Log.isLoggable;

import android.os.SystemClock;
import android.os.Trace;
import android.util.ArrayMap;
import android.util.MutableLong;

import com.sprd.ext.LogUtils;

/**
 * A wrapper around {@link Trace} with some utility information.
 *
 * To enable any tracing log, execute the following command:
 * $ adb shell setprop log.tag.LAUNCHER_TRACE VERBOSE
 * $ adb shell setprop log.tag.TAGNAME VERBOSE
 */
public class TraceHelper {

    private static final boolean ENABLED = isLoggable("LAUNCHER_TRACE", VERBOSE) ||
            LogUtils.DEBUG || LogUtils.DEBUG_PERFORMANCE;

    private static final boolean SYSTEM_TRACE = ENABLED;
    private static final ArrayMap<String, MutableLong> sUpTimes = ENABLED ? new ArrayMap<>() : null;

    public static void beginSection(String sectionName) {
        if (ENABLED) {
            synchronized (sUpTimes) {
                MutableLong time = sUpTimes.get(sectionName);
                if (time == null) {
                    time = new MutableLong((isLoggable(sectionName, VERBOSE)
                            || LogUtils.DEBUG || LogUtils.DEBUG_PERFORMANCE) ? 0 : -1);
                    sUpTimes.put(sectionName, time);
                }
                if (time.value >= 0) {
                    if (SYSTEM_TRACE) {
                        Trace.beginSection(sectionName);
                    }
                    time.value = SystemClock.uptimeMillis();
                }
            }
        }
    }

    public static void partitionSection(String sectionName, String partition) {
        if (ENABLED) {
            synchronized (sUpTimes) {
                MutableLong time = sUpTimes.get(sectionName);
                if (time != null && time.value >= 0) {

                    if (SYSTEM_TRACE) {
                        Trace.endSection();
                        Trace.beginSection(sectionName);
                    }

                    long now = SystemClock.uptimeMillis();
                    LogUtils.d(sectionName, partition + " : " + (now - time.value));
                    time.value = now;
                }
            }
        }
    }

    public static void endSection(String sectionName) {
        if (ENABLED) {
            endSection(sectionName, "End");
        }
    }

    public static void endSection(String sectionName, String msg) {
        if (ENABLED) {
            synchronized (sUpTimes) {
                MutableLong time = sUpTimes.get(sectionName);
                if (time != null && time.value >= 0) {
                    if (SYSTEM_TRACE) {
                        Trace.endSection();
                    }
                    LogUtils.d(sectionName, msg + " : " + (SystemClock.uptimeMillis() - time.value));
                }
            }
        }
    }
}
