package com.example.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.worldlyDataStore by preferencesDataStore(name = "worldly_preferences")

class OnboardingPreferences(private val context: Context) {
    private val onboardingDoneKey = booleanPreferencesKey("onboarding_completed")

    val onboardingCompleted: Flow<Boolean> = context.worldlyDataStore.data.map { prefs ->
        prefs[onboardingDoneKey] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.worldlyDataStore.edit { prefs ->
            prefs[onboardingDoneKey] = completed
        }
    }
}
