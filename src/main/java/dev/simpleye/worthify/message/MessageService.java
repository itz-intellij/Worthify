package dev.simpleye.worthify.message;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.gui.ColorUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class MessageService {

    private final WorthifyPlugin plugin;
    private volatile YamlConfiguration lang;

    public MessageService(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        this.lang = plugin.getConfigManager().getLangConfig();
    }

    public String get(String key) {
        YamlConfiguration cfg = lang;
        if (cfg == null || key == null || key.isEmpty()) {
            return "";
        }
        return cfg.getString(key, "");
    }

    public String format(String key, Map<String, String> placeholders) {
        String raw = get(key);
        if (raw.isEmpty()) {
            return "";
        }

        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                String k = e.getKey();
                if (k == null || k.isEmpty()) {
                    continue;
                }
                String v = e.getValue() == null ? "" : e.getValue();
                raw = raw.replace("{" + k + "}", v);
                raw = raw.replace("${" + k + "}", v);
            }
        }

        return ColorUtil.colorize(raw);
    }

    public void send(CommandSender sender, String key) {
        if (sender == null) {
            return;
        }

        YamlConfiguration cfg = lang;
        if (cfg != null && cfg.isList(key)) {
            for (String line : cfg.getStringList(key)) {
                String colored = ColorUtil.colorize(line);
                if (!colored.isEmpty()) {
                    sender.sendMessage(colored);
                }
            }
            return;
        }

        String msg = format(key, null);
        if (!msg.isEmpty()) {
            sender.sendMessage(msg);
        }
    }

    public void send(CommandSender sender, String key, Object... placeholderPairs) {
        if (sender == null) {
            return;
        }
        Map<String, String> map = new HashMap<>();
        if (placeholderPairs != null) {
            for (int i = 0; i + 1 < placeholderPairs.length; i += 2) {
                Object k = placeholderPairs[i];
                Object v = placeholderPairs[i + 1];
                if (k == null) {
                    continue;
                }
                map.put(k.toString().toLowerCase(Locale.ROOT), v == null ? "" : v.toString());
            }
        }
        String msg = format(key, map);
        if (!msg.isEmpty()) {
            sender.sendMessage(msg);
        }
    }
}
