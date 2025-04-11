package com.mengyangx.vlac.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mengyangx.vlac.VesperaLumenAntiCheat
import net.fabricmc.loader.api.FabricLoader
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 * Language manager for VLAC.
 */
object LanguageManager {
    private val logger = LogManager.getLogger(LanguageManager::class.java)
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private var languageDir = Path.of("VLAC/lang")
    private var currentLanguage = "en_US"
    private val translations = mutableMapOf<String, Map<String, String>>()
    private val defaultLanguages = arrayOf("en_US", "zh_CN")
    
    // 语言模式：AUTO自动，MANUAL手动
    enum class LanguageMode { AUTO, MANUAL }
    
    // 存储每个玩家的语言设置和模式
    private val playerSettings = mutableMapOf<UUID, Pair<String, LanguageMode>>()
    
    /**
     * 初始化语言管理器
     *
     * @param langDir 语言文件目录
     */
    fun init(langDir: Path) {
        this.languageDir = langDir
        registerLanguageFiles()
    }

    fun registerLanguageFiles() {
        try {
            if (!Files.exists(languageDir)) {
                Files.createDirectories(languageDir)
                logger.info("Created language directory: $languageDir")
            }

            migrateOldFormat()
            createDefaultJsonLanguages()
            loadCustomLanguages()

            // 修复对config的引用，直接使用默认语言
            // 由于VesperaLumenAntiCheat.config可能未初始化，我们暂时使用默认语言
            // val configLanguage = VesperaLumenAntiCheat.config.getConfig().language
            // if (configLanguage.isNotEmpty()) {
            //     setLanguage(configLanguage)
            // }
        } catch (e: Exception) {
            logger.error("Failed to register language files: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Migrates old format properties files to new JSON format
     */
    private fun migrateOldFormat() {
        try {
            Files.list(languageDir).forEach { file ->
                val fileName = file.fileName.toString()
                if (fileName.endsWith(".properties")) {
                    val langCode = fileName.substringBeforeLast(".")
                    logger.info("Migrating $fileName to JSON format...")
                    
                    val properties = Properties()
                    FileReader(file.toFile(), StandardCharsets.UTF_8).use { reader ->
                        properties.load(reader)
                    }
                    
                    val entries = mutableMapOf<String, String>()
                    properties.forEach { key, value ->
                        entries[key.toString()] = value.toString()
                    }
                    
                    val jsonFile = languageDir.resolve("$langCode.json")
                    FileWriter(jsonFile.toFile(), StandardCharsets.UTF_8).use { writer ->
                        gson.toJson(entries, writer)
                    }
                    
                    Files.delete(file)
                    logger.info("Successfully migrated $fileName to $langCode.json")
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to migrate old format files: ${e.message}")
        }
    }

    /**
     * Creates default JSON language files if they don't exist
     */
    private fun createDefaultJsonLanguages() {
        for (lang in defaultLanguages) {
            val file = languageDir.resolve("$lang.json")
            if (!Files.exists(file)) {
                logger.info("Creating default language file: $lang.json")
                
                val entries = createDefaultTranslations(lang)
                
                FileWriter(file.toFile(), StandardCharsets.UTF_8).use { writer ->
                    gson.toJson(entries, writer)
                }
                
                logger.info("Created default language file: $lang.json")
            }
            
            // Load the language
            loadLanguage(lang)
        }
    }
    
    /**
     * Creates default translations for a given language
     */
    private fun createDefaultTranslations(lang: String): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        
        when (lang) {
            "en_US" -> {
                // Debug command
                entries["vlac.command.debug.help"] = "Debug commands for VLAC"
                entries["vlac.command.debug.enable"] = "Enable debug mode"
                entries["vlac.command.debug.disable"] = "Disable debug mode"
                entries["vlac.command.debug.enabled"] = "Debug mode enabled"
                entries["vlac.command.debug.disabled"] = "Debug mode disabled"
                entries["vlac.command.debug.add"] = "Add player to exempt list"
                entries["vlac.command.debug.remove"] = "Remove player from exempt list"
                entries["vlac.command.debug.list"] = "List all exempt players"
                entries["vlac.command.debug.added"] = "Added %s to exempt list"
                entries["vlac.command.debug.removed"] = "Removed %s from exempt list"
                entries["vlac.command.debug.exempt_list"] = "Exempt players: %s"
                entries["vlac.command.debug.enable_success"] = "Debug mode enabled successfully"
                entries["vlac.command.debug.disable_success"] = "Debug mode disabled successfully"
                entries["vlac.command.debug.enable_log"] = "Debug mode enabled by"
                entries["vlac.command.debug.disable_log"] = "Debug mode disabled by"
                
                // Language command
                entries["vlac.command.help.title"] = "VLAC Commands:"
                entries["vlac.command.help.reload"] = "Reload configuration"
                entries["vlac.command.help.toggle"] = "Toggle VLAC on/off"
                entries["vlac.command.help.debug"] = "Manage debug mode"
                entries["vlac.command.help.language"] = "Change language"
                entries["vlac.command.help.status"] = "Show VLAC status"
                entries["vlac.command.help.exempt"] = "Add/remove player exemption"
                entries["vlac.command.help.help"] = "Show this help message"
                
                entries["vlac.command.language.help"] = "Change language"
                entries["vlac.command.language.change"] = "Change language to %s"
                entries["vlac.command.language.list"] = "List all available languages"
                entries["vlac.command.language.current"] = "Current language: %s"
                entries["vlac.command.language.available"] = "Available languages: %s"
                entries["vlac.command.language.changed"] = "Language changed to %s"
                entries["vlac.command.language.not_found"] = "Language %s not found"
                entries["vlac.command.language.success"] = "Language changed to"
                entries["vlac.command.language.failed"] = "Failed to change language"
                entries["vlac.command.language.unsupported"] = "Unsupported language"
                entries["vlac.command.language.log"] = "Language changed by"
                entries["vlac.command.language.error"] = "Error changing language"
                // 新增的语言项
                entries["vlac.command.language.auto_mode"] = "Auto language mode, detected as"
                entries["vlac.command.language.mode_auto"] = "Auto"
                entries["vlac.command.language.mode_manual"] = "Manual"
                entries["vlac.command.language.player_only"] = "This command can only be used by players"
                
                // Toggle command
                entries["vlac.command.toggle.enable_success"] = "VLAC enabled successfully"
                entries["vlac.command.toggle.disable_success"] = "VLAC disabled successfully"
                entries["vlac.command.toggle.enable_log"] = "VLAC enabled by"
                entries["vlac.command.toggle.disable_log"] = "VLAC disabled by"
                
                // Reload command
                entries["vlac.command.reload.success"] = "Configuration reloaded successfully"
                entries["vlac.command.reload.failed"] = "Failed to reload configuration: %s"
                
                // Exempt command
                entries["vlac.command.exempt.player_not_found"] = "Player not found: %s"
                entries["vlac.command.exempt.added"] = "Player added to exemption list: %s"
                entries["vlac.command.exempt.removed"] = "Player removed from exemption list: %s"
                entries["vlac.command.exempt.invalid_state"] = "Invalid state. Use 'add' or 'remove'"
                entries["vlac.command.exempt.failed"] = "Failed to update exemption list: %s"
                
                // Status display
                entries["vlac.status.enabled"] = "Enabled"
                entries["vlac.status.disabled"] = "Disabled"
                entries["vlac.status.title"] = "VLAC Status"
                entries["vlac.status.mod"] = "Mod Status"
                entries["vlac.status.debug"] = "Debug Mode"
                entries["vlac.status.language"] = "Language"
                entries["vlac.status.luckperms"] = "LuckPerms Support"
                entries["vlac.status.memory"] = "Memory Usage"
                
                // System messages
                entries["vlac.system.reload.start"] = "Reloading VLAC..."
                entries["vlac.system.reload.done"] = "Reload completed"
                entries["vlac.system.reload.failed"] = "Reload failed: %s"
                entries["vlac.system.language.reloaded"] = "Language files reloaded"
                entries["vlac.system.language.reload_failed"] = "Failed to reload language files: %s"
                entries["vlac.system.config.loading"] = "Loading configuration..."
                entries["vlac.system.config.loaded"] = "Configuration loaded"
                entries["vlac.system.commands.registered"] = "Commands registered"
                entries["vlac.startup.luckperms"] = "Initializing LuckPerms integration..."
                entries["vlac.system.luckperms.connected"] = "Successfully connected to LuckPerms API"
                entries["vlac.system.luckperms.connection_failed"] = "Failed to connect to LuckPerms API"
                entries["vlac.system.luckperms.init_failed"] = "Failed to initialize LuckPerms: %s"
                entries["vlac.system.debug.enabled"] = "Debug mode is enabled"
                entries["vlac.system.mod.enabled"] = "Mod successfully enabled"
            }
            "zh_CN" -> {
                // Debug command
                entries["vlac.command.debug.help"] = "VLAC 调试命令"
                entries["vlac.command.debug.enable"] = "启用调试模式"
                entries["vlac.command.debug.disable"] = "禁用调试模式"
                entries["vlac.command.debug.enabled"] = "调试模式已启用"
                entries["vlac.command.debug.disabled"] = "调试模式已禁用"
                entries["vlac.command.debug.add"] = "添加玩家到豁免列表"
                entries["vlac.command.debug.remove"] = "从豁免列表中移除玩家"
                entries["vlac.command.debug.list"] = "列出所有豁免玩家"
                entries["vlac.command.debug.added"] = "已将 %s 添加到豁免列表"
                entries["vlac.command.debug.removed"] = "已将 %s 从豁免列表中移除"
                entries["vlac.command.debug.exempt_list"] = "豁免玩家: %s"
                entries["vlac.command.debug.enable_success"] = "调试模式已成功启用"
                entries["vlac.command.debug.disable_success"] = "调试模式已成功禁用"
                entries["vlac.command.debug.enable_log"] = "调试模式被启用，操作者："
                entries["vlac.command.debug.disable_log"] = "调试模式被禁用，操作者："
                
                // Language command
                entries["vlac.command.help.title"] = "VLAC 命令:"
                entries["vlac.command.help.reload"] = "重新加载配置"
                entries["vlac.command.help.toggle"] = "切换 VLAC 开关"
                entries["vlac.command.help.debug"] = "管理调试模式"
                entries["vlac.command.help.language"] = "更改语言"
                entries["vlac.command.help.status"] = "显示 VLAC 状态"
                entries["vlac.command.help.exempt"] = "添加/移除玩家豁免"
                entries["vlac.command.help.help"] = "显示此帮助信息"
                
                entries["vlac.command.language.help"] = "更改语言"
                entries["vlac.command.language.change"] = "更改语言为 %s"
                entries["vlac.command.language.list"] = "列出所有可用语言"
                entries["vlac.command.language.current"] = "当前语言: %s"
                entries["vlac.command.language.available"] = "可用语言: %s"
                entries["vlac.command.language.changed"] = "语言已更改为 %s"
                entries["vlac.command.language.not_found"] = "未找到语言 %s"
                entries["vlac.command.language.success"] = "语言已更改为"
                entries["vlac.command.language.failed"] = "更改语言失败"
                entries["vlac.command.language.unsupported"] = "不支持的语言"
                entries["vlac.command.language.log"] = "语言被更改，操作者："
                entries["vlac.command.language.error"] = "更改语言时出错："
                // 新增的语言项
                entries["vlac.command.language.auto_mode"] = "自动语言模式，检测为"
                entries["vlac.command.language.mode_auto"] = "自动"
                entries["vlac.command.language.mode_manual"] = "手动"
                entries["vlac.command.language.player_only"] = "此命令只能由玩家使用"
                
                // Toggle command
                entries["vlac.command.toggle.enable_success"] = "VLAC 已成功启用"
                entries["vlac.command.toggle.disable_success"] = "VLAC 已成功禁用"
                entries["vlac.command.toggle.enable_log"] = "VLAC 被启用，操作者："
                entries["vlac.command.toggle.disable_log"] = "VLAC 被禁用，操作者："
                
                // Reload command
                entries["vlac.command.reload.success"] = "配置重新加载成功"
                entries["vlac.command.reload.failed"] = "重新加载配置失败: %s"
                
                // Exempt command
                entries["vlac.command.exempt.player_not_found"] = "未找到玩家: %s"
                entries["vlac.command.exempt.added"] = "已将玩家添加到豁免列表: %s"
                entries["vlac.command.exempt.removed"] = "已将玩家从豁免列表中移除: %s"
                entries["vlac.command.exempt.invalid_state"] = "无效状态，请使用 'add' 或 'remove'"
                entries["vlac.command.exempt.failed"] = "更新豁免列表失败: %s"
                
                // Status display
                entries["vlac.status.enabled"] = "已启用"
                entries["vlac.status.disabled"] = "已禁用"
                entries["vlac.status.title"] = "VLAC 状态"
                entries["vlac.status.mod"] = "模组状态"
                entries["vlac.status.debug"] = "调试模式"
                entries["vlac.status.language"] = "语言"
                entries["vlac.status.luckperms"] = "LuckPerms 支持"
                entries["vlac.status.memory"] = "内存使用"
                
                // System messages
                entries["vlac.system.reload.start"] = "正在重新加载 VLAC..."
                entries["vlac.system.reload.done"] = "重新加载完成"
                entries["vlac.system.reload.failed"] = "重新加载失败: %s"
                entries["vlac.system.language.reloaded"] = "语言文件已重新加载"
                entries["vlac.system.language.reload_failed"] = "重新加载语言文件失败: %s"
                entries["vlac.system.config.loading"] = "正在加载配置..."
                entries["vlac.system.config.loaded"] = "配置已加载"
                entries["vlac.system.commands.registered"] = "命令已注册"
                entries["vlac.startup.luckperms"] = "正在初始化 LuckPerms 集成..."
                entries["vlac.system.luckperms.connected"] = "成功连接到 LuckPerms API"
                entries["vlac.system.luckperms.connection_failed"] = "连接到 LuckPerms API 失败"
                entries["vlac.system.luckperms.init_failed"] = "初始化 LuckPerms 失败: %s"
                entries["vlac.system.debug.enabled"] = "调试模式已启用"
                entries["vlac.system.mod.enabled"] = "模组已成功启用"
            }
        }
        
        return entries
    }

    /**
     * Loads custom language files from the language directory
     */
    private fun loadCustomLanguages() {
        try {
            Files.list(languageDir).forEach { file ->
                val fileName = file.fileName.toString()
                if (fileName.endsWith(".json")) {
                    val langCode = fileName.substringBeforeLast(".")
                    if (!translations.containsKey(langCode)) {
                        loadLanguage(langCode)
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to load custom languages: ${e.message}")
        }
    }

    /**
     * Loads a language file
     */
    private fun loadLanguage(lang: String) {
        val file = languageDir.resolve("$lang.json")
        if (Files.exists(file)) {
            try {
                FileReader(file.toFile(), StandardCharsets.UTF_8).use { reader ->
                    val type = object : TypeToken<Map<String, String>>() {}.type
                    val entries: Map<String, String> = gson.fromJson(reader, type)
                    translations[lang] = entries
                }
                logger.info("Loaded language: $lang")
            } catch (e: Exception) {
                logger.error("Failed to load language $lang: ${e.message}")
            }
        } else {
            logger.warn("Language file not found: $lang.json")
        }
    }
    
    /**
     * Sets the current language
     */
    fun setLanguage(lang: String): Boolean {
        if (translations.containsKey(lang)) {
            currentLanguage = lang
            logger.info("Language changed to: $lang")
            return true
        }
        logger.warn("Language not found: $lang")
        return false
    }

    /**
     * Gets the current language
     */
    fun getLanguage(): String {
        return currentLanguage
    }

    /**
     * Gets a localized string
     */
    fun getString(key: String, vararg args: Any): String {
        val translation = translations[currentLanguage] ?: return key
        val value = translation[key] ?: return key
        return if (args.isEmpty()) value else String.format(value, *args)
    }

    /**
     * Reloads all languages
     * 
     * @param langDir 语言文件目录
     */
    fun reloadLanguages(langDir: Path) {
        this.languageDir = langDir
        translations.clear()
        registerLanguageFiles()
        logger.info("Reloaded all languages")
    }

    /**
     * 根据客户端语言代码确定应使用的语言
     * 简体中文、繁体中文、文言文 -> 使用简体中文
     * 其他所有语言 -> 使用英文
     */
    fun determineLanguageFromClientCode(clientLanguageCode: String): String {
        return when {
            clientLanguageCode.startsWith("zh") || clientLanguageCode == "lzh" -> "zh_CN"
            else -> "en_US" // 默认使用英语
        }
    }
    
    /**
     * 获取玩家的语言设置
     */
    fun getPlayerLanguage(playerId: UUID): String {
        val setting = playerSettings[playerId]
        if (setting == null) {
            // 玩家没有设置，默认使用服务器默认语言
            return currentLanguage
        }
        
        // 返回当前设置的语言
        return setting.first
    }
    
    /**
     * 获取玩家的语言模式
     */
    fun getPlayerLanguageMode(playerId: UUID): LanguageMode {
        return playerSettings[playerId]?.second ?: LanguageMode.AUTO
    }
    
    /**
     * 设置玩家语言为自动模式
     */
    fun setPlayerLanguageAuto(playerId: UUID, clientLanguageCode: String): Boolean {
        val language = determineLanguageFromClientCode(clientLanguageCode)
        playerSettings[playerId] = Pair(language, LanguageMode.AUTO)
        logger.info("Set player $playerId language to AUTO (detected as: $language)")
        return true
    }
    
    /**
     * 手动设置玩家语言
     */
    fun setPlayerLanguageManual(playerId: UUID, lang: String): Boolean {
        if (!translations.containsKey(lang)) {
            logger.warn("Language not found: $lang")
            return false
        }
        
        playerSettings[playerId] = Pair(lang, LanguageMode.MANUAL)
        logger.info("Set player $playerId language to $lang (manual mode)")
        return true
    }
    
    /**
     * 根据玩家ID和客户端语言获取本地化字符串
     */
    fun getStringForPlayer(playerId: UUID, clientLanguageCode: String, key: String, vararg args: Any): String {
        // 获取该玩家应该使用的语言
        val playerSetting = playerSettings[playerId]
        
        // 确定最终使用的语言
        val langToUse = when {
            // 没有设置或是自动模式，则根据客户端语言确定
            playerSetting == null || playerSetting.second == LanguageMode.AUTO -> 
                determineLanguageFromClientCode(clientLanguageCode)
            
            // 手动模式，使用设置的语言
            else -> playerSetting.first
        }
        
        // 获取翻译
        val translation = translations[langToUse] ?: translations["en_US"] ?: return key
        val value = translation[key] ?: return key
        return if (args.isEmpty()) value else String.format(value, *args)
    }
} 