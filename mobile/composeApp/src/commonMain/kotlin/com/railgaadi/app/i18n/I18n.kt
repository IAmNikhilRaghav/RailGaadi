package com.railgaadi.app.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.railgaadi.app.model.SupportedLanguage
import com.railgaadi.app.util.PlatformStorage

object I18n {
    private const val KEY_LANG = "railgaadi_selected_language"

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

    var currentLanguage by mutableStateOf(loadSavedLanguage())
        private set

    fun setLanguage(code: String) {
        currentLanguage = code
        try {
            PlatformStorage.saveString(KEY_LANG, code)
        } catch (_: Throwable) {}
    }

    private fun loadSavedLanguage(): String {
        return try {
            PlatformStorage.getString(KEY_LANG)?.takeIf { code ->
                languages.any { it.code == code }
            } ?: "en"
        } catch (_: Throwable) {
            "en"
        }
    }

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
            "select_language" to "Select Language",
            // Analytics Timeline & Corridor
            "halt" to "Halt",
            "halt_mins" to "m",
            "pass_through_stations" to "pass-through stations",
            "stations" to "stations",
            "crossing" to "Crossing",
            "at" to "At",
            "delayed_by" to "Delayed by",
            "mins_delayed" to "Delayed by",
            "mins_early" to "Early",
            "on_time_label" to "On Time",
            "start_station" to "START",
            "end_station" to "END",
            "journey_route_profile" to "Journey Route Profile",
            "total_route" to "Total Route",
            "halts" to "Halts",
            "total_stops" to "Total Stops",
            "are_you_inside" to "Are you inside this train?",
            "enable_accurate_tracking" to "Enable accurate tracking",
            "updated_just_now" to "Updated just now",
            "km_to" to "km to",
            "destination_alarm" to "Destination Alarm",
            "search_train_prompt" to "Search for a Train",
            "search_train_subprompt" to "Tap above to track any train live"
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
            "select_language" to "भाषा चुनें",
            // Analytics Timeline & Corridor
            "halt" to "स्टॉप",
            "halt_mins" to " मि.",
            "pass_through_stations" to "बीच के स्टेशन",
            "stations" to "स्टेशन",
            "crossing" to "क्रॉस कर रही है",
            "at" to "पर",
            "delayed_by" to "देरी",
            "mins_delayed" to "देरी",
            "mins_early" to "पहले",
            "on_time_label" to "समय पर",
            "start_station" to "प्रस्थान",
            "end_station" to "गंतव्य",
            "journey_route_profile" to "यात्रा मार्ग विवरण",
            "total_route" to "कुल दूरी",
            "halts" to "ठहराव",
            "total_stops" to "कुल स्टेशन",
            "are_you_inside" to "क्या आप इस ट्रेन में हैं?",
            "enable_accurate_tracking" to "सटीक ट्रैकिंग सक्षम करें",
            "updated_just_now" to "अभी अपडेट हुआ",
            "km_to" to "किमी शेष",
            "destination_alarm" to "गंतव्य अलार्म",
            "search_train_prompt" to "ट्रेन खोजें",
            "search_train_subprompt" to "लाइव ट्रैक करने के लिए ऊपर टैप करें"
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
            "select_language" to "भाषा निवडा",
            "halt" to "थांबा",
            "halt_mins" to " मि.",
            "pass_through_stations" to "मधील स्थानके",
            "stations" to "स्थानके",
            "crossing" to "पार करत आहे",
            "at" to "येथे",
            "delayed_by" to "उशिरा",
            "mins_delayed" to "उशिरा",
            "mins_early" to "लवकर",
            "on_time_label" to "वेळेवर",
            "start_station" to "सुरुवात",
            "end_station" to "शेवट",
            "journey_route_profile" to "प्रवास मार्ग तपशील",
            "total_route" to "एकूण अंतर",
            "halts" to "थांबे",
            "total_stops" to "एकूण स्थानके",
            "are_you_inside" to "तुम्ही या ट्रेनमध्ये आहात का?",
            "enable_accurate_tracking" to "अचूक ट्रॅकिंग सुरू करा",
            "updated_just_now" to "आत्ताच अपडेट केले",
            "km_to" to "किमी बाकी",
            "destination_alarm" to "गंतव्य अलार्म",
            "search_train_prompt" to "गाडी शोधा",
            "search_train_subprompt" to "थेट ट्रॅक करण्यासाठी वर टॅप करा"
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
            "select_language" to "ভাষা নির্বাচন করুন",
            "halt" to "স্টপ",
            "halt_mins" to " মি.",
            "pass_through_stations" to "মধ্যবর্তী স্টেশন",
            "stations" to "স্টেশন",
            "crossing" to "অতিক্রম করছে",
            "at" to "এ",
            "delayed_by" to "দেরিতে",
            "mins_delayed" to "দেরি",
            "mins_early" to "আগে",
            "on_time_label" to "সময়মতো",
            "start_station" to "শুরু",
            "end_station" to "গন্তব্য",
            "journey_route_profile" to "যাত্রাপথ বিবরণ",
            "total_route" to "মোট দূরত্ব",
            "halts" to "স্টপ",
            "total_stops" to "মোট স্টেশন",
            "are_you_inside" to "আপনি কি এই ট্রেনে আছেন?",
            "enable_accurate_tracking" to "সঠিক ট্র্যাকিং চালু করুন",
            "updated_just_now" to "এইমাত্র আপডেট হয়েছে",
            "km_to" to "কিমি বাকি",
            "destination_alarm" to "গন্তব্য অ্যালার্ম",
            "search_train_prompt" to "ট্রেন খুঁজুন",
            "search_train_subprompt" to "লাইভ দেখতে উপরে আলতো চাপুন"
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
            "select_language" to "భాషను ఎంచుకోండి",
            "halt" to "హాల్ట్",
            "halt_mins" to " ని.",
            "pass_through_stations" to "మధ్య స్టేషన్లు",
            "stations" to "స్టేషన్లు",
            "crossing" to "దాటుతోంది",
            "at" to "వద్ద",
            "delayed_by" to "ఆలస్యం",
            "mins_delayed" to "ఆలస్యం",
            "mins_early" to "ముందుగా",
            "on_time_label" to "సమయానికి",
            "start_station" to "ప్రారంభం",
            "end_station" to "గమ్యం",
            "journey_route_profile" to "ప్రయాణ వివరాలు",
            "total_route" to "మొత్తం దూరం",
            "halts" to "హాల్టులు",
            "total_stops" to "మొత్తం స్టేషన్లు",
            "are_you_inside" to "మీరు ఈ రైలులో ఉన్నారా?",
            "enable_accurate_tracking" to "ఖచ్చితమైన ట్రాకింగ్ ప్రారంభించండి",
            "updated_just_now" to "ఇప్పుడే నవీకరించబడింది",
            "km_to" to "కి.మీ మిగిలింది",
            "destination_alarm" to "గమ్యస్థాన అలారం",
            "search_train_prompt" to "రైలును వెతకండి",
            "search_train_subprompt" to "లైవ్ ట్రాక్ చేయడానికి పైన నొక్కండి"
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
            "select_language" to "மொழியைத் தேர்ந்தெடுக்கவும்",
            "halt" to "நிறுத்தம்",
            "halt_mins" to " நிமி.",
            "pass_through_stations" to "இடைப்பட்ட நிலையங்கள்",
            "stations" to "நிலையங்கள்",
            "crossing" to "கடக்கிறது",
            "at" to "இல்",
            "delayed_by" to "தாமதம்",
            "mins_delayed" to "தாமதம்",
            "mins_early" to "முன்னதாக",
            "on_time_label" to "சரியான நேரம்",
            "start_station" to "தொடக்க",
            "end_station" to "சேருமிடம்",
            "journey_route_profile" to "பயண விவரக்குறிப்பு",
            "total_route" to "மொத்த தூரம்",
            "halts" to "நிறுத்தங்கள்",
            "total_stops" to "மொத்த நிலையங்கள்",
            "are_you_inside" to "இந்த ரயிலில் இருக்கிறீர்களா?",
            "enable_accurate_tracking" to "துல்லியமான கண்காணிப்பை இயக்கு",
            "updated_just_now" to "சமீபத்தில் புதுப்பிக்கப்பட்டது",
            "km_to" to "கி.மீ மீதம்",
            "destination_alarm" to "இலக்கு எச்சரிக்கை",
            "search_train_prompt" to "ரயிலைத் தேடுங்கள்",
            "search_train_subprompt" to "நேரலையைக் கண்காணிக்க மேலே தட்டவும்"
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
            "select_language" to "ભાષા પસંદ કરો",
            "halt" to "રોકાણ",
            "halt_mins" to " મિ.",
            "pass_through_stations" to "વચ્ચેના સ્ટેશનો",
            "stations" to "સ્ટેશનો",
            "crossing" to "ક્રોસ કરી રહી છે",
            "at" to "પર",
            "delayed_by" to "મોડી",
            "mins_delayed" to "મોડી",
            "mins_early" to "વહેલી",
            "on_time_label" to "સમયસર",
            "start_station" to "પ્રસ્થાન",
            "end_station" to "ગંતવ્ય",
            "journey_route_profile" to "પ્રવાસ માર્ગ પ્રોફાઇલ",
            "total_route" to "કુલ અંતર",
            "halts" to "રોકાણો",
            "total_stops" to "કુલ સ્ટેશનો",
            "are_you_inside" to "શું તમે આ ટ્રેનમાં છો?",
            "enable_accurate_tracking" to "સચોટ ટ્રેકિંગ ચાલુ કરો",
            "updated_just_now" to "હમણાં જ અપડેટ કર્યું",
            "km_to" to "કિમી બાકી",
            "destination_alarm" to "ગંતવ્ય એલાર્મ",
            "search_train_prompt" to "ટ્રેન શોધો",
            "search_train_subprompt" to "લાઇવ ટ્રેક કરવા ઉપર ટેપ કરો"
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
            "select_language" to "ಭಾಷೆಯನ್ನು ಆಯ್ಕೆಮಾಡಿ",
            "halt" to "ನಿಲುಗಡೆ",
            "halt_mins" to " ನಿ.",
            "pass_through_stations" to "ಮಧ್ಯದ ನಿಲ್ದಾಣಗಳು",
            "stations" to "ನಿಲ್ದಾಣಗಳು",
            "crossing" to "ದಾಟುತ್ತಿದೆ",
            "at" to "ನಲ್ಲಿ",
            "delayed_by" to "ತಡವಾಗಿದೆ",
            "mins_delayed" to "ತಡ",
            "mins_early" to "ಮುಂಚಿತವಾಗಿ",
            "on_time_label" to "ಸಮಯಕ್ಕೆ",
            "start_station" to "ಆರಂಭ",
            "end_station" to "ಗಮ್ಯಸ್ಥಾನ",
            "journey_route_profile" to "ಪ್ರಯಾಣದ ಮಾರ್ಗ ವಿವರ",
            "total_route" to "ಒಟ್ಟು ದೂರ",
            "halts" to "ನಿಲುಗಡೆಗಳು",
            "total_stops" to "ಒಟ್ಟು ನಿಲ್ದಾಣಗಳು",
            "are_you_inside" to "ನೀವು ಈ ರೈಲಿನಲ್ಲಿದ್ದೀರಾ?",
            "enable_accurate_tracking" to "ನಿಖರವಾದ ಟ್ರ್ಯಾಕಿಂಗ್ ಸಕ್ರಿಯಗೊಳಿಸಿ",
            "updated_just_now" to "ಈಗಷ್ಟೇ ನವೀಕರಿಸಲಾಗಿದೆ",
            "km_to" to "ಕಿ.ಮೀ ಬಾಕಿ",
            "destination_alarm" to "ಗಮ್ಯಸ್ಥಾನ ಅಲಾರಾಂ",
            "search_train_prompt" to "ರೈಲು ಹುಡುಕಿ",
            "search_train_subprompt" to "ಲೈವ್ ಟ್ರ್ಯಾಕ್ ಮಾಡಲು ಮೇಲೆ ಟ್ಯಾಪ್ ಮಾಡಿ"
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
            "rs_filter_by" to "ഫിൽറ്റർ ചെയ്യുക:",
            "rs_all_dates" to "എല്ലാ തീയതികളും",
            "rs_pan_india" to "ഇന്ത്യൻ ട്രെയിനുകളും റൂട്ടുകളും",
            "weekly_schedule" to "ആഴ്ചയിലെ ഷെഡ്യൂൾ:",
            "select_language" to "ഭാഷ തിരഞ്ഞെടുക്കുക",
            "halt" to "സ്റ്റോപ്പ്",
            "halt_mins" to " മിനിറ്റ്",
            "pass_through_stations" to "ഇടയ്ക്കുള്ള സ്റ്റേഷനുകൾ",
            "stations" to "സ്റ്റേഷനുകൾ",
            "crossing" to "കടന്നുപോകുന്നു",
            "at" to "ൽ",
            "delayed_by" to "വൈകുന്നു",
            "mins_delayed" to "വൈകി",
            "mins_early" to "നേരത്തെ",
            "on_time_label" to "സമയത്തിന്",
            "start_station" to "ആരംഭം",
            "end_station" to "ലക്ഷ്യസ്ഥാനം",
            "journey_route_profile" to "യാത്രാ റൂട്ട് വിവരങ്ങൾ",
            "total_route" to "ആകെ ദൂരം",
            "halts" to "സ്റ്റോപ്പുകൾ",
            "total_stops" to "ആകെ സ്റ്റേഷനുകൾ",
            "are_you_inside" to "നിങ്ങൾ ഈ ട്രെയിനിലാണോ?",
            "enable_accurate_tracking" to "കൃത്യമായ ട്രാക്കിംഗ് പ്രവർത്തനക്ഷമമാക്കുക",
            "updated_just_now" to "ഇപ്പോൾ അപ്‌ഡേറ്റുചെയ്‌തു",
            "km_to" to "കി.മീ ബാക്കി",
            "destination_alarm" to "ഡെസ്റ്റിനേഷൻ അലാറം",
            "search_train_prompt" to "ട്രെയിൻ തിരയുക",
            "search_train_subprompt" to "തത്സമയം കാണാൻ മുകളിൽ ടാപ്പ് ചെയ്യുക"
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
            "select_language" to "ਭਾਸ਼ਾ ਚੁਣੋ",
            "halt" to "ਸਟਾਪ",
            "halt_mins" to " ਮਿੰਟ",
            "pass_through_stations" to "ਵਿਚਕਾਰਲੇ ਸਟੇਸ਼ਨ",
            "stations" to "ਸਟੇਸ਼ਨ",
            "crossing" to "ਲੰਘ ਰਹੀ ਹੈ",
            "at" to "ਤੇ",
            "delayed_by" to "ਲੇਟ",
            "mins_delayed" to "ਲੇਟ",
            "mins_early" to "ਪਹਿਲਾਂ",
            "on_time_label" to "ਸਮੇਂ ਸਿਰ",
            "start_station" to "ਸ਼ੁਰੂਆਤ",
            "end_station" to "ਮੰਜ਼ਿਲ",
            "journey_route_profile" to "ਸਫ਼ਰ ਰੂਟ ਵੇਰਵਾ",
            "total_route" to "ਕੁੱਲ ਦੂਰੀ",
            "halts" to "ਸਟਾਪ",
            "total_stops" to "ਕੁੱਲ ਸਟੇਸ਼ਨ",
            "are_you_inside" to "ਕੀ ਤੁਸੀਂ ਇਸ ਟਰੇਨ ਵਿੱਚ ਹੋ?",
            "enable_accurate_tracking" to "ਸਹੀ ਟਰੈਕਿੰਗ ਚਾਲੂ ਕਰੋ",
            "updated_just_now" to "ਹੁਣੇ ਅਪਡੇਟ ਹੋਇਆ",
            "km_to" to "ਕਿਮੀ ਬਾਕੀ",
            "destination_alarm" to "ਮੰਜ਼ਿਲ ਅਲਾਰਮ",
            "search_train_prompt" to "ਟਰੇਨ ਲੱਭੋ",
            "search_train_subprompt" to "ਲਾਈਵ ਟਰੈਕ ਕਰਨ ਲਈ ਉੱਪਰ ਟੈਪ ਕਰੋ"
        )
    )

    fun t(key: String, fallback: String = key): String {
        return translations[currentLanguage]?.get(key)
            ?: translations["en"]?.get(key)
            ?: fallback
    }

    val currentPill: String
        get() = languages.find { it.code == currentLanguage }?.pill ?: "EN"

    /**
     * Translates any Indian station name into the selected language (Hindi Devanagari when Hindi is selected).
     */
    fun stationName(name: String): String {
        if (currentLanguage == "hi") {
            return IndicTransliteration.stationToHindi(name)
        }
        return name
    }

    /**
     * Translates any Indian train name into the selected language (Hindi Devanagari when Hindi is selected).
     */
    fun trainName(name: String): String {
        if (currentLanguage == "hi") {
            return IndicTransliteration.trainToHindi(name)
        }
        return name
    }

    /**
     * Formats delay duration into natural localized text (e.g. "Delayed by 2 mins" / "2 मिनट की देरी").
     */
    fun formatDelay(delayMinutes: Int): String {
        if (delayMinutes == 0) return t("on_time_label", "On Time")
        val absMinutes = kotlin.math.abs(delayMinutes)
        val durationStr = if (absMinutes >= 60) {
            val hrs = absMinutes / 60
            val rem = absMinutes % 60
            if (rem == 0) {
                if (currentLanguage == "hi") "$hrs घंटे" else "$hrs hr"
            } else {
                if (currentLanguage == "hi") "$hrs घंटे $rem मिनट" else "$hrs hr $rem min"
            }
        } else {
            if (currentLanguage == "hi") "$absMinutes मिनट" else "$absMinutes mins"
        }

        return if (delayMinutes > 0) {
            if (currentLanguage == "hi") "$durationStr की ${t("delayed_by")}"
            else "${t("delayed_by")} $durationStr"
        } else {
            if (currentLanguage == "hi") "$durationStr ${t("mins_early")}"
            else "$durationStr ${t("mins_early")}"
        }
    }

    /**
     * Formats station halt duration into localized string (e.g. "Halt 2m" / "स्टॉप 2 मि.").
     */
    fun formatHalt(minutes: Int): String {
        return "${t("halt")} $minutes${t("halt_mins")}"
    }

    /**
     * Formats platform number into localized string (e.g. "PF 2" / "प्लेटफ़ॉर्म 2").
     */
    fun formatPlatform(platform: String): String {
        val clean = platform.replace(Regex("^(PF|Platform|प्लेटफ़ॉर्म)\\s*", RegexOption.IGNORE_CASE), "").trim()
        return if (currentLanguage == "hi") "प्लेटफ़ॉर्म $clean" else "PF $clean"
    }

    /**
     * Formats pass-through station count (e.g. "4 pass-through stations" / "4 बीच के स्टेशन").
     */
    fun formatPassThrough(count: Int): String {
        return "$count ${t("pass_through_stations")}"
    }

    /**
     * Formats crossing or arrived status (e.g. "Crossing Bhadan" / "भदान क्रॉस कर रही है").
     */
    fun formatCrossingStatus(name: String, isCrossing: Boolean): String {
        val localizedName = stationName(name)
        return if (isCrossing) {
            if (currentLanguage == "hi") "$localizedName ${t("crossing")}"
            else "${t("crossing")} $localizedName"
        } else {
            if (currentLanguage == "hi") "$localizedName ${t("at")}"
            else "${t("at")} $localizedName"
        }
    }

    /**
     * Formats distance remaining (e.g. "3 km to Etawah Jn" / "इटावा जं. 3 किमी शेष").
     */
    fun formatKmRemaining(km: Int, destinationName: String): String {
        val localizedName = stationName(destinationName)
        return if (currentLanguage == "hi") {
            "$localizedName $km ${t("km_to")}"
        } else {
            "$km ${t("km_to")} $localizedName"
        }
    }

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
