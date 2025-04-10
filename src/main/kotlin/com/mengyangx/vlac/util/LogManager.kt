package com.mengyangx.vlac.util

import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader

/**
 * Log management utility class
 */
object LogManager {
    private val logger = LoggerFactory.getLogger("VLAC")
    private var logFile: File? = null
    private var logWriter: FileWriter? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    private var server: MinecraftServer? = null
    
    init {
        ServerLifecycleEvents.SERVER_STARTED.register { s -> server = s }
        ServerLifecycleEvents.SERVER_STOPPED.register { server = null }
    }
    
    /**
     * Initialize log system
     */
    fun init() {
        try {
            // Create log directory
            val logDir = File("logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            // Create log file
            val currentDate = SimpleDateFormat("yyyy-MM-dd").format(Date())
            logFile = File(logDir, "vlac-$currentDate.log")
            logWriter = FileWriter(logFile, true)
            
            info("Log system initialized")
        } catch (e: Exception) {
            logger.error("Failed to initialize log system: ${e.message ?: "Unknown error"}")
        }
    }
    
    /**
     * Write log
     */
    private fun writeLog(level: String, message: String) {
        try {
            val timestamp = dateFormat.format(Date())
            val logMessage = "[$timestamp] [$level] $message\n"
            logWriter?.write(logMessage)
            logWriter?.flush()
        } catch (e: Exception) {
            logger.error("Failed to write log: ${e.message ?: "Unknown error"}")
        }
    }
    
    /**
     * Log info message
     */
    fun info(message: String) {
        logger.info(message)
        writeLog("INFO", message)
    }
    
    /**
     * Log warning message
     */
    fun warn(message: String) {
        logger.warn(message)
        writeLog("WARN", message)
    }
    
    /**
     * Log error message
     */
    fun error(message: String) {
        logger.error(message)
        writeLog("ERROR", message)
    }
    
    /**
     * Log debug message
     */
    fun debug(message: String) {
        logger.debug(message)
        writeLog("DEBUG", message)
    }
    
    /**
     * Broadcast message to game
     */
    fun broadcast(message: String) {
        try {
            val currentServer = server
            if (currentServer != null) {
                val text = Text.literal(message)
                currentServer.playerManager.broadcast(text, false)
            }
        } catch (e: Exception) {
            error("Failed to broadcast message: ${e.message ?: "Unknown error"}")
        }
    }
    
    /**
     * Close log system
     */
    fun close() {
        try {
            logWriter?.close()
            logWriter = null
            info("Log system closed")
        } catch (e: Exception) {
            logger.error("Failed to close log system: ${e.message ?: "Unknown error"}")
        }
    }
} 