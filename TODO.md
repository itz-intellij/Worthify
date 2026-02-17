# Worthify - TODO

## Version support

- [ X ] Support **multiple Minecraft versions** ("all versions support") via a defined compatibility policy (e.g. latest + LTS) ( IDK about that maybe 1.21.X is decent so im gonna make it as done )

## Working On It

- [ ] Ensure every command retu- [ ] Add database-backed sell history (SQLite)rns consistent usage + permission errors
- [ ] Add categories and sorting to `/worth` GUI
- [ ] Add audit logging for admin changes (`/setworth`)
- [ ] PlaceholderAPI placeholders
- [ ] Add API for other plugins (expose worth lookup + sell events)

## Done :)

- [ X ] Add more guardrails (validate worth values, prevent negatives)
- [ X ] Add `/worthify version` and `/worthify help`
- [ X ] Add `worthify.*` permission (optional) for admins
- [ X ] Add better messages + configurable message file
- [ X ] Improve Balance Top ( make it to be a gui with a clean look )
- [ X ] Improve config.yml file
- [ X ] Make Sellgui , Sellhistory , BalanceTop guis Editable ( Each One Of Them Should Have A File Cause The Config Looks Missy :) )
- [ X ] Add All Items 
- [ X ] Add config validation + safe defaults (avoid missing config keys)
- [ X ] Add database-backed sell history (SQLite)
- [ X ] Add localization support (message keys + language files)