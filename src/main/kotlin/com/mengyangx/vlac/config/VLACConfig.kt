package com.mengyangx.vlac.config

import com.mengyangx.vlac.util.LogManager
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * VLAC Configuration System
 */
object VLACConfig {
    private val dumperOptions = DumperOptions().apply {
        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        isPrettyFlow = true
    }
    private val yaml = Yaml(dumperOptions)
    private var configDir: Path? = null
    private var configFile: File? = null
    
    // Configuration settings
    private var enabled = true
    private var language = "en_US"
    private var debug = false
    private var useLuckPerms = true
    private var bypassGroups = mutableListOf("admin", "mod")

    /**
     * Initialize configuration system
     * 
     * @param baseConfigDir Base configuration directory path
     */
    fun init(baseConfigDir: Path) {
        // Create VLAC directory in config folder
        this.configDir = baseConfigDir
        this.configFile = Paths.get(configDir.toString(), "config.yml").toFile()
        
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir)
                LogManager.info("Created configuration directory: $configDir")
            } catch (e: Exception) {
                LogManager.error("Could not create configuration directory: ${e.message}")
                return
            }
        }
        
        loadConfig()
    }
    
    /**
     * Load configuration file
     */
    private fun loadConfig() {
        if (configFile!!.exists()) {
            try {
                FileReader(configFile!!).use { reader ->
                    val config = yaml.load<Map<String, Any>>(reader)
                    LogManager.info("Configuration file loaded")
                    
                    // Read main configuration
                    enabled = config["enabled"] as? Boolean ?: true
                    language = config["language"] as? String ?: "en_US"
                    debug = config["debug"] as? Boolean ?: false
                    
                    // Read permissions configuration
                    val permissionsSection = config["permissions"] as? Map<String, Any>
                    if (permissionsSection != null) {
                        useLuckPerms = permissionsSection["useLuckPerms"] as? Boolean ?: true
                        
                        val groups = permissionsSection["bypassGroups"] as? List<String>
                        if (groups != null) {
                            bypassGroups.clear()
                            bypassGroups.addAll(groups)
                        }
                    }
                }
            } catch (e: Exception) {
                LogManager.error("Error loading configuration file: ${e.message}")
                createDefaultConfig()
            }
        } else {
            createDefaultConfig()
        }
    }
    
    /**
     * Create default configuration file
     */
    private fun createDefaultConfig() {
        // Create configuration structure
        val config = mutableMapOf<String, Any>()
        
        // Main configuration
        config["enabled"] = true
        config["language"] = "en_US"
        config["debug"] = false
        
        // Permissions configuration
        val permissionsMap = mutableMapOf<String, Any>()
        permissionsMap["useLuckPerms"] = true
        permissionsMap["bypassGroups"] = bypassGroups
        config["permissions"] = permissionsMap
        
        // Detection configuration
        val detectionsMap = mutableMapOf<String, Any>()
        
        // Fly detection
        val flyMap = mutableMapOf<String, Any>()
        flyMap["enabled"] = true
        flyMap["sensitivity"] = 2
        detectionsMap["fly"] = flyMap
        
        // Speed detection
        val speedMap = mutableMapOf<String, Any>()
        speedMap["enabled"] = true
        speedMap["maxSpeed"] = 0.8
        detectionsMap["speed"] = speedMap
        
        config["detections"] = detectionsMap
        
        // Punishment configuration
        val punishmentsMap = mutableMapOf<String, Any>()
        punishmentsMap["defaultAction"] = "warn"
        punishmentsMap["warnMessage"] = "§c[VLAC] Potential cheating behavior detected"
        punishmentsMap["kickMessage"] = "§c[VLAC] You have been kicked for potential cheating behavior"
        config["punishments"] = punishmentsMap
        
        saveConfig(config)
        LogManager.info("Default configuration file created")
    }
    
    /**
     * Save configuration to file
     */
    private fun saveConfig(config: Map<String, Any> = getCurrentConfig()) {
        try {
            FileWriter(configFile!!).use { writer ->
                yaml.dump(config, writer)
            }
            LogManager.info("Configuration file saved")
        } catch (e: Exception) {
            LogManager.error("Error saving configuration file: ${e.message}")
        }
    }
    
    /**
     * Get current configuration
     */
    private fun getCurrentConfig(): Map<String, Any> {
        val config = mutableMapOf<String, Any>()
        
        // Main configuration
        config["enabled"] = enabled
        config["language"] = language
        config["debug"] = debug
        
        // Permissions configuration
        val permissionsMap = mutableMapOf<String, Any>()
        permissionsMap["useLuckPerms"] = useLuckPerms
        permissionsMap["bypassGroups"] = bypassGroups
        config["permissions"] = permissionsMap
        
        return config
    }
    
    /**
     * Check if mod is enabled
     */
    fun isEnabled(): Boolean {
        return enabled
    }
    
    /**
     * Set mod enabled state
     */
    fun setEnabled(value: Boolean) {
        enabled = value
        saveConfig()
    }
    
    /**
     * Get mod language
     */
    fun getLanguage(): String {
        return language
    }
    
    /**
     * Set mod language
     */
    fun setLanguage(value: String) {
        language = value.lowercase()
        saveConfig()
    }
    
    /**
     * Check if debug mode is enabled
     */
    fun isDebugEnabled(): Boolean {
        return debug
    }
    
    /**
     * Set debug mode
     */
    fun setDebug(value: Boolean) {
        debug = value
        saveConfig()
    }
    
    /**
     * Reload configuration
     */
    fun reload() {
        loadConfig()
        LogManager.info("Configuration reloaded")
    }

    /**
     * Check if LuckPerms is enabled
     */
    fun isLuckPermsEnabled(): Boolean {
        return useLuckPerms
    }
    
    /**
     * Get bypass groups list
     */
    fun getBypassGroups(): List<String> {
        return bypassGroups
    }
} 