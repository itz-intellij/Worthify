package dev.simpleye.worthify.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class EconomyHook {

    private final JavaPlugin plugin;
    private Economy economy;
    private BalanceStore balanceStore;
    private boolean internalEnabled;
    private double internalStartingBalance;
    private boolean vaultOverrideProvider;

    public EconomyHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void hook() {
        this.economy = null;

        FileConfiguration cfg = plugin.getConfig();
        this.internalEnabled = cfg.getBoolean("economy.internal.enabled", true);
        this.internalStartingBalance = cfg.getDouble("economy.internal.starting_balance", 0.0D);
        this.vaultOverrideProvider = cfg.getBoolean("economy.vault.override_provider", false);

        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            if (internalEnabled) {
                enableInternal();
                plugin.getLogger().info("Vault not found. Using internal Worthify economy.");
            } else {
                plugin.getLogger().warning("Vault not found and internal economy is disabled. Selling will be disabled.");
            }
            return;
        }

        if (vaultOverrideProvider && internalEnabled) {
            enableInternal();
            tryRegisterAsVaultEconomyProvider(ServicePriority.Highest);
            if (economy != null) {
                return;
            }
        }

        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            if (internalEnabled) {
                enableInternal();
                tryRegisterAsVaultEconomyProvider(ServicePriority.Normal);
                plugin.getLogger().warning("No Vault economy provider found. Using internal Worthify economy.");
            } else {
                plugin.getLogger().warning("No Vault economy provider found and internal economy is disabled. Selling will be disabled.");
            }
            return;
        }

        economy = provider.getProvider();
        if (economy == null) {
            if (internalEnabled) {
                enableInternal();
                plugin.getLogger().warning("Vault economy provider is null. Using internal Worthify economy.");
            } else {
                plugin.getLogger().warning("Vault economy provider is null and internal economy is disabled. Selling will be disabled.");
            }
        }
    }

    private void tryRegisterAsVaultEconomyProvider(ServicePriority priority) {
        if (!internalEnabled || balanceStore == null) {
            return;
        }

        try {
            for (RegisteredServiceProvider<Economy> reg : Bukkit.getServicesManager().getRegistrations(Economy.class)) {
                Economy existing = reg == null ? null : reg.getProvider();
                if (existing instanceof WorthifyVaultEconomy) {
                    this.economy = existing;
                    return;
                }
            }

            WorthifyVaultEconomy provider = new WorthifyVaultEconomy(this);
            ServicePriority p = priority == null ? ServicePriority.Normal : priority;
            Bukkit.getServicesManager().register(Economy.class, provider, plugin, p);
            this.economy = provider;
            plugin.getLogger().info("Registered Worthify as Vault economy provider.");
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to register Worthify as Vault economy provider: " + t.getMessage());
        }
    }

    public boolean isEnabled() {
        return economy != null || (internalEnabled && balanceStore != null);
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (economy != null) {
            return economy.depositPlayer(player, amount).transactionSuccess();
        }
        if (internalEnabled && balanceStore != null) {
            return balanceStore.deposit(player, amount, internalStartingBalance);
        }
        return false;
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0.0D) {
            return true;
        }

        if (economy != null) {
            EconomyResponse response = economy.withdrawPlayer(player, amount);
            return response.transactionSuccess();
        }

        if (internalEnabled && balanceStore != null) {
            return balanceStore.withdraw(player, amount, internalStartingBalance);
        }

        return false;
    }

    public boolean transfer(OfflinePlayer from, OfflinePlayer to, double amount) {
        if (from == null || to == null) {
            return false;
        }
        if (amount <= 0.0D) {
            return false;
        }

        if (!withdraw(from, amount)) {
            return false;
        }
        if (!deposit(to, amount)) {
            deposit(from, amount);
            return false;
        }
        return true;
    }

    public double getBalance(OfflinePlayer player) {
        if (economy != null) {
            return economy.getBalance(player);
        }
        if (internalEnabled && balanceStore != null) {
            return balanceStore.getBalance(player, internalStartingBalance);
        }
        return 0.0D;
    }

    public boolean isUsingInternalEconomy() {
        return economy == null && internalEnabled && balanceStore != null;
    }

    public boolean resetInternalBalance(OfflinePlayer player) {
        if (!isUsingInternalEconomy()) {
            return false;
        }
        return balanceStore.reset(player);
    }

    public List<Map.Entry<UUID, Double>> topInternalBalances(int limit) {
        if (!isUsingInternalEconomy()) {
            return java.util.Collections.emptyList();
        }

        List<Map.Entry<UUID, Double>> list = new ArrayList<>(balanceStore.snapshot().entrySet());
        list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        if (limit <= 0 || limit == Integer.MAX_VALUE) {
            return list;
        }

        int cap = Math.max(1, Math.min(100, limit));
        if (list.size() > cap) {
            return new ArrayList<>(list.subList(0, cap));
        }
        return list;
    }

    private void enableInternal() {
        if (!internalEnabled) {
            return;
        }
        if (balanceStore == null) {
            balanceStore = new BalanceStore(plugin);
        }
        balanceStore.load();
    }

    boolean internalHasAccount(OfflinePlayer player) {
        if (balanceStore == null) {
            return false;
        }
        return balanceStore.hasAccount(player);
    }

    boolean internalCreateAccount(OfflinePlayer player) {
        if (balanceStore == null) {
            return false;
        }
        return balanceStore.ensureAccount(player, internalStartingBalance);
    }

    double internalGetBalance(OfflinePlayer player) {
        if (balanceStore == null) {
            return 0.0D;
        }
        return balanceStore.getBalance(player, internalStartingBalance);
    }

    boolean internalDeposit(OfflinePlayer player, double amount) {
        if (balanceStore == null) {
            return false;
        }
        balanceStore.ensureAccount(player, internalStartingBalance);
        return balanceStore.deposit(player, amount, internalStartingBalance);
    }

    boolean internalWithdraw(OfflinePlayer player, double amount) {
        if (balanceStore == null) {
            return false;
        }
        balanceStore.ensureAccount(player, internalStartingBalance);
        return balanceStore.withdraw(player, amount, internalStartingBalance);
    }
}
