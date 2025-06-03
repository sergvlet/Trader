# import os
# import sys
# import pandas as pd
# from sklearn.model_selection import train_test_split
# from sklearn.ensemble import RandomForestClassifier
# import joblib
#
#
# def load_data(data_path):
#     print(f"[Python] Проверяем наличие файла данных: {data_path}")
#     if not os.path.isfile(data_path):
#         print(f"[Python] Файл данных не найден: {data_path}")
#         sys.exit(1)
#
#     try:
#         df = pd.read_csv(data_path)
#     except pd.errors.EmptyDataError:
#         print(f"[Python] Файл найден, но он пуст: {data_path}")
#         sys.exit(1)
#     except Exception as e:
#         print(f"[Python] Ошибка при чтении файла: {e}")
#         sys.exit(1)
#
#     if df.empty:
#         print(f"[Python] DataFrame пустой: {data_path}")
#         sys.exit(1)
#
#     print(f"[Python] Данные загружены. Размер: {df.shape}")
#     print(f"[Python] Колонки в датасете: {list(df.columns)}")
#     print(f"[Python] Первые строки:\n{df.head()}")
#     return df
#
#
# def preprocess(df, target_col):
#     print("[Python] Начинаем предобработку данных...")
#
#     # Обработка datetime (если есть)
#     if 'datetime' in df.columns:
#         df['datetime'] = pd.to_datetime(df['datetime'], errors='coerce')
#         df['hour'] = df['datetime'].dt.hour
#         df['minute'] = df['datetime'].dt.minute
#         df['dayofweek'] = df['datetime'].dt.dayofweek
#         df = df.drop(columns=['datetime'])
#
#     # Генерация target если нет
#     if target_col not in df.columns or df[target_col].isna().all():
#         print(f"[Python] Целевая колонка '{target_col}' отсутствует или пуста. Генерируем автоматически.")
#         if 'close' not in df.columns:
#             print("[Python] Колонка 'close' не найдена. Не можем сгенерировать target.")
#             sys.exit(1)
#         df[target_col] = (df['close'].shift(-1) > df['close']).astype(int)
#
#     df = df.dropna(subset=[target_col])
#     df = df.dropna()
#
#     if df.empty:
#         print("[Python] После очистки данных DataFrame пуст.")
#         sys.exit(1)
#
#     X = df.drop(columns=[target_col])
#     y = df[target_col]
#
#     print(f"[Python] Предобработка завершена. Размер X: {X.shape}, y: {y.shape}")
#     return X, y
#
#
# def train_model(X_train, y_train):
#     print("[Python] Обучаем модель RandomForestClassifier...")
#     model = RandomForestClassifier(n_estimators=100, random_state=42)
#     model.fit(X_train, y_train)
#     print("[Python] Модель обучена.")
#     return model
#
#
# def main():
#     script_dir = os.path.dirname(os.path.abspath(__file__))
#     data_path = os.path.join(script_dir, 'dataset.csv')
#     target_col = os.environ.get('ML_TARGET_COLUMN', 'target')
#
#     df = load_data(data_path)
#     X, y = preprocess(df, target_col)
#
#     X_train, X_test, y_train, y_test = train_test_split(
#         X, y, test_size=0.2, random_state=42
#     )
#     print("[Python] Разделили данные на обучающую и тестовую выборки.")
#
#     model = train_model(X_train, y_train)
#
#     models_dir = os.path.join(script_dir, 'models')
#     os.makedirs(models_dir, exist_ok=True)
#     model_path = os.path.join(models_dir, 'rf_model.pkl')
#
#     joblib.dump(model, model_path)
#     print(f"[Python] Модель сохранена: {model_path}")
#     print("[Python] Скрипт завершён успешно.")
#
#
# if __name__ == '__main__':
#     main()

import ccxt
import pandas as pd
import time
import sys

# === Настройки ===
symbol = 'BTC/USDT'
timeframe = '1m'
limit = 1000
tp_threshold = 0.005  # 0.5%
sl_threshold = 0.003  # 0.3%
lookahead = 10        # сколько свечей вперёд проверять

# === Загрузка OHLCV ===
def fetch_ohlcv(symbol, timeframe, since=None, limit=500):
    exchange = ccxt.binance({'enableRateLimit': True})
    print(f"[Python] Загружаем {symbol} {timeframe} с Binance...")
    return exchange.fetch_ohlcv(symbol, timeframe, since=since, limit=limit)

# === Генерация таргета по TP/SL ===
def generate_target_tp_sl(df: pd.DataFrame, tp=0.005, sl=0.003, lookahead=10) -> pd.DataFrame:
    df = df.copy()
    target = []

    for i in range(len(df) - lookahead):
        entry_price = df.iloc[i]['close']
        future_data = df.iloc[i+1:i+1+lookahead]

        tp_price = entry_price * (1 + tp)
        sl_price = entry_price * (1 - sl)

        hit = None
        for _, row in future_data.iterrows():
            if row['high'] >= tp_price:
                hit = 1
                break
            if row['low'] <= sl_price:
                hit = 0
                break
        target.append(hit if hit is not None else -1)

    df = df.iloc[:len(target)]  # Обрезаем, чтобы соответствовало длине
    df['target'] = target
    df.dropna(inplace=True)
    return df

# === Основной блок ===
def main():
    since = int((time.time() - 60 * 60 * 6) * 1000)  # последние 6 часов
    ohlcv = fetch_ohlcv(symbol, timeframe, since=since, limit=limit)
    df = pd.DataFrame(ohlcv, columns=['timestamp', 'open', 'high', 'low', 'close', 'volume'])
    df['datetime'] = pd.to_datetime(df['timestamp'], unit='ms')
    df = df[['datetime', 'open', 'high', 'low', 'close', 'volume']]

    # Генерация таргета по TP/SL
    df = generate_target_tp_sl(df, tp=tp_threshold, sl=sl_threshold, lookahead=lookahead)

    # Сохранение
    filename = 'dataset_tp_sl.csv'
    df.to_csv(filename, index=False)
    print(f"[Python] Сохранено в {filename} с колонкой target (TP/SL логика)")

if __name__ == '__main__':
    main()
