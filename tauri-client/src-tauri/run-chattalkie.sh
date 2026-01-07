#!/bin/bash
#
# ChatTalkie Launcher with WebKitGTK Media Permissions Fix
# This script sets environment variables to enable camera/microphone access
# on Fedora with Wayland session.
#

# Force WebKitGTK to use the new media permission portal
export WEBKIT_FORCE_SANDBOX=0

# Enable PipeWire camera access
export LIBCAMERA_LOG_LEVELS="*:3"

# GTK debugging (optional - uncomment if needed)
# export GTK_DEBUG=interactive

# XDG Portal debugging (optional - shows portal interactions)
# export XDG_PORTAL_DEBUG=1

# For development: Navigate to the tauri-client directory
cd "$(dirname "$0")"

# Run the Tauri app
if [ -f "./target/release/tauri-client" ]; then
    exec ./target/release/tauri-client "$@"
elif [ -f "./target/debug/tauri-client" ]; then
    exec ./target/debug/tauri-client "$@"
else
    echo "Error: Tauri binary not found. Run 'npm run tauri build' first."
    exit 1
fi
