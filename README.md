# VillagerHolocrons (PaperMC)

This project is a PaperMC plugin port of the attached `VillagerHolocrons.sk` Skript project, built from the provided PaperMC plugin template.

## What it does

- Adds an **Empty Holocron** item based on an echo shard.
- Empty holocrons can drop from configured ores and from fishing.
- Right-click a villager with an empty holocron to record its:
  - profession
  - villager type
  - villager level
  - villager experience
  - custom name
  - visible trades
- Right-click another villager with a charged holocron to overwrite that villager with the stored data.
- Stores holocron records in `plugins/VillagerHolocrons/records.yml` so they survive restarts.

## Commands

- `/holocron` - gives the player an empty holocron

## Permission

- `holocron.admin`

## Config

Main settings are in `config.yml`:

- `messages.*`
- `permissions.admin`
- `items.empty-name`
- `items.charged-name`
- `items.reset-after-apply`
- `chances.ore-drop`
- `chances.fishing-drop`
- `ore-materials`


## Reference source

The original attached Skript project is included under `reference/` for comparison during migration:

- `reference/VillagerHolocrons.sk`
- `reference/README-original.md`

## Build

```bash
./gradlew build
```

The plugin jar will be created in:

```bash
app/build/libs/
```

## Notes about the port

This Paper port intentionally uses the public Paper/Bukkit API instead of Skript + SkBee raw villager NBT copying. That means the plugin reproduces the gameplay-facing parts of the Skript reliably, while avoiding brittle direct-NBT internals.

The port currently copies the villager state that is directly accessible from the Paper API: profession, type, level, XP, custom name, and merchant recipes.
