package com.mengyangx.vlac.permission

/**
 * VLAC权限管理类
 * 包含所有权限节点的定义
 */
object VLACPermission {
    // 基本权限
    const val PREFIX = "vlac"
    
    // 管理员权限
    const val ADMIN = "$PREFIX.admin"
    
    // 命令权限
    const val COMMAND_PREFIX = "$PREFIX.command"
    const val COMMAND_RELOAD = "$COMMAND_PREFIX.reload"
    const val COMMAND_TOGGLE = "$COMMAND_PREFIX.toggle"
    const val COMMAND_DEBUG = "$COMMAND_PREFIX.debug"
    const val COMMAND_LANGUAGE = "$COMMAND_PREFIX.language"
    const val COMMAND_STATUS = "$COMMAND_PREFIX.status"
    
    // 豁免权限
    const val BYPASS_PREFIX = "$PREFIX.bypass"
    const val BYPASS_ALL = "$BYPASS_PREFIX.all"
    
    // 通知权限
    const val NOTIFICATION = "$PREFIX.notification"
    
    /**
     * 获取所有权限节点列表
     */
    fun getAllPermissions(): List<String> {
        return listOf(
            ADMIN,
            COMMAND_RELOAD,
            COMMAND_TOGGLE,
            COMMAND_DEBUG,
            COMMAND_LANGUAGE,
            COMMAND_STATUS,
            BYPASS_ALL,
            NOTIFICATION
        )
    }
    
    /**
     * 获取权限描述
     */
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            ADMIN -> "完全访问VLAC的所有功能"
            COMMAND_RELOAD -> "重新加载VLAC配置的权限"
            COMMAND_TOGGLE -> "启用或禁用VLAC的权限"
            COMMAND_DEBUG -> "切换调试模式的权限"
            COMMAND_LANGUAGE -> "设置VLAC语言的权限"
            COMMAND_STATUS -> "查看VLAC状态的权限"
            BYPASS_ALL -> "绕过所有VLAC检测的权限"
            NOTIFICATION -> "接收VLAC违规通知的权限"
            else -> "未知权限"
        }
    }
} 