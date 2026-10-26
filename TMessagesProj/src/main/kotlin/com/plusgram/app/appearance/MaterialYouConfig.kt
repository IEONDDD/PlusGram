package com.plusgram.app.appearance

import android.content.SharedPreferences
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FileLog
import tw.nekomimi.nekogram.NekoConfig
import tw.nekomimi.nekogram.config.ConfigItem

/**
 * Собственные цвета Material You: цвет акцента и режим фона.
 *
 * Акцент: [ACCENT_SYSTEM] — системная палитра, [ACCENT_CUSTOM] — свой цвет [accentColor].
 * Фон: [BACKGROUND_SYSTEM] — системный нейтральный, [BACKGROUND_GRAY] — серый,
 * [BACKGROUND_BLACK] — почти чёрный в тёмной теме.
 *
 * Схема та же, что у [AppearanceConfig]: те же SharedPreferences, тот же [ConfigItem],
 * собственный список. Ключи с префиксом PlusgramColor.
 */
object MaterialYouConfig {

    const val ACCENT_SYSTEM = 0
    const val ACCENT_CUSTOM = 1

    const val BACKGROUND_SYSTEM = 0
    const val BACKGROUND_GRAY = 1
    const val BACKGROUND_BLACK = 2

    private val sync = Any()
    private val configs = ArrayList<ConfigItem>()

    @Volatile
    private var configLoaded = false

    /** Seed-цвет акцента по умолчанию — спокойный синий. */
    private val DEFAULT_ACCENT_SEED = 0xFF4E6EF2.toInt()

    @JvmField
    val accentMode = addConfig("PlusgramColorAccentMode", ConfigItem.configTypeInt, ACCENT_SYSTEM)

    @JvmField
    val accentColor = addConfig("PlusgramColorAccentColor", ConfigItem.configTypeInt, DEFAULT_ACCENT_SEED)

    @JvmField
    val backgroundMode = addConfig("PlusgramColorBackgroundMode", ConfigItem.configTypeInt, BACKGROUND_SYSTEM)

    @JvmStatic
    fun getConfigTypes(): Map<String, Int> = configs.associate { it.key to it.type }

    @JvmStatic
    fun getPreferences(): SharedPreferences = NekoConfig.getPreferences()

    @JvmStatic
    fun accentMode(): Int {
        ensureLoaded()
        return accentMode.Int()
    }

    @JvmStatic
    fun accentColor(): Int {
        ensureLoaded()
        return accentColor.Int()
    }

    @JvmStatic
    fun backgroundMode(): Int {
        ensureLoaded()
        return backgroundMode.Int()
    }

    @JvmStatic
    fun customAccent(): Boolean = accentMode() == ACCENT_CUSTOM

    @JvmStatic
    fun customBackground(): Boolean = backgroundMode() != BACKGROUND_SYSTEM

    private fun addConfig(key: String, type: Int, defaultValue: Any?): ConfigItem {
        val item = ConfigItem(key, type, defaultValue)
        configs.add(item)
        return item
    }

    @JvmStatic
    fun init() {
        loadConfig(false)
    }

    @JvmStatic
    fun ensureLoaded() {
        if (!configLoaded) loadConfig(false)
    }

    @JvmStatic
    fun loadConfig(force: Boolean) {
        synchronized(sync) {
            if (configLoaded && !force) return
            if (ApplicationLoader.applicationContext == null) return
            val preferences = getPreferences()
            for (item in configs) {
                try {
                    when (item.type) {
                        ConfigItem.configTypeBool ->
                            item.value = preferences.getBoolean(item.key, item.defaultValue as Boolean)

                        ConfigItem.configTypeInt ->
                            item.value = preferences.getInt(item.key, item.defaultValue as Int)

                        ConfigItem.configTypeLong ->
                            item.value = preferences.getLong(item.key, item.defaultValue as Long)

                        ConfigItem.configTypeFloat ->
                            item.value = preferences.getFloat(item.key, item.defaultValue as Float)

                        ConfigItem.configTypeString ->
                            item.value = preferences.getString(item.key, item.defaultValue as String?)
                    }
                } catch (e: Exception) {
                    FileLog.e(e)
                }
            }
            configLoaded = true
        }
    }

    /** Сбрасывает цвета к значениям по умолчанию. */
    @JvmStatic
    fun reset() {
        synchronized(sync) {
            val editor = getPreferences().edit()
            for (item in configs) {
                editor.remove(item.key)
                item.value = item.defaultValue
            }
            editor.apply()
        }
    }
}
