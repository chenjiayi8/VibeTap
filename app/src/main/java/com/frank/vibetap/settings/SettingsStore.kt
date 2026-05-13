package com.frank.vibetap.settings

class SettingsStore(initialValue: AppSettings = AppSettings()) {
    private var currentValue: AppSettings = initialValue

    fun read(): AppSettings = currentValue

    fun save(newValue: AppSettings) {
        currentValue = newValue
    }
}
