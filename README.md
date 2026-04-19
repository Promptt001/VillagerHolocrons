# Holocron

A Skript-powered villager recording and replay system for Minecraft servers.

Holocron turns an **echo shard** into a reusable data vessel that can **record a villager's state** and later **apply that recorded state to another villager**. In practice, this functions as a villager template-transfer system backed by Skript variables and SkBee NBT handling.

This repository is intended to be both:
- a clean distribution point for the script itself, and
- a detailed design document explaining how the system works, what it stores, and what tradeoffs it intentionally makes.

## What this project does

The script introduces a custom item called a **Holocron** with two states:

- **Empty Holocron**: ready to record a villager
- **Charged Holocron**: contains a serialized villager snapshot and can overwrite another villager

Holocrons are obtained from:
- a simple admin command (`/holocron`)
- configured ore drops
- configured fishing drops

Once obtained:
1. Right-click a villager with an **empty holocron** to record it.
2. The item becomes **charged**, gains lore describing the captured villager, and stores a backing record id.
3. Right-click another villager with the **charged holocron** to overwrite that target with the recorded villager state.
4. Depending on configuration, the holocron either resets to empty or remains charged.

---

## Repository contents

A typical repository layout for this project would be:

```text
.
├── Holocron.sk
├── Holocron_annotated_audit.sk
├── README.md
└── docs/
    └── security-notes.md
```

Suggested usage:
- `Holocron.sk`: the original functional script
- `Holocron_annotated_audit.sk`: the same script with inline explanatory and audit comments
- `README.md`: this document
- `docs/security-notes.md`: optional standalone notes for operational caveats

---

## Design goals

This script prioritizes:

- **simple operator experience**
- **readable Skript logic**
- **self-contained state management**
- **clear player-facing item feedback**
- **full villager transfer behavior rather than narrow trade-copy behavior**

The design is intentionally biased toward convenience and feature expressiveness over strict hardening. That is a deliberate choice for this project.

---

## Non-goals

This project does **not** try to be:

- a minimal trade-copy-only implementation
- a protection-aware admin framework
- an anti-dupe or anti-forgery system
- a plugin-integrated villager permissions system
- a heavily abstracted Skript library

The script is focused on delivering a straightforward villager recording/apply mechanic.

---

## Core gameplay model

### Empty Holocron
The empty holocron is implemented as an **echo shard** with:
- a custom display name
- custom lore
- a persistent data tag indicating `state = empty`
- a unique `serial` tag

### Charged Holocron
When a villager is recorded, the item is converted in-place into a charged state by:
- changing the display name
- replacing the lore with a villager summary
- adding a backing record `id`
- preserving a per-item `serial`
- applying a hidden enchantment to force the enchanted glint

This gives players immediate visual confirmation that the item now contains a villager record.

---

## High-level architecture

The script has four major subsystems:

### 1. Item factory and visual state management
Responsible for:
- building an empty holocron item
- applying charged visuals
- generating lore for both empty and charged states

Primary functions:
- `makeEmptyHolocron()`
- `applyChargedHolocronVisuals(item)`
- `makeChargedHolocronLore(id)`

### 2. Trade formatting and lore summarization
Responsible for:
- converting villager trade NBT into readable text
- formatting enchantments into player-friendly strings
- rendering trade lines into lore

Primary functions:
- `formatHolocronEnchantId(id)`
- `formatHolocronEnchantLevel(level)`
- `formatHolocronItemEnchantSummary(item)`
- `formatHolocronTradeStack(itemNbt)`
- `buildHolocronTradeLore(v)`

### 3. Persistent villager snapshot storage
Responsible for:
- generating a unique record id
- storing villager NBT in Skript variables
- storing summary metadata for lore and debugging
- deleting stored records after use

Primary functions:
- `sanitizeVillagerNbt(raw)`
- `deleteHolocronRecord(id)`

Stored namespaces:
- `{holocron.nbt::%id%}`
- `{holocron.profession::%id%}`
- `{holocron.type::%id%}`
- `{holocron.level::%id%}`
- `{holocron.customname::%id%}`
- `{holocron.trades::%id%::*}`

### 4. Event handlers and player interaction flow
Responsible for:
- admin item distribution
- ore-based drops
- fishing-based drops
- villager recording
- villager overwrite/apply behavior

Primary handlers:
- `command /holocron`
- `on break`
- `on fish catch`
- `on right click on villager`

---

## Dependency model

This project assumes a server stack capable of:
- running **Skript**
- exposing persistent data tags on items
- reading and writing entity/item NBT

In practice, that means this script is designed around **Skript + SkBee** on a modern Paper-compatible server.

### Required capabilities
The script relies on the following feature classes:
- item persistent data tags
- entity NBT extraction
- NBT compound manipulation
- spawning an entity with explicit NBT
- item reconstruction from NBT for trade display

If your Skript/addon stack does not support those behaviors exactly, the script will need adaptation.

---

## Configuration surface

The script exposes its operational settings through `options:`.

### Permissions
- `holocron-admin-permission`
  - permission required to use `/holocron`

### Display names
- `holocron-empty-name`
- `holocron-charged-name`

### Acquisition chances
- `holocron-ore-drop-chance`
- `holocron-fishing-drop-chance`

### Persistent data keys
- `holocron-namespace-state`
- `holocron-namespace-id`
- `holocron-namespace-serial`

### Messaging
- `holocron-msg-prefix`

### Consumption behavior
- `holocron-reset-after-apply`
  - if `true`, applying a charged holocron turns it back into a fresh empty holocron
  - if `false`, the item remains charged after use

---

## Data model

The design uses **two storage layers**.

### Layer 1: item-level identity
Stored directly on the echo shard as persistent data tags:

- `state`
  - `empty` or `charged`
- `id`
  - the backing record id for a charged holocron
- `serial`
  - a unique identifier for the physical item instance

### Layer 2: variable-backed villager record
Stored in Skript variables under a generated record id.

This record includes:
- sanitized villager NBT
- profession text
- villager biome/type text
- villager level text
- optional custom name
- a lore-friendly summary of all visible trades

### Why both layers exist
This split allows the item itself to stay lightweight while the large payload remains in the variable store.

That architecture has three benefits:
1. lore remains responsive and readable
2. the actual villager NBT survives reloads through the variable backend
3. the charged item only needs to carry a pointer, not the full serialized payload

---

## Record flow

When a player right-clicks a villager with an **empty holocron**, the script performs the following sequence:

1. Confirm the held item is an echo shard.
2. Confirm the item has the custom holocron state tag.
3. Cancel the normal villager interaction.
4. Generate a new record id.
5. Read the villager's full NBT.
6. Sanitize that NBT by removing transient runtime/location fields.
7. Store the sanitized NBT in Skript variables.
8. Store readable summary fields for profession, type, level, custom name, and trade lore.
9. Rewrite the held item into the charged state.
10. Show the player a confirmation message.

### Why sanitization is necessary
Raw entity NBT contains fields that are not appropriate to replay verbatim on a fresh spawn, such as:
- UUID
- position
- rotation
- motion
- transient timers and state flags

The sanitization step removes several runtime-specific fields so the villager can be recreated more cleanly at a new location.

---

## Apply flow

When a player right-clicks a villager with a **charged holocron**, the script performs the following sequence:

1. Confirm the charged item contains a backing record id.
2. Confirm the backing record still exists.
3. Save the target villager's current location and facing.
4. Reconstruct the stored villager NBT from variables.
5. Delete the target villager.
6. Spawn a new villager at the target location using the stored NBT.
7. Reapply the previous target's yaw and pitch.
8. Notify the player.
9. Delete the stored backing record.
10. Either reset the item to empty or leave it charged based on config.

### Important design note
This is an **overwrite** operation, not a merge.

The target villager is replaced by a newly spawned villager based on the stored record. This is why the script behaves more like a villager transfer/template system than a small metadata editor.

---

## Trade lore rendering design

One of the strongest UX elements in the script is that a charged holocron exposes a readable summary of the captured villager directly in the item's lore.

The lore includes:
- profession
- biome/type
- villager level
- optional custom name
- a readable trade list

Trade rendering attempts to be human-friendly rather than raw-NBT-accurate.

### Enchantment formatting choices
The script converts enchantment ids such as:
- `minecraft:sharpness`
- `minecraft:unbreaking`

into labels such as:
- `Sharpness`
- `Unbreaking`

It also converts numeric levels into Roman numerals where appropriate:
- `1 -> I`
- `5 -> V`
- `10 -> X`

### Special handling for enchanted books
Enchanted books are awkward to stringify consistently across Skript versions and addon combinations. The script therefore includes defensive formatting logic that:
- trusts stored-enchant text when needed
- strips placeholder values such as `<none>`
- appends numeric levels only when they resolve cleanly

This part of the design is primarily about player readability and cross-version tolerance.

---

## Ore and fishing acquisition model

The script supports two passive acquisition paths.

### Ore drops
When a supported ore block is broken, the script can roll an empty holocron drop.

Supported ores include:
- coal ore and deepslate coal ore
- copper ore and deepslate copper ore
- iron ore and deepslate iron ore
- gold ore and deepslate gold ore
- redstone ore and deepslate redstone ore
- lapis ore and deepslate lapis ore
- emerald ore and deepslate emerald ore
- diamond ore and deepslate diamond ore
- nether quartz ore
- nether gold ore
- ancient debris

### Fishing drops
When the player catches a fish, the script rolls the configured fishing chance.

Behavior by value:
- `<= 0`: disabled
- `>= 100`: guaranteed
- otherwise: standard percentage roll

This approach makes the acquisition model easy to tune without touching core logic.

---

## Player-facing UX choices

Several implementation details are specifically about presentation quality.

### Visual distinction
Charged holocrons use:
- a distinct name
- a unique lore block
- a hidden enchantment glint

### Readability over raw data
The script stores raw NBT for replay, but players only see curated summaries.

### Item identity persistence
The item's `serial` tag provides a stable identifier for the physical item instance, even though in the current design it is primarily informational rather than enforcement-oriented.

---

## Command surface

### `/holocron`
Spawns a fresh empty holocron for the executing player.

This command is intentionally simple and mainly serves:
- testing
- admin distribution
- controlled access for private servers

---

## Operational assumptions

This design assumes the operator understands and accepts the following behavior characteristics:

- the script stores villager records in Skript variables
- the script uses near-full villager snapshots, not just trade recipes
- the script treats tagged echo shards as holocrons
- the apply path is based on target replacement rather than in-place mutation
- the script is most appropriate for private, controlled, or intentionally permissive servers

---

## Accepted tradeoffs

This repository intentionally preserves several tradeoffs in favor of simplicity and feature power.

### Full-state capture over minimal-state capture
Rather than only storing professions or offers, the script stores a broad villager snapshot and reapplies it later.

**Why:** this keeps the mechanic expressive and predictable for operators who want strong transfer behavior.

### Variable-backed payload over fully self-contained item payload
The item carries a pointer, while the full NBT lives in Skript variables.

**Why:** this keeps item metadata compact and makes lore generation easier.

### Lightweight authenticity model
The script uses tags and ids without a full registry or ownership/signature enforcement system.

**Why:** lower complexity, fewer moving parts, easier maintenance.

### Direct overwrite behavior
The target villager is replaced outright instead of selectively edited field by field.

**Why:** simpler logic, more deterministic replay of the recorded state.

These are not accidental omissions; they are part of the current design posture.

---

## Known caveats

The following are useful to understand before deploying the script:

- It is possible to treat this as a villager templating or cloning mechanic.
- The backing record store can grow over time if many charged holocrons are created and never consumed.
- Because the item is pointer-based, duplicated charged items may reference the same record.
- Compatibility should be tested on the exact Skript/SkBee/server stack in use.
- Protection, NPC, economy, or claim plugins may affect behavior depending on server setup.

For this repository, these caveats are considered understood and accepted by the maintainer.

---

## Implementation notes for maintainers

### Why `sanitizeVillagerNbt(raw)` exists
Entity NBT contains many fields that should not be replayed verbatim across locations or sessions. Sanitization removes obvious transient fields before storage.

### Why lore is stored separately from NBT
Trade lore is pre-rendered and stored independently so the charged item can present a stable, readable summary without re-parsing the villager every time the item is viewed.

### Why the charged glint uses a real enchantment
Minecraft's glint rendering is easiest to trigger by adding a harmless enchant and hiding it from the tooltip. This is reliable and keeps the item visually distinct.

### Why the code uses helper functions heavily
The helper functions make the main interaction flow much easier to reason about and modify. In Skript, this is particularly valuable because event handlers become hard to maintain when formatting and storage logic are inlined.

---

## Extending the project

If you want to grow this system later, the cleanest extension points are:

- add permissions to record/apply separately
- add owner binding to holocrons
- add expiry/cleanup commands for stored records
- add logging/audit output
- add world or region restrictions
- add a whitelist-based villager field copy model
- add support for alternate acquisition methods or crafting
- add admin inspection commands for stored record ids

Because the current script already separates item construction, lore building, storage, and interaction flow, it is a good base for incremental extension.

---

## Installation

1. Install the required server-side dependencies for your stack.
2. Place the script in your server's Skript scripts directory.
3. Reload or restart Skript.
4. Use `/holocron` to issue a test item.
5. Validate villager record/apply behavior on your exact server build before broad rollout.

---

## Recommended repository note

A short note like the following is worth keeping in the repository description or top-level notes:

> This project intentionally preserves the original gameplay and operational tradeoffs of the Holocron mechanic. The annotated version documents design assumptions and caveats, but the script is not positioned as a hardened public-economy solution.

---

## Summary

Holocron is a compact but capable Skript system built around one core idea:

**capture a villager, carry its identity in an item, and reapply that identity elsewhere.**

Its implementation combines:
- custom item identity via persistent data tags
- variable-backed NBT storage
- readable lore summarization
- event-driven acquisition and interaction logic

For servers that want that mechanic intentionally and understand the tradeoffs, it is a clean and practical solution.
