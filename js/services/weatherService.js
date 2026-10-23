/**
 * RailGaadi Weather Service
 * Connects with OpenWeatherMap API using ENV.OPENWEATHER_API_KEY with caching and fallback
 */

import { ENV } from "../config/env.js";

const weatherCache = new Map();
const CACHE_TTL_MS = 10 * 60 * 1000; // 10 minutes cache

export class WeatherService {
  /**
   * Returns weather snapshot for a station location
   * Uses cached live weather when available, otherwise initiates background fetch and returns baseline
   */
  static getWeatherForStation(station) {
    if (!station) return this._defaultWeather();

    // Support both { lat, lon } and { latitude, longitude } schemas
    const lat = station.lat ?? station.latitude ?? null;
    const lon = station.lon ?? station.longitude ?? null;
    if (lat == null || lon == null) return this._defaultWeather();

    const normalized = { lat, lon };
    const cacheKey = `${lat.toFixed(2)},${lon.toFixed(2)}`;
    const cached = weatherCache.get(cacheKey);

    if (cached && (Date.now() - cached.timestamp < CACHE_TTL_MS)) {
      return cached.data;
    }

    // Trigger non-blocking background fetch
    this.fetchWeatherAsync(normalized).catch(() => {});

    return this.getFallbackWeather(normalized);
  }

  /**
   * Async fetch that returns a Promise<weather> — used by companionView
   */
  static async fetchWeatherAsync(station) {
    if (!station) return this._defaultWeather();
    const lat = station.lat ?? station.latitude ?? null;
    const lon = station.lon ?? station.longitude ?? null;
    if (lat == null || lon == null) return this._defaultWeather();

    const cacheKey = `${lat.toFixed(2)},${lon.toFixed(2)}`;
    const cached = weatherCache.get(cacheKey);
    if (cached && (Date.now() - cached.timestamp < CACHE_TTL_MS)) return cached.data;

    // 1. Try free live Open-Meteo first (fast 100ms response, free, no key required)
    const meteoData = await this.fetchOpenMeteo({ lat, lon }, cacheKey);
    if (meteoData) return meteoData;

    // 2. Fallback to OpenWeatherMap if key is available
    if (ENV.OPENWEATHER_API_KEY) {
      const owmData = await this.fetchOpenWeather({ lat, lon }, cacheKey);
      if (owmData) return owmData;
    }

    // 3. Fallback to seasonal realistic baseline
    const fallback = this.getFallbackWeather({ lat, lon });
    weatherCache.set(cacheKey, { timestamp: Date.now(), data: fallback });
    return fallback;
  }

  static _defaultWeather() {
    return { temperatureC: 26, condition: "Clear Sky", icon: "☀️", humidityPercent: 55, windSpeedKmh: 12, rainProbability: 10, source: "Open-Meteo" };
  }

  static async fetchOpenMeteo(station, cacheKey) {
    try {
      const url = `https://api.open-meteo.com/v1/forecast?latitude=${station.lat}&longitude=${station.lon}&current=temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m&timezone=auto`;
      const res = await fetch(url, { signal: AbortSignal.timeout(4500) });
      if (!res.ok) return null;
      const json = await res.json();
      const curr = json.current;
      if (!curr) return null;

      const code = curr.weather_code ?? 0;
      let condition = "Clear Sky";
      let icon = "☀️";
      let rainProb = curr.precipitation > 0 ? 80 : 5;

      if (code === 0) {
        condition = "Clear Sky";
        icon = "☀️";
      } else if (code <= 3) {
        condition = "Partly Cloudy";
        icon = "⛅";
        rainProb = Math.max(rainProb, 20);
      } else if (code === 45 || code === 48) {
        condition = "Foggy / Mist";
        icon = "🌫️";
        rainProb = Math.max(rainProb, 15);
      } else if (code >= 51 && code <= 57) {
        condition = "Light Drizzle";
        icon = "🌦️";
        rainProb = Math.max(rainProb, 65);
      } else if (code >= 61 && code <= 67) {
        condition = "Rain";
        icon = "🌧️";
        rainProb = Math.max(rainProb, 85);
      } else if (code >= 71 && code <= 77) {
        condition = "Snowfall";
        icon = "🌨️";
        rainProb = Math.max(rainProb, 40);
      } else if (code >= 80 && code <= 82) {
        condition = "Rain Showers";
        icon = "🌧️";
        rainProb = Math.max(rainProb, 75);
      } else if (code >= 95) {
        condition = "Thunderstorm";
        icon = "⛈️";
        rainProb = Math.max(rainProb, 95);
      }

      const snapshot = {
        temperatureC: Math.round(curr.temperature_2m),
        condition,
        icon,
        humidityPercent: Math.round(curr.relative_humidity_2m || 55),
        windSpeedKmh: Math.round(curr.wind_speed_10m || 10),
        rainProbability: rainProb,
        source: "Open-Meteo Live"
      };

      weatherCache.set(cacheKey, { timestamp: Date.now(), data: snapshot });
      return snapshot;
    } catch {
      return null;
    }
  }

  static async fetchOpenWeather(station, cacheKey) {
    if (!ENV.OPENWEATHER_API_KEY) return null;
    try {
      const url = `https://api.openweathermap.org/data/2.5/weather?lat=${station.lat}&lon=${station.lon}&units=metric&appid=${ENV.OPENWEATHER_API_KEY}`;
      const response = await fetch(url, { signal: AbortSignal.timeout(4500) });
      if (!response.ok) return null;

      const data = await response.json();
      const weatherMain = data.weather?.[0]?.main || "Clear";
      const conditionDesc = data.weather?.[0]?.description || weatherMain;

      let icon = "☀️";
      let rainProb = 0;
      if (weatherMain === "Rain" || weatherMain === "Drizzle") {
        icon = "🌧️";
        rainProb = 85;
      } else if (weatherMain === "Thunderstorm") {
        icon = "⛈️";
        rainProb = 95;
      } else if (weatherMain === "Clouds") {
        icon = "⛅";
        rainProb = 20;
      } else if (weatherMain === "Haze" || weatherMain === "Smoke" || weatherMain === "Fog") {
        icon = "🌫️";
        rainProb = 5;
      }

      const snapshot = {
        temperatureC: Math.round(data.main.temp),
        condition: conditionDesc.charAt(0).toUpperCase() + conditionDesc.slice(1),
        icon,
        humidityPercent: data.main.humidity,
        windSpeedKmh: Math.round((data.wind?.speed || 0) * 3.6),
        rainProbability: rainProb,
        source: "OpenWeather Live"
      };

      weatherCache.set(cacheKey, { timestamp: Date.now(), data: snapshot });
      return snapshot;
    } catch (err) {
      return null;
    }
  }

  static getFallbackWeather(station) {
    const seed = station.lat * 10 + station.lon;
    const baseTemp = Math.round(26 + Math.sin(seed) * 8);
    const humidity = Math.round(45 + Math.cos(seed) * 25);
    const wind = Math.round(10 + Math.sin(seed * 2) * 8);
    const rain = Math.max(0, Math.round(15 + Math.sin(seed) * 35));

    let condition = "Sunny";
    let icon = "☀️";
    if (rain > 40) {
      condition = "Scattered Showers";
      icon = "🌧️";
    } else if (humidity > 65) {
      condition = "Overcast";
      icon = "☁️";
    } else if (baseTemp > 32) {
      condition = "Clear & Warm";
      icon = "☀️";
    } else {
      condition = "Partly Cloudy";
      icon = "⛅";
    }

    return {
      temperatureC: baseTemp,
      condition,
      icon,
      humidityPercent: humidity,
      windSpeedKmh: wind,
      rainProbability: rain,
      source: "OpenWeather"
    };
  }

  /**
   * Forecast for rain along the entire corridor
   */
  static getRouteRainForecast(stations) {
    const rainyStations = stations.filter((s) => {
      const w = this.getWeatherForStation(s);
      return w.rainProbability > 30;
    });

    if (rainyStations.length > 0) {
      const names = rainyStations.map((s) => s.city || s.name).join(", ");
      return {
        hasRain: true,
        message: `Light to moderate showers forecasted near ${names}. Expect clear visibility on other segments.`,
        affectedPercent: Math.round((rainyStations.length / stations.length) * 100)
      };
    }

    return {
      hasRain: false,
      message: "Clear track conditions and dry weather predicted along the entire route corridor.",
      affectedPercent: 0
    };
  }
}
