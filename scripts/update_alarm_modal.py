with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/ui/components/DestinationAlarmModal.kt", "r") as f:
    c = f.read()

# Add import if missing
if "import com.railgaadi.app.i18n.I18n" not in c:
    c = c.replace("import com.railgaadi.app.theme.*", "import com.railgaadi.app.theme.*\nimport com.railgaadi.app.i18n.I18n")

# Header
c = c.replace('text = "Destination Wake-Up Alarm",', 'text = I18n.t("alarm_title"),')
c = c.replace('text = "GPS geofence alerts you even in sleep mode",', 'text = I18n.t("alarm_sub"),')
c = c.replace('text = "● ARMED",', 'text = "● " + I18n.t("armed_badge"),')

# Station selector
c = c.replace('text = "TARGET DESTINATION STATION",', 'text = I18n.t("target_destination"),')
c = c.replace('text = selectedStation.name,\n                                    fontSize = 14.sp,',
              'text = I18n.stationName(selectedStation.name),\n                                    fontSize = 14.sp,')
c = c.replace('text = if (isStationPickerOpen) "▲ Change" else "▼ Change"',
              'text = (if (isStationPickerOpen) "▲ " else "▼ ") + I18n.t("change")')
c = c.replace('Text(stn.name, fontSize = 13.sp, fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal, color = textPrimary)',
              'Text(I18n.stationName(stn.name), fontSize = 13.sp, fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal, color = textPrimary)')

# Trigger selector
c = c.replace('text = "WHEN TO WAKE YOU UP",', 'text = I18n.t("when_to_wake"),')
c = c.replace('"By Distance"', 'I18n.t("by_distance")')
c = c.replace('"By Time"', 'I18n.t("by_time")')
c = c.replace('text = "Custom Distance",', 'text = I18n.t("custom_distance"),')
c = c.replace('text = "Enter any distance (1 - 999 km)",', 'text = I18n.t("enter_any_distance"),')
c = c.replace('text = "Custom Time",', 'text = I18n.t("custom_time"),')
c = c.replace('text = "Enter minutes before arrival",', 'text = I18n.t("enter_minutes_before"),')

c = c.replace('"Alarms automatically as soon as the train is ${triggerDistanceKm.toInt()} km away from ${selectedStation.name}"',
              '"${I18n.t(\\"alarm_auto_distance\\")} ${triggerDistanceKm.toInt()} km ${I18n.t(\\"km_to\\")} ${I18n.stationName(selectedStation.name)}"')
c = c.replace('"Alarms estimated $triggerMinutesBefore minutes before the train pulls into ${selectedStation.name}"',
              '"${I18n.t(\\"alarm_auto_time\\")} $triggerMinutesBefore min (${I18n.stationName(selectedStation.name)})"')

# Sound & toggles
c = c.replace('text = "ALERT SOUND & TONES",', 'text = I18n.t("alert_sound_tones"),')
c = c.replace('Text("Station Voice Announcement", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)',
              'Text(I18n.t("station_voice_announcement"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)')
c = c.replace('Text("IR-style speaker announcement", fontSize = 11.sp, color = textSecondary)',
              'Text(I18n.t("station_voice_sub"), fontSize = 11.sp, color = textSecondary)')
c = c.replace('Text("Continuous Vibration", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)',
              'Text(I18n.t("continuous_vibration"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)')
c = c.replace('Text("Haptic pulses while phone is in pocket", fontSize = 11.sp, color = textSecondary)',
              'Text(I18n.t("vibration_sub"), fontSize = 11.sp, color = textSecondary)')

# Buttons
c = c.replace('Text("🔊 Test Sound", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RailBluePrimary)',
              'Text("🔊 " + I18n.t("test_sound"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RailBluePrimary)')
c = c.replace('if (existingAlarm?.isArmed == true) "Update Alarm" else "Arm Wake-Up Alarm"',
              'if (existingAlarm?.isArmed == true) I18n.t("update_alarm") else I18n.t("arm_wake_up_alarm")')
c = c.replace('Text("✕ Turn Off Wake-Up Alarm", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RailRedDelayed)',
              'Text("✕ " + I18n.t("turn_off_alarm"), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RailRedDelayed)')

with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/ui/components/DestinationAlarmModal.kt", "w") as f:
    f.write(c)

print("Updated DestinationAlarmModal.kt successfully!")
