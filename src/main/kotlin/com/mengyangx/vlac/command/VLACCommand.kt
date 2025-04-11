package com.mengyangx.vlac.command

import com.mengyangx.vlac.config.VLACConfig
import com.mengyangx.vlac.util.LanguageManager
import com.mengyangx.vlac.util.LogManager
import com.mengyangx.vlac.util.PermissionUtils
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.server.command.CommandManager
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.text.DecimalFormat
import java.util.*

/**
 * VLAC command handler class
 */
object VLACCommand {
    /**
     * Register commands
     */
    fun register(dispatcher: CommandDispatcher<ServerCommandSource>) {
        val vlac = CommandManager.literal("vlac")
            .requires { source -> true } // Allow all players to use the base command
            .executes { context -> helpCommand(context) } // Root command shows help
            .then(CommandManager.literal("reload")
                .requires { source -> source.hasPermissionLevel(4) || source.player?.let { PermissionUtils.hasCommandPermission(it, "reload") } ?: false }
                .executes { context -> reloadCommand(context) })
            .then(CommandManager.literal("toggle")
                .requires { source -> source.hasPermissionLevel(4) || source.player?.let { PermissionUtils.hasCommandPermission(it, "toggle") } ?: false }
                .executes { context -> toggleVLAC(context) }
                .then(CommandManager.literal("true")
                    .executes { context -> setVLACEnabled(context, true) })
                .then(CommandManager.literal("false")
                    .executes { context -> setVLACEnabled(context, false) }))
            .then(CommandManager.literal("debug")
                .requires { source -> source.hasPermissionLevel(4) || source.player?.let { PermissionUtils.hasCommandPermission(it, "debug") } ?: false }
                .executes { context -> toggleDebug(context) }
                .then(CommandManager.literal("true")
                    .executes { context -> setDebugEnabled(context, true) })
                .then(CommandManager.literal("false")
                    .executes { context -> setDebugEnabled(context, false) }))
            .then(CommandManager.literal("language")
                .requires { source -> source.hasPermissionLevel(3) || source.player?.let { PermissionUtils.hasCommandPermission(it, "language") } ?: false }
                .executes { context -> showCurrentLanguage(context) }
                .then(CommandManager.literal("auto")
                    .executes { context -> setLanguageAuto(context) })
                .then(CommandManager.argument("lang", StringArgumentType.word())
                    .suggests { _, builder ->
                        builder.suggest("zh_CN").suggest("en_US").suggest("auto")
                        builder.buildFuture()
                    }
                    .executes { context -> 
                        val langArg = StringArgumentType.getString(context, "lang")
                        if (langArg == "auto") {
                            setLanguageAuto(context)
                        } else {
                            setLanguage(context, langArg)
                        }
                    }))
            .then(CommandManager.literal("exempt")
                .requires { source -> source.hasPermissionLevel(4) || source.player?.let { PermissionUtils.hasCommandPermission(it, "exempt") } ?: false }
                .then(CommandManager.argument("player", StringArgumentType.word())
                    .suggests { context, builder ->
                        val source = context.source
                        val players = source.server.playerManager.playerList
                        for (player in players) {
                            builder.suggest(player.name.string)
                        }
                        builder.buildFuture()
                    }
                    .then(CommandManager.argument("state", StringArgumentType.word())
                        .suggests { _, builder ->
                            builder.suggest("add")
                            builder.suggest("remove")
                            builder.buildFuture()
                        }
                        .executes { context -> exemptCommand(context) })))
            .then(CommandManager.literal("help")
                .executes { context -> helpCommand(context) })
            .then(CommandManager.literal("status")
                .requires { source -> true } // Allow all players to view status
                .executes { context -> showStatus(context) })
        
        dispatcher.register(vlac)
        LogManager.info("VLAC commands registered")
    }
    
    /**
     * Reload configuration command
     */
    private fun reloadCommand(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val player = source.player
        
        try {
            // Reload configuration
            LogManager.info("Reloading configuration...")
            VLACConfig.reload()
            
            // Reload language files
            LogManager.info("Reloading language files...")
            com.mengyangx.vlac.VesperaLumenAntiCheat.reloadLanguage()
            
            // Send feedback
            source.sendFeedback({ Text.literal(LanguageManager.getString("vlac.command.reload.success")).formatted(Formatting.GREEN) }, true)
            return 1
        } catch (e: Exception) {
            source.sendFeedback({ Text.literal(LanguageManager.getString("vlac.command.reload.failed", e.message ?: "Unknown error")).formatted(Formatting.RED) }, true)
            return 0
        }
    }
    
    /**
     * Toggle VLAC state
     */
    private fun toggleVLAC(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val currentState = VLACConfig.isEnabled()
        val newState = !currentState
        
        return setVLACEnabled(context, newState)
    }
    
    /**
     * Toggle debug mode
     */
    private fun toggleDebug(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val currentState = VLACConfig.isDebugEnabled()
        val newState = !currentState
        
        return setDebugEnabled(context, newState)
    }
    
    /**
     * Set VLAC enabled state
     */
    fun setVLACEnabled(context: CommandContext<ServerCommandSource>, enabled: Boolean): Int {
        val source = context.source
        VLACConfig.setEnabled(enabled)
        
        if (enabled) {
            source.sendMessage(Text.literal("§a${LanguageManager.getString("vlac.command.toggle.enable_success")}"))
            LogManager.info("${LanguageManager.getString("vlac.command.toggle.enable_log")} ${source.name}")
        } else {
            source.sendMessage(Text.literal("§c${LanguageManager.getString("vlac.command.toggle.disable_success")}"))
            LogManager.info("${LanguageManager.getString("vlac.command.toggle.disable_log")} ${source.name}")
        }
        
        return 1
    }
    
    /**
     * Set debug mode state
     */
    fun setDebugEnabled(context: CommandContext<ServerCommandSource>, enabled: Boolean): Int {
        val source = context.source
        VLACConfig.setDebug(enabled)
        
        if (enabled) {
            source.sendMessage(Text.literal("§a${LanguageManager.getString("vlac.command.debug.enable_success")}"))
            LogManager.info("${LanguageManager.getString("vlac.command.debug.enable_log")} ${source.name}")
        } else {
            source.sendMessage(Text.literal("§c${LanguageManager.getString("vlac.command.debug.disable_success")}"))
            LogManager.info("${LanguageManager.getString("vlac.command.debug.disable_log")} ${source.name}")
        }
        
        return 1
    }
    
    /**
     * Set language to auto mode
     */
    fun setLanguageAuto(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val player = source.player
        
        if (player == null) {
            source.sendFeedback({ Text.literal("§c${LanguageManager.getString("vlac.command.language.player_only")}") }, true)
            return 0
        }
        
        // 在1.21.5中无法直接访问clientLanguageCode，默认使用英文
        val clientLanguageCode = "en_us"
        LanguageManager.setPlayerLanguageAuto(player.uuid, clientLanguageCode)
        val detectedLang = LanguageManager.determineLanguageFromClientCode(clientLanguageCode)
        
        // 使用玩家当前语言显示反馈信息
        val message = LanguageManager.getStringForPlayer(player.uuid, clientLanguageCode, "vlac.command.language.auto_mode")
        source.sendFeedback({ Text.literal("§a$message: $detectedLang") }, true)
        LogManager.info("${LanguageManager.getString("vlac.command.language.log")} ${source.name} (auto:$detectedLang)")
        return 1
    }
    
    /**
     * Set language
     */
    fun setLanguage(context: CommandContext<ServerCommandSource>, lang: String): Int {
        val source = context.source
        val player = source.player
        
        if (lang != "zh_CN" && lang != "en_US") {
            // 根据玩家或控制台显示不同的语言
            if (player != null) {
                // 在1.21.5中无法直接访问clientLanguageCode，默认使用英文
                val clientLanguageCode = "en_us"
                val unsupported = LanguageManager.getStringForPlayer(player.uuid, clientLanguageCode, "vlac.command.language.unsupported")
                val available = LanguageManager.getStringForPlayer(player.uuid, clientLanguageCode, "vlac.command.language.available")
                source.sendFeedback({ Text.literal("§c$unsupported: $lang, $available: zh_CN, en_US, auto") }, true)
            } else {
                source.sendFeedback({ Text.literal("§c${LanguageManager.getString("vlac.command.language.unsupported")}: $lang, ${LanguageManager.getString("vlac.command.language.available")}: zh_CN, en_US") }, true)
            }
            return 0
        }
        
        // 玩家设置语言
        if (player != null) {
            // 为该玩家设置手动语言模式
            // 在1.21.5中无法直接访问clientLanguageCode，默认使用英文
            val clientLanguageCode = "en_us"
            LanguageManager.setPlayerLanguageManual(player.uuid, lang)
            
            val success = LanguageManager.getStringForPlayer(player.uuid, clientLanguageCode, "vlac.command.language.success")
            val mode = LanguageManager.getStringForPlayer(player.uuid, clientLanguageCode, "vlac.command.language.mode_manual")
            source.sendFeedback({ Text.literal("§a$success: $lang ($mode)") }, true)
            LogManager.info("${LanguageManager.getString("vlac.command.language.log")} ${source.name} $lang (manual)")
        }
        // 控制台或命令方块设置全局默认语言
        else {
            // 保存到配置
            VLACConfig.setLanguage(lang)
            
            // 设置默认语言
            LanguageManager.setLanguage(lang)
            
            source.sendFeedback({ Text.literal("§a${LanguageManager.getString("vlac.command.language.success")}: $lang (Server Default)") }, true)
            LogManager.info("${LanguageManager.getString("vlac.command.language.log")} ${source.name} $lang (server default)")
        }
        
        return 1
    }
    
    /**
     * Show current language
     */
    fun showCurrentLanguage(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val player = source.player
        
        if (player != null) {
            // 在1.21.5中无法直接访问clientLanguageCode，默认使用英文
            val clientLanguageCode = "en_us"
            val mode = LanguageManager.getPlayerLanguageMode(player.uuid)
            val currentLang = LanguageManager.getPlayerLanguage(player.uuid)
            
            // 显示当前语言和模式
            val currentKey = LanguageManager.getStringForPlayer(player.uuid, clientLanguageCode, "vlac.command.language.current")
            val modeText = if (mode == LanguageManager.LanguageMode.AUTO) {
                val autoText = LanguageManager.getStringForPlayer(player.uuid, clientLanguageCode, "vlac.command.language.mode_auto")
                "§b$autoText"
            } else {
                val manualText = LanguageManager.getStringForPlayer(player.uuid, clientLanguageCode, "vlac.command.language.mode_manual")
                "§e$manualText"
            }
                
            source.sendFeedback({ Text.literal("§e$currentKey: §a$currentLang ($modeText)") }, false)
        } else {
            // 控制台显示全局默认语言
            val currentLang = LanguageManager.getLanguage()
            source.sendFeedback({ Text.literal("§e${LanguageManager.getString("vlac.command.language.current")}: §a$currentLang (Server Default)") }, false)
        }
        
        return 1
    }
    
    /**
     * Display VLAC status
     */
    fun showStatus(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val statusEnabled = if (VLACConfig.isEnabled()) "§a${LanguageManager.getString("vlac.status.enabled")}" else "§c${LanguageManager.getString("vlac.status.disabled")}"
        val statusDebug = if (VLACConfig.isDebugEnabled()) "§a${LanguageManager.getString("vlac.status.enabled")}" else "§c${LanguageManager.getString("vlac.status.disabled")}"
        val statusLuckPerms = if (VLACConfig.isLuckPermsEnabled()) "§a${LanguageManager.getString("vlac.status.enabled")}" else "§c${LanguageManager.getString("vlac.status.disabled")}"
        
        source.sendFeedback({ Text.literal("§6----- ${LanguageManager.getString("vlac.status.title")} -----") }, false)
        source.sendFeedback({ Text.literal("§e${LanguageManager.getString("vlac.status.mod")}: $statusEnabled") }, false)
        source.sendFeedback({ Text.literal("§e${LanguageManager.getString("vlac.status.debug")}: $statusDebug") }, false)
        source.sendFeedback({ Text.literal("§e${LanguageManager.getString("vlac.status.language")}: §a${VLACConfig.getLanguage()}") }, false)
        source.sendFeedback({ Text.literal("§e${LanguageManager.getString("vlac.status.luckperms")}: $statusLuckPerms") }, false)
        
        // Add server runtime information
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val totalMemory = runtime.totalMemory() / 1024 / 1024
        
        source.sendFeedback({ Text.literal("§e${LanguageManager.getString("vlac.status.memory")}: §a$usedMemory MB §7/ §a$totalMemory MB") }, false)
        
        return 1
    }
    
    /**
     * Display help information
     */
    private fun helpCommand(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val player = source.player
        
        // 根据玩家或控制台选择适当的语言
        if (player != null) {
            // 在1.21.5中无法直接访问clientLanguageCode，默认使用英文
            val clientLanguageCode = "en_us"
            
            // 使用玩家特定的语言
            val title = LanguageManager.getStringForPlayer(player.uuid, clientLanguageCode, "vlac.command.help.title")
            val helpReload = LanguageManager.getStringForPlayer(player.uuid, clientLanguageCode, "vlac.command.help.reload")
            val helpToggle = LanguageManager.getStringForPlayer(player.uuid, clientLanguageCode, "vlac.command.help.toggle")
            val helpDebug = LanguageManager.getStringForPlayer(player.uuid, clientLanguageCode, "vlac.command.help.debug")
            val helpLanguage = LanguageManager.getStringForPlayer(player.uuid, clientLanguageCode, "vlac.command.help.language")
            val helpExempt = LanguageManager.getStringForPlayer(player.uuid, clientLanguageCode, "vlac.command.help.exempt")
            val helpStatus = LanguageManager.getStringForPlayer(player.uuid, clientLanguageCode, "vlac.command.help.status")
            val helpHelp = LanguageManager.getStringForPlayer(player.uuid, clientLanguageCode, "vlac.command.help.help")
            
            source.sendFeedback({ Text.literal(title).formatted(Formatting.GOLD) }, false)
            source.sendFeedback({ Text.literal("/vlac reload - $helpReload").formatted(Formatting.YELLOW) }, false)
            source.sendFeedback({ Text.literal("/vlac toggle [true|false] - $helpToggle").formatted(Formatting.YELLOW) }, false)
            source.sendFeedback({ Text.literal("/vlac debug [true|false] - $helpDebug").formatted(Formatting.YELLOW) }, false)
            source.sendFeedback({ Text.literal("/vlac language <zh_CN|en_US|auto> - $helpLanguage").formatted(Formatting.YELLOW) }, false)
            source.sendFeedback({ Text.literal("/vlac exempt <player> <add|remove> - $helpExempt").formatted(Formatting.YELLOW) }, false)
            source.sendFeedback({ Text.literal("/vlac status - $helpStatus").formatted(Formatting.YELLOW) }, false)
            source.sendFeedback({ Text.literal("/vlac help - $helpHelp").formatted(Formatting.YELLOW) }, false)
        } else {
            // 使用服务器默认语言
            source.sendFeedback({ Text.literal(LanguageManager.getString("vlac.command.help.title")).formatted(Formatting.GOLD) }, false)
            source.sendFeedback({ Text.literal("/vlac reload - ${LanguageManager.getString("vlac.command.help.reload")}").formatted(Formatting.YELLOW) }, false)
            source.sendFeedback({ Text.literal("/vlac toggle [true|false] - ${LanguageManager.getString("vlac.command.help.toggle")}").formatted(Formatting.YELLOW) }, false)
            source.sendFeedback({ Text.literal("/vlac debug [true|false] - ${LanguageManager.getString("vlac.command.help.debug")}").formatted(Formatting.YELLOW) }, false)
            source.sendFeedback({ Text.literal("/vlac language <zh_CN|en_US|auto> - ${LanguageManager.getString("vlac.command.help.language")}").formatted(Formatting.YELLOW) }, false)
            source.sendFeedback({ Text.literal("/vlac exempt <player> <add|remove> - ${LanguageManager.getString("vlac.command.help.exempt")}").formatted(Formatting.YELLOW) }, false)
            source.sendFeedback({ Text.literal("/vlac status - ${LanguageManager.getString("vlac.command.help.status")}").formatted(Formatting.YELLOW) }, false)
            source.sendFeedback({ Text.literal("/vlac help - ${LanguageManager.getString("vlac.command.help.help")}").formatted(Formatting.YELLOW) }, false)
        }
        
        return 1
    }
    
    /**
     * Exempt command handler
     */
    private fun exemptCommand(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val player = source.player
        val targetName = StringArgumentType.getString(context, "player")
        val state = StringArgumentType.getString(context, "state")
        
        try {
            val target = source.server.playerManager.getPlayer(targetName)
            if (target == null) {
                source.sendFeedback({ Text.literal(LanguageManager.getString("vlac.command.exempt.player_not_found", targetName)).formatted(Formatting.RED) }, true)
                return 0
            }
            
            when (state.lowercase()) {
                "add" -> {
                    // TODO: Add exemption
                    source.sendFeedback({ Text.literal(LanguageManager.getString("vlac.command.exempt.added", targetName)).formatted(Formatting.GREEN) }, true)
                }
                "remove" -> {
                    // TODO: Remove exemption
                    source.sendFeedback({ Text.literal(LanguageManager.getString("vlac.command.exempt.removed", targetName)).formatted(Formatting.GREEN) }, true)
                }
                else -> {
                    source.sendFeedback({ Text.literal(LanguageManager.getString("vlac.command.exempt.invalid_state")).formatted(Formatting.RED) }, true)
                }
            }
            return 1
        } catch (e: Exception) {
            source.sendFeedback({ Text.literal(LanguageManager.getString("vlac.command.exempt.failed", e.message ?: "Unknown error")).formatted(Formatting.RED) }, true)
            return 0
        }
    }
}