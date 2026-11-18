# Changelog

All notable changes to Enrichment Solution. Format based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/); the project uses semantic versioning with a
pre-release suffix while in beta.

## [0.2.0-beta] — unreleased

Public beta target. Adds the full mid-game tech tree on top of the 0.1.0 alpha.

### Added

- **Quad Enrichment Chamber**: four Enrichment Chambers in a 2×2 merge automatically into a multiblock
  that runs four parallel channels (50 ticks per operation) with auto-sort.
- **PCB Printer** with five unordered-input recipes: Robot, Basic, Advanced, Ultimate and Ultra
  Circuit.
- **Stamping Press**: Pressed Aluminium and Mechanical Eyes.
- **Geothermal binary cycle**: Fluid Pump (water/crude oil), Oil Refinery (crude oil → liquid
  isobutane), Heat Exchanger multiblock (liquid → gaseous isobutane using hot water), Geothermal
  Generator (20 FE per mB of gas) and Condenser (exhaust gas back to liquid using cooling water).
- **Water Tank multiblock** up to 20×20×20 with dedicated input/output ports, 200,000 mB per block and
  a heat multiplier derived from depth, lava/magma nearby and desert biomes.
- **Thermal pipes** (Isolated, Reinforced, Vacuum, Cryogenic) alongside the four standard fluid pipe
  tiers, with per-block heat retention up to 100 %.
- **Energy cells** in four tiers with per-face input/output/disabled buttons; the block item keeps its
  stored energy and the cells can power the Robot.
- **Configurator** to cycle push/pull/none on conduits and input/output/disabled on machine faces.
- **Hi-Tech Goggles** with an inspector HUD: fluid name, amount, temperature, thermal multiplier and
  cable charge for the block under the crosshair within 4 blocks.
- **Robot**: mining, item collection, follow/stay, owner defence, jetpack, energy-cell power, Robot
  Charger docking and the Auto Recharger upgrade.
- **Solar Panel upgrade** with the Solar Upgrade Template (2-panel advanced form: 150 FE/t clear,
  40 FE/t rain).
- **Reactor Core** burning Enriched Uranium (512 FE/t, 1,200 ticks per fuel item) with heat tracking.
- **Advancement tree** from the first Machine Casing to the Reactor Core and the goggles.
- **JEI integration**: Enrichment, PCB Printing, Oil Refining and Stamping categories with FE/t,
  duration and total cost.
- **Mining tags**: every machine, conduit, energy cell, tank part, casing, reactor block and ore is
  now `minecraft:mineable/pickaxe`; machines and the steel/deepslate ores also require an iron-tier
  pickaxe to drop.
- Datapack crafting recipes for machines, conduits, cells, tools and the robot.

### Changed

- Machine faces are configurable (input / output / disabled) from the GUI, and conduits expose
  push / pull / none per side.
- Fluid temperature and thermal multiplier travel with the fluid itself (data components), so hot
  water cools down along a pipe run according to the pipe tier.
- Version bumped to `0.2.0-beta`.

### Fixed

- The common network layer no longer references client classes. The goggles HUD is registered as a
  sink by the client setup, so a dedicated server no longer attempts to load `client.InspectHud`.
- Steel Ore, Deepslate Steel Ore and the four thermal pipes were missing from
  `minecraft:mineable/pickaxe`, so they could not be harvested at all.

### Known issues

- Machine processing recipes are still hardcoded in the BlockEntities (datapack recipes pending); only
  crafting recipes are JSON today.
- Reactor Core structure validation is a placeholder (`isStructureValid` always returns `true`).
- Energy and time costs are compile-time constants; no server config yet.
- Balance and TPS stress testing are still in progress.
- `pt_br` is missing the configurator and conduit-mode keys.

## [0.1.0] — initial alpha

First playable alpha of the mod.

### Added

- Machine Casing as the shared crafting base.
- Steel, Bauxite, Aluminium, Meteorotite, Unstable Old Uranium and Oxidated Plutonium ores (stone and
  deepslate variants), raw items, ingots and smelting/blasting recipes.
- Enrichment Chamber: Raw Uranium → Enriched Uranium (4,000 FE per operation).
- Solar Panel with clear/rain generation and an internal energy buffer.
- Energy cables for moving FE between machines.
- Reactor Core burning Enriched Uranium.
- Creative tab, block/item models, loot tables and English/Portuguese translations.
