# Echo Music v5.2.83
- Fixed an issue where the wrong song would play or the song wouldn't play at all.
- Optimized liquid glass styling for light mode by making the text color adaptive by default.
- Fixed an issue where the liquid glass floating mini player would revert to the standard mini player design on album and playlist screens by making the bottom navigation bar persistent across these detail screens.
- Fixed a background crash (`ForegroundServiceStartNotAllowedException`) on Android 12+ that could occur when connecting or disconnecting from Google Cast sessions while the app was minimized.
- Fixed a crash (`Using WebView from more than one process at once`) that prevented the crash reporter from launching successfully on Android 9+ devices.
- Feature/custom fonts (#943) by @berruetaa
- chore(l10n): update translations (#951) by @weblate
- feat(cast): improve Google Cast volume sync, device picker and audio output integration (#976) by @Hitomatito

Thanks to you all for making this project reach 3k+ GitHub stars, more than 3K+ lossless music and more than 4K+ canvas - it's just possible cause of the support of the community - there are still some bugs so I will be polishing the app by the time. Take care!
