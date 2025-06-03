import ccxt
import pandas as pd
import time
import datetime
import sys

# === Параметры ===
symbol = 'BTC/USDT'
timeframe = '1m'
limit = 1000
future_shift = 5  # через сколько минут смотреть на цену
since_minutes_ago = 60 * 6  # 6 часов назад

# === Получение OHLCV ===
def fetch_ohlcv(symbol, timeframe, since=None, limit=500):
    exchange = ccxt.binance({
        'enableRateLimit': True,
        'timeout': 10000
    })

    try:
        print(f"[Python] Загружаем {symbol} {timeframe} с Binance...")
        ohlcv = exchange.fetch_ohlcv(symbol, timeframe, since=since, limit=limit)
        print(f"[Python] Получено {len(ohlcv)} свечей.")
        return ohlcv
    except Exception as e:
        print(f"[Ошибка при загрузке данных]: {e}")
        sys.exit(1)

# === Генерация целевой переменной ===
def generate_target(df: pd.DataFrame, shift: int = 5) -> pd.DataFrame:
    df = df.copy()
    df['future_close'] = df['close'].shift(-shift)
    df['target'] = (df['future_close'] > df['close']).astype(int)
    df.drop(columns=['future_close'], inplace=True)
    df.dropna(inplace=True)
    return df

# === Основной блок ===
def main():
    since = int((time.time() - since_minutes_ago * 60) * 1000)
    ohlcv = fetch_ohlcv(symbol, timeframe, since=since, limit=limit)

    # Конвертация в DataFrame
    df = pd.DataFrame(ohlcv, columns=['timestamp', 'open', 'high', 'low', 'close', 'volume'])
    df['datetime'] = pd.to_datetime(df['timestamp'], unit='ms')
    df = df[['datetime', 'open', 'high', 'low', 'close', 'volume']]

    # Генерация колонки target
    df = generate_target(df, shift=future_shift)

    # Сохранение
    filename = 'dataset.csv'
    df.to_csv(filename, index=False)
    print(f"[Python] Данные сохранены в {filename} с колонкой 'target'.")

if __name__ == '__main__':
    main()
