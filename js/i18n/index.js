/**
 * RailGaadi Internationalization (i18n) Engine — v3
 * Ultra-lightweight, zero-dependency translation & railway transliteration dictionary
 * Uses natural, spoken Indian Railway vocabulary (non-bookish, natural IRCTC/UTS standard)
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

// Natural Indian Railway Dictionary across UI sections
const TRANSLATIONS = {
  en: {
    // Navigation Tabs
    tab_live_map: "Live Map",
    tab_routes: "Routes",
    tab_analytics: "Analytics",
    tab_weather: "Weather",
    tab_station: "Station",

    // Quick Menu & Preferences
    menu_title: "Preferences",
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

    // Search Section
    search_placeholder: "Search by train no. or name",
    search_track_live: "Track Live",
    search_recent: "Recent:",
    search_ticker_1: "Track live train status",
    search_ticker_2: "Track Any Indian Train Live",

    // Live Status Card & Metrics
    status_card_title: "Live Running Status",
    status_on_time: "ON TIME",
    status_delayed: "DELAYED",
    status_early: "EARLY",
    status_arrived: "ARRIVED",
    status_scheduled: "SCHEDULED",
    status_cancelled: "CANCELLED",
    status_running: "RUNNING",
    journey_progress: "Journey Progress",
    current_station_spotlight: "Current / Last Reported Station",
    next_station_spotlight: "Next Upcoming Station",
    scheduled: "Scheduled",
    reported: "Reported",
    platform: "Platform",
    pf_abbr: "PF",
    halt: "Halt",
    speed: "Speed",
    distance_covered: "Covered",
    distance_remaining: "Remaining",
    runs_on: "Runs on",
    running_today: "Running today",
    not_today: "Not today",
    find_between: "Find Trains Between Stations",
    find_between_sub: "Station A → Station B & Running Routine",
    find_btn: "Find",
    telemetry_verified: "Showing latest verified telemetry",

    // Timeline & Analytics
    full_journey_timeline: "Full Journey Timeline & Station Events",
    overview: "Overview",
    description: "Description",
    show_desc: "Show Description",
    hide_desc: "Hide Description",
    next_station: "Next Station",
    remaining: "remaining",
    at_station: "At",
    delayed_by: "Delayed by",
    on_time: "On Time",
    mins: "mins",
    stations: "Stations",
    covered: "Covered",
    scheduled_stop: "Scheduled stop",
    pass_through: "Pass-through",
    live_train: "Live Train",
    passed: "Passed",

    // Analytics Cards
    schedule_variance: "Schedule Variance",
    en_route_delay: "En-route operational delay",
    journey_distance: "Journey Distance",
    highest_route_point: "Highest Route Point",
    near_station: "Near",
    route_elevation_profile: "Route Elevation Profile",
    route_elevation_desc: "Terrain altitude along the rail corridor (Source: OpenTopography SRTM)",
    live_telemetry_velocity: "Live Telemetry & Velocity curve",
    live_speed_sub: "Continuous track speed graph and deceleration markers",
    peak_elevation: "Peak",

    // Routes (A to B) Page
    rs_hero_title: "Find Trains Between Stations",
    rs_from_label: "From station",
    rs_to_label: "To station",
    rs_from_placeholder: "From station (e.g. NDLS, New Delhi)",
    rs_to_placeholder: "To station (e.g. BPL, Bhopal)",
    rs_swap_title: "Swap stations",
    rs_today: "Today",
    rs_tomorrow: "Tomorrow",
    rs_all_days: "All days",
    rs_find_trains: "Find Trains",
    rs_available_trains: "Available Trains",
    rs_pan_india_trains: "Pan-India Trains & Routes",

    // Station Board Page
    board_hero_title: "Live Station Board",
    board_departures: "Departures",
    board_arrivals: "Arrivals",
    board_expected: "Expected",
    board_direction: "Direction",

    // Weather & Landmarks Page
    weather_hero_title: "Live Weather & Landmark Companion",
    weather_temp: "Temperature",
    weather_humidity: "Humidity",
    weather_wind: "Wind Speed",
    landmarks_title: "En-route Landmarks",
    // New component keys
    delay_data_unavailable: "Live delay data unavailable",
    running_on_schedule: "Running on schedule",
    enroute_delay: "En-route operational delay",
    complete: "complete",
    route_data_loading: "Route data loading",
    fetching_elevation: "Fetching elevation data",
    rs_showing: "Showing",
    rs_trains: "trains",
    board_scheduled: "Scheduled",
    board_th_train: "Train",
    board_th_destination: "Destination",
    board_th_origin: "Origin",
    board_departs: "Departs",
    board_arrives: "Arrives",
    board_th_status: "Status",
    board_track: "Track",
    board_no_trains: "No",
    board_listed_for: "currently listed for",
    board_live_timetable: "Live station timetable",
    board_station_label: "Station",
    board_plt: "Plt",
    board_trains: "trains",
    board_departing: "departing",
    board_arriving: "arriving",
    early: "early",
    humidity: "Humidity",
    wind_speed: "Wind",
    rain_chance: "Rain Chance",
    weather_current_stn: "Current Station",
    weather_next_stop: "Next Stop",
    weather_destination: "Destination",
    landmarks_title: "Landmarks & Natural Features Along the Route",
    view_on_map: "View on Map",
    live_weather_sync: "Live Weather Sync",
    saved_trains: "Saved Trains",
    fav_favourites: "Favourites",
    fav_recent: "Recent",
    fav_empty: "No favourites yet — tap the heart on any train to save it",
    fav_remove: "Remove from favourites",
    fav_add: "Add to favourites",
  },

  hi: {
    // Navigation Tabs (Natural Railway spoken Hindi)
    tab_live_map: "लाइव मैप",
    tab_routes: "ट्रेन मार्ग",
    tab_analytics: "यात्रा विवरण",
    tab_weather: "मौसम",
    tab_station: "स्टेशन बोर्ड",

    // Quick Menu & Preferences
    menu_title: "Preferences (प्राथमिकताएं)",
    appearance: "डार्क थीम",
    dark_mode: "डार्क मोड",
    dark_mode_sub: "रात में देखने के लिए आरामदायक",
    delay_alerts: "ट्रेन देरी अलर्ट",
    delay_alerts_sub: "देरी होने पर तुरंत सूचना",
    saved_trains: "सुरक्षित ट्रेनें (Saved)",
    saved_trains_sub: "पसंदीदा ट्रेनों तक सीधी पहुंच",
    share_status: "लाइव स्थिति साझा करें",
    share_status_sub: "व्हाट्सएप द्वारा ट्रेन लिंक भेजें",
    map_style: "नक्शा शैली (Map Style)",
    select_language: "भाषा चुनें / Select Language",
    select_language_sub: "अपनी पसंदीदा भाषा चुनें",

    // Map styles
    map_light: "लाइट",
    map_dark: "डार्क",
    map_satellite: "सैटेलाइट",
    map_terrain: "भूभाग",

    // Search Section
    search_placeholder: "ट्रेन नंबर या नाम से खोजें",
    search_track_live: "लाइव देखें",
    search_recent: "हाल की खोजें:",
    search_ticker_1: "ट्रेन की लाइव रनिंग स्थिति देखें",
    search_ticker_2: "किसी भी भारतीय ट्रेन को लाइव ट्रैक करें",

    // Live Status Card & Metrics
    status_card_title: "लाइव रनिंग स्थिति",
    status_on_time: "समय पर (ON TIME)",
    status_delayed: "देरी से",
    status_early: "समय से पहले",
    status_arrived: "पहुंच चुकी है",
    status_scheduled: "शेड्यूल",
    status_cancelled: "रद्द (Cancelled)",
    status_running: "रस्ते में है",
    journey_progress: "सफ़र की प्रगति (Progress)",
    current_station_spotlight: "वर्तमान / पिछला स्टेशन",
    next_station_spotlight: "अगला आने वाला स्टेशन",
    scheduled: "निर्धारित",
    reported: "वास्तविक",
    platform: "प्लेटफ़ॉर्म",
    pf_abbr: "प्लेटफ़ॉर्म",
    halt: "ठहराव",
    speed: "स्पीड",
    distance_covered: "तय दूरी",
    distance_remaining: "बची दूरी",
    runs_on: "चलने के दिन",
    running_today: "आज चलेगी",
    not_today: "आज नहीं चलेगी",
    find_between: "स्टेशनों के बीच ट्रेनें खोजें",
    find_between_sub: "स्टेशन A से स्टेशन B तक का समय व रूट",
    find_btn: "खोजें",
    telemetry_verified: "ताज़ा लाइव डेटा प्रदर्शित",

    // Timeline & Analytics
    full_journey_timeline: "पूरा टाइमलाइन व स्टेशन विवरण",
    overview: "अवलोकन (Overview)",
    description: "पूरा विवरण",
    show_desc: "विवरण देखें",
    hide_desc: "विवरण छिपाएं",
    next_station: "अगला स्टेशन",
    remaining: "शेष",
    at_station: "पर",
    delayed_by: "देरी",
    on_time: "समय पर",
    mins: "मिनट",
    stations: "स्टेशन",
    covered: "पूर्ण",
    scheduled_stop: "ठहराव",
    pass_through: "बिना रुके गुजरेगी",
    live_train: "लाइव ट्रेन",
    passed: "गुज़र चुकी",

    // Analytics Cards
    schedule_variance: "समय अंतर (Schedule Variance)",
    en_route_delay: "सफ़र में कुल देरी",
    journey_distance: "कुल यात्रा दूरी",
    highest_route_point: "मार्ग का सबसे ऊंचा बिंदु",
    near_station: "पास",
    route_elevation_profile: "मार्ग की ऊंचाई व ढलान (Elevation Profile)",
    route_elevation_desc: "रेल मार्ग के साथ जमीन की ऊंचाई (SRTM डेटा)",
    live_telemetry_velocity: "लाइव ट्रेन स्पीड व टेलीमेट्री ग्राफ़",
    live_speed_sub: "ट्रेन गति ग्राफ एवं धीमे होने के संकेत",
    peak_elevation: "उच्चतम",

    // Routes (A to B) Page
    rs_hero_title: "स्टेशनों के बीच ट्रेनें खोजें",
    rs_from_label: "कहाँ से (From)",
    rs_to_label: "कहाँ तक (To)",
    rs_from_placeholder: "प्रस्थान स्टेशन (जैसे NDLS, नई दिल्ली)",
    rs_to_placeholder: "गंतव्य स्टेशन (जैसे BPL, भोपाल)",
    rs_swap_title: "स्टेशन बदलें",
    rs_today: "आज",
    rs_tomorrow: "कल",
    rs_all_days: "सभी दिन",
    rs_find_trains: "ट्रेनें खोजें",
    rs_available_trains: "उपलब्ध ट्रेनें",
    rs_pan_india_trains: "प्रमुख भारतीय ट्रेनें व मार्ग",

    // Station Board Page
    board_hero_title: "लाइव स्टेशन बोर्ड (Arrival / Departure)",
    board_departures: "प्रस्थान (Departures)",
    board_arrivals: "आगमन (Arrivals)",
    board_expected: "अपेक्षित समय",
    board_direction: "दिशा / मार्ग",

    // Weather & Landmarks Page
    weather_hero_title: "लाइव मौसम व मार्ग के स्थल",
    weather_temp: "तापमान",
    weather_humidity: "नमी",
    weather_wind: "हवा की गति",
    landmarks_title: "मार्ग के प्रमुख स्थल",
    // New component keys (Hindi)
    delay_data_unavailable: "लाइव देरी डेटा उपलब्ध नहीं",
    running_on_schedule: "समय पर चल रही है",
    enroute_delay: "सफ़र में देरी हुई है",
    complete: "पूर्ण",
    route_data_loading: "रूट डेटा लोड हो रहा है",
    fetching_elevation: "ऊंचाई डेटा लोड हो रहा है",
    rs_showing: "दिखाई जा रही हैं",
    rs_trains: "ट्रेनें",
    board_scheduled: "निर्धारित",
    board_th_train: "ट्रेन",
    board_th_destination: "गंतव्य",
    board_th_origin: "प्रस्थान",
    board_departs: "रवाना",
    board_arrives: "आगमन",
    board_th_status: "स्थिति",
    board_track: "ट्रैक",
    board_no_trains: "कोई",
    board_listed_for: "के लिए उपलब्ध नहीं",
    board_live_timetable: "लाइव स्टेशन टाइमटेबल",
    board_station_label: "स्टेशन",
    board_plt: "प्लेटफ़ॉर्म",
    board_trains: "ट्रेनें",
    board_departing: "रवाना होती हैं",
    board_arriving: "आती हैं",
    early: "जल्दी",
    humidity: "नमी",
    wind_speed: "हवा",
    rain_chance: "बारिश की संभावना",
    weather_current_stn: "वर्तमान स्टेशन",
    weather_next_stop: "अगला ठहराव",
    weather_destination: "गंतव्य",
    weather_hero_title: "लाइव मौसम व रूट के स्थल",
    landmarks_title: "रूट पर प्रमुख स्थल व प्राकृतिक विशेषताएं",
    view_on_map: "नक्शे पर देखें",
    live_weather_sync: "लाइव मौसम अपडेट",
    saved_trains: "सुरक्षित ट्रेनें",
    fav_favourites: "पसंदीदा (Favourites)",
    fav_recent: "हाल की खोजें",
    fav_empty: "अभी कोई पसंदीदा नहीं — किसी भी ट्रेन पर दिल का निशान दबाएं",
    fav_remove: "पसंदीदा से हटाएं",
    fav_add: "पसंदीदा में जोड़ें",
  },

  bn: {
    tab_live_map: "লাইভ ম্যাপ",
    tab_routes: "ট্রেন রুট",
    tab_analytics: "যাত্রার বিবরণ",
    tab_weather: "আবহাওয়া",
    tab_station: "স্টেশন বোর্ড",
    menu_title: "Preferences (পছন্দসমূহ)",
    search_placeholder: "ট্রেন নম্বর বা নাম দিয়ে খুঁজুন",
    search_track_live: "লাইভ দেখুন",
    search_recent: "সাম্প্রতিক:",
    overview: "সংক্ষিপ্ত বিবরণ",
    description: "সম্পূর্ণ বিবরণ",
    show_desc: "বিবরণ দেখুন",
    hide_desc: "বিবরণ লুকান",
    next_station: "পরবর্তী স্টেশন",
    remaining: "বাকি",
    at_station: "এ",
    delayed_by: "বিলম্ব",
    on_time: "ঠিক সময়",
    platform: "প্ল্যাটফর্ম",
    halt: "বিরতি",
    mins: "মিনিট",
    stations: "স্টেশন",
    covered: "সম্পন্ন"
  },

  mr: {
    tab_live_map: "थेट नकाशा",
    tab_routes: "गाडीचे मार्ग",
    tab_analytics: "प्रवासाचा तपशील",
    tab_weather: "हवामान",
    tab_station: "स्थानक फलक",
    menu_title: "Preferences (प्राधान्ये)",
    search_placeholder: "गाडी क्रमांक किंवा नावाने शोधा",
    search_track_live: "थेट पहा",
    search_recent: "अलीकडील:",
    overview: "आढावा",
    description: "पूर्ण तपशील",
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
    tab_routes: "రైలు మార్గాలు",
    tab_analytics: "ప్రయాణ వివరాలు",
    tab_weather: "వాతావరణం",
    tab_station: "స్టేషన్ బోర్డు",
    menu_title: "Preferences (ప్రాధాన్యతలు)",
    search_placeholder: "రైలు సంఖ్య లేదా పేరుతో వెతకండి",
    search_track_live: "లైవ్ చూడండి",
    search_recent: "ఇటీవలి:",
    overview: "అవలోకనం",
    description: "పూర్తి వివరాలు",
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
    tab_routes: "ரயில் பாதைகள்",
    tab_analytics: "பயண விவரம்",
    tab_weather: "வானிலை",
    tab_station: "நிலைய பலகை",
    menu_title: "Preferences (விருப்பத்தேர்வுகள்)",
    search_placeholder: "ரயில் எண் அல்லது பெயரால் தேடுக",
    search_track_live: "நேரலை பார்க்க",
    search_recent: "சமீபத்திய:",
    overview: "மேலோட்டம்",
    description: "முழு விவரம்",
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
    tab_routes: "ટ્રેન રૂટ્સ",
    tab_analytics: "મુસાફરી વિગત",
    tab_weather: "હવામાન",
    tab_station: "સ્ટેશન બોર્ડ",
    menu_title: "Preferences (પસંદગીઓ)",
    search_placeholder: "ટ્રેન નંબર અથવા નામથી શોધો",
    search_track_live: "લાઈવ જુઓ",
    search_recent: "તાજેતરનું:",
    overview: "ઝાંખી",
    description: "સંપૂર્ણ વિગત",
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
    tab_routes: "ರೈಲು ಮಾರ್ಗಗಳು",
    tab_analytics: "ಪ್ರಯಾಣದ ವಿವರ",
    tab_weather: "ಹವಾಮಾನ",
    tab_station: "ನಿಲ್ದಾಣ ಬೋರ್ಡ್",
    menu_title: "Preferences (ಆದ್ಯತೆಗಳು)",
    search_placeholder: "ರೈಲು ಸಂಖ್ಯೆ ಅಥವಾ ಹೆಸರಿನಿಂದ ಹುಡುಕಿ",
    search_track_live: "ಲೈವ್ ನೋಡಿ",
    search_recent: "ಇತ್ತೀಚಿನ:",
    overview: "ಅವಲೋಕನ",
    description: "ಪೂರ್ಣ ವಿವರಣೆ",
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

/* ── Common Indian Railway Stations Bilingual Mapping (Spoken Devanagari) ── */
const STATION_NAMES_HINDI = {
  "New Delhi": "नई दिल्ली",
  "Delhi": "दिल्ली",
  "Old Delhi": "पुरानी दिल्ली",
  "Hazrat Nizamuddin": "हज़रत निज़ामुद्दीन",
  "Delhi Sarai Rohilla": "दिल्ली सराय रोहिल्ला",
  "Anand Vihar Terminal": "आनंद विहार टर्मिनल",
  "Jaipur": "जयपुर",
  "Jaipur Jn": "जयपुर जं.",
  "Gandhinagar Jaipur": "गांधीनगर जयपुर",
  "Getor Jagatpura": "गेतोर जगतपुरा",
  "Khatipura": "खातीपुरा",
  "Kanakpura": "कनकपुरा",
  "Bindayaka": "बिन्दायका",
  "Dhanakya": "धानक्या",
  "Sheosingh Pura": "शिवसिंहपुरा",
  "Bobas": "बोबास",
  "Asalpur Jobner": "आसलपुर जोबनेर",
  "Dhinda": "ढिंडा",
  "Hirnoda": "हिरनोदा",
  "Phulera Jn": "फुलेरा जं.",
  "Phulera Junction": "फुलेरा जंक्शन",
  "Naraina": "नरैना",
  "Kishangarh": "किशनगढ़",
  "Madar Jn": "मदार जं.",
  "Ajmer": "अजमेर",
  "Ajmer Jn": "अजमेर जं.",
  "Beawar": "ब्यावर",
  "Marwar Jn": "मारवाड़ जं.",
  "Falna": "फालना",
  "Abu Road": "आबू रोड",
  "Palanpur": "पालनपुर",
  "Mehsana": "मेहसाणा",
  "Ahmedabad": "अहमदाबाद",
  "Ahmedabad Jn": "अहमदाबाद जं.",
  "Vadodara": "वडोदरा",
  "Surat": "सूरत",
  "Vapi": "वापी",
  "Borivali": "बोरीवली",
  "Mumbai Central": "मुंबई सेंट्रल",
  "Bandra Terminus": "बांद्रा टर्मिनस",
  "Chhatrapati Shivaji Maharaj Terminus": "छत्रपति शिवाजी महाराज टर्मिनस",
  "Dadar": "दादर",
  "Kalyan": "कल्याण",
  "Pune": "पुणे",
  "Bhopal": "भोपाल",
  "Bhopal Jn": "भोपाल जं.",
  "Rani Kamlapati": "रानी कमलापति",
  "Itarsi": "इटारसी",
  "Nagpur": "नागपुर",
  "Jabalpur": "जबलपुर",
  "Gwalior": "ग्वालियर",
  "Jhansi": "झांसी (वीरांगना लक्ष्मीबाई)",
  "Agra Cantt": "आगरा कैंट",
  "Mathura Jn": "मथुरा जं.",
  "Kanpur Central": "कानपुर सेंट्रल",
  "Prayagraj Jn": "प्रयागराज जं.",
  "Prayagraj": "प्रयागराज",
  "Varanasi": "वाराणसी",
  "Varanasi Jn": "वाराणसी जं.",
  "Lucknow": "लखनऊ",
  "Lucknow Charbagh": "लखनऊ चारबाग",
  "Gorakhpur": "गोरखपुर",
  "Patna": "पटना",
  "Patna Jn": "पटना जं.",
  "Danapur": "दानापुर",
  "Gaya": "गया",
  "Deen Dayal Upadhyaya": "पंडित दीन दयाल उपाध्याय जं.",
  "Howrah": "हावड़ा",
  "Sealdah": "सियालदह",
  "Kolkata": "कोलकाता",
  "Amritsar": "अमृतसर",
  "Chandigarh": "चंडीगढ़",
  "Ludhiana": "लुधियाना",
  "Jodhpur": "जोधपुर",
  "Bikaner": "बीकानेर",
  "Kota": "कोटा",
  "Kota Jn": "कोटा जं.",
  "Udaipur City": "उदयपुर सिटी",
  "Indore": "इंदौर",
  "Ujjain": "उज्जैन",
  "Raipur": "रायपुर",
  "Bilaspur": "बिलासपुर",
  "Bhubaneswar": "भुवनेश्वर",
  "Puri": "पुरी",
  "Ranchi": "राँची",
  "Tatanagar": "टाटानगर",
  "Dhanbad": "धनबाद",
  "Guwahati": "गुवाहाटी",
  "Secunderabad": "सिकंदराबाद",
  "Hyderabad": "हैदराबाद",
  "Vijayawada": "विजयवाड़ा",
  "Visakhapatnam": "विशाखापट्टनम",
  "Chennai Central": "चेन्नई सेंट्रल",
  "Bengaluru City": "बेंगलुरु सिटी (KSR)",
  "Yesvantpur": "यशवंतपुर",
  "Coimbatore": "कोयंबटूर",
  "Madurai": "मदुरै",
  "Thiruvananthapuram Central": "तिरुवनंतपुरम सेंट्रल",
  "Ernakulam": "एर्नाकुलम"
};

/* ── Railway Suffix Translation (Jn, Cantt, Express, etc.) ── */
const RAIL_SUFFIX_MAP_HI = [
  [/\bJunction\b/gi, "जंक्शन"],
  [/\bJn\.?\b/gi, "जं."],
  [/\bCantonment\b/gi, "कैंट"],
  [/\bCantt\.?\b/gi, "कैंट"],
  [/\bCentral\b/gi, "सेंट्रल"],
  [/\bTerminus\b/gi, "टर्मिनस"],
  [/\bTerminal\b/gi, "टर्मिनल"],
  [/\bRoad\b/gi, "रोड"],
  [/\bRd\.?\b/gi, "रोड"],
  [/\bTown\b/gi, "टाउन"],
  [/\bCity\b/gi, "सिटी"],
  [/\bHalt\b/gi, "हॉल्ट"],
  [/\bSuperfast\b/gi, "सुपरफास्ट"],
  [/\bSF\b/g, "सुपरफास्ट"],
  [/\bExpress\b/gi, "एक्सप्रेस"],
  [/\bExp\.?\b/gi, "एक्सप्रेस"],
  [/\bMail\b/gi, "मेल"],
  [/\bPassenger\b/gi, "पैसेंजर"],
  [/\bPass\.?\b/gi, "पैसेंजर"],
  [/\bIntercity\b/gi, "इंटरसिटी"],
  [/\bSpecial\b/gi, "स्पेशल"],
  [/\bSpl\.?\b/gi, "स्पेशल"],
  [/\bRajdhani\b/gi, "राजधानी"],
  [/\bShatabdi\b/gi, "शताब्दी"],
  [/\bVande Bharat\b/gi, "वंदे भारत"],
  [/\bGarib Rath\b/gi, "गरीब रथ"],
  [/\bJan Shatabdi\b/gi, "जन शताब्दी"],
  [/\bDuronto\b/gi, "दुरंतो"],
  [/\bHumsafar\b/gi, "हमसफ़र"],
  [/\bAntyodaya\b/gi, "अंत्योदय"],
  [/\bDouble Decker\b/gi, "डबल डेकर"]
];

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

/** Translate or transliterate Station names cleanly */
export function translateStationName(name) {
  if (!name || typeof name !== "string") return name || "";
  const lang = getLanguage();
  if (lang === "en") return name;

  const trimmed = name.trim();
  // Exact match from curated Indian station dictionary
  if (STATION_NAMES_HINDI[trimmed]) {
    return STATION_NAMES_HINDI[trimmed];
  }

  // Handle common rail suffixes (Jn, Cantt, Road, etc.)
  let translated = trimmed;
  for (const [pattern, replacement] of RAIL_SUFFIX_MAP_HI) {
    translated = translated.replace(pattern, replacement);
  }
  return translated;
}

/** Translate Train name with categories (e.g. "Bhopal Jn. KWP BPL Express" ➔ "भोपाल जं. KWP BPL एक्सप्रेस") */
export function translateTrainName(name) {
  if (!name || typeof name !== "string") return name || "";
  const lang = getLanguage();
  if (lang === "en") return name;

  let translated = name;
  // Apply station replacements inside train name
  for (const [enName, hiName] of Object.entries(STATION_NAMES_HINDI)) {
    if (translated.includes(enName)) {
      translated = translated.replace(new RegExp(enName, "g"), hiName);
    }
  }

  // Apply category replacements (Express, Superfast, SF, Mail, etc.)
  for (const [pattern, replacement] of RAIL_SUFFIX_MAP_HI) {
    translated = translated.replace(pattern, replacement);
  }
  return translated;
}
