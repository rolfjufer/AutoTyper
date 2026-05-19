# ⌨️ AutoTyper - IntelliJ Live Coding Simulator

[![JetBrains Plugin](https://img.shields.io/badge/JetBrains-Marketplace-blue?logo=jetbrains)](https://plugins.jetbrains.com)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![IntelliJ Version](https://img.shields.io/badge/IntelliJ-2023.3%2B-purple?logo=intellijidea)](https://www.jetbrains.com/idea/)

**Simulate realistic, structured typing of Java classes in the IntelliJ editor — perfect for live coding demonstrations in lectures and presentations.**

Instead of typing code line-by-line from top to bottom, AutoTyper types code in the **natural development order** — just like a real developer would write it.

![AutoTyper Demo](docs/demo.png)

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 📝 **Step Markers** | Define typing order with simple `// @step N` comments |
| 📁 **Auto File Creation** | Automatically creates the Java file in the correct package directory |
| 📦 **Package-Aware** | Detects `package` declarations and creates proper directory structure |
| ⌨️ **Alt+N Shortcut** | Global shortcut to advance to next step (works even with editor focus) |
| ⚡ **Adjustable Speed** | From 30 WPM (slow demo) to 600 WPM (turbo) |
| ⏸️ **Auto-Pause** | Pauses between steps so you can explain |
| 🎯 **Natural Feel** | Randomized delays simulate real human typing |
| 📊 **Progress Indicator** | Shows current step and character progress |

---

## 🚀 Installation

### From JetBrains Marketplace (recommended)

1. In IntelliJ IDEA: `Settings` → `Plugins` → `Marketplace`
2. Search for **"AutoTyper"**
3. Click **Install** → Restart IntelliJ

### From ZIP (manual)

1. Download the latest release ZIP from [Releases](https://github.com/rolfjufer/autotyper-plugin/releases)
2. In IntelliJ: `Settings` → `Plugins` → ⚙️ → `Install Plugin from Disk...`
3. Select the ZIP → Restart IntelliJ

### Build from Source

```bash
git clone https://github.com/rolfjufer/autotyper-plugin.git
cd autotyper-plugin
gradle buildPlugin
# Output: build/distributions/autotyper-plugin-*.zip
```

---

## 📄 License

This project is licensed under the **MIT License**.

The software is provided "as is", without warranty of any kind. For more details, see the [LICENSE](LICENSE) file.
