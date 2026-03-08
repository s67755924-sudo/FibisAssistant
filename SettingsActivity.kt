package com.sabrina.fibis

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreferenceCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Настройки"
            setBackgroundDrawable(null)
        }

        if (savedInstanceState == null) {
            // Создаем фрагмент
            val fragment = MySettingsFragment()

            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, fragment)
                .commit()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

// Создаем отдельный класс фрагмента
class MySettingsFragment : PreferenceFragmentCompat() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Загружаем настройки из XML
        setPreferencesFromResource(R.xml.settings_preferences, rootKey)

        // Инициализируем SharedPreferences
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        setupPreferences()
    }

    private fun setupPreferences() {
        // Голосовая активация
        val voiceActivationPref = findPreference<SwitchPreferenceCompat>("voice_activation")
        voiceActivationPref?.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = newValue as Boolean
            if (isEnabled) {
                Toast.makeText(requireContext(), "Фоновая активация включена", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Фоновая активация отключена", Toast.LENGTH_SHORT).show()
            }
            true
        }

        // Озвучка ответов
        val ttsEnabledPref = findPreference<SwitchPreferenceCompat>("tts_enabled")
        ttsEnabledPref?.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = newValue as Boolean
            Toast.makeText(
                requireContext(),
                if (isEnabled) "Озвучка включена" else "Озвучка отключена",
                Toast.LENGTH_SHORT
            ).show()
            true
        }

        // Анимации
        val animationsEnabledPref = findPreference<SwitchPreferenceCompat>("animations_enabled")
        animationsEnabledPref?.setOnPreferenceChangeListener { _, newValue ->
            val isEnabled = newValue as Boolean
            Toast.makeText(
                requireContext(),
                if (isEnabled) "Анимации включены" else "Анимации отключены",
                Toast.LENGTH_SHORT
            ).show()
            true
        }

        // Темная тема
        val darkThemePref = findPreference<SwitchPreferenceCompat>("dark_theme")
        darkThemePref?.setOnPreferenceChangeListener { _, newValue ->
            Toast.makeText(
                requireContext(),
                if (newValue as Boolean) "Темная тема включена" else "Светлая тема включена",
                Toast.LENGTH_SHORT
            ).show()
            true
        }
    }
}