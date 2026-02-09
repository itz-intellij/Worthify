package dev.simpleye.worthify.worth;

import dev.simpleye.worthify.compat.MaterialResolver;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.EnumMap;
import java.util.Map;

public final class WorthManager {

    private volatile EnumMap<Material, Double> prices = new EnumMap<>(Material.class);

    public void reload(YamlConfiguration pricesConfig, MaterialResolver materialResolver, java.util.function.Consumer<String> warnLogger) {
        ConfigurationSection section = pricesConfig.getConfigurationSection("prices");
        EnumMap<Material, Double> next = new EnumMap<>(Material.class);

        if (section != null) {
            for (Map.Entry<String, Object> entry : section.getValues(false).entrySet()) {
                Material material = materialResolver == null ? Material.matchMaterial(entry.getKey()) : materialResolver.resolve(entry.getKey());
                if (material == null) {
                    if (warnLogger != null) {
                        String aliasTarget = materialResolver == null ? null : materialResolver.getAliasTarget(entry.getKey());
                        if (aliasTarget != null) {
                            warnLogger.accept("Unknown material in prices.yml: '" + entry.getKey() + "' (alias tried: '" + aliasTarget + "')");
                        } else {
                            warnLogger.accept("Unknown material in prices.yml: '" + entry.getKey() + "'");
                        }
                    }
                    continue;
                }

                Double value = toDouble(entry.getValue());
                if (value == null) {
                    continue;
                }

                next.put(material, value);
            }
        }

        this.prices = next;
    }

    public void reload(YamlConfiguration pricesConfig) {
        reload(pricesConfig, null, null);
    }

    public double getUnitPrice(Material material) {
        Double value = prices.get(material);
        return value == null ? 0.0D : value;
    }

    public Map<Material, Double> getPricesSnapshot() {
        return new EnumMap<>(prices);
    }

    private static Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
