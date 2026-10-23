/**
 * RailGaadi Centralized Environment Configuration
 * Reads from Vite environment (import.meta.env) with fallbacks
 */

export const ENV = {
  MAPTILER_API_KEY:
    (typeof import.meta !== "undefined" && import.meta.env?.VITE_MAPTILER_API_KEY) ||
    "",

  OPENWEATHER_API_KEY:
    (typeof import.meta !== "undefined" && import.meta.env?.VITE_OPENWEATHER_API_KEY) ||
    "",

  OPENTOPOGRAPHY_API_KEY:
    (typeof import.meta !== "undefined" && import.meta.env?.VITE_OPENTOPOGRAPHY_API_KEY) ||
    "",

  RAILRADAR_API_KEY:
    (typeof import.meta !== "undefined" && import.meta.env?.VITE_RAILRADAR_API_KEY) ||
    ""
};

