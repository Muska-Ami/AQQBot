package top.alazeprt.aqqbot.handler

import me.lucko.spark.api.statistic.StatisticWindow
import top.alazeprt.aqqbot.AQQBot
import top.alazeprt.aqqbot.AQQBot.spark
import kotlin.math.round

class InformationHandler {
    companion object {
        fun getTPS(groupId: Long) {
            if(spark == null) {
                AQQBot.oneBotClient.bot.sendGroupMsg(groupId,
                    "服务器尚未安装spark插件, 无法获取TPS! 请联系服务器管理员!", true)
                return
            } else {
                val tps = spark?.tps()
                val tps5Secs = roundTPS(tps?.poll(StatisticWindow.TicksPerSecond.SECONDS_5)?: -1.0)
                val tps10Secs = roundTPS(tps?.poll(StatisticWindow.TicksPerSecond.SECONDS_10)?: -1.0)
                val tps1Min = roundTPS(tps?.poll(StatisticWindow.TicksPerSecond.MINUTES_1)?: -1.0)
                val tps5Min = roundTPS(tps?.poll(StatisticWindow.TicksPerSecond.MINUTES_5)?: -1.0)
                val tps15Min = roundTPS(tps?.poll(StatisticWindow.TicksPerSecond.MINUTES_15)?: -1.0)
                AQQBot.oneBotClient.bot.sendGroupMsg(groupId,
                    "服务器TPS: $tps5Secs, $tps10Secs, $tps1Min, $tps5Min, $tps15Min", true)
            }

        }

        private fun roundTPS(tps: Double): String {
            return if (tps > 20) {
                String.format("%.2f", tps.toInt().toDouble())
            } else {
                String.format("%.2f", tps)
            }
        }
    }
}