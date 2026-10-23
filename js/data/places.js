/**
 * RailGaadi Geographic Points of Interest (POIs) along Railway Corridors
 * Bounded by distance from tracks, categorized for Travel Companion
 */

export const POI_DATA = [
  // Rivers & Lakes
  {
    id: "poi_riv_yamuna_pryj",
    name: "Yamuna & Ganga Sangam",
    category: "river",
    categoryLabel: "River Confluence",
    lat: 25.4299,
    lon: 81.8841,
    corridors: ["22436", "12301"],
    distanceFromRouteKm: 4.8,
    description: "Sacred confluence of Ganga, Yamuna, and mythical Saraswati. Visible south of Prayagraj junction.",
    source: "OpenStreetMap"
  },
  {
    id: "poi_riv_ganga_bsb",
    name: "Ganga River Ghats",
    category: "river",
    categoryLabel: "Holy River",
    lat: 25.3109,
    lon: 83.0104,
    corridors: ["22436"],
    distanceFromRouteKm: 3.2,
    description: "The timeless curved crescent of 84 ancient ghats along the holy Ganga in Varanasi.",
    source: "OpenStreetMap"
  },
  {
    id: "poi_riv_chambal",
    name: "Chambal River Ravines",
    category: "river",
    categoryLabel: "River & Gorge",
    lat: 26.6900,
    lon: 78.1000,
    corridors: ["12002"],
    distanceFromRouteKm: 1.1,
    description: "Pristine sanctuary waters famous for gharials and the rugged historical ravines of Chambal.",
    source: "OpenStreetMap"
  },
  {
    id: "poi_riv_narmada_brc",
    name: "Narmada River Crossing",
    category: "river",
    categoryLabel: "Major River",
    lat: 21.7050,
    lon: 72.9800,
    corridors: ["12952", "20901"],
    distanceFromRouteKm: 0.6,
    description: "Massive railway bridge crossing the sacred Narmada river connecting Ankleshwar to Bharuch.",
    source: "OpenStreetMap"
  },

  // Mountains & Ghats
  {
    id: "poi_mtn_aravalli",
    name: "Northern Aravalli Ridges",
    category: "mountain",
    categoryLabel: "Mountain Range",
    lat: 27.8000,
    lon: 76.8000,
    corridors: ["12952"],
    distanceFromRouteKm: 8.5,
    description: "One of the world's oldest folded mountain ranges flanking the Western Railway corridor.",
    source: "OpenTopography"
  },
  {
    id: "poi_mtn_vindhya",
    name: "Vindhyachal Escarpment",
    category: "mountain",
    categoryLabel: "Plateau & Hills",
    lat: 24.3000,
    lon: 77.9000,
    corridors: ["12002"],
    distanceFromRouteKm: 5.2,
    description: "Forested sandstone ridges forming the historic geographical dividing line between northern and southern India.",
    source: "OpenTopography"
  },
  {
    id: "poi_mtn_parasnath",
    name: "Parasnath Hill (Shikharji)",
    category: "mountain",
    categoryLabel: "Mountain Peak",
    lat: 23.9628,
    lon: 86.1311,
    corridors: ["12301"],
    distanceFromRouteKm: 7.4,
    description: "Highest mountain peak in Jharkhand state (1,365 m), revered as the most sacred Jain pilgrimage site.",
    source: "OpenTopography"
  },

  // Bridges & Engineering Marvels
  {
    id: "poi_bdg_malviya",
    name: "Malviya Double-Decker Bridge",
    category: "bridge",
    categoryLabel: "Historic Rail Bridge",
    lat: 25.3220,
    lon: 83.0380,
    corridors: ["22436"],
    distanceFromRouteKm: 0.2,
    description: "Iconic truss girder bridge across the Ganges with railway track on lower deck and road on upper.",
    source: "Indian Railways Archive"
  },
  {
    id: "poi_bdg_golden",
    name: "Historic Golden Bridge (Bharuch)",
    category: "bridge",
    categoryLabel: "Heritage Bridge",
    lat: 21.7000,
    lon: 72.9850,
    corridors: ["12952", "20901"],
    distanceFromRouteKm: 0.9,
    description: "Engineering marvel built in 1881 across the vast Narmada river to withstand seasonal torrents.",
    source: "OpenStreetMap"
  },
  {
    id: "poi_bdg_naini",
    name: "Old Naini Rail-cum-Road Bridge",
    category: "bridge",
    categoryLabel: "Historic Truss Bridge",
    lat: 25.4240,
    lon: 81.8600,
    corridors: ["22436", "12301"],
    distanceFromRouteKm: 0.3,
    description: "Grand multi-span steel bridge constructed in 1865 spanning the Yamuna into Prayagraj.",
    source: "OpenStreetMap"
  },

  // Monuments & Heritage
  {
    id: "poi_mon_taj",
    name: "Taj Mahal",
    category: "monument",
    categoryLabel: "UNESCO World Heritage",
    lat: 27.1751,
    lon: 78.0421,
    corridors: ["12002"],
    distanceFromRouteKm: 4.1,
    description: "White marble mausoleum commissioned by Shah Jahan in 1631 on the southern bank of the Yamuna River.",
    source: "UNESCO"
  },
  {
    id: "poi_mon_gwalior_fort",
    name: "Gwalior Fort Citadel",
    category: "monument",
    categoryLabel: "Hilltop Citadel",
    lat: 26.2307,
    lon: 78.1695,
    corridors: ["12002"],
    distanceFromRouteKm: 2.8,
    description: "Majestic 8th-century fortress perched 300 feet above Gwalior plain, prominent from the train window.",
    source: "Archaeological Survey of India"
  },
  {
    id: "poi_mon_sarnath",
    name: "Dhamek Stupa, Sarnath",
    category: "monument",
    categoryLabel: "Buddhist Monument",
    lat: 25.3811,
    lon: 83.0244,
    corridors: ["22436"],
    distanceFromRouteKm: 6.2,
    description: "Solid cylindrical brick tower marking the spot where Lord Buddha preached his first sermon.",
    source: "Archaeological Survey of India"
  },

  // Cities & Junctions
  {
    id: "poi_city_kanpur",
    name: "Kanpur Central Rail Hub",
    category: "city",
    categoryLabel: "Junction Hub",
    lat: 26.4538,
    lon: 80.3512,
    corridors: ["22436", "12301"],
    distanceFromRouteKm: 0.0,
    description: "One of the busiest railway stations in Asia handling over 300 passenger trains daily.",
    source: "IRCTC"
  },
  {
    id: "poi_city_surat",
    name: "Surat Diamond City",
    category: "city",
    categoryLabel: "Commercial Metropolis",
    lat: 21.2049,
    lon: 72.8407,
    corridors: ["12952", "20901"],
    distanceFromRouteKm: 0.0,
    description: "Global capital for diamond cutting and polishing situated on the banks of the Tapi River.",
    source: "OpenStreetMap"
  }
];
