package top.alazeprt.aqqbot.qq

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import taboolib.common.platform.function.submit
import taboolib.platform.VelocityPlugin
import top.alazeprt.aonebot.action.GetGroupMemberList
import top.alazeprt.aonebot.event.Listener
import top.alazeprt.aonebot.event.SubscribeBotEvent
import top.alazeprt.aonebot.event.message.GroupMessageEvent
import top.alazeprt.aonebot.event.notice.GroupMemberDecreaseEvent
import top.alazeprt.aqqbot.AQQBot
import top.alazeprt.aqqbot.AQQBot.config
import top.alazeprt.aqqbot.AQQBot.customCommands
import top.alazeprt.aqqbot.AQQBot.isBukkit
import top.alazeprt.aqqbot.AQQBot.isFileStorage
import top.alazeprt.aqqbot.AQQBot.oneBotClient
import top.alazeprt.aqqbot.handler.CommandHandler
import top.alazeprt.aqqbot.handler.InformationHandler
import top.alazeprt.aqqbot.handler.WhitelistHandler
import top.alazeprt.aqqbot.util.AFormatter
import top.alazeprt.aqqbot.util.AI18n.get
import top.alazeprt.aqqbot.util.DBQuery.qqInDatabase
import top.alazeprt.aqqbot.util.DBQuery.removePlayer

class BotListener : Listener {
    @SubscribeBotEvent
    fun onGroupMessage(event: GroupMessageEvent) {
        if (!AQQBot.enableGroups.contains(event.groupId.toString())) {
            return
        }
        var message = ""
        oneBotClient.action(GetGroupMemberList(event.groupId)) { memberList ->
            event.jsonMessage.forEach {
                val jsonObject = it.asJsonObject ?: return@forEach
                when (jsonObject.get("type").asString) {
                    "text" -> message += jsonObject.get("data").asJsonObject.get("text").asString
                    "image" -> message += "[图片]"
                    "at" -> memberList.forEach { member ->
                        if (member.member.userId == jsonObject.get("data").asJsonObject.get("qq").asLong) {
                            message += "@${member.member.nickname}"
                        }
                    }
                }
            }
            val handleInfo = InformationHandler.handle(message, event)
            val handleWl = WhitelistHandler.handle(message, event)
            val handleCommand = CommandHandler.handle(message, event)
            var handleCustom = false
            customCommands.forEach {
                if (it.handle(message, event.senderId.toString(), event.groupId.toString())) {
                    handleCustom = true
                    return@forEach
                }
            }
            val doBroadcast = canForwardMessage(message) != null && !(handleInfo || handleWl || handleCustom || handleCommand) && isBukkit
            if (doBroadcast) {
                Bukkit.broadcastMessage(
                    AFormatter.pluginToChat(
                        get(
                            "game.chat_from_qq", mutableMapOf(
                                "groupId" to event.groupId.toString(),
                                "userName" to event.senderNickName,
                                "message" to replaceCqCode(canForwardMessage(message) ?: return@action)
                            )
                        )
                    )
                )
            }
        }
    }

    @SubscribeBotEvent
    fun onMemberLeave(event: GroupMemberDecreaseEvent) {
        val userId = event.userId.toString()
        if (isFileStorage && !AQQBot.dataMap.containsKey(userId)) {
            return
        } else if (!isFileStorage && qqInDatabase(userId.toLong()) == null) {
            return
        }
        if (isFileStorage) {
            AQQBot.dataMap.forEach { (k, v) ->
                if (k == userId) {
                    AQQBot.dataMap.remove(k)
                    return
                }
            }
        } else {
            val playerName = qqInDatabase(userId.toLong())!!
            removePlayer(userId.toLong(), playerName)
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

    private fun replaceCqCode(str: String): String {
        val regex = """\[CQ:(\w+)([^]]*)]""".toRegex()

        // 定义一个映射，用于将类型替换为对应的内容
        val typeMapping = mapOf(
            "at" to "At someone",  // 例如：CQ:at 替换为 "At someone"
            "qq" to "QQ number",    // 例如：CQ:qq 替换为 "QQ number"
            // 可以根据实际需要继续添加其他类型的映射
        )

        // 替换过程
        return str.replace(regex) { matchResult ->
            val type = matchResult.groupValues[1]
            val params = matchResult.groupValues[2]

            val paramsMap = HashMap<String, String>()
            params.trimStart(',').split(',').forEach {
                val kv = it.split('=')
                paramsMap[kv[0]] = kv[1]
            }

            val rs = when (type) {
                "face" -> when (paramsMap["id"]) {
                    "0" -> "[惊讶]"
                    "1" -> "[撇嘴]"
                    "2" -> "[色]"
                    "3" -> "[发呆]"
                    "4" -> "[得意]"
                    "5" -> "[流泪]"
                    "6" -> "[害羞]"
                    "7" -> "[闭嘴]"
                    "8" -> "[睡]"
                    "9" -> "[大哭]"
                    "10" -> "[尴尬]"
                    "11" -> "[发怒]"
                    "12" -> "[调皮]"
                    "13" -> "[呲牙]"
                    "14" -> "[微笑]"
                    "15" -> "[难过]"
                    "16" -> "[酷]"
                    "18" -> "[撒娇]"
                    "19" -> "[呕]"
                    "20" -> "[撇嘴笑]"
                    "21" -> "[可爱]"
                    "22" -> "[白眼]"
                    "23" -> "[傲慢]"
                    "24" -> "[饥饿]"
                    "25" -> "[困]"
                    "26" -> "[惊恐]"
                    "27" -> "[流汗]"
                    "28" -> "[憨笑]"
                    "29" -> "[悠闲]"
                    "30" -> "[奋斗]"
                    "31" -> "[咒骂]"
                    "32" -> "[疑惑]"
                    "33" -> "[嘘]"
                    "34" -> "[晕]"
                    "35" -> "[折磨]"
                    "36" -> "[衰]"
                    "37" -> "[骷髅]"
                    "38" -> "[敲打]"
                    "39" -> "[再见]"
                    "41" -> "[发抖]"
                    "42" -> "[爱情]"
                    "43" -> "[跳跳]"
                    "46" -> "[猪头]"
                    "49" -> "[拥抱]"
                    "53" -> "[蛋糕]"
                    "54" -> "[闪电]"
                    "55" -> "[炸弹]"
                    "56" -> "[刀]"
                    "57" -> "[足球]"
                    "59" -> "[便便]"
                    "60" -> "[咖啡]"
                    "61" -> "[饭]"
                    "62" -> "[药丸]"
                    "63" -> "[玫瑰]"
                    "64" -> "[凋谢]"
                    "66" -> "[爱心]"
                    "67" -> "[心碎]"
                    "69" -> "[礼物]"
                    "72" -> "[邮件]"
                    "74" -> "[太阳]"
                    "75" -> "[月亮]"
                    "76" -> "[赞]"
                    "77" -> "[踩]"
                    "78" -> "[握手]"
                    "79" -> "[胜利]"
                    "85" -> "[飞吻]"
                    "86" -> "[怄火]"
                    "89" -> "[西瓜]"
                    "90" -> "[雨]"
                    "91" -> "[多云]"
                    "96" -> "[冷汗]"
                    "97" -> "[擦汗]"
                    "98" -> "[抠鼻]"
                    "99" -> "[鼓掌]"
                    "100" -> "[糗大了]"
                    "101" -> "[坏笑]"
                    "102" -> "[左哼哼]"
                    "103" -> "[右哼哼]"
                    "104" -> "[哈欠]"
                    "105" -> "[鄙视]"
                    "106" -> "[委屈]"
                    "107" -> "[快哭了]"
                    "108" -> "[阴脸]"
                    "109" -> "[左亲亲]"
                    "110" -> "[吓]"
                    "111" -> "[可怜]"
                    "112" -> "[菜刀]"
                    "114" -> "[篮球]"
                    "115" -> "[乒乓]"
                    "116" -> "[吻]"
                    "117" -> "[瓢虫]"
                    "123" -> "[OK]"
                    "124" -> "[转圈]"
                    "172" -> "[眨眼睛]"
                    "173" -> "[泪奔]"
                    "174" -> "[无奈]"
                    "175" -> "[卖萌]"
                    "176" -> "[小纠结]"
                    "177" -> "[吐血]"
                    "178" -> "[斜眼笑]"
                    "179" -> "[doge]"
                    "180" -> "[傻眼]"
                    "181" -> "[戳戳]"
                    "182" -> "[笑哭]"
                    "183" -> "[我最美]"
                    else -> "[未知表情]"
                }
                "video" -> "[视频]"
                "at" -> "@${if (paramsMap["qq"] == "all") "全体成员" else paramsMap["name"] ?: paramsMap["qq"]}"
                "rps" -> "[猜拳]"
                "dice" -> "[掷骰子]"
                "anonymous" -> "[匿名消息]"
                "share" -> "[链接分享]"
                "contact" -> "[推荐联系人]"
                "location" -> "[位置分享]"
                "image" -> "[图片]"
                "reply" -> "[回复]"
                "redbag" -> "[QQ红包]"
                "forward" -> "[合并转发]"
                "xml" -> "[卡片消息]"
                "json" -> "[卡片消息]"
                else -> "[未知内容]"
            }
            rs
        }
    }

    private fun canForwardMessage(message: String): String? {
        if (!config.getBoolean("chat.group_to_server.enable")) {
            return null
        }
        if (config.getStringList("chat.group_to_server.prefix").contains("")) {
            return formatter.regexFilter(config.getStringList("chat.group_to_server.filter"), message)
        }
        config.getStringList("chat.group_to_server.prefix").forEach {
            if (message.startsWith(it)) {
                return formatter.regexFilter(config.getStringList("chat.group_to_server.filter"), message.substring(it.length))
            }
        }
        return null
    }

    companion object {
        val formatter = AFormatter()
    }
}