package it.jaschke.alexandria;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;

/**
 * Created by saj on 27/01/15.
 */
public class SettingsActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener {

    private static final String LOG_TAG = SettingsActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        ListPreference startScreenPreference = (ListPreference) findPreference(getString(R.string.pref_start_fragment_key));
        startScreenPreference.setOnPreferenceChangeListener(this);
        startScreenPreference.setSummary(startScreenPreference.getEntry());
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String newValueStr = newValue.toString();
        if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int newValueIndex = listPreference.findIndexOfValue(newValueStr);
            // The state of the preference is updated AFTER onPreferenceChange, so we can't use the convenient getEntry()
            listPreference.setSummary(listPreference.getEntries()[newValueIndex]);
        }
        else {
            preference.setSummary(newValue.toString());
        }
        return true;
    }
}
