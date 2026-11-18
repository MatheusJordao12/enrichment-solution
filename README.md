# Enrichment Solution

A machinery + nuclear power mod for **Minecraft 1.21.1** on **NeoForge**, built around ore enrichment,
circuit manufacturing, a geothermal binary-cycle power chain, industrial fluid transport and an
autonomous mining robot.

> **Status: beta (`0.2.0-beta`).** Everything listed here works in game, but balance numbers are not
> final and dedicated-server validation is still in progress. Back up your world before updating.

---

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loader | NeoForge `21.1.x` (built against `21.1.266`) |
| Java | 21 |
| Side | Client **and** server (works on dedicated servers) |
| JEI | Optional — adds four recipe categories |

No other mod is required. Everything is registered under the `enrichment_solution` namespace.

---

## Features

### Ore processing and materials

- Ores: **Steel**, **Bauxite**, **Aluminium**, **Meteorotite**, **Unstable Old Uranium** and
  **Oxidated Plutonium** (stone + deepslate variants, except Meteorotite), plus raw ore items,
  ingots, blasting/smelting recipes and world generation.
- Machine Casings are the shared crafting base of most machines (PCB Printer, Stamping Press, Oil
  Refinery, Heat Exchanger, Geothermal Generator, Condenser, Fluid Pump, Robot Charger and the energy
  cells).

### Enrichment Chamber

Turns **Raw Uranium into Enriched Uranium**, the fuel of the reactor.

- 50,000 FE buffer, accepts up to 2,000 FE/t
- 40 FE/t over 100 ticks → **4,000 FE per operation**
- Per-face I/O configuration, with one input and one output slot

**Quad Chamber:** place four Enrichment Chambers in a 2×2 (same facing) and they merge automatically
when the last one is placed. The Quad Chamber runs **four parallel channels** at 50 ticks per
operation, exposes 4 input + 4 output slots in a single GUI and has an **auto-sort** toggle. Opening
any of the four blocks opens the master's GUI; breaking one unforms the whole structure.

### PCB Printer

Manufactures circuits from 3 unordered input slots (just put the ingredients in, order does not matter).

- 20 FE/t over 100 ticks → **2,000 FE per print**
- Recipes:
  - **Robot Circuit** — 2 Steel Ingot + 2 Aluminium Ingot + 2 Redstone
  - **Basic Circuit** — 1 Redstone + 1 Iron Ingot + 1 Aluminium Ingot
  - **Advanced Circuit** — 1 Gold Ingot + 1 Aluminium Ingot + 1 Basic Circuit
  - **Ultimate Circuit** — 1 Diamond + 1 Advanced Circuit + 1 Gold Ingot
  - **Ultra Circuit** — 1 Diamond + 1 Ultimate Circuit + 1 Netherite Ingot

### Stamping Press

- 100,000 FE buffer, 25 FE/t over 80 ticks → **2,000 FE per operation**
- 4 input slots + 1 output slot
- **Pressed Aluminium** — 1 Aluminium Ingot
- **Mechanical Eyes** — 1 Pressed Aluminium + Glowstone Dust + Robot Circuit + Glass Pane

### Fluid chain (geothermal binary cycle)

The power chain is a closed loop: crude oil → liquid isobutane → gaseous isobutane → FE → back to
liquid isobutane.

1. **Fluid Pump** — scans up to 64 blocks straight down and pumps the first water or crude oil source
   it finds. 100,000 FE buffer, 16,000 mB tank, 200 FE per 10-tick cycle (1,000 mB per cycle).
2. **Oil Refinery** — 100 mB Crude Oil → 50 mB Liquid Isobutane, 40 FE/t over 25 ticks
   (**1,000 FE per operation**), two 16,000 mB tanks, auto-push to neighbours.
3. **Water Tank (multiblock)** — stores water and computes a heat multiplier from its surroundings:
   Y 80 ≈ 1×, Y −50 ≈ 20×, plus Lava nearby ×3, Magma blocks ×2.25, Desert biome ×1.25.
   200,000 mB per block, structures up to 20×20×20, with dedicated **Tank Water Input** and
   **Tank Water Output** ports.
4. **Heat Exchanger (multiblock)** — boils Liquid Isobutane into Gaseous Isobutane using hot water.
   8,000 mB water + 16,000 mB isobutane per block, connected blocks merge into one structure (up to
   20×20×20) and scale the boil rate: 5 mB/t per block at 1× multiplier, clamped between 5 and
   50 mB/t per block.
5. **Geothermal Generator** — burns Gaseous Isobutane at up to 50 mB/t and **20 FE per mB**
   (up to 1,000 FE/t). 200,000 FE buffer, 2,000 FE/t output, 16,000 mB tanks for input gas and for
   the exhaust gas that it pushes out.
6. **Condenser** — closes the loop: Gaseous Isobutane + cooling water (**1 mB water per 5 mB gas**)
   → Liquid Isobutane, 1:1 with no loss, up to 50 mB/t.

### Power generation and storage

- **Solar Panel** — 60 FE/t in clear weather, 15 FE/t while raining; 150,000 FE buffer, 1,000 FE/t
  output. Right-click with a **Solar Upgrade Template** to convert it into the 2-panel advanced form
  (150 FE/t clear, 40 FE/t rain) with a transformation animation.
- **Reactor Core** — burns **Enriched Uranium** (1,200 ticks ≈ 60 s per item) for **512 FE/t**.
  1,000,000 FE buffer, 10,000 FE/t output, heat readout in the GUI. Reactor Casing is the matching
  structural block.
- **Energy Cells** (Basic / Advanced / Ultimate / Ultra) — 1M / 4M / 16M / 64M FE storage with
  2,000 / 8,000 / 32,000 / 128,000 FE/t per-face transfer and per-face input/output/none buttons.
  The block item keeps its charge when broken.

### Conduits

**Energy Cables** — 2,000 / 8,000 / 32,000 / 128,000 FE/t throughput with internal buffers.

**Fluid Pipes** — buffer, throughput and heat retention per block:

| Tier | Buffer | Throughput | Heat retained per block |
|---|---|---|---|
| Basic | 1,000 mB | 200 mB/t | 96.0 % |
| Advanced | 4,000 mB | 1,000 mB/t | 97.0 % |
| Ultimate | 16,000 mB | 4,000 mB/t | 98.0 % |
| Ultra | 64,000 mB | 16,000 mB/t | 98.5 % |

**Thermal Pipes** (insulated) — the same flow mechanics with much better heat retention, so hot
fluids stay hot over long runs:

| Tier | Buffer | Throughput | Heat retained per block |
|---|---|---|---|
| Isolated | 2,000 mB | 500 mB/t | 99.5 % |
| Reinforced | 8,000 mB | 2,000 mB/t | 99.75 % |
| Vacuum | 32,000 mB | 8,000 mB/t | 99.9 % |
| Cryogenic | 128,000 mB | 32,000 mB/t | 100 % |

Every conduit and machine face can be configured to **push**, **pull** or **none** (machines:
input / output / disabled) from its screen or with the Configurator.

### Tools and gear

- **Hi-Tech Goggles** (head slot) — while worn, look at a fluid pipe or energy cable within
  **4 blocks** and a HUD appears next to the crosshair showing the fluid name, amount, temperature
  and thermal multiplier, or the stored/max FE of a cable.
- **Configurator** — right-click a cable/pipe connection to cycle push → pull → none, or a machine
  face to cycle input → output → disabled. Sneak to cycle backwards.
- **Robot** — a companion machine with 1 tool slot, 9 storage slots and 3 energy-cell slots (it runs
  on Energy Cell items). It follows or stays, mines stone columns with the pickaxe you give it,
  collects the drops, defends its owner, uses a jetpack to close distance (60 FE per block) and can
  dock at the **Robot Charger**. Installing the **Auto Recharger Upgrade Template** lets it return to
  the charger on its own when the cells run low. Only the owner can open it.
- **Fluids**: Crude Oil, Liquid Isobutane and Gaseous Isobutane, with buckets for the first two.
- **Advancements** walk a new player from the first Machine Casing to the Reactor Core.

---

## JEI integration

When JEI is installed, four categories are added: **Enrichment**, **PCB Printing**, **Oil Refining**
and **Stamping**. Each page shows the exact FE/t, duration and total FE of the operation, and the
machine appears as a recipe catalyst.

---

## Building from source

```bash
./gradlew build          # Windows: gradlew.bat build
```

The jar is written to `build/libs/enrichment_solution-0.2.0-beta.jar`.

Dev runs:

```bash
./gradlew runClient      # client with the mod loaded
./gradlew runServer      # dedicated server
./gradlew runData        # data generation into src/generated/resources
```

`gradle.properties` pins a JDK 21 through `org.gradle.java.home`. If your JDK 21 lives somewhere
else, adjust that line before building.

## Installing

1. Install Minecraft 1.21.1 + NeoForge 21.1.x.
2. Drop the jar into `.minecraft/mods/` (client and/or server).
3. JEI is optional and only needed client-side.

Modpacks are welcome. License: MIT (as declared in `gradle.properties`).

---

## Known limitations (beta)

- Machine processing recipes are still hardcoded in the BlockEntities; datapack-driven recipes are
  planned. Only crafting recipes are datapack JSON today.
- `isStructureValid` on the Reactor Core is a placeholder, so the core burns fuel without validating
  a casing shell.
- Energy/time costs are compile-time constants (no server config yet); balance is subject to change.
- The Portuguese translation is missing a few keys (configurator and conduit mode strings).
- Mining tags cover every machine, conduit, cell, tank part and ore. Tier requirements
  (`needs_iron_tool`) currently cover machines and the deepslate/steel ores only.

## Reporting issues

Use the issue template in `.github/ISSUE_TEMPLATE/bug_report.md` and include your NeoForge version,
the mod version, the `latest.log` and, if possible, the steps to reproduce.
