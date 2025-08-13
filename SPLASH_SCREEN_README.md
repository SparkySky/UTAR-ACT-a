# UTAR ACT Splash Screen

## Overview
A beautiful animated splash screen for the UTAR ACT Android application with smooth animations and modern design.

## Features

### Animations
- **Logo Animation**: Scale, rotate, and bounce effects for the UTAR ACT logo
- **Text Animations**: Fade-in and slide-up animations for app name and tagline
- **Progress Bar**: Pulsing animation to indicate loading
- **Smooth Transitions**: Fade transition to LoginActivity

### Design Elements
- **Gradient Background**: Blue gradient using UTARACT brand colors
- **Full Screen**: Immersive experience with transparent status and navigation bars
- **Brand Colors**: Consistent use of UTARACT blue and white colors
- **Modern Typography**: Bold app name with descriptive tagline

## Files Created/Modified

### New Files
- `app/src/main/res/layout/activity_splash.xml` - Splash screen layout
- `app/src/main/res/drawable/splash_background.xml` - Gradient background
- `app/src/main/res/anim/fade_in.xml` - Fade-in animation
- `app/src/main/res/anim/scale_in.xml` - Scale-in animation
- `app/src/main/res/anim/slide_up.xml` - Slide-up animation
- `app/src/main/res/anim/logo_animation.xml` - Complex logo animation
- `app/src/main/res/anim/pulse.xml` - Pulse animation
- `app/src/main/java/com/meow/utaract/SplashActivity.java` - Splash activity

### Modified Files
- `app/src/main/AndroidManifest.xml` - Set SplashActivity as launcher
- `app/src/main/res/values/themes.xml` - Added NoActionBar theme
- `app/src/main/res/values-night/themes.xml` - Added NoActionBar theme for dark mode
- `app/src/main/res/values/colors.xml` - Added UTARACT brand colors
- `app/src/main/res/values/strings.xml` - Added app logo string

## Animation Timeline
1. **0ms**: Logo starts scale, rotate, and bounce animation
2. **800ms**: App name fades in
3. **1200ms**: Tagline slides up and fades in
4. **1500ms**: Progress bar starts pulsing
5. **3000ms**: Navigate to LoginActivity with fade transition

## Customization

### Colors
The splash screen uses UTARACT brand colors defined in `colors.xml`:
- `utaract_blue`: #3B82F6
- `utaract_dark_blue`: #1E3A8A
- `utaract_white`: #FFFFFF

### Duration
To change the splash screen duration, modify the delay in `SplashActivity.java`:
```java
handler.postDelayed(this::navigateToLogin, 3000); // 3 seconds
```

### Logo
Replace the logo by changing the `android:src` attribute in `activity_splash.xml`:
```xml
android:src="@drawable/your_logo"
```

## Technical Details

### Theme
The splash screen uses `Theme.UTARACT.NoActionBar` which:
- Hides the action bar
- Makes the screen fullscreen
- Uses transparent status and navigation bars

### Performance
- Animations use Android's built-in animation system
- Proper cleanup of handlers to prevent memory leaks
- Smooth 60fps animations

### Compatibility
- Works on Android API 21+ (Android 5.0+)
- Supports both light and dark themes
- Responsive design for different screen sizes 