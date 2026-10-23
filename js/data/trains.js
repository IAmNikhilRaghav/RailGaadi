/**
 * RailGaadi Curated Train Dataset
 * Includes premier Indian Railways services with verified route stations,
 * realistic timings, geospatial coordinates, distances, and elevation samples.
 */

export const TRAINS_DATA = [
  {
    trainId: "22436",
    number: "22436",
    name: "Vande Bharat Express",
    type: "Superfast / Vande Bharat",
    origin: "New Delhi (NDLS)",
    destination: "Varanasi Jn (BSB)",
    totalDistanceKm: 759,
    avgSpeedKmh: 95,
    runsOn: ["Tue", "Wed", "Fri", "Sat", "Sun"],
    status: "ON_TIME",
    delayMinutes: 0,
    currentStationIndex: 2, // Prayagraj Jn
    progressPercent: 65,
    speedKmh: 110,
    lastUpdatedSecondsAgo: 14,
    stations: [
      {
        code: "NDLS",
        name: "New Delhi",
        city: "New Delhi",
        lat: 28.6429,
        lon: 77.2195,
        scheduledArr: "START",
        scheduledDep: "06:00",
        actualArr: "START",
        actualDep: "06:00",
        distanceKm: 0,
        elevationM: 216,
        platform: "16",
        haltMin: 0
      },
      {
        code: "CNB",
        name: "Kanpur Central",
        city: "Kanpur",
        lat: 26.4538,
        lon: 80.3512,
        scheduledArr: "10:08",
        scheduledDep: "10:10",
        actualArr: "10:06",
        actualDep: "10:10",
        distanceKm: 440,
        elevationM: 126,
        platform: "5",
        haltMin: 2
      },
      {
        code: "PRYJ",
        name: "Prayagraj Jn",
        city: "Prayagraj",
        lat: 25.4484,
        lon: 81.8336,
        scheduledArr: "12:08",
        scheduledDep: "12:10",
        actualArr: "12:07",
        actualDep: "12:10",
        distanceKm: 635,
        elevationM: 98,
        platform: "6",
        haltMin: 2
      },
      {
        code: "BSB",
        name: "Varanasi Jn",
        city: "Varanasi",
        lat: 25.3267,
        lon: 82.9863,
        scheduledArr: "14:00",
        scheduledDep: "TERMINATES",
        actualArr: "13:58 (ETA)",
        actualDep: "TERMINATES",
        distanceKm: 759,
        elevationM: 81,
        platform: "1",
        haltMin: 0
      }
    ],
    // Detailed intermediate geo coordinates for high-fidelity polyline rendering
    routeCoordinates: [
      [77.2195, 28.6429], // NDLS
      [77.3178, 28.5355], // Noida Sector
      [77.5840, 28.3240], // Sikandrabad
      [77.8500, 28.0500], // Aligarh Outskirts
      [78.5000, 27.5000], // Tundla / Firozabad
      [79.0300, 27.0000], // Etawah
      [79.8000, 26.6000], // Rura
      [80.3512, 26.4538], // CNB (Kanpur)
      [80.8000, 26.1000], // Fatehpur corridor
      [81.2500, 25.7500], // Sirathu
      [81.8336, 25.4484], // PRYJ (Prayagraj)
      [82.2500, 25.3500], // Janghai Jn
      [82.6000, 25.3300], // Bhadohi
      [82.9863, 25.3267]  // BSB (Varanasi)
    ],
    elevationProfile: [
      { distanceKm: 0, elevationMeters: 216, station: "NDLS" },
      { distanceKm: 130, elevationMeters: 185 },
      { distanceKm: 280, elevationMeters: 160 },
      { distanceKm: 440, elevationMeters: 126, station: "CNB" },
      { distanceKm: 540, elevationMeters: 110 },
      { distanceKm: 635, elevationMeters: 98, station: "PRYJ" },
      { distanceKm: 700, elevationMeters: 88 },
      { distanceKm: 759, elevationMeters: 81, station: "BSB" }
    ]
  },
  {
    trainId: "12952",
    number: "12952",
    name: "Mumbai Tejas Rajdhani",
    type: "Rajdhani Express",
    origin: "New Delhi (NDLS)",
    destination: "Mumbai Central (MMCT)",
    totalDistanceKm: 1386,
    avgSpeedKmh: 91,
    runsOn: ["Daily"],
    status: "DELAYED",
    delayMinutes: 18,
    currentStationIndex: 3, // Ratlam Jn
    progressPercent: 54,
    speedKmh: 122,
    lastUpdatedSecondsAgo: 22,
    stations: [
      {
        code: "NDLS",
        name: "New Delhi",
        city: "Delhi",
        lat: 28.6429,
        lon: 77.2195,
        scheduledArr: "START",
        scheduledDep: "16:55",
        actualArr: "START",
        actualDep: "16:55",
        distanceKm: 0,
        elevationM: 216,
        platform: "3",
        haltMin: 0
      },
      {
        code: "KOTA",
        name: "Kota Jn",
        city: "Kota",
        lat: 25.2138,
        lon: 75.8648,
        scheduledArr: "21:30",
        scheduledDep: "21:40",
        actualArr: "21:38",
        actualDep: "21:48",
        distanceKm: 466,
        elevationM: 253,
        platform: "1",
        haltMin: 10
      },
      {
        code: "NAD",
        name: "Nagda Jn",
        city: "Nagda",
        lat: 23.4542,
        lon: 75.4132,
        scheduledArr: "00:05",
        scheduledDep: "00:07",
        actualArr: "00:20",
        actualDep: "00:22",
        distanceKm: 691,
        elevationM: 469,
        platform: "2",
        haltMin: 2
      },
      {
        code: "RTM",
        name: "Ratlam Jn",
        city: "Ratlam",
        lat: 23.3341,
        lon: 75.0376,
        scheduledArr: "00:40",
        scheduledDep: "00:45",
        actualArr: "00:58",
        actualDep: "01:03",
        distanceKm: 732,
        elevationM: 494,
        platform: "4",
        haltMin: 5
      },
      {
        code: "BRC",
        name: "Vadodara Jn",
        city: "Vadodara",
        lat: 22.3107,
        lon: 73.1812,
        scheduledArr: "03:52",
        scheduledDep: "04:00",
        actualArr: "04:10 (ETA)",
        actualDep: "04:18",
        distanceKm: 993,
        elevationM: 36,
        platform: "1",
        haltMin: 8
      },
      {
        code: "ST",
        name: "Surat",
        city: "Surat",
        lat: 21.2049,
        lon: 72.8407,
        scheduledArr: "05:13",
        scheduledDep: "05:18",
        actualArr: "05:31 (ETA)",
        actualDep: "05:36",
        distanceKm: 1122,
        elevationM: 18,
        platform: "2",
        haltMin: 5
      },
      {
        code: "BVI",
        name: "Borivali",
        city: "Mumbai",
        lat: 19.2288,
        lon: 72.8569,
        scheduledArr: "07:35",
        scheduledDep: "07:37",
        actualArr: "07:53 (ETA)",
        actualDep: "07:55",
        distanceKm: 1356,
        elevationM: 14,
        platform: "7",
        haltMin: 2
      },
      {
        code: "MMCT",
        name: "Mumbai Central",
        city: "Mumbai",
        lat: 18.9696,
        lon: 72.8193,
        scheduledArr: "08:35",
        scheduledDep: "TERMINATES",
        actualArr: "08:53 (ETA)",
        actualDep: "TERMINATES",
        distanceKm: 1386,
        elevationM: 7,
        platform: "5",
        haltMin: 0
      }
    ],
    routeCoordinates: [
      [77.2195, 28.6429], // NDLS
      [77.3000, 28.1500], // Mathura route
      [77.7000, 27.5000], // Bharatpur
      [76.8000, 26.5000], // Sawai Madhopur
      [75.8648, 25.2138], // Kota
      [75.5500, 24.3000], // Shamgarh
      [75.4132, 23.4542], // Nagda
      [75.0376, 23.3341], // Ratlam
      [74.2000, 22.8000], // Meghnagar / Dahod
      [73.5500, 22.4500], // Godhra
      [73.1812, 22.3107], // Vadodara
      [72.9500, 21.7000], // Bharuch
      [72.8407, 21.2049], // Surat
      [72.9000, 20.3700], // Vapi
      [72.8000, 19.8000], // Dahanu Road
      [72.8569, 19.2288], // Borivali
      [72.8193, 18.9696]  // Mumbai Central
    ],
    elevationProfile: [
      { distanceKm: 0, elevationMeters: 216, station: "NDLS" },
      { distanceKm: 300, elevationMeters: 220 },
      { distanceKm: 466, elevationMeters: 253, station: "KOTA" },
      { distanceKm: 691, elevationMeters: 469, station: "NAD" },
      { distanceKm: 732, elevationMeters: 494, station: "RTM (Highest)" },
      { distanceKm: 850, elevationMeters: 310 },
      { distanceKm: 993, elevationMeters: 36, station: "BRC" },
      { distanceKm: 1122, elevationMeters: 18, station: "ST" },
      { distanceKm: 1356, elevationMeters: 14, station: "BVI" },
      { distanceKm: 1386, elevationMeters: 7, station: "MMCT" }
    ]
  },
  {
    trainId: "12002",
    number: "12002",
    name: "Bhopal Shatabdi Express",
    type: "Shatabdi Express",
    origin: "New Delhi (NDLS)",
    destination: "Rani Kamlapati (RKMP)",
    totalDistanceKm: 708,
    avgSpeedKmh: 84,
    runsOn: ["Daily"],
    status: "ON_TIME",
    delayMinutes: 0,
    currentStationIndex: 3, // Gwalior
    progressPercent: 44,
    speedKmh: 130,
    lastUpdatedSecondsAgo: 8,
    stations: [
      {
        code: "NDLS",
        name: "New Delhi",
        city: "Delhi",
        lat: 28.6429,
        lon: 77.2195,
        scheduledArr: "START",
        scheduledDep: "06:00",
        actualArr: "START",
        actualDep: "06:00",
        distanceKm: 0,
        elevationM: 216,
        platform: "1",
        haltMin: 0
      },
      {
        code: "MTJ",
        name: "Mathura Jn",
        city: "Mathura",
        lat: 27.4924,
        lon: 77.6737,
        scheduledArr: "07:19",
        scheduledDep: "07:20",
        actualArr: "07:17",
        actualDep: "07:20",
        distanceKm: 141,
        elevationM: 177,
        platform: "1",
        haltMin: 1
      },
      {
        code: "AGC",
        name: "Agra Cantt",
        city: "Agra",
        lat: 27.1591,
        lon: 78.0098,
        scheduledArr: "07:50",
        scheduledDep: "07:55",
        actualArr: "07:49",
        actualDep: "07:55",
        distanceKm: 195,
        elevationM: 167,
        platform: "1",
        haltMin: 5
      },
      {
        code: "GWL",
        name: "Gwalior Jn",
        city: "Gwalior",
        lat: 26.2183,
        lon: 78.1828,
        scheduledArr: "09:23",
        scheduledDep: "09:28",
        actualArr: "09:22",
        actualDep: "09:28",
        distanceKm: 313,
        elevationM: 212,
        platform: "1",
        haltMin: 5
      },
      {
        code: "VGLJ",
        name: "VGL Jhansi Jn",
        city: "Jhansi",
        lat: 25.4484,
        lon: 78.5685,
        scheduledArr: "10:45",
        scheduledDep: "10:50",
        actualArr: "10:45 (ETA)",
        actualDep: "10:50",
        distanceKm: 410,
        elevationM: 258,
        platform: "2",
        haltMin: 5
      },
      {
        code: "BPL",
        name: "Bhopal Jn",
        city: "Bhopal",
        lat: 23.2599,
        lon: 77.4126,
        scheduledArr: "14:07",
        scheduledDep: "14:12",
        actualArr: "14:07 (ETA)",
        actualDep: "14:12",
        distanceKm: 702,
        elevationM: 505,
        platform: "1",
        haltMin: 5
      },
      {
        code: "RKMP",
        name: "Rani Kamlapati",
        city: "Bhopal",
        lat: 23.2185,
        lon: 77.4395,
        scheduledArr: "14:40",
        scheduledDep: "TERMINATES",
        actualArr: "14:40 (ETA)",
        actualDep: "TERMINATES",
        distanceKm: 708,
        elevationM: 496,
        platform: "5",
        haltMin: 0
      }
    ],
    routeCoordinates: [
      [77.2195, 28.6429], // NDLS
      [77.5000, 28.1000],
      [77.6737, 27.4924], // MTJ
      [78.0098, 27.1591], // AGC
      [78.1000, 26.6500], // Dholpur / Chambal Bridge
      [78.1828, 26.2183], // GWL
      [78.4000, 25.8000], // Datia
      [78.5685, 25.4484], // Jhansi
      [78.4000, 24.8000], // Lalitpur
      [78.1000, 24.1800], // Bina Jn
      [77.7500, 23.5300], // Vidisha
      [77.4126, 23.2599], // Bhopal Jn
      [77.4395, 23.2185]  // RKMP
    ],
    elevationProfile: [
      { distanceKm: 0, elevationMeters: 216, station: "NDLS" },
      { distanceKm: 141, elevationMeters: 177, station: "MTJ" },
      { distanceKm: 195, elevationMeters: 167, station: "AGC" },
      { distanceKm: 313, elevationMeters: 212, station: "GWL" },
      { distanceKm: 410, elevationMeters: 258, station: "VGLJ" },
      { distanceKm: 560, elevationMeters: 410, station: "BINA" },
      { distanceKm: 702, elevationMeters: 505, station: "BPL (Highest)" },
      { distanceKm: 708, elevationMeters: 496, station: "RKMP" }
    ]
  },
  {
    trainId: "12301",
    number: "12301",
    name: "Howrah Rajdhani Express",
    type: "Rajdhani Express",
    origin: "Howrah Jn (HWH)",
    destination: "New Delhi (NDLS)",
    totalDistanceKm: 1451,
    avgSpeedKmh: 86,
    runsOn: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat"],
    status: "EARLY",
    delayMinutes: -6,
    currentStationIndex: 3, // Gaya Jn
    progressPercent: 32,
    speedKmh: 115,
    lastUpdatedSecondsAgo: 31,
    stations: [
      {
        code: "HWH",
        name: "Howrah Jn",
        city: "Kolkata",
        lat: 22.5850,
        lon: 88.3426,
        scheduledArr: "START",
        scheduledDep: "16:50",
        actualArr: "START",
        actualDep: "16:50",
        distanceKm: 0,
        elevationM: 12,
        platform: "9",
        haltMin: 0
      },
      {
        code: "ASN",
        name: "Asansol Jn",
        city: "Asansol",
        lat: 23.6889,
        lon: 86.9661,
        scheduledArr: "18:57",
        scheduledDep: "19:00",
        actualArr: "18:55",
        actualDep: "19:00",
        distanceKm: 200,
        elevationM: 106,
        platform: "4",
        haltMin: 3
      },
      {
        code: "DHN",
        name: "Dhanbad Jn",
        city: "Dhanbad",
        lat: 23.7957,
        lon: 86.4304,
        scheduledArr: "19:55",
        scheduledDep: "20:00",
        actualArr: "19:50",
        actualDep: "19:58",
        distanceKm: 259,
        elevationM: 227,
        platform: "3",
        haltMin: 5
      },
      {
        code: "GAYA",
        name: "Gaya Jn",
        city: "Gaya",
        lat: 24.8028,
        lon: 85.0022,
        scheduledArr: "22:31",
        scheduledDep: "22:34",
        actualArr: "22:25",
        actualDep: "22:32",
        distanceKm: 459,
        elevationM: 113,
        platform: "1",
        haltMin: 3
      },
      {
        code: "DDU",
        name: "Pt DD Upadhyaya Jn",
        city: "Mughalsarai",
        lat: 25.2818,
        lon: 83.1186,
        scheduledArr: "00:45",
        scheduledDep: "00:55",
        actualArr: "00:42 (ETA)",
        actualDep: "00:52",
        distanceKm: 664,
        elevationM: 79,
        platform: "6",
        haltMin: 10
      },
      {
        code: "PRYJ",
        name: "Prayagraj Jn",
        city: "Prayagraj",
        lat: 25.4484,
        lon: 81.8336,
        scheduledArr: "02:43",
        scheduledDep: "02:45",
        actualArr: "02:39 (ETA)",
        actualDep: "02:45",
        distanceKm: 817,
        elevationM: 98,
        platform: "1",
        haltMin: 2
      },
      {
        code: "CNB",
        name: "Kanpur Central",
        city: "Kanpur",
        lat: 26.4538,
        lon: 80.3512,
        scheduledArr: "04:40",
        scheduledDep: "04:45",
        actualArr: "04:35 (ETA)",
        actualDep: "04:45",
        distanceKm: 1011,
        elevationM: 126,
        platform: "2",
        haltMin: 5
      },
      {
        code: "NDLS",
        name: "New Delhi",
        city: "Delhi",
        lat: 28.6429,
        lon: 77.2195,
        scheduledArr: "10:05",
        scheduledDep: "TERMINATES",
        actualArr: "09:59 (ETA)",
        actualDep: "TERMINATES",
        distanceKm: 1451,
        elevationM: 216,
        platform: "11",
        haltMin: 0
      }
    ],
    routeCoordinates: [
      [88.3426, 22.5850], // HWH
      [87.8000, 23.2000], // Barddhaman
      [86.9661, 23.6889], // ASN
      [86.4304, 23.7957], // DHN
      [86.0000, 24.1000], // Parasnath
      [85.5000, 24.4500], // Koderma
      [85.0022, 24.8028], // GAYA
      [84.1000, 24.9500], // Dehri on Sone
      [83.1186, 25.2818], // DDU
      [81.8336, 25.4484], // PRYJ
      [80.3512, 26.4538], // CNB
      [79.0000, 27.0000], // Etawah
      [78.0000, 27.7000], // Aligarh
      [77.2195, 28.6429]  // NDLS
    ],
    elevationProfile: [
      { distanceKm: 0, elevationMeters: 12, station: "HWH" },
      { distanceKm: 200, elevationMeters: 106, station: "ASN" },
      { distanceKm: 259, elevationMeters: 227, station: "DHN" },
      { distanceKm: 330, elevationMeters: 380, station: "Parasnath / Hills" },
      { distanceKm: 459, elevationMeters: 113, station: "GAYA" },
      { distanceKm: 664, elevationMeters: 79, station: "DDU" },
      { distanceKm: 817, elevationMeters: 98, station: "PRYJ" },
      { distanceKm: 1011, elevationMeters: 126, station: "CNB" },
      { distanceKm: 1451, elevationMeters: 216, station: "NDLS" }
    ]
  },
  {
    trainId: "20901",
    number: "20901",
    name: "Mumbai Central - Gandhinagar Vande Bharat",
    type: "Vande Bharat Express",
    origin: "Mumbai Central (MMCT)",
    destination: "Gandhinagar Capital (GNC)",
    totalDistanceKm: 522,
    avgSpeedKmh: 85,
    runsOn: ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat"],
    status: "ON_TIME",
    delayMinutes: 0,
    currentStationIndex: 2, // Surat
    progressPercent: 50,
    speedKmh: 130,
    lastUpdatedSecondsAgo: 5,
    stations: [
      {
        code: "MMCT",
        name: "Mumbai Central",
        city: "Mumbai",
        lat: 18.9696,
        lon: 72.8193,
        scheduledArr: "START",
        scheduledDep: "06:00",
        actualArr: "START",
        actualDep: "06:00",
        distanceKm: 0,
        elevationM: 7,
        platform: "5",
        haltMin: 0
      },
      {
        code: "BVI",
        name: "Borivali",
        city: "Mumbai",
        lat: 19.2288,
        lon: 72.8569,
        scheduledArr: "06:23",
        scheduledDep: "06:25",
        actualArr: "06:22",
        actualDep: "06:25",
        distanceKm: 30,
        elevationM: 14,
        platform: "6",
        haltMin: 2
      },
      {
        code: "ST",
        name: "Surat",
        city: "Surat",
        lat: 21.2049,
        lon: 72.8407,
        scheduledArr: "08:50",
        scheduledDep: "08:53",
        actualArr: "08:49",
        actualDep: "08:53",
        distanceKm: 263,
        elevationM: 18,
        platform: "1",
        haltMin: 3
      },
      {
        code: "BRC",
        name: "Vadodara Jn",
        city: "Vadodara",
        lat: 22.3107,
        lon: 73.1812,
        scheduledArr: "09:56",
        scheduledDep: "09:59",
        actualArr: "09:56 (ETA)",
        actualDep: "09:59",
        distanceKm: 392,
        elevationM: 36,
        platform: "2",
        haltMin: 3
      },
      {
        code: "ADI",
        name: "Ahmedabad Jn",
        city: "Ahmedabad",
        lat: 23.0225,
        lon: 72.5714,
        scheduledArr: "11:25",
        scheduledDep: "11:30",
        actualArr: "11:25 (ETA)",
        actualDep: "11:30",
        distanceKm: 491,
        elevationM: 53,
        platform: "1",
        haltMin: 5
      },
      {
        code: "GNC",
        name: "Gandhinagar Capital",
        city: "Gandhinagar",
        lat: 23.2233,
        lon: 72.6492,
        scheduledArr: "12:25",
        scheduledDep: "TERMINATES",
        actualArr: "12:25 (ETA)",
        actualDep: "TERMINATES",
        distanceKm: 522,
        elevationM: 81,
        platform: "1",
        haltMin: 0
      }
    ],
    routeCoordinates: [
      [72.8193, 18.9696], // MMCT
      [72.8569, 19.2288], // BVI
      [72.8000, 19.9000], // Palghar
      [72.9000, 20.3700], // Vapi
      [72.8407, 21.2049], // Surat
      [72.9500, 21.7000], // Bharuch
      [73.1812, 22.3107], // Vadodara
      [72.9500, 22.5500], // Anand
      [72.5714, 23.0225], // Ahmedabad
      [72.6492, 23.2233]  // Gandhinagar
    ],
    elevationProfile: [
      { distanceKm: 0, elevationMeters: 7, station: "MMCT" },
      { distanceKm: 30, elevationMeters: 14, station: "BVI" },
      { distanceKm: 160, elevationMeters: 12 },
      { distanceKm: 263, elevationMeters: 18, station: "ST" },
      { distanceKm: 392, elevationMeters: 36, station: "BRC" },
      { distanceKm: 491, elevationMeters: 53, station: "ADI" },
      { distanceKm: 522, elevationMeters: 81, station: "GNC (Highest)" }
    ]
  }
];
