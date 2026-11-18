with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/ui/components/PreferencesModal.kt", "r") as f:
    c = f.read()

c = c.replace('text = "Preferences",', 'text = I18n.t("preferences"),')
c = c.replace('title = "Dark Theme",\n                        subtitle = "Comfortable viewing at night",',
              'title = I18n.t("pref_dark_theme"),\n                        subtitle = I18n.t("pref_dark_theme_sub"),')
c = c.replace('title = "Offline GPS Mode",\n                        subtitle = if (TrainTrackerState.isOfflineModeEnabled) "Active • Tracking via Satellites" else "Track inside trains with 0% internet",',
              'title = I18n.t("pref_offline_gps"),\n                        subtitle = if (TrainTrackerState.isOfflineModeEnabled) I18n.t("pref_offline_gps_active") else I18n.t("pref_offline_gps_sub"),')
c = c.replace('title = "Trip Delay Alerts",\n                        subtitle = "Live notifications on delay",',
              'title = I18n.t("pref_trip_delay"),\n                        subtitle = I18n.t("pref_trip_delay_sub"),')
c = c.replace('title = "Select Language / भाषा",', 'title = I18n.t("pref_select_language"),')
c = c.replace('title = "Saved Trains & Routes",\n                        subtitle = "Quick access to pinned trains (${TrainTrackerState.savedTrains.size})",',
              'title = I18n.t("pref_saved_trains"),\n                        subtitle = "${I18n.t("pref_saved_trains_sub")} (${TrainTrackerState.savedTrains.size})",')
c = c.replace('title = "Share Live Status",\n                        subtitle = "Send journey status link",',
              'title = I18n.t("pref_share_status"),\n                        subtitle = I18n.t("pref_share_status_sub"),')
c = c.replace('text = "MAP STYLE",', 'text = I18n.t("pref_map_style"),')

old_styles = """                            val styles = listOf(
                                "light" to "Light",
                                "dark" to "Dark",
                                "satellite" to "Satellite",
                                "terrain" to "Terrain"
                            )"""

new_styles = """                            val styles = listOf(
                                "light" to I18n.t("map_style_light"),
                                "dark" to I18n.t("map_style_dark"),
                                "satellite" to I18n.t("map_style_satellite"),
                                "terrain" to I18n.t("map_style_terrain")
                            )"""
c = c.replace(old_styles, new_styles)

with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/ui/components/PreferencesModal.kt", "w") as f:
    f.write(c)

print("Updated PreferencesModal.kt successfully")
