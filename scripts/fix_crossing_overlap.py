with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/ui/screens/AnalyticsScreen.kt", "r") as f:
    c = f.read()

old_block = """                                            if (isTrainInThisSegment && !isExpanded) {
                                                // ─────────────────────────────────────────────────────────────
                                                // ACTIVE TRAIN ON PLAIN BAR (Pass-Through Corridor)
                                                // ─────────────────────────────────────────────────────────────
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(IntrinsicSize.Min),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // LEFT: Scheduled / Estimated Time Column (WIMT style — no speed shown)
                                                    Column(
                                                        modifier = Modifier.width(62.dp),
                                                        horizontalAlignment = Alignment.Start,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        val timeDisplay = nextStation?.scheduledArr?.takeIf { it.isNotBlank() }
                                                            ?: curStation?.scheduledDep?.takeIf { it.isNotBlank() }
                                                            ?: ""
                                                        if (timeDisplay.isNotBlank()) {
                                                            Text(
                                                                text = timeDisplay,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isDark) DarkTextPrimary else LightTextPrimary
                                                            )
                                                        }
                                                    }

                                                    // CENTER: Active Track Spine with Pulsing Train Node
                                                    Column(
                                                        modifier = Modifier
                                                            .width(28.dp)
                                                            .fillMaxHeight(),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .width(3.dp)
                                                                .weight(1f)
                                                                .background(trackDone)
                                                        )
                                                        LiveTrainPulseNode()
                                                        Box(
                                                            modifier = Modifier
                                                                .width(3.dp)
                                                                .weight(1f)
                                                                .background(trackFuture)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    // RIGHT: Interactive Active Train Corridor Status Card
                                                    Card(
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = RailBluePrimary.copy(alpha = if (isDark) 0.16f else 0.08f)
                                                        ),
                                                        border = BorderStroke(1.dp, RailBluePrimary.copy(alpha = 0.45f)),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable {
                                                                expandedSegments.value = expandedSegments.value + segIdx
                                                            }
                                                            .padding(vertical = 2.dp)
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                                        ) {
                                                            val cardOdometer = TrainTrackerState.liveDistanceOffsetKm
                                                            val nextHaltStation = segments.getOrNull(segIdx + 1)?.haltStation
                                                            val targetStop = nextHaltStation ?: nextStation ?: upcomingStation
                                                            val curStn = curStation ?: stations.getOrNull(curIdx)
                                                            val stopDist = targetStop?.let { TrainTrackerState.getLiveRemainingKmToStation(it, curStn) }
                                                                ?: kmToNext
                                                                ?: kmToUpcoming
                                                                ?: if (targetStop != null && curStn != null && targetStop.distanceKm > curStn.distanceKm) {
                                                                    (targetStop.distanceKm - curStn.distanceKm).coerceAtLeast(0.0)
                                                                } else null

                                                            val approachingHeader = when {
                                                                stopDist != null && stopDist <= 0.3 -> "Arrived at ${I18n.stationName(targetStop?.name ?: "Station")}"
                                                                stopDist != null && stopDist <= 8.0 -> {
                                                                    val dist = stopDist.roundToInt().coerceAtLeast(1)
                                                                    I18n.formatKmRemaining(dist, targetStop?.name ?: "")
                                                                }
                                                                else -> {
                                                                    val cur = curStation ?: stations.getOrNull(curIdx)
                                                                    val name = cur?.name ?: ""
                                                                    if (name.isNotBlank()) {
                                                                        I18n.formatCrossingStatus(name, cur?.haltMinutes == 0)
                                                                    } else {
                                                                        I18n.t("en_route", "En Route")
                                                                    }
                                                                }
                                                            }

                                                            // Line 1: Header + Compact stations expand button
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = approachingHeader,
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = RailBluePrimary,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    modifier = Modifier.weight(1f, fill = false)
                                                                )

                                                                if (ptCount > 0) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .clip(RoundedCornerShape(20.dp))
                                                                            .background(RailBluePrimary.copy(alpha = 0.15f))
                                                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                                                    ) {
                                                                        Row(
                                                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                                            verticalAlignment = Alignment.CenterVertically
                                                                        ) {
                                                                            Text("▼", fontSize = 8.sp, color = RailBluePrimary)
                                                                            Text(
                                                                                text = I18n.formatPassThrough(ptCount),
                                                                                fontSize = 10.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = RailBluePrimary
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            // Line 2: Full width delay text (appears in one line without truncating)
                                                            val delay = liveStatus?.delayMinutes ?: 0
                                                            val delayText = I18n.formatDelay(delay)
                                                            Text(
                                                                text = "((•)) $delayText",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = if (delay <= 0) RailGreenOnTime else RailRedDelayed,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                // Standard Pass-Through Collapse/Expand Pill Row
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(IntrinsicSize.Min),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {"""

new_block = """                                            if (isTrainInThisSegment && !isExpanded) {
                                                // ─────────────────────────────────────────────────────────────
                                                // ACTIVE TRAIN ON PLAIN BAR (Pass-Through Corridor) - SEPARATE ROW
                                                // ─────────────────────────────────────────────────────────────
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(IntrinsicSize.Min),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // LEFT: Scheduled / Estimated Time Column (WIMT style — no speed shown)
                                                    Column(
                                                        modifier = Modifier.width(62.dp),
                                                        horizontalAlignment = Alignment.Start,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        val timeDisplay = nextStation?.scheduledArr?.takeIf { it.isNotBlank() }
                                                            ?: curStation?.scheduledDep?.takeIf { it.isNotBlank() }
                                                            ?: ""
                                                        if (timeDisplay.isNotBlank()) {
                                                            Text(
                                                                text = timeDisplay,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isDark) DarkTextPrimary else LightTextPrimary
                                                            )
                                                        }
                                                    }

                                                    // CENTER: Active Track Spine with Pulsing Train Node
                                                    Column(
                                                        modifier = Modifier
                                                            .width(28.dp)
                                                            .fillMaxHeight(),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .width(3.dp)
                                                                .weight(1f)
                                                                .background(trackDone)
                                                        )
                                                        LiveTrainPulseNode()
                                                        Box(
                                                            modifier = Modifier
                                                                .width(3.dp)
                                                                .weight(1f)
                                                                .background(trackFuture)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    // RIGHT: Interactive Active Train Corridor Status Card
                                                    Card(
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = RailBluePrimary.copy(alpha = if (isDark) 0.16f else 0.08f)
                                                        ),
                                                        border = BorderStroke(1.dp, RailBluePrimary.copy(alpha = 0.45f)),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable {
                                                                expandedSegments.value = expandedSegments.value + segIdx
                                                            }
                                                            .padding(vertical = 4.dp)
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                                        ) {
                                                            val cardOdometer = TrainTrackerState.liveDistanceOffsetKm
                                                            val nextHaltStation = segments.getOrNull(segIdx + 1)?.haltStation
                                                            val targetStop = nextHaltStation ?: nextStation ?: upcomingStation
                                                            val curStn = curStation ?: stations.getOrNull(curIdx)
                                                            val stopDist = targetStop?.let { TrainTrackerState.getLiveRemainingKmToStation(it, curStn) }
                                                                ?: kmToNext
                                                                ?: kmToUpcoming
                                                                ?: if (targetStop != null && curStn != null && targetStop.distanceKm > curStn.distanceKm) {
                                                                    (targetStop.distanceKm - curStn.distanceKm).coerceAtLeast(0.0)
                                                                } else null

                                                            val approachingHeader = when {
                                                                stopDist != null && stopDist <= 0.3 -> "Arrived at ${I18n.stationName(targetStop?.name ?: "Station")}"
                                                                stopDist != null && stopDist <= 8.0 -> {
                                                                    val dist = stopDist.roundToInt().coerceAtLeast(1)
                                                                    I18n.formatKmRemaining(dist, targetStop?.name ?: "")
                                                                }
                                                                else -> {
                                                                    val cur = curStation ?: stations.getOrNull(curIdx)
                                                                    val name = cur?.name ?: ""
                                                                    if (name.isNotBlank()) {
                                                                        I18n.formatCrossingStatus(name, cur?.haltMinutes == 0)
                                                                    } else {
                                                                        I18n.t("en_route", "En Route")
                                                                    }
                                                                }
                                                            }

                                                            // Line 1: Full-width Header — never truncated or cramped
                                                            Text(
                                                                text = approachingHeader,
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = RailBluePrimary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.fillMaxWidth()
                                                            )

                                                            // Line 2: Full width delay text
                                                            val delay = liveStatus?.delayMinutes ?: 0
                                                            val delayText = I18n.formatDelay(delay)
                                                            Text(
                                                                text = "((•)) $delayText",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = if (delay <= 0) RailGreenOnTime else RailRedDelayed,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Standard Pass-Through Collapse/Expand Pill Row (Always displayed as separate banner)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(IntrinsicSize.Min),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {"""

assert old_block in c, "Could not find target block in AnalyticsScreen.kt"
c = c.replace(old_block, new_block)

# Also remove the closing brace for the old 'else' block around line 1304
old_else_close = """                                                    // RIGHT: empty departure column
                                                    Spacer(modifier = Modifier.width(62.dp))
                                                }
                                            }"""

new_else_close = """                                                    // RIGHT: empty departure column
                                                    Spacer(modifier = Modifier.width(62.dp))
                                                }"""

assert old_else_close in c, "Could not find old_else_close in AnalyticsScreen.kt"
c = c.replace(old_else_close, new_else_close)

with open("mobile/composeApp/src/commonMain/kotlin/com/railgaadi/app/ui/screens/AnalyticsScreen.kt", "w") as f:
    f.write(c)

print("Successfully updated AnalyticsScreen.kt to separate Crossing and Stations Banner!")
