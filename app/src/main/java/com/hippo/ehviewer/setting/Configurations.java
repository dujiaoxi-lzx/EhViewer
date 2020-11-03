package com.hippo.ehviewer.setting;

public interface Configurations {

    boolean contains(String key);

    boolean getBoolean(String key, boolean defValue);

    void putBoolean(String key, boolean value);

    int getInt(String key, int defValue);

    void putInt(String key, int value);

    String getString(String key, String defValue);

    void putString(String key, String value);
}
