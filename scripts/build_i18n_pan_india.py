# -*- coding: utf-8 -*-
import os

indic_translit_code = '''package com.railgaadi.app.i18n

/**
 * Indic Transliteration and Localization Engine for Indian Railways.
 * Provides authentic multi-script localization across all 10 supported Indian languages:
 * English (en), Hindi (hi), Marathi (mr), Bengali (bn), Telugu (te),
 * Tamil (ta), Gujarati (gu), Kannada (kn), Malayalam (ml), and Punjabi (pa).
 */
object IndicTransliteration {

    // Common Railway Categories & Suffixes in Devanagari
    val RAILWAY_TERMS = mapOf(
        "JN" to "जं.",
        "JNJUNCTION" to "जंक्शन",
        "JUNCTION" to "जंक्शन",
        "CENTRAL" to "सेंट्रल",
        "CANTT" to "कैंट",
        "CANT" to "कैंट",
        "CITY" to "सिटी",
        "ROAD" to "रोड",
        "HALT" to "हाल्ट",
        "HLT" to "हाल्ट",
        "TERMINUS" to "टर्मिनस",
        "TERMINAL" to "टर्मिनस",
        "TOWN" to "टाउन",
        "NORTH" to "उत्तर",
        "SOUTH" to "दक्षिण",
        "EAST" to "पूर्व",
        "WEST" to "पश्चिम",
        "BRIDGE" to "पुल",
        "FORT" to "किला",
        "CAMP" to "कैंप",
        "COLONY" to "कॉलोनी",
        "DHAM" to "धाम",
        "EXP" to "एक्सप्रेस",
        "EXPRESS" to "एक्सप्रेस",
        "SUPERFAST" to "सुपरफास्ट",
        "SF" to "सुपरफास्ट",
        "MAIL" to "मेल",
        "SPECIAL" to "स्पेशल",
        "PASSENGER" to "पैसेंजर",
        "INTERCITY" to "इंटरसिटी",
        "LOCAL" to "लोकल",
        "FAST" to "फास्ट",
        "RAJDHANI" to "राजधानी",
        "SHATABDI" to "शताब्दी",
        "VANDE" to "वंदे",
        "BHARAT" to "भारत",
        "VANDE BHARAT" to "वंदे भारत",
        "DURONTO" to "दुरंतो",
        "HUMSAFAR" to "हमसफ़र",
        "GARIB" to "गरीब",
        "RATH" to "रथ",
        "GARIB RATH" to "गरीब रथ",
        "TEJAS" to "तेजस",
        "JAN" to "जन",
        "JAN SHATABDI" to "जन शताब्दी",
        "SAMPARK" to "संपर्क",
        "KRANTI" to "क्रांति",
        "SAMPARK KRANTI" to "संपर्क क्रांति",
        "ANTYODAYA" to "अंत्योदय",
        "MEMU" to "मेमू",
        "DEMU" to "डेमू",
        "EMU" to "ईएमयू"
    )

    // Curated high-accuracy dictionary for major Indian Railway stations and cities
    val STATION_DICTIONARY = mapOf(
        "NEW DELHI" to "नई दिल्ली",
        "DELHI" to "दिल्ली",
        "OLD DELHI" to "पुरानी दिल्ली",
        "ANAND VIHAR" to "आनंद विहार",
        "HAZRAT NIZAMUDDIN" to "हज़रत निज़ामुद्दीन",
        "NIZAMUDDIN" to "निज़ामुद्दीन",
        "DELHI SARAI ROHILLA" to "दिल्ली सराय रोहिल्ला",
        "SARAI ROHILLA" to "सराय रोहिल्ला",
        "DELHI CANTT" to "दिल्ली कैंट",
        "GHAZIABAD" to "गाज़ियाबाद",
        "KHURJA" to "खुर्जा",
        "SOMNA" to "सोमना",
        "ALIGARH" to "अलीगढ़",
        "HATHRAS" to "हाथरस",
        "TUNDLA" to "टूंडला",
        "FIROZABAD" to "फ़िरोज़ाबाद",
        "SHIKOHABAD" to "शिकोहाबाद",
        "BHADAN" to "भदान",
        "ETAWAH" to "इटावा",
        "BHARTHANA" to "भरथना",
        "PHAPHUND" to "फफूंद",
        "RURA" to "रूरा",
        "PANKI" to "पनकी",
        "PANKI DHAM" to "पनकी धाम",
        "KANPUR" to "कानपुर",
        "KANPUR CENTRAL" to "कानपुर सेंट्रल",
        "KANPUR ANWRGANJ" to "कानपुर अनवरगंज",
        "UNNAO" to "उन्नाव",
        "LUCKNOW" to "लखनऊ",
        "LUCKNOW CHARBAGH" to "लखनऊ चारबाग",
        "VARANASI" to "वाराणसी",
        "BANARAS" to "बनारस",
        "PRAYAGRAJ" to "प्रयागराज",
        "PRAYAGRAJ JN" to "प्रयागराज जं.",
        "ALLAHABAD" to "इलाहाबाद",
        "PT DEEN DAYAL UPADHYAYA" to "पं. दीन दयाल उपाध्याय जं.",
        "DDU" to "पं. दीन दयाल उपाध्याय जं.",
        "MUGHALSARAI" to "मुगलसराय",
        "GAYA" to "गया",
        "PATNA" to "पटना",
        "DHANBAD" to "धनबाद",
        "ASANSOL" to "आसनसोल",
        "HOWRAH" to "हावड़ा",
        "SEALDAH" to "सियालदह",
        "KOLKATA" to "कोलकाता",
        "SHALIMAR" to "शालीमार",
        "AGRA" to "आगरा",
        "AGRA CANTT" to "आगरा कैंट",
        "AGRA FORT" to "आगरा फोर्ट",
        "MATHURA" to "मथुरा",
        "GWALIOR" to "ग्वालियर",
        "JHANSI" to "झांसी",
        "VIRANGANA LAKSHMIBAI JHANSI" to "वीरांगना लक्ष्मीबाई झांसी",
        "BHOPAL" to "भोपाल",
        "RANI KAMALAPATI" to "रानी कमलापति",
        "HABIBGANJ" to "हबीबगंज",
        "ITARSI" to "इटारसी",
        "NAGPUR" to "नागपुर",
        "JABALPUR" to "जबलपुर",
        "KATNI" to "कटनी",
        "SATNA" to "सतना",
        "REWA" to "रीवा",
        "JAIPUR" to "जयपुर",
        "JAISALMER" to "जैसलमेर",
        "JODHPUR" to "जोधपुर",
        "AJMER" to "अजमेर",
        "UDAIPUR" to "उदयपुर",
        "UDAIPUR CITY" to "उदयपुर सिटी",
        "KOTA" to "कोटा",
        "BIKANER" to "बीकानेर",
        "SHRI GANGANAGAR" to "श्री गंगानगर",
        "NOHAR" to "नोहर",
        "AMRAPUR" to "अमरापुर",
        "ARAVALI" to "अरवलि",
        "MUMBAI" to "मुंबई",
        "MUMBAI CENTRAL" to "मुंबई सेंट्रल",
        "CSMT" to "छत्रपति शिवाजी महाराज टर्मिनस",
        "CHHATRAPATI SHIVAJI MAHARAJ TERMINUS" to "छत्रपति शिवाजी महाराज टर्मिनस",
        "BANDRA" to "बांद्रा",
        "BANDRA TERMINUS" to "बांद्रा टर्मिनस",
        "DADAR" to "दादर",
        "LOKMANYA TILAK TERMINUS" to "लोकमान्य तिलक टर्मिनस",
        "LTT" to "लोकमान्य तिलक टर्मिनस",
        "THANE" to "ठाणे",
        "KALYAN" to "कल्याण",
        "PANVEL" to "पनवेल",
        "PUNE" to "पुणे",
        "AHMEDABAD" to "अहमदाबाद",
        "VADODARA" to "वडोदरा",
        "SURAT" to "सूरत",
        "RAJKOT" to "राजकोट",
        "HYDERABAD" to "हैदराबाद",
        "SECUNDERABAD" to "सिकंदराबाद",
        "KACHEGUDA" to "काचीगुडा",
        "VIJAYAWADA" to "विजयवाड़ा",
        "VISAKHAPATNAM" to "विशाखापत्तनम",
        "TIRUPATI" to "तिरुपति",
        "CHENNAI" to "चेन्नई",
        "CHENNAI CENTRAL" to "चेन्नई सेंट्रल",
        "CHENNAI EGMORE" to "चेन्नई एग्मोर",
        "COIMBATORE" to "कोयंबटूर",
        "MADURAI" to "मदुरै",
        "BENGALURU" to "बेंगलुरु",
        "BANGALORE" to "बैंगलोर",
        "KSR BENGALURU" to "केएसआर बेंगलुरु",
        "YESVANTPUR" to "यशवंतपुर",
        "MYSURU" to "मैसूरु",
        "MYSORE" to "मैसूर",
        "KOCHI" to "कोच्चि",
        "ERNAKULAM" to "एर्नाकुलम",
        "THIRUVANANTHAPURAM" to "तिरुवनंतपुरम",
        "TRIVANDRUM" to "त्रिवेंद्रम",
        "BHUBANESWAR" to "भुवनेश्वर",
        "PURI" to "पुरी",
        "CUTTACK" to "कटक",
        "RAIPUR" to "रायपुर",
        "BILASPUR" to "बिलासपुर",
        "GUWAHATI" to "गुवाहाटी",
        "AMRITSAR" to "अमृतसर",
        "CHANDIGARH" to "चंडीगढ़",
        "LUDHIANA" to "लुधियाना",
        "JALANDHAR" to "जालंधर",
        "JAMMU TAWI" to "जम्मू तवी",
        "HARIDWAR" to "हरिद्वार",
        "DEHRADUN" to "देहरादून",
        "GORAKHPUR" to "गोरखपुर",
        "AYODHYA" to "अयोध्या",
        "AYODHYA DHAM" to "अयोध्या धाम",
        "KANAKPURA" to "कनकपुरा",
        "ANDHERI" to "अंधेरी",
        "BORIVALI" to "बोरीवली",
        "KHAR" to "खार",
        "SANTA CRUZ" to "सांताक्रूज़",
        "KAHILIYA" to "कहीलिया",
        "KHARGHAR" to "खारघर",
        "KHANYAN" to "खन्यां",
        "JAIS CITY" to "जायस सिटी",
        "JAI JAI WANTI" to "जय जय वंती"
    )

    private val PLACE_SUFFIXES = listOf(
        "samudram" to "समुद्रम",
        "bazaar" to "बाज़ार",
        "kheda" to "खेड़ा",
        "khera" to "खेड़ा",
        "halli" to "हल्ली",
        "patti" to "पट्टी",
        "puram" to "पुरम",
        "nagar" to "नगर",
        "mandi" to "मंडी",
        "bazar" to "बाज़ार",
        "dham" to "धाम",
        "kund" to "कुंड",
        "gaon" to "गांव",
        "ganj" to "गंज",
        "ghat" to "घाट",
        "wadi" to "वाड़ी",
        "garh" to "गढ़",
        "abad" to "बाद",
        "road" to "रोड",
        "halt" to "हाल्ट",
        "cantt" to "कैंट",
        "cant" to "कैंट",
        "city" to "सिटी",
        "pur" to "पुर",
        "pet" to "पेट",
        "sar" to "सर"
    )

    // Curated Pan-India Rail Corridor Landmarks across all 10 languages
    private val LANDMARK_NAMES = mapOf(
        "Red Fort (Lal Qila)" to mapOf(
            "en" to "Red Fort (Lal Qila)", "hi" to "लाल किला", "bn" to "লাল কেল্লা", "mr" to "लाल किल्ला",
            "te" to "ఎర్రకోట", "ta" to "செங்கோட்டை", "gu" to "લાલ કિલ્લો", "kn" to "ಕೆಂಪು ಕೋಟೆ", "ml" to "ചെങ്കോട്ട", "pa" to "ਲਾਲ ਕਿਲ੍ਹਾ"
        ),
        "Qutub Minar Complex" to mapOf(
            "en" to "Qutub Minar Complex", "hi" to "क़ुतुब मीनार परिसर", "bn" to "কুতুব মিনার চত্বর", "mr" to "कुतुब मिनार संकुल",
            "te" to "కుతుబ్ మీనార్ సముదాయం", "ta" to "குதுப் மினார் வளாகம்", "gu" to "કુતુબ મિનાર પરિસર", "kn" to "ಕುತುಬ್ ಮಿನಾರ್ ಸಂಕೀರ್ಣ", "ml" to "ഖുതുബ് മിനാർ കോംപ്ലക്സ്", "pa" to "ਕੁਤਬ ਮੀਨਾਰ ਕੰਪਲੈਕਸ"
        ),
        "India Gate" to mapOf(
            "en" to "India Gate", "hi" to "इंडिया गेट", "bn" to "ইন্ডিয়া গেট", "mr" to "इंडिया गेट",
            "te" to "ఇండియా గేట్", "ta" to "இந்தியா கேட்", "gu" to "ઇન્ડિયા ગેટ", "kn" to "ಇಂಡಿಯಾ ಗೇಟ್", "ml" to "ഇന്ത്യാ ഗേറ്റ്", "pa" to "ਇੰਡੀਆ ਗੇਟ"
        ),
        "Humayun's Tomb" to mapOf(
            "en" to "Humayun's Tomb", "hi" to "हुमायूँ का मक़बरा", "bn" to "হুমায়ূনের সমাধি", "mr" to "हुमायूँची कबर",
            "te" to "హుమాయూన్ సమాధి", "ta" to "ஹுமாயூன் கல்லறை", "gu" to "હુમાયુનો મકબરો", "kn" to "ಹುಮಾಯೂನ್ ಸಮಾಧಿ", "ml" to "ഹുമയൂണിന്റെ ശവകുടീരം", "pa" to "ਹੁਮਾਯੂੰ ਦਾ ਮਕਬਰਾ"
        ),
        "Lotus Temple" to mapOf(
            "en" to "Lotus Temple", "hi" to "कमल मंदिर (लोटस टेम्पल)", "bn" to "লোটাস টেম্পল", "mr" to "कमळ मंदिर",
            "te" to "లోటస్ టెంపుల్", "ta" to "தாமரைக் கோவில்", "gu" to "કમળ મંદિર", "kn" to "ಕಮಲ ದೇವಾಲಯ", "ml" to "ലോട്ടസ് ടെമ്പിൾ", "pa" to "ਕਮਲ ਮੰਦਰ"
        ),
        "Yamuna River Crossing" to mapOf(
            "en" to "Yamuna River Crossing", "hi" to "यमुना नदी रेल पुल", "bn" to "যমুনা নদী রেল সেতু", "mr" to "यमुना नदी रेल्वे पूल",
            "te" to "యమునా నది రైలు వంతెన", "ta" to "யமுனை நதி ரயில் பாலம்", "gu" to "યમુના નદી રેલ પુલ", "kn" to "ಯಮುನಾ ನದಿ ರೈಲು ಸೇತುವೆ", "ml" to "യമുനാ നദി റെയിൽ പാലം", "pa" to "ਯਮੁਨਾ ਨਦੀ ਰੇਲ ਪੁਲ"
        ),
        "Taj Mahal Viewpoint" to mapOf(
            "en" to "Taj Mahal Viewpoint", "hi" to "ताजमहल व्यूपॉइंट", "bn" to "তাজমহল ভিউপয়েন্ট", "mr" to "ताजमहाल दृश्य",
            "te" to "తాజ్ మహల్ వ్యూపాయింట్", "ta" to "தாஜ் மஹால் பார்வை மையம்", "gu" to "તાજમહેલ વ્યૂપોઇન્ટ", "kn" to "ತಾಜ್ ಮಹಲ್ ವೀಕ್ಷಣಾ ತಾಣ", "ml" to "താജ് മഹൽ വ്യൂപോയിന്റ്", "pa" to "ਤਾਜ ਮਹਿਲ ਵਿਊਪੁਆਇੰਟ"
        ),
        "Agra Fort" to mapOf(
            "en" to "Agra Fort", "hi" to "आगरा किला", "bn" to "আগ্রা দুর্গ", "mr" to "आग्रा किल्ला",
            "te" to "ఆగ్రా కోట", "ta" to "ஆக்ரா கோட்டை", "gu" to "આગ્રા કિલ્લો", "kn" to "ಆಗ್ರಾ ಕೋಟೆ", "ml" to "ആഗ്ര കോട്ട", "pa" to "ਆਗਰਾ ਕਿਲ੍ਹਾ"
        ),
        "Fatehpur Sikri" to mapOf(
            "en" to "Fatehpur Sikri", "hi" to "फ़तेहपुर सीकरी", "bn" to "ফতেহপুর সিকরি", "mr" to "फतेहपूर सिक्री",
            "te" to "ఫతేపూర్ సిక్రీ", "ta" to "ஃபதேபூர் சிக்ரி", "gu" to "ફતેહપુર સીક્રી", "kn" to "ಫತೇಪುರ್ ಸಿಕ್ರಿ", "ml" to "ഫത്തേപൂർ സിക്രി", "pa" to "ਫਤਿਹਪੁਰ ਸੀਕਰੀ"
        ),
        "Gwalior Fort" to mapOf(
            "en" to "Gwalior Fort", "hi" to "ग्वालियर किला", "bn" to "গোয়ালিয়র দুর্গ", "mr" to "ग्वाल्हेर किल्ला",
            "te" to "గ్వాలియర్ కోట", "ta" to "குவாலியர் கோட்டை", "gu" to "ગ્વાલિયર કિલ્લો", "kn" to "ಗ್ವಾಲಿಯರ್ ಕೋಟೆ", "ml" to "ഗ്വാളിയോർ കോട്ട", "pa" to "ਗਵਾਲੀਅਰ ਕਿਲ੍ਹਾ"
        ),
        "Ganga River Rail Bridge (Kanpur)" to mapOf(
            "en" to "Ganga River Rail Bridge (Kanpur)", "hi" to "गंगा नदी रेल पुल (कानपुर)", "bn" to "গঙ্গা নদী রেল সেতু (কানপুর)", "mr" to "गंगा नदी रेल्वे पूल (कानपूर)",
            "te" to "గంగా నది రైలు వంతెన (కాన్పూర్)", "ta" to "கங்கை நதி ரயில் பாலம் (கான்பூர்)", "gu" to "ગંગા નદી રેલ પુલ (કાનપુર)", "kn" to "ಗಂಗಾ ನದಿ ರೈಲು ಸೇತುವೆ (ಕಾನ್ಪುರ)", "ml" to "ഗംഗാ നദി റെയിൽ പാലം (കാൺപൂർ)", "pa" to "ਗੰਗਾ ਨਦੀ ਰੇਲ ਪੁਲ (ਕਾਨਪੁਰ)"
        ),
        "Triveni Sangam" to mapOf(
            "en" to "Triveni Sangam", "hi" to "त्रिवेणी संगम (प्रयागराज)", "bn" to "ত্রিবেণী সঙ্গম", "mr" to "त्रिवेणी संगम",
            "te" to "త్రివేణి సంగమం", "ta" to "திரிவேணி சங்கமம்", "gu" to "ત્રિવેણી સંગમ", "kn" to "ತ್ರಿವೇಣಿ ಸಂಗಮ", "ml" to "ത്രിവേണി സംഗമം", "pa" to "ਤ੍ਰਿਵੇਣੀ ਸੰਗਮ"
        ),
        "Curzon Bridge (Prayagraj)" to mapOf(
            "en" to "Curzon Bridge (Prayagraj)", "hi" to "कर्ज़न ब्रिज (प्रयागराज)", "bn" to "কার্জন ব্রিজ", "mr" to "कर्झन ब्रिज",
            "te" to "కర్జన్ వంతెన", "ta" to "கர்சன் பாலம்", "gu" to "કર્ઝન બ્રિજ", "kn" to "ಕರ್ಜನ್ ಸೇತುವೆ", "ml" to "കർസൺ പാലം", "pa" to "ਕਰਜ਼ਨ ਬ੍ਰਿਜ"
        ),
        "Kashi Vishwanath Corridor" to mapOf(
            "en" to "Kashi Vishwanath Corridor", "hi" to "काशी विश्वनाथ कॉरिडोर (वाराणसी)", "bn" to "কাশী বিশ্বনাথ করিডোর", "mr" to "काशी विश्वनाथ कॉरिडोअर",
            "te" to "కాశీ విశ్వనాథ్ కారిడార్", "ta" to "காசி விஸ்வநாதர் வழித்தடம்", "gu" to "કાશી વિશ્વનાથ કોરિડોર", "kn" to "ಕಾಶಿ ವಿಶ್ವನಾಥ ಕಾರಿಡಾರ್", "ml" to "കാശി വിശ്വനാഥ് കോറിഡോർ", "pa" to "ਕਾਸ਼ੀ ਵਿਸ਼ਵਨਾਥ ਕੋਰੀਡੋਰ"
        ),
        "Malviya Bridge" to mapOf(
            "en" to "Malviya Bridge", "hi" to "मालवीय रेल पुल (वाराणसी)", "bn" to "মালব্য ব্রিজ", "mr" to "मालवीय पूल",
            "te" to "మాలవ్య వంతెన", "ta" to "மால்வியா பாலம்", "gu" to "માલવિયા બ્રિજ", "kn" to "ಮಾಲವೀಯ ಸೇತುವೆ", "ml" to "മാളവ്യ പാലം", "pa" to "ਮਾਲਵੀਆ ਬ੍ਰਿਜ"
        ),
        "Hawa Mahal" to mapOf(
            "en" to "Hawa Mahal", "hi" to "हवा महल (जयपुर)", "bn" to "হাওয়া মহল", "mr" to "हवा महल",
            "te" to "హవా మహల్", "ta" to "ஹவா மஹால்", "gu" to "હવા મહેલ", "kn" to "ಹವಾ ಮಹಲ್", "ml" to "ഹവാ മഹൽ", "pa" to "ਹਵਾ ਮਹਿਲ"
        ),
        "Amer (Amber) Fort" to mapOf(
            "en" to "Amer (Amber) Fort", "hi" to "आमेर किला (जयपुर)", "bn" to "আম্বর দুর্গ", "mr" to "आमेर किल्ला",
            "te" to "అంబర్ కోట", "ta" to "அம்பர் கோட்டை", "gu" to "આમેર કિલ્લો", "kn" to "ಅಮೇರ್ ಕೋಟೆ", "ml" to "ആംബർ കോട്ട", "pa" to "ਆਮੇਰ ਕਿਲ੍ਹਾ"
        ),
        "Mehrangarh Fort Jodhpur" to mapOf(
            "en" to "Mehrangarh Fort Jodhpur", "hi" to "मेहरानगढ़ किला (जोधपुर)", "bn" to "মেহরানগড় দুর্গ", "mr" to "मेहरानगड किल्ला",
            "te" to "మెహ్రాన్‌గఢ్ కోట", "ta" to "மெஹ்ராங்கர் கோட்டை", "gu" to "મેહરાનગઢ કિલ્લો", "kn" to "ಮೆಹ್ರಾನ್‌ಗಢ್ ಕೋಟೆ", "ml" to "മെഹ്‌റാൻഗഡ് കോട്ട", "pa" to "ਮੇਹਰਾਨਗੜ੍ਹ ਕਿਲ੍ਹਾ"
        ),
        "Umaid Bhawan Palace" to mapOf(
            "en" to "Umaid Bhawan Palace", "hi" to "उम्मेद भवन पैलेस", "bn" to "উমেদ ভবন প্যালেস", "mr" to "उम्मेद भवन पॅलेस",
            "te" to "ఉమైద్ భవన్ ప్యాలెస్", "ta" to "உமைத் பவன் அரண்மனை", "gu" to "ઉમ્મેદ ભવન પેલેસ", "kn" to "ಉಮೈದ್ ಭವನ್ ಅರಮನೆ", "ml" to "ഉമൈദ് ഭവൻ കൊട്ടാരം", "pa" to "ਉਮੈਦ ਭਵਨ ਪੈਲੇਸ"
        ),
        "Thar Desert Dunes" to mapOf(
            "en" to "Thar Desert Dunes", "hi" to "थार मरुस्थल के टीले", "bn" to "থর মরুভূমির বালিয়াড়ি", "mr" to "थार वाळवंटातील टेकड्या",
            "te" to "థార్ ఎడారి ఇసుక దిబ్బలు", "ta" to "தார் பாலைவன மணல் திட்டுகள்", "gu" to "થાર રણના ઢૂવા", "kn" to "ಥಾರ್ ಮರುಭೂಮಿ ಮರಳುದಿಣ್ಣೆಗಳು", "ml" to "ഥാർ മരുഭൂമിയിലെ മണൽക്കുന്നുകൾ", "pa" to "ਥਾਰ ਮਾਰੂਥਲ ਦੇ ਟਿੱਬੇ"
        ),
        "Howrah Bridge" to mapOf(
            "en" to "Howrah Bridge", "hi" to "हावड़ा ब्रिज (कोलकाता)", "bn" to "হাওড়া ব্রিজ", "mr" to "हावडा ब्रिज",
            "te" to "హౌరా వంతెన", "ta" to "ஹவுரா பாலம்", "gu" to "હાવડા બ્રિજ", "kn" to "ಹೌರಾ ಸೇತುವೆ", "ml" to "ഹൗറ പാലം", "pa" to "ਹਾਵੜਾ ਬ੍ਰਿਜ"
        ),
        "Victoria Memorial" to mapOf(
            "en" to "Victoria Memorial", "hi" to "विक्टोरिया मेमोरियल", "bn" to "ভিক্টোরিয়া মেমোরিয়াল", "mr" to "व्हिक्टोरिया मेमोरियल",
            "te" to "విక్టోరియా మెమోరియల్", "ta" to "விக்டோரியா நினைவிடம்", "gu" to "વિક્ટોરિયા મેમોરિયલ", "kn" to "ವಿಕ್ಟೋರಿಯಾ ಸ್ಮಾರಕ", "ml" to "വിക്ടോറിയ മെമ്മോറിയൽ", "pa" to "ਵਿਕਟੋਰੀਆ ਮੈਮੋਰੀਅਲ"
        ),
        "Damodar River Rail Bridge" to mapOf(
            "en" to "Damodar River Rail Bridge", "hi" to "दामोदर नदी रेल पुल", "bn" to "দামোদর নদ রেল সেতু", "mr" to "दामोदर नदी रेल्वे पूल",
            "te" to "దామోదర్ నది రైలు వంతెన", "ta" to "தாமோதர் நதி ரயில் பாலம்", "gu" to "દામોદર નદી રેલ પુલ", "kn" to "ದಾಮೋದರ್ ನದಿ ರೈಲು ಸೇತುವೆ", "ml" to "ദാമോദർ നദി റെയിൽ പാലം", "pa" to "ਦਾਮੋਦਰ ਨਦੀ ਰੇਲ ਪੁਲ"
        ),
        "Parasnath Hill (Shikharji)" to mapOf(
            "en" to "Parasnath Hill (Shikharji)", "hi" to "पारसनाथ पहाड़ी (शिखरजी)", "bn" to "পরেশনাথ পাহাড় (শিখরজী)", "mr" to "पारसनाथ टेकडी (शिखरजी)",
            "te" to "పారస్‌నాథ్ కొండ (శిఖర్జీ)", "ta" to "பரஸ்நாத் மலை (சிகர்ஜி)", "gu" to "પારસનાથ પહાડી (શિખરજી)", "kn" to "ಪಾರಸ್‌ನಾಥ್ ಬೆಟ್ಟ (ಶಿಖರ್ಜಿ)", "ml" to "പരസ്നാഥ് കുന്ന് (ശിഖർജി)", "pa" to "ਪਾਰਸਨਾਥ ਪਹਾੜੀ (ਸ਼ਿਖਰਜੀ)"
        ),
        "Chhatrapati Shivaji Maharaj Terminus" to mapOf(
            "en" to "Chhatrapati Shivaji Maharaj Terminus", "hi" to "छत्रपति शिवाजी महाराज टर्मिनस", "bn" to "ছত্রপতি শিবাজী মহারাজ টার্মিনাস", "mr" to "छत्रपती शिवाजी महाराज टर्मिनस",
            "te" to "ఛత్రపతి శివాజీ మహారాజ్ టెర్మినస్", "ta" to "சத்ரபதி சிவாஜி மகாராஜ் முனையம்", "gu" to "છત્રપતિ શિવાજી મહારાજ ટર્મિનસ", "kn" to "ಛತ್ರಪತಿ ಶಿವಾಜಿ ಮಹಾರಾಜ್ ಟರ್ಮಿನಸ್", "ml" to "ഛത്രപതി ശിവാജി മഹാരാജ് ടെർമിനസ്", "pa" to "ਛਤਰਪਤੀ ਸ਼ਿਵਾਜੀ ਮਹਾਰਾਜ ਟਰਮੀਨਸ"
        ),
        "Western Ghats (Sahyadri Hills)" to mapOf(
            "en" to "Western Ghats (Sahyadri Hills)", "hi" to "पश्चिमी घाट (सह्याद्री पर्वतमाला)", "bn" to "পশ্চিমঘাট পর্বতমালা (সহ্যাদ্রি)", "mr" to "पश्चिम घाट (सह्याद्री)",
            "te" to "పశ్చిమ కనుమలు (సహ్యాద్రి)", "ta" to "மேற்கு தொடர்ச்சி மலை (சஹ்யாத்ரி)", "gu" to "પશ્ચિમી ઘાટ (સહ્યાદ્રી)", "kn" to "ಪಶ್ಚಿಮ ಘಟ್ಟಗಳು (ಸಹ್ಯಾದ್ರಿ)", "ml" to "പശ്ചിമഘട്ടം (സഹ്യാദ്രി)", "pa" to "ਪੱਛਮੀ ਘਾਟ (ਸਹਿਯਾਦਰੀ)"
        ),
        "Narmada River Bridge (Bharuch)" to mapOf(
            "en" to "Narmada River Bridge (Bharuch)", "hi" to "नर्मदा नदी रेल पुल (भरूच)", "bn" to "নর্মদা নদী রেল সেতু (ভরুচ)", "mr" to "नर्मदा नदी रेल्वे पूल (भरूच)",
            "te" to "నర్మదా నది రైలు వంతెన (భరూచ్)", "ta" to "நர்மதை நதி ரயில் பாலம் (பரூச்)", "gu" to "નર્મદા નદી ગોલ્ડન બ્રિજ (ભરૂચ)", "kn" to "ನರ್ಮದಾ ನದಿ ರೈಲು ಸೇತುವೆ (ಭರೂಚ್)", "ml" to "നർമ്മദാ നദി റെയിൽ പാലം (ഭറൂച്ച്)", "pa" to "ਨਰਮਦਾ ਨਦੀ ਰੇਲ ਪੁਲ (ਭਰੂਚ)"
        ),
        "Charminar" to mapOf(
            "en" to "Charminar", "hi" to "चारमीनार (हैदराबाद)", "bn" to "চারমিনার", "mr" to "चारमिनार",
            "te" to "చార్మినార్ (హైదరాబాద్)", "ta" to "சார்மினார்", "gu" to "ચારમીનાર", "kn" to "ಚಾರ್ಮಿನಾರ್ (ಹೈದರಾಬಾದ್)", "ml" to "ചാർമിനാർ", "pa" to "ਚਾਰਮੀਨਾਰ"
        ),
        "Golconda Fort" to mapOf(
            "en" to "Golconda Fort", "hi" to "गोलकोंडा किला", "bn" to "গোলকোন্ডা দুর্গ", "mr" to "गोवळकोंडा किल्ला",
            "te" to "గోల్కొండ కోట", "ta" to "கோல்கொண்டா கோட்டை", "gu" to "ગોલકોન્ડા કિલ્લો", "kn" to "ಗೋಲ್ಕೊಂಡಾ ಕೋಟೆ", "ml" to "ഗോൽക്കൊണ്ട കോട്ട", "pa" to "ਗੋਲਕੁੰਡਾ ਕਿਲ੍ਹਾ"
        ),
        "Mysuru (Mysore) Palace" to mapOf(
            "en" to "Mysuru (Mysore) Palace", "hi" to "मैसूरु पैलेस (अम्बा विलास)", "bn" to "মহীশূর রাজপ্রাসাদ", "mr" to "म्हैसूर पॅलेस",
            "te" to "మైసూరు ప్యాలెస్", "ta" to "மைசூர் அரண்மனை", "gu" to "મૈસૂર પેલેસ", "kn" to "ಮೈಸೂರು ಅರಮನೆ (ಅಂಬಾ ವಿಲಾಸ)", "ml" to "മൈസൂർ കൊട്ടാരം", "pa" to "ਮੈਸੂਰ ਪੈਲੇਸ"
        ),
        "Brihadeeswarar Temple (Thanjavur)" to mapOf(
            "en" to "Brihadeeswarar Temple (Thanjavur)", "hi" to "बृहदीश्वर मंदिर (तंजावुर)", "bn" to "বৃহদীশ্বর মন্দির", "mr" to "बृहदीश्वर मंदिर (तंजावर)",
            "te" to "బృహదీశ్వర ఆలయం (తంజావూరు)", "ta" to "தஞ்சை பெரிய கோவில் (பிரகதீஸ்வரர்)", "gu" to "બૃહદીશ્વર મંદિર (તંજાવુર)", "kn" to "ಬೃಹದೀಶ್ವರ ದೇವಾಲಯ (ತಂಜಾವೂರು)", "ml" to "ബൃഹദീശ്വര ക്ഷേത്രം (തഞ്ചാവൂർ)", "pa" to "ਬ੍ਰਿਹਦੀਸ਼ਵਰ ਮੰਦਰ (ਤੰਜਾਵੁਰ)"
        )
    )

    // Tamil phonetic consonants map
    private val TAMIL_MAP = mapOf(
        'ख' to "க", 'ग' to "க", 'घ' to "க",
        'छ' to "ச", 'ज' to "ஜ", 'झ' to "ச",
        'ठ' to "ட", 'ड' to "ட", 'ढ' to "ட",
        'थ' to "த", 'द' to "த", 'ध' to "த",
        'फ' to "ப", 'ब' to "ப", 'भ' to "ப",
        'श' to "ஷ", 'ष' to "ஷ", 'स' to "ஸ",
        'ज़' to "ஜ", 'फ़' to "ப"
    )

    /**
     * Translates any Devanagari text into other Brahmic Indic scripts (Bengali, Gurmukhi,
     * Gujarati, Telugu, Kannada, Malayalam, Tamil) using standard Unicode script offsets.
     */
    fun convertDevanagariToScript(devanagariText: String, lang: String): String {
        if (lang == "hi" || lang == "mr" || lang == "en") return devanagariText

        val offset = when (lang) {
            "bn" -> 0x0980 - 0x0900
            "pa" -> 0x0A00 - 0x0900
            "gu" -> 0x0A80 - 0x0900
            "te" -> 0x0C00 - 0x0900
            "kn" -> 0x0C80 - 0x0900
            "ml" -> 0x0D00 - 0x0900
            "ta" -> 0x0B80 - 0x0900
            else -> 0
        }

        if (lang == "ta") {
            val sb = StringBuilder()
            for (ch in devanagariText) {
                val replaced = TAMIL_MAP[ch]
                if (replaced != null) {
                    sb.append(replaced)
                } else {
                    val cp = ch.code
                    if (cp in 0x0900..0x097F) {
                        val tcp = cp + offset
                        if (tcp in 0x0B82..0x0BD7) {
                            sb.append(tcp.toChar())
                        } else {
                            sb.append(ch)
                        }
                    } else {
                        sb.append(ch)
                    }
                }
            }
            return sb.toString()
        }

        if (offset != 0) {
            val sb = StringBuilder()
            for (ch in devanagariText) {
                val cp = ch.code
                if (cp in 0x0900..0x097F) {
                    sb.append((cp + offset).toChar())
                } else {
                    sb.append(ch)
                }
            }
            return sb.toString()
        }

        return devanagariText
    }

    /**
     * Translates any pan-India railway station name into authentic Hindi.
     */
    fun stationToHindi(name: String): String {
        if (name.isBlank()) return ""
        val clean = name.trim()

        val upper = clean.uppercase()
        STATION_DICTIONARY[upper]?.let { return it }

        val tokens = clean.split(Regex("[\\s,]+"))
        val translatedTokens = tokens.map { token ->
            val upperToken = token.uppercase()
            STATION_DICTIONARY[upperToken]
                ?: RAILWAY_TERMS[upperToken]
                ?: transliterateWordToHindi(token)
        }

        return translatedTokens.joinToString(" ")
    }

    /**
     * Translates a train name into Hindi.
     */
    fun trainToHindi(trainName: String): String {
        if (trainName.isBlank()) return ""
        val tokens = trainName.trim().split(Regex("[\\s,]+"))
        return tokens.joinToString(" ") { token ->
            val upper = token.uppercase()
            if (token.all { it.isDigit() }) {
                token
            } else {
                RAILWAY_TERMS[upper]
                    ?: STATION_DICTIONARY[upper]
                    ?: transliterateWordToHindi(token)
            }
        }
    }

    /**
     * Translates any pan-India station into any of the 10 supported languages.
     */
    fun stationToLanguage(name: String, lang: String): String {
        if (lang == "en" || name.isBlank()) return name
        val hindi = stationToHindi(name)
        return if (lang == "hi") hindi else convertDevanagariToScript(hindi, lang)
    }

    /**
     * Translates any train name into any of the 10 supported languages.
     */
    fun trainToLanguage(trainName: String, lang: String): String {
        if (lang == "en" || trainName.isBlank()) return trainName
        val hindi = trainToHindi(trainName)
        return if (lang == "hi") hindi else convertDevanagariToScript(hindi, lang)
    }

    /**
     * Translates landmark names into any of the 10 supported languages.
     */
    fun landmarkToLanguage(name: String, lang: String): String {
        if (lang == "en" || name.isBlank()) return name
        val match = LANDMARK_NAMES[name]
        if (match != null) {
            val localized = match[lang] ?: match["hi"]
            if (localized != null) return localized
        }
        val hindi = stationToHindi(name)
        return if (lang == "hi") hindi else convertDevanagariToScript(hindi, lang)
    }

    /**
     * Algorithmic phonetic transliteration of an English/Romanized word into Devanagari.
     */
    fun transliterateWordToHindi(word: String): String {
        if (word.isBlank()) return ""
        if (word.all { it.isDigit() || !it.isLetter() }) return word

        val s = word.lowercase()

        for ((sfx, hindiSfx) in PLACE_SUFFIXES) {
            if (s.endsWith(sfx) && s.length > sfx.length + 2) {
                val root = s.substring(0, s.length - sfx.length)
                val transliteratedRoot = transliteratePhonetic(root)
                return transliteratedRoot + hindiSfx
            }
        }

        return transliteratePhonetic(s)
    }

    private fun transliteratePhonetic(s: String): String {
        val sb = StringBuilder()
        var i = 0
        val n = s.length
        var hasPreviousConsonant = false

        while (i < n) {
            val c = s[i]
            val next = if (i + 1 < n) s[i + 1] else ' '
            val next2 = if (i + 2 < n) s[i + 2] else ' '

            if (c == 'c' && next == 'h' && next2 == 'h') {
                sb.append("छ")
                i += 3
                hasPreviousConsonant = true
                continue
            }

            if (c == 'k' && next == 'h') { sb.append("ख"); i += 2; hasPreviousConsonant = true; continue }
            if (c == 'g' && next == 'h') { sb.append("घ"); i += 2; hasPreviousConsonant = true; continue }
            if (c == 'c' && next == 'h') { sb.append("च"); i += 2; hasPreviousConsonant = true; continue }
            if (c == 'j' && next == 'h') { sb.append("झ"); i += 2; hasPreviousConsonant = true; continue }
            if (c == 't' && next == 'h') { sb.append("थ"); i += 2; hasPreviousConsonant = true; continue }
            if (c == 'd' && next == 'h') { sb.append("ध"); i += 2; hasPreviousConsonant = true; continue }
            if (c == 'p' && next == 'h') { sb.append("फ"); i += 2; hasPreviousConsonant = true; continue }
            if (c == 'b' && next == 'h') { sb.append("भ"); i += 2; hasPreviousConsonant = true; continue }
            if (c == 's' && next == 'h') { sb.append("श"); i += 2; hasPreviousConsonant = true; continue }

            if (c == 'a' && next == 'a') {
                sb.append(if (hasPreviousConsonant) "ा" else "आ")
                i += 2
                hasPreviousConsonant = false
                continue
            }
            if (c == 'e' && next == 'e') {
                sb.append(if (hasPreviousConsonant) "ी" else "ई")
                i += 2
                hasPreviousConsonant = false
                continue
            }
            if (c == 'o' && next == 'o') {
                sb.append(if (hasPreviousConsonant) "ू" else "ऊ")
                i += 2
                hasPreviousConsonant = false
                continue
            }
            if (c == 'a' && next == 'i') {
                sb.append(if (hasPreviousConsonant) "ै" else "ऐ")
                i += 2
                hasPreviousConsonant = false
                continue
            }
            if (c == 'a' && next == 'u') {
                sb.append(if (hasPreviousConsonant) "ौ" else "औ")
                i += 2
                hasPreviousConsonant = false
                continue
            }

            when (c) {
                'a' -> {
                    if (!hasPreviousConsonant) sb.append("अ")
                    hasPreviousConsonant = false
                }
                'i' -> {
                    sb.append(if (hasPreviousConsonant) "ि" else "इ")
                    hasPreviousConsonant = false
                }
                'u' -> {
                    sb.append(if (hasPreviousConsonant) "ु" else "उ")
                    hasPreviousConsonant = false
                }
                'e' -> {
                    sb.append(if (hasPreviousConsonant) "े" else "ए")
                    hasPreviousConsonant = false
                }
                'o' -> {
                    sb.append(if (hasPreviousConsonant) "ो" else "ओ")
                    hasPreviousConsonant = false
                }
                'k' -> { sb.append("क"); hasPreviousConsonant = true }
                'g' -> { sb.append("ग"); hasPreviousConsonant = true }
                'c' -> { sb.append("क"); hasPreviousConsonant = true }
                'j' -> { sb.append("ज"); hasPreviousConsonant = true }
                't' -> { sb.append("ट"); hasPreviousConsonant = true }
                'd' -> { sb.append("ड"); hasPreviousConsonant = true }
                'n' -> { sb.append("न"); hasPreviousConsonant = true }
                'p' -> { sb.append("प"); hasPreviousConsonant = true }
                'b' -> { sb.append("ब"); hasPreviousConsonant = true }
                'm' -> { sb.append("म"); hasPreviousConsonant = true }
                'y' -> { sb.append("य"); hasPreviousConsonant = true }
                'r' -> { sb.append("र"); hasPreviousConsonant = true }
                'l' -> { sb.append("ल"); hasPreviousConsonant = true }
                'v', 'w' -> { sb.append("व"); hasPreviousConsonant = true }
                's' -> { sb.append("स"); hasPreviousConsonant = true }
                'h' -> { sb.append("ह"); hasPreviousConsonant = true }
                'z' -> { sb.append("ज़"); hasPreviousConsonant = true }
                'f' -> { sb.append("फ़"); hasPreviousConsonant = true }
                'q' -> { sb.append("क़"); hasPreviousConsonant = true }
                'x' -> { sb.append("क्स"); hasPreviousConsonant = true }
                else -> {
                    sb.append(c)
                    hasPreviousConsonant = false
                }
            }
            i++
        }

        return sb.toString()
    }
}
'''

with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/i18n/IndicTransliteration.kt", "w", encoding="utf-8") as f:
    f.write(indic_translit_code)

print("Wrote IndicTransliteration.kt successfully!")
