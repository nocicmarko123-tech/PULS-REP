package com.example.domain

enum class SpeedLevel(
    val bolts: String,
    val titleSr: String,
    val shortTitleSr: String,
    val tempoDescriptionSr: String,
    val reminderStyleSr: String,
    val defaultRestSeconds: Int,
    val defaultReminderIntervalMinutes: Int,
    val downSeconds: Int,
    val holdSeconds: Int,
    val upSeconds: Int
) {
    SLOW(
        bolts = "⚡",
        titleSr = "Sporo",
        shortTitleSr = "Sporo",
        tempoDescriptionSr = "Kontrolisana forma • 3s spuštanje",
        reminderStyleSr = "Mirniji podsetnici i duža pauza (75s)",
        defaultRestSeconds = 75,
        defaultReminderIntervalMinutes = 45,
        downSeconds = 3,
        holdSeconds = 1,
        upSeconds = 2
    ),
    STANDARD(
        bolts = "⚡⚡",
        titleSr = "Srednje",
        shortTitleSr = "Srednje",
        tempoDescriptionSr = "Ujednačen tempo • 2s dole / 2s gore",
        reminderStyleSr = "Standardni podsetnici i pauza 45s",
        defaultRestSeconds = 45,
        defaultReminderIntervalMinutes = 30,
        downSeconds = 2,
        holdSeconds = 0,
        upSeconds = 2
    ),
    FAST(
        bolts = "⚡⚡⚡",
        titleSr = "Brzo",
        shortTitleSr = "Brzo",
        tempoDescriptionSr = "Dinamičan tempo • brzi prelaz",
        reminderStyleSr = "Česti i brzi podsetnici, kratka pauza (25s)",
        defaultRestSeconds = 25,
        defaultReminderIntervalMinutes = 15,
        downSeconds = 1,
        holdSeconds = 0,
        upSeconds = 1
    );

    val badgeLabel: String
        get() = "$bolts $titleSr"

    companion object {
        fun fromName(name: String?): SpeedLevel {
            return entries.firstOrNull { it.name == name } ?: STANDARD
        }
    }
}
