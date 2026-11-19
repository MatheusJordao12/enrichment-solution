# Changelog

All notable changes to Enrichment Solution. Format based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); this project adheres to
[Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0]

First public release — "the basics done well." Focused, polished industrial machinery, geothermal
binary-cycle power generation, conduits, and an autonomous mining robot for **Minecraft 1.21.1 (NeoForge)**.

### Added

- **In-Game Guide Book (`Enrichment Guide`)**: automatically delivered to players on their first
  world join (and craftable), covering ores, energy, conduits, machines, the geothermal fluid
  chain, and the autonomous robot.
- **Quad Enrichment Chamber**: four Enrichment Chambers placed in a 2×2 merge automatically into
  a multiblock running four parallel channels (50 ticks per operation) with auto-sort.
- **PCB Printer**: manufactures circuits from three unordered input slots (Robot, Basic, Advanced,
  Ultimate, and Ultra Circuits).
- **Stamping Press**: crafts Pressed Aluminium and Mechanical Eyes.
- **Geothermal binary-cycle power chain**:
  - **Fluid Pump**: scans down up to 64 blocks to pump water or crude oil.
  - **Oil Refinery**: refines Crude Oil into Liquid Isobutane.
  - **Water Tank multiblock**: up to 20×20×20 with dedicated input/output ports, 200,000 mB per
    block, and a heat multiplier from depth, nearby lava/magma, and desert biomes.
  - **Heat Exchanger multiblock**: up to 20×20×20, boils Liquid Isobutane into Gaseous Isobutane
    using hot water.
  - **Geothermal Generator**: burns Gaseous Isobutane (20 FE per mB, up to 1,000 FE/t) and outputs
    exhaust gas.
  - **Condenser**: cools exhaust Gaseous Isobutane back into Liquid Isobutane using cooling water.
- **Solar Panel upgrade**: right-click a Solar Panel with a **Solar Upgrade Template** to upgrade
  it into the 2-panel advanced form (150 FE/t clear, 40 FE/t rain).
- **Energy Cells** in four tiers (Basic, Advanced, Ultimate, Ultra) with per-face
  input/output/disabled configuration, retained energy on block items, and Robot power support.
- **Conduits**:
  - **Energy Cables** in four tiers (Basic, Advanced, Ultimate, Ultra).
  - **Fluid Pipes** in four tiers (Basic, Advanced, Ultimate, Ultra) with per-block heat retention.
  - **Thermal Pipes** in four tiers (Isolated, Reinforced, Vacuum, Cryogenic) with high heat
    retention up to 100%.
- **Configurator**: cycles push/pull/none on conduits and input/output/disabled on machine faces.
- **Hi-Tech Goggles**: head-slot wearable with an inspector HUD showing fluid name, amount,
  temperature, thermal multiplier, or cable energy storage within 4 blocks.
- **Autonomous Robot & Robot Charger**: mining, item collection, follow/stay modes, owner defence,
  jetpack, energy-cell power, charger docking, and the **Auto Recharger Upgrade Template**.
- **Worldgen**: natural ore generation for **Steel**, **Bauxite**, and **Aluminium** ores (stone
  and deepslate variants), plus Crude Oil lakes and underground pockets.
- **Advancement tree**: guides progression from Machine Casing through machines, power, and gear.
- **JEI integration**: Enrichment, PCB Printing, Oil Refining, and Stamping recipe categories with
  FE/t, duration, and total FE cost.
- **Mining tags & localization**: full `minecraft:mineable/pickaxe` and tool-tier tag coverage,
  dedicated server compatibility, and complete English (`en_us`) and Brazilian Portuguese (`pt_br`)
  translations.

### Changed

- Machine faces are configurable (input / output / disabled) from the GUI, and conduits expose
  push / pull / none per side.
- Fluid temperature and thermal multiplier travel with fluids via data components, cooling down
  along pipe runs according to pipe tier.
- Version bumped to `1.0.0` for the first public release.

### Fixed

- Separated client-only HUD code from the common network layer so dedicated servers run cleanly
  without attempting to load client classes.
- Added missing `minecraft:mineable/pickaxe` tags to Steel Ore, Deepslate Steel Ore, and Thermal
  Pipes.
- Completed missing `pt_br` localization keys.

### Removed / Deferred

- **Reactor Core**, **Reactor Casing**, **Meteorotite**, **Oxidated Plutonium**, and **Unstable
  Old Uranium** were deferred and removed from the `1.0.0` release to keep the initial public
  release focused and polished.

## [0.1.0] — initial alpha

First playable alpha of the mod.

### Added

- Machine Casing as the shared crafting base.
- Initial ores, raw items, ingots, and smelting/blasting recipes.
- Enrichment Chamber: Raw Uranium → Enriched Uranium (4,000 FE per operation).
- Solar Panel with clear/rain generation and an internal energy buffer.
- Energy cables for moving FE between machines.
- Creative tab, block/item models, loot tables, and initial English/Portuguese translations.
