with open("mobile/composeApp/src/commonTest/kotlin/com/railgaadi/app/RailGaadiFeaturesTest.kt", "r") as f:
    c = f.read()

new_tests = """
    // ── 7. 10 LANGUAGES COMPREHENSIVE TRANSLATION & LOCALIZATION TESTS ─────

    @Test
    fun testAll10LanguagesStationAndTrainTranslation() {
        val i18n = com.railgaadi.app.i18n.I18n
        val translit = com.railgaadi.app.i18n.IndicTransliteration

        // Validate station translations across all 10 supported languages
        val testLanguages = listOf("en", "hi", "bn", "mr", "te", "ta", "gu", "kn", "ml", "pa")
        for (lang in testLanguages) {
            i18n.setLanguage(lang)
            val stnName = i18n.stationName("New Delhi")
            assertTrue(stnName.isNotBlank(), "Station name in $lang must not be blank")

            val trainName = i18n.trainName("14702 Amrapur Aravali Express")
            assertTrue(trainName.isNotBlank(), "Train name in $lang must not be blank")
            assertTrue(trainName.startsWith("14702"), "Train number must be preserved in $lang")
        }

        // Specific script checks
        assertEquals("नई दिल्ली", translit.stationToLanguage("New Delhi", "hi"))
        assertEquals("নঈ দিল্লী", translit.stationToLanguage("New Delhi", "bn"))
        assertEquals("નઈ દિલ્લી", translit.stationToLanguage("New Delhi", "gu"))
        assertEquals("ನಈ ದಿಲ್ಲೀ", translit.stationToLanguage("New Delhi", "kn"))
        assertEquals("నఈ దిల్లీ", translit.stationToLanguage("New Delhi", "te"))
        assertEquals("ਨਈ ਦਿਲ੍ਲੀ", translit.stationToLanguage("New Delhi", "pa"))

        // Reset
        i18n.setLanguage("en")
    }

    @Test
    fun testAqiAdvisoryGuideInAllLanguages() {
        val i18n = com.railgaadi.app.i18n.I18n
        val testLanguages = listOf("en", "hi", "bn", "mr", "te", "ta", "gu", "kn", "ml", "pa")

        for (lang in testLanguages) {
            i18n.setLanguage(lang)
            // Header & tabs
            assertTrue(i18n.t("health_advisory_clean_air").isNotBlank(), "Advisory title missing in $lang")
            assertTrue(i18n.t("tab_health_risks").isNotBlank(), "Health risks tab missing in $lang")
            assertTrue(i18n.t("tab_travel_care").isNotBlank(), "Travel care tab missing in $lang")
            assertTrue(i18n.t("tab_clean_city").isNotBlank(), "Clean city tab missing in $lang")

            // Advisory item details
            assertTrue(i18n.t("adv_lungs_title").isNotBlank(), "Lungs title missing in $lang")
            assertTrue(i18n.t("adv_mask_title").isNotBlank(), "Mask title missing in $lang")
            assertTrue(i18n.t("adv_burning_title").isNotBlank(), "Burning title missing in $lang")

            // Outlined UI parts
            assertTrue(i18n.t("search_trains").isNotBlank(), "Search trains missing in $lang")
            assertTrue(i18n.t("alarm_title").isNotBlank(), "Alarm title missing in $lang")
            assertTrue(i18n.t("preferences").isNotBlank(), "Preferences missing in $lang")
        }

        i18n.setLanguage("en")
    }

    @Test
    fun testPanIndiaLandmarksTranslation() {
        val translit = com.railgaadi.app.i18n.IndicTransliteration

        // Red Fort
        assertEquals("Red Fort (Lal Qila)", translit.landmarkToLanguage("Red Fort (Lal Qila)", "en"))
        assertEquals("लाल किला", translit.landmarkToLanguage("Red Fort (Lal Qila)", "hi"))
        assertEquals("লাল কেল্লা", translit.landmarkToLanguage("Red Fort (Lal Qila)", "bn"))
        assertEquals("ఎర్రకోట", translit.landmarkToLanguage("Red Fort (Lal Qila)", "te"))
        assertEquals("செங்கோட்டை", translit.landmarkToLanguage("Red Fort (Lal Qila)", "ta"))
        assertEquals("લાલ કિલ્લો", translit.landmarkToLanguage("Red Fort (Lal Qila)", "gu"))
        assertEquals("ಕೆಂಪು ಕೋಟೆ", translit.landmarkToLanguage("Red Fort (Lal Qila)", "kn"))
        assertEquals("ചെങ്കോട്ട", translit.landmarkToLanguage("Red Fort (Lal Qila)", "ml"))
        assertEquals("ਲਾਲ ਕਿਲ੍ਹਾ", translit.landmarkToLanguage("Red Fort (Lal Qila)", "pa"))

        // Howrah Bridge
        assertEquals("Howrah Bridge", translit.landmarkToLanguage("Howrah Bridge", "en"))
        assertEquals("हावड़ा ब्रिज (कोलकाता)", translit.landmarkToLanguage("Howrah Bridge", "hi"))
        assertEquals("হাওড়া ব্রিজ", translit.landmarkToLanguage("Howrah Bridge", "bn"))

        // Charminar
        assertEquals("చార్మినార్ (హైదరాబాద్)", translit.landmarkToLanguage("Charminar", "te"))
        assertEquals("சார்மினார்", translit.landmarkToLanguage("Charminar", "ta"))

        // Western Ghats
        assertEquals("ಪಶ್ಚಿಮ ಘಟ್ಟಗಳು (ಸಹ್ಯಾದ್ರಿ)", translit.landmarkToLanguage("Western Ghats (Sahyadri Hills)", "kn"))
    }
"""

c = c.replace("    private fun calculateDistanceKm", new_tests + "\n    private fun calculateDistanceKm")

with open("mobile/composeApp/src/commonTest/kotlin/com/railgaadi/app/RailGaadiFeaturesTest.kt", "w") as f:
    f.write(c)

print("Updated RailGaadiFeaturesTest.kt successfully!")
