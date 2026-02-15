 Worthify

Worthify is a lightweight Paper plugin that assigns a configurable worth to items and lets players sell items for in-game currency.

## Features

- Sell items from your hand or sell all sellable items
- Configurable item worth values (`prices.yml`)
- Sell history GUI (`/sellhistory`)
- Player-to-player payments (`/pay`)
- Optional client-side item tooltip worth line (requires ProtocolLib)
- Optional Vault integration (uses your server economy if available)
- Standalone internal economy fallback if Vault isn’t installed (`balances.yml`)
- Worth multiplier system (global + category + per-material overrides)
- Multiplier GUI (`/multiplier`) to view/edit category multipliers (only works when enabled)

## Commands

- `/sell [hand|all]`
  - Permission: `worthify.sell`
- `/pay <player> <amount>`
  - Permission: `worthify.sell`
- `/worth [page]`
  - Permission: `worthify.sell`
- `/sellhistory [page]`
  - Permission: `worthify.sell`
- `/multiplier` (alias: `/mult`)
  - Permission: `worthify.sell`
  - Notes: Only opens when `worth_multiplier.enabled: true`. Everyone can view, but editing requires `worthify.admin`.
- `/setworth <number>`
  - Permission: `worthify.admin`
- `/worthify reload`
  - Permission: `worthify.admin`

## Permissions

- `worthify.sell`
  - Default: true
- `worthify.admin`
  - Default: op

## Dependencies

- Required: none
- Optional:
  - Vault (to use an external economy provider such as EssentialsX Economy)
  - ProtocolLib (to display a client-side `Worth: $X` tooltip line on items without breaking stacking )

## Configuration

Worthify writes config files into `plugins/Worthify/`.

- `config.yml`
  - `economy.internal.enabled`: enable internal economy fallback when Vault is missing
  - `economy.internal.starting_balance`: default balance for players not in `balances.yml`
  - `worth_lore.enabled`: enable worth tooltip injection (requires ProtocolLib)
  - `worth_lore.line`: format line (supports `{worth}` or `${worth}`)
  - `worth_multiplier.enabled`: enables worth multiplier logic and the multiplier GUI
  - `worth_multiplier.value`: global multiplier applied to all items
  - `worth_multiplier.categories.*`: category multipliers (ores/food/seeds)
  - `worth_multiplier.materials.*`: per-material multiplier overrides
- `prices.yml`
  - Stores per-material prices under `prices.<MATERIAL_NAME>`
- `sellhistory.yml`
  - Stores per-player sell history entries
- `balances.yml`
  - Stores per-player balances when using internal economy

## Notes

- If Vault is installed and an economy provider is available, Worthify will use it.
- If Vault is not installed, Worthify will fall back to its internal economy automatically (if enabled in config).
