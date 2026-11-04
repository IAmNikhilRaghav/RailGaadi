/**
 * RailGaadi Internationalization (i18n) Engine
 * Ultra-lightweight, zero-dependency translation dictionary for Indian Languages
 */

export const SUPPORTED_LANGUAGES = [
  { code: "en", label: "English",   native: "English",  pill: "EN", flag: "🇮🇳" },
  { code: "hi", label: "Hindi",     native: "हिंदी",     pill: "हिं", flag: "🇮🇳" },
  { code: "bn", label: "Bengali",   native: "বাংলা",     pill: "বাং", flag: "🇮🇳" },
  { code: "mr", label: "Marathi",   native: "मराठी",     pill: "मरा", flag: "🇮🇳" },
  { code: "te", label: "Telugu",    native: "తెలుగు",    pill: "తె", flag: "🇮🇳" },
  { code: "ta", label: "Tamil",     native: "தமிழ்",     pill: "த",  flag: "🇮🇳" },
  { code: "gu", label: "Gujarati",  native: "ગુજરાતી",   pill: "ગુ", flag: "🇮🇳" },
  { code: "kn", label: "Kannada",   native: "ಕನ್ನಡ",    pill: "ಕ",  flag: "🇮🇳" }
];

const TRANSLATIONS = {
  en: {
    // Navigation
    tab_live_map: "Live Map",
    tab_routes: "Routes",
    tab_analytics: "Analytics",
    tab_weather: "Weather",
    tab_station: "Station",

    // Quick Menu & Header
    menu_title: "Menu & Quick Tools",
    appearance: "Appearance",
    dark_mode: "Dark Theme",
    dark_mode_sub: "Comfortable viewing at night",
    delay_alerts: "Trip Delay Alerts",
    delay_alerts_sub: "Live notifications on delay",
    saved_trains: "Saved Trains & Routes",
    saved_trains_sub: "Quick access to pinned trains",
    share_status: "Share Live Status",
    share_status_sub: "Send live train link via WhatsApp",
    map_style: "Map Style",
    select_language: "Select Language / भाषा चुनें",
    select_language_sub: "Choose your preferred language",

    // Map styles
    map_light: "Light",
    map_dark: "Dark",
    map_satellite: "Satellite",
    map_terrain: "Terrain",

    // Timeline / Analytics
    overview: "Overview",
    description: "Description",
    show_desc: "Show Description",
    hide_desc: "Hide Description",
    next_station: "Next Station",
    remaining: "remaining",
    at_station: "At",
    delayed_by: "Delayed by",
    on_time: "On Time",
    platform: "Platform",
    halt: "Halt",
    mins: "mins",
    stations: "Stations",
    covered: "Covered"
  },

  hi: {
    tab_live_map: "लाइव मैप",
    tab_routes: "मार्ग",
    tab_analytics: "एनालिटिक्स",
    tab_weather: "मौसम",
    tab_station: "स्टेशन",

    menu_title: "मेनू और टूल्स",
    appearance: "डार्क थीम",
    dark_mode: "डार्क मोड",
    dark_mode_sub: "रात में देखने के लिए आरामदायक",
    delay_alerts: "ट्रेन देरी अलर्ट",
    delay_alerts_sub: "देरी होने पर तुरंत सूचना",
    saved_trains: "सुरक्षित ट्रेनें व मार्ग",
    saved_trains_sub: "पसंदीदा ट्रेनों तक सीधी पहुंच",
    share_status: "लाइव स्थिति साझा करें",
    share_status_sub: "व्हाट्सएप द्वारा ट्रेन लिंक भेजें",
    map_style: "नक्शा शैली",
    select_language: "भाषा चुनें / Select Language",
    select_language_sub: "अपनी पसंदीदा भाषा चुनें",

    map_light: "लाइट",
    map_dark: "डार्क",
    map_satellite: "सैटेलाइट",
    map_terrain: "भूभाग",

    overview: "अवलोकन",
    description: "विवरण",
    show_desc: "विवरण देखें",
    hide_desc: "विवरण छिपाएं",
    next_station: "अगला स्टेशन",
    remaining: "शेष",
    at_station: "पर",
    delayed_by: "देरी",
    on_time: "समय पर",
    platform: "प्लेटफ़ॉर्म",
    halt: "ठहराव",
    mins: "मिनट",
    stations: "स्टेशन",
    covered: "पूर्ण"
  },

  bn: {
    tab_live_map: "লাইভ ম্যাপ",
    tab_routes: "রুট",
    tab_analytics: "অ্যানালিটিক্স",
    tab_weather: "আবহাওয়া",
    tab_station: "স্টেশন",

    menu_title: "মেনু ও সরঞ্জাম",
    appearance: "ডার্ক থিম",
    dark_mode: "ডার্ক মোড",
    dark_mode_sub: "রাতের জন্য সুবিধাজনক",
    delay_alerts: "দেরি সতর্কতা",
    delay_alerts_sub: "দেরি হলে লাইভ বিজ্ঞপ্তি",
    saved_trains: "সংরক্ষিত ট্রেন",
    saved_trains_sub: "সংরক্ষিত ট্রেনের দ্রুত তালিকা",
    share_status: "লাইভ অবস্থা শেয়ার করুন",
    share_status_sub: "হোয়াটসঅ্যাপে ট্রেনের তথ্য পাঠান",
    map_style: "ম্যাপের ধরন",
    select_language: "ভাষা নির্বাচন করুন",
    select_language_sub: "আপনার পছন্দের ভাষা বেছে নিন",

    map_light: "লাইট",
    map_dark: "ডার্ক",
    map_satellite: "স্যাটেলাইট",
    map_terrain: "ভূপ্রকৃতি",

    overview: "সংক্ষিপ্ত বিবরণ",
    description: "বিস্তারিত",
    show_desc: "বিস্তারিত দেখুন",
    hide_desc: "লুকান",
    next_station: "পরবর্তী স্টেশন",
    remaining: "বাকি",
    at_station: "এ",
    delayed_by: "বিলম্ব",
    on_time: "ঠিক সময়",
    platform: "প্ল্যাটফর্ম",
    halt: "যাত্রাবিরতি",
    mins: "মিনিট",
    stations: "স্টেশন",
    covered: "অতিক্রান্ত"
  },

  mr: {
    tab_live_map: "थेट नकाशा",
    tab_routes: "मार्ग",
    tab_analytics: "विश्लेषण",
    tab_weather: "हवामान",
    tab_station: "स्थानक",

    menu_title: "मेनू आणि टूल्स",
    appearance: "डार्क थीम",
    dark_mode: "डार्क मोड",
    dark_mode_sub: "रात्रीसाठी सोयीस्कर",
    delay_alerts: "उशीर सूचना",
    delay_alerts_sub: "ट्रेन उशिरा असल्यास सूचना",
    saved_trains: "जतन केलेल्या गाड्या",
    saved_trains_sub: "आपल्या आवडत्या गाड्यांची यादी",
    share_status: "थेट स्थिती शेअर करा",
    share_status_sub: "व्हॉट्सॲपवर ट्रेन लिंक पाठवा",
    map_style: "नकाशा प्रकार",
    select_language: "भाषा निवडा",
    select_language_sub: "आपली आवडती भाषा निवडा",

    map_light: "लाइट",
    map_dark: "डार्क",
    map_satellite: "उपग्रह",
    map_terrain: "भूरूप",

    overview: "आढावा",
    description: "तपशील",
    show_desc: "तपशील पहा",
    hide_desc: "तपशील लपवा",
    next_station: "पुढील स्थानक",
    remaining: "बाकी",
    at_station: "येथे",
    delayed_by: "उशीर",
    on_time: "वेळेवर",
    platform: "प्लॅटफॉर्म",
    halt: "थांबा",
    mins: "मिनिटे",
    stations: "स्थानके",
    covered: "पूर्ण"
  },

  te: {
    tab_live_map: "లైవ్ మ్యాప్",
    tab_routes: "రూట్లు",
    tab_analytics: "విశ్లేషణ",
    tab_weather: "వాతావరణం",
    tab_station: "స్టేషన్",

    menu_title: "మెనూ & టూల్స్",
    appearance: "డార్క్ థీమ్",
    dark_mode: "డార్క్ మోడ్",
    dark_mode_sub: "రాత్రి సమయాల్లో సులభమైన వీక్షణ",
    delay_alerts: "ఆలస్యం అలర్ట్స్",
    delay_alerts_sub: "రైలు ఆలస్యమైతే నోటిఫికేషన్",
    saved_trains: "సేవ్ చేసిన రైళ్లు",
    saved_trains_sub: "మీ ప్రియమైన రైళ్ల జాబితా",
    share_status: "లైవ్ స్టేటస్ షేర్ చేయండి",
    share_status_sub: "వాట్సాప్ ద్వారా లింక్ పంపండి",
    map_style: "మ్యాప్ శైలి",
    select_language: "భాషను ఎంచుకోండి",
    select_language_sub: "మీ ప్రాధాన్యత గల భాషను ఎంచుకోండి",

    map_light: "లైట్",
    map_dark: "డార్క్",
    map_satellite: "శాటిలైట్",
    map_terrain: "భూభాగం",

    overview: "అవలోకనం",
    description: "వివరాలు",
    show_desc: "వివరాలు చూడండి",
    hide_desc: "దాచు",
    next_station: "తదుపరి స్టేషన్",
    remaining: "మిగిలి ఉంది",
    at_station: "వద్ద",
    delayed_by: "ఆలస్యం",
    on_time: "సమయానికి",
    platform: "ప్లాట్‌ఫాం",
    halt: "విరామం",
    mins: "నిమిషాలు",
    stations: "స్టేషన్లు",
    covered: "పూర్తయింది"
  },

  ta: {
    tab_live_map: "லைவ் மேப்",
    tab_routes: "பாதைகள்",
    tab_analytics: "பகுப்பாய்வு",
    tab_weather: "வானிலை",
    tab_station: "நிலையம்",

    menu_title: "பட்டியல் & கருவிகள்",
    appearance: "டார்க் தீம்",
    dark_mode: "டார்க் மோட்",
    dark_mode_sub: "இரவு நேரத்திற்கு ஏற்றது",
    delay_alerts: "தாமத எச்சரிக்கை",
    delay_alerts_sub: "ரயில் தாமதமானால் அறிவிப்பு",
    saved_trains: "சேமிக்கப்பட்ட ரயில்கள்",
    saved_trains_sub: "சேமிக்கப்பட்ட ரயில்களின் பட்டியல்",
    share_status: "நேரலை நிலையை பகிர்க",
    share_status_sub: "வாட்ஸ்அப் மூலம் பகிரவும்",
    map_style: "வரைபட பாணி",
    select_language: "மொழியைத் தேர்ந்தெடுக்கவும்",
    select_language_sub: "உங்கள் மொழியைத் தேர்ந்தெடுக்கவும்",

    map_light: "லைட்",
    map_dark: "டார்க்",
    map_satellite: "செயற்கைக்கோள்",
    map_terrain: "நிலப்பரப்பு",

    overview: "மேலோட்டம்",
    description: "விவரம்",
    show_desc: "விவரங்களை பார்க்க",
    hide_desc: "மறைக்க",
    next_station: "அடுத்த நிலையம்",
    remaining: "மீதமுள்ளது",
    at_station: "இல் உள்ளது",
    delayed_by: "தாமதம்",
    on_time: "சரியான நேரம்",
    platform: "நடைமேடை",
    halt: "நிறுத்தம்",
    mins: "நிமிடங்கள்",
    stations: "நிலையங்கள்",
    covered: "முடிந்தது"
  },

  gu: {
    tab_live_map: "લાઈવ નકશો",
    tab_routes: "રૂટ્સ",
    tab_analytics: "વિશ્લેષણ",
    tab_weather: "હવામાન",
    tab_station: "સ્ટેશન",

    menu_title: "મેનુ અને સાધનો",
    appearance: "ડાર્ક થીમ",
    dark_mode: "ડાર્ક મોડ",
    dark_mode_sub: "રાત્રિ માટે અનુકૂળ",
    delay_alerts: "વિલંબ ચેતવણી",
    delay_alerts_sub: "ટ્રેન મોડી થાય ત્યારે સૂચના",
    saved_trains: "સાચવેલી ટ્રેનો",
    saved_trains_sub: "મનપસંદ ટ્રેનોની યાદી",
    share_status: "લાઈવ સ્ટેટસ શેર કરો",
    share_status_sub: "વોટ્સએપ દ્વારા લિંક મોકલો",
    map_style: "નકશા શૈલી",
    select_language: "ભાષા પસંદ કરો",
    select_language_sub: "તમારી મનપસંદ ભાષા પસંદ કરો",

    map_light: "લાઇટ",
    map_dark: "ડાર્ક",
    map_satellite: "સેટેલાઇટ",
    map_terrain: "ભૂપ્રદેશ",

    overview: "ઝાંખી",
    description: "વિગતવાર",
    show_desc: "વિગતો જુઓ",
    hide_desc: "વિગતો છુપાવો",
    next_station: "આગળનું સ્ટેશન",
    remaining: "બાકી",
    at_station: "પર",
    delayed_by: "મોડી",
    on_time: "સમયસર",
    platform: "પ્લેટફોર્મ",
    halt: "રોકાણ",
    mins: "મિનિટ",
    stations: "સ્ટેશનો",
    covered: "પૂર્ણ"
  },

  kn: {
    tab_live_map: "ಲೈವ್ ನಕ್ಷೆ",
    tab_routes: "ಮಾರ್ಗಗಳು",
    tab_analytics: "ವಿಶ್ಲೇಷಣೆ",
    tab_weather: "ಹವಾಮಾನ",
    tab_station: "ನಿಲ್ದಾಣ",

    menu_title: "ಮೆನು ಮತ್ತು ಪರಿಕರಗಳು",
    appearance: "ಡಾರ್ಕ್ ಥೀಮ್",
    dark_mode: "ಡಾರ್ಕ್ ಮೋಡ್",
    dark_mode_sub: "ರಾತ್ರಿ ವೀಕ್ಷಣೆಗೆ ಸೂಕ್ತ",
    delay_alerts: "ವಿಳಂಬ ಎಚ್ಚರಿಕೆ",
    delay_alerts_sub: "ರೈಲು ವಿಳಂಬವಾದಾಗ ಸೂಚನೆ",
    saved_trains: "ಉಳಿಸಿದ ರೈಲುಗಳು",
    saved_trains_sub: "ಮೆಚ್ಚಿನ ರೈಲುಗಳ ಪಟ್ಟಿ",
    share_status: "ಲೈವ್ ಸ್ಥಿತಿ ಹಂಚಿಕೊಳ್ಳಿ",
    share_status_sub: "ವಾಟ್ಸಾಪ್ ಮೂಲಕ ಲಿಂಕ್ ಕಳುಹಿಸಿ",
    map_style: "ನಕ್ಷೆಯ ಶೈಲಿ",
    select_language: "ಭಾಷೆಯನ್ನು ಆಯ್ಕೆಮಾಡಿ",
    select_language_sub: "ನಿಮ್ಮ ಆಯ್ಕೆಯ ಭಾಷೆ ಆಯ್ಕೆಮಾಡಿ",

    map_light: "ಲೈಟ್",
    map_dark: "ಡಾರ್ಕ್",
    map_satellite: "ಉಪಗ್ರಹ",
    map_terrain: "ಭೂಪ್ರದೇಶ",

    overview: "ಅವಲೋಕನ",
    description: "ವಿವರಣೆ",
    show_desc: "ವಿವರ ನೋಡಿ",
    hide_desc: "ಮರೆಮಾಡಿ",
    next_station: "ಮುಂದಿನ ನಿಲ್ದಾಣ",
    remaining: "ಉಳಿದಿದೆ",
    at_station: "ರಲ್ಲಿ",
    delayed_by: "ವಿಳಂಬ",
    on_time: "ಸಮಯಕ್ಕೆ ಸರಿಯಾಗಿ",
    platform: "ಪ್ಲಾಟ್‌ಫಾರ್ಮ್",
    halt: "ನಿಲುಗಡೆ",
    mins: "ನಿಮಿಷ",
    stations: "ನಿಲ್ದಾಣಗಳು",
    covered: "ಪೂರ್ಣಗೊಂಡಿದೆ"
  }
};

const STORAGE_KEY_LANG = "railgaadi_lang_v1";

let currentLanguage = (() => {
  try {
    return localStorage.getItem(STORAGE_KEY_LANG) || "en";
  } catch {
    return "en";
  }
})();

export function getLanguage() {
  return currentLanguage;
}

export function getLanguagePill(code = currentLanguage) {
  const match = SUPPORTED_LANGUAGES.find(l => l.code === code);
  return match ? match.pill : "EN";
}

export function setLanguage(code) {
  const exists = SUPPORTED_LANGUAGES.some(l => l.code === code);
  currentLanguage = exists ? code : "en";
  try {
    localStorage.setItem(STORAGE_KEY_LANG, currentLanguage);
  } catch {}
  document.documentElement.setAttribute("lang", currentLanguage);
  return currentLanguage;
}

export function t(key, fallback = "") {
  const langDict = TRANSLATIONS[currentLanguage] || TRANSLATIONS.en;
  if (langDict && langDict[key] !== undefined) {
    return langDict[key];
  }
  const enDict = TRANSLATIONS.en;
  if (enDict && enDict[key] !== undefined) {
    return enDict[key];
  }
  return fallback || key;
}

