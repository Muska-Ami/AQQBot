package top.alazeprt.aqqbot.handler

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import taboolib.common.platform.function.submit
import taboolib.platform.VelocityPlugin
import top.alazeprt.aonebot.action.GetGroupMemberInfo
import top.alazeprt.aonebot.action.SendGroupMessage
import top.alazeprt.aonebot.action.SetGroupCard
import top.alazeprt.aonebot.event.message.GroupMessageEvent
import top.alazeprt.aonebot.util.GroupRole
import top.alazeprt.aqqbot.AQQBot
import top.alazeprt.aqqbot.AQQBot.config
import top.alazeprt.aqqbot.AQQBot.isBukkit
import top.alazeprt.aqqbot.AQQBot.isFileStorage
import top.alazeprt.aqqbot.debug.ALogger
import top.alazeprt.aqqbot.util.AI18n.get
import top.alazeprt.aqqbot.util.DBQuery.addPlayer
import top.alazeprt.aqqbot.util.DBQuery.playerInDatabase
import top.alazeprt.aqqbot.util.DBQuery.qqInDatabase
import top.alazeprt.aqqbot.util.DBQuery.removePlayer

class WhitelistHandler {
    companion object {
        private fun bind(userId: String, groupId: Long, data: String) {
            val playerName: String
            if (isFileStorage && AQQBot.dataMap.containsKey(userId)) {
                AQQBot.oneBotClient.action(
                    SendGroupMessage(groupId, get("qq.whitelist.already_bind"), true))
                return
            } else if (!isFileStorage && qqInDatabase(userId.toLong()) != null) {
                AQQBot.oneBotClient.action(
                    SendGroupMessage(groupId, get("qq.whitelist.already_bind"), true))
                return
            }
            if (config.getString("whitelist.verify_method")?.uppercase() == "VERIFY_CODE") {
                var name: String? = null
                AQQBot.verifyCodeMap.forEach { (k, v) ->
                    if (v.first == data) {
                        name = k
                        return@forEach
                    }
                }
                if (name == null) {
                    AQQBot.oneBotClient.action(SendGroupMessage(groupId, get("qq.whitelist.verify_code_not_exist"), true))
                    return
                }
                playerName = name!!
            } else {
                playerName = data
                if (!validateName(playerName)) {
                    AQQBot.oneBotClient.action(SendGroupMessage(groupId, get("qq.whitelist.invalid_name"), true))
                    return
                }
            }
            if (isFileStorage) {
                AQQBot.dataMap.values.forEach {
                    if (it == playerName) {
                        AQQBot.oneBotClient.action(SendGroupMessage(groupId, get("qq.whitelist.already_exist"), true))
                        return
                    }
                }
            } else {
                if (playerInDatabase(playerName)) {
                    AQQBot.oneBotClient.action(SendGroupMessage(groupId, get("qq.whitelist.already_exist"), true))
                    return
                }
            }
            if (isFileStorage) AQQBot.dataMap[userId] = playerName
            else addPlayer(userId.toLong(), playerName)
            AQQBot.oneBotClient.action(SendGroupMessage(groupId, get("qq.whitelist.bind_successful"), true))
            ALogger.log("$userId bind $userId to account $playerName")
            if (config.getString("whitelist.verify_method")?.uppercase() == "VERIFY_CODE") {
                AQQBot.verifyCodeMap.remove(playerName)
            }
            if (config.getBoolean("whitelist.change_nickname_on_bind.enable")) {
                AQQBot.oneBotClient.action(GetGroupMemberInfo(groupId, userId.toLong())) {
                    val newName = config.getString("whitelist.change_nickname_on_bind.format")!!
                        .replace("\${playerName}", playerName)
                        .replace("\${qq}", userId)
                        .replace("\${nickName}", it.member.nickname)
                    AQQBot.oneBotClient.action(SetGroupCard(groupId, userId.toLong(), newName))
                }
            }
        }
        
        private fun unbind(userId: String, groupId: Long, playerName: String) {
            if (isFileStorage && !AQQBot.dataMap.containsKey(userId)) {
                AQQBot.oneBotClient.action(SendGroupMessage(groupId, get("qq.whitelist.not_bind"), true))
                return
            } else if (!isFileStorage && qqInDatabase(userId.toLong()) == null) {
                AQQBot.oneBotClient.action(SendGroupMessage(groupId, get("qq.whitelist.not_bind"), true))
                return
            }
            if (isFileStorage) {
                AQQBot.dataMap.forEach { (k, v) ->
                    if (v == playerName && k == userId) {
                        AQQBot.dataMap.remove(k)
                        AQQBot.oneBotClient.action(SendGroupMessage(groupId, get("qq.whitelist.unbind_successful"), true))
                        ALogger.log("$userId unbind $userId to account $playerName")
                        submit {
                            if (isBukkit) {
                                for (player in Bukkit.getOnlinePlayers()) {
                                    if (player.name == playerName) {
                                        player.kickPlayer(get("game.kick_when_unbind"))
                                    }
                                }
                            } else {
                                for (player in VelocityPlugin.getInstance().server.allPlayers) {
                                    if (player.username == playerName) {
                                        player.disconnect(Component.text(get("game.kick_when_unbind")))
                                    }
                                }
                            }
                        }
                        return
                    } else if (k == userId) {
                        AQQBot.oneBotClient.action(SendGroupMessage(groupId, get("qq.whitelist.bind_by_other", mutableMapOf(Pair("name", v))), true))
                        return
                    }
                }
                AQQBot.oneBotClient.action(SendGroupMessage(groupId, get("qq.whitelist.invalid_bind"), true))
            } else {
                if (qqInDatabase(userId.toLong()) != playerName) {
                    AQQBot.oneBotClient.action(SendGroupMessage(groupId, get("qq.whitelist.bind_by_other", mutableMapOf(Pair("name", qqInDatabase(userId.toLong())!!))), true))
                    return
                }
                removePlayer(userId.toLong(), playerName)
                AQQBot.oneBotClient.action(SendGroupMessage(groupId, get("qq.whitelist.unbind_successful"), true))
                ALogger.log("$userId unbind $userId to account $playerName")
                submit {
                    if (isBukkit) {
                        for (player in Bukkit.getOnlinePlayers()) {
                            if (player.name == playerName) {
                                player.kickPlayer(get("game.kick_when_unbind"))
                            }
                        }
                    } else {
                        for (player in VelocityPlugin.getInstance().server.allPlayers) {
                            if (player.username == playerName) {
                                player.disconnect(Component.text(get("game.kick_when_unbind")))
                            }
                        }
                    }
                }
            }
        }

        private fun validateName(name: String): Boolean {
            val regex = "^\\w+\$"
            return name.matches(regex.toRegex())
        }

        fun handle(message: String, event: GroupMessageEvent): Boolean {
            if (!config.getBoolean("whitelist.enable")) {
                return false
            }
            if (message.split(" ").size < 2) return false
            config.getStringList("whitelist.prefix.bind").forEach {
                if (message.lowercase().startsWith(it.lowercase())) {
                    val playerName = message.split(" ")[1]
                    AQQBot.oneBotClient.action(GetGroupMemberInfo(event.groupId, event.senderId)) { sender ->
                        if (message.split(" ").size == 3 && (sender.role == GroupRole.ADMIN || sender.role == GroupRole.OWNER)) {
                            WhitelistAdminHandler.handle(message, event, "bind")
                        } else {
                            bind(event.senderId.toString(), event.groupId, playerName)
                        }
                    }
                    return true
                }
            }
            config.getStringList("whitelist.prefix.unbind").forEach {
                if (message.lowercase().startsWith(it.lowercase())) {
                    val playerName = message.substring(it.length + 1)
                    AQQBot.oneBotClient.action(GetGroupMemberInfo(event.groupId, event.senderId)) { sender ->
                        if (message.split(" ").size == 3 && (sender.role == GroupRole.ADMIN || sender.role == GroupRole.OWNER)) {
                            WhitelistAdminHandler.handle(message, event, "unbind")
                        } else {
                            unbind(event.senderId.toString(), event.groupId, playerName)
                        }
                    }
                    return true
                }
            }
            return false
        }
    }
}