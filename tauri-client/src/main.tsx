import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./index.css"; // Ensure styles are loaded

// POLYFILL: WebKitGTK / Safari RTCPeerConnection Support
// This must run before any other code requiring WebRTC
if (typeof window !== 'undefined') {
  if (typeof window.RTCPeerConnection === 'undefined') {
    // @ts-ignore
    if (typeof window.webkitRTCPeerConnection !== 'undefined') {
      console.log('[Polyfill] Patching webkitRTCPeerConnection -> RTCPeerConnection');
      // @ts-ignore
      window.RTCPeerConnection = window.webkitRTCPeerConnection;
    } else {
      console.error('[Polyfill] RTCPeerConnection not found and webkit prefix also missing!');
    }
  }
}

import ErrorBoundary from "./components/ErrorBoundary";

ReactDOM.createRoot(document.getElementById("root") as HTMLElement).render(
  <React.StrictMode>
    <ErrorBoundary>
      <App />
    </ErrorBoundary>
  </React.StrictMode>
);
