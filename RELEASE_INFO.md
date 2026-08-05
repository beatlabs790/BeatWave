# Echo Music v5.2.85
- Optimized liquid glass styling for light mode by making the text color adaptive.
- Fixed an issue where the liquid glass floating mini player would revert to the standard mini player design on album and playlist screens by making the bottom navigation bar persistent across these detail screens.
- Fixed a background crash (`ForegroundServiceStartNotAllowedException`) on Android 12+ that could occur when connecting or disconnecting from Google Cast sessions while the app was minimized.
- Fixed a crash (`Using WebView from more than one process at once`) that prevented the crash reporter from launching successfully on Android 9+ devices.