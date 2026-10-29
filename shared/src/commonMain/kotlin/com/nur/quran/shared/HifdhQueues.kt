package com.nur.quran.shared

enum class HifdhQueue {
    SABAQ,
    SABQI,
    MANZIL
}

object HifdhQueues {
    fun classify(
        key: String,
        history: Map<String, HifdhHistoryEntry>,
        sabqiSurahId: Int?
    ): HifdhQueue {
        val entry = history[key]
        val totalReviews = entry?.card?.reps ?: 0
        val state = entry?.card?.state ?: 0
        if (entry == null || entry.card == null || state == FsrsState.NEW || state == FsrsState.LEARNING || totalReviews < 3) {
            return HifdhQueue.SABAQ
        }
        if (sabqiSurahId != null && key.substringBefore(":").toIntOrNull() == sabqiSurahId) {
            return HifdhQueue.SABQI
        }
        return HifdhQueue.MANZIL
    }

    fun splitQueue(
        history: Map<String, HifdhHistoryEntry>,
        sabqiSurahId: Int?
    ): Map<HifdhQueue, List<String>> {
        val sabaq = mutableListOf<String>()
        val sabqi = mutableListOf<String>()
        val manzil = mutableListOf<String>()
        for (key in history.keys) {
            when (classify(key, history, sabqiSurahId)) {
                HifdhQueue.SABAQ -> sabaq.add(key)
                HifdhQueue.SABQI -> sabqi.add(key)
                HifdhQueue.MANZIL -> manzil.add(key)
            }
        }
        return mapOf(
            HifdhQueue.SABAQ to sabaq,
            HifdhQueue.SABQI to sabqi,
            HifdhQueue.MANZIL to manzil
        )
    }

    fun dueInQueue(
        keys: List<String>,
        history: Map<String, HifdhHistoryEntry>,
        nowMs: Long
    ): List<String> {
        return keys.filter { key ->
            val entry = history[key]
            entry == null || entry.card == null || entry.card.isDue(nowMs)
        }
    }

    fun randomQueuePick(
        keys: List<String>,
        seed: Long = System.currentTimeMillis()
    ): String? {
        if (keys.isEmpty()) return null
        return keys[kotlin.random.Random(seed).nextInt(keys.size)]
    }

    fun queueLabel(queue: HifdhQueue): String = when (queue) {
        HifdhQueue.SABAQ -> "Sabaq (New)"
        HifdhQueue.SABQI -> "Sabqi (Recent)"
        HifdhQueue.MANZIL -> "Manzil (Old)"
    }
}
