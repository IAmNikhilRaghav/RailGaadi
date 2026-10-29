/**
 * RailGaadi Centralized Environment Configuration
 * Reads from Vite environment (import.meta.env) with production-ready live fallbacks
 * Also checks localStorage overrides if customized by user
 */

function getEnv(key, defaultVal = "") {
  try {
    if (typeof localStorage !== "undefined") {
      const custom = localStorage.getItem(`railgaadi_env_${key}`);
      if (custom) return custom;
    }
  } catch {}

  if (typeof import.meta !== "undefined" && import.meta.env?.[key]) {
    return import.meta.env[key];
  }

  return defaultVal;
}

export const ENV = {
  MAPTILER_API_KEY: getEnv(
    "VITE_MAPTILER_API_KEY",
    "bE1cKVXeMzq0rVuxxZ8n"
  ),

  OPENWEATHER_API_KEY: getEnv(
    "VITE_OPENWEATHER_API_KEY",
    "865aa76503e675534c7a1e134b457335"
  ),

  OPENTOPOGRAPHY_API_KEY: getEnv(
    "VITE_OPENTOPOGRAPHY_API_KEY",
    "0e9747b1179065e9bfd50ddc0650ac41"
  ),

  RAILRADAR_API_KEY: getEnv(
    "VITE_RAILRADAR_API_KEY",
    "rg_efcbe8da364f42cea8ee306a195aae2e"
  )
};
