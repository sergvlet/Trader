package com.chicu.trader.trading.indicator;

import com.chicu.trader.trading.model.Candle;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class IndicatorServiceImpl implements IndicatorService {

    @Override
    public double rsi(List<Candle> history, int period) {
        double gain = 0, loss = 0;
        for (int i = 1; i <= period; i++) {
            double change = history.get(i).getClose() - history.get(i - 1).getClose();
            if (change > 0) gain += change;
            else loss -= change;
        }
        double avgGain = gain / period, avgLoss = loss / period;
        double rs = avgLoss == 0 ? 100 : avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    @Override
    public double sma(List<Candle> history, int period) {
        return history.stream()
                .limit(period)
                .mapToDouble(Candle::getClose)
                .average()
                .orElse(0);
    }

    @Override
    public double bbLower(List<Candle> history, int period, double k) {
        double mean = sma(history, period);
        double variance = history.stream()
                .limit(period)
                .mapToDouble(c -> Math.pow(c.getClose() - mean, 2))
                .average()
                .orElse(0);
        return mean - k * Math.sqrt(variance);
    }

    @Override
    public double bbUpper(List<Candle> history, int period, double k) {
        double mean = sma(history, period);
        double variance = history.stream()
                .limit(period)
                .mapToDouble(c -> Math.pow(c.getClose() - mean, 2))
                .average()
                .orElse(0);
        return mean + k * Math.sqrt(variance);
    }

    @Override
    public double vwma(List<Candle> history, int period) {
        double num = 0, den = 0;
        for (int i = 0; i < period && i < history.size(); i++) {
            double v = history.get(i).getVolume();
            num += history.get(i).getClose() * v;
            den += v;
        }
        return den == 0 ? 0 : num / den;
    }

    @Override
    public double atr(List<Candle> history, int period) {
        double sum = 0;
        for (int i = 1; i <= period && i < history.size(); i++) {
            Candle cur = history.get(i);
            Candle prev = history.get(i - 1);
            double tr = Math.max(
                    cur.getHigh() - cur.getLow(),
                    Math.max(
                            Math.abs(cur.getHigh() - prev.getClose()),
                            Math.abs(cur.getLow() - prev.getClose())
                    )
            );
            sum += tr;
        }
        return sum / period;
    }

    @Override
    public double[][] buildFeatures(List<Candle> history) {
        int n = history.size();
        if (n < 27) {
            log.warn("⚠️ Недостаточно свечей для фичей: {} < 27", n);
            return new double[0][0];
        }

        double[][] feats = new double[n - 26][4];
        for (int i = 26; i < n; i++) {
            try {
                Candle c = history.get(i);
                double rsi = rsi(history.subList(i - 14, i + 1), 14);
                double lower = bbLower(history.subList(i - 20, i + 1), 20, 2);
                double upper = bbUpper(history.subList(i - 20, i + 1), 20, 2);
                double bbPct = (c.getClose() - lower) / (upper - lower);
                double volPct = c.getVolume() / vwma(history.subList(i - 20, i + 1), 20);
                double ema12 = sma(history.subList(i - 12, i + 1), 12);
                double ema26 = sma(history.subList(i - 26, i + 1), 26);
                feats[i - 26] = new double[]{rsi, bbPct, volPct, (ema12 - ema26) / ema26};
            } catch (Exception e) {
                log.warn("⛔ Ошибка фичи на i={} → {}", i, e.getMessage());
                feats[i - 26] = new double[]{0, 0, 0, 0};
            }
        }
        return feats;
    }
}
