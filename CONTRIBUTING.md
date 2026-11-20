# Contributing to Enrichment Solution

Thank you for your interest in contributing! This project is licensed under the [MIT License](LICENSE).

## Project Structure
- `com.enrichmentsolution.mod.common` — Gameplay logic, blocks, items, block entities, entities, menus, recipes, configs, and networking (must never import `client` classes).
- `com.enrichmentsolution.mod.client` — Screens, renderers, models, CTM, and client-only setup (`Dist.CLIENT`).
- `com.enrichmentsolution.mod.registry` — Deferred registers for blocks, items, block entities, menus, entities, fluids, and data components.
- `com.enrichmentsolution.mod.datagen` — Data generators (`runData`) outputting to `src/generated/resources/`.
- `com.enrichmentsolution.mod.gametest` — Automated NeoForge GameTests (`runGameTestServer`).

## Building & Testing
Requires **Java 21** (adjust `org.gradle.java.home` in `gradle.properties` or pass `-Dorg.gradle.java.home=...` if needed):

```bash
./gradlew runData --no-daemon
./gradlew build --no-daemon
./gradlew runGameTestServer --no-daemon
```

## Guidelines
- Keep client-only code strictly inside `client/` and gated behind `Dist.CLIENT`.
- Keep `en_us.json` and `pt_br.json` at 100% key parity.
- Prefer datagen (`ModDataGenerators`) over hand-writing blockstates, loot tables, tags, or recipes.
