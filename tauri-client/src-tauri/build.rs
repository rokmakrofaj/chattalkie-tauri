/*
 * build.rs - Build script for Tauri client
 *
 * Links the native-call-core library for audio/video capture.
 */

fn main() {
    // Standard Tauri build
    tauri_build::build();

    // Link native-call-core library (Linux only)
    // Link native-call-core library (Linux only) - REMOVED
    // WebKitGTK 4.1 has native WebRTC support, so we don't need this custom C library anymore.
    /*
    #[cfg(target_os = "linux")]
    {
        // Path to the native-call-core build directory
        let native_lib_path = std::path::Path::new(env!("CARGO_MANIFEST_DIR"))
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .join("native-call-core")
            .join("build");

        println!("cargo:rustc-link-search=native={}", native_lib_path.display());
        println!("cargo:rustc-link-lib=dylib=native-call");
        
        // Add run-path (RPATH) to ensure the binary finds the library at runtime
        // without needing LD_LIBRARY_PATH or installation to /usr/local/lib
        println!("cargo:rustc-link-arg=-Wl,-rpath,{}", native_lib_path.display());
        
        // Re-run if library changes
        println!("cargo:rerun-if-changed={}/libnative-call.so", native_lib_path.display());
    }
    */
}
