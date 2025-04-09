package com.mengyangx.vlac.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.mengyangx.vlac.VesperaLumenAntiCheat
import com.mengyangx.vlac.config.VLACConfig
import net.fabricmc.fabric.api.resource.ResourceManagerHelper
import net.fabricmc.fabric.api.resource.ResourcePackActivationType
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resource.ResourceType
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileReader
import java.io.FileWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*

/**
 * Language manager for VLAC
 * 
 * Handles both built-in language files (assets/vespera-lumen-anticheat/lang/*.json)
 * and dynamic runtime language files (config/vlac/lang/*.json)
 */
object LanguageManager {
    private val logger = LoggerFactory.getLogger("VLAC")
    private val MOD_ID = VesperaLumenAntiCheat.MOD_ID
    private val extraTranslations = mutableMapOf<String, MutableMap<String, String>>()
    private var currentLanguage = "en_us"  // Always use English by default
    private val gson = GsonBuilder().setPrettyPrinting().create()
    
    /**
     * Register built-in language files to Minecraft's resource system
     * This should be called during mod initialization
     */
    fun registerLanguageFiles() {
        try {
            logger.info("Registering VLAC language files...")
            
            // Register the mod's resource pack with Minecraft
            // This will load the language files from assets/vespera-lumen-anticheat/lang/*.json
            ResourceManagerHelper.registerBuiltinResourcePack(
                Identifier(MOD_ID, "languages"),
                FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow(),
                ResourcePackActivationType.DEFAULT_ENABLED
            )
            
            logger.info("VLAC language files registered successfully")
        } catch (e: Exception) {
            logger.error("Failed to register language files: ${e.message}")
        }
    }
    
    /**
     * Initialize language manager
     */
    fun init(langDir: Path) {
        try {
            // Create custom language directory if not exists
            val dir = langDir.toFile()
            if (!dir.exists()) {
                dir.mkdirs()
                logger.info("Created custom language directory: $langDir")
            }
            
            // Migrate old format files (yaml/properties) to JSON if they exist
            migrateOldFormatFiles(dir)
            
            // Load custom language files
            loadCustomLanguages(dir)
            
            // Always use English
            currentLanguage = "en_us"
            logger.info("Current language set to: $currentLanguage")
            
        } catch (e: Exception) {
            logger.error("Failed to initialize language manager: ${e.message}")
        }
    }
    
    /**
     * Migrate old format files (yaml/properties) to JSON
     */
    private fun migrateOldFormatFiles(dir: File) {
        try {
            // Check for old .yml files
            val enYmlFile = File(dir, "en_US.yml")
            
            // Check for old .properties files
            val enPropsFile = File(dir, "en_US.properties")
            
            // Just log their existence - full migration would be complex
            // and requires converting nested YAML structure to flat keys
            if (enYmlFile.exists()) {
                logger.info("Found old YAML language files. These are no longer supported. " +
                           "Please use JSON format language files.")
            }
            
            if (enPropsFile.exists()) {
                logger.info("Found old properties language files. These are no longer supported. " +
                           "Please use JSON format language files.")
            }
            
            // Create default JSON language file
            createDefaultLanguageFile(dir, "en_us")
            
        } catch (e: Exception) {
            logger.error("Failed to migrate old format files: ${e.message}")
        }
    }
    
    /**
     * Create default JSON language file
     */
    private fun createDefaultLanguageFile(dir: File, lang: String) {
        val file = File(dir, "$lang.json")
        if (!file.exists()) {
            try {
                val translations = mutableMapOf<String, String>()
                
                // English translations
                translations["vlac.status.enabled"] = "Enabled"
                translations["vlac.status.disabled"] = "Disabled"
                translations["vlac.status.title"] = "VLAC Status"
                translations["vlac.status.mod"] = "Mod Status"
                translations["vlac.status.debug"] = "Debug Mode"
                translations["vlac.status.language"] = "Language"
                translations["vlac.status.luckperms"] = "LuckPerms Support"
                translations["vlac.status.memory"] = "Memory Usage"
                
                // Command messages
                translations["vlac.command.help.title"] = "VLAC Commands:"
                translations["vlac.command.help.reload"] = "/vlac reload - Reload configuration"
                translations["vlac.command.help.toggle"] = "Toggle VLAC on/off"
                translations["vlac.command.help.debug"] = "/vlac debug - Manage debug mode"
                translations["vlac.command.help.language"] = "Change language"
                translations["vlac.command.help.status"] = "Show VLAC status"
                translations["vlac.command.help.exempt"] = "Add/remove player exemption"
                translations["vlac.command.help.help"] = "Show this help message"
                
                translations["vlac.command.reload.success"] = "Configuration reloaded successfully"
                translations["vlac.command.reload.failed"] = "Failed to reload configuration: %s"
                translations["vlac.command.reload.log"] = "Configuration reloaded by"
                translations["vlac.command.reload.error"] = "Error reloading configuration"
                translations["vlac.command.reload.usage"] = "Usage: /vlac reload"
                translations["vlac.command.reload.help"] = "Reload VLAC configuration"
                
                translations["vlac.command.toggle.current"] = "Current status"
                translations["vlac.command.toggle.enabled"] = "Enabled"
                translations["vlac.command.toggle.disabled"] = "Disabled"
                translations["vlac.command.toggle.enable_success"] = "VLAC enabled successfully"
                translations["vlac.command.toggle.disable_success"] = "VLAC disabled successfully"
                translations["vlac.command.toggle.enable_log"] = "VLAC enabled by"
                translations["vlac.command.toggle.disable_log"] = "VLAC disabled by"
                
                translations["vlac.command.debug.current"] = "Debug mode"
                translations["vlac.command.debug.enabled"] = "Debug mode enabled"
                translations["vlac.command.debug.disabled"] = "Debug mode disabled"
                translations["vlac.command.debug.added"] = "Added %s to debug exempt list"
                translations["vlac.command.debug.removed"] = "Removed %s from debug exempt list"
                translations["vlac.command.debug.toggle"] = "Debug mode: %s"
                translations["vlac.command.debug.usage"] = "Usage: /vlac debug <enable|disable|toggle|add|remove> [player]"
                translations["vlac.command.debug.help"] = "Enable or disable debug mode"
                translations["vlac.command.debug.failed"] = "Failed to set debug mode"
                translations["vlac.command.debug.enable_success"] = "Debug mode enabled successfully"
                translations["vlac.command.debug.disable_success"] = "Debug mode disabled successfully"
                translations["vlac.command.debug.enable_log"] = "Debug mode enabled by"
                translations["vlac.command.debug.disable_log"] = "Debug mode disabled by"
                
                translations["vlac.command.language.current"] = "Current language"
                translations["vlac.command.language.success"] = "Language changed to"
                translations["vlac.command.language.failed"] = "Failed to change language"
                translations["vlac.command.language.unsupported"] = "Unsupported language"
                translations["vlac.command.language.available"] = "Available languages: %s"
                translations["vlac.command.language.log"] = "Language changed by"
                translations["vlac.command.language.error"] = "Error changing language"
                translations["vlac.command.lang.changed"] = "Language changed to %s"
                translations["vlac.command.lang.notfound"] = "Language %s not found"
                translations["vlac.command.lang.usage"] = "Usage: /vlac lang <language>"
                translations["vlac.command.lang.help"] = "Change the language"
                
                translations["vlac.command.exempt.player_not_found"] = "Player not found"
                translations["vlac.command.exempt.added"] = "Player added to exemption list"
                translations["vlac.command.exempt.removed"] = "Player removed from exemption list"
                translations["vlac.command.exempt.invalid_state"] = "Invalid state. Use 'add' or 'remove'"
                translations["vlac.command.exempt.failed"] = "Failed to update exemption list"
                
                // System messages
                translations["vlac.system.language.reloaded"] = "Language files reloaded"
                translations["vlac.system.language.reload_failed"] = "Failed to reload language files: %s"
                translations["vlac.system.config.loading"] = "Loading configuration..."
                translations["vlac.system.config.loaded"] = "Configuration loaded"
                translations["vlac.system.commands.registered"] = "VLAC commands registered"
                translations["vlac.system.luckperms.connected"] = "Successfully connected to LuckPerms API"
                translations["vlac.system.luckperms.connection_failed"] = "Failed to connect to LuckPerms API"
                translations["vlac.system.luckperms.init_failed"] = "Failed to initialize LuckPerms: %s"
                translations["vlac.system.debug.enabled"] = "Debug mode enabled"
                translations["vlac.system.mod.enabled"] = "Mod successfully enabled"
                translations["vlac.system.mod.disabled"] = "Mod disabled"
                
                // Permission messages
                translations["vlac.permission.registering"] = "Registering VLAC permission nodes..."
                translations["vlac.permission.registered"] = "Permission node registered: %s - %s"
                translations["vlac.permission.init_failed"] = "Failed to initialize LuckPerms integration: %s"
                translations["vlac.permission.node_info"] = "Permission node: %s - %s"
                translations["vlac.permission.node_registered"] = "Permission node registered: %s"
                translations["vlac.permission.node_register_failed"] = "Failed to register permission node: %s"
                translations["vlac.permission.register_failed"] = "Failed to register VLAC permission nodes: %s"
                translations["vlac.permission.check_failed"] = "Failed to check permission: %s"
                translations["vlac.permission.get_player_groups_failed"] = "Failed to get player groups: %s"
                translations["vlac.permission.group_check_failed"] = "Failed to check player group: %s"
                translations["vlac.permission.add_to_group_failed"] = "Failed to add player to group: %s"
                translations["vlac.permission.remove_from_group_failed"] = "Failed to remove player from group: %s"
                translations["vlac.permission.error"] = "Failed to register permission: %s"
                translations["vlac.permission.check.failed"] = "Failed to check permission: %s"
                
                // Log messages
                translations["vlac.log.initialized"] = "Log system initialized successfully"
                translations["vlac.log.init_failed"] = "Failed to initialize log system: %s"
                translations["vlac.log.write_failed"] = "Failed to write log: %s"
                translations["vlac.log.broadcast_failed"] = "Failed to broadcast message: %s"
                translations["vlac.log.closed"] = "Log system closed successfully"
                translations["vlac.log.close_failed"] = "Failed to close log system: %s"
                translations["vlac.log.violation"] = "Violation logged: Player %s failed %s check with VL %s"
                translations["vlac.config.loaded"] = "Configuration loaded from %s"
                translations["vlac.config.created"] = "Created default configuration at %s"
                translations["vlac.config.error"] = "Error loading configuration: %s"
                translations["vlac.language.loaded"] = "Loaded language: %s"
                translations["vlac.language.created"] = "Created default language file: %s"
                translations["vlac.startup.config"] = "Loading configuration..."
                translations["vlac.startup.enabled"] = "Mod successfully enabled (took %s ms)"
                translations["vlac.startup.commands"] = "Registering commands..."
                translations["vlac.startup.luckperms"] = "Initializing LuckPerms integration..."
                translations["vlac.startup.debug"] = "Debug mode is %s"
                translations["vlac.check.failed"] = "Player %s failed %s check. VL: %s"
                translations["vlac.check.passed"] = "Player %s passed %s check"
                translations["vlac.punish.kick"] = "Player %s was kicked for failing %s check"
                translations["vlac.punish.ban"] = "Player %s was banned for failing %s check"
                
                // Save translations to file
                FileWriter(file).use { writer ->
                    gson.toJson(translations, writer)
                }
                
                logger.info("Created default JSON language file: $lang")
            } catch (e: Exception) {
                logger.error("Failed to create default JSON language file $lang: ${e.message}")
            }
        }
    }
    
    /**
     * Load custom language files from config directory
     */
    private fun loadCustomLanguages(dir: File) {
        try {
            val files = dir.listFiles { file -> file.isFile && file.extension == "json" }
            if (files != null) {
                for (file in files) {
                    val langCode = file.nameWithoutExtension.lowercase()
                    try {
                        val type = object : TypeToken<Map<String, String>>() {}.type
                        val translations = gson.fromJson<Map<String, String>>(FileReader(file), type)
                        
                        // Store translations
                        extraTranslations[langCode] = translations.toMutableMap()
                        logger.info("Loaded custom language file: $langCode")
                    } catch (e: Exception) {
                        logger.error("Failed to load custom language file $langCode: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to load custom language files: ${e.message}")
        }
    }
    
    /**
     * Set current language
     */
    fun setLanguage(lang: String) {
        // Always use English
        currentLanguage = "en_us"
        logger.info("Language set to: $currentLanguage")
    }
    
    /**
     * Get current language
     */
    fun getCurrentLanguage(): String {
        return currentLanguage
    }
    
    /**
     * Get localized string
     * First tries to get from custom translations, then falls back to Minecraft translation system
     */
    fun getString(key: String, vararg args: Any?): String {
        try {
            // Add prefix if needed
            val fullKey = if (!key.startsWith("vlac.")) "vlac.$key" else key
            
            // Try to get from custom translations first
            val customTranslation = extraTranslations[currentLanguage]?.get(fullKey)
                ?: extraTranslations["en_us"]?.get(fullKey)
            
            if (customTranslation != null) {
                return if (args.isEmpty()) {
                    customTranslation
                } else {
                    try {
                        String.format(customTranslation, *args)
                    } catch (e: Exception) {
                        logger.error("Error formatting message '$fullKey': ${e.message}")
                        customTranslation
                    }
                }
            }
            
            // Fall back to Minecraft translation system
            val text = if (args.isEmpty()) {
                Text.translatable(fullKey)
            } else {
                Text.translatable(fullKey, *args)
            }
            
            return text.string
        } catch (e: Exception) {
            logger.error("Error getting message for key '$key': ${e.message}")
            return key
        }
    }
    
    /**
     * Reload all languages
     */
    fun reloadLanguages(langDir: Path) {
        extraTranslations.clear()
        init(langDir)
    }
} 