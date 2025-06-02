# python_ml/data/data_collect.py

import os
import time
import pandas as pd
import ccxt

def fetch_ohlcv(symbol: str, timeframe: str, since: int = None, limit: int = 500):
    """
    Загружает OHLCV с биржи Binance через ccxt.
    - symbol: например, 'BTC/USDT'
    - timeframe: '1m', '5m', '1h', '4h', '1d' и т.п.
    - since: метка времени в миллисекундах (если None — загрузит последние limit свечей)
    - limit: сколько свечей за раз запрашивать (максимум 1000 для Binance)
    """
    exchange = ccxt.binance({
        'enableRateLimit': True,
        # по желанию: 'apiKey': '...', 'secret': '...' — но для публичных исторических данных не нужны
    })
    all_ohlcv = []
    params = {}  # здесь можно добавить дополнительные параметры, если нужно
    while True:
        ohlcv = exchange.fetch_ohlcv(symbol, timeframe, since=since, limit=limit, params=params)
        if not ohlcv:
            break
        all_ohlcv += ohlcv
        if len(ohlcv) < limit:
            break
        # сдвигаем since к времени последней полученной свечи + 1 миллисекунды
        since = ohlcv[-1][0] + 1
        time.sleep(exchange.rateLimit / 1000)  # спим, чтобы не превысить лимит запросов
    return all_ohlcv

def main():
    # 1) Параметры сбора
    symbol    = 'BTC/USDT'    # можно сделать аргументом
    timeframe = '1m'          # можно брать из настроек пользователя (ai_trading_settings.timeframe)
    # Если нужно забрать всю историю — поставьте since = None (он сам возьмёт последние limit свечей).
    # Или можно посчитать since, например, год назад: int((time.time() - 365*24*60*60)*1000)
    since = None
    limit = 1000  # максимальное число назад за один запрос

    # 2) Получаем массив OHLCV: [ [timestamp, open, high, low, close, volume], ... ]
    print(f"[Python] Начинаю загрузку исторических данных для {symbol} {timeframe}...")
    ohlcv = fetch_ohlcv(symbol, timeframe, since=since, limit=limit)
    if not ohlcv:
        print("[Python] Ошибка: не удалось получить данные или данные пустые.")
        return

    # 3) Переводим в DataFrame
    df = pd.DataFrame(ohlcv, columns=['timestamp', 'open', 'high', 'low', 'close', 'volume'])
    # Приведём timestamp из милисекунд в человекочитаемый формат, если нужно:
    df['datetime'] = pd.to_datetime(df['timestamp'], unit='ms')
    df = df[['datetime', 'open', 'high', 'low', 'close', 'volume']]

    print(f"[Python] Данные загружены. Количество строк: {len(df)}")

    # 4) Сохраняем в CSV (папка python_ml/data/)
    data_folder = os.path.join(os.path.dirname(__file__), '')
    os.makedirs(data_folder, exist_ok=True)
    dataset_path = os.path.join(data_folder, 'dataset.csv')
    df.to_csv(dataset_path, index=False)
    print(f"[Python] Данные сохранены в файл: {dataset_path}")


if __name__ == '__main__':
    main()
