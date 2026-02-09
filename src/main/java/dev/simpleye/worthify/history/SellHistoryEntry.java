package dev.simpleye.worthify.history;

public record SellHistoryEntry(long timestampMillis, String materialName, int soldAmount, double total) {
}
