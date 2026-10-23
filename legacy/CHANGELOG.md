# Changelog

## 0.1.1 — 2026-10-22

- Apply held buttons and one-shot actions to their actual Minecraft key-binding
  objects. Forge 1.5.2's key-code lookup retains only one binding per key, so
  Galacticraft/Flan's Mod Space aliases could receive jump instead of Minecraft.
- Give an active stick priority on its movement axis. An opposite keyboard/mod
  flag can no longer cancel backward movement. Keyboard movement still works
  when the corresponding stick axis is centered.
- Add duplicate-binding unit tests and in-game checks for actual backward
  displacement and jump height, including a conflicting forward binding.
- Verify the jump fix with a separate copy of the local V0LTZ pack (90 loaded
  mods), using a generated test world. Installed instance files are unchanged.
