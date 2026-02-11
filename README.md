# Worthify

Worthify is a lightweight Paper plugin that assigns a configurable "worth" value to items and lets players sell items for in-game currency.

- **Server software**: Paper / Spigot-compatible (built against Paper API)
- **Minecraft version**: `1.21.x` (see `api-version` in `plugin.yml`)
- **Optional integrations**:
  - Vault (economy)             
  - PlaceholderAPI
  - ProtocolLib (client-side "Worth" tooltip on items without breaking stacking)

## Features

- Sell items from your hand or sell all sellable items
- Configurable item worth values
- Sell history GUI
- Worth multiplier system (global + category + per-material overrides)
- Multiplier GUI (`/multiplier`) to view/edit category multipliers (only works when enabled)
- Simple admin tooling for reloading without restarting

## Commands

- **`/sell [hand|all]`**
  - **Permission**: `worthify.sell`
- **`/pay <player> <amount>`**
  - **Permission**: `worthify.sell`
- **`/worth [page]`**
  - **Permission**: `worthify.sell`
- **`/sellhistory [page]`**
  - **Permission**: `worthify.sell`
- **`/multiplier`** *(alias: `/mult`)*
  - **Permission**: `worthify.sell`
  - **Notes**: Only opens when `worth_multiplier.enabled: true`. Everyone can view, but editing requires `worthify.admin`.
- **`/setworth <number>`**
  - **Permission**: `worthify.admin`
- **`/worthify reload`**
  - **Permission**: `worthify.admin`

## Permissions

- **`worthify.sell`**
  - Default: `true`
- **`worthify.admin`**
  - Default: `op`

## Installation

1. Download the latest `Worthify` jar from:
   - Modrinth: *(https://modrinth.com/project/worthify)*
2. Put the jar into your server’s `plugins/` folder.
3. (Optional) Install **Vault** and an economy plugin (e.g. EssentialsX Economy) to use your server's main economy.
   - If Vault is not installed, Worthify will fall back to its own internal economy (balances stored in `balances.yml`).
4. Restart the server.

## Configuration

Worthify writes config files into `plugins/Worthify/`.

- Item values are stored in the plugin’s prices config (see `plugins/Worthify/`).
- After editing configs you can reload with:
  - `/worthify reload`

### Worth multiplier

Worthify can multiply sell payouts and displayed worth values.

- The multiplier only applies when `worth_multiplier.enabled: true`.
- Final multiplier per item is:
  - `worth_multiplier.value` × `worth_multiplier.categories.<category>` × `worth_multiplier.materials.<MATERIAL>` (optional override)

Example:

```yml
worth_multiplier:
  enabled: true
  value: 1.0
  categories:
    ores: 2.0
    food: 1.0
    seeds: 0.5
  materials:
    DIAMOND: 3.0
```

### Multiplier GUI

- `/multiplier` opens the GUI (only when enabled)
- Everyone can open/view when enabled
- Only `worthify.admin` can change category multipliers

## Update checker & Auto-updater

Worthify can automatically check for new releases on Modrinth and optionally download updates.

- **Update checker** (enabled by default):
    - Periodically queries Modrinth for the latest version.
    - Logs a console warning when an update is available.
    - Notifies OPs (or players with `worthify.update`) on join.

- **Auto-updater** (disabled by default, opt-in):
    - When enabled, Worthify will download the latest jar into `plugins/update/Worthify.jar`.
    - The update is applied on the **next server restart**.
    - A clear warning is printed on startup when auto-updater is enabled.

### Config

```yml
update_checker:
  enabled: true
  modrinth_project: "worthify"
  interval_minutes: 360
  notify_on_join: true

auto_updater:
  enabled: false
```

## Building

This project uses Gradle.

```bash
./gradlew clean build
```

The built jar will be in:

- `build/libs/`

## Support / Links

- **Issues**: *(https://github.com/S1mple-ye/Worthify/issues)*
- **Modrinth**: *(https://modrinth.com/plugin/worthify)*

## License

MIT — see [`LICENSE`](./LICENSE).
