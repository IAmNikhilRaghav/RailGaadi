package com.railgaadi.app.i18n

/**
 * Indic Transliteration and Localization Engine for Indian Railways.
 * Provides authentic Hindi (Devanagari) transliteration for:
 * 1. Pan-India railway station names (e.g. "New Delhi" -> "नई दिल्ली", "Kanpur Central" -> "कानपुर सेंट्रल")
 * 2. Indian train names & types (e.g. "Prayagraj SF Express" -> "प्रयागराज सुपरफास्ट एक्सप्रेस")
 * 3. Railway suffixes and junctions ("Jn", "Cantt", "Central", "Terminus", etc.)
 *
 * Combines a high-frequency curated station & city dictionary with a fallback
 * phonetic Devanagari transliteration engine to ensure 100% pan-India coverage.
 */
object IndicTransliteration {

    // Common Railway Categories & Suffixes
    private val RAILWAY_TERMS = mapOf(
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
        "SPECIAL" to "स्पेशल" ,
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
    private val STATION_DICTIONARY = mapOf(
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
        "CHARBAGH" to "चारबाग",
        "PRAYAGRAJ" to "प्रयागराज",
        "PRAYAGRAJ JN" to "प्रयागराज जं.",
        "ALLAHABAD" to "इलाहाबाद",
        "VARANASI" to "वाराणसी",
        "BANARAS" to "बनारस",
        "MUGHAL SARAI" to "मुगलसराय",
        "PT DEEN DAYAL UPADHYAYA" to "पं. दीन दयाल उपाध्याय",
        "DDU" to "पं. दीन दयाल उपाध्याय",
        "GORAKHPUR" to "गोरखपुर",
        "BASTI" to "बस्ती",
        "GONDA" to "गोंडा",
        "AYODHYA" to "अयोध्या",
        "AYODHYA DHAM" to "अयोध्या धाम",
        "AYODHYA CANTT" to "अयोध्या कैंट",
        "FAIZABAD" to "फैज़ाबाद",
        "BAREILLY" to "बरेली",
        "MORADABAD" to "मुरादाबाद",
        "MEERUT" to "मेरठ",
        "MEERUT CITY" to "मेरठ सिटी",
        "SAHARANPUR" to "सहारनपुर",
        "AGRA" to "आगरा",
        "AGRA CANTT" to "आगरा कैंट",
        "AGRA FORT" to "आगरा फोर्ट",
        "MATHURA" to "मथुरा",
        "JHANSI" to "झांसी",
        "VIRANGANA LAKSHMIBAI" to "वीरांगना लक्ष्मीबाई",
        "GWALIOR" to "ग्वालियर",
        "MORENA" to "मुरैना",
        "BHOPAL" to "भोपाल",
        "RANI KAMLAPATI" to "रानी कमलापति",
        "HABIBGANJ" to "हबीबगंज",
        "INDORE" to "इंदौर",
        "UJJAIN" to "उज्जैन",
        "JABALPUR" to "जबलपुर",
        "ITOLA" to "इटोला",
        "PATNA" to "पटना",
        "GAYA" to "गया",
        "MUZAFFARPUR" to "मुज़फ़्फ़रपुर",
        "BHAGALPUR" to "भागलपुर",
        "DHANBAD" to "धनबाद",
        "RANCHI" to "रांची",
        "JAMSHEDPUR" to "जमशेदपुर",
        "TATANAGAR" to "टाटानगर",
        "HOWRAH" to "हावड़ा",
        "SEALDAH" to "सियालदह",
        "KOLKATA" to "कोलकाता",
        "MUMBAI" to "मुंबई",
        "MUMBAI CSMT" to "मुंबई सीएसएमटी",
        "MUMBAI CENTRAL" to "मुंबई सेंट्रल",
        "BANDRA" to "बांद्रा",
        "BANDRA TERMINUS" to "बांद्रा टर्मिनस",
        "LOKMANYA TILAK" to "लोकमान्य तिलक",
        "LTT" to "लोकमान्य तिलक टर्मिनस",
        "DADAR" to "दादर",
        "THANE" to "ठाणे",
        "KALYAN" to "कल्याण",
        "PANVEL" to "पनवेल",
        "PUNE" to "पुणे",
        "NAGPUR" to "नागपुर",
        "NASIK" to "नासिक",
        "NASHIK ROAD" to "नासिक रोड",
        "SURAT" to "सूरत",
        "VADODARA" to "वडोदरा",
        "AHMEDABAD" to "अहमदाबाद",
        "RAJKOT" to "राजकोट",
        "JAIPUR" to "जयपुर",
        "JODHPUR" to "जोधपुर",
        "KOTA" to "कोटा",
        "AJMER" to "अजमेर",
        "BIKANER" to "बीकानेर",
        "UDAIPUR" to "उदयपुर",
        "AMBALA" to "अंबाला",
        "AMBALA CANTT" to "अंबाला कैंट",
        "CHANDIGARH" to "चंडीगढ़",
        "LUDHIANA" to "लुधियाना",
        "AMRITSAR" to "अमृतसर",
        "JALANDHAR" to "जालंधर",
        "JAMMU" to "जम्मू",
        "JAMMU TAWI" to "जम्मू तवी",
        "KATRA" to "कटड़ा",
        "SHRI MATA VAISHNO DEVI" to "श्री माता वैष्णो देवी कटड़ा",
        "DEHRADUN" to "देहरादून",
        "HARIDWAR" to "हरिद्वार",
        "RISHIKESH" to "ऋषिकेश",
        "HYDERABAD" to "हैदराबाद",
        "SECUNDERABAD" to "सिकंदराबाद",
        "KACHEGUDA" to "काचीगुड़ा",
        "VIJAYAWADA" to "विजयवाड़ा",
        "VISAKHAPATNAM" to "विशाखापट्टनम",
        "TIRUPATI" to "तिरुपति",
        "CHENNAI" to "चेन्नई",
        "CHENNAI CENTRAL" to "चेन्नई सेंट्रल",
        "CHENNAI EGMORE" to "चेन्नई एगमोर",
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
        "GUWAHATI" to "गुवाहाटी"
    )

    /**
     * Translates any pan-India railway station name into authentic Hindi.
     * Checks curated dictionary first, resolves railway suffixes, and uses
     * phonetic Devanagari transliteration for unseen stations.
     */
    fun stationToHindi(name: String): String {
        if (name.isBlank()) return ""
        val clean = name.trim()

        // 1. Direct dictionary match
        val upper = clean.uppercase()
        STATION_DICTIONARY[upper]?.let { return it }

        // 2. Tokenize and translate each word
        val tokens = clean.split(Regex("[\\s,]+"))
        val translatedTokens = tokens.map { token ->
            val upperToken = token.uppercase()
            // Check station dictionary for individual token
            STATION_DICTIONARY[upperToken]
                ?: RAILWAY_TERMS[upperToken]
                ?: transliterateWordToHindi(token)
        }

        return translatedTokens.joinToString(" ")
    }

    /**
     * Translates a train name into Hindi (e.g. "12404 Prayagraj SF Express" -> "12404 प्रयागराज सुपरफास्ट एक्सप्रेस").
     */
    fun trainToHindi(trainName: String): String {
        if (trainName.isBlank()) return ""
        val tokens = trainName.trim().split(Regex("[\\s,]+"))
        return tokens.joinToString(" ") { token ->
            val upper = token.uppercase()
            // Keep pure numbers unchanged (e.g. "12404")
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
     * Algorithmic phonetic transliteration of an English/Romanized Indian word into Devanagari.
     * Covers consonants, digraphs (kh, gh, ch, chh, jh, th, dh, ph, sh, bh),
     * vowels, matras, and common endings.
     */
    fun transliterateWordToHindi(word: String): String {
        if (word.isBlank()) return ""
        // Keep pure numbers or special symbols unchanged
        if (word.all { it.isDigit() || !it.isLetter() }) return word

        val s = word.lowercase()
        val sb = StringBuilder()
        var i = 0
        val n = s.length

        var hasPreviousConsonant = false

        while (i < n) {
            // Check 3-char sequences first (e.g. "chh")
            if (i + 3 <= n) {
                val tri = s.substring(i, i + 3)
                when (tri) {
                    "chh" -> {
                        sb.append("छ")
                        hasPreviousConsonant = true
                        i += 3
                        continue
                    }
                }
            }

            // Check 2-char sequences
            if (i + 2 <= n) {
                val bi = s.substring(i, i + 2)
                when (bi) {
                    "kh" -> { sb.append("ख"); hasPreviousConsonant = true; i += 2; continue }
                    "gh" -> { sb.append("घ"); hasPreviousConsonant = true; i += 2; continue }
                    "ch" -> { sb.append("च"); hasPreviousConsonant = true; i += 2; continue }
                    "jh" -> { sb.append("झ"); hasPreviousConsonant = true; i += 2; continue }
                    "th" -> { sb.append("थ"); hasPreviousConsonant = true; i += 2; continue }
                    "dh" -> { sb.append("ध"); hasPreviousConsonant = true; i += 2; continue }
                    "ph" -> { sb.append("फ"); hasPreviousConsonant = true; i += 2; continue }
                    "bh" -> { sb.append("भ"); hasPreviousConsonant = true; i += 2; continue }
                    "sh" -> { sb.append("श"); hasPreviousConsonant = true; i += 2; continue }
                    "zh" -> { sb.append("झ"); hasPreviousConsonant = true; i += 2; continue }
                    "aa" -> {
                        sb.append(if (hasPreviousConsonant) "ा" else "आ")
                        hasPreviousConsonant = false
                        i += 2; continue
                    }
                    "ee" -> {
                        sb.append(if (hasPreviousConsonant) "ी" else "ई")
                        hasPreviousConsonant = false
                        i += 2; continue
                    }
                    "oo" -> {
                        sb.append(if (hasPreviousConsonant) "ू" else "ऊ")
                        hasPreviousConsonant = false
                        i += 2; continue
                    }
                    "ai" -> {
                        sb.append(if (hasPreviousConsonant) "ै" else "ऐ")
                        hasPreviousConsonant = false
                        i += 2; continue
                    }
                    "au" -> {
                        sb.append(if (hasPreviousConsonant) "ौ" else "औ")
                        hasPreviousConsonant = false
                        i += 2; continue
                    }
                }
            }

            // Single characters
            val c = s[i]
            when (c) {
                'a' -> {
                    // In Indian romanization, interior 'a' often represents inherent vowel
                    if (hasPreviousConsonant) {
                        // If it is at the end of word, usually pronounced with 'aa' matra (e.g. Somna -> सोमना, Etawa -> इटावा)
                        if (i == n - 1) {
                            sb.append("ा")
                        }
                        // otherwise inherent vowel
                    } else {
                        sb.append("अ")
                    }
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
