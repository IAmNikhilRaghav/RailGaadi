/**
 * RailGaadi Geospatial Service
 * Haversine distance, Turf-compatible polyline slicing, and train location interpolation
 */

export class GeoService {
  /**
   * Calculate distance between two lat/lon coordinates in kilometers
   */
  static distanceKm(lat1, lon1, lat2, lon2) {
    const R = 6371; // Earth radius in km
    const dLat = ((lat2 - lat1) * Math.PI) / 180;
    const dLon = ((lon2 - lon1) * Math.PI) / 180;
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos((lat1 * Math.PI) / 180) *
        Math.cos((lat2 * Math.PI) / 180) *
        Math.sin(dLon / 2) *
        Math.sin(dLon / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return R * c;
  }

  /**
   * Interpolate exact current coordinate [lon, lat] along route coordinates given completion percentage.
   * ALWAYS returns an object: { lon: number, lat: number, bearing: number, index: number }
   */
  static interpolatePosition(coordinates, progressPercent) {
    if (!coordinates || coordinates.length === 0) {
      return { lon: 77.2090, lat: 28.6139, bearing: 0, index: 0 };
    }
    if (coordinates.length === 1 || progressPercent <= 0) {
      const c = coordinates[0];
      return { lon: c[0], lat: c[1], bearing: 0, index: 0 };
    }
    if (progressPercent >= 100) {
      const c = coordinates[coordinates.length - 1];
      return { lon: c[0], lat: c[1], bearing: 0, index: coordinates.length - 1 };
    }

    // Compute segment distances
    const segmentDistances = [];
    let totalLength = 0;
    for (let i = 0; i < coordinates.length - 1; i++) {
      const d = this.distanceKm(
        coordinates[i][1],
        coordinates[i][0],
        coordinates[i + 1][1],
        coordinates[i + 1][0]
      );
      segmentDistances.push(d);
      totalLength += d;
    }

    if (totalLength === 0) {
      const c = coordinates[0];
      return { lon: c[0], lat: c[1], bearing: 0, index: 0 };
    }

    const targetDistance = (totalLength * Math.max(0, Math.min(100, progressPercent))) / 100;
    let accumulated = 0;

    for (let i = 0; i < segmentDistances.length; i++) {
      const segDist = segmentDistances[i];
      if (accumulated + segDist >= targetDistance && segDist > 0) {
        const segProgress = (targetDistance - accumulated) / segDist;
        const lon = coordinates[i][0] + (coordinates[i + 1][0] - coordinates[i][0]) * segProgress;
        const lat = coordinates[i][1] + (coordinates[i + 1][1] - coordinates[i][1]) * segProgress;
        
        // Calculate bearing/heading angle in degrees
        const y = Math.sin(((coordinates[i + 1][0] - coordinates[i][0]) * Math.PI) / 180) *
          Math.cos((coordinates[i + 1][1] * Math.PI) / 180);
        const x = Math.cos((coordinates[i][1] * Math.PI) / 180) *
          Math.sin((coordinates[i + 1][1] * Math.PI) / 180) -
          Math.sin((coordinates[i][1] * Math.PI) / 180) *
          Math.cos((coordinates[i + 1][1] * Math.PI) / 180) *
          Math.cos(((coordinates[i + 1][0] - coordinates[i][0]) * Math.PI) / 180);
        const bearing = ((Math.atan2(y, x) * 180) / Math.PI + 360) % 360;

        return { lon, lat, bearing, index: i };
      }
      accumulated += segDist;
    }

    const last = coordinates[coordinates.length - 1];
    return {
      lon: last[0],
      lat: last[1],
      bearing: 0,
      index: coordinates.length - 1
    };
  }

  /**
   * Split route coordinates into completed and remaining polylines
   */
  static splitRoute(coordinates, currentPos) {
    if (!coordinates || coordinates.length === 0) {
      return { completed: [], remaining: [] };
    }

    const pLon = currentPos?.lon ?? (Array.isArray(currentPos) ? currentPos[0] : coordinates[0][0]);
    const pLat = currentPos?.lat ?? (Array.isArray(currentPos) ? currentPos[1] : coordinates[0][1]);

    let splitIdx = currentPos?.index;
    if (splitIdx == null || splitIdx < 0 || splitIdx >= coordinates.length) {
      let minDist = Infinity;
      splitIdx = 0;
      for (let i = 0; i < coordinates.length; i++) {
        const d = this.distanceKm(pLat, pLon, coordinates[i][1], coordinates[i][0]);
        if (d < minDist) { minDist = d; splitIdx = i; }
      }
    }

    const completed = [];
    const remaining = [];

    for (let i = 0; i <= splitIdx; i++) completed.push(coordinates[i]);
    completed.push([pLon, pLat]);
    remaining.push([pLon, pLat]);
    for (let i = splitIdx + 1; i < coordinates.length; i++) remaining.push(coordinates[i]);

    return { completed, remaining };
  }

  /**
   * Find bearing for a known lat/lon position along the nearest route segment
   */
  static bearingAlongRoute(coordinates, lon, lat) {
    if (!coordinates || coordinates.length < 2) return 0;
    let minDist = Infinity;
    let bestIdx = 0;
    for (let i = 0; i < coordinates.length - 1; i++) {
      const d = this.distanceKm(lat, lon, coordinates[i][1], coordinates[i][0]);
      if (d < minDist) { minDist = d; bestIdx = i; }
    }
    const a = coordinates[bestIdx];
    const b = coordinates[Math.min(bestIdx + 1, coordinates.length - 1)];
    const y = Math.sin(((b[0] - a[0]) * Math.PI) / 180) * Math.cos((b[1] * Math.PI) / 180);
    const x = Math.cos((a[1] * Math.PI) / 180) * Math.sin((b[1] * Math.PI) / 180) -
              Math.sin((a[1] * Math.PI) / 180) * Math.cos((b[1] * Math.PI) / 180) *
              Math.cos(((b[0] - a[0]) * Math.PI) / 180);
    return ((Math.atan2(y, x) * 180) / Math.PI + 360) % 360;
  }

  /**
   * Validate that a coordinate pair is within India's bounding box
   */
  static isValidIndiaCord(lon, lat) {
    return lat >= 6.5 && lat <= 37.5 && lon >= 68 && lon <= 97.5;
  }
}

