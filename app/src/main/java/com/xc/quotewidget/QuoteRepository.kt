package com.xc.quotewidget

import android.content.Context

data class Quote(val text: String, val author: String)

object QuoteRepository {
    private const val PREFS_NAME = "quote_prefs"
    private const val KEY_INDEX  = "current_index"

    private val quotes = listOf(
        Quote("我只知道一件事，那就是我什么都不知道。", "苏格拉底"),
        Quote("生活就像骑自行车，要保持平衡，就得不断前进。", "爱因斯坦"),
        Quote("你所浪费的今天，是昨天死去的人奢望的明天。", "马克·吐温"),
        Quote("人生最重要的不是我们置身何处，而是我们将前往何处。", "歌德"),
        Quote("凡是过去，皆为序章。", "莎士比亚"),
        Quote("把每一个黎明看作生命的开始，把每一个黄昏看作生命的小结。", "约翰·罗斯金"),
        Quote("成功不是终点，失败也不是终结，只有勇气才是永恒。", "丘吉尔"),
        Quote("我思故我在。", "笛卡尔"),
        Quote("知识就是力量。", "培根"),
        Quote("给我一个支点，我就能撬动整个地球。", "阿基米德"),
        Quote("世界上最快乐的事，莫过于为理想而奋斗。", "苏格拉底"),
        Quote("不要等待机会，而要创造机会。", "林肯"),
        Quote("天才是百分之一的灵感，加上百分之九十九的汗水。", "爱迪生"),
        Quote("勇气不是没有恐惧，而是判断出还有比恐惧更重要的东西。", "安布罗斯·雷德穆恩"),
        Quote("人生没有彩排，每天都是直播。", "梁实秋"),
        Quote("所有的胜利，与征服自己的胜利比起来，都是微不足道的。", "拿破仑"),
        Quote("人只有献身社会，才能找出那短暂而有风险的生命的意义。", "爱因斯坦"),
        Quote("你无法左右天气，但你可以改变心情。", "叔本华"),
        Quote("宁可做过，不要错过。", "王尔德"),
        Quote("每个人都是自己命运的建筑师。", "萨鲁斯特"),
        Quote("书籍是人类进步的阶梯。", "高尔基"),
        Quote("我宁愿靠自己的力量，打开我的前途，而不愿求有力者垂青。", "雨果"),
        Quote("即使再黑暗的时刻，也记得开灯。", "邓布利多"),
        Quote("时间不会等人，只有行动才能创造未来。", "卢梭"),
        Quote("乌云遮不住太阳，苦难磨砺意志。", "托尔斯泰"),
        Quote("黑暗中总有一颗星，照亮你前行的路。", "歌德"),
        Quote("在你绝望的时候，别忘了你曾经有多渴望这一切。", "无名氏"),
        Quote("如果你正在经历地狱，那就继续走下去。", "丘吉尔"),
        Quote("世界以痛吻我，要我报之以歌。", "泰戈尔"),
        Quote("生命中最困难的，不是做出选择，而是做了选择之后继续活下去。", "卡缪"),
    )

    fun getCurrentQuote(context: Context): Quote {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val idx = prefs.getInt(KEY_INDEX, 0)
        return quotes[idx % quotes.size]
    }

    fun advanceToNext(context: Context): Quote {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val next = (prefs.getInt(KEY_INDEX, 0) + 1) % quotes.size
        prefs.edit().putInt(KEY_INDEX, next).apply()
        return quotes[next]
    }
}
