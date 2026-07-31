# RavenB++ (1.7.10 Linux/Wayland Port)

Raven b++ is a pvp and utility mod for minecraft. This is a 1:1 port of the original 1.8.9 version to **Minecraft 1.7.10** with full **Linux (Arch/Wayland)** support.<br>
Not related to Raven B3/B4 in any way.

## Changes from Original

- **Minecraft 1.7.10** compatible (Forge 10.13.4.1614)
- **Linux/Wayland** compatible: replaced `java.awt.Robot` with LWJGL input, added `xdg-open` and `wl-copy` fallbacks
- All rendering converted from GlStateManager to direct GL11 calls
- BlockPos references replaced with int coordinates for 1.7.10 API
- Tessellator/WorldRenderer API updated for 1.7.10

## Community

Official Discord of RavenB++:
[https://discord.gg/UqJ8ngteud](https://discord.gg/UqJ8ngteud)

---

# Installation & Download

* Download Forge for Minecraft 1.7.10 [here](https://maven.minecraftforge.net/net/minecraftforge/forge/1.7.10-10.13.4.1614-1.7.10/forge-1.7.10-10.13.4.1614-1.7.10-installer.jar) and run the installer.
* After launching the Forge profile once:
  * On Linux: go to `~/.minecraft/mods`
  * On Windows: go to `%appdata%\.minecraft\mods`
  * On macOS: go to `~/Library/Application Support/minecraft/mods/`
* Put the built jar into the `mods` folder.
* Launch Minecraft using the Forge 1.7.10 profile and you're good to go.

---

# Building Ravenb++ Yourself

1. Make sure you have **Java 8** installed and set as `JAVA_HOME`.
2. Clone this repository to your machine.
3. Open a terminal in the project folder.
4. Run:

```bash
./gradlew build
```

5. The compiled build will be located in:

```text
build/libs
```
