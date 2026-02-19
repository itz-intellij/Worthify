package dev.simpleye.worthify.command;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.message.MessageService;
import dev.simpleye.worthify.selltools.SellToolType;
import dev.simpleye.worthify.selltools.SellToolUtil;
import dev.simpleye.worthify.selltools.TimeUnitParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SellWandCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "worthify.selltools.give";

    private final WorthifyPlugin plugin;
    private final SellToolUtil util;

    public SellWandCommand(WorthifyPlugin plugin, SellToolUtil util) {
        this.plugin = plugin;
        this.util = util;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageService messages = plugin.getMessages();

        if (!sender.hasPermission(PERMISSION) && !sender.hasPermission("worthify.admin") && !sender.isOp()) {
            if (messages != null) {
                messages.send(sender, "errors.no_permission");
            } else {
                sender.sendMessage(ChatColor.RED + "No permission.");
            }
            return true;
        }

        // /sellwand usage <uses> [player]
        // Creates a usage-only wand (no expiry shown in lore; disappears when uses reaches 0)
        if (args.length >= 1 && args[0].equalsIgnoreCase("usage")) {
            if (args.length < 2 || args.length > 3) {
                if (messages != null) {
                    messages.send(sender, "selltools.wand.usage");
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /sellwand usage <uses> [player]");
                }
                return true;
            }

            int uses;
            try {
                uses = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                if (messages != null) {
                    messages.send(sender, "selltools.invalid_time", "time", args[1]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid uses: " + args[1]);
                }
                return true;
            }
            if (uses <= 0) {
                if (messages != null) {
                    messages.send(sender, "selltools.invalid_time", "time", args[1]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid uses: " + args[1]);
                }
                return true;
            }

            Player target;
            if (args.length == 3) {
                target = Bukkit.getPlayerExact(args[2]);
            } else {
                target = sender instanceof Player p ? p : null;
            }

            if (target == null) {
                if (messages != null) {
                    messages.send(sender, "selltools.player_required");
                } else {
                    sender.sendMessage(ChatColor.RED + "Player is required when using this from console.");
                }
                return true;
            }

            if (!util.isEnabled(SellToolType.WAND)) {
                if (messages != null) {
                    messages.send(sender, "selltools.disabled");
                } else {
                    sender.sendMessage(ChatColor.RED + "Sell tools are disabled.");
                }
                return true;
            }

            ItemStack item = util.createUsageTool(SellToolType.WAND, uses);
            target.getInventory().addItem(item);

            if (messages != null) {
                messages.send(sender, "selltools.given", "player", target.getName());
            }
            return true;
        }

        // /sellwand selfdestruction <time> <unit> [player]
        if (args.length >= 1 && args[0].equalsIgnoreCase("selfdestruction")) {
            if (args.length < 3 || args.length > 4) {
                if (messages != null) {
                    messages.send(sender, "selltools.wand.usage");
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /sellwand selfdestruction <time> <unit> [player]");
                }
                return true;
            }

            int time;
            try {
                time = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                if (messages != null) {
                    messages.send(sender, "selltools.invalid_time", "time", args[1]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid time: " + args[1]);
                }
                return true;
            }

            long duration = TimeUnitParser.parseDurationMillis(time, args[2]);
            if (duration <= 0L) {
                if (messages != null) {
                    messages.send(sender, "selltools.invalid_unit", "unit", args[2]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid unit: " + args[2]);
                }
                return true;
            }

            Player target;
            if (args.length == 4) {
                target = Bukkit.getPlayerExact(args[3]);
            } else {
                target = sender instanceof Player p ? p : null;
            }

            if (target == null) {
                if (messages != null) {
                    messages.send(sender, "selltools.player_required");
                } else {
                    sender.sendMessage(ChatColor.RED + "Player is required when using this from console.");
                }
                return true;
            }

            if (!util.isEnabled(SellToolType.WAND)) {
                if (messages != null) {
                    messages.send(sender, "selltools.disabled");
                } else {
                    sender.sendMessage(ChatColor.RED + "Sell tools are disabled.");
                }
                return true;
            }

            long expiresAt = System.currentTimeMillis() + duration;
            ItemStack item = util.createTool(SellToolType.WAND, expiresAt);
            target.getInventory().addItem(item);

            if (messages != null) {
                messages.send(sender, "selltools.given", "player", target.getName());
            }
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("setuses")) {
            if (args.length < 2 || args.length > 3) {
                if (messages != null) {
                    messages.send(sender, "selltools.wand.usage");
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /sellwand setuses <uses> [player]");
                }
                return true;
            }

            int uses;
            try {
                uses = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                if (messages != null) {
                    messages.send(sender, "selltools.invalid_time", "time", args[1]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid uses: " + args[1]);
                }
                return true;
            }
            if (uses < 0) {
                if (messages != null) {
                    messages.send(sender, "selltools.invalid_time", "time", args[1]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid uses: " + args[1]);
                }
                return true;
            }

            Player target;
            if (args.length == 3) {
                target = Bukkit.getPlayerExact(args[2]);
            } else {
                target = sender instanceof Player p ? p : null;
            }

            if (target == null) {
                if (messages != null) {
                    messages.send(sender, "selltools.player_required");
                } else {
                    sender.sendMessage(ChatColor.RED + "Player is required when using this from console.");
                }
                return true;
            }

            ItemStack hand = target.getInventory().getItemInMainHand();
            if (!util.isTool(hand, SellToolType.WAND)) {
                if (messages != null) {
                    messages.send(sender, "selltools.wand.hold_wand");
                } else {
                    sender.sendMessage(ChatColor.RED + "Target must hold a sell wand in their main hand.");
                }
                return true;
            }

            util.setUses(hand, uses);

            if (messages != null) {
                messages.send(sender, "selltools.wand.uses_set", "player", target.getName(), "uses", Integer.toString(uses));
            } else {
                sender.sendMessage(ChatColor.GREEN + "Set sell wand uses for " + target.getName() + " to " + uses + ".");
            }
            return true;
        }

        // Alias for old syntax: /sellwand give <uses> [player]
        if (args.length >= 1 && args[0].equalsIgnoreCase("give")) {
            if (args.length < 2 || args.length > 3) {
                if (messages != null) {
                    messages.send(sender, "selltools.wand.usage");
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /sellwand give <uses> [player]");
                }
                return true;
            }

            int uses;
            try {
                uses = Integer.parseInt(args[1]);
            } catch (NumberFormatException ex) {
                if (messages != null) {
                    messages.send(sender, "selltools.invalid_time", "time", args[1]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid uses: " + args[1]);
                }
                return true;
            }
            if (uses <= 0) {
                if (messages != null) {
                    messages.send(sender, "selltools.invalid_time", "time", args[1]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Invalid uses: " + args[1]);
                }
                return true;
            }

            Player target;
            if (args.length == 3) {
                target = Bukkit.getPlayerExact(args[2]);
            } else {
                target = sender instanceof Player p ? p : null;
            }

            if (target == null) {
                if (messages != null) {
                    messages.send(sender, "selltools.player_required");
                } else {
                    sender.sendMessage(ChatColor.RED + "Player is required when using this from console.");
                }
                return true;
            }

            if (!util.isEnabled(SellToolType.WAND)) {
                if (messages != null) {
                    messages.send(sender, "selltools.disabled");
                } else {
                    sender.sendMessage(ChatColor.RED + "Sell tools are disabled.");
                }
                return true;
            }

            ItemStack item = util.createUsageTool(SellToolType.WAND, uses);
            target.getInventory().addItem(item);

            if (messages != null) {
                messages.send(sender, "selltools.given", "player", target.getName());
            }
            return true;
        }

        // Old syntax alias: /sellwand <time> <unit> [player]
        // Behaves like /sellwand selfdestruction <time> <unit> [player]
        if (args.length >= 2 && args.length <= 3) {
            String[] rewritten;
            if (args.length == 2) {
                rewritten = new String[]{"selfdestruction", args[0], args[1]};
            } else {
                rewritten = new String[]{"selfdestruction", args[0], args[1], args[2]};
            }
            return onCommand(sender, command, label, rewritten);
        }

        if (messages != null) {
            messages.send(sender, "selltools.wand.usage");
        } else {
            sender.sendMessage(ChatColor.RED + "Usage: /sellwand selfdestruction <time> <unit> [player] OR /sellwand usage <uses> [player]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args == null) {
            return Collections.emptyList();
        }

        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String start = args[0].toLowerCase(java.util.Locale.ROOT);
            for (String s : new String[]{"usage", "selfdestruction", "setuses", "give"}) {
                if (s.startsWith(start)) {
                    out.add(s);
                }
            }
            return out;
        }

        String sub = args[0].toLowerCase(java.util.Locale.ROOT);
        if (sub.equals("selfdestruction")) {
            if (args.length == 3) {
                return filterPrefix(args[2], units());
            }
            if (args.length == 4) {
                return null;
            }
        }

        if (sub.equals("usage") || sub.equals("give") || sub.equals("setuses")) {
            if (args.length == 3) {
                return null;
            }
        }

        return Collections.emptyList();
    }

    private static List<String> units() {
        return List.of("seconds", "minutes", "hours", "days", "s", "m", "h", "d");
    }

    private static List<String> filterPrefix(String prefix, List<String> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        if (prefix == null) {
            return new ArrayList<>(options);
        }
        String p = prefix.toLowerCase(java.util.Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String s : options) {
            if (s.toLowerCase(java.util.Locale.ROOT).startsWith(p)) {
                out.add(s);
            }
        }
        return out;
    }
}
