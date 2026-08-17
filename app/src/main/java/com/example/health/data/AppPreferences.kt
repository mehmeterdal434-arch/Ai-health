package com.example.health.data

import android.content.Context
import android.content.SharedPreferences
import com.example.health.engine.UserHealthProfile
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("health_ai_preferences", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private val _profile = MutableStateFlow(loadProfile())
    val profile: StateFlow<UserHealthProfile> = _profile.asStateFlow()

    fun loadThemeMode(): AppThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return try {
            AppThemeMode.valueOf(saved ?: AppThemeMode.SYSTEM.name)
        } catch (e: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun loadProfile(): UserHealthProfile {
        return UserHealthProfile(
            stepGoal = prefs.getInt(KEY_STEP_GOAL, 10000),
            sleepBaselineHours = prefs.getFloat(KEY_SLEEP_GOAL, 8.0f).toDouble(),
            restingHeartRateBaselineBpm = prefs.getInt(KEY_HR_BASELINE, 65),
            activeCalorieGoalKcal = prefs.getInt(KEY_CALORIE_GOAL, 500),
            activeMinutesGoal = prefs.getInt(KEY_ACTIVE_MINUTES_GOAL, 30),
            waterGoalMl = prefs.getInt(KEY_WATER_GOAL, 2500)
        )
    }

    fun saveProfile(profile: UserHealthProfile) {
        prefs.edit()
            .putInt(KEY_STEP_GOAL, profile.stepGoal)
            .putFloat(KEY_SLEEP_GOAL, profile.sleepBaselineHours.toFloat())
            .putInt(KEY_HR_BASELINE, profile.restingHeartRateBaselineBpm)
            .putInt(KEY_CALORIE_GOAL, profile.activeCalorieGoalKcal)
            .putInt(KEY_ACTIVE_MINUTES_GOAL, profile.activeMinutesGoal)
            .putInt(KEY_WATER_GOAL, profile.waterGoalMl)
            .apply()
        _profile.value = profile
    }

    fun getCustomApiKey(): String {
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    fun saveCustomApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    fun clearAllPreferences() {
        prefs.edit().clear().apply()
        _themeMode.value = AppThemeMode.SYSTEM
        _profile.value = UserHealthProfile()
    }

    companion object {
        private const val KEY_THEME_MODE = "pref_theme_mode"
        private const val KEY_STEP_GOAL = "pref_step_goal"
        private const val KEY_SLEEP_GOAL = "pref_sleep_goal"
        private const val KEY_HR_BASELINE = "pref_hr_baseline"
        private const val KEY_CALORIE_GOAL = "pref_calorie_goal"
        private const val KEY_ACTIVE_MINUTES_GOAL = "pref_active_minutes_goal"
        private const val KEY_WATER_GOAL = "pref_water_goal"
        private const val KEY_API_KEY = "pref_custom_api_key"
    }
}
