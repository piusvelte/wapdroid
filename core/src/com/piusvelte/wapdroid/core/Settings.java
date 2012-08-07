/*
 * Wapdroid - Android Location based Wifi Manager
 * Copyright (C) 2009 Bryan Emmanuel
 * 
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.

 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Bryan Emmanuel piusvelte@gmail.com
 */
package com.piusvelte.wapdroid.core;

import com.piusvelte.wapdroid.core.R;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private SharedPreferences mSharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getPreferenceManager().setSharedPreferencesName(getString(R.string.key_preferences));
		addPreferencesFromResource(R.xml.preferences);
		mSharedPreferences = getSharedPreferences(getString(R.string.key_preferences), MODE_PRIVATE);
	}

	@Override
	public void onPause() {
		super.onPause();
		mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();
		mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
	}

	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(getString(R.string.key_manageWifi))) {
			if (sharedPreferences.getBoolean(key, true)) {
				this.startService(new Intent(this, WapdroidService.class));
				(new AlertDialog.Builder(this)
				.setMessage(R.string.background_info)
				.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						startActivity(new Intent(android.provider.Settings.ACTION_WIFI_IP_SETTINGS));
					}
				})
				)
				.show();
			} else this.stopService(new Intent(this, WapdroidService.class));
			// update widgets
			AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
			this.sendBroadcast(new Intent(this, WapdroidWidget.class).setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE).putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetManager.getAppWidgetIds(new ComponentName(this, WapdroidWidget.class))));
		}
	}
}