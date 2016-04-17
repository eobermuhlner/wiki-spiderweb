package ch.obermuhlner.android.wikibrowser;

import android.os.Bundle;
import android.preference.PreferenceActivity;

import ch.obermuhlner.android.wikibrowser.trial.R;

public class ApplicationPreferencesActivity extends PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		addPreferencesFromResource(R.xml.preferences);
	}
}
