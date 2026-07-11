package com.example.unimove.ui.screens.onboarding

import com.example.unimove.R
import java.util.Locale

data class CollegeOption(
    val name: String,
    val logoRes: Int,
    val aliases: List<String> = emptyList()
)

val collegeOptions = listOf(
    CollegeOption("Kolej Burhanuddin Helmi", R.drawable.logo_kolej_kbh, aliases = listOf("kbh")),
    CollegeOption("Kolej Ibu Zain", R.drawable.logo_kolej_kiz, aliases = listOf("kiz")),
    CollegeOption("Kolej Pendeta Za'ba", R.drawable.logo_kolej_kpz, aliases = listOf("kpz")),
    CollegeOption("Kolej Rahim Kajai", R.drawable.logo_kolej_krk, aliases = listOf("krk")),
    CollegeOption("Kolej Tun Dr Ismail", R.drawable.logo_kolej_ktdi, aliases = listOf("ktdi")),
    CollegeOption("Kolej Keris Mas", R.drawable.logo_kolej_kkm, aliases = listOf("kkm")),
    CollegeOption("Kolej Tun Hussein Onn", R.drawable.logo_kolej_ktho, aliases = listOf("ktho")),
    CollegeOption("Kolej Aminuddin Baki", R.drawable.logo_kolej_kab, aliases = listOf("kab")),
    CollegeOption("Kolej Ungku Omar", R.drawable.logo_kolej_kuo, aliases = listOf("kuo")),
    CollegeOption("Kolej 13", R.drawable.logo_kolej_k13, aliases = listOf("k13")),
    CollegeOption("Kolej Dato' Onn", R.drawable.logo_kolej_kdo, aliases = listOf("kdo")),
    CollegeOption("Kolej Ibu Yusof", R.drawable.logo_kolej_kiy, aliases = listOf("kiy")),
    CollegeOption("Kolej Tun Syed Nasir", R.drawable.logo_kolej_ktsn, aliases = listOf("ktsn")),
    CollegeOption("Other", R.drawable.ic_college)
)

fun resolveCollegeLogoRes(collegeName: String?): Int {
    val option = findCollegeOption(collegeName)
    return option?.logoRes ?: R.drawable.ic_college
}

private fun findCollegeOption(collegeName: String?): CollegeOption? {
    if (collegeName.isNullOrBlank()) return null
    val normalized = normalizeCollegeKey(collegeName)
    return collegeOptions.firstOrNull { option ->
        normalizeCollegeKey(option.name) == normalized ||
            option.aliases.any { normalizeCollegeKey(it) == normalized }
    }
}

private fun normalizeCollegeKey(value: String): String {
    return value
        .lowercase(Locale.getDefault())
        .replace(Regex("[^a-z0-9]"), "")
}

