package com.novoda.tpbot.bot.video.calling;

import android.content.SharedPreferences;

import com.novoda.tpbot.FeatureSelectionPersistence;

// TODO: Remove persistence, this is an implementation detail. Should be concerned with "feature" only.
public final class VideoCallSharedPreferencesPersistence implements FeatureSelectionPersistence {

    private static final String VIDEO_CALL_PREFERENCES_ON_OFF_KEY = "video_call_preferences_on_off";
    private static final boolean ON = true;
    private static final boolean OFF = false;

    private final SharedPreferences sharedPreferences;

    public VideoCallSharedPreferencesPersistence(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    @Override
    public boolean isFeatureEnabled() {
        return sharedPreferences.getBoolean(VIDEO_CALL_PREFERENCES_ON_OFF_KEY, OFF);
    }

    @Override
    public void setFeatureEnabled() {
        sharedPreferences.edit()
                .putBoolean(VIDEO_CALL_PREFERENCES_ON_OFF_KEY, ON)
                .apply();
    }

    @Override
    public void setFeatureDisabled() {
        sharedPreferences.edit()
                .putBoolean(VIDEO_CALL_PREFERENCES_ON_OFF_KEY, OFF)
                .apply();
    }

}
