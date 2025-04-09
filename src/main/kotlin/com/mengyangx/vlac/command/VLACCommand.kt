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
                .requires { source -> source.hasPermissionLevel(4) || source.player?.let { PermissionUtils.hasCommandPermission(it, "language") } ?: false }
                .executes { context -> showCurrentLanguage(context) }
                .then(CommandManager.argument("lang", StringArgumentType.word())
                    .suggests { _, builder ->
                        builder.suggest("zh_CN").suggest("en_US")
                        builder.buildFuture()
                    }
                    .executes { context -> 
                        setLanguage(context, StringArgumentType.getString(context, "lang"))
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
            source.sendFeedback({ Text.literal(LanguageManager.getString("command.reload.success")).formatted(Formatting.GREEN) }, true)
            return 1
        } catch (e: Exception) {
            source.sendFeedback({ Text.literal(LanguageManager.getString("command.reload.failed", e.message ?: "Unknown error")).formatted(Formatting.RED) }, true)
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
            source.sendMessage(Text.literal("§a${LanguageManager.getString("command.toggle.enable_success")}"))
            LogManager.info("${LanguageManager.getString("command.toggle.enable_log")} ${source.name}")
        } else {
            source.sendMessage(Text.literal("§c${LanguageManager.getString("command.toggle.disable_success")}"))
            LogManager.info("${LanguageManager.getString("command.toggle.disable_log")} ${source.name}")
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
            source.sendMessage(Text.literal("§a${LanguageManager.getString("command.debug.enable_success")}"))
            LogManager.info("${LanguageManager.getString("command.debug.enable_log")} ${source.name}")
        } else {
            source.sendMessage(Text.literal("§c${LanguageManager.getString("command.debug.disable_success")}"))
            LogManager.info("${LanguageManager.getString("command.debug.disable_log")} ${source.name}")
        }
        
        return 1
    }
    
    /**
     * Set language
     */
    fun setLanguage(context: CommandContext<ServerCommandSource>, lang: String): Int {
        val source = context.source
        
        if (lang != "zh_CN" && lang != "en_US") {
            source.sendMessage(Text.literal("§c${LanguageManager.getString("command.language.unsupported")}: $lang, ${LanguageManager.getString("command.language.available")}: zh_CN, en_US"))
            return 0
        }
        
        // Save to configuration
        VLACConfig.setLanguage(lang)
        
        // Reload language files and apply
        try {
            // Set language
            LanguageManager.setLanguage(lang)
            
            // Force reload language files
            com.mengyangx.vlac.VesperaLumenAntiCheat.reloadLanguage()
            
            source.sendMessage(Text.literal("§a${LanguageManager.getString("command.language.success")}: $lang"))
            LogManager.info("${LanguageManager.getString("command.language.log")} ${source.name} $lang")
        } catch (e: Exception) {
            source.sendMessage(Text.literal("§c${LanguageManager.getString("command.language.failed")}: ${e.message ?: "Unknown error"}"))
            LogManager.error("${LanguageManager.getString("command.language.error")}: ${e.message ?: "Unknown error"}")
        }
        
        return 1
    }
    
    /**
     * Show current language
     */
    fun showCurrentLanguage(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val currentLang = VLACConfig.getLanguage()
        source.sendMessage(Text.literal("§e${LanguageManager.getString("command.language.current")}: $currentLang"))
        return 1
    }
    
    /**
     * Display VLAC status
     */
    fun showStatus(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        val statusEnabled = if (VLACConfig.isEnabled()) "§a${LanguageManager.getString("status.enabled")}" else "§c${LanguageManager.getString("status.disabled")}"
        val statusDebug = if (VLACConfig.isDebugEnabled()) "§a${LanguageManager.getString("status.enabled")}" else "§c${LanguageManager.getString("status.disabled")}"
        val statusLuckPerms = if (VLACConfig.isLuckPermsEnabled()) "§a${LanguageManager.getString("status.enabled")}" else "§c${LanguageManager.getString("status.disabled")}"
        
        source.sendFeedback({ Text.literal("§6----- ${LanguageManager.getString("status.title")} -----") }, false)
        source.sendFeedback({ Text.literal("§e${LanguageManager.getString("status.mod")}: $statusEnabled") }, false)
        source.sendFeedback({ Text.literal("§e${LanguageManager.getString("status.debug")}: $statusDebug") }, false)
        source.sendFeedback({ Text.literal("§e${LanguageManager.getString("status.language")}: §a${VLACConfig.getLanguage()}") }, false)
        source.sendFeedback({ Text.literal("§e${LanguageManager.getString("status.luckperms")}: $statusLuckPerms") }, false)
        
        // Add server runtime information
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
        val totalMemory = runtime.totalMemory() / 1024 / 1024
        
        source.sendFeedback({ Text.literal("§e${LanguageManager.getString("status.memory")}: §a$usedMemory MB §7/ §a$totalMemory MB") }, false)
        
        return 1
    }
    
    /**
     * Display help information
     */
    private fun helpCommand(context: CommandContext<ServerCommandSource>): Int {
        val source = context.source
        
        source.sendFeedback({ Text.literal(LanguageManager.getString("command.help.title")).formatted(Formatting.GOLD) }, false)
        source.sendFeedback({ Text.literal("/vlac reload - ${LanguageManager.getString("command.help.reload")}").formatted(Formatting.YELLOW) }, false)
        source.sendFeedback({ Text.literal("/vlac toggle [true|false] - ${LanguageManager.getString("command.help.toggle")}").formatted(Formatting.YELLOW) }, false)
        source.sendFeedback({ Text.literal("/vlac debug [true|false] - ${LanguageManager.getString("command.help.debug")}").formatted(Formatting.YELLOW) }, false)
        source.sendFeedback({ Text.literal("/vlac language <zh_CN|en_US> - ${LanguageManager.getString("command.help.language")}").formatted(Formatting.YELLOW) }, false)
        source.sendFeedback({ Text.literal("/vlac exempt <player> <add|remove> - ${LanguageManager.getString("command.help.exempt")}").formatted(Formatting.YELLOW) }, false)
        source.sendFeedback({ Text.literal("/vlac status - ${LanguageManager.getString("command.help.status")}").formatted(Formatting.YELLOW) }, false)
        source.sendFeedback({ Text.literal("/vlac help - ${LanguageManager.getString("command.help.help")}").formatted(Formatting.YELLOW) }, false)
        
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
                source.sendFeedback({ Text.literal(LanguageManager.getString("command.exempt.player_not_found", targetName)).formatted(Formatting.RED) }, true)
                return 0
            }
            
            when (state.lowercase()) {
                "add" -> {
                    // TODO: Add exemption
                    source.sendFeedback({ Text.literal(LanguageManager.getString("command.exempt.added", targetName)).formatted(Formatting.GREEN) }, true)
                }
                "remove" -> {
                    // TODO: Remove exemption
                    source.sendFeedback({ Text.literal(LanguageManager.getString("command.exempt.removed", targetName)).formatted(Formatting.GREEN) }, true)
                }
                else -> {
                    source.sendFeedback({ Text.literal(LanguageManager.getString("command.exempt.invalid_state")).formatted(Formatting.RED) }, true)
                }
            }
            return 1
        } catch (e: Exception) {
            source.sendFeedback({ Text.literal(LanguageManager.getString("command.exempt.failed", e.message ?: "Unknown error")).formatted(Formatting.RED) }, true)
            return 0
        }
    }
}