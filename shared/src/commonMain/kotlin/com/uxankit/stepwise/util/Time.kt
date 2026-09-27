package com.uxankit.stepwise.util

import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

object Time {
    fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())

    fun hourNow(): Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour

    fun dateOf(millis: Long): LocalDate =
        Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date

    fun startOfWeek(date: LocalDate): LocalDate = date.minus(DatePeriod(days = date.dayOfWeek.ordinal))

    fun weekOf(date: LocalDate): List<LocalDate> {
        val monday = startOfWeek(date)
        return (0 until 7).map { monday.plus(DatePeriod(days = it)) }
    }

    fun daysBetween(from: LocalDate, to: LocalDate): Int = (to.toEpochDays() - from.toEpochDays()).toInt()
}

object Ids {
    private const val ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

    fun step(): String = (1..12).map { ALPHABET[Random.nextInt(ALPHABET.length)] }.joinToString("")
}

object DateText {
    private val weekdays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    private val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )

    fun weekdayShort(day: DayOfWeek): String = weekdays[day.ordinal].take(3)

    fun weekdayLetter(day: DayOfWeek): String = weekdays[day.ordinal].take(1)

    /** "Sunday, 27 September" */
    fun long(date: LocalDate): String =
        "${weekdays[date.dayOfWeek.ordinal]}, ${date.day} ${months[date.month.ordinal]}"

    /** "Fri, 2 Oct" */
    fun short(date: LocalDate): String =
        "${weekdayShort(date.dayOfWeek)}, ${date.day} ${months[date.month.ordinal].take(3)}"

    /** Calm, relative wording for deadlines. Never "overdue". */
    fun due(date: LocalDate, today: LocalDate = Time.today()): String = when (Time.daysBetween(today, date)) {
        0 -> "Due today"
        1 -> "Due tomorrow"
        in 2..6 -> "Due ${weekdays[date.dayOfWeek.ordinal]}"
        else -> "Due ${short(date)}"
    }

    fun added(createdAt: Long, now: Long = Time.nowMillis(), today: LocalDate = Time.today()): String {
        if (now - createdAt < 5 * 60_000L) return "Added just now"
        val days = Time.daysBetween(Time.dateOf(createdAt), today)
        return when {
            days <= 0 -> "Added today"
            days == 1 -> "Added yesterday"
            days < 7 -> "Added $days days ago"
            days < 14 -> "Added last week"
            else -> "Added ${short(Time.dateOf(createdAt))}"
        }
    }

    fun greeting(hour: Int = Time.hourNow()): String = when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
}
