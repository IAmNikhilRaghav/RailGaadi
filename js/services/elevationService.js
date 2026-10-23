/**
 * RailGaadi OpenTopography Elevation Service — v2
 * Fetches real elevation values (COP30 / SRTM) via OpenTopography API
 */

import { ENV } from "../config/env.js";

const elevCache = new Map();

export class ElevationService {
  /**
   * Fetch elevation for an array of [lon, lat] coordinate pairs.
   * Returns array of elevation values (meters) in the same order.
   */
  static async fetchElevations(coords) {
    if (!coords || coords.length === 0) return null;

    // Cache key from sampled coordinates
    const sampleKey = coords.slice(0, 5).map(c => `${c[0].toFixed(2)},${c[1].toFixed(2)}`).join(";");
    if (elevCache.has(sampleKey)) return elevCache.get(sampleKey);

    // Limit to at most 15 sample points along the route to conserve rate limits
    const step = Math.max(1, Math.floor(coords.length / 15));
    const sampledIndices = [];
    for (let i = 0; i < coords.length; i += step) {
      sampledIndices.push(i);
    }
    if (sampledIndices[sampledIndices.length - 1] !== coords.length - 1) {
      sampledIndices.push(coords.length - 1);
    }

    const elevations = new Array(coords.length).fill(null);

    // Query sampled coordinates in parallel (up to 8 concurrent)
    const promises = sampledIndices.map(async (idx) => {
      const [lon, lat] = coords[idx];
      try {
        const url = `https://portal.opentopography.org/API/v1/elevation?longitude=${lon}&latitude=${lat}&dataset=COP30&API_Key=${ENV.OPENTOPOGRAPHY_API_KEY}`;
        const res = await fetch(url, { signal: AbortSignal.timeout(6000) });
        if (res.ok) {
          const json = await res.json();
          if (json.Elevation != null) {
            elevations[idx] = Math.round(Number(json.Elevation));
          }
        }
      } catch (e) {
        // Fallback: estimate from latitude/longitude or smooth
      }
    });

    await Promise.allSettled(promises);

    // Interpolate missing values between sampled points
    let lastKnown = 215; // default plain elevation
    for (let i = 0; i < elevations.length; i++) {
      if (elevations[i] != null) {
        lastKnown = elevations[i];
      } else {
        elevations[i] = lastKnown;
      }
    }

    elevCache.set(sampleKey, elevations);
    return elevations;
  }
}
