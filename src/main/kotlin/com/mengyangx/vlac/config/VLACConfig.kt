package com.mengyangx.vlac.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * VLAC模组的配置系统
 */
object VLACConfig {
    private val logger = LoggerFactory.getLogger("vespera-lumen-anticheat")
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private var configDir: Path? = null
    private var configFile: File? = null
    
    // 默认配置
    private var config = JsonObject()
    
    // 权限配置
    private var useLuckPerms = true
    private var bypassGroups = mutableListOf("admin", "mod")

    /**
     * 初始化配置系统
     * 
     * @param configDir 配置目录的路径
     */
    fun init(configDir: Path) {
        this.configDir = configDir
        this.configFile = Paths.get(configDir.toString(), "vlac-config.json").toFile()
        
        if (!Files.exists(configDir)) {
            try {
                Files.createDirectories(configDir)
                logger.info("创建配置目录: $configDir")
            } catch (e: Exception) {
                logger.error("无法创建配置目录: ${e.message}")
                return
            }
        }
        
        loadConfig()
    }
    
    /**
     * 加载配置文件
     */
    private fun loadConfig() {
        if (configFile?.exists() == true) {
            try {
                FileReader(configFile!!).use { reader ->
                    config = gson.fromJson(reader, JsonObject::class.java)
                    logger.info("已加载配置文件")
                    
                    // 读取权限配置
                    if (config.has("permissions")) {
                        val permissionsObj = config.getAsJsonObject("permissions")
                        useLuckPerms = permissionsObj.get("useLuckPerms")?.asBoolean ?: true
                        
                        if (permissionsObj.has("bypassGroups")) {
                            bypassGroups.clear()
                            val bypassGroupsArray = permissionsObj.getAsJsonArray("bypassGroups")
                            bypassGroupsArray.forEach { 
                                bypassGroups.add(it.asString) 
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error("加载配置文件时出错: ${e.message}")
                createDefaultConfig()
            }
        } else {
            createDefaultConfig()
        }
    }
    
    /**
     * 创建默认配置文件
     */
    private fun createDefaultConfig() {
        // 创建基本配置结构
        config = JsonObject()
        
        // 常规配置
        val generalObj = JsonObject()
        generalObj.addProperty("enabled", true)
        generalObj.addProperty("notifyAdmins", true)
        config.add("general", generalObj)
        
        // 权限配置
        val permissionsObj = JsonObject()
        permissionsObj.addProperty("useLuckPerms", true)
        val bypassGroupsArray = gson.toJsonTree(bypassGroups).asJsonArray
        permissionsObj.add("bypassGroups", bypassGroupsArray)
        config.add("permissions", permissionsObj)
        
        // 检测配置
        val detectionsObj = JsonObject()
        
        // 飞行检测
        val flyObj = JsonObject()
        flyObj.addProperty("enabled", true)
        flyObj.addProperty("sensitivity", 2)
        detectionsObj.add("fly", flyObj)
        
        // 速度检测
        val speedObj = JsonObject()
        speedObj.addProperty("enabled", true)
        speedObj.addProperty("maxSpeed", 0.8)
        detectionsObj.add("speed", speedObj)
        
        config.add("detections", detectionsObj)
        
        // 处罚配置
        val punishmentsObj = JsonObject()
        punishmentsObj.addProperty("defaultAction", "warn")
        punishmentsObj.addProperty("warnMessage", "§c[VLAC] 检测到可能的作弊行为")
        punishmentsObj.addProperty("kickMessage", "§c[VLAC] 您因可能的作弊行为被踢出服务器")
        config.add("punishments", punishmentsObj)
        
        saveConfig()
        logger.info("已创建默认配置文件")
    }
    
    /**
     * 保存配置到文件
     */
    fun saveConfig() {
        try {
            FileWriter(configFile!!).use { writer ->
                gson.toJson(config, writer)
            }
            logger.info("已保存配置文件")
        } catch (e: Exception) {
            logger.error("保存配置文件时出错: ${e.message}")
        }
    }
    
    /**
     * 获取是否使用LuckPerms
     */
    fun isLuckPermsEnabled(): Boolean {
        return useLuckPerms
    }
    
    /**
     * 获取豁免组列表
     */
    fun getBypassGroups(): List<String> {
        return bypassGroups
    }
} 