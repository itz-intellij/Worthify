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
  - `/_worthify reload_`

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
