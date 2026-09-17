package org.axostudio.axonpcs.config

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.io.File
import java.nio.file.Files
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class ConfigManager(private val plugin: Plugin) {
    private val miniMessage = MiniMessage.miniMessage()
    private val legacySerializer = LegacyComponentSerializer.legacyAmpersand()
    private val languageConfigs = ConcurrentHashMap<String, YamlConfiguration>()

    @Volatile
    var settings: NpcSettings = NpcSettings()
        private set

    @Volatile
    var fallbackLanguage: String = "en"
        private set

    @Volatile
    var autoDetectLanguage: Boolean = true
        private set

    fun load() {
        plugin.saveDefaultConfig()
        plugin.reloadConfig()

        settings = NpcSettings.fromConfig(plugin.config)
        fallbackLanguage = plugin.config.getString("language.fallback", "en")?.lowercase(Locale.ROOT) ?: "en"
        autoDetectLanguage = plugin.config.getBoolean("language.auto-detect-client", true)

        loadLanguages()
    }

    private fun loadLanguages() {
        languageConfigs.clear()
        val langDir = File(plugin.dataFolder, "lang")
        if (!langDir.exists()) {
            langDir.mkdirs()
        }
        migrateLegacyLanguages(langDir)

        listOf("en.yml", "es.yml", "ru.yml").forEach { name ->
            val file = File(langDir, name)
            if (!file.exists()) {
                saveBundledLanguage(name, file)
            }
        }

        langDir.listFiles { file ->
            file.isFile && (file.extension.equals("yml", true) || file.extension.equals("yaml", true))
        }?.sortedBy { it.name.lowercase(Locale.ROOT) }?.forEach { file ->
            val langCode = file.nameWithoutExtension.lowercase(Locale.ROOT)
            languageConfigs[langCode] = YamlConfiguration.loadConfiguration(file)
        }
    }

    private fun saveBundledLanguage(name: String, target: File) {
        plugin.getResource("languages/$name")?.use { input ->
            target.outputStream().buffered().use { output -> input.copyTo(output) }
        }
    }

    private fun migrateLegacyLanguages(langDir: File) {
        val legacyDir = File(plugin.dataFolder, "languages")
        if (!legacyDir.isDirectory) return

        var migrated = 0
        legacyDir.listFiles { file ->
            file.isFile && (file.extension.equals("yml", true) || file.extension.equals("yaml", true))
        }?.forEach { source ->
            val target = File(langDir, source.name)
            if (target.exists()) return@forEach
            try {
                Files.move(source.toPath(), target.toPath())
                migrated++
            } catch (moveError: Exception) {
                plugin.logger.warning("Could not migrate language file ${source.name} to lang: ${moveError.message}")
            }
        }

        if (legacyDir.list()?.isEmpty() == true) {
            legacyDir.delete()
        }
        if (migrated > 0) {
            plugin.logger.info("Migrated $migrated language file(s) from languages to lang.")
        }
    }

    private val bundledConfigs = ConcurrentHashMap<String, YamlConfiguration>()

    private fun getBundledConfig(lang: String): YamlConfiguration? {
        val cached = bundledConfigs[lang]
        if (cached != null) return cached

        return try {
            plugin.getResource("languages/$lang.yml")?.use { input ->
                java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8).use { reader ->
                    val cfg = YamlConfiguration.loadConfiguration(reader)
                    bundledConfigs[lang] = cfg
                    cfg
                }
            }
        } catch (ignored: Exception) {
            null
        }
    }

    fun getRaw(sender: CommandSender?, key: String): String {
        val lang = resolveLanguage(sender)
        val config = languageConfigs[lang]
        config?.getString(key)?.let { return it }

        val fallbackConfig = languageConfigs[fallbackLanguage]
        fallbackConfig?.getString(key)?.let { return it }

        getBundledConfig(lang)?.getString(key)?.let { return it }
        getBundledConfig(fallbackLanguage)?.getString(key)?.let { return it }
        getBundledConfig("en")?.getString(key)?.let { return it }

        return "<red>Missing message: $key</red>"
    }

    fun send(sender: CommandSender, key: String, placeholders: Map<String, String> = emptyMap()) {
        val raw = getRaw(sender, key)
        var formatted = raw
        for ((k, v) in placeholders) {
            formatted = formatted.replace("{$k}", v).replace("%$k%", v)
        }
        sender.sendMessage(parse(formatted))
    }

    fun parse(text: String): Component {
        return if (text.contains("&") || text.contains("§")) {
            legacySerializer.deserialize(text.replace("§", "&"))
        } else {
            try {
                miniMessage.deserialize(text)
            } catch (e: Exception) {
                legacySerializer.deserialize(text)
            }
        }
    }

    private fun resolveLanguage(sender: CommandSender?): String {
        if (sender !is Player || !autoDetectLanguage) {
            return fallbackLanguage
        }
        val clientLocale = sender.locale().language.lowercase(Locale.ROOT)
        return if (languageConfigs.containsKey(clientLocale)) clientLocale else fallbackLanguage
    }
}
