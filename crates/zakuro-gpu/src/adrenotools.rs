//! Support for loading custom Adreno GPU drivers (such as Mesa Turnip) on Android
//! via the libadrenotools library.

use ash::vk;

pub fn load_entry() -> Result<ash::Entry, String> {
    #[cfg(target_os = "android")]
    {
        if let Some(entry) = try_load_adrenotools() {
            return Ok(entry);
        }
    }

    // Default fallback to standard libvulkan.so loader
    unsafe { ash::Entry::load() }.map_err(|e| format!("no Vulkan loader, {e}"))
}

#[cfg(target_os = "android")]
fn try_load_adrenotools() -> Option<ash::Entry> {
    let hook_lib_dir = std::env::var_os("ZAKURO_HOOK_LIB_DIR")?;
    let custom_driver_dir = std::env::var_os("ZAKURO_CUSTOM_DRIVER_DIR")?;

    let hook_dir = hook_lib_dir.to_string_lossy().into_owned();
    let mut custom_dir = custom_driver_dir.to_string_lossy().into_owned();

    if custom_dir.is_empty() || custom_dir == "system" {
        log::info!("Using system Vulkan driver (no custom driver configured)");
        return None;
    }
    if !custom_dir.ends_with('/') {
        custom_dir.push('/');
    }

    let driver_filename = std::env::var("ZAKURO_CUSTOM_DRIVER_LIB")
        .unwrap_or_else(|_| "libvulkan_freedreno.so".to_string());
    if driver_filename.is_empty() {
        log::info!("No custom driver library specified, using system driver");
        return None;
    }

    let driver_path = format!("{custom_dir}{driver_filename}");

    if !std::path::Path::new(&driver_path).exists() {
        log::info!("Custom driver not found at '{driver_path}', using system driver");
        return None;
    }

    log::info!("Found custom Turnip driver at '{driver_path}', initializing AdrenoTools...");
    log::info!("  Hook lib dir: '{hook_dir}'");

    unsafe {
        let adrenotools_path = format!("{hook_dir}/libadrenotools.so");
        let adrenotools_c = std::ffi::CString::new(adrenotools_path.clone()).ok()?;
        let adrenotools_handle = libc::dlopen(adrenotools_c.as_ptr(), libc::RTLD_NOW | libc::RTLD_LOCAL);

        if adrenotools_handle.is_null() {
            let err = std::ffi::CStr::from_ptr(libc::dlerror()).to_string_lossy();
            log::warn!("Failed to dlopen libadrenotools.so at '{adrenotools_path}': {err}");
            return None;
        }

        let open_fn_name = b"adrenotools_open_libvulkan\0";
        let open_sym = libc::dlsym(adrenotools_handle, open_fn_name.as_ptr() as *const _);
        if open_sym.is_null() {
            log::warn!("Failed to dlsym adrenotools_open_libvulkan");
            return None;
        }

        type OpenFn = unsafe extern "C" fn(
            libc::c_int,
            libc::c_int,
            *const libc::c_char,
            *const libc::c_char,
            *const libc::c_char,
            *const libc::c_char,
            *const libc::c_char,
            *mut *mut libc::c_void,
        ) -> *mut libc::c_void;

        let open_fn: OpenFn = std::mem::transmute(open_sym);

        let hook_dir_c = std::ffi::CString::new(hook_dir).ok()?;
        let custom_dir_c = std::ffi::CString::new(custom_dir).ok()?;
        let driver_name_c = std::ffi::CString::new(driver_filename).ok()?;

        // Flags: ADRENOTOOLS_DRIVER_CUSTOM = 1
        let vk_handle = open_fn(
            libc::RTLD_NOW | libc::RTLD_LOCAL,
            1, // ADRENOTOOLS_DRIVER_CUSTOM
            std::ptr::null(), // tmpLibDir (null means use memfd on Android 29+)
            hook_dir_c.as_ptr(),
            custom_dir_c.as_ptr(),
            driver_name_c.as_ptr(),
            std::ptr::null(),
            std::ptr::null_mut(),
        );

        if vk_handle.is_null() {
            log::warn!("adrenotools_open_libvulkan returned NULL, falling back to system Vulkan");
            return None;
        }

        log::info!("AdrenoTools successfully hooked Vulkan! Resolving vkGetInstanceProcAddr...");
        let proc_sym = libc::dlsym(vk_handle, b"vkGetInstanceProcAddr\0".as_ptr() as *const _);
        if proc_sym.is_null() {
            log::warn!("Failed to resolve vkGetInstanceProcAddr from hooked Vulkan handle");
            return None;
        }

        let get_proc_addr: vk::PFN_vkGetInstanceProcAddr = std::mem::transmute(proc_sym);
        let static_fn = ash::StaticFn {
            get_instance_proc_addr: get_proc_addr,
        };
        let entry = ash::Entry::from_static_fn(static_fn);
        log::info!("Successfully loaded ash::Entry via AdrenoTools (Turnip driver active)!");
        Some(entry)
    }
}
