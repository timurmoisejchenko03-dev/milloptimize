# MillOptimize 🚀

**MillOptimize** is a high-performance optimization addon specifically engineered for the **Millénaire** mod. It is designed to help maintain high TPS (Ticks Per Second) in large-scale settlements by optimizing how NPC AI is handled and how villages are processed.

## 🎯 Target Environment
- **Minecraft Version**: 1.21.1
- **Target Mod**: Millénaire **9.0.0-beta.1**
- **Platform**: NeoForge / Forge

## 🌟 The Core Mission
As settlements grow, the computational load of NPC AI and village mechanics can become overwhelming for even the most powerful servers. **MillOptimize** bridges the gap between "realistic simulation" and "smooth gameplay" by intelligently throttling non-essential background processes.

## 🛠 Evolution & Heritage
This project is a **dedicated fork and successor** to [MillMix_Jubi](https://github.com/G1ll0u/MillMix_Jubi). 

While it preserves and refines the core optimizations from the original project, it has been completely overhauled to support modern versions and introduces several next-generation features.

## ✨ Key Features

### 🏘 Village Persistence (The "Cherry on Top")
*Unique to this version.* 
In standard Millénaire, villages often "pause" or lose their momentum when no players are nearby or if a player disconnects. 
**MillOptimize** introduces a persistence layer:
- **Active State Tracking**: The mod remembers the last village visited by the player.
- **Continuous Evolution**: Villages continue to build, expand, and function even after a player disconnects. 
- **Persistence Layer**: Uses NBT data to ensure that village progress is saved and restored correctly between sessions.
- **Server Harmony**: Designed to keep the world "alive" without causing unnecessary CPU spikes.

### 🚀 AI Throttling (Distance-Based Optimization)
- **Smart Awareness**: The mod detects NPCs that are outside the player's immediate vicinity (beyond 100 blocks).
- **Dynamic Throttling**: It automatically disables the goal scheduler for these NPCs.
- **Result**: Massive reduction in CPU overhead in sprawling maps where you only see a fraction of the inhabitants at any one time.

### 🔍 Village Search Caching
- Prevents the "stutter" often caused by frequent village lookups.
- By caching the player's last position, the mod avoids heavy chunk recalculations if the player is stationary or moving slowly.

## ⚙️ Technical Details
- **Mod Type**: Optimization / Addon
- **Optimization Strategy**: Dynamic throttling and NBT-based state management.

## 🚀 Installation
1. Ensure you have **Millénaire 9.0.0-beta.1** installed.
2. Place the `milloptimize.jar` file into your `mods` folder.
3. Enjoy a lag-free, living world!

---
*Ported and enhanced from MillMix_Jubi.*
