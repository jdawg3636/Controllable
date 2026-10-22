# Controllable for Minecraft 1.5.2

An independent legacy implementation of Controllable's controller features for
**Minecraft 1.5.2 / Forge 7.8.1.738**. Forge 36.2.34 is for Minecraft 1.16.5 and
cannot be used for this backport. The modern upstream source remains in the
repository's original directories; this directory is a separate Gradle build.

## Install

1. Install Minecraft **1.5.2** and Forge **7.8.1.738** in your launcher.
2. Use a **Java 8** game runtime (recommended for legacy Forge).
3. Copy `build/libs/controllable-0.1.0+1.5.2.jar` into that instance's `mods` folder.
   Use the plain JAR, not the `-dev` or `-sources` JAR.
4. Connect your controller and launch. Open settings with **F8**, **Guide**, or
   **Back + Start**. The Xbox Guide button can be reserved by the operating system.

JNA, SDL2, and the controller mapping database are included. No other mod or
manual SDL installation is required. SDL libraries extract automatically into
`controllable_natives/2.32.10/<platform>` within the game directory. Native loading
errors appear in settings and in the game log; keyboard/mouse play stays available.

This is a client mod. Servers do not need it; its network metadata does not require
clients or servers to install it. Each game instance has independent settings and
can select a different controller. Hotplug and controller reconnection are supported.

## Xbox 360 controls

| Control | In game | Menus / inventory |
| --- | --- | --- |
| Left stick | Analog movement | Cursor |
| Right stick | Camera | Scroll vertically |
| A | Jump / swim / fly upward | Left click / hold to drag |
| B | Sneak / fly downward | Back / close |
| X | Inventory | Right click / split stack |
| Y | Drop item | Quick move hovered inventory slot |
| LT / RT | Use / place; attack / mine | Right / left click |
| LB / RB | Previous / next hotbar slot | Scroll up / down |
| Left stick click | Sprint while moving forward | On-screen keyboard for focused text field |
| Right stick click | Pick block | — |
| D-pad up / down | Perspective / drop | Navigate buttons and inventory slots |
| D-pad left / right | Chat / unassigned | Navigate buttons and inventory slots |
| Back (hold) | Radial wheel; choose with left stick, release to activate | — |
| Start | Pause | Back |
| Guide or Back + Start | Settings | Settings |

Jump twice to toggle creative flight, as with the keyboard. Keyboard and mouse can
be used alongside the controller. Input releases on disconnect, loss of focus, and
GUI transitions to avoid stuck actions. Analog movement defaults to local worlds;
servers receive digital movement unless **Analog on servers** is enabled.

Settings include sensitivity, radial dead zone, inverted look, trigger threshold,
cursor speed, hints, damage rumble, toggle sneak, controller selection, optional
merging of two controllers, action remapping, and configurable radial actions.
The stronger stick of either merged device wins; buttons combine and triggers use
the larger value. Choose a different secondary controller to enable merging.

The settings pages provide mappings for all registered vanilla/Forge key bindings,
including mod key handlers. These supplement the default actions: unbind any default
action that shares a button if you do not want both actions to occur. The on-screen
keyboard sends normal character events to the focused field. It offers letters,
numbers, common punctuation, shift, space, backspace and Enter; ordinary physical
keyboard input also works. For chat, D-pad left opens it automatically.

Configuration is saved in `config/controllable.properties`. Radial slots can contain
`inventory`, `chat`, `drop`, `perspective`, `screenshot`, `fullscreen`, `playerlist`,
`pick`, or `settings`. Controller IDs follow SDL's standardized layout; 15 is LT,
16 is RT, and 17 means unbound. Native SDL mappings cover Xbox 360 and many other
controller families without platform-specific button-number assumptions.

## Platforms

The JAR includes the **same SDL 2.32.10 binaries as upstream ControllableSDL**:

| OS | Architectures |
| --- | --- |
| Windows | x86, x86-64, ARM64 |
| Linux | x86, x86-64, ARM64 |
| macOS | x86-64, ARM64 (universal SDL library) |

These describe the controller backend. Minecraft 1.5.2 itself uses LWJGL 2;
your launcher must separately provide a compatible JVM and Minecraft/LWJGL natives
for the host architecture. Packaging ARM SDL does not make Mojang's old x86-only
LWJGL binaries run on ARM. SDL native provenance and checksums are in `vendor/README.md`.

## Build and development

Run Gradle using **JDK 17 or 21**. Gradle uses JDK 17 to compile and selects/downloads a Java 8 toolchain
for the development game. Output bytecode targets Java 7 (class version 51) so old
Forge's ASM 4 scanner can read every class in the mod, including bundled JNA.

From the repository root:

```sh
bash legacy/gradlew -p legacy build
bash legacy/gradlew -p legacy runClient
```

Or from this directory: `./gradlew build` / `./gradlew runClient` (Windows:
`gradlew.bat build`). The release is `legacy/build/libs/controllable-0.1.0+1.5.2.jar`.
`build` runs unit/native tests, verifies the vendored SDL checksum, remaps to
Minecraft's production names, and checks all platform libraries and class versions.
The build needs network access for Gradle/Minecraft/Forge/Maven dependencies,
but does not need GitHub Packages credentials. `genSources` generates the old
Minecraft/Forge sources; `genIntellijRuns` creates IDE run configurations.

The Unimined 1.4.1 FG1 development launcher needs two compatibility settings:
JarModAgent is disabled so it does not bypass Forge's runtime marker transforms,
and Java 8's verifier is disabled **only for the merged development client** because
its server entrypoint references a server-only class. These settings do not change
the distributed mod or require a coremod.

## Verification

```sh
./gradlew test
./gradlew runClient -Psmoke
./gradlew remapJar exportClientLaunch -Psmoke
python3 tools/production_smoke.py
./gradlew clean build
```

`-Psmoke` adds a test-only Forge mod and must not be used to build a release. It
attaches an SDL virtual gamepad and drives a real integrated-server session, writes
`run/client/smoke-result.txt`, and exits. On Linux it focuses its own Minecraft X11
window for this test. The production runner installs the remapped test JAR into
the generated instance temporarily and runs against obfuscated Forge classes,
without development output/classes or JarModAgent. It removes the test JAR afterward.
The test world is named `Controller smoke test` within the generated instance.

Automated native tests exercise axes, buttons, hotplug and safe unsupported rumble.
CI is configured for Windows, Linux and macOS, including ARM64 runners. Hardware
and OS coverage actually executed locally is recorded in `VALIDATION.md`;
physical Xbox 360 compatibility and unexecuted OS checks should not be inferred
from a successful Linux virtual-controller test.

This is a substantial adaptation, not byte-for-byte feature parity with the
Minecraft 26.2 user interface: modern recipe-book/JEI/REI/EMI integrations have no
corresponding vanilla API in 1.5.2. Controller-specific actions use the old game's
available mechanics. MIT upstream license and third-party notices are preserved.
