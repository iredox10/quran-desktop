#[cfg_attr(mobile, tauri::mobile_entry_point)]
pub fn run() {
  tauri::Builder::default()
    .plugin(tauri_plugin_store::Builder::new().build())
    .plugin(tauri_plugin_notification::init())
    .plugin(tauri_plugin_updater::Builder::new().build())
    .setup(|app| {
      if cfg!(debug_assertions) {
        app.handle().plugin(
          tauri_plugin_log::Builder::default()
            .level(log::LevelFilter::Info)
            .build(),
        )?;
      }
      app.handle().plugin(tauri_plugin_fs::init())?;
      app.handle().plugin(tauri_plugin_http::init())?;

      // Tray Setup
      use tauri::menu::{Menu, MenuItem};
      use tauri::tray::{TrayIconBuilder, MouseButton, MouseButtonState, TrayIconEvent};
      use tauri::{Manager, Emitter};

      let show_i = MenuItem::with_id(app, "show", "Show Quran", true, None::<&str>)?;
      let hide_i = MenuItem::with_id(app, "hide", "Hide", true, None::<&str>)?;
      let play_i = MenuItem::with_id(app, "play", "Play/Pause Audio", true, None::<&str>)?;
      let quit_i = MenuItem::with_id(app, "quit", "Quit", true, None::<&str>)?;

      let menu = Menu::with_items(app, &[&show_i, &hide_i, &play_i, &quit_i])?;

      let tray = TrayIconBuilder::new()
        .menu(&menu)
        .show_menu_on_left_click(false)
        .icon(app.default_window_icon().unwrap().clone())
        .on_menu_event(|app, event| match event.id.as_ref() {
          "show" => {
            if let Some(window) = app.get_webview_window("main") {
              window.show().unwrap();
              window.set_focus().unwrap();
            }
          }
          "hide" => {
            if let Some(window) = app.get_webview_window("main") {
              window.hide().unwrap();
            }
          }
          "play" => {
            app.emit("tray-play-pause", ()).unwrap();
          }
          "quit" => {
            app.exit(0);
          }
          _ => {}
        })
        .on_tray_icon_event(|tray, event| {
          if let TrayIconEvent::Click {
            button: MouseButton::Left,
            button_state: MouseButtonState::Up,
            ..
          } = event
          {
            if let Some(window) = tray.app_handle().get_webview_window("main") {
              if window.is_visible().unwrap_or(false) {
                window.hide().unwrap();
              } else {
                window.show().unwrap();
                window.set_focus().unwrap();
              }
            }
          }
        })
        .build(app)?;

      Ok(())
    })
    .on_window_event(|window, event| match event {
      tauri::WindowEvent::CloseRequested { api, .. } => {
        // Intercept close to minimize to tray instead
        window.hide().unwrap();
        api.prevent_close();
      }
      _ => {}
    })
    .run(tauri::generate_context!())
    .expect("error while running tauri application");
}
