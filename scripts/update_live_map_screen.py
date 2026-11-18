with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/ui/screens/LiveMapScreen.kt", "r") as f:
    c = f.read()

# 1. Onboarding sheet title
c = c.replace('"DIRECTIONS TO YOUR ONBOARDING STATION"', 'I18n.t("directions_onboarding_title")')
c = c.replace('selectedStation.name,\n                                    fontSize = 18.sp,',
              'I18n.stationName(selectedStation.name),\n                                    fontSize = 18.sp,')
c = c.replace('"Boarding Station (Where you start your journey):"', 'I18n.t("boarding_station_prompt")')
c = c.replace('text = "${stn.name}${if (isBoarding) " (Boarding)" else if (isOrigin) " (Origin)" else ""}",',
              'text = "${I18n.stationName(stn.name)}${if (isBoarding) " " + I18n.t("boarding_suffix") else if (isOrigin) " " + I18n.t("origin_suffix") else ""}",')

c = c.replace('if (selectedAlternativeRoute == 1) "Alternative" else "Fastest"',
              'if (selectedAlternativeRoute == 1) I18n.t("alternative") else I18n.t("fastest")')
c = c.replace('"${if (selectedAlternativeRoute == 1) "Alternative Route" else "Recommended"} • $navDistanceKm"',
              '"${if (selectedAlternativeRoute == 1) I18n.t("alternative") else I18n.t("recommended")} • $navDistanceKm"')
c = c.replace('"▶ Start (${if (selectedAlternativeRoute == 1) "Alternative" else "Fastest"})"',
              '"▶ " + (if (selectedAlternativeRoute == 1) I18n.t("start_alternative") else I18n.t("start_fastest"))')
c = c.replace('"Open Google Maps"', 'I18n.t("open_google_maps")')
c = c.replace('"✕ Cancel"', '"✕ " + I18n.t("cancel")')

# 2. Delay label & Next stop in bottom sheet
c = c.replace('if (isDelayed) "${formatDelayDuration(liveStatus?.delayMinutes ?: 0)} LATE" else I18n.t("status_on_time")',
              'if (isDelayed) I18n.formatDelay(liveStatus?.delayMinutes ?: 0) else I18n.t("status_on_time")')
c = c.replace('MetricBox(title = "Next Stop", value = liveStatus?.nextStation?.take(14) ?: "Next Halt", icon = "📍")',
              'MetricBox(title = I18n.t("next_stop"), value = liveStatus?.nextStation?.let { I18n.stationName(it).take(14) } ?: I18n.t("next_halt"), icon = "📍")')
c = c.replace('MetricBox(title = "Delay", value = if (delayMin == 0) "On time" else formatDelayDuration(delayMin), icon = "⏱️")',
              'MetricBox(title = I18n.t("delay_label"), value = I18n.formatDelay(delayMin), icon = "⏱️")')
c = c.replace('"Directions to ${selectedStation.name}"',
              '"${I18n.t("directions_to")} ${I18n.stationName(selectedStation.name)}"')

with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/ui/screens/LiveMapScreen.kt", "w") as f:
    f.write(c)

print("Updated LiveMapScreen.kt successfully!")
