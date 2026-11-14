# CocoVis 0.2.0

Client-side Fabric mod for Minecraft 26.3.

## Features
- **Clan nametags**: `Nick ▎ ✦ CLAN`, clan is read from the TAB list.
- **Diamond HUD**: dark panel with the diamond count (diamond block = 9).
- **Price tooltips**: "Цена: N алм." on items, prices in `config/cocovis.json`.
- **Motion blur**: bundles Natural Motion Blur 1.4.5+mc26.3 (LGPL-3.0, by Pasi),
  same as the old CocoVis 0.9.0. Needs **YetAnotherConfigLib (YACL) >= 3.9.6** in `mods`.

## Build (no own repo setup needed)
1. Create an empty GitHub repo, upload everything from this folder (keep `.github/`).
2. Actions tab -> the `build` run -> download the `cocovis` artifact.
3. Put the jar into `.minecraft/mods` with Fabric API 0.161.0+26.3 and YACL.

## Config: `.minecraft/config/cocovis.json`
`clanPattern` (group 1 = clan), `showDiamondHud`, `hudX`, `hudY`, `hudScale`,
`showTooltips`, `prices`. In JSON every backslash is doubled, e.g. `^\\[([^\\]]+)\\]`.
