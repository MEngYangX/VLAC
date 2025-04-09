package com.mengyangx.vlac.util

import com.mengyangx.vlac.permission.VLACPermission
import net.luckperms.api.LuckPerms
import net.luckperms.api.LuckPermsProvider
import net.luckperms.api.model.user.User
import net.luckperms.api.node.Node
import net.minecraft.server.network.ServerPlayerEntity
import com.mengyangx.vlac.util.LogManager
import net.luckperms.api.platform.PlayerAdapter
import net.minecraft.entity.player.PlayerEntity
import org.slf4j.LoggerFactory
import net.luckperms.api.node.NodeType
import net.luckperms.api.node.types.PermissionNode
import net.luckperms.api.query.QueryOptions
import net.luckperms.api.context.DefaultContextKeys
import net.luckperms.api.context.ImmutableContextSet

/**
 * Permission utility class for handling LuckPerms-related permission checks
 */
object PermissionUtils {
    private val logger = LoggerFactory.getLogger("VLAC")
    private var luckPerms: LuckPerms? = null
    private var isLuckPermsAvailable = false

    /**
     * Initialize LuckPerms API
     * Called when the mod starts
     * 
     * @return Whether initialization was successful
     */
    fun init(luckPerms: net.luckperms.api.LuckPerms): Boolean {
        try {
            this.luckPerms = luckPerms
            logger.info(LanguageManager.getString("system.luckperms.connected"))
            
            // Register VLAC permission nodes
            logger.info(LanguageManager.getString("permission.registering"))
            
            // Create and register permission nodes
            val vlacAdmin = luckPerms.nodeBuilderRegistry.forPermission()
                .permission("vlac.admin")
                .build()
            
            val vlacDebug = luckPerms.nodeBuilderRegistry.forPermission()
                .permission("vlac.debug")
                .build()
            
            val vlacView = luckPerms.nodeBuilderRegistry.forPermission()
                .permission("vlac.view")
                .build()
            
            val vlacExempt = luckPerms.nodeBuilderRegistry.forPermission()
                .permission("vlac.exempt")
                .build()
            
            // Register permissions with default group
            val defaultGroup = luckPerms.groupManager.getGroup("default")
            if (defaultGroup != null) {
                defaultGroup.data().add(vlacView)
                luckPerms.groupManager.saveGroup(defaultGroup)
            }
            
            logger.info(LanguageManager.getString("permission.registered"))
            return true
        } catch (e: Exception) {
            logger.error(LanguageManager.getString("permission.init_failed", e.message))
            return false
        }
    }
    
    /**
     * Check if LuckPerms plugin is loaded
     */
    private fun isLuckPermsPluginLoaded(): Boolean {
        return try {
            // Try to check if LuckPerms exists via FabricLoader
            val fabricLoader = Class.forName("net.fabricmc.loader.api.FabricLoader")
            val instance = fabricLoader.getMethod("getInstance").invoke(null)
            val isModLoadedMethod = fabricLoader.getMethod("isModLoaded", String::class.java)
            isModLoadedMethod.invoke(instance, "luckperms") as Boolean
        } catch (e: Exception) {
            // If exception occurs, try to directly use LuckPermsProvider
            try {
                LuckPermsProvider.get()
                true
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Register VLAC permissions to LuckPerms
     */
    private fun registerVLACPermissions() {
        val luckPerms = luckPerms ?: return
        
        try {
            LogManager.info(LanguageManager.getString("permission.registering"))
            
            // 获取所有VLAC权限
            val allPermissions = VLACPermission.getAllPermissions()
            
            // 记录所有权限节点
            for (permission in allPermissions) {
                val description = VLACPermission.getPermissionDescription(permission)
                LogManager.debug(LanguageManager.getString("permission.node_info", permission, description))
                
                try {
                    val node = luckPerms.nodeBuilderRegistry.forPermission()
                        .permission(permission)
                        .value(true)
                        .build()
                    
                    luckPerms.groupManager.getGroup("default")?.data()?.add(node)
                    LogManager.debug(LanguageManager.getString("permission.node_registered", permission))
                } catch (e: Exception) {
                    LogManager.debug(LanguageManager.getString("permission.node_register_failed", e.message))
                }
            }
            
            LogManager.info(LanguageManager.getString("permission.registered"))
        } catch (e: Exception) {
            LogManager.error(LanguageManager.getString("permission.register_failed", e.message))
        }
    }
    
    /**
     * Check if a player has a specific permission
     *
     * @param player Player to check
     * @param permission Permission node
     * @return True if the player has permission, false otherwise
     */
    fun hasPermission(player: PlayerEntity, permission: String): Boolean {
        if (player !is ServerPlayerEntity) return false
        
        return try {
            val user = luckPerms?.userManager?.getUser(player.uuid) ?: return false
            val node = luckPerms?.nodeBuilderRegistry?.forPermission()
                ?.permission(permission)
                ?.build() ?: return false
            user.data().contains(node, net.luckperms.api.node.NodeEqualityPredicate.EXACT).asBoolean()
        } catch (e: Exception) {
            logger.error(LanguageManager.getString("permission.check_failed", e.message))
            false
        }
    }
    
    /**
     * Get LuckPerms user object
     */
    private fun getUser(player: ServerPlayerEntity): User? {
        if (!isLuckPermsAvailable || luckPerms == null) {
            return null
        }
        
        return luckPerms!!.userManager.getUser(player.uuid)
    }

    /**
     * Check if a player has anticheat exemption permission
     *
     * @param player Player to check
     * @param checkType Check type (e.g., "fly", "speed", etc.)
     * @return True if the player has exemption permission, false otherwise
     */
    fun hasExemption(player: ServerPlayerEntity, checkType: String): Boolean {
        // Global exemption permission
        if (hasPermission(player, VLACPermission.BYPASS_ALL)) {
            return true
        }
        
        // Specific check exemption permission
        val permissionNode = "${VLACPermission.BYPASS_PREFIX}.$checkType"
        return hasPermission(player, permissionNode)
    }

    /**
     * Check if a player has permission to use VLAC management commands
     *
     * @param player Player to check
     * @param command Command name
     * @return True if the player has permission, false otherwise
     */
    fun hasCommandPermission(player: ServerPlayerEntity, command: String): Boolean {
        // Admin permission
        if (hasPermission(player, VLACPermission.ADMIN)) {
            return true
        }
        
        // Specific command permission
        val permissionNode = "${VLACPermission.COMMAND_PREFIX}.$command"
        return hasPermission(player, permissionNode)
    }
    
    /**
     * Get all groups a player belongs to
     *
     * @param player Player
     * @return List of group names the player belongs to
     */
    fun getPlayerGroups(player: PlayerEntity): List<String> {
        if (player !is ServerPlayerEntity) return emptyList()
        
        return try {
            val user = luckPerms?.userManager?.getUser(player.uuid) ?: return emptyList()
            user.data().toCollection().asSequence()
                .filter { it.type == NodeType.INHERITANCE }
                .map { it.key.substring(6) } // Remove "group." prefix
                .toList()
        } catch (e: Exception) {
            logger.error(LanguageManager.getString("permission.get_player_groups_failed", e.message))
            emptyList()
        }
    }

    /**
     * Check if a player is exempt from VLAC
     *
     * @param player Player to check
     * @return True if the player is exempt, false otherwise
     */
    fun isExempt(player: PlayerEntity): Boolean {
        return hasPermission(player, "vlac.exempt")
    }

    /**
     * Check if a player is in a specific group
     *
     * @param player Player to check
     * @param group Group name
     * @return True if the player is in the group, false otherwise
     */
    fun isInGroup(player: PlayerEntity, group: String): Boolean {
        if (player !is ServerPlayerEntity) return false
        
        return try {
            val user = luckPerms?.userManager?.getUser(player.uuid) ?: return false
            val node = luckPerms?.nodeBuilderRegistry?.forInheritance()
                ?.group(group)
                ?.build() ?: return false
            user.data().contains(node, net.luckperms.api.node.NodeEqualityPredicate.EXACT).asBoolean()
        } catch (e: Exception) {
            logger.error(LanguageManager.getString("permission.group_check_failed", e.message))
            false
        }
    }

    /**
     * Add a player to a specific group
     *
     * @param player Player to add
     * @param group Group name
     * @return True if the player was added to the group, false otherwise
     */
    fun addToGroup(player: PlayerEntity, group: String): Boolean {
        if (player !is ServerPlayerEntity) return false
        
        return try {
            val user = luckPerms?.userManager?.getUser(player.uuid) ?: return false
            val node = luckPerms?.nodeBuilderRegistry?.forInheritance()
                ?.group(group)
                ?.build()
            
            if (node != null) {
                user.data().add(node)
                luckPerms?.userManager?.saveUser(user)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            logger.error(LanguageManager.getString("permission.add_to_group_failed", e.message))
            false
        }
    }

    /**
     * Remove a player from a specific group
     *
     * @param player Player to remove
     * @param group Group name
     * @return True if the player was removed from the group, false otherwise
     */
    fun removeFromGroup(player: PlayerEntity, group: String): Boolean {
        if (player !is ServerPlayerEntity) return false
        
        return try {
            val user = luckPerms?.userManager?.getUser(player.uuid) ?: return false
            val node = luckPerms?.nodeBuilderRegistry?.forInheritance()
                ?.group(group)
                ?.build()
            
            if (node != null) {
                user.data().remove(node)
                luckPerms?.userManager?.saveUser(user)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            logger.error(LanguageManager.getString("permission.remove_from_group_failed", e.message))
            false
        }
    }
} 