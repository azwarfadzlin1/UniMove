package com.example.unimove.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "onboarding_prefs")

class OnboardingPreferences(private val context: Context) {
    companion object {
        val COMPLETED_KEY = booleanPreferencesKey("COMPLETED")
        val USER_NAME_KEY = stringPreferencesKey("USER_NAME")
        val AVATAR_KEY = stringPreferencesKey("AVATAR_KEY")
    }

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[COMPLETED_KEY] ?: false
    }

    val userName: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[USER_NAME_KEY]
    }

    val avatarKey: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[AVATAR_KEY]
    }

    suspend fun saveBasicProfile(name: String, avatar: String) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME_KEY] = name
            preferences[AVATAR_KEY] = avatar
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[COMPLETED_KEY] = completed
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
