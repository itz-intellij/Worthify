package dev.simpleye.worthify.sell;

public record SellResult(boolean success, boolean economyMissing, int soldAmount, double total) {

    public static SellResult success(int soldAmount, double total) {
        return new SellResult(true, false, soldAmount, total);
    }

    public static SellResult nothingToSell() {
        return new SellResult(false, false, 0, 0.0D);
    }

    public static SellResult disabledEconomy() {
        return new SellResult(false, true, 0, 0.0D);
    }
}
