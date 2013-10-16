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

package com.android.bitmap;

import android.content.res.AssetFileDescriptor;


import java.io.IOException;
import java.io.InputStream;

/**
 * The decode task uses this class to get input to decode. You must implement at least one of
 * {@link #createFd()} or {@link #createInputStream()}. {@link DecodeTask} will prioritize
 * {@link #createFd()} before falling back to {@link #createInputStream()}.
 * <p>
 * Objects of this type will also serve as cache keys to fetch cached data for {@link PooledCache}s,
 * so they must implement {@link #equals(Object)} and {@link #hashCode()}.
 */

public interface RequestKey {

    @Override
    public boolean equals(Object o);

    @Override
    public int hashCode();

    /**
     * Create an {@link AssetFileDescriptor} for a local file stored on the device. This method will
     * be called first; if it returns null, {@link #createInputStream()} will be called.
     */
    public AssetFileDescriptor createFd() throws IOException;

    /**
     * Create an {@link InputStream} for a file. This method will be called if {@link #createFd()}
     * returns null.
     */
    public InputStream createInputStream() throws IOException;

    /**
     * Return true if the image source may have be oriented in either portrait or landscape, and
     * will need to be automatically re-oriented based on accompanying Exif metadata.
     */
    public boolean hasOrientationExif() throws IOException;
}