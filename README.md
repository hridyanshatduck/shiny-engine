# Android Live Wallpaper App

This app displays an image (per.png) before unlocking, plays a video (vid1.mp4) after unlocking, then shows another image (post.png) and sets it as the wallpaper. It is optimized for battery usage and landscape tablets.

## Features
- Image before unlock
- Video after unlock
- Image after video ends, set as wallpaper
- Battery optimization
- Landscape tablet optimization

## Build Instructions
This project is ready for GitHub Actions APK build. See `.github/workflows/android.yml` for CI setup.

## Assets
- Place `per.png`, `post.png`, and `vid1.mp4` in `app/src/main/res/drawable` and `app/src/main/res/raw` as appropriate.

## License
Specify your license here.
