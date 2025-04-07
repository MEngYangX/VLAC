package com.mengyangx.vlac

import com.mengyangx.vlac.config.VLACConfig
import com.mengyangx.vlac.util.PermissionUtils
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory

/**
 * Vespera Lumen AntiCheat主类
 */
object VesperaLumenAntiCheat : ModInitializer {
    private val logger = LoggerFactory.getLogger("vespera-lumen-anticheat")
    
    // 模组ID
    const val MOD_ID = "vespera-lumen-anticheat"

	override fun onInitialize() {
		logger.info("Vespera Lumen AntiCheat 初始化中...")
		
		// 初始化配置
		val configDir = FabricLoader.getInstance().configDir.resolve(MOD_ID)
		VLACConfig.init(configDir)
		
		// 初始化权限系统
		if (VLACConfig.isLuckPermsEnabled()) {
		    try {
		        PermissionUtils.init()
		        logger.info("LuckPerms支持已启用")
		    } catch (e: Exception) {
		        logger.error("初始化LuckPerms支持时出错: ${e.message}")
		        logger.warn("将使用默认权限系统")
		    }
		} else {
		    logger.info("LuckPerms支持已在配置中禁用")
		}
		
		logger.info("Vespera Lumen AntiCheat 已成功初始化!")
	}
}