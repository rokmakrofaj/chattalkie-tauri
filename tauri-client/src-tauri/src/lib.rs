// ChatTalkie Tauri Desktop App - DÜZELTİLMİŞ VERSİYON
// Uses WebKitGTK WebRTC support on Linux

#[tauri::command]
fn greet(name: &str) -> String {
    format!("Hello, {}! You've been greeted from Rust!", name)
}

#[tauri::command]
async fn check_webrtc_support(window: tauri::WebviewWindow) -> Result<bool, String> {
    #[cfg(target_os = "linux")]
    {
        // with_webview returns Result<()> (or similar unit type) in this binding, 
        // so we perform side effects and return Ok(true) if successful.
        window.with_webview(|webview| {
            #[cfg(target_os = "linux")]
            {
                use webkit2gtk::{WebViewExt, SettingsExt};
                
                let gtk_webview = webview.inner();
                
                if let Some(settings) = gtk_webview.settings() {
                    settings.set_enable_webrtc(true);
                    settings.set_enable_media_stream(true);
                    settings.set_enable_media_capabilities(true);
                    settings.set_enable_mediasource(true);
                    settings.set_enable_developer_extras(true);
                    settings.set_enable_write_console_messages_to_stdout(true);
                    settings.set_hardware_acceleration_policy(
                        webkit2gtk::HardwareAccelerationPolicy::Always
                    );
                }
            }
        }).map_err(|e| e.to_string())?;
        
        Ok(true)
    }
    
    #[cfg(not(target_os = "linux"))]
    {
        Ok(true)
    }
}

#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
    // Linux Environment Variables
    #[cfg(target_os = "linux")]
    {
        std::env::set_var("WEBKIT_DISABLE_COMPOSITING_MODE", "1");
        std::env::set_var("WEBKIT_WEBRTC_ENABLED", "1");
        std::env::set_var("GST_PLUGIN_FEATURE_RANK", "vah264dec:MAX,vaav1dec:MAX,vavp8dec:MAX,vavp9dec:MAX");
        std::env::set_var("WEBKIT_FORCE_SANDBOX", "0");
        std::env::set_var("GST_DEBUG", "webrtc*:3");
        std::env::set_var("PULSE_PROP_media.role", "phone");
    }

    tauri::Builder::default()
        .plugin(tauri_plugin_opener::init())
        .invoke_handler(tauri::generate_handler![
            greet,
            check_webrtc_support
        ])
        .setup(|app| {
            #[cfg(target_os = "linux")]
            {
                use tauri::Manager;
                
                if let Some(window) = app.get_webview_window("main") {
                    println!("[Tauri] Configuring WebKitGTK WebRTC for main window");
                    
                    let _ = window.with_webview(|webview| {
                        #[cfg(target_os = "linux")]
                        {
                            use webkit2gtk::{WebViewExt, SettingsExt, PermissionRequestExt};
                            // Import ObjectExt to use .is::<T>()
                            use webkit2gtk::glib::ObjectExt; 
                            
                            let gtk_webview = webview.inner();
                            
                            if let Some(settings) = gtk_webview.settings() {
                                settings.set_enable_webrtc(true);
                                settings.set_enable_media_stream(true);
                                settings.set_enable_media_capabilities(true);
                                settings.set_enable_mediasource(true);
                                settings.set_enable_encrypted_media(true);
                                settings.set_enable_javascript(true);
                                settings.set_enable_webgl(true);
                                settings.set_enable_webaudio(true);
                                settings.set_enable_developer_extras(true);
                                settings.set_enable_write_console_messages_to_stdout(true);
                                
                                settings.set_user_agent(Some(
                                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                ));
                                
                                println!("[Tauri] ✅ WebKitGTK WebRTC settings applied");
                                
                                gtk_webview.connect_permission_request(|_, request| {
                                    if request.is::<webkit2gtk::UserMediaPermissionRequest>() || 
                                       request.is::<webkit2gtk::DeviceInfoPermissionRequest>() {
                                        println!("[Tauri] Auto-allowing permission request");
                                        request.allow();
                                        return true;
                                    }
                                    false
                                });
                            }
                        }
                    });
                }
            }
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
