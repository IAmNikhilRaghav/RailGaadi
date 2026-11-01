/**
 * RailGaadi Notification Service — Phase 2
 * Browser push notification support for delay alerts and station arrivals
 */

import { store } from "../state/store.js";

class NotificationService {
  constructor() {
    this.permission = typeof window !== "undefined" && typeof window.Notification !== "undefined"
      ? window.Notification.permission
      : "default";
    this.lastNotifiedDelay = null;
    this.lastNotifiedStation = null;
  }

  get isSupported() {
    return typeof window !== "undefined" && typeof window.Notification !== "undefined";
  }

  async requestPermission() {
    if (!this.isSupported) return false;
    try {
      const result = await Notification.requestPermission();
      this.permission = result;
      return result === "granted";
    } catch {
      return false;
    }
  }

  async toggleNotifications() {
    const state = store.getState();
    if (state.notificationsEnabled) {
      store.setNotificationsEnabled(false);
      store.showToast("Delay notifications disabled");
      return;
    }

    if (!this.isSupported) {
      store.showToast("Browser notifications not supported on this device");
      return;
    }

    const granted = await this.requestPermission();
    if (granted) {
      store.setNotificationsEnabled(true);
      this.permission = "granted";
      store.showToast("🔔 Delay notifications enabled! You'll be alerted when delays exceed 15 minutes.");
      // Send a test notification
      this.send("RailGaadi Alerts Active", {
        body: "You'll now receive delay alerts when your tracked train is delayed by more than 15 minutes.",
        icon: "/manifest.json",
        tag: "railgaadi-test"
      });
    } else {
      store.showToast("Notification permission denied — enable it in browser settings");
    }
  }

  send(title, options = {}) {
    if (!this.isSupported || typeof window.Notification === "undefined" || window.Notification.permission !== "granted") return;
    try {
      new window.Notification(title, {
        icon: "https://images.unsplash.com/photo-1532103054090-a33923a7c120?w=192&h=192&fit=crop",
        badge: "https://images.unsplash.com/photo-1532103054090-a33923a7c120?w=192&h=192&fit=crop",
        ...options
      });
    } catch (e) {
      console.warn("[NotificationService] Failed to send notification:", e);
    }
  }

  /**
   * Check live status and fire appropriate notifications
   * Called by trackerService after each successful status fetch
   */
  checkAndNotify(liveStatus, activeTrain) {
    if (!store.getState().notificationsEnabled) return;
    if (!liveStatus || !activeTrain) return;

    const delay     = liveStatus.delayMinutes ?? 0;
    const trainName = activeTrain.name || `Train #${activeTrain.trainId}`;
    const threshold = store.getState().delayNotifThreshold;

    // Notify on significant delay change
    if (delay > threshold && delay !== this.lastNotifiedDelay) {
      this.lastNotifiedDelay = delay;
      this.send(`⏱️ ${trainName} Delayed`, {
        body: `Train is running ${delay} minutes late. Current station: ${liveStatus.currentStation?.name || "Unknown"}.`,
        tag: `delay-${activeTrain.trainId}`
      });
    }

    // Notify on delay recovery
    if (delay <= 0 && this.lastNotifiedDelay > threshold) {
      this.lastNotifiedDelay = delay;
      this.send(`✅ ${trainName} On Time`, {
        body: `Great news — train is now running on schedule!`,
        tag: `delay-${activeTrain.trainId}`
      });
    }

    // Notify on next station approach
    const nextStn = liveStatus.nextStation?.name;
    if (nextStn && nextStn !== this.lastNotifiedStation) {
      this.lastNotifiedStation = nextStn;
      const platform = liveStatus.nextStation?.platform;
      this.send(`🚂 Approaching ${nextStn}`, {
        body: `${trainName} is approaching ${nextStn}${platform ? ` on Platform ${platform}` : ""}.`,
        tag: `station-${activeTrain.trainId}`
      });
    }
  }
}

export const notificationService = new NotificationService();
