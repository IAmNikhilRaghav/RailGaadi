package com.railgaadi.app.model

data class StationInfo(
    val code: String,
    val name: String,
    val city: String = "",
    val state: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0
)

object StationDataService {
    // Dynamic station cache populated by live search, route geometry & user interactions
    private val dynamicCache = mutableMapOf<String, StationInfo>()

    // Comprehensive Directory of Major Pan-India Railway Stations & Junctions with exact coordinates
    val allStations = listOf(
        // Delhi NCR & Haryana
        StationInfo("NDLS", "New Delhi", "Delhi", "Delhi", 28.6423, 77.2200),
        StationInfo("DLI", "Old Delhi Jn", "Delhi", "Delhi", 28.6606, 77.2285),
        StationInfo("NZM", "Hazrat Nizamuddin", "Delhi", "Delhi", 28.5888, 77.2534),
        StationInfo("ANVT", "Anand Vihar Terminal", "Delhi", "Delhi", 28.6469, 77.3160),
        StationInfo("DEC", "Delhi Cantt", "Delhi", "Delhi", 28.5912, 77.1214),
        StationInfo("GZB", "Ghaziabad Jn", "Ghaziabad", "Uttar Pradesh", 28.6538, 77.4326),
        StationInfo("UMB", "Ambala Cantt Jn", "Ambala", "Haryana", 30.3389, 76.8270),
        StationInfo("KLK", "Kalka", "Kalka", "Haryana", 30.8377, 76.9318),
        StationInfo("RE", "Rewari Jn", "Rewari", "Haryana", 28.2028, 76.6094),

        // Uttar Pradesh
        StationInfo("MTJ", "Mathura Jn", "Mathura", "Uttar Pradesh", 27.4801, 77.6731),
        StationInfo("MRT", "Mathura Cantt", "Mathura", "Uttar Pradesh", 27.4912, 77.6851),
        StationInfo("AGC", "Agra Cantt", "Agra", "Uttar Pradesh", 27.1580, 77.9902),
        StationInfo("AF", "Agra Fort", "Agra", "Uttar Pradesh", 27.1767, 78.0081),
        StationInfo("TDL", "Tundla Jn", "Tundla", "Uttar Pradesh", 27.2064, 78.2415),
        StationInfo("ALJN", "Aligarh Jn", "Aligarh", "Uttar Pradesh", 27.8974, 78.0880),
        StationInfo("KRJ", "Khurja Jn", "Khurja", "Uttar Pradesh", 28.2064, 77.8190),
        StationInfo("KJY", "Khurja City", "Khurja", "Uttar Pradesh", 28.2532, 77.8420),
        StationInfo("KSV", "Kosi Kalan", "Kosi Kalan", "Uttar Pradesh", 27.7886, 77.4460),
        StationInfo("CNB", "Kanpur Central", "Kanpur", "Uttar Pradesh", 26.4542, 80.3510),
        StationInfo("PRYJ", "Prayagraj Jn", "Prayagraj", "Uttar Pradesh", 25.4462, 81.8288),
        StationInfo("BSB", "Varanasi Jn", "Varanasi", "Uttar Pradesh", 25.3273, 82.9865),
        StationInfo("DDU", "Pt. Deen Dayal Upadhyaya Jn", "Mughalsarai", "Uttar Pradesh", 25.2818, 83.1189),
        StationInfo("LKO", "Lucknow Charbagh", "Lucknow", "Uttar Pradesh", 26.8307, 80.9253),
        StationInfo("LJN", "Lucknow Jn", "Lucknow", "Uttar Pradesh", 26.8318, 80.9220),
        StationInfo("GKP", "Gorakhpur Jn", "Gorakhpur", "Uttar Pradesh", 26.7593, 83.3815),
        StationInfo("MB", "Moradabad Jn", "Moradabad", "Uttar Pradesh", 28.8316, 78.7658),
        StationInfo("BE", "Bareilly Jn", "Bareilly", "Uttar Pradesh", 28.3372, 79.4108),
        StationInfo("VGLJ", "Veerangana Lakshmibai Jhansi", "Jhansi", "Uttar Pradesh", 25.4436, 78.5530),
        StationInfo("AY", "Ayodhya Dham Jn", "Ayodhya", "Uttar Pradesh", 26.7922, 82.1998),
        StationInfo("AYC", "Ayodhya Cantt", "Ayodhya", "Uttar Pradesh", 26.7723, 82.1388),
        StationInfo("ETW", "Etawah Jn", "Etawah", "Uttar Pradesh", 26.7855, 79.0253),

        // Rajasthan
        StationInfo("JP", "Jaipur Jn", "Jaipur", "Rajasthan", 26.9202, 75.7869),
        StationInfo("GADJ", "Gandhinagar Jaipur", "Jaipur", "Rajasthan", 26.8837, 75.8012),
        StationInfo("DO", "Dausa Jn", "Dausa", "Rajasthan", 26.8920, 76.3380),
        StationInfo("BKI", "Bandikui Jn", "Bandikui", "Rajasthan", 27.0393, 76.5667),
        StationInfo("AWR", "Alwar Jn", "Alwar", "Rajasthan", 27.5593, 76.6213),
        StationInfo("FL", "Phulera Jn", "Phulera", "Rajasthan", 26.8730, 75.2410),
        StationInfo("RGS", "Ringas Jn", "Ringas", "Rajasthan", 27.3580, 75.5680),
        StationInfo("SIKR", "Sikar Jn", "Sikar", "Rajasthan", 27.6160, 75.1430),
        StationInfo("CUR", "Churu", "Churu", "Rajasthan", 28.2930, 74.9660),
        StationInfo("RTGH", "Ratangarh Jn", "Ratangarh", "Rajasthan", 28.0830, 74.6200),
        StationInfo("BKN", "Bikaner Jn", "Bikaner", "Rajasthan", 28.0141, 73.3166),
        StationInfo("LGNH", "Lalgarh Jn", "Bikaner", "Rajasthan", 28.0559, 73.2819),
        StationInfo("JU", "Jodhpur Jn", "Jodhpur", "Rajasthan", 26.2838, 73.0232),
        StationInfo("AII", "Ajmer Jn", "Ajmer", "Rajasthan", 26.4566, 74.6375),
        StationInfo("UDZ", "Udaipur City", "Udaipur", "Rajasthan", 24.5854, 73.6972),
        StationInfo("KOTA", "Kota Jn", "Kota", "Rajasthan", 25.2236, 75.8805),
        StationInfo("BTE", "Bharatpur Jn", "Bharatpur", "Rajasthan", 27.2152, 77.4930),
        StationInfo("BME", "Barmer", "Barmer", "Rajasthan", 25.7445, 71.3971),
        StationInfo("SWM", "Sawai Madhopur Jn", "Sawai Madhopur", "Rajasthan", 25.9928, 76.3685),
        StationInfo("ABR", "Abu Road", "Abu Road", "Rajasthan", 24.4820, 72.7810),
        StationInfo("FA", "Falna", "Falna", "Rajasthan", 25.2358, 73.2350),

        // Maharashtra & Mumbai
        StationInfo("MMCT", "Mumbai Central", "Mumbai", "Maharashtra", 18.9696, 72.8193),
        StationInfo("CSMT", "Chhatrapati Shivaji Maharaj Terminus", "Mumbai", "Maharashtra", 18.9401, 72.8354),
        StationInfo("BDTS", "Bandra Terminus", "Mumbai", "Maharashtra", 19.0620, 72.8420),
        StationInfo("LTT", "Lokmanya Tilak Terminus", "Mumbai", "Maharashtra", 19.0694, 72.8911),
        StationInfo("DR", "Dadar Central", "Mumbai", "Maharashtra", 19.0172, 72.8430),
        StationInfo("DDR", "Dadar Western", "Mumbai", "Maharashtra", 19.0197, 72.8435),
        StationInfo("BVI", "Borivali", "Mumbai", "Maharashtra", 19.2287, 72.8564),
        StationInfo("TNA", "Thane", "Thane", "Maharashtra", 19.1858, 72.9754),
        StationInfo("KYN", "Kalyan Jn", "Kalyan", "Maharashtra", 19.2347, 73.1297),
        StationInfo("PNVL", "Panvel Jn", "Navi Mumbai", "Maharashtra", 18.9909, 73.1218),
        StationInfo("PUNE", "Pune Jn", "Pune", "Maharashtra", 18.5294, 73.8731),
        StationInfo("NGP", "Nagpur Jn", "Nagpur", "Maharashtra", 21.1536, 79.0890),
        StationInfo("BSL", "Bhusaval Jn", "Bhusaval", "Maharashtra", 21.0473, 75.7887),
        StationInfo("MMR", "Manmad Jn", "Manmad", "Maharashtra", 20.2499, 74.4383),
        StationInfo("NK", "Nashik Road", "Nashik", "Maharashtra", 19.9575, 73.8340),
        StationInfo("SUR", "Solapur Jn", "Solapur", "Maharashtra", 17.6645, 75.8934),
        StationInfo("KOP", "Kolhapur CSMT", "Kolhapur", "Maharashtra", 16.7020, 74.2410),

        // West Bengal & East
        StationInfo("HWH", "Howrah Jn", "Kolkata", "West Bengal", 22.5841, 88.3410),
        StationInfo("SDAH", "Sealdah", "Kolkata", "West Bengal", 22.5668, 88.3747),
        StationInfo("KOAA", "Kolkata Chitpur", "Kolkata", "West Bengal", 22.6012, 88.3852),
        StationInfo("KGP", "Kharagpur Jn", "Kharagpur", "West Bengal", 22.3414, 87.3285),
        StationInfo("ASN", "Asansol Jn", "Asansol", "West Bengal", 23.6914, 86.9752),
        StationInfo("NJP", "New Jalpaiguri", "Siliguri", "West Bengal", 26.6830, 88.4434),
        StationInfo("MLDT", "Malda Town", "Malda", "West Bengal", 25.0157, 88.1303),

        // Bihar & Jharkhand
        StationInfo("PNBE", "Patna Jn", "Patna", "Bihar", 25.6026, 85.1368),
        StationInfo("PPTA", "Patliputra Jn", "Patna", "Bihar", 25.6360, 85.0930),
        StationInfo("DNR", "Danapur", "Patna", "Bihar", 25.6170, 85.0480),
        StationInfo("GAYA", "Gaya Jn", "Gaya", "Bihar", 24.8040, 84.9993),
        StationInfo("MFP", "Muzaffarpur Jn", "Muzaffarpur", "Bihar", 26.1210, 85.3850),
        StationInfo("BJU", "Barauni Jn", "Barauni", "Bihar", 25.4740, 85.9720),
        StationInfo("DHN", "Dhanbad Jn", "Dhanbad", "Jharkhand", 23.7910, 86.4290),
        StationInfo("RNC", "Ranchi Jn", "Ranchi", "Jharkhand", 23.3510, 85.3350),
        StationInfo("TATA", "Tatanagar Jn", "Jamshedpur", "Jharkhand", 22.7690, 86.2006),

        // Madhya Pradesh & Chhattisgarh
        StationInfo("BPL", "Bhopal Jn", "Bhopal", "Madhya Pradesh", 23.2669, 77.4131),
        StationInfo("RKMP", "Rani Kamlapati", "Bhopal", "Madhya Pradesh", 23.2054, 77.4384),
        StationInfo("INDB", "Indore Jn", "Indore", "Madhya Pradesh", 22.7170, 75.8680),
        StationInfo("GWL", "Gwalior Jn", "Gwalior", "Madhya Pradesh", 26.2165, 78.1823),
        StationInfo("JBP", "Jabalpur Jn", "Jabalpur", "Madhya Pradesh", 23.1670, 79.9520),
        StationInfo("UJN", "Ujjain Jn", "Ujjain", "Madhya Pradesh", 23.1820, 75.7770),
        StationInfo("RTM", "Ratlam Jn", "Ratlam", "Madhya Pradesh", 23.3404, 75.0508),
        StationInfo("R", "Raipur Jn", "Raipur", "Chhattisgarh", 21.2580, 81.6300),
        StationInfo("BSP", "Bilaspur Jn", "Bilaspur", "Chhattisgarh", 22.0575, 82.1676),
        StationInfo("DURG", "Durg Jn", "Durg", "Chhattisgarh", 21.1900, 81.2850),

        // Gujarat
        StationInfo("ADI", "Ahmedabad Jn", "Ahmedabad", "Gujarat", 23.0255, 72.6015),
        StationInfo("BRC", "Vadodara Jn", "Vadodara", "Gujarat", 22.3108, 73.1811),
        StationInfo("ST", "Surat", "Surat", "Gujarat", 21.2066, 72.8408),
        StationInfo("RJT", "Rajkot Jn", "Rajkot", "Gujarat", 22.3080, 70.8020),
        StationInfo("GNC", "Gandhinagar Capital", "Gandhinagar", "Gujarat", 23.2320, 72.6480),
        StationInfo("BVC", "Bhavnagar Terminus", "Bhavnagar", "Gujarat", 21.7640, 72.1380),
        StationInfo("VAPI", "Vapi", "Vapi", "Gujarat", 20.3743, 72.9091),

        // Punjab, Chandigarh, J&K, Uttarakhand
        StationInfo("ASR", "Amritsar Jn", "Amritsar", "Punjab", 31.6315, 74.8580),
        StationInfo("LDH", "Ludhiana Jn", "Ludhiana", "Punjab", 30.9080, 75.8560),
        StationInfo("JUC", "Jalandhar City", "Jalandhar", "Punjab", 31.3317, 75.5915),
        StationInfo("CDG", "Chandigarh", "Chandigarh", "Chandigarh", 30.7020, 76.8221),
        StationInfo("HW", "Haridwar Jn", "Haridwar", "Uttarakhand", 29.9478, 78.1549),
        StationInfo("DDN", "Dehradun", "Dehradun", "Uttarakhand", 30.3140, 78.0336),
        StationInfo("JAT", "Jammu Tawi", "Jammu", "Jammu & Kashmir", 32.7070, 74.8801),
        StationInfo("SVDK", "SMVD Katra", "Katra", "Jammu & Kashmir", 32.9930, 74.9310),

        // South India
        StationInfo("MAS", "Chennai Central", "Chennai", "Tamil Nadu", 13.0848, 80.2749),
        StationInfo("MS", "Chennai Egmore", "Chennai", "Tamil Nadu", 13.0780, 80.2610),
        StationInfo("CBE", "Coimbatore Jn", "Coimbatore", "Tamil Nadu", 10.9976, 76.9663),
        StationInfo("MDU", "Madurai Jn", "Madurai", "Tamil Nadu", 9.9199, 78.1103),
        StationInfo("TPJ", "Tiruchchirappalli Jn", "Trichy", "Tamil Nadu", 10.7941, 78.6854),
        StationInfo("SBC", "KSR Bengaluru", "Bengaluru", "Karnataka", 12.9776, 77.5681),
        StationInfo("YPR", "Yesvantpur Jn", "Bengaluru", "Karnataka", 13.0230, 77.5500),
        StationInfo("MYS", "Mysuru Jn", "Mysore", "Karnataka", 12.3160, 76.6450),
        StationInfo("UBL", "SSS Hubballi Jn", "Hubli", "Karnataka", 15.3503, 75.1481),
        StationInfo("SC", "Secunderabad Jn", "Hyderabad", "Telangana", 17.4331, 78.5015),
        StationInfo("HYB", "Hyderabad Deccan", "Hyderabad", "Telangana", 17.3934, 78.4674),
        StationInfo("BZA", "Vijayawada Jn", "Vijayawada", "Andhra Pradesh", 16.5183, 80.6186),
        StationInfo("VSKP", "Visakhapatnam Jn", "Visakhapatnam", "Andhra Pradesh", 17.7216, 83.2895),
        StationInfo("TPTY", "Tirupati Main", "Tirupati", "Andhra Pradesh", 13.6276, 79.4190),
        StationInfo("TVC", "Thiruvananthapuram Central", "Trivandrum", "Kerala", 8.4867, 76.9512),
        StationInfo("ERS", "Ernakulam Jn", "Kochi", "Kerala", 9.9695, 76.2907),
        StationInfo("TCR", "Thrissur", "Thrissur", "Kerala", 10.5148, 76.2079),
        StationInfo("CLT", "Kozhikode Main", "Calicut", "Kerala", 11.2462, 75.7804),
        StationInfo("MAO", "Madgaon Jn", "Goa", "Goa", 15.2679, 73.9707),

        // Odisha & Northeast
        StationInfo("BBS", "Bhubaneswar", "Bhubaneswar", "Odisha", 20.2654, 85.8426),
        StationInfo("PURI", "Puri", "Puri", "Odisha", 19.8130, 85.8310),
        StationInfo("CTC", "Cuttack Jn", "Cuttack", "Odisha", 20.4666, 85.9016),
        StationInfo("GHY", "Guwahati", "Guwahati", "Assam", 26.1826, 91.7519),
        StationInfo("DBRG", "Dibrugarh", "Dibrugarh", "Assam", 27.4636, 94.9353)
    )

    private val stationMapByCode = allStations.associateBy { it.code.uppercase() }

    fun cacheStation(stn: StationInfo) {
        val code = stn.code.uppercase().trim()
        if (code.isNotEmpty()) {
            dynamicCache[code] = stn
        }
    }

    fun getCoordinates(code: String): Pair<Double, Double> {
        val clean = code.uppercase().trim()
        val cached = dynamicCache[clean]
        if (cached != null && cached.lat != 0.0 && cached.lon != 0.0) {
            return Pair(cached.lat, cached.lon)
        }

        val base = stationMapByCode[clean]
        if (base != null && base.lat != 0.0 && base.lon != 0.0) {
            return Pair(base.lat, base.lon)
        }

        return Pair(28.6139, 77.2090) // Default fallback to Delhi center
    }

    fun searchStations(query: String): List<StationInfo> {
        val q = query.trim().uppercase()
        if (q.isEmpty()) return allStations.take(12)

        val merged = (dynamicCache.values + allStations).distinctBy { it.code.uppercase() }

        return merged.filter { stn ->
            stn.code.contains(q, ignoreCase = true) ||
            stn.name.contains(q, ignoreCase = true) ||
            stn.city.contains(q, ignoreCase = true)
        }.sortedWith(
            compareBy<StationInfo> { stn ->
                if (stn.code.equals(q, ignoreCase = true)) 0
                else if (stn.code.startsWith(q, ignoreCase = true)) 1
                else if (stn.name.startsWith(q, ignoreCase = true)) 2
                else 3
            }.thenBy { it.name }
        ).take(15)
    }

    fun resolveStationCode(input: String): String {
        val clean = input.trim()
        // If formatted as "Station Name (CODE)"
        val parenMatch = Regex("""\(([A-Z0-9]+)\)""").find(clean)
        if (parenMatch != null) {
            return parenMatch.groupValues[1].uppercase()
        }

        val cleanUpper = clean.uppercase()
        if (dynamicCache.containsKey(cleanUpper)) {
            return cleanUpper
        }
        if (stationMapByCode.containsKey(cleanUpper)) {
            return cleanUpper
        }

        val nameMatch = (dynamicCache.values + allStations).find {
            it.name.contains(clean, ignoreCase = true) || it.city.contains(clean, ignoreCase = true)
        }
        if (nameMatch != null) return nameMatch.code

        return cleanUpper
    }
}
