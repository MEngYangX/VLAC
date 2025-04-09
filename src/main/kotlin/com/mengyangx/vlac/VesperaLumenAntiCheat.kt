package com.mengyangx.vlac

import com.mengyangx.vlac.command.VLACCommand
import com.mengyangx.vlac.config.VLACConfig
import com.mengyangx.vlac.util.LanguageManager
import com.mengyangx.vlac.util.LogManager
import com.mengyangx.vlac.util.PermissionUtils
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.nio.file.Paths
import java.text.DecimalFormat

/**
 * Vespera Lumen Anti-Cheat
 */
class VesperaLumenAntiCheat : ModInitializer {
    companion object {
        const val MOD_ID = "vespera-lumen-anticheat"
        const val MOD_NAME = "Vespera Lumen Anti-Cheat"
        const val MOD_VERSION = "1.0.0"
        const val MOD_AUTHOR = "MengyangX"
        const val MOD_DESCRIPTION = "A powerful anti-cheat plugin for Minecraft servers"
        const val MOD_WEBSITE = "https://github.com/MengyangX/VLAC"
        
        private val logger = LoggerFactory.getLogger("VLAC")
        private var luckPerms: LuckPerms? = null
        private var isLuckPermsAvailable = false
        
        // Configuration directory
        lateinit var configDir: Path
        
        /**
         * Reload language files
         */
        fun reloadLanguage() {
            try {
                val langDir = configDir.resolve("lang")
                LanguageManager.reloadLanguages(langDir)
                logger.info(LanguageManager.getString("system.language.reloaded"))
            } catch (e: Exception) {
                logger.error(LanguageManager.getString("system.language.reload_failed", e.message))
            }
        }
        
        /**
         * Get LuckPerms instance
         */
        fun getLuckPerms(): LuckPerms? {
            return luckPerms
        }
        
        /**
         * Check if LuckPerms is available
         */
        fun isLuckPermsAvailable(): Boolean {
            return isLuckPermsAvailable
        }
    }
    
    override fun onInitialize() {
        // Setup config directory first, so we can load languages
        configDir = FabricLoader.getInstance().configDir.resolve("vlac")
        
        // Load language files before anything else
        val langDir = configDir.resolve("lang")
        LanguageManager.init(langDir)
        
        // Print banner using the ASCII art (this doesn't need translation)
        logger.info("")
        logger.info("  __     __  _          _       ____ ")
        logger.info("  \\ \\   / / | |        / \\     / ___|")
        logger.info("   \\ \\ / /  | |       / _ \\   | |                       vespera lumen anticheat $MOD_VERSION")
        logger.info("    \\ V /   | |___   / ___ \\  | |___")
        logger.info("     \\_/    |_____| /_/   \\_\\  \\____|")
        logger.info("")
        logger.info("Fabric API Version: 0.119.9+1.21.5")
        logger.info("Minecraft Version: 1.21.5")
        logger.info("")
        
        // Load configuration
        logger.info(LanguageManager.getString("system.config.loading"))
        VLACConfig.init(configDir)
        logger.info(LanguageManager.getString("system.config.loaded"))
        
        // Register commands
        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
            VLACCommand.register(dispatcher)
            logger.info(LanguageManager.getString("system.commands.registered"))
        }
        
        // Initialize LuckPerms
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            try {
                luckPerms = LuckPermsProvider.get()
                isLuckPermsAvailable = PermissionUtils.init(luckPerms!!)
                if (isLuckPermsAvailable) {
                    logger.info(LanguageManager.getString("system.luckperms.connected"))
                } else {
                    logger.warn(LanguageManager.getString("system.luckperms.connection_failed"))
                }
            } catch (e: Exception) {
                logger.error(LanguageManager.getString("system.luckperms.init_failed", e.message))
                isLuckPermsAvailable = false
            }
        }
        
        // Enable debug mode if configured
        if (VLACConfig.isDebugEnabled()) {
            logger.info(LanguageManager.getString("system.debug.enabled"))
        }
        
        // Calculate startup time (if needed in the future)
        logger.info(LanguageManager.getString("system.mod.enabled"))
    }
}