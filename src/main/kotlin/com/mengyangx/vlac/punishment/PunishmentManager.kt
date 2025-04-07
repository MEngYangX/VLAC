package com.mengyangx.vlac.punishment

import com.mengyangx.vlac.config.VLACConfig
import com.mengyangx.vlac.util.PermissionUtils
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.slf4j.LoggerFactory

/**
 * 处罚管理器，负责对检测到的作弊行为执行处罚
 */
object PunishmentManager {
    private val logger = LoggerFactory.getLogger("vespera-lumen-anticheat")
    
    /**
     * 处罚类型枚举
     */
    enum class PunishmentType {
        WARN,    // 警告
        KICK,    // 踢出
        BAN,     // 封禁
        COMMAND  // 执行命令
    }
    
    /**
     * 处理违规行为
     *
     * @param player 违规玩家
     * @param checkType 检测类型
     * @param violation 违规详情
     */
    fun handleViolation(player: ServerPlayerEntity, checkType: String, violation: String) {
        // 检查玩家是否有豁免权限
        if (shouldExempt(player, checkType)) {
            logger.info("玩家 ${player.name.string} 因权限豁免跳过处罚 (检测: $checkType)")
            return
        }
        
        // 记录违规行为
        logger.warn("检测到违规: 玩家=${player.name.string}, 类型=$checkType, 详情=$violation")
        
        // 执行处罚
        // 在实际应用中，这里可以根据配置和违规程度决定处罚类型
        applyPunishment(player, PunishmentType.WARN, "检测到可能的作弊行为: $checkType")
    }
    
    /**
     * 应用处罚
     *
     * @param player 要处罚的玩家
     * @param type 处罚类型
     * @param reason 处罚原因
     */
    private fun applyPunishment(player: ServerPlayerEntity, type: PunishmentType, reason: String) {
        when (type) {
            PunishmentType.WARN -> {
                // 向玩家发送警告消息
                player.sendMessage(Text.of("§c[VLAC] $reason"))
                
                // 通知管理员
                notifyAdmins(player, type, reason)
            }
            PunishmentType.KICK -> {
                // 踢出玩家
                player.networkHandler.disconnect(Text.of("§c[VLAC] 您已被踢出: $reason"))
                
                // 通知管理员
                notifyAdmins(player, type, reason)
            }
            PunishmentType.BAN -> {
                // 这里可以实现封禁逻辑，可能需要调用服务器的API
                // 例如：server.getPlayerManager().getUserBanList().add(new BanEntry(player.getGameProfile().getName()));
                
                // 踢出玩家
                player.networkHandler.disconnect(Text.of("§c[VLAC] 您已被封禁: $reason"))
                
                // 通知管理员
                notifyAdmins(player, type, reason)
            }
            PunishmentType.COMMAND -> {
                // 执行自定义命令
                // 在实际应用中，这里可以从配置中读取要执行的命令
                // 例如：player.getServer().getCommandManager().execute(server.getCommandSource(), command);
            }
        }
    }
    
    /**
     * 通知管理员有关违规行为
     */
    private fun notifyAdmins(player: ServerPlayerEntity, type: PunishmentType, reason: String) {
        val message = "§e[VLAC] 玩家 ${player.name.string} 已受到 ${type.name} 处罚: $reason"
        
        // 向所有有权限的管理员发送消息
        player.server.playerManager.playerList.forEach { admin ->
            if (PermissionUtils.hasPermission(admin, "vlac.notifications")) {
                admin.sendMessage(Text.of(message))
            }
        }
    }
    
    /**
     * 检查玩家是否应该被豁免
     *
     * @param player 要检查的玩家
     * @param checkType 检测类型
     * @return 如果玩家应该被豁免返回true，否则返回false
     */
    private fun shouldExempt(player: ServerPlayerEntity, checkType: String): Boolean {
        // 检查玩家是否有特定豁免权限
        if (PermissionUtils.hasExemption(player, checkType)) {
            return true
        }
        
        // 检查玩家是否在豁免组中
        if (VLACConfig.isLuckPermsEnabled()) {
            val playerGroups = PermissionUtils.getPlayerGroups(player)
            val bypassGroups = VLACConfig.getBypassGroups()
            
            return playerGroups.any { it in bypassGroups }
        }
        
        return false
    }
} 