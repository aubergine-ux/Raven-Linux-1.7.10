# RavenB++ (1.7.10 Linux/Wayland Port)

Raven b++ is a pvp and utility mod for minecraft. This is a 1:1 port of the original 1.8.9 version to **Minecraft 1.7.10** with full **Linux (Arch/Wayland)** support.<br>
Not related to Raven B3/B4 in any way.

## Changes from Original

- **Minecraft 1.7.10** compatible (Forge 10.13.4.1614)
- **Linux/Wayland** compatible: replaced `java.awt.Robot` with LWJGL input, added `xdg-open` and `wl-copy` fallbacks
- All rendering converted from GlStateManager to direct GL11 calls
- BlockPos references replaced with int coordinates for 1.7.10 API
- Tessellator/WorldRenderer API updated for 1.7.10
