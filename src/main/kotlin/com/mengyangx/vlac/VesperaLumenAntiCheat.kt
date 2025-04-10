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
        
        // VLACConfig实例
        lateinit var config: VLACConfig
        
        /**
         * Reload language files
         */
        fun reloadLanguage() {
            try {
                val langDir = configDir.resolve("lang")
                LanguageManager.reloadLanguages(langDir)
                logger.info("Language files reloaded")
            } catch (e: Exception) {
                logger.error("Failed to reload language files: ${e.message ?: "Unknown error"}")
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
        
        // Initialize VLACConfig
        config = VLACConfig
        
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
        logger.info(LanguageManager.getString("vlac.system.config.loading"))
        VLACConfig.init(configDir)
        logger.info(LanguageManager.getString("vlac.system.config.loaded"))
        
        // Register commands
        CommandRegistrationCallback.EVENT.register { dispatcher, registryAccess, environment ->
            VLACCommand.register(dispatcher)
            logger.info(LanguageManager.getString("vlac.system.commands.registered"))
        }
        
        // 将LuckPerms初始化从SERVER_STARTING移动到SERVER_STARTED事件
        // 这确保了LuckPerms已经完全加载并可用
        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            // 延迟1秒钟再初始化，确保LuckPerms完全加载
            server.submitAndJoin {
                try {
                    Thread.sleep(1000) // 等待1秒，确保LuckPerms完全加载
                    logger.info(LanguageManager.getString("vlac.startup.luckperms"))
                    
                    // 检查LuckPerms是否已加载
                    val fabricLoader = FabricLoader.getInstance()
                    if (fabricLoader.isModLoaded("luckperms")) {
                        try {
                            luckPerms = LuckPermsProvider.get()
                            isLuckPermsAvailable = PermissionUtils.init(luckPerms!!)
                            if (isLuckPermsAvailable) {
                                logger.info(LanguageManager.getString("vlac.system.luckperms.connected"))
                            } else {
                                logger.warn(LanguageManager.getString("vlac.system.luckperms.connection_failed"))
                            }
                        } catch (e: Exception) {
                            logger.error("LuckPerms API access failed: ${e.message ?: "Unknown error"}")
                            logger.error("Stack trace: ${e.stackTraceToString()}")
                            isLuckPermsAvailable = false
                        }
                    } else {
                        logger.warn("LuckPerms mod is not loaded. Permission features will be disabled.")
                        isLuckPermsAvailable = false
                    }
                } catch (e: Exception) {
                    logger.error(LanguageManager.getString("vlac.system.luckperms.init_failed", e.message ?: "Unknown error"))
                    logger.error("Stack trace: ${e.stackTraceToString()}")
                    isLuckPermsAvailable = false
                }
            }
        }
        
        // Enable debug mode if configured
        if (VLACConfig.isDebugEnabled()) {
            logger.info(LanguageManager.getString("vlac.system.debug.enabled"))
        }
        
        // Calculate startup time (if needed in the future)
        logger.info(LanguageManager.getString("vlac.system.mod.enabled"))
    }
}