# Fix DestinationAlarmModal.kt
with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/ui/components/DestinationAlarmModal.kt", "r") as f:
    c = f.read()

c = c.replace('\\"alarm_auto_distance\\"', '"alarm_auto_distance"')
c = c.replace('\\"km_to\\"', '"km_to"')
c = c.replace('\\"alarm_auto_time\\"', '"alarm_auto_time"')

with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/ui/components/DestinationAlarmModal.kt", "w") as f:
    f.write(c)

# Fix WeatherScreen.kt
with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/ui/screens/WeatherScreen.kt", "r") as f:
    w = f.read()

w = w.replace('\\"feels_like\\"', '"feels_like"')
w = w.replace('\\"dew_point\\"', '"dew_point"')
w = w.replace('\\"observed\\"', '"observed"')
w = w.replace('\\"current_impact\\"', '"current_impact"')

with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/ui/screens/WeatherScreen.kt", "w") as f:
    f.write(w)

print("Fixed string templates in both files")
