/**
 * RailGaadi Share Journey Modal — v2
 * Uses live store state (activeTrain) — no hardcoded getTrainById calls
 */

import { store } from "../state/store.js";

export function renderShareModal() {
  const state = store.getState();
  if (!state.isShareModalOpen) return "";

  const train    = state.activeTrain;
  const trainId  = state.activeTrainId;
  const name     = train?.name   || `Train #${trainId || "—"}`;
  const number   = train?.number || trainId || "—";
  const origin   = train?.origin || "";
  const dest     = train?.destination || "";

  const shareUrl = `${window.location.origin}${window.location.pathname}#train=${trainId}&date=${state.activeDate}`;

  return `
    <div class="modal-backdrop" id="shareModalBackdrop" role="dialog" aria-modal="true" aria-labelledby="shareModalTitle">
      <div class="modal-card">
        <div class="modal-header">
          <h3 id="shareModalTitle" style="font-size:var(--text-md);font-weight:700;">Share Live Train Status</h3>
          <button class="header-btn btn-icon-only" id="closeShareModalBtn" aria-label="Close modal">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M18 6 6 18"/><path d="m6 6 12 12"/>
            </svg>
          </button>
        </div>

        <div class="modal-body">
          <p style="font-size:var(--text-sm);color:var(--color-text-secondary);margin-bottom:var(--space-2);">
            Share this link with family or pickup contacts.
            They'll see real-time progress for <strong>${name} (#${number})</strong>
            ${origin && dest ? ` from <strong>${origin}</strong> to <strong>${dest}</strong>` : ""}.
          </p>

          <div class="share-url-box">
            <input type="text" class="share-url-input" id="shareUrlInputField" value="${shareUrl}" readonly />
            <button class="header-btn" id="copyShareUrlBtn" style="flex-shrink:0;margin-left:8px;">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <rect width="14" height="14" x="8" y="8" rx="2" ry="2"/><path d="M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"/>
              </svg>
              <span>Copy</span>
            </button>
          </div>

          ${typeof navigator !== "undefined" && navigator.share ? `
            <button class="header-btn" id="nativeShareBtn" style="width:100%;height:44px;background:var(--color-accent);color:white;border:none;font-weight:700;margin-top:10px;">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/>
                <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
              </svg>
              <span>Share via WhatsApp / Messages</span>
            </button>
          ` : ""}
        </div>
      </div>
    </div>
  `;
}

export function attachShareModalEvents() {
  const backdrop  = document.getElementById("shareModalBackdrop");
  const closeBtn  = document.getElementById("closeShareModalBtn");
  const copyBtn   = document.getElementById("copyShareUrlBtn");
  const input     = document.getElementById("shareUrlInputField");
  const nativeBtn = document.getElementById("nativeShareBtn");

  backdrop?.addEventListener("click", e => { if (e.target === backdrop) store.closeShareModal(); });
  closeBtn?.addEventListener("click", () => store.closeShareModal());

  if (copyBtn && input) {
    copyBtn.addEventListener("click", () => {
      input.select();
      navigator.clipboard.writeText(input.value)
        .catch(() => document.execCommand("copy"))
        .finally(() => {
          store.showToast("Copied tracking link to clipboard! 📋");
          store.closeShareModal();
        });
    });
  }

  if (nativeBtn && input) {
    nativeBtn.addEventListener("click", () => {
      const s = store.getState();
      const t = s.activeTrain;
      navigator.share({
        title: `Track ${t?.name || "Train"} (#${t?.number || s.activeTrainId}) on RailGaadi`,
        text:  `Live running status for ${t?.name || "this train"} (${t?.origin || ""} → ${t?.destination || ""}):`,
        url:   input.value
      }).catch(() => {});
    });
  }
}
