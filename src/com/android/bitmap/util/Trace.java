/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.bitmap.util;

import java.lang.reflect.Method;

public class Trace {

    private static Method sBegin;
    private static Method sEnd;

    public static void init() {
        if (sBegin != null && sEnd != null) {
            return;
        }
        try {
            final Class<?> cls = Class.forName("android.os.Trace");
            sBegin = cls.getMethod("beginSection", String.class);
            sEnd = cls.getMethod("endSection");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void beginSection(String tag) {
        if (sBegin == null) {
            return;
        }
        try {
            sBegin.invoke(null, tag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void endSection() {
        if (sEnd == null) {
            return;
        }
        try {
            sEnd.invoke(null, (Object[]) null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
