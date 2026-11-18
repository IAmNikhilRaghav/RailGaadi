with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/ui/screens/WeatherScreen.kt", "r") as f:
    c = f.read()

if "import com.railgaadi.app.i18n.I18n" not in c:
    c = c.replace("import com.railgaadi.app.theme.*", "import com.railgaadi.app.theme.*\nimport com.railgaadi.app.i18n.I18n")

# Top corridor station selector
c = c.replace('Text("Refresh", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RailBluePrimary)',
              'Text(I18n.t("refresh"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RailBluePrimary)')
c = c.replace('text = "Corridor Stations (Tap to view local Weather & AQI)",',
              'text = I18n.t("corridor_stations_hint"),')
c = c.replace('text = "● Live: $liveStnName",',
              'text = "● " + I18n.t("live_badge") + ": " + I18n.stationName(liveStnName),')
c = c.replace('text = stn.name,\n                                        fontSize = 11.sp,\n                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,',
              'text = I18n.stationName(stn.name),\n                                        fontSize = 11.sp,\n                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,')

# Weather Card
c = c.replace('Text("🌡️ Weather", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)',
              'Text("🌡️ " + I18n.t("weather"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary)')
c = c.replace('Text(w.stationName, fontSize = 11.sp, color = textSecondary)',
              'Text(I18n.stationName(w.stationName), fontSize = 11.sp, color = textSecondary)')
c = c.replace('Text("Feels like ${w.apparentTemperature.roundToInt()}°C", fontSize = 11.sp, color = textSecondary)',
              'Text("${I18n.t(\\"feels_like\\")} ${w.apparentTemperature.roundToInt()}°C", fontSize = 11.sp, color = textSecondary)')

# Metrics 2x2
c = c.replace('title = "Wind",', 'title = I18n.t("wind"),')
c = c.replace('title = "Humidity",', 'title = I18n.t("humidity"),')
c = c.replace('title = "Visibility",', 'title = I18n.t("visibility"),')
c = c.replace('title = "Precipitation",', 'title = I18n.t("precipitation"),')
c = c.replace('extra = "Dew Point: ${w.dewPointC.roundToInt()}°C",',
              'extra = "${I18n.t(\\"dew_point\\")}: ${w.dewPointC.roundToInt()}°C",')
c = c.replace('extra = if (w.isFogAdvisory) "⚠️ Low Visibility" else "Clear View",',
              'extra = if (w.isFogAdvisory) "⚠️ " + I18n.t("low_visibility") else I18n.t("clear_view"),')
c = c.replace('extra = "${w.rainProbability}% chance",',
              'extra = "${w.rainProbability}% " + I18n.t("chance"),')

# Air Quality card
c = c.replace('"AIR QUALITY",', 'I18n.t("air_quality"),')
c = c.replace('text = "CPCB NAQI",', 'text = I18n.t("cpcb_naqi"),')
c = c.replace('"National Air Quality Index (India)",', 'I18n.t("naqi_india"),')
c = c.replace('text = "POLLUTANT CONCENTRATIONS (CPCB)",', 'text = I18n.t("pollutant_concentrations"),')
c = c.replace('Text("Primary", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RailBluePrimary, maxLines = 1)',
              'Text(I18n.t("primary_pollutant"), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = RailBluePrimary, maxLines = 1)')
c = c.replace('"Standard: Central Pollution Control Board (CPCB) NAQI · Copernicus CAMS via ECMWF"',
              'I18n.t("standard_cpcb")')
c = c.replace('text = "Observed ${w.lastUpdatedTime}",',
              'text = "${I18n.t(\\"observed\\")} ${w.lastUpdatedTime}",')

# AQI Collapsible Header
c = c.replace('text = "Health Advisory & Clean Air Action",', 'text = I18n.t("health_advisory_clean_air"),')
c = c.replace('text = if (isExpanded) "Tap to collapse" else "Consequences, travel protection & citizen tips",',
              'text = if (isExpanded) I18n.t("tap_to_collapse") else I18n.t("tap_to_expand"),')
c = c.replace('text = if (isExpanded) "Hide ▲" else "Guide ▼",',
              'text = if (isExpanded) I18n.t("hide") else I18n.t("guide"),')

# Tabs in collapsible
old_tabs = """                    listOf(
                        "🩺 Health Risks",
                        "🛡️ Travel Care",
                        "🌱 Clean City"
                    ).forEachIndexed"""
new_tabs = """                    listOf(
                        "🩺 " + I18n.t("tab_health_risks"),
                        "🛡️ " + I18n.t("tab_travel_care"),
                        "🌱 " + I18n.t("tab_clean_city")
                    ).forEachIndexed"""
c = c.replace(old_tabs, new_tabs)

# HealthConsequencesTab
old_impact = """        val impactSummary = when {
            aqi > 400 -> "Hazardous: Severe respiratory and cardiovascular strain across healthy population; emergency for lung/heart patients."
            aqi > 300 -> "Very Poor: Triggers respiratory illness on prolonged exposure. Significant risk for asthma, bronchitis, or heart conditions."
            aqi > 200 -> "Poor: Causes breathing discomfort, throat burning, and eye stinging on prolonged outdoor exposure."
            aqi > 100 -> "Moderate: Breathing discomfort for sensitive individuals, children, and elderly travelers on extended platform stays."
            else -> "Good / Satisfactory: Air quality is safe for normal outdoor breathing."
        }"""
new_impact = """        val impactSummary = when {
            aqi > 400 -> I18n.t("impact_hazardous")
            aqi > 300 -> I18n.t("impact_very_poor")
            aqi > 200 -> I18n.t("impact_poor")
            aqi > 100 -> I18n.t("impact_moderate")
            else -> I18n.t("impact_good")
        }"""
c = c.replace(old_impact, new_impact)

c = c.replace('"Current Impact: ${category.label} (AQI $aqi)"',
              '"${I18n.t(\\"current_impact\\")}: ${category.label} (AQI $aqi)"')

# Health Risk Items
c = c.replace('AdvisoryItem("🫁 Lungs & Airway", "Fine PM2.5 penetrates deep into alveoli and bloodstream, reducing oxygen uptake and causing persistent coughing or wheezing.", textPrimary, textSecondary)',
              'AdvisoryItem("🫁 " + I18n.t("adv_lungs_title"), I18n.t("adv_lungs_desc"), textPrimary, textSecondary)')
c = c.replace('AdvisoryItem("👁️ Eyes & Throat", "Nitrogen dioxide (NO₂) and ozone cause stinging eyes, dry throat, and sinus congestion along rail corridors.", textPrimary, textSecondary)',
              'AdvisoryItem("👁️ " + I18n.t("adv_eyes_title"), I18n.t("adv_eyes_desc"), textPrimary, textSecondary)')
c = c.replace('AdvisoryItem("👶 High-Risk Groups", "Children, elderly travelers, pregnant women, and asthmatics suffer 3x higher vulnerability during railway transit in smog.", textPrimary, textSecondary)',
              'AdvisoryItem("👶 " + I18n.t("adv_risk_title"), I18n.t("adv_risk_desc"), textPrimary, textSecondary)')

# Travel Protection Items
c = c.replace('AdvisoryItem("😷 Wear N95 Mask", "Cloth masks don\'t filter PM2.5. Wear a certified N95/FFP2 mask when waiting on open platforms or at carriage doors.", textPrimary, textSecondary)',
              'AdvisoryItem("😷 " + I18n.t("adv_mask_title"), I18n.t("adv_mask_desc"), textPrimary, textSecondary)')
c = c.replace('AdvisoryItem("🪟 Pull Down Windows", "In Sleeper and General coaches, keep glass shutters down while crossing major industrial corridors and congested rail yards.", textPrimary, textSecondary)',
              'AdvisoryItem("🪟 " + I18n.t("adv_windows_title"), I18n.t("adv_windows_desc"), textPrimary, textSecondary)')
c = c.replace('AdvisoryItem("💧 Regular Warm Hydration", "Sipping warm water or tea helps clear toxic particles trapped in throat mucous membranes and prevents dehydration.", textPrimary, textSecondary)',
              'AdvisoryItem("💧 " + I18n.t("adv_hydration_title"), I18n.t("adv_hydration_desc"), textPrimary, textSecondary)')
c = c.replace('AdvisoryItem("👁️ Saline Eye / Nasal Relief", "Carry sterile lubricating eye drops and saline nasal spray to quickly rinse smog particulate residue at station halts.", textPrimary, textSecondary)',
              'AdvisoryItem("👁️ " + I18n.t("adv_saline_title"), I18n.t("adv_saline_desc"), textPrimary, textSecondary)')
c = c.replace('AdvisoryItem("🚶 Minimize Platform Strolls", "Avoid brisk walking or heavy exercise during long halts; stay inside shaded, ventilated waiting halls or AC coaches.", textPrimary, textSecondary)',
              'AdvisoryItem("🚶 " + I18n.t("adv_strolls_title"), I18n.t("adv_strolls_desc"), textPrimary, textSecondary)')

# Citizen Initiatives Items
c = c.replace('AdvisoryItem("🚫 Zero Open Burning", "Never burn dried leaves, plastic, or domestic waste. Report illegal garbage or farm fires on the official CPCB Sameer App.", textPrimary, textSecondary)',
              'AdvisoryItem("🚫 " + I18n.t("adv_burning_title"), I18n.t("adv_burning_desc"), textPrimary, textSecondary)')
c = c.replace('AdvisoryItem("🚆 Champion Public Transit", "Traveling by Indian Railways and Metro rather than solo cars prevents kilograms of carbon and particulate emissions per trip.", textPrimary, textSecondary)',
              'AdvisoryItem("🚆 " + I18n.t("adv_transit_title"), I18n.t("adv_transit_desc"), textPrimary, textSecondary)')
c = c.replace('AdvisoryItem("🌿 Plant Native Air Purifiers", "Grow native trees (Peepal, Neem, Banyan) in your society and keep indoor plants (Snake Plant, Areca Palm, Tulsi) at home.", textPrimary, textSecondary)',
              'AdvisoryItem("🌿 " + I18n.t("adv_plants_title"), I18n.t("adv_plants_desc"), textPrimary, textSecondary)')
c = c.replace('AdvisoryItem("🚗 Vehicle PUC & Carpooling", "Keep your vehicle\'s PUC certification valid, maintain correct tyre pressure, and turn off your engine at railway crossings.", textPrimary, textSecondary)',
              'AdvisoryItem("🚗 " + I18n.t("adv_puc_title"), I18n.t("adv_puc_desc"), textPrimary, textSecondary)')
c = c.replace('AdvisoryItem("💧 Dust Suppression", "Sprinkle water on loose soil or unpaved paths around your home/shop to prevent ambient dust from kicking up into the air.", textPrimary, textSecondary)',
              'AdvisoryItem("💧 " + I18n.t("adv_dust_title"), I18n.t("adv_dust_desc"), textPrimary, textSecondary)')

# Landmarks Section
c = c.replace('"🏛️ Nearby Landmarks & Monuments"', 'I18n.t("nearby_landmarks_title")')
c = c.replace('"No Landmarks Nearby"', 'I18n.t("no_landmarks_nearby")')
c = c.replace('"No major landmarks found within 60 km of the current station."',
              'I18n.t("no_landmarks_nearby_desc")')

# Inside LandmarkCard
c = c.replace('Text(\n                        landmark.name,',
              'Text(\n                        I18n.landmarkName(landmark.name),')
c = c.replace('Text(\n                            landmark.category.replaceFirstChar { it.uppercase() },',
              'Text(\n                            I18n.landmarkCategory(landmark.category),')
c = c.replace('Text("Open Maps", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)',
              'Text(I18n.t("open_maps"), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)')

with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/ui/screens/WeatherScreen.kt", "w") as f:
    f.write(c)

print("Updated WeatherScreen.kt successfully!")
