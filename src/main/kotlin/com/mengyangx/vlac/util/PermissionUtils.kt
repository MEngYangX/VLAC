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
            logger.info("Successfully connected to LuckPerms API")
            
            // Register VLAC permission nodes
            logger.info("Registering VLAC permission nodes...")
            
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
            
            logger.info("VLAC permission nodes registered")
            return true
        } catch (e: Exception) {
            logger.error("Failed to initialize LuckPerms: ${e.message ?: "Unknown error"}")
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
            LogManager.info("Registering VLAC permission nodes...")
            
            // 获取所有VLAC权限
            val allPermissions = VLACPermission.getAllPermissions()
            
            // 记录所有权限节点
            for (permission in allPermissions) {
                val description = VLACPermission.getPermissionDescription(permission)
                LogManager.debug("Permission node: $permission - $description")
                
                try {
                    val node = luckPerms.nodeBuilderRegistry.forPermission()
                        .permission(permission)
                        .value(true)
                        .build()
                    
                    luckPerms.groupManager.getGroup("default")?.data()?.add(node)
                    LogManager.debug("Permission node registered: $permission")
                } catch (e: Exception) {
                    LogManager.debug("Failed to register permission node: ${e.message ?: "Unknown error"}")
                }
            }
            
            LogManager.info("VLAC permission nodes registered")
        } catch (e: Exception) {
            LogManager.error("Failed to register VLAC permission nodes: ${e.message ?: "Unknown error"}")
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
            val lp = luckPerms ?: return false
            val user = lp.userManager.getUser(player.uuid) ?: return false
            
            // 使用查询API检查权限
            val options = QueryOptions.defaultContextualOptions()
            user.getCachedData().getPermissionData(options).checkPermission(permission).asBoolean()
        } catch (e: Exception) {
            logger.error("Failed to check permission: ${e.message ?: "Unknown error"}")
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
            logger.error("Failed to get player groups: ${e.message ?: "Unknown error"}")
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
            val lp = luckPerms ?: return false
            val user = lp.userManager.getUser(player.uuid) ?: return false
            
            // 检查用户的继承节点
            val options = QueryOptions.defaultContextualOptions()
            user.getInheritedGroups(options).any { it.name.equals(group, ignoreCase = true) }
        } catch (e: Exception) {
            logger.error("Failed to check group membership: ${e.message ?: "Unknown error"}")
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
            logger.error("Failed to add player to group: ${e.message ?: "Unknown error"}")
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
            logger.error("Failed to remove player from group: ${e.message ?: "Unknown error"}")
            false
        }
    }
} 