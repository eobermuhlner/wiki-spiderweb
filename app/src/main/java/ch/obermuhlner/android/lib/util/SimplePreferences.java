package ch.obermuhlner.android.lib.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class SimplePreferences {

	private final Context context;
	private final SharedPreferences sharedPreferences;

	public SimplePreferences(Context context) {
		this(context, PreferenceManager.getDefaultSharedPreferences(context));
	}

	public SimplePreferences(Context context, SharedPreferences sharedPreferences) {
		this.context = context;
		this.sharedPreferences = sharedPreferences;
	}
	
	public boolean getBoolean(int stringId) {
		return getBoolean(stringId, false);
	}
	
	public boolean getBoolean(int stringId, boolean defaultValue) {
		String key = context.getResources().getString(stringId);
		return sharedPreferences.getBoolean(key, defaultValue);
	}
	
	public int getInteger(int stringId) {
		return getInteger(stringId, 0);
	}
	
	public int getInteger(int stringId, int defaultValue) {
		String key = context.getResources().getString(stringId);
		return sharedPreferences.getInt(key, defaultValue);
	}
	
	public String getString(int stringId) {
		return getString(stringId, null);
	}
	
	public String getString(int stringId, String defaultValue) {
		String key = context.getResources().getString(stringId);
		return sharedPreferences.getString(key, defaultValue);
	}

	public float getFloat(int stringId) {
		return getFloat(stringId, 0);
	}
	
	public float getFloat(int stringId, float defaultValue) {
		String key = context.getResources().getString(stringId);
		return sharedPreferences.getFloat(key, defaultValue);
	}
	
	public void setString(int stringId, String value) {
		String key = context.getResources().getString(stringId);
		Editor edit = sharedPreferences.edit();
		edit.putString(key, value);
		edit.commit();
	}
}
