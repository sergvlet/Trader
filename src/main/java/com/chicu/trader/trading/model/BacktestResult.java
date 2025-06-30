package com.chicu.trader.trading.model;

import lombok.Getter;

import java.util.*;

@Getter
public class BacktestResult {

    @Getter
    public static class Trade {
        private final String symbol;
        private final long entryTime;
        private final double entryPrice;
        private final long exitTime;
        private final double exitPrice;
        private final double pnl;
        private final boolean win;

        public Trade(String symbol, long entryTime, double entryPrice,
                     long exitTime, double exitPrice, double commissionPct) {
            this.symbol = symbol;
            this.entryTime = entryTime;
            this.entryPrice = entryPrice;
            this.exitTime = exitTime;
            this.exitPrice = exitPrice;
            // PnL с учётом комиссии
            double grossPnl = (exitPrice - entryPrice) / entryPrice;
            this.pnl = grossPnl - (commissionPct * 2.0 / 100.0);
            this.win = this.pnl > 0;
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
        return (int) trades.stream().filter(Trade::isWin).count();
    }

    public double getWinRate() {
        return trades.isEmpty() ? 0.0 : (double) getWinCount() / trades.size();
    }

    public int getTotalTrades() {
        return trades.size();
    }

    public Map<String, Double> pnlBySymbol() {
        Map<String, Double> result = new HashMap<>();
        for (Trade t : trades) {
            result.put(t.getSymbol(), result.getOrDefault(t.getSymbol(), 0.0) + t.getPnl());
        }
        return result;
    }

    public List<String> getLosingSymbols() {
        Map<String, Double> pnlMap = pnlBySymbol();
        List<String> losers = new ArrayList<>();
        for (Map.Entry<String, Double> e : pnlMap.entrySet()) {
            if (e.getValue() < 0) losers.add(e.getKey());
        }
        return losers;
    }
}
