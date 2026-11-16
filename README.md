# CocoVis 0.0.1 — Reference UI Update

Client-side Fabric mod for Minecraft 26.3.

## UI changes
- Reworked Visuals screen toward the supplied references: graphite/near-black surfaces, blue active state, restrained borders and compact toggles.
- Sidebar category navigation with module counts.
- Search field filters the selected category by module name and description.
- Two-column feature layout with per-module settings actions.
- Updated shared UI palette and the draggable info HUD panel.
- Existing module behaviour and configuration remain wired to the original ModuleManager.

## Build
Requires JDK 25. The repository GitHub Actions workflow builds with Gradle and uploads the JAR artifact.

```bash
gradle build --no-daemon
```

The compiled JAR is written to `build/libs/`.
