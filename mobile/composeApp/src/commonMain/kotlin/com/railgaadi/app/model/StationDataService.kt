package com.railgaadi.app.model

data class StationInfo(
    val code: String,
    val name: String,
    val city: String = "",
    val state: String = ""
)

object StationDataService {
    // 100+ Major Indian Railway Junctions and Stations across all Zones
    val allStations = listOf(
        // Delhi NCR
        StationInfo("NDLS", "New Delhi", "Delhi", "Delhi"),
        StationInfo("DLI", "Old Delhi Jn", "Delhi", "Delhi"),
        StationInfo("NZM", "Hazrat Nizamuddin", "Delhi", "Delhi"),
        StationInfo("ANVT", "Anand Vihar Terminal", "Delhi", "Delhi"),
        StationInfo("DEC", "Delhi Cantt", "Delhi", "Delhi"),
        StationInfo("GZB", "Ghaziabad Jn", "Ghaziabad", "Uttar Pradesh"),

        // Uttar Pradesh
        StationInfo("MTJ", "Mathura Jn", "Mathura", "Uttar Pradesh"),
        StationInfo("AGC", "Agra Cantt", "Agra", "Uttar Pradesh"),
        StationInfo("AF", "Agra Fort", "Agra", "Uttar Pradesh"),
        StationInfo("TDL", "Tundla Jn", "Tundla", "Uttar Pradesh"),
        StationInfo("ALJN", "Aligarh Jn", "Aligarh", "Uttar Pradesh"),
        StationInfo("CNB", "Kanpur Central", "Kanpur", "Uttar Pradesh"),
        StationInfo("PRYJ", "Prayagraj Jn", "Prayagraj", "Uttar Pradesh"),
        StationInfo("BSB", "Varanasi Jn", "Varanasi", "Uttar Pradesh"),
        StationInfo("DDU", "Pt. Deen Dayal Upadhyaya Jn", "Mughalsarai", "Uttar Pradesh"),
        StationInfo("LKO", "Lucknow Charbagh", "Lucknow", "Uttar Pradesh"),
        StationInfo("LJN", "Lucknow Jn", "Lucknow", "Uttar Pradesh"),
        StationInfo("GKP", "Gorakhpur Jn", "Gorakhpur", "Uttar Pradesh"),
        StationInfo("MB", "Moradabad Jn", "Moradabad", "Uttar Pradesh"),
        StationInfo("BE", "Bareilly Jn", "Bareilly", "Uttar Pradesh"),
        StationInfo("VGLJ", "Veerangana Lakshmibai Jhansi", "Jhansi", "Uttar Pradesh"),
        StationInfo("AY", "Ayodhya Dham Jn", "Ayodhya", "Uttar Pradesh"),
        StationInfo("AYC", "Ayodhya Cantt", "Ayodhya", "Uttar Pradesh"),

        // Rajasthan
        StationInfo("JP", "Jaipur Jn", "Jaipur", "Rajasthan"),
        StationInfo("GADJ", "Gandhinagar Jaipur", "Jaipur", "Rajasthan"),
        StationInfo("BKN", "Bikaner Jn", "Bikaner", "Rajasthan"),
        StationInfo("LGNH", "Lalgarh Jn", "Bikaner", "Rajasthan"),
        StationInfo("JU", "Jodhpur Jn", "Jodhpur", "Rajasthan"),
        StationInfo("AII", "Ajmer Jn", "Ajmer", "Rajasthan"),
        StationInfo("UDZ", "Udaipur City", "Udaipur", "Rajasthan"),
        StationInfo("KOTA", "Kota Jn", "Kota", "Rajasthan"),
        StationInfo("BTE", "Bharatpur Jn", "Bharatpur", "Rajasthan"),
        StationInfo("BME", "Barmer", "Barmer", "Rajasthan"),
        StationInfo("AWR", "Alwar Jn", "Alwar", "Rajasthan"),
        StationInfo("BKI", "Bandikui Jn", "Bandikui", "Rajasthan"),
        StationInfo("SWM", "Sawai Madhopur Jn", "Sawai Madhopur", "Rajasthan"),
        StationInfo("ABR", "Abu Road", "Abu Road", "Rajasthan"),

        // Maharashtra & Mumbai
        StationInfo("MMCT", "Mumbai Central", "Mumbai", "Maharashtra"),
        StationInfo("CSMT", "Chhatrapati Shivaji Maharaj Terminus", "Mumbai", "Maharashtra"),
        StationInfo("BDTS", "Bandra Terminus", "Mumbai", "Maharashtra"),
        StationInfo("LTT", "Lokmanya Tilak Terminus", "Mumbai", "Maharashtra"),
        StationInfo("DR", "Dadar Central", "Mumbai", "Maharashtra"),
        StationInfo("DDR", "Dadar Western", "Mumbai", "Maharashtra"),
        StationInfo("BVI", "Borivali", "Mumbai", "Maharashtra"),
        StationInfo("TNA", "Thane", "Thane", "Maharashtra"),
        StationInfo("KYN", "Kalyan Jn", "Kalyan", "Maharashtra"),
        StationInfo("PNVL", "Panvel Jn", "Navi Mumbai", "Maharashtra"),
        StationInfo("PUNE", "Pune Jn", "Pune", "Maharashtra"),
        StationInfo("NGP", "Nagpur Jn", "Nagpur", "Maharashtra"),
        StationInfo("BSL", "Bhusaval Jn", "Bhusaval", "Maharashtra"),
        StationInfo("MMR", "Manmad Jn", "Manmad", "Maharashtra"),
        StationInfo("NK", "Nashik Road", "Nashik", "Maharashtra"),
        StationInfo("SUR", "Solapur Jn", "Solapur", "Maharashtra"),
        StationInfo("KOP", "Kolhapur CSMT", "Kolhapur", "Maharashtra"),

        // West Bengal & East
        StationInfo("HWH", "Howrah Jn", "Kolkata", "West Bengal"),
        StationInfo("SDAH", "Sealdah", "Kolkata", "West Bengal"),
        StationInfo("KOAA", "Kolkata Chitpur", "Kolkata", "West Bengal"),
        StationInfo("KGP", "Kharagpur Jn", "Kharagpur", "West Bengal"),
        StationInfo("ASN", "Asansol Jn", "Asansol", "West Bengal"),
        StationInfo("NJP", "New Jalpaiguri", "Siliguri", "West Bengal"),
        StationInfo("MLDT", "Malda Town", "Malda", "West Bengal"),

        // Bihar & Jharkhand
        StationInfo("PNBE", "Patna Jn", "Patna", "Bihar"),
        StationInfo("PPTA", "Patliputra Jn", "Patna", "Bihar"),
        StationInfo("DNR", "Danapur", "Patna", "Bihar"),
        StationInfo("GAYA", "Gaya Jn", "Gaya", "Bihar"),
        StationInfo("MFP", "Muzaffarpur Jn", "Muzaffarpur", "Bihar"),
        StationInfo("BJU", "Barauni Jn", "Barauni", "Bihar"),
        StationInfo("DHN", "Dhanbad Jn", "Dhanbad", "Jharkhand"),
        StationInfo("RNC", "Ranchi Jn", "Ranchi", "Jharkhand"),
        StationInfo("TATA", "Tatanagar Jn", "Jamshedpur", "Jharkhand"),

        // Madhya Pradesh & Chhattisgarh
        StationInfo("BPL", "Bhopal Jn", "Bhopal", "Madhya Pradesh"),
        StationInfo("RKMP", "Rani Kamlapati", "Bhopal", "Madhya Pradesh"),
        StationInfo("INDB", "Indore Jn", "Indore", "Madhya Pradesh"),
        StationInfo("GWL", "Gwalior Jn", "Gwalior", "Madhya Pradesh"),
        StationInfo("JBP", "Jabalpur Jn", "Jabalpur", "Madhya Pradesh"),
        StationInfo("UJN", "Ujjain Jn", "Ujjain", "Madhya Pradesh"),
        StationInfo("RTM", "Ratlam Jn", "Ratlam", "Madhya Pradesh"),
        StationInfo("R", "Raipur Jn", "Raipur", "Chhattisgarh"),
        StationInfo("BSP", "Bilaspur Jn", "Bilaspur", "Chhattisgarh"),
        StationInfo("DURG", "Durg Jn", "Durg", "Chhattisgarh"),

        // Gujarat
        StationInfo("ADI", "Ahmedabad Jn", "Ahmedabad", "Gujarat"),
        StationInfo("BRC", "Vadodara Jn", "Vadodara", "Gujarat"),
        StationInfo("ST", "Surat", "Surat", "Gujarat"),
        StationInfo("RJT", "Rajkot Jn", "Rajkot", "Gujarat"),
        StationInfo("GNC", "Gandhinagar Capital", "Gandhinagar", "Gujarat"),
        StationInfo("BVC", "Bhavnagar Terminus", "Bhavnagar", "Gujarat"),
        StationInfo("VAPI", "Vapi", "Vapi", "Gujarat"),

        // Punjab, Haryana, Chandigarh, Himachal, J&K
        StationInfo("ASR", "Amritsar Jn", "Amritsar", "Punjab"),
        StationInfo("LDH", "Ludhiana Jn", "Ludhiana", "Punjab"),
        StationInfo("JUC", "Jalandhar City", "Jalandhar", "Punjab"),
        StationInfo("CDG", "Chandigarh", "Chandigarh", "Chandigarh"),
        StationInfo("UMB", "Ambala Cantt Jn", "Ambala", "Haryana"),
        StationInfo("KLK", "Kalka", "Kalka", "Haryana"),
        StationInfo("HW", "Haridwar Jn", "Haridwar", "Uttarakhand"),
        StationInfo("DDN", "Dehradun", "Dehradun", "Uttarakhand"),
        StationInfo("JAT", "Jammu Tawi", "Jammu", "Jammu & Kashmir"),
        StationInfo("SVDK", "SMVD Katra", "Katra", "Jammu & Kashmir"),

        // South India (Tamil Nadu, Karnataka, Telangana, Andhra, Kerala)
        StationInfo("MAS", "Chennai Central", "Chennai", "Tamil Nadu"),
        StationInfo("MS", "Chennai Egmore", "Chennai", "Tamil Nadu"),
        StationInfo("CBE", "Coimbatore Jn", "Coimbatore", "Tamil Nadu"),
        StationInfo("MDU", "Madurai Jn", "Madurai", "Tamil Nadu"),
        StationInfo("TPJ", "Tiruchchirappalli Jn", "Trichy", "Tamil Nadu"),
        StationInfo("SBC", "KSR Bengaluru", "Bengaluru", "Karnataka"),
        StationInfo("YPR", "Yesvantpur Jn", "Bengaluru", "Karnataka"),
        StationInfo("MYS", "Mysuru Jn", "Mysore", "Karnataka"),
        StationInfo("UBL", "SSS Hubballi Jn", "Hubli", "Karnataka"),
        StationInfo("SC", "Secunderabad Jn", "Hyderabad", "Telangana"),
        StationInfo("HYB", "Hyderabad Deccan", "Hyderabad", "Telangana"),
        StationInfo("BZA", "Vijayawada Jn", "Vijayawada", "Andhra Pradesh"),
        StationInfo("VSKP", "Visakhapatnam Jn", "Visakhapatnam", "Andhra Pradesh"),
        StationInfo("TPTY", "Tirupati Main", "Tirupati", "Andhra Pradesh"),
        StationInfo("TVC", "Thiruvananthapuram Central", "Trivandrum", "Kerala"),
        StationInfo("ERS", "Ernakulam Jn", "Kochi", "Kerala"),
        StationInfo("TCR", "Thrissur", "Thrissur", "Kerala"),
        StationInfo("CLT", "Kozhikode Main", "Calicut", "Kerala"),
        StationInfo("MAO", "Madgaon Jn", "Goa", "Goa"),

        // Odisha & Northeast
        StationInfo("BBS", "Bhubaneswar", "Bhubaneswar", "Odisha"),
        StationInfo("PURI", "Puri", "Puri", "Odisha"),
        StationInfo("CTC", "Cuttack Jn", "Cuttack", "Odisha"),
        StationInfo("GHY", "Guwahati", "Guwahati", "Assam"),
        StationInfo("DBRG", "Dibrugarh", "Dibrugarh", "Assam")
    )

    private val stationMapByCode = allStations.associateBy { it.code.uppercase() }

    fun searchStations(query: String): List<StationInfo> {
        val q = query.trim().uppercase()
        if (q.isEmpty()) return allStations.take(12)

        return allStations.filter { stn ->
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
        // If has parentheses like "Mathura Jn (MTJ)"
        val parenMatch = Regex("""\(([A-Z0-9]+)\)""").find(clean)
        if (parenMatch != null) {
            return parenMatch.groupValues[1].uppercase()
        }

        // Direct code match
        val codeMatch = stationMapByCode[clean.uppercase()]
        if (codeMatch != null) return codeMatch.code

        // Search by name match
        val nameMatch = allStations.find {
            it.name.contains(clean, ignoreCase = true) || it.city.contains(clean, ignoreCase = true)
        }
        if (nameMatch != null) return nameMatch.code

        return clean.uppercase()
    }
}
