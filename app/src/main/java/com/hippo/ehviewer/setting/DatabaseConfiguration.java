package com.hippo.ehviewer.setting;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.hippo.ehviewer.EhDB;
import com.hippo.ehviewer.dao.Configuration;

import java.util.Map;

public class DatabaseConfiguration implements Configurations {

    private static DatabaseConfiguration INSTANCE = null;
    private static final Object lock = new Object();

    private DatabaseConfiguration() {
    }

    public static DatabaseConfiguration getInstance() {
        if (INSTANCE == null) {
            synchronized (lock) {
                if (INSTANCE == null) {
                    INSTANCE = new DatabaseConfiguration();
                }
            }
        }
        return INSTANCE;
    }

    @Override
    public boolean contains(String key) {
        return EhDB.getConfiguration(key) != null;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Configuration configuration = EhDB.getConfiguration(key);
        if (configuration == null) {
            putBoolean(key, defValue);
            return defValue;
        }
        if ("true".equalsIgnoreCase(configuration.value)) {
            return true;
        } else if ("false".equalsIgnoreCase(configuration.value)) {
            return false;
        } else {
            putBoolean(key, defValue);
            return defValue;
        }
    }

    @Override
    public void putBoolean(String key, boolean value) {
        EhDB.putConfiguration(key, "" + value);
    }

    @Override
    public int getInt(String key, int defValue) {
        Configuration configuration = EhDB.getConfiguration(key);
        if (configuration == null) {
            putInt(key, defValue);
            return defValue;
        }
        try {
            return Integer.parseInt(configuration.value);
        } catch (Exception ignore) {
            putInt(key, defValue);
            return defValue;
        }
    }

    @Override
    public void putInt(String key, int value) {
        EhDB.putConfiguration(key, "" + value);
    }

    @Override
    public String getString(String key, String defValue) {
        Configuration configuration = EhDB.getConfiguration(key);
        if (configuration == null) {
            putString(key, defValue);
            return defValue;
        }
        return configuration.value;
    }

    @Override
    public void putString(String key, String value) {
        EhDB.putConfiguration(key, value);
    }

}
