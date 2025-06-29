package com.chicu.trader.trading.model;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter

public class BacktestResult {
    @Getter
    public static class Trade {
        private final long entryTime;
        private final double entryPrice;
        private final long exitTime;
        private final double exitPrice;
        private final double pnl;

        public Trade(long entryTime, double entryPrice,
                     long exitTime, double exitPrice) {
            this.entryTime = entryTime;
            this.entryPrice = entryPrice;
            this.exitTime = exitTime;
            this.exitPrice = exitPrice;
            this.pnl = (exitPrice - entryPrice) / entryPrice;
        }
    }

    private final List<Trade> trades = new ArrayList<>();

    public void addTrade(Trade t) {
        trades.add(t);
    }

    public double getTotalPnl() {
        return trades.stream()
                .mapToDouble(Trade::getPnl)
                .sum();
    }

    public int getWinCount() {
        return (int) trades.stream()
                .filter(t -> t.getPnl() > 0)
                .count();
    }

    public double getWinRate() {
        return trades.isEmpty()
                ? 0
                : (double) getWinCount() / trades.size();
    }

    // можно добавить maxDrawdown, avgDuration и т.д.
}
