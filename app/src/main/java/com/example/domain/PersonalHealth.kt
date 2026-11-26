package com.example.domain

import java.util.Locale
import kotlin.math.roundToInt

data class PersonalProfile(
    val ageYears: Int = 14,
    val heightCm: Int = 172,
    val weightKg: Int = 48
) {
    val heightMeters: Float
        get() = (heightCm.coerceAtLeast(100)) / 100f

    val bmiValue: Float
        get() = weightKg.toFloat() / (heightMeters * heightMeters)

    val bmiFormatted: String
        get() = String.format(Locale("sr", "RS"), "%.1f", bmiValue)

    val recommendedWaterLitersFormatted: String
        get() {
            val liters = ((weightKg * 0.035f) + 0.3f).coerceIn(1.5f, 3.2f)
            return String.format(Locale("sr", "RS"), "%.1f L", liters)
        }

    val recommendedSleepLabel: String
        get() = if (ageYears < 18) "8–10 sati sna" else "7–9 sati sna"

    val isTeen: Boolean
        get() = ageYears in 10..17

    val growthSummarySr: String
        get() = when {
            isTeen && bmiValue < 18.5f ->
                "Sa $ageYears godina, $heightCm cm i $weightKg kg imaš vitku građu u periodu aktivnog rasta. Fokus treninga treba da bude na pravilnoj formi, jačanju mišića i stabilnosti — uz redovnu, hranljivu ishranu i dovoljno sna, bez ikakvih dijeta za mršavljenje."
            isTeen ->
                "Sa $ageYears godina ($heightCm cm, $weightKg kg) tvoje telo se intenzivno razvija. Trening sopstvenom težinom sa kontrolisanim pokretima, dobrim snom i hidratacijom gradi trajnu snagu i zdravo držanje."
            bmiValue < 18.5f ->
                "Sa $heightCm cm i $weightKg kg fokusiraj se na postepeno jačanje mišića, kvalitetne obroke bogate proteinima i dobrim ugljenim hidratima, kao i redovan odmor između treninga."
            else ->
                "Tvoj lični plan kombinuje kontrolisanu tehniku, postepen napredak i redovan oporavak prilagođen tvojim merama ($heightCm cm, $weightKg kg)."
        }

    val bmiNoteSr: String
        get() = if (isTeen) {
            "Indeks mase (ITG/BMI): $bmiFormatted — Kod uzrasta ispod 18 godina ovaj broj se uvek tumači prema krivama rasta i razvoja, a ne kao tabela za odrasle. Tvoj glavni pokazatelj napretka su energija, dobra forma i postepena snaga."
        } else {
            "Indeks telesne mase (BMI): $bmiFormatted — Orijenatacioni pokazatelj uz praćenje obima treninga i opšteg osećaja."
        }

    val safeRepAdviceSr: String
        get() = if (isTeen) {
            "Preporučeni ritam za tvoj uzrast: 3–4 treninga nedeljno umerenim tempom, fokus na čistoj tehnici (⚡ Sporo ili ⚡⚡ Srednji ritam) i bar 1–2 dana aktivnog odmora."
        } else {
            "Prilagodi tempo svom osećaju i uvek završi seriju sa 1–2 ponavljanja rezerve uz očuvanu formu."
        }
}

data class DailyHealthChecklist(
    val dateKey: String = "",
    val waterDone: Boolean = false,
    val mealsDone: Boolean = false,
    val sleepDone: Boolean = false,
    val warmupDone: Boolean = false
) {
    val completedCount: Int
        get() = listOf(waterDone, mealsDone, sleepDone, warmupDone).count { it }
}

data class HealthPrinciple(
    val id: String,
    val emoji: String,
    val title: String,
    val subtitle: String,
    val detail: String,
    val isOrangeHighlight: Boolean = false
)

object PersonalHealthAdvisor {
    fun buildPrinciples(profile: PersonalProfile): List<HealthPrinciple> {
        return listOf(
            HealthPrinciple(
                id = "growth_strength",
                emoji = "💪",
                title = "Snaga i pravilan razvoj pre svega",
                subtitle = if (profile.isTeen) "Prilagođeno za ${profile.ageYears} god. (${profile.heightCm} cm • ${profile.weightKg} kg)" else "Održiv napredak",
                detail = if (profile.isTeen) {
                    "Sa ${profile.ageYears} godina telo gradi kosti, tetive i mišiće. Vežbe sopstvenom težinom (sklekovi, čučnjevi, plank, iskoraci) su najbezbedniji način da ojačaš bez opterećenja kičme tegovima."
                } else {
                    "Redovni treninzi sa sopstvenom težinom jačaju zglobove, srce i mišiće uz minimalan rizik od povreda."
                },
                isOrangeHighlight = true
            ),
            HealthPrinciple(
                id = "nutrition_fuel",
                emoji = "🥗",
                title = "Energija i ishrana (bez preskakanja obroka)",
                subtitle = if (profile.bmiValue < 18.5f) "Podrška za vitku građu (${profile.weightKg} kg)" else "Uravnotežen unos",
                detail = if (profile.bmiValue < 18.5f) {
                    "Za ${profile.heightCm} cm i ${profile.weightKg} kg važno je da jedeš dovoljno kvalitetne hrane: 3 puna obroka i 1–2 užine dnevno. Uključi proteine (jaja, meso, riba, sir, jogurt, pasulj), složene ugljene hidrate (ovas, pirinač, krompir, integralni hleb) i zdrave masti (orasi, maslinovo ulje) kako bi telo imalo materijala za rast i snagu."
                } else {
                    "Kombinuj izvore proteina, povrće, voće i celovite žitarice za stabilnu energiju tokom celog dana."
                }
            ),
            HealthPrinciple(
                id = "sleep_recovery",
                emoji = "😴",
                title = "San i dani oporavka",
                subtitle = profile.recommendedSleepLabel,
                detail = "Mišići ne rastu dok radiš sklekove, već dok spavaš i odmaraš. Uz ${profile.recommendedSleepLabel} i 1–2 dana pauze nedeljno imaćeš više snage i bolji fokus u školi i sportu."
            ),
            HealthPrinciple(
                id = "form_joints",
                emoji = "🛡️",
                title = "Čuvanje zglobova i pravilna tehnika",
                subtitle = "Kvalitet > brzina",
                detail = "Ako osetiš oštar bol u kolenima, ramenima ili donjim leđima, odmah prekini seriju. Uvek zagrej zglobove 5 minuta pre vežbanja i drži leđa ravno tokom sklekova i planka.",
                isOrangeHighlight = true
            ),
            HealthPrinciple(
                id = "hydration",
                emoji = "💧",
                title = "Pametna hidratacija",
                subtitle = "Preporuka oko ${profile.recommendedWaterLitersFormatted} dnevno",
                detail = "Pij vodu u manjim gutljajima pre, tokom pauza i posle treninga. Dobra hidratacija sprečava umor, glavobolju i grčeve u mišićima."
            )
        )
    }
}
