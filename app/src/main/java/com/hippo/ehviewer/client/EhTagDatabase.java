/*
 * Copyright 2019 Hippo Seven
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

package com.hippo.ehviewer.client;

import android.content.Context;

import androidx.annotation.Nullable;

import com.hippo.ehviewer.AppConfig;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.util.ExceptionUtils;
import com.hippo.util.IoThreadPoolExecutor;
import com.hippo.yorozuya.FileUtils;
import com.hippo.yorozuya.IOUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;
import okio.Okio;

public class EhTagDatabase {

    private final Map<String, Map<String, String>> tags;

    public EhTagDatabase(BufferedSource source) {
        Map<String, Map<String, String>> tags = new HashMap<>();
        try {
            JSONObject object = new JSONObject(source.readString(StandardCharsets.UTF_8));
            JSONArray data = object.getJSONArray("data");
            for (int i = 0; i < data.length(); i++) {
                JSONObject names = data.getJSONObject(i);
                String namespace = names.getString("namespace");
                Map<String, String> map = new HashMap<>();
                tags.put(namespace, map);
                JSONObject namesData = names.getJSONObject("data");
                for (Iterator<String> it = namesData.keys(); it.hasNext(); ) {
                    String s = it.next();
                    map.put(s, namesData.getJSONObject(s).getString("name"));
                }
            }
        } catch (Exception ignore) {
        }
        this.tags = tags;
    }

    public String getTranslation(String namespace, String tag) {
        try {
            Map<String, String> jsonObject = tags.get(namespace);
            return jsonObject == null ? tag : jsonObject.get(tag);
        } catch (Exception e) {
            return tag;
        }
    }

    private static volatile EhTagDatabase instance;
    // TODO more lock for different language
    private static final Lock lock = new ReentrantLock();

    @Nullable
    public static EhTagDatabase getInstance(Context context) {
        if (!isPossible(context)) {
            instance = null;
        }
        return instance;
    }

    private static String[] getMetadata(Context context) {
        String[] metadata = context.getResources().getStringArray(R.array.tag_translation_metadata);
        if (metadata.length == 2) {
            return metadata;
        } else {
            return null;
        }
    }

    public static boolean isPossible(Context context) {
        return getMetadata(context) != null;
    }

    private static boolean save(OkHttpClient client, String url, File file) {
        Request request = new Request.Builder().url(url).build();
        Call call = client.newCall(request);
        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                return false;
            }
            ResponseBody body = response.body();
            if (body == null) {
                return false;
            }

            try (InputStream is = body.byteStream(); OutputStream os = new FileOutputStream(file)) {
                IOUtils.copy(is, os);
                os.flush();
            }

            return true;
        } catch (Throwable t) {
            ExceptionUtils.throwIfFatal(t);
            return false;
        }
    }

    public static void load(Context context) {
        instance = null;
        String[] urls = getMetadata(context);
        if (urls == null) {
            return;
        }

        String dataName = urls[0];

        IoThreadPoolExecutor.getInstance().execute(() -> {
            if (!lock.tryLock()) {
                return;
            }

            try {
                File dir = AppConfig.getFilesDir("tag-translations");
                if (dir == null || !dir.exists() || !dir.isDirectory()) {
                    return;
                }

                File dataFile = new File(dir, dataName);

                if (!dataFile.exists() || !dataFile.isFile()) {
                    return;
                }
                // Read new EhTagDatabase
                try (BufferedSource source = Okio.buffer(Okio.source(dataFile))) {
                    instance = new EhTagDatabase(source);
                } catch (Exception e) {
                    FileUtils.delete(dataFile);
                }
            } finally {
                lock.unlock();
            }
        });
    }

    public static void update(Context context) {
        instance = null;
        String[] urls = getMetadata(context);
        if (urls == null) {
            // Clear tags if it's not possible
            return;
        }

        String dataName = urls[0];
        String dataUrl = urls[1];

        IoThreadPoolExecutor.getInstance().execute(() -> {
            if (!lock.tryLock()) {
                return;
            }

            try {
                File dir = AppConfig.getFilesDir("tag-translations");
                if (dir == null) {
                    return;
                }

                OkHttpClient client = EhApplication.getOkHttpClient(EhApplication.getInstance());

                // Save new data
                File tempDataFile = new File(dir, dataName + ".tmp");
                if (!save(client, dataUrl, tempDataFile)) {
                    FileUtils.delete(tempDataFile);
                    return;
                }

                // Replace current sha1 and current data with new sha1 and new data
                File dataFile = new File(dir, dataName);
                FileUtils.delete(dataFile);
                tempDataFile.renameTo(dataFile);

                // Read new EhTagDatabase
                try (BufferedSource source = Okio.buffer(Okio.source(dataFile))) {
                    instance = new EhTagDatabase(source);
                } catch (Exception e) {
                    FileUtils.delete(dataFile);
                }
            } finally {
                lock.unlock();
            }
        });
    }
}
