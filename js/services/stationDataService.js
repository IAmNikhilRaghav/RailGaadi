/**
 * RailGaadi Station & Catalog Data Service
 * Comprehensive Station Geo-Coordinates Directory (8,690+ Stations)
 * All-India Train Catalog (5,200+ Trains)
 * Enables live geographic enrichment, route geometry synthesis, and resilient local search
 */

const CORE_STATIONS = {"NDLS":[77.22,28.6423,"NEW DELHI"],"HWH":[88.341,22.5841,"HOWRAH JN"],"BME":[71.3971,25.7445,"BARMER"],"BVI":[72.8564,19.2287,"BORIVALI"],"BSB":[82.9865,25.3273,"VARANASI JN"],"CNB":[80.351,26.4542,"KANPUR CENTRAL"],"PNBE":[85.1368,25.6026,"PATNA JN"],"ADI":[72.6015,23.0255,"AHMEDABAD JN"],"HYB":[78.4674,17.3934,"HYDERABAD DECCAN"],"SBC":[77.5681,12.9776,"BANGALORE CITY JN"],"MAS":[80.2749,13.0848,"CHENNAI CENTRAL"],"GHY":[91.7519,26.1826,"GUWAHATI"],"JAT":[74.8801,32.707,"JAMMU TAWI"],"KOTA":[75.8805,25.2236,"KOTA JN"],"RTM":[75.0508,23.3404,"RATLAM JN"],"BPL":[77.4131,23.2669,"BHOPAL JN"],"GAYA":[84.9993,24.804,"GAYA JN"],"ASN":[86.9752,23.6914,"ASANSOL JN"],"DHN":[86.429,23.791,"DHANBAD JN"],"PNME":[86.0379,23.988,"PARASNATH"],"LKO":[80.9253,26.8307,"LUCKNOW"],"MB":[78.7658,28.8316,"MORADABAD"],"BE":[79.4108,28.3372,"BAREILLY"],"ALD":[81.8288,25.4462,"ALLAHABAD JN"],"GKP":[83.3815,26.7593,"GORAKHPUR JN"],"VAPI":[72.9091,20.3743,"VAPI"],"ST":[72.8408,21.2066,"SURAT"],"BRC":[73.1811,22.3108,"VADODARA JN"],"ANND":[72.9657,22.5613,"ANAND JN"],"PNVL":[73.1218,18.9909,"PANVEL"],"KYN":[73.1297,19.2347,"KALYAN JN"],"TNA":[72.9754,19.1858,"THANE"],"DR":[72.843,19.0172,"MUMBAI DADAR CENTRAL"],"PUNE":[73.8731,18.5294,"PUNE JN"],"DD":[74.5786,18.4635,"DAUND JN"],"SUR":[75.8934,17.6645,"SOLAPUR JN"],"NGP":[79.089,21.1536,"NAGPUR"],"BZA":[80.6186,16.5183,"VIJAYAWADA JN"],"GNT":[80.442,16.3008,"GUNTUR JN"],"TPTY":[79.419,13.6276,"TIRUPATI MAIN"],"RU":[79.5063,13.6363,"RENIGUNTA JN"],"MDU":[78.1103,9.9199,"MADURAI JN"],"TPJ":[78.6854,10.7941,"TIRUCHIRAPPALLI"],"CBE":[76.9663,10.9976,"COIMBATORE JN"],"CLT":[75.7804,11.2462,"KOZHIKODE"],"CAN":[75.3683,11.8707,"KANNUR"],"TVC":[76.9512,8.4867,"TRIVANDRUM CENTRAL"],"ERS":[76.2907,9.9695,"ERNAKULAM JN"],"AWY":[76.3565,10.1082,"ALUVA"],"TCR":[76.2079,10.5148,"THRISUR"],"SRR":[76.2722,10.7589,"SHORANUR JN"],"MAQ":[74.8441,12.8623,"MANGALORE CNTL"],"UBL":[75.1481,15.3503,"HUBLI JN"],"VSKP":[83.2895,17.7216,"VISHAKAPATNAM"],"BAM":[84.7967,19.2961,"BRAHMAPUR"],"KUR":[85.7082,20.1531,"KHURDA ROAD JN"],"BBS":[85.8426,20.2654,"BHUBANESWAR"],"CTC":[85.9016,20.4666,"CUTTACK"],"RPR":[84.3052,25.5654,"RAGHUNATHPUR"],"BSP":[82.1676,22.0575,"BILASPUR JN"],"JSG":[84.0174,21.8623,"JHARSUGUDA JN"],"ROU":[84.8631,22.2284,"ROURKELA"],"TATA":[86.2006,22.769,"TATANAGAR JN"],"KGP":[87.3285,22.3414,"KHARAGPUR JN"],"SDAH":[88.3747,22.5668,"KOLKATA SEALDAH"],"KOAA":[88.3852,22.6012,"KOLKATA CHITPUR"],"MLDT":[88.1303,25.0157,"MALDA TOWN"],"NJP":[88.4434,26.683,"NEW JALPAIGURI"],"APDJ":[89.534,26.5233,"ALIPUR DUAR JN"],"RNY":[91.6059,26.4479,"RANGIYA JN"],"NBQ":[90.5368,26.4758,"NEW BONGAIGAON"],"DBRG":[94.9353,27.4636,"DIBRUGARH"],"JUC":[75.5915,31.3317,"JALANDHAR CITY"],"ASR":[74.858,31.6315,"AMRITSAR JN"],"CDG":[76.8221,30.702,"CHANDIGARH"],"UMB":[76.827,30.3389,"AMBALA CANT JN"],"KLK":[76.9318,30.8377,"Kalka"],"HW":[78.1549,29.9478,"HARIDWAR JN"],"DDN":[78.0336,30.314,"DEHRADUN"],"KGM":[79.5463,29.2666,"KATHGODAM"],"RE":[76.6094,28.2028,"REWARI"],"AWR":[76.6213,27.5593,"ALWAR"],"BKI":[76.5667,27.0393,"BANDIKUI JN"],"JP":[75.7869,26.9202,"JAIPUR"],"AII":[74.6375,26.4566,"AJMER JN"],"FA":[73.235,25.2358,"FALNA"],"BKN":[73.3166,28.0141,"BIKANER JN"],"JU":[73.0232,26.2838,"JODHPUR JN"],"MTJ":[77.6731,27.4801,"MATHURA JN"],"AGC":[77.9902,27.158,"AGRA CANTT"],"GWL":[78.1823,26.2165,"GWALIOR JN"],"JHS":[78.553,25.4436,"JHANSI JN"],"BINA":[78.1832,24.171,"BINA JN"],"BAP":[74.6617,19.6196,"BELAPUR"],"KPG":[74.5033,19.9029,"KOPARGAON"],"MMR":[74.4383,20.2499,"MANMAD JN"],"JL":[75.5629,21.0181,"JALGAON JN"],"BSL":[75.7887,21.0473,"BHUSAVAL JN"],"AK":[77.0055,20.7231,"AKOLA JN"],"BD":[77.7323,20.8568,"BADNERA JN"],"WR":[78.5942,20.7328,"WARDHA JN"],"BPQ":[79.3486,19.849,"BALHARSHAH"],"KZJ":[79.511,17.9742,"KAZIPET JN"],"SC":[78.5015,17.4331,"SECUNDERABAD JN"],"BMT":[78.4586,17.4389,"BEGAMPET"],"SNF":[78.4359,17.4616,"SANAT NAGAR"],"OGL":[80.0574,15.4981,"ONGOLE"],"NLR":[79.9894,14.4606,"NELLORE"],"GDR":[79.8451,14.1482,"GUDUR JN"],"KPD":[79.1353,12.9727,"KATPADI JN"],"JTJ":[78.5778,12.5609,"JOLARPETTAI"],"SA":[78.1134,11.6717,"SALEM JN"],"ED":[77.7259,11.3277,"ERODE JN"],"TUP":[77.3412,11.1089,"TIRUPPUR"],"TEN":[77.708,8.7364,"TIRUNELVELI"],"NCJ":[77.4435,8.1738,"NAGERCOIL JN"],"QLN":[76.5968,8.8866,"KOLLAM JN"],"ALLP":[76.3225,9.4838,"ALLEPPEY"],"KTYM":[76.5322,9.5942,"KOTTAYAM"],"ERN":[76.2861,9.9916,"ERNAKULAM TOWN"],"PGT":[76.6419,10.8021,"PALAKKAD"],"TIR":[75.9219,10.9178,"TIRUR"],"TLY":[75.493,11.7531,"THALASSERY"],"MAO":[73.9707,15.2679,"MADGAON"],"KRMI":[73.9247,15.4902,"KARMALI"],"SWV":[73.7853,15.8665,"SAWANTWADI ROAD"],"RN":[73.3569,17.0028,"RATNAGIRI"],"ROHA":[73.1236,18.4465,"ROHA"],"CHI":[73.5224,17.5425,"CHIPLUN"],"VR":[72.8117,19.4544,"VIRAR"],"BYR":[72.853,19.3118,"BHAYANDAR"],"BSR":[72.8322,19.3824,"VASAI ROAD"],"NIG":[72.846,19.3509,"NAIGAON"],"MIRA":[72.8561,19.2803,"MIRA ROAD"],"DAH":[74.6742,19.2419,"DEHARE"],"KILE":[72.8521,19.2046,"KANDIVLI"],"GMN":[72.8494,19.1644,"GOREGAON"],"JOS":[72.8491,19.1366,"JOGESHWARI"],"ADH":[72.8469,19.1174,"ANDHERI"],"VLP":[72.8436,19.0993,"VILE PARLE"],"STC":[72.8413,19.082,"SANTA CRUZ"],"BA":[72.8405,19.0545,"BANDRA"],"MM":[72.8469,19.0405,"MUMBAI MAHIM JN"],"MRU":[72.8471,19.0286,"MUMBAI MATUNGA ROAD"],"EPR":[72.8354,19.0069,"Mumbai Elphinstone Road"],"DDR":[72.8435,19.0197,"MUMBAI DADAR WEST"],"PL":[72.8296,18.9949,"Mumbai Lower Parel"],"MX":[72.8239,18.9821,"Mumbai Mahalakshmi"],"MSD":[72.8386,18.9524,"BOMBAY MASJID"],"CHG":[72.8327,18.9868,"MUMBAI CHINCHPOKLI"]};

class StationDataService {
  constructor() {
    this.stationsMap = new Map();
    this.trainsCatalog = null;
    this.isStationsLoading = false;
    this.isStationsLoaded = false;
    this.isTrainsLoading = false;

    // Pre-populate with core stations for synchronous zero-latency lookups
    Object.entries(CORE_STATIONS).forEach(([code, data]) => {
      this.stationsMap.set(code.toUpperCase(), {
        code: code.toUpperCase(),
        lon: data[0],
        lat: data[1],
        name: data[2]
      });
    });

    // Initiate non-blocking full directory load
    this.loadAllStations();
  }

  /**
   * Asynchronously loads all 8,690+ Indian railway stations from public/data/stations.json
   */
  async loadAllStations() {
    if (this.isStationsLoaded || this.isStationsLoading) return;
    if (typeof window === "undefined") return;
    this.isStationsLoading = true;
    try {
      const res = await fetch('/data/stations.json');
      if (res.ok) {
        const json = await res.json();
        Object.entries(json).forEach(([code, data]) => {
          this.stationsMap.set(code.toUpperCase(), {
            code: code.toUpperCase(),
            lon: data[0],
            lat: data[1],
            name: data[2]
          });
        });
        this.isStationsLoaded = true;
      }
    } catch (e) {
      console.warn('[StationDataService] Error loading full stations directory:', e);
    } finally {
      this.isStationsLoading = false;
    }
  }

  /**
   * Asynchronously loads the 5,200+ Indian Trains catalog
   */
  async loadTrainsCatalog() {
    if (this.trainsCatalog || this.isTrainsLoading) return this.trainsCatalog || [];
    if (typeof window === "undefined") return [];
    this.isTrainsLoading = true;
    try {
      const res = await fetch('/data/trains_index.json');
      if (res.ok) {
        this.trainsCatalog = await res.json();
      }
    } catch (e) {
      console.warn('[StationDataService] Error loading trains catalog:', e);
      this.trainsCatalog = [];
    } finally {
      this.isTrainsLoading = false;
    }
    return this.trainsCatalog || [];
  }

  /**
   * Synchronous lookup for station coordinates and info
   */
  getStation(codeOrName) {
    if (!codeOrName) return null;
    const clean = String(codeOrName).trim().toUpperCase();
    if (this.stationsMap.has(clean)) {
      return this.stationsMap.get(clean);
    }

    // Try finding by name if not matching code
    for (const stn of this.stationsMap.values()) {
      if (stn.name.toUpperCase() === clean || stn.name.toUpperCase().startsWith(clean)) {
        return stn;
      }
    }
    return null;
  }

  /**
   * Enriches route stations returned from API (which may lack lat/lon)
   * with exact coordinates from the station directory.
   */
  enrichRouteStations(stations) {
    if (!Array.isArray(stations)) return [];
    return stations.map((stn, idx) => {
      const code = (stn.code || stn.stationCode || stn.stationId || '').trim().toUpperCase();
      const match = this.getStation(code) || this.getStation(stn.name);

      const lat = stn.latitude || stn.lat || (match ? match.lat : null);
      const lon = stn.longitude || stn.lon || stn.lng || (match ? match.lon : null);

      return {
        ...stn,
        latitude: lat,
        longitude: lon,
        name: stn.name || match?.name || code
      };
    });
  }

  /**
   * Returns all trains in the catalog (used for between-station filtering)
   */
  async getAllTrains() {
    return this.loadTrainsCatalog();
  }

  /**
   * Searches local trains catalog (covers mail, express, passenger, EMU/MEMU locals)
   */
  async searchCatalog(query, limit = 15) {
    const q = String(query || '').trim().toLowerCase();
    if (q.length < 2) return [];

    const catalog = await this.loadTrainsCatalog();
    if (!catalog || catalog.length === 0) return [];

    const matches = [];
    for (const t of catalog) {
      const matchNumber = t.number && t.number.toLowerCase().includes(q);
      const matchName   = t.name && t.name.toLowerCase().includes(q);
      const matchOrigin = t.origin && t.origin.toLowerCase().includes(q);
      const matchDest   = t.destination && t.destination.toLowerCase().includes(q);

      if (matchNumber || matchName || matchOrigin || matchDest) {
        matches.push(t);
        if (matches.length >= limit) break;
      }
    }

    return matches;
  }
}

export const stationDataService = new StationDataService();
