package dev.simpleye.worthify.update;

import dev.simpleye.worthify.WorthifyPlugin;
import dev.simpleye.worthify.message.MessageService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class UpdateNotifyListener implements Listener {

    private final WorthifyPlugin plugin;

    public UpdateNotifyListener(WorthifyPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!plugin.isUpdateCheckerNotifyOnJoin()) {
            return;
        }

        if (!player.isOp() && !player.hasPermission("worthify.update")) {
            return;
        }

        ModrinthUpdateChecker checker = plugin.getUpdateChecker();
        if (checker == null || !checker.isUpdateAvailable()) {
            return;
        }

        ModrinthUpdateChecker.UpdateInfo info = checker.getLatestUpdate();
        if (info == null) {
            return;
        }

        MessageService messages = plugin.getMessages();
        if (messages != null) {
            messages.send(player, "update.available",
                    "current", info.getCurrentVersion(),
                    "latest", info.getLatestVersion(),
                    "url", info.getUrl());
        } else {
            player.sendMessage(ChatColor.YELLOW + "Worthify update available: v" + info.getCurrentVersion() + " -> v" + info.getLatestVersion());
            player.sendMessage(ChatColor.GRAY + info.getUrl());
        }
    }
}
