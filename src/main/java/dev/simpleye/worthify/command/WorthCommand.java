package dev.simpleye.worthify.command;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.gui.WorthGuiManager;
import dev.simpleye.worthify.message.MessageService;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class WorthCommand implements CommandExecutor {

    private final WorthifyPlugin plugin;
    private final WorthGuiManager worthGuiManager;

    public WorthCommand(WorthifyPlugin plugin, WorthGuiManager worthGuiManager) {
        this.plugin = plugin;
        this.worthGuiManager = worthGuiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageService messages = plugin.getMessages();
        if (!(sender instanceof Player player)) {
            if (messages != null) {
                messages.send(sender, "errors.players_only");
            } else {
                sender.sendMessage("Players only.");
            }
            return true;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("hand")) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item == null || item.getType().isAir()) {
                if (messages != null) {
                    messages.send(player, "worth.hand_empty");
                } else {
                    player.sendMessage(ChatColor.RED + "You're not holding anything.");
                }
                return true;
            }

            Material type = item.getType();
            double unit = plugin.getWorthManager().getUnitPrice(type);
            double mult = plugin.getWorthMultiplier(type);
            double unitMultiplied = unit * mult;
            double total = unitMultiplied * item.getAmount();

            if (unit <= 0.0D) {
                if (messages != null) {
                    messages.send(player, "worth.no_worth", "item", type.name());
                } else {
                    player.sendMessage(ChatColor.YELLOW + "No worth set for " + ChatColor.WHITE + type.name() + ChatColor.YELLOW + ".");
                }
                return true;
            }

            if (messages != null) {
                messages.send(player, "worth.hand_value",
                        "item", type.name(),
                        "unit", String.format(java.util.Locale.US, "%.2f", unitMultiplied),
                        "amount", Integer.toString(item.getAmount()),
                        "total", String.format(java.util.Locale.US, "%.2f", total));
            } else {
                player.sendMessage(ChatColor.GREEN + "Worth of " + ChatColor.WHITE + type.name() + ChatColor.GREEN + ": "
                        + ChatColor.AQUA + "$" + String.format(java.util.Locale.US, "%.2f", unitMultiplied)
                        + ChatColor.GRAY + " each" + ChatColor.DARK_GRAY + " (x" + item.getAmount() + ")" + ChatColor.GREEN
                        + " = " + ChatColor.AQUA + "$" + String.format(java.util.Locale.US, "%.2f", total));
            }
            return true;
        }

        int page = 1;
        String query = null;

        if (args.length >= 1) {
            int parsedPage = tryParseInt(args[0]);
            if (parsedPage != Integer.MIN_VALUE) {
                page = parsedPage;
                if (args.length >= 2) {
                    query = join(args, 1);
                }
            } else {
                query = join(args, 0);
            }
        }

        worthGuiManager.open(player, page, query);
        return true;
    }

    private static int tryParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return Integer.MIN_VALUE;
        }
    }

    private static String join(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i > start) {
                sb.append(' ');
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }
}
