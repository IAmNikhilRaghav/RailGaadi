package com.railgaadi.app.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.railgaadi.app.model.SupportedLanguage

object I18n {
    val languages = listOf(
        SupportedLanguage("en", "English", "English", "EN"),
        SupportedLanguage("hi", "Hindi", "हिन्दी", "हिंद"),
        SupportedLanguage("bn", "Bengali", "বাংলা", "বাং"),
        SupportedLanguage("mr", "Marathi", "मराठी", "मरा"),
        SupportedLanguage("te", "Telugu", "తెలుగు", "తెలు"),
        SupportedLanguage("ta", "Tamil", "தமிழ்", "தமி"),
        SupportedLanguage("gu", "Gujarati", "ગુજરાતી", "ગુજ"),
        SupportedLanguage("kn", "Kannada", "ಕನ್ನಡ", "ಕನ್ನ"),
        SupportedLanguage("ml", "Malayalam", "മലയാളം", "മല"),
        SupportedLanguage("pa", "Punjabi", "ਪੰਜਾਬੀ", "ਪੰਜ")
    )

    var currentLanguage by mutableStateOf("en")

    private val translations: Map<String, Map<String, String>> = mapOf(
        "en" to mapOf(
            "tab_live_map" to "Live Map",
            "tab_routes" to "Routes",
            "tab_analytics" to "Analytics",
            "tab_weather" to "Weather",
            "tab_station" to "Station",
            "status_on_time" to "ON TIME",
            "status_delayed" to "DELAYED",
            "status_early" to "EARLY",
            "search_placeholder" to "Search by train no. or name",
            "search_track_live" to "Track Live",
            "speed" to "Speed",
            "platform" to "Platform",
            "running_today" to "Runs today",
            "rs_filter_by" to "Filter by:",
            "rs_all_dates" to "All Dates",
            "rs_pan_india" to "Pan-India Trains & Routes",
            "weekly_schedule" to "Weekly Schedule:",
            "select_language" to "Select Language"
        ),
        "hi" to mapOf(
            "tab_live_map" to "लाइव मैप",
            "tab_routes" to "ट्रेन रूट्स",
            "tab_analytics" to "एनालिटिक्स",
            "tab_weather" to "मौसम",
            "tab_station" to "स्टेशन",
            "status_on_time" to "समय पर",
            "status_delayed" to "देरी से",
            "status_early" to "समय से पहले",
            "search_placeholder" to "ट्रेन नंबर या नाम दर्ज करें",
            "search_track_live" to "लाइव ट्रैक करें",
            "speed" to "रफ़्तार",
            "platform" to "प्लेटफ़ॉर्म",
            "running_today" to "आज चल रही है",
            "rs_filter_by" to "फ़िल्टर करें:",
            "rs_all_dates" to "सभी तारीखें",
            "rs_pan_india" to "प्रमुख भारतीय ट्रेनें व मार्ग",
            "weekly_schedule" to "साप्ताहिक शेड्यूल:",
            "select_language" to "भाषा चुनें"
        ),
        "bn" to mapOf(
            "tab_live_map" to "লাইভ ম্যাপ",
            "tab_routes" to "রুট",
            "tab_analytics" to "বিশ্লেষণ",
            "tab_weather" to "আবহাওয়া",
            "tab_station" to "স্টেশন",
            "status_on_time" to "সময়মতো",
            "status_delayed" to "দেরি",
            "status_early" to "আগে",
            "search_placeholder" to "ট্রেন নম্বর বা নাম লিখুন",
            "search_track_live" to "লাইভ ট্র্যাক",
            "speed" to "গতি",
            "platform" to "প্ল্যাটফর্ম",
            "running_today" to "আজ চলছে",
            "rs_filter_by" to "ফিল্টার:",
            "rs_all_dates" to "সব তারিখ",
            "rs_pan_india" to "ভারতীয় ট্রেন ও রুট",
            "weekly_schedule" to "সাপ্তাহিক সময়সূচী:",
            "select_language" to "ভাষা নির্বাচন করুন"
        ),
        "mr" to mapOf(
            "tab_live_map" to "थेट नकाशा",
            "tab_routes" to "मार्ग",
            "tab_analytics" to "विश्लेषण",
            "tab_weather" to "हवामान",
            "tab_station" to "स्थानक",
            "status_on_time" to "वेळेवर",
            "status_delayed" to "उशिरा",
            "status_early" to "वेळेपूर्वी",
            "search_placeholder" to "गाडी क्रमांक किंवा नाव टाका",
            "search_track_live" to "थेट ट्रॅक करा",
            "speed" to "वेग",
            "platform" to "प्लॅटफॉर्म",
            "running_today" to "आज धावत आहे",
            "rs_filter_by" to "फिल्टर करा:",
            "rs_all_dates" to "सर्व तारखा",
            "rs_pan_india" to "प्रमुख भारतीय गाड्या व मार्ग",
            "weekly_schedule" to "साप्ताहिक वेळापत्रक:",
            "select_language" to "भाषा निवडा"
        ),
        "te" to mapOf(
            "tab_live_map" to "లైవ్ మ్యాప్",
            "tab_routes" to "రూట్లు",
            "tab_analytics" to "విశ్లేషణ",
            "tab_weather" to "వాతావరణం",
            "tab_station" to "స్టేషన్",
            "status_on_time" to "సమయానికి",
            "status_delayed" to "ఆలస్యం",
            "status_early" to "ముందుగా",
            "search_placeholder" to "రైలు నంబర్ లేదా పేరు నమోదు చేయండి",
            "search_track_live" to "లైవ్ ట్రాక్ చేయండి",
            "speed" to "వేగం",
            "platform" to "ప్లాట్‌ఫాం",
            "running_today" to "ఈరోజు నడుస్తోంది",
            "rs_filter_by" to "ఫిల్టర్:",
            "rs_all_dates" to "అన్ని తేదీలు",
            "rs_pan_india" to "భారతీయ రైళ్లు & మార్గాలు",
            "weekly_schedule" to "వారపు షెడ్యూల్:",
            "select_language" to "భాషను ఎంచుకోండి"
        ),
        "ta" to mapOf(
            "tab_live_map" to "நேரலை வரைபடம்",
            "tab_routes" to "வழித்தடங்கள்",
            "tab_analytics" to "பகுப்பாய்வு",
            "tab_weather" to "வானிலை",
            "tab_station" to "நிலையம்",
            "status_on_time" to "சரியான நேரம்",
            "status_delayed" to "தாமதம்",
            "status_early" to "முன்கூட்டியே",
            "search_placeholder" to "ரயில் எண் அல்லது பெயர்",
            "search_track_live" to "நேரலை கண்காணிப்பு",
            "speed" to "வேகம்",
            "platform" to "நடைமேடை",
            "running_today" to "இன்று இயங்குகிறது",
            "rs_filter_by" to "வடிகட்டுதல்:",
            "rs_all_dates" to "அனைத்து தேதிகள்",
            "rs_pan_india" to "இந்திய ரயில்கள் & வழித்தடங்கள்",
            "weekly_schedule" to "வாராந்திர அட்டவணை:",
            "select_language" to "மொழியைத் தேர்ந்தெடுக்கவும்"
        ),
        "gu" to mapOf(
            "tab_live_map" to "લાઇવ નકશો",
            "tab_routes" to "રૂટ",
            "tab_analytics" to "વિશ્લેષણ",
            "tab_weather" to "હવામાન",
            "tab_station" to "સ્ટેશન",
            "status_on_time" to "સમયસર",
            "status_delayed" to "મોડી",
            "status_early" to "વહેલી",
            "search_placeholder" to "ટ્રેન નંબર અથવા નામ દાખલ કરો",
            "search_track_live" to "લાઇવ ટ્રેક કરો",
            "speed" to "ઝડપ",
            "platform" to "પ્લેટફોર્મ",
            "running_today" to "આજે દોડે છે",
            "rs_filter_by" to "ફિલ્ટર કરો:",
            "rs_all_dates" to "બધી તારીખો",
            "rs_pan_india" to "ભારતીય ટ્રેનો અને રૂટ્સ",
            "weekly_schedule" to "સાપ્તાહિક શેડ્યૂલ:",
            "select_language" to "ભાષા પસંદ કરો"
        ),
        "kn" to mapOf(
            "tab_live_map" to "ಲೈವ್ ನಕ್ಷೆ",
            "tab_routes" to "ರೈಲು ಮಾರ್ಗಗಳು",
            "tab_analytics" to "ವಿಶ್ಲೇಷಣೆ",
            "tab_weather" to "ಹವಾಮಾನ",
            "tab_station" to "ನಿಲ್ದಾಣ",
            "status_on_time" to "ಸಮಯಕ್ಕೆ ಸರಿಯಾಗಿ",
            "status_delayed" to "ತಡವಾಗಿದೆ",
            "status_early" to "ಮುಂಚಿತವಾಗಿ",
            "search_placeholder" to "ರೈಲು ಸಂಖ್ಯೆ ಅಥವಾ ಹೆಸರು ನಮೂದಿಸಿ",
            "search_track_live" to "ಲೈವ್ ಟ್ರ್ಯಾಕ್ ಮಾಡಿ",
            "speed" to "ವೇಗ",
            "platform" to "ಪ್ಲಾಟ್‌ಫಾರ್ಮ್",
            "running_today" to "ಇಂದು ಚಲಿಸುತ್ತಿದೆ",
            "rs_filter_by" to "ಫಿಲ್ಟರ್ ಮಾಡಿ:",
            "rs_all_dates" to "ಎಲ್ಲಾ ದಿನಾಂಕಗಳು",
            "rs_pan_india" to "ಭಾರತೀಯ ರೈಲುಗಳು ಮತ್ತು ಮಾರ್ಗಗಳು",
            "weekly_schedule" to "ಸಾಪ್ತಾಹಿಕ ವೇಳಾಪಟ್ಟಿ:",
            "select_language" to "ಭಾಷೆಯನ್ನು ಆಯ್ಕೆಮಾಡಿ"
        ),
        "ml" to mapOf(
            "tab_live_map" to "തത്സമയ മാപ്പ്",
            "tab_routes" to "റൂട്ടുകൾ",
            "tab_analytics" to "വിശകലനം",
            "tab_weather" to "കാലാവസ്ഥ",
            "tab_station" to "സ്റ്റേഷൻ",
            "status_on_time" to "സമയത്തിന്",
            "status_delayed" to "വൈകുന്നു",
            "status_early" to "നേരത്തെ",
            "search_placeholder" to "ട്രെയിൻ നമ്പർ അല്ലെങ്കിൽ പേര് നൽകുക",
            "search_track_live" to "തത്സമയം ട്രാക്ക് ചെയ്യുക",
            "speed" to "വേഗത",
            "platform" to "പ്ലാറ്റ്‌ഫോം",
            "running_today" to "ഇന്ന് സർവീസ് ഉണ്ട്",
            "rs_filter_by" to "ഫിൽട്ടർ ചെയ്യുക:",
            "rs_all_dates" to "എല്ലാ തീയതികളും",
            "rs_pan_india" to "ഇന്ത്യൻ ട്രെയിനുകളും റൂട്ടുകളും",
            "weekly_schedule" to "ആഴ്ചയിലെ ഷെഡ്യൂൾ:",
            "select_language" to "ഭാഷ തിരഞ്ഞെടുക്കുക"
        ),
        "pa" to mapOf(
            "tab_live_map" to "ਲਾਈਵ ਨਕਸ਼ਾ",
            "tab_routes" to "ਟਰੇਨ ਰੂਟ",
            "tab_analytics" to "ਵਿਸ਼ਲੇਸ਼ਣ",
            "tab_weather" to "ਮੌਸਮ",
            "tab_station" to "ਸਟੇਸ਼ਨ",
            "status_on_time" to "ਸਮੇਂ ਸਿਰ",
            "status_delayed" to "ਲੇਟ",
            "status_early" to "ਸਮੇਂ ਤੋਂ ਪਹਿਲਾਂ",
            "search_placeholder" to "ਟਰੇਨ ਨੰਬਰ ਜਾਂ ਨਾਮ ਦਰਜ ਕਰੋ",
            "search_track_live" to "ਲਾਈਵ ਟਰੈਕ ਕਰੋ",
            "speed" to "ਰਫ਼ਤਾਰ",
            "platform" to "ਪਲੇਟਫਾਰਮ",
            "running_today" to "ਅੱਜ ਚੱਲ ਰਹੀ ਹੈ",
            "rs_filter_by" to "ਫਿਲਟਰ ਕਰੋ:",
            "rs_all_dates" to "ਸਾਰੀਆਂ ਤਾਰੀਖਾਂ",
            "rs_pan_india" to "ਭਾਰਤੀ ਟਰੇਨਾਂ ਅਤੇ ਰੂਟ",
            "weekly_schedule" to "ਹਫ਼ਤਾਵਾਰੀ ਸ਼ਡਿਊਲ:",
            "select_language" to "ਭਾਸ਼ਾ ਚੁਣੋ"
        )
    )

    fun t(key: String, fallback: String = key): String {
        return translations[currentLanguage]?.get(key)
            ?: translations["en"]?.get(key)
            ?: fallback
    }

    val currentPill: String
        get() = languages.find { it.code == currentLanguage }?.pill ?: "EN"

    fun dayLabels(): List<String> {
        return when (currentLanguage) {
            "hi", "mr" -> listOf("र", "सो", "मं", "बु", "गु", "शु", "श")
            "bn" -> listOf("র", "সো", "ম", "বু", "বৃ", "শু", "শ")
            "te" -> listOf("ఆ", "సో", "మం", "బు", "గు", "శు", "శ")
            "ta" -> listOf("ஞா", "தி", "செ", "பு", "வி", "வெ", "ச")
            "gu" -> listOf("ર", "સો", "મં", "બુ", "ગુ", "શુ", "શ")
            "kn" -> listOf("ಭಾ", "ಸೋ", "ಮಂ", "ಬು", "ಗು", "ಶು", "ಶ")
            "ml" -> listOf("ഞാ", "തി", "ചൊ", "ബു", "വ്യാ", "വെ", "ശ")
            "pa" -> listOf("ਐ", "ਸੋ", "ਮੰ", "ਬੁੱ", "ਵੀ", "ਸ਼ੁ", "ਸ਼")
            else -> listOf("S", "M", "T", "W", "T", "F", "S")
        }
    }
}

