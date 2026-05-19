# AutoTyper - IntelliJ Live Coding Simulator Plugin

A plugin that simulates realistic typing of pre-written Java classes in the IntelliJ editor.
Perfect for live coding demonstrations in lectures and presentations.

## Features

- 📁 Load Java files from a configurable folder
- ⌨️ Simulate typing with natural-feeling delays
- ⚡ Adjustable speed (30-600 WPM) via slider
- ⏸️ Pause/Resume/Stop controls
- 📊 Progress indicator
- 🎯 Natural typing simulation with random variations

## Quick Start

1. Build the plugin: `./gradlew buildPlugin`
2. Install: Settings → Plugins → ⚙️ → Install from Disk → select `build/distributions/autotyper-plugin-1.0.0.zip`
3. Copy your Java files to `~/autotyper-snippets/`
4. Open the "AutoTyper" tool window (right sidebar)
5. Select a file and click Start!

## Requirements

- IntelliJ IDEA 2023.3+
- JDK 17+
- Gradle 8.5+
