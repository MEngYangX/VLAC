package com.mengyangx.vlac.util

import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.minecraft.server.network.ServerPlayerEntity
import org.slf4j.LoggerFactory

/**
 * 权限工具类，用于处理与LuckPerms相关的权限检查
 */
object PermissionUtils {
    private val logger = LoggerFactory.getLogger("vespera-lumen-anticheat")
    private var luckPermsApi: LuckPerms? = null
    private var isLuckPermsAvailable = false

    /**
     * 初始化LuckPerms API
     * 在模组启动时调用
     */
    fun init() {
        try {
            luckPermsApi = LuckPermsProvider.get()
            isLuckPermsAvailable = true
            logger.info("已成功连接到LuckPerms API")
        } catch (e: Exception) {
            logger.warn("无法连接到LuckPerms API: ${e.message}")
            logger.warn("权限相关功能将被禁用")
            isLuckPermsAvailable = false
        }
    }

    /**
     * 检查玩家是否有指定权限
     *
     * @param player 要检查的玩家
     * @param permission 权限节点
     * @return 如果玩家有权限返回true，否则返回false
     */
    fun hasPermission(player: ServerPlayerEntity, permission: String): Boolean {
        if (!isLuckPermsAvailable) {
            return player.hasPermissionLevel(4) // 如果LuckPerms不可用，则默认使用原版OP权限
        }

        val user = luckPermsApi!!.userManager.getUser(player.uuid)
        return user?.cachedData?.permissionData?.checkPermission(permission)?.asBoolean() ?: false
    }

    /**
     * 检查玩家是否有反作弊豁免权限
     *
     * @param player 要检查的玩家
     * @param checkType 检查类型（例如："飞行检测"、"速度检测"等）
     * @return 如果玩家有豁免权限返回true，否则返回false
     */
    fun hasExemption(player: ServerPlayerEntity, checkType: String): Boolean {
        // 全局豁免权限
        if (hasPermission(player, "vlac.bypass.all")) {
            return true
        }
        
        // 特定检测豁免权限
        val permissionNode = "vlac.bypass.$checkType"
        return hasPermission(player, permissionNode)
    }

    /**
     * 检查玩家是否有使用VLAC管理命令的权限
     *
     * @param player 要检查的玩家
     * @param command 命令名称
     * @return 如果玩家有权限返回true，否则返回false
     */
    fun hasCommandPermission(player: ServerPlayerEntity, command: String): Boolean {
        // 管理员权限
        if (hasPermission(player, "vlac.admin")) {
            return true
        }
        
        // 特定命令权限
        val permissionNode = "vlac.command.$command"
        return hasPermission(player, permissionNode)
    }
    
    /**
     * 获取玩家的所有组
     *
     * @param player 玩家
     * @return 玩家所属组的名称列表
     */
    fun getPlayerGroups(player: ServerPlayerEntity): List<String> {
        if (!isLuckPermsAvailable) {
            return emptyList()
        }
        
        val user = luckPermsApi!!.userManager.getUser(player.uuid) ?: return emptyList()
        return user.getInheritedGroups(user.queryOptions).map { it.name }
    }
} 