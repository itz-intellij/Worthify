# Configuration

Worthify writes config files to:

`plugins/Worthify/`

## Important sections

- `prices.yml` for item values
- `config.yml` for plugin behavior
- `lang/*.yml` for messages
- `gui/*.yml` for GUI layouts

## Worth multiplier

The multiplier applies only when enabled.

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

Final item multiplier:

`global value × category value × material override (if present)`
