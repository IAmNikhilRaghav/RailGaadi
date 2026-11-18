with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/ui/screens/RoutesScreen.kt", "r") as f:
    c = f.read()

# 1. Search Trains Title
c = c.replace('text = "Search Trains",', 'text = I18n.t("search_trains"),')

# 2. From label and placeholder
c = c.replace('label = { Text("From", fontSize = 12.sp) },',
              'label = { Text(I18n.t("from"), fontSize = 12.sp) },')
c = c.replace('placeholder = { Text("Station name or code", color = Color.Gray, fontSize = 12.sp) },',
              'placeholder = { Text(I18n.t("station_code_or_name"), color = Color.Gray, fontSize = 12.sp) },')

# 3. From dropdown station name
old_from_drop = """                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(stn.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)"""
new_from_drop = """                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(I18n.stationName(stn.name), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)"""
c = c.replace(old_from_drop, new_from_drop, 1)

# 4. To label and placeholder
c = c.replace('label = { Text("To", fontSize = 12.sp) },',
              'label = { Text(I18n.t("to"), fontSize = 12.sp) },')

# 5. To dropdown station name
c = c.replace(old_from_drop, new_from_drop)

# 6. Recently Searched Trains
c = c.replace('"Recently Searched Trains",', 'I18n.t("recently_searched_trains"),')

# 7. Plan your train journey
c = c.replace('Text("Plan Your Train Journey", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)',
              'Text(I18n.t("plan_journey_title"), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)')
c = c.replace('"Search from 8,690+ Indian Railway stations. Pick a date and track trains live."',
              'I18n.t("plan_journey_sub")')

# 8. Feature pills
c = c.replace('FeaturePill(icon = "⚡", label = "Live GPS", modifier = Modifier.weight(1f))',
              'FeaturePill(icon = "⚡", label = I18n.t("live_gps"), modifier = Modifier.weight(1f))')
c = c.replace('FeaturePill(icon = "⏱️", label = "Delays", modifier = Modifier.weight(1f))',
              'FeaturePill(icon = "⏱️", label = I18n.t("delays"), modifier = Modifier.weight(1f))')
c = c.replace('FeaturePill(icon = "📅", label = "Calendar", modifier = Modifier.weight(1f))',
              'FeaturePill(icon = "📅", label = I18n.t("calendar"), modifier = Modifier.weight(1f))')

# 9. Date picker modal
c = c.replace('text = "Choose the date",\n                    fontWeight = FontWeight.Bold,\n                    fontSize = 16.sp,',
              'text = I18n.t("choose_the_date"),\n                    fontWeight = FontWeight.Bold,\n                    fontSize = 16.sp,')
c = c.replace('text = "for departure from $departureName",',
              'text = "${I18n.t("for_departure_from")} ${I18n.stationName(departureName)}",')
c = c.replace('text = "Choose from Calendar",',
              'text = I18n.t("choose_from_calendar"),')
c = c.replace('Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)',
              'Text(I18n.t("cancel"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)')

# Date options Yesterday, Today, Tomorrow
old_date_opts = """                val options = listOfNotNull(
                    yesterdayDate?.let { "Yesterday" to it },
                    "Today" to todayDate,
                    "Tomorrow" to tomorrowDate
                )"""
new_date_opts = """                val options = listOfNotNull(
                    yesterdayDate?.let { I18n.t("yesterday") to it },
                    I18n.t("today") to todayDate,
                    I18n.t("tomorrow") to tomorrowDate
                )"""
c = c.replace(old_date_opts, new_date_opts)

# 10. Track button
c = c.replace('text = "Track →",', 'text = "${I18n.t("track")} →",')

with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/ui/screens/RoutesScreen.kt", "w") as f:
    f.write(c)

print("Updated RoutesScreen.kt successfully!")
