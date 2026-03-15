package com.openbiblescholar.data.model

/**
 * Built-in reading plan definitions.
 * These are embedded in the app and require no download.
 */
object ReadingPlans {

    val WHOLE_BIBLE_ONE_YEAR: ReadingPlan = ReadingPlan(
        id = "whole_bible_1yr",
        name = "Bible in One Year",
        description = "Read through the entire Bible in 365 days with balanced OT/NT daily readings.",
        durationDays = 365,
        type = PlanType.WHOLE_BIBLE,
        dailyReadings = buildWholeBiblePlan()
    )

    val NEW_TESTAMENT_90: ReadingPlan = ReadingPlan(
        id = "nt_90",
        name = "New Testament in 90 Days",
        description = "Read through all 27 books of the New Testament in 90 days.",
        durationDays = 90,
        type = PlanType.NEW_TESTAMENT,
        dailyReadings = buildNTPlan()
    )

    val PSALMS_PROVERBS: ReadingPlan = ReadingPlan(
        id = "psalms_proverbs",
        name = "Psalms & Proverbs",
        description = "One Psalm and one Proverbs section daily for 150 days.",
        durationDays = 150,
        type = PlanType.PSALMS_PROVERBS,
        dailyReadings = buildPsalmsPlan()
    )

    val GOSPELS_30: ReadingPlan = ReadingPlan(
        id = "gospels_30",
        name = "The Four Gospels (30 Days)",
        description = "Journey through Matthew, Mark, Luke and John in one month.",
        durationDays = 30,
        type = PlanType.GOSPELS,
        dailyReadings = buildGospelsPlan()
    )

    val ALL_PLANS = listOf(WHOLE_BIBLE_ONE_YEAR, NEW_TESTAMENT_90, PSALMS_PROVERBS, GOSPELS_30)

    // ── Plan builders ─────────────────────────────────────────────────────────

    private fun buildNTPlan(): List<DailyReading> {
        // NT has 260 chapters across 27 books; ~2.9 chapters/day for 90 days
        val ntBooks = listOf(
            "Matthew" to 28, "Mark" to 16, "Luke" to 24, "John" to 21,
            "Acts" to 28, "Romans" to 16, "1 Corinthians" to 16, "2 Corinthians" to 13,
            "Galatians" to 6, "Ephesians" to 6, "Philippians" to 4, "Colossians" to 4,
            "1 Thessalonians" to 5, "2 Thessalonians" to 3, "1 Timothy" to 6,
            "2 Timothy" to 4, "Titus" to 3, "Philemon" to 1, "Hebrews" to 13,
            "James" to 5, "1 Peter" to 5, "2 Peter" to 3, "1 John" to 5,
            "2 John" to 1, "3 John" to 1, "Jude" to 1, "Revelation" to 22
        )

        val allChapters = mutableListOf<Pair<String, Int>>()
        for ((book, chapters) in ntBooks) {
            for (ch in 1..chapters) allChapters.add(book to ch)
        }

        val readings = mutableListOf<DailyReading>()
        val chaptersPerDay = (allChapters.size.toDouble() / 90).toInt().coerceAtLeast(2)

        allChapters.chunked(chaptersPerDay).take(90).forEachIndexed { dayIdx, chunks ->
            val passages = chunks.groupBy { it.first }.map { (book, entries) ->
                val chapters = entries.map { it.second }.sorted()
                PassageRange(book, chapters.first(), chapters.last())
            }
            readings.add(DailyReading(day = dayIdx + 1, passages = passages))
        }
        return readings
    }

    private fun buildPsalmsPlan(): List<DailyReading> {
        return (1..150).map { day ->
            DailyReading(
                day = day,
                passages = listOf(
                    PassageRange("Psalms", day, day),
                    PassageRange("Proverbs", ((day - 1) % 31) + 1, ((day - 1) % 31) + 1)
                ),
                theme = "Praise & Wisdom"
            )
        }
    }

    private fun buildGospelsPlan(): List<DailyReading> {
        val gospelChapters = listOf(
            "Matthew" to 28, "Mark" to 16, "Luke" to 24, "John" to 21
        )
        val allChapters = mutableListOf<Pair<String, Int>>()
        for ((book, chapters) in gospelChapters) {
            for (ch in 1..chapters) allChapters.add(book to ch)
        }
        return allChapters.chunked(3).take(30).mapIndexed { idx, chunks ->
            DailyReading(
                day = idx + 1,
                passages = chunks.map { (book, ch) -> PassageRange(book, ch, ch) }
            )
        }
    }

    private fun buildWholeBiblePlan(): List<DailyReading> {
        // Simplified representative structure — production app loads from JSON asset
        val schedule = mutableListOf<DailyReading>()
        // OT + NT interleaved — simplified to Genesis + Matthew pairing
        // Full 365-day plan would be loaded from assets/reading_plans/whole_bible_1yr.json
        val otBooks = listOf(
            "Genesis" to 50, "Exodus" to 40, "Leviticus" to 27, "Numbers" to 36,
            "Deuteronomy" to 34, "Joshua" to 24, "Judges" to 21, "Ruth" to 4,
            "1 Samuel" to 31, "2 Samuel" to 24, "1 Kings" to 22, "2 Kings" to 25,
            "1 Chronicles" to 29, "2 Chronicles" to 36, "Ezra" to 10, "Nehemiah" to 13,
            "Esther" to 10, "Job" to 42, "Psalms" to 150, "Proverbs" to 31,
            "Ecclesiastes" to 12, "Song of Solomon" to 8, "Isaiah" to 66,
            "Jeremiah" to 52, "Lamentations" to 5, "Ezekiel" to 48, "Daniel" to 12,
            "Hosea" to 14, "Joel" to 3, "Amos" to 9, "Obadiah" to 1, "Jonah" to 4,
            "Micah" to 7, "Nahum" to 3, "Habakkuk" to 3, "Zephaniah" to 3,
            "Haggai" to 2, "Zechariah" to 14, "Malachi" to 4
        )
        val ntBooks = listOf(
            "Matthew" to 28, "Mark" to 16, "Luke" to 24, "John" to 21, "Acts" to 28,
            "Romans" to 16, "1 Corinthians" to 16, "2 Corinthians" to 13, "Galatians" to 6,
            "Ephesians" to 6, "Philippians" to 4, "Colossians" to 4, "1 Thessalonians" to 5,
            "2 Thessalonians" to 3, "1 Timothy" to 6, "2 Timothy" to 4, "Titus" to 3,
            "Philemon" to 1, "Hebrews" to 13, "James" to 5, "1 Peter" to 5,
            "2 Peter" to 3, "1 John" to 5, "2 John" to 1, "3 John" to 1,
            "Jude" to 1, "Revelation" to 22
        )

        val otChapters = mutableListOf<Pair<String, Int>>()
        for ((book, chapters) in otBooks) for (ch in 1..chapters) otChapters.add(book to ch)

        val ntChapters = mutableListOf<Pair<String, Int>>()
        for ((book, chapters) in ntBooks) for (ch in 1..chapters) ntChapters.add(book to ch)

        for (day in 1..365) {
            val passages = mutableListOf<PassageRange>()
            // ~3 OT chapters per day
            val otIdx = ((day - 1) * 3).coerceAtMost(otChapters.size - 1)
            val otSlice = otChapters.drop(otIdx).take(3)
            otSlice.groupBy { it.first }.forEach { (book, entries) ->
                val chs = entries.map { it.second }.sorted()
                passages.add(PassageRange(book, chs.first(), chs.last()))
            }
            // ~0.74 NT chapters per day (NT loops twice roughly)
            val ntIdx = ((day - 1) % ntChapters.size)
            val (ntBook, ntCh) = ntChapters[ntIdx]
            passages.add(PassageRange(ntBook, ntCh, ntCh))

            schedule.add(DailyReading(day = day, passages = passages))
        }
        return schedule
    }
}
