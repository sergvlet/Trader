// src/main/java/com/chicu/trader/trading/backtest/BacktestResult.java
package com.chicu.trader.trading.backtest;

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

            double grossPnl = (exitPrice - entryPrice) / entryPrice;
            // комиссия дважды: вход + выход
            this.pnl = grossPnl - (commissionPct * 2.0 / 100.0);
            this.win = this.pnl > 0;
        }
    }

    private final List<Trade> trades = new ArrayList<>();

    public void addTrade(Trade t) {
        trades.add(t);
    }

    public int getTotalTrades() {
        return trades.size();
    }

    public double getTotalPnl() {
        return trades.stream()
                .mapToDouble(Trade::getPnl)
                .sum();
    }

    public int getWinCount() {
        return (int) trades.stream()
                .filter(Trade::isWin)
                .count();
    }

    public double getWinRate() {
        return trades.isEmpty() ? 0.0 : (double) getWinCount() / trades.size();
    }

    public Map<String, Double> getPnlBySymbol() {
        Map<String, Double> result = new HashMap<>();
        for (Trade trade : trades) {
            result.merge(trade.getSymbol(), trade.getPnl(), Double::sum);
        }
        return result;
    }

    public List<String> getLosingSymbols() {
        Map<String, Double> pnlMap = getPnlBySymbol();
        List<String> losers = new ArrayList<>();
        pnlMap.forEach((sym, pnl) -> { if (pnl < 0) losers.add(sym); });
        return losers;
    }
}
