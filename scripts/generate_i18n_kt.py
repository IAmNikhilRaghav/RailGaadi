# -*- coding: utf-8 -*-
import sys

# Define base keys and English text
en = {
    # App Tabs & Status
    "tab_live_map": "Live Map",
    "tab_routes": "Routes",
    "tab_analytics": "Analytics",
    "tab_weather": "Weather",
    "tab_station": "Station",
    "status_on_time": "ON TIME",
    "status_delayed": "DELAYED",
    "status_early": "EARLY",
    "search_placeholder": "Search by train no. or name",
    "search_track_live": "Track Live",
    "speed": "Speed",
    "platform": "Platform",
    "running_today": "Runs today",
    "rs_filter_by": "Filter by:",
    "rs_all_dates": "All Dates",
    "rs_pan_india": "Pan-India Trains & Routes",
    "weekly_schedule": "Weekly Schedule:",
    "select_language": "Select Language",
    "halt": "Halt",
    "halt_mins": "m",
    "pass_through_stations": "pass-through stations",
    "stations": "stations",
    "crossing": "Crossing",
    "at": "At",
    "delayed_by": "Delayed by",
    "mins_delayed": "Delayed by",
    "mins_early": "Early",
    "on_time_label": "On Time",
    "start_station": "START",
    "end_station": "END",
    "journey_route_profile": "Journey Route Profile",
    "total_route": "Total Route",
    "halts": "Halts",
    "total_stops": "Total Stops",
    "are_you_inside": "Are you inside this train?",
    "enable_accurate_tracking": "Enable accurate tracking",
    "updated_just_now": "Updated just now",
    "km_to": "km to",
    "destination_alarm": "Destination Alarm",
    "search_train_prompt": "Search for a Train",
    "search_train_subprompt": "Tap above to track any train live",

    # Routes & Search (Photos 1, 2, 8, 11)
    "search_trains": "Search Trains",
    "from": "From",
    "to": "To",
    "station_code_or_name": "Station name or code",
    "today": "Today",
    "yesterday": "Yesterday",
    "tomorrow": "Tomorrow",
    "recently_searched_trains": "Recently Searched Trains",
    "track": "Track",
    "plan_journey_title": "Plan Your Train Journey",
    "plan_journey_sub": "Search from 8,690+ Indian Railway stations. Pick a date and track trains live.",
    "choose_the_date": "Choose the date",
    "for_departure_from": "for departure from",
    "choose_from_calendar": "Choose from Calendar",
    "cancel": "Cancel",
    "searching_trains": "Searching trains…",
    "trains_found": "Trains Found",
    "live_gps": "Live GPS",
    "delays": "Delays",
    "calendar": "Calendar",

    # Preferences (Photo 3)
    "preferences": "Preferences",
    "pref_dark_theme": "Dark Theme",
    "pref_dark_theme_sub": "Comfortable viewing at night",
    "pref_offline_gps": "Offline GPS Mode",
    "pref_offline_gps_sub": "Track inside trains with 0% internet",
    "pref_offline_gps_active": "Active • Tracking via Satellites",
    "pref_trip_delay": "Trip Delay Alerts",
    "pref_trip_delay_sub": "Live notifications on delay",
    "pref_select_language": "Select Language / भाषा",
    "pref_saved_trains": "Saved Trains & Routes",
    "pref_saved_trains_sub": "Quick access to pinned trains",
    "pref_share_status": "Share Live Status",
    "pref_share_status_sub": "Send journey status link",
    "pref_map_style": "MAP STYLE",
    "map_style_light": "Light",
    "map_style_dark": "Dark",
    "map_style_satellite": "Satellite",
    "map_style_terrain": "Terrain",

    # Weather & AQI (Photos 4, 9, 10)
    "corridor_stations_hint": "Corridor Stations (Tap to view local Weather & AQI)",
    "live_badge": "Live",
    "weather": "Weather",
    "feels_like": "Feels like",
    "refresh": "Refresh",
    "wind": "Wind",
    "humidity": "Humidity",
    "visibility": "Visibility",
    "precipitation": "Precipitation",
    "gusts_up_to": "Gusts up to",
    "gentle_airflow": "Gentle ambient airflow.",
    "dew_point": "Dew Point",
    "clear_view": "Clear View",
    "low_visibility": "Low Visibility",
    "chance": "chance",
    "none_expected": "None expected",
    "air_quality": "AIR QUALITY",
    "cpcb_naqi": "CPCB NAQI",
    "naqi_india": "National Air Quality Index (India)",
    "pollutant_concentrations": "POLLUTANT CONCENTRATIONS (CPCB)",
    "primary_pollutant": "Primary",
    "standard_cpcb": "Standard: Central Pollution Control Board (CPCB) NAQI · Copernicus CAMS via ECMWF",
    "observed": "Observed",
    "aqi_unavailable": "AQI data unavailable for this location",
    "no_cpcb_data": "No CPCB station data or modelled estimate available.",
    "aqi_good": "Good",
    "aqi_satisfactory": "Satisfactory",
    "aqi_moderate": "Moderate",
    "aqi_poor": "Poor",
    "aqi_very_poor": "Very Poor",
    "aqi_severe": "Severe",

    # AQI Advisory Guide (Photo 9)
    "health_advisory_clean_air": "Health Advisory & Clean Air Action",
    "tap_to_collapse": "Tap to collapse",
    "tap_to_expand": "Consequences, travel protection & citizen tips",
    "hide": "Hide",
    "guide": "Guide",
    "tab_health_risks": "Health Risks",
    "tab_travel_care": "Travel Care",
    "tab_clean_city": "Clean City",
    "current_impact": "Current Impact",
    "impact_hazardous": "Hazardous: Severe respiratory and cardiovascular strain across healthy population; emergency for lung/heart patients.",
    "impact_very_poor": "Very Poor: Triggers respiratory illness on prolonged exposure. Significant risk for asthma, bronchitis, or heart conditions.",
    "impact_poor": "Poor: Causes breathing discomfort, throat burning, and eye stinging on prolonged outdoor exposure.",
    "impact_moderate": "Moderate: Breathing discomfort for sensitive individuals, children, and elderly travelers on extended platform stays.",
    "impact_good": "Good / Satisfactory: Air quality is safe for normal outdoor breathing.",

    "adv_lungs_title": "Lungs & Airway",
    "adv_lungs_desc": "Fine PM2.5 penetrates deep into alveoli and bloodstream, reducing oxygen uptake and causing persistent coughing or wheezing.",
    "adv_eyes_title": "Eyes & Throat",
    "adv_eyes_desc": "Nitrogen dioxide (NO₂) and ozone cause stinging eyes, dry throat, and sinus congestion along rail corridors.",
    "adv_risk_title": "High-Risk Groups",
    "adv_risk_desc": "Children, elderly travelers, pregnant women, and asthmatics suffer 3x higher vulnerability during railway transit in smog.",

    "adv_mask_title": "Wear N95 Mask",
    "adv_mask_desc": "Cloth masks don't filter PM2.5. Wear a certified N95/FFP2 mask when waiting on open platforms or at carriage doors.",
    "adv_windows_title": "Pull Down Windows",
    "adv_windows_desc": "In Sleeper and General coaches, keep glass shutters down while crossing major industrial corridors and congested rail yards.",
    "adv_hydration_title": "Regular Warm Hydration",
    "adv_hydration_desc": "Sipping warm water or tea helps clear toxic particles trapped in throat mucous membranes and prevents dehydration.",
    "adv_saline_title": "Saline Eye / Nasal Relief",
    "adv_saline_desc": "Carry sterile lubricating eye drops and saline nasal spray to quickly rinse smog particulate residue at station halts.",
    "adv_strolls_title": "Minimize Platform Strolls",
    "adv_strolls_desc": "Avoid brisk walking or heavy exercise during long halts; stay inside shaded, ventilated waiting halls or AC coaches.",

    "adv_burning_title": "Zero Open Burning",
    "adv_burning_desc": "Never burn dried leaves, plastic, or domestic waste. Report illegal garbage or farm fires on the official CPCB Sameer App.",
    "adv_transit_title": "Champion Public Transit",
    "adv_transit_desc": "Traveling by Indian Railways and Metro rather than solo cars prevents kilograms of carbon and particulate emissions per trip.",
    "adv_plants_title": "Plant Native Air Purifiers",
    "adv_plants_desc": "Grow native trees (Peepal, Neem, Banyan) in your society and keep indoor plants (Snake Plant, Areca Palm, Tulsi) at home.",
    "adv_puc_title": "Vehicle PUC & Carpooling",
    "adv_puc_desc": "Keep your vehicle's PUC certification valid, maintain correct tyre pressure, and turn off your engine at railway crossings.",
    "adv_dust_title": "Dust Suppression",
    "adv_dust_desc": "Sprinkle water on loose soil or unpaved paths around your home/shop to prevent ambient dust from kicking up into the air.",

    # Landmarks (Photo 9)
    "nearby_landmarks_title": "Nearby Landmarks & Monuments",
    "no_landmarks_nearby": "No Landmarks Nearby",
    "no_landmarks_nearby_desc": "No major landmarks found within 60 km of the current station.",
    "open_maps": "Open Maps",
    "cat_heritage": "Heritage",
    "cat_monument": "Monument",
    "cat_river": "River",
    "cat_bridge": "Bridge",
    "cat_mountain": "Mountain",

    # LiveMap Onboarding & Navigation (Photos 5, 6)
    "directions_onboarding_title": "DIRECTIONS TO YOUR ONBOARDING STATION",
    "boarding_station_prompt": "Boarding Station (Where you start your journey):",
    "boarding_suffix": "(Boarding)",
    "origin_suffix": "(Origin)",
    "fastest": "Fastest",
    "alternative": "Alternative",
    "recommended": "Recommended",
    "start_fastest": "Start (Fastest)",
    "start_alternative": "Start (Alternative)",
    "open_google_maps": "Open Google Maps",
    "next_stop": "Next Stop",
    "next_halt": "Next Halt",
    "delay_label": "Delay",
    "late_label": "LATE",
    "directions_to": "Directions to",

    # Destination Alarm (Photo 7)
    "alarm_title": "Destination Wake-Up Alarm",
    "alarm_sub": "GPS geofence alerts you even in sleep mode",
    "target_destination": "TARGET DESTINATION STATION",
    "change": "Change",
    "when_to_wake": "WHEN TO WAKE YOU UP",
    "by_distance": "By Distance",
    "by_time": "By Time",
    "custom_distance": "Custom Distance",
    "enter_any_distance": "Enter any distance (1 - 999 km)",
    "custom_time": "Custom Time",
    "enter_minutes_before": "Enter minutes before arrival",
    "alarm_auto_distance": "Alarms automatically as soon as the train is",
    "alarm_auto_time": "Alarms estimated",
    "alert_sound_tones": "ALERT SOUND & TONES",
    "station_voice_announcement": "Station Voice Announcement",
    "station_voice_sub": "IR-style speaker announcement",
    "continuous_vibration": "Continuous Vibration",
    "vibration_sub": "Haptic pulses while phone is in pocket",
    "test_sound": "Test Sound",
    "arm_wake_up_alarm": "Arm Wake-Up Alarm",
    "update_alarm": "Update Alarm",
    "turn_off_alarm": "Turn Off Wake-Up Alarm",
    "armed_badge": "ARMED"
}

# 2. HINDI (hi)
hi = {
    "tab_live_map": "लाइव मैप",
    "tab_routes": "ट्रेन रूट्स",
    "tab_analytics": "एनालिटिक्स",
    "tab_weather": "मौसम",
    "tab_station": "स्टेशन",
    "status_on_time": "समय पर",
    "status_delayed": "देरी से",
    "status_early": "समय से पहले",
    "search_placeholder": "ट्रेन नंबर या नाम दर्ज करें",
    "search_track_live": "लाइव ट्रैक करें",
    "speed": "गति",
    "platform": "प्लेटफ़ॉर्म",
    "running_today": "आज चलेगी",
    "rs_filter_by": "फ़िल्टर करें:",
    "rs_all_dates": "सभी तारीखें",
    "rs_pan_india": "अखिल भारतीय ट्रेनें व मार्ग",
    "weekly_schedule": "साप्ताहिक समय-सारणी:",
    "select_language": "भाषा चुनें",
    "halt": "स्टॉप",
    "halt_mins": "मि.",
    "pass_through_stations": "बीच के स्टेशन",
    "stations": "स्टेशन",
    "crossing": "क्रॉस कर रही है",
    "at": "पर",
    "delayed_by": "देरी",
    "mins_delayed": "देरी",
    "mins_early": "पहले",
    "on_time_label": "समय पर",
    "start_station": "आरंभ",
    "end_station": "समाप्त",
    "journey_route_profile": "यात्रा मार्ग प्रोफाइल",
    "total_route": "कुल दूरी",
    "halts": "ठहराव",
    "total_stops": "कुल स्टेशन",
    "are_you_inside": "क्या आप इस ट्रेन के अंदर हैं?",
    "enable_accurate_tracking": "सटीक ट्रैकिंग चालू करें",
    "updated_just_now": "अभी-अभी अपडेट हुआ",
    "km_to": "किमी शेष",
    "destination_alarm": "गंतव्य अलार्म",
    "search_train_prompt": "ट्रेन खोजें",
    "search_train_subprompt": "लाइव ट्रैक करने के लिए ऊपर टैप करें",

    # Routes & Search
    "search_trains": "ट्रेन खोजें",
    "from": "कहाँ से",
    "to": "कहाँ तक",
    "station_code_or_name": "स्टेशन का नाम या कोड",
    "today": "आज",
    "yesterday": "कल (बीता हुआ)",
    "tomorrow": "कल (आने वाला)",
    "recently_searched_trains": "हाल ही में खोजी गई ट्रेनें",
    "track": "ट्रैक करें",
    "plan_journey_title": "अपनी ट्रेन यात्रा की योजना बनाएं",
    "plan_journey_sub": "8,690+ भारतीय रेलवे स्टेशनों से खोजें। तारीख चुनें और ट्रेन लाइव ट्रैक करें।",
    "choose_the_date": "तारीख चुनें",
    "for_departure_from": "प्रस्थान स्टेशन:",
    "choose_from_calendar": "कैलेंडर से चुनें",
    "cancel": "रद्द करें",
    "searching_trains": "ट्रेनें खोजी जा रही हैं…",
    "trains_found": "ट्रेनें मिलीं",
    "live_gps": "लाइव जीपीएस",
    "delays": "देरी सूचना",
    "calendar": "कैलेंडर",

    # Preferences
    "preferences": "प्राथमिकताएं",
    "pref_dark_theme": "डार्क थीम",
    "pref_dark_theme_sub": "रात में आंखों के लिए आरामदायक दृश्य",
    "pref_offline_gps": "ऑफलाइन जीपीएस मोड",
    "pref_offline_gps_sub": "बिना इंटरनेट ट्रेन के अंदर ट्रैक करें",
    "pref_offline_gps_active": "सक्रिय • सैटेलाइट द्वारा ट्रैकिंग",
    "pref_trip_delay": "यात्रा देरी अलर्ट",
    "pref_trip_delay_sub": "ट्रेन देरी पर लाइव सूचनाएं प्राप्त करें",
    "pref_select_language": "भाषा चुनें / Select Language",
    "pref_saved_trains": "सहेजी गई ट्रेनें और रूट्स",
    "pref_saved_trains_sub": "पिन की गई ट्रेनों तक त्वरित पहुंच",
    "pref_share_status": "लाइव स्थिति साझा करें",
    "pref_share_status_sub": "यात्रा स्थिति का लिंक भेजें",
    "pref_map_style": "मानचित्र शैली (MAP STYLE)",
    "map_style_light": "लाइट",
    "map_style_dark": "डार्क",
    "map_style_satellite": "सैटेलाइट",
    "map_style_terrain": "भूभाग (Terrain)",

    # Weather & AQI
    "corridor_stations_hint": "रूट स्टेशन (स्थानीय मौसम व AQI देखने के लिए टैप करें)",
    "live_badge": "लाइव",
    "weather": "मौसम",
    "feels_like": "महसूस",
    "refresh": "रिफ्रेश",
    "wind": "हवा",
    "humidity": "आर्द्रता",
    "visibility": "दृश्यता",
    "precipitation": "बारिश / वर्षण",
    "gusts_up_to": "हवा के झोंके",
    "gentle_airflow": "हल्की प्राकृतिक हवा।",
    "dew_point": "ओस बिंदु",
    "clear_view": "साफ़ दृश्य",
    "low_visibility": "कम दृश्यता",
    "chance": "संभावना",
    "none_expected": "कोई संभावना नहीं",
    "air_quality": "वायु गुणवत्ता (AIR QUALITY)",
    "cpcb_naqi": "CPCB राष्ट्रीय NAQI",
    "naqi_india": "राष्ट्रीय वायु गुणवत्ता सूचकांक (भारत)",
    "pollutant_concentrations": "प्रदूषक सांद्रता (CPCB)",
    "primary_pollutant": "मुख्य प्रदूषक",
    "standard_cpcb": "मानक: केंद्रीय प्रदूषण नियंत्रण बोर्ड (CPCB) · कॉपरनिकस CAMS ECMWF",
    "observed": "अवलोकन समय",
    "aqi_unavailable": "इस स्थान के लिए AQI डेटा उपलब्ध नहीं है",
    "no_cpcb_data": "कोई CPCB स्टेशन डेटा या मॉडल अनुमान उपलब्ध नहीं।",
    "aqi_good": "अच्छा (Good)",
    "aqi_satisfactory": "संतोषजनक (Satisfactory)",
    "aqi_moderate": "मध्यम (Moderate)",
    "aqi_poor": "खराब (Poor)",
    "aqi_very_poor": "बहुत खराब (Very Poor)",
    "aqi_severe": "गंभीर (Severe)",

    # AQI Advisory Guide
    "health_advisory_clean_air": "स्वास्थ्य सलाह और स्वच्छ वायु कदम",
    "tap_to_collapse": "बंद करने के लिए टैप करें",
    "tap_to_expand": "स्वास्थ्य प्रभाव, यात्रा सुरक्षा और नागरिक सुझाव",
    "hide": "छिपाएं ▲",
    "guide": "गाइड ▼",
    "tab_health_risks": "🩺 स्वास्थ्य जोखिम",
    "tab_travel_care": "🛡️ यात्रा सुरक्षा",
    "tab_clean_city": "🌱 स्वच्छ शहर",
    "current_impact": "वर्तमान प्रभाव",
    "impact_hazardous": "अति गंभीर: स्वस्थ लोगों में श्वसन व हृदय संबंधी गंभीर तनाव; फेफड़े व हृदय रोगियों के लिए आपातकाल।",
    "impact_very_poor": "बहुत खराब: लंबे समय तक रहने पर सांस की बीमारी हो सकती है। अस्थमा, ब्रोंकाइटिस और हृदय रोगियों के लिए बड़ा जोखिम।",
    "impact_poor": "खराब: लंबे समय तक संपर्क में रहने पर सांस लेने में तकलीफ, गले में जलन और आंखों में जलन पैदा करता है।",
    "impact_moderate": "मध्यम: संवेदनशील लोगों, बच्चों और बुजुर्गों को प्लेटफॉर्म पर लंबे ठहराव के दौरान सांस लेने में परेशानी हो सकती है।",
    "impact_good": "अच्छा / संतोषजनक: हवा की गुणवत्ता सामान्य रूप से सांस लेने के लिए बिल्कुल सुरक्षित है।",

    "adv_lungs_title": "फेफड़े और श्वसन मार्ग",
    "adv_lungs_desc": "बारीक PM2.5 कण फेफड़ों की गहराई में जाकर रक्तप्रवाह में मिल जाते हैं, जिससे ऑक्सीजन कम होती है और खांसी या घरघराहट होती है।",
    "adv_eyes_title": "आंखें और गला",
    "adv_eyes_desc": "नाइट्रोजन डाइऑक्साइड (NO₂) और ओजोन के कारण रेल गलियारों में आंखों में जलन, सूखा गला और साइनस की समस्या होती है।",
    "adv_risk_title": "उच्च जोखिम वाले समूह",
    "adv_risk_desc": "स्मॉग के दौरान रेलवे यात्रा में बच्चों, बुजुर्गों, गर्भवती महिलाओं और अस्थमा रोगियों को 3 गुना अधिक संवेदनशीलता होती है।",

    "adv_mask_title": "N95 मास्क पहनें",
    "adv_mask_desc": "कपड़े का मास्क PM2.5 को नहीं रोकता। खुले प्लेटफॉर्म या कोच के दरवाजे पर खड़े होने पर प्रमाणित N95/FFP2 मास्क पहनें।",
    "adv_windows_title": "खिड़कियां नीचे रखें",
    "adv_windows_desc": "स्लीपर और जनरल कोचों में औद्योगिक क्षेत्रों व बड़े यार्डों से गुजरते समय कांच के शटर नीचे रखें।",
    "adv_hydration_title": "नियमित गुनगुना पानी पिएं",
    "adv_hydration_desc": "गुनगुना पानी या चाय पीने से गले की श्लेष्मा झिल्ली में फंसे विषाक्त कण साफ होते हैं और निर्जलीकरण नहीं होता।",
    "adv_saline_title": "आई ड्रॉप्स और सेलाइन स्प्रे",
    "adv_saline_desc": "स्टेशन पर ठहराव के दौरान स्मॉग के कणों को साफ करने के लिए सेलाइन आई ड्रॉप्स व नेज़ल स्प्रे साथ रखें।",
    "adv_strolls_title": "प्लेटफॉर्म पर ज्यादा न घूमें",
    "adv_strolls_desc": "लंबे ठहराव के दौरान भारी व्यायाम या तेज टहलने से बचें; हवादार प्रतीक्षालय या वातानुकूलित कोच के अंदर रहें।",

    "adv_burning_title": "खुले में कचरा न जलाएं",
    "adv_burning_desc": "सूखे पत्ते, प्लास्टिक या कचरा कभी न जलाएं। अवैध आग लगने पर आधिकारिक CPCB समीर ऐप पर तुरंत रिपोर्ट करें।",
    "adv_transit_title": "सार्वजनिक परिवहन को बढ़ावा दें",
    "adv_transit_desc": "निजी कारों के बजाय भारतीय रेल और मेट्रो से यात्रा करने से प्रति यात्रा भारी मात्रा में प्रदूषण कम होता है।",
    "adv_plants_title": "देशी वायु शोधक पेड़ लगाएं",
    "adv_plants_desc": "सोसाइटी में पीपल, नीम, बरगद लगाएं और घर में स्नेक प्लांट, एरिका पाम, तुलसी जैसे पौधे रखें।",
    "adv_puc_title": "पीयूसी और कारपूलिंग",
    "adv_puc_desc": "वाहन का पीयूसी वैध रखें, टायर का दबाव सही रखें और रेलवे क्रॉसिंग पर इंजन बंद करें।",
    "adv_dust_title": "धूल नियंत्रण",
    "adv_dust_desc": "घर या दुकान के आसपास की खुली मिट्टी पर पानी छिड़कें ताकि हवा में धूल न उड़े।",

    # Landmarks
    "nearby_landmarks_title": "🏛️ पास के ऐतिहासिक स्थल और स्मारक",
    "no_landmarks_nearby": "आस-पास कोई प्रमुख स्मारक नहीं",
    "no_landmarks_nearby_desc": "वर्तमान स्टेशन के 60 किमी के दायरे में कोई प्रमुख ऐतिहासिक स्थल नहीं मिला।",
    "open_maps": "मानचित्र खोलें",
    "cat_heritage": "धरोहर",
    "cat_monument": "स्मारक",
    "cat_river": "नदी",
    "cat_bridge": "रेल पुल",
    "cat_mountain": "पहाड़",

    # LiveMap Onboarding & Navigation
    "directions_onboarding_title": "आपके बोर्डिंग स्टेशन का रास्ता",
    "boarding_station_prompt": "बोर्डिंग स्टेशन (जहाँ से आपकी यात्रा शुरू होती है):",
    "boarding_suffix": "(बोर्डिंग)",
    "origin_suffix": "(आरंभ)",
    "fastest": "सबसे तेज़",
    "alternative": "वैकल्पिक मार्ग",
    "recommended": "अनुशंसित",
    "start_fastest": "शुरू करें (सबसे तेज़)",
    "start_alternative": "शुरू करें (वैकल्पिक)",
    "open_google_maps": "गूगल मैप्स में खोलें",
    "next_stop": "अगला स्टॉप",
    "next_halt": "अगला ठहराव",
    "delay_label": "देरी",
    "late_label": "देरी",
    "directions_to": "दिशानिर्देश:",

    # Destination Alarm
    "alarm_title": "गंतव्य वेक-अप अलार्म",
    "alarm_sub": "स्लीप मोड में भी जीपीएस जियोफेंस आपको जगा देगा",
    "target_destination": "लक्षित गंतव्य स्टेशन",
    "change": "बदलें",
    "when_to_wake": "अलार्म कब बजे",
    "by_distance": "दूरी के आधार पर",
    "by_time": "समय के आधार पर",
    "custom_distance": "कस्टम दूरी",
    "enter_any_distance": "दूरी दर्ज करें (1 - 999 किमी)",
    "custom_time": "कस्टम समय",
    "enter_minutes_before": "पहुंचने से कितने मिनट पहले",
    "alarm_auto_distance": "ट्रेन जैसे ही गंतव्य से इतनी दूरी पर होगी अलार्म बज जाएगा:",
    "alarm_auto_time": "ट्रेन पहुंचने से अनुमानित इतने मिनट पहले अलार्म बजेगा:",
    "alert_sound_tones": "अलार्म टोन व ध्वनियाँ",
    "station_voice_announcement": "स्टेशन वॉयस अनाउंसमेंट",
    "station_voice_sub": "भारतीय रेलवे शैली का उद्घोषक",
    "continuous_vibration": "लगातार कंपन (Vibration)",
    "vibration_sub": "जेब में फोन होने पर हैप्टिक पल्स",
    "test_sound": "ध्वनि जांचें",
    "arm_wake_up_alarm": "अलार्म सेट करें",
    "update_alarm": "अलार्म अपडेट करें",
    "turn_off_alarm": "अलार्म बंद करें",
    "armed_badge": "सक्रिय"
}

# Python Brahmic transliterator for generating Bengali, Marathi, Telugu, Tamil, Gujarati, Kannada, Malayalam, Punjabi
def devanagari_to_lang(text, target_lang):
    if target_lang in ("hi", "mr", "en"):
        return text

    offsets = {
        "bn": 0x0980 - 0x0900,
        "pa": 0x0A00 - 0x0900,
        "gu": 0x0A80 - 0x0900,
        "te": 0x0C00 - 0x0900,
        "kn": 0x0C80 - 0x0900,
        "ml": 0x0D00 - 0x0900,
        "ta": 0x0B80 - 0x0900,
    }

    tamil_map = {
        'ख': "க", 'ग': "க", 'घ': "க",
        'छ': "ச", 'ज': "ஜ", 'झ': "ச",
        'ठ': "ட", 'ड': "ட", 'ढ': "ட",
        'थ': "த", 'द': "த", 'ध': "த",
        'फ': "ப", 'ब': "ப", 'भ': "ப",
        'श': "ஷ", 'ष': "ஷ", 'स': "ஸ",
        'ज़': "ஜ", 'फ़': "ப"
    }

    if target_lang == "ta":
        sb = []
        for ch in text:
            if ch in tamil_map:
                sb.append(tamil_map[ch])
            else:
                cp = ord(ch)
                if 0x0900 <= cp <= 0x097F:
                    tcp = cp + offsets["ta"]
                    if 0x0B82 <= tcp <= 0x0BD7:
                        sb.append(chr(tcp))
                    else:
                        sb.append(ch)
                else:
                    sb.append(ch)
        return "".join(sb)

    if target_lang in offsets:
        off = offsets[target_lang]
        sb = []
        for ch in text:
            cp = ord(ch)
            if 0x0900 <= cp <= 0x097F:
                sb.append(chr(cp + off))
            else:
                sb.append(ch)
        return "".join(sb)

    return text

# Specific authentic hand-crafted terms for each regional language
# Marathi (mr)
mr = dict(hi)
mr["tab_routes"] = "रेल्वे मार्ग"
mr["tab_station"] = "स्थानक"
mr["status_on_time"] = "वेळेवर"
mr["status_delayed"] = "उशिरा"
mr["search_placeholder"] = "गाडी क्र. किंवा नाव शोधा"
mr["search_trains"] = "गाडी शोधा"
mr["from"] = "कुठून"
mr["to"] = "कुठे"
mr["today"] = "आज"
mr["yesterday"] = "काल"
mr["tomorrow"] = "उद्या"
mr["recently_searched_trains"] = "अलीकडे शोधलेल्या गाड्या"
mr["preferences"] = "प्राधान्ये"
mr["pref_dark_theme"] = "डार्क थीम"
mr["pref_offline_gps"] = "ऑफलाइन जीपीएस मोड"
mr["pref_trip_delay"] = "विलंब सूचना"
mr["weather"] = "हवामान"
mr["wind"] = "वारा"
mr["humidity"] = "आर्द्रता"
mr["visibility"] = "दृश्यमानता"
mr["precipitation"] = "पर्जन्यवृष्टी"
mr["air_quality"] = "हवेची गुणवत्ता"
mr["fastest"] = "सर्वात जलद"
mr["alarm_title"] = "गंतव्य वेक-अप अलार्म"
mr["arm_wake_up_alarm"] = "अलार्म सुरू करा"
mr["open_google_maps"] = "गुगल मॅप्स उघडा"
mr["choose_the_date"] = "तारीख निवडा"
mr["cancel"] = "रद्द करा"

# Bengali (bn)
bn = {k: devanagari_to_lang(v, "bn") for k, v in hi.items()}
bn["tab_live_map"] = "লাইভ ম্যাপ"
bn["tab_routes"] = "ট্রেন রুট"
bn["tab_analytics"] = "অ্যানালিটিক্স"
bn["tab_weather"] = "আবহাওয়া"
bn["tab_station"] = "স্টেশন"
bn["status_on_time"] = "ঠিক সময়ে"
bn["status_delayed"] = "বিলম্বিত"
bn["search_trains"] = "ট্রেন খুঁজুন"
bn["from"] = "কোথা থেকে"
bn["to"] = "কোথায়"
bn["today"] = "আজ"
bn["yesterday"] = "গতকাল"
bn["tomorrow"] = "আগামীকাল"
bn["recently_searched_trains"] = "সম্প্রতি খোঁজা ট্রেন"
bn["preferences"] = "পছন্দসমূহ"
bn["weather"] = "আবহাওয়া"
bn["wind"] = "বাতাস"
bn["humidity"] = "আর্দ্রতা"
bn["visibility"] = "দৃশ্যমানতা"
bn["air_quality"] = "বায়ুর মান"
bn["fastest"] = "দ্রুততম"
bn["open_google_maps"] = "গুগল ম্যাপ খুলুন"
bn["cancel"] = "বাতিল"

# Telugu (te)
te = {k: devanagari_to_lang(v, "te") for k, v in hi.items()}
te["tab_live_map"] = "లైవ్ మ్యాప్"
te["tab_routes"] = "రైలు రూట్లు"
te["tab_analytics"] = "విశ్లేషణలు"
te["tab_weather"] = "వాతావరణం"
te["tab_station"] = "స్టేషన్"
te["status_on_time"] = "సమయానికి"
te["status_delayed"] = "ఆలస్యం"
te["search_trains"] = "రైళ్లు శోధించండి"
te["from"] = "ఎక్కడి నుండి"
te["to"] = "ఎక్కడికి"
te["today"] = "ఈ రోజు"
te["yesterday"] = "నిన్న"
te["tomorrow"] = "రేపు"
te["recently_searched_trains"] = "ఇటీవల శోధించిన రైళ్లు"
te["preferences"] = "ప్రాధాన్యతలు"
te["weather"] = "వాతావరణం"
te["wind"] = "గాలి"
te["humidity"] = "తేమ"
te["visibility"] = "దృశ్యత"
te["air_quality"] = "గాలి నాణ్యత"
te["fastest"] = "వేగవంతమైనది"
te["open_google_maps"] = "గూగుల్ మ్యాప్స్ తెరవండి"
te["cancel"] = "రద్దు చేయండి"

# Tamil (ta)
ta = {k: devanagari_to_lang(v, "ta") for k, v in hi.items()}
ta["tab_live_map"] = "நேரலை வரைபடம்"
ta["tab_routes"] = "ரயில் பாதைகள்"
ta["tab_analytics"] = "பகுப்பாய்வு"
ta["tab_weather"] = "வானிலை"
ta["tab_station"] = "நிலையம்"
ta["status_on_time"] = "சரியான நேரம்"
ta["status_delayed"] = "தாமதம்"
ta["search_trains"] = "ரயில்களைத் தேடுங்கள்"
ta["from"] = "புறப்படும் இடம்"
ta["to"] = "சேருமிடம்"
ta["today"] = "இன்று"
ta["yesterday"] = "நேற்று"
ta["tomorrow"] = "நாளை"
ta["recently_searched_trains"] = "சமீபத்தில் தேடிய ரயில்கள்"
ta["preferences"] = "விருப்பத்தேர்வுகள்"
ta["weather"] = "வானிலை"
ta["wind"] = "காற்று"
ta["humidity"] = "ஈரப்பதம்"
ta["visibility"] = "பார்வைத்திறன்"
ta["air_quality"] = "காற்றின் தரம்"
ta["fastest"] = "மிக விரைவானது"
ta["open_google_maps"] = "கூகுள் மேப்ஸைத் திறக்கவும்"
ta["cancel"] = "ரத்துசெய்"

# Gujarati (gu)
gu = {k: devanagari_to_lang(v, "gu") for k, v in hi.items()}
gu["tab_live_map"] = "લાઇવ મેપ"
gu["tab_routes"] = "ટ્રેન રૂટ્સ"
gu["tab_analytics"] = "એનાલિટિક્સ"
gu["tab_weather"] = "હવામાન"
gu["tab_station"] = "સ્ટેશન"
gu["status_on_time"] = "સમયસર"
gu["status_delayed"] = "વિલંબિત"
gu["search_trains"] = "ટ્રેન શોધો"
gu["from"] = "ક્યાંથી"
gu["to"] = "ક્યાં સુધી"
gu["today"] = "આજે"
gu["yesterday"] = "ગઈકાલે"
gu["tomorrow"] = "આવતીકાલે"
gu["recently_searched_trains"] = "તાજેતરમાં શોધેલી ટ્રેનો"
gu["preferences"] = "પસંદગીઓ"
gu["weather"] = "હવામાન"
gu["wind"] = "પવન"
gu["humidity"] = "ભેજ"
gu["visibility"] = "દૃશ્યતા"
gu["air_quality"] = "હવાની ગુણવત્તા"
gu["fastest"] = "સૌથી ઝડપી"
gu["open_google_maps"] = "ગૂગલ મેપ્સ ખોલો"
gu["cancel"] = "રદ કરો"

# Kannada (kn)
kn = {k: devanagari_to_lang(v, "kn") for k, v in hi.items()}
kn["tab_live_map"] = "ಲೈವ್ ನಕ್ಷೆ"
kn["tab_routes"] = "ರೈಲು ಮಾರ್ಗಗಳು"
kn["tab_analytics"] = "ವಿಶ್ಲೇಷಣೆ"
kn["tab_weather"] = "ಹವಾಮಾನ"
kn["tab_station"] = "ನಿಲ್ದಾಣ"
kn["status_on_time"] = "ಸಮಯಕ್ಕೆ ಸರಿಯಾಗಿ"
kn["status_delayed"] = "ವಿಳಂಬ"
kn["search_trains"] = "ರೈಲುಗಳನ್ನು ಹುಡುಕಿ"
kn["from"] = "ಎಲ್ಲಿಂದ"
kn["to"] = "ಎಲ್ಲಿಗೆ"
kn["today"] = "ಇಂದು"
kn["yesterday"] = "ನಿನ್ನೆ"
kn["tomorrow"] = "ನಾಳೆ"
kn["recently_searched_trains"] = "ಇತ್ತೀಚೆಗೆ ಹುಡುಕಿದ ರೈಲುಗಳು"
kn["preferences"] = "ಆದ್ಯತೆಗಳು"
kn["weather"] = "ಹವಾಮಾನ"
kn["wind"] = "ಗಾಳಿ"
kn["humidity"] = "ಆರ್ದ್ರತೆ"
kn["visibility"] = "ಗೋಚರತೆ"
kn["air_quality"] = "ವಾಯು ಗುಣಮಟ್ಟ"
kn["fastest"] = "ಅತ್ಯಂತ ವೇಗದ"
kn["open_google_maps"] = "ಗೂಗಲ್ ಮ್ಯಾಪ್ಸ್ ತೆರೆಯಿರಿ"
kn["cancel"] = "ರದ್ದುಮಾಡಿ"

# Malayalam (ml)
ml = {k: devanagari_to_lang(v, "ml") for k, v in hi.items()}
ml["tab_live_map"] = "തത്സമയ മാപ്പ്"
ml["tab_routes"] = "ട്രെയിൻ റൂട്ടുകൾ"
ml["tab_analytics"] = "വിശകലനം"
ml["tab_weather"] = "കാലാവസ്ഥ"
ml["tab_station"] = "സ്റ്റേഷൻ"
ml["status_on_time"] = "കൃത്യസമയത്ത്"
ml["status_delayed"] = "വൈകി"
ml["search_trains"] = "ട്രെയിനുകൾ തിരയുക"
ml["from"] = "എവിടെ നിന്ന്"
ml["to"] = "എങ്ങോട്ട്"
ml["today"] = "ഇന്ന്"
ml["yesterday"] = "ഇന്നലെ"
ml["tomorrow"] = "നാളെ"
ml["recently_searched_trains"] = "സമീപകാലത്ത് തിരഞ്ഞവ"
ml["preferences"] = "മുൻഗണനകൾ"
ml["weather"] = "കാലാവസ്ഥ"
ml["wind"] = "കാറ്റ്"
ml["humidity"] = "ഈർപ്പം"
ml["visibility"] = "ദൃശ്യപരത"
ml["air_quality"] = "വായു ഗുണനിലവാരം"
ml["fastest"] = "ഏറ്റവും വേഗതയേറിയത്"
ml["open_google_maps"] = "ഗൂഗിൾ മാപ്പ് തുറക്കുക"
ml["cancel"] = "റദ്ദാക്കുക"

# Punjabi (pa)
pa = {k: devanagari_to_lang(v, "pa") for k, v in hi.items()}
pa["tab_live_map"] = "ਲਾਈਵ ਨਕਸ਼ਾ"
pa["tab_routes"] = "ਰੇਲ ਰੂਟ"
pa["tab_analytics"] = "ਐਨਾਲਿਟਿਕਸ"
pa["tab_weather"] = "ਮੌਸਮ"
pa["tab_station"] = "ਸਟੇਸ਼ਨ"
pa["status_on_time"] = "ਸਮੇਂ ਸਿਰ"
pa["status_delayed"] = "ਲੇਟ"
pa["search_trains"] = "ਰੇਲਗੱਡੀਆਂ ਖੋਜੋ"
pa["from"] = "ਕਿੱਥੋਂ"
pa["to"] = "ਕਿੱਥੇ ਤੱਕ"
pa["today"] = "ਅੱਜ"
pa["yesterday"] = "ਕੱਲ੍ਹ (ਬੀਤਿਆ)"
pa["tomorrow"] = "ਕੱਲ੍ਹ (ਆਉਣ ਵਾਲਾ)"
pa["recently_searched_trains"] = "ਹਾਲ ਹੀ ਵਿੱਚ ਖੋਜੀਆਂ ਰੇਲਾਂ"
pa["preferences"] = "ਤਰਜੀਹਾਂ"
pa["weather"] = "ਮੌਸਮ"
pa["wind"] = "ਹਵਾ"
pa["humidity"] = "ਨਮੀ"
pa["visibility"] = "ਦਿੱਖ"
pa["air_quality"] = "ਹਵਾ ਦੀ ਗੁਣਵੱਤਾ"
pa["fastest"] = "ਸਭ ਤੋਂ ਤੇਜ਼"
pa["open_google_maps"] = "ਗੂਗਲ ਮੈਪਸ ਖੋਲ੍ਹੋ"
pa["cancel"] = "ਰੱਦ ਕਰੋ"

langs = {
    "en": en, "hi": hi, "bn": bn, "mr": mr, "te": te,
    "ta": ta, "gu": gu, "kn": kn, "ml": ml, "pa": pa
}

def escape_kotlin(s):
    return s.replace('\\', '\\\\').replace('"', '\\"').replace('$', '\\$')

# Generate Kotlin file
lines = []
lines.append("package com.railgaadi.app.i18n\n")
lines.append("import androidx.compose.runtime.getValue")
lines.append("import androidx.compose.runtime.mutableStateOf")
lines.append("import androidx.compose.runtime.setValue")
lines.append("import com.railgaadi.app.model.SupportedLanguage")
lines.append("import com.railgaadi.app.util.PlatformStorage\n")
lines.append("object I18n {")
lines.append("    private const val KEY_LANG = \"railgaadi_selected_language\"\n")
lines.append("    val languages = listOf(")
lines.append('        SupportedLanguage("en", "English", "English", "EN"),')
lines.append('        SupportedLanguage("hi", "Hindi", "हिन्दी", "हिंद"),')
lines.append('        SupportedLanguage("bn", "Bengali", "বাংলা", "বাং"),')
lines.append('        SupportedLanguage("mr", "Marathi", "मराठी", "मरा"),')
lines.append('        SupportedLanguage("te", "Telugu", "తెలుగు", "తెలు"),')
lines.append('        SupportedLanguage("ta", "Tamil", "தமிழ்", "தமி"),')
lines.append('        SupportedLanguage("gu", "Gujarati", "ગુજરાતી", "ગુજ"),')
lines.append('        SupportedLanguage("kn", "Kannada", "ಕನ್ನಡ", "ಕನ್ನ"),')
lines.append('        SupportedLanguage("ml", "Malayalam", "മലയാളം", "മല"),')
lines.append('        SupportedLanguage("pa", "Punjabi", "ਪੰਜਾਬੀ", "ਪੰਜ")')
lines.append("    )\n")
lines.append("    var currentLanguage by mutableStateOf(loadSavedLanguage())")
lines.append("        private set\n")
lines.append("    fun setLanguage(code: String) {")
lines.append("        currentLanguage = code")
lines.append("        try {")
lines.append("            PlatformStorage.saveString(KEY_LANG, code)")
lines.append("        } catch (_: Throwable) {}")
lines.append("    }\n")
lines.append("    private fun loadSavedLanguage(): String {")
lines.append("        return try {")
lines.append("            PlatformStorage.getString(KEY_LANG)?.takeIf { code ->")
lines.append("                languages.any { it.code == code }")
lines.append('            } ?: "en"')
lines.append("        } catch (_: Throwable) {")
lines.append('            "en"')
lines.append("        }")
lines.append("    }\n")

# Translations map
lines.append("    private val translations: Map<String, Map<String, String>> = mapOf(")
lang_keys = list(langs.keys())
for i, lk in enumerate(lang_keys):
    lines.append(f'        "{lk}" to mapOf(')
    dict_items = list(langs[lk].items())
    for j, (k, v) in enumerate(dict_items):
        comma = "," if j < len(dict_items) - 1 else ""
        lines.append(f'            "{k}" to "{escape_kotlin(v)}"{comma}')
    closing = ")," if i < len(lang_keys) - 1 else ")"
    lines.append(f'        {closing}')
lines.append("    )\n")

# Helpers
lines.append('    fun t(key: String, fallback: String = key): String {')
lines.append('        return translations[currentLanguage]?.get(key)')
lines.append('            ?: translations["en"]?.get(key)')
lines.append('            ?: fallback')
lines.append('    }\n')

lines.append('    val currentPill: String')
lines.append('        get() = languages.find { it.code == currentLanguage }?.pill ?: "EN"\n')

lines.append('    fun stationName(name: String): String {')
lines.append('        return IndicTransliteration.stationToLanguage(name, currentLanguage)')
lines.append('    }\n')

lines.append('    fun trainName(name: String): String {')
lines.append('        return IndicTransliteration.trainToLanguage(name, currentLanguage)')
lines.append('    }\n')

lines.append('    fun landmarkName(name: String): String {')
lines.append('        return IndicTransliteration.landmarkToLanguage(name, currentLanguage)')
lines.append('    }\n')

lines.append('    fun landmarkCategory(category: String): String {')
lines.append('        return when (category.lowercase()) {')
lines.append('            "heritage" -> t("cat_heritage")')
lines.append('            "monument" -> t("cat_monument")')
lines.append('            "river" -> t("cat_river")')
lines.append('            "bridge" -> t("cat_bridge")')
lines.append('            "mountain" -> t("cat_mountain")')
lines.append('            else -> category.replaceFirstChar { it.uppercase() }')
lines.append('        }')
lines.append('    }\n')

lines.append('    fun formatDelay(delayMinutes: Int): String {')
lines.append('        if (delayMinutes == 0) return t("on_time_label", "On Time")')
lines.append('        val absMinutes = kotlin.math.abs(delayMinutes)')
lines.append('        val durationStr = if (absMinutes >= 60) {')
lines.append('            val hrs = absMinutes / 60')
lines.append('            val rem = absMinutes % 60')
lines.append('            if (rem == 0) {')
lines.append('                if (currentLanguage == "en") "$hrs hr" else "$hrs घंटे"')
lines.append('            } else {')
lines.append('                if (currentLanguage == "en") "$hrs hr $rem min" else "$hrs घंटे $rem मिनट"')
lines.append('            }')
lines.append('        } else {')
lines.append('            if (currentLanguage == "en") "$absMinutes mins" else "$absMinutes मिनट"')
lines.append('        }')
lines.append('        return if (delayMinutes > 0) {')
lines.append('            if (currentLanguage == "en") "${t("delayed_by")} $durationStr"')
lines.append('            else "$durationStr ${t("delayed_by")}"')
lines.append('        } else {')
lines.append('            "$durationStr ${t("mins_early")}"')
lines.append('        }')
lines.append('    }\n')

lines.append('    fun formatHalt(minutes: Int): String {')
lines.append('        return "${t("halt")} $minutes${t("halt_mins")}"')
lines.append('    }\n')

lines.append('    fun formatPlatform(platform: String): String {')
lines.append('        val clean = platform.replace(Regex("^(PF|Platform|प्लेटफ़ॉर्म)\\\\s*", RegexOption.IGNORE_CASE), "").trim()')
lines.append('        return "${t("platform")} $clean"')
lines.append('    }\n')

lines.append('    fun formatPassThrough(count: Int): String {')
lines.append('        return "$count ${t("pass_through_stations")}"')
lines.append('    }\n')

lines.append('    fun formatCrossingStatus(name: String, isCrossing: Boolean): String {')
lines.append('        val localizedName = stationName(name)')
lines.append('        return if (isCrossing) {')
lines.append('            if (currentLanguage == "en") "${t("crossing")} $localizedName"')
lines.append('            else "$localizedName ${t("crossing")}"')
lines.append('        } else {')
lines.append('            if (currentLanguage == "en") "${t("at")} $localizedName"')
lines.append('            else "$localizedName ${t("at")}"')
lines.append('        }')
lines.append('    }\n')

lines.append('    fun formatKmRemaining(km: Int, destinationName: String): String {')
lines.append('        val localizedName = stationName(destinationName)')
lines.append('        return if (currentLanguage == "en") {')
lines.append('            "$km ${t("km_to")} $localizedName"')
lines.append('        } else {')
lines.append('            "$localizedName $km ${t("km_to")}"')
lines.append('        }')
lines.append('    }\n')

lines.append('    fun dayLabels(): List<String> {')
lines.append('        return when (currentLanguage) {')
lines.append('            "hi", "mr" -> listOf("र", "सो", "मं", "बु", "गु", "शु", "श")')
lines.append('            "bn" -> listOf("র", "সো", "ম", "বু", "বৃ", "শু", "শ")')
lines.append('            "te" -> listOf("ఆ", "సో", "మం", "బు", "గు", "శు", "శ")')
lines.append('            "ta" -> listOf("ஞா", "தி", "செ", "பு", "வி", "வெ", "ச")')
lines.append('            "gu" -> listOf("ર", "સો", "મં", "બુ", "ગુ", "શુ", "શ")')
lines.append('            "kn" -> listOf("ಭಾ", "ಸೋ", "ಮಂ", "ಬು", "ಗು", "ಶು", "ಶ")')
lines.append('            "ml" -> listOf("ഞാ", "തി", "ചൊ", "ബു", "വ്യാ", "വെ", "ശ")')
lines.append('            "pa" -> listOf("ਐ", "ਸੋ", "ਮੰ", "ਬੁੱ", "ਵੀ", "ਸ਼ੁ", "ਸ਼")')
lines.append('            else -> listOf("S", "M", "T", "W", "T", "F", "S")')
lines.append('        }')
lines.append('    }')
lines.append('}')

with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/i18n/I18n.kt", "w", encoding="utf-8") as f:
    f.write("\n".join(lines))

print(f"Generated I18n.kt successfully with {len(en)} keys across 10 languages!")
