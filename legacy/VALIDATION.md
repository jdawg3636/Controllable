# Version 0.1.1 validation — October 22, 2026

The jump failure was reproduced in a separate copy of the local V0LTZ pack.
Forge reported all 90 mods loaded. The old 0.1.0 JAR failed the new assertion
that A reaches `movementInput.jump`; the 0.1.1 fix passed it and an actual jump-height
check. The copy used the pack's patched jar, mods, coremods, config, Chocolate and
Flan data, with a fresh generated world. The installed instance and user worlds
were not modified. The test launch uses a Java 8 development classpath, so it is
not a byte-for-byte reproduction of the launcher's entire runtime environment.

Backward walking already passed with a virtual controller in the unmodified pack.
The added movement regression deliberately leaves the forward binding set while
holding the stick backward. Version 0.1.1 preserves the requested reverse direction
and checks actual backward displacement, not just an input flag.

Nine JUnit tests passed, including duplicate key-code aliases, isolated pulse and
release behavior, physical-key preservation, and opposing keyboard/stick input.
The full integration suite passed with the remapped 0.1.1 test JAR in `mods`:

```text
[SMOKE] PASS: SDL virtual gamepad attached
[SMOKE] PASS: Minecraft receives virtual controller
[SMOKE] PASS: Minecraft window is focused
[SMOKE] PASS: settings screen opens
[SMOKE] PASS: GUI bridge dispatches Escape
[SMOKE] PASS: positive stick Y produces backward movement input
[SMOKE] PASS: positive stick Y actually walks player backward despite conflicting forward binding
[SMOKE] PASS: A reaches player movement jump input
[SMOKE] PASS: A actually lifts player off the ground
[SMOKE] PASS: analog stick moves player
[SMOKE] PASS: right stick rotates camera
[SMOKE] PASS: X opens inventory
[SMOKE] PASS: B closes inventory
[SMOKE] PASS: Back opens radial wheel
[SMOKE] PASS: radial release opens selected inventory action
[SMOKE] PASS: Chat opens on-screen keyboard
[SMOKE] PASS: on-screen keyboard sends chat and closes
[SMOKE] PASS: A selects a vanilla GuiSlot list entry
[SMOKE] PASS: right stick scrolls vanilla list
[SMOKE] PASS: right trigger holds attack
[SMOKE] PASS: attack releases with trigger
[SMOKE] PASS: left trigger holds use
[SMOKE] PASS: A holds jump
[SMOKE] PASS: B holds sneak
[SMOKE] PASS: held inputs release cleanly
[SMOKE] PASS: mapped Forge mod key receives both tick callbacks
[SMOKE] PASS: mapped Forge mod key receives release callback
[SMOKE] PASS: two controllers merge movement and buttons
[SMOKE] PASS: attack held before disable
[SMOKE] PASS: disabling controller releases held attack
[SMOKE] PASS: enabling restores held controller input
[SMOKE] PASS: disconnect releases held attack
[SMOKE] PASS: All integration checks passed
```

Testing used SDL virtual controllers on Linux x86-64. Physical Xbox 360 hardware
and other operating systems were not retested. SDL binaries are unchanged from
0.1.0. The clean release build excludes the smoke mod and checks all eight native
resources and every bundled class's Java 7 compatibility.

Release: `build/libs/controllable-0.1.1+1.5.2.jar`

SHA-256: `87f270ab9bd5bf4ec4513cfa7a0be1869bd2039d039d62f64ce5271a50465894`

---

# Original 0.1.0 validation

Tested locally on October 21, 2026 (America/Chicago), on Linux x86-64.
Build: JDK 17, Gradle 8.14, Unimined 1.4.1. Game: Temurin Java 8,
Minecraft 1.5.2, Forge 7.8.1.738, LWJGL 2.9.0, bundled SDL 2.32.10.

## Automated tests

- Five JUnit tests: stick normalization/dead-zone behavior, merged stick/button
  semantics, configuration round-trip, malformed configuration bounds, and
  the actual bundled native SDL library with a virtual game controller.
- Native test checks axes, SDL trigger ranges, button press/release, attachment,
  detachment, extraction, and harmless failure of unsupported virtual rumble.
- `clean build` verifies the SDL vendor checksum, all eight bundled platform
  native paths, required resources, and Java class version <= 51 for every class.
  It also rejects any release containing the integration test mod.
- Development integrated-server smoke test passed.
- Production integrated-server smoke test passed using the remapped JAR in the
  instance's `mods` directory, obfuscated Forge/Minecraft classes, no development
  class directories, no JarModAgent, and normal Java 8 verification enabled.

The production run completed at approximately 01:58 local time. Its assertions:

```text
[SMOKE] PASS: SDL virtual gamepad attached
[SMOKE] PASS: Minecraft receives virtual controller
[SMOKE] PASS: Minecraft window is focused
[SMOKE] PASS: settings screen opens
[SMOKE] PASS: GUI bridge dispatches Escape
[SMOKE] PASS: analog stick moves player
[SMOKE] PASS: right stick rotates camera
[SMOKE] PASS: X opens inventory
[SMOKE] PASS: B closes inventory
[SMOKE] PASS: Back opens radial wheel
[SMOKE] PASS: radial release opens selected inventory action
[SMOKE] PASS: Chat opens on-screen keyboard
[SMOKE] PASS: on-screen keyboard sends chat and closes
[SMOKE] PASS: A selects a vanilla GuiSlot list entry
[SMOKE] PASS: right stick scrolls vanilla list
[SMOKE] PASS: right trigger holds attack
[SMOKE] PASS: attack releases with trigger
[SMOKE] PASS: left trigger holds use
[SMOKE] PASS: A holds jump
[SMOKE] PASS: B holds sneak
[SMOKE] PASS: held inputs release cleanly
[SMOKE] PASS: mapped Forge mod key receives both tick callbacks
[SMOKE] PASS: mapped Forge mod key receives release callback
[SMOKE] PASS: two controllers merge movement and buttons
[SMOKE] PASS: attack held before disable
[SMOKE] PASS: disabling controller releases held attack
[SMOKE] PASS: enabling restores held controller input
[SMOKE] PASS: disconnect releases held attack
[SMOKE] PASS: All integration checks passed
```

The settings and on-screen keyboard were also inspected from actual Minecraft
screenshots. The ordinary release build excludes the smoke mod; its source is
kept in `src/smoke` so the integration test can be repeated.

## Coverage limits

No physical Xbox 360 controller was available to test, and Windows/macOS/ARM
executables were not run on this Linux host. The SDL libraries for those systems
are the exact pinned upstream binaries, packaged unchanged. A six-runner CI matrix
is supplied but was not dispatched from this workspace. Physical force feedback,
third-party mod GUIs/key handlers, remote multiplayer servers, and every launcher
combination have not been exhaustively tested. The in-game test uses a registered
Forge test key handler to verify the legacy callback bridge.

The Java 7 class target is checked; gameplay was run on Java 8. On ARM, the old
game's LWJGL/JVM compatibility is a launcher concern independent of bundled SDL.
This port supplies the old game's applicable controller features, not the modern
26.2 UI and integrations with mods that do not exist on 1.5.2.

## Final release

`clean build` passed after the final native-extraction cleanup.

Artifact: `build/libs/controllable-0.1.0+1.5.2.jar`

SHA-256: `e70f0257e9b540638c34d702e122b9294fe34ecb59a84524d31d20b7e573b31f`
