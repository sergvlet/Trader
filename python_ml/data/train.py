# python_ml/data/train.py

import os
import sys
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.ensemble import RandomForestClassifier
import joblib

def load_data(data_path):
    """
    Загружает данные из CSV и возвращает DataFrame.
    Обрабатывает случай пустого или некорректного файла.
    """
    if not os.path.isfile(data_path):
        print(f"[Python] Файл данных не найден: {data_path}")
        sys.exit(1)

    try:
        df = pd.read_csv(data_path)
    except pd.errors.EmptyDataError:
        print(f"[Python] Файл данных найден, но он пуст или не имеет столбцов: {data_path}")
        sys.exit(1)
    except Exception as e:
        print(f"[Python] Ошибка при чтении CSV: {e}")
        sys.exit(1)

    if df.empty:
        print(f"[Python] Данные в файле есть, но DataFrame пустой: {data_path}")
        sys.exit(1)

    print(f"[Python] Данные загружены. Размер: {df.shape}")
    return df

def preprocess(df):
    """
    Простая предобработка: удаляем строки с пропусками,
    проверяем наличие столбца 'target',
    разделяем на признаки X и целевую y.
    """
    df = df.dropna()
    if 'target' not in df.columns:
        print("[Python] В DataFrame отсутствует колонка 'target'.")
        sys.exit(1)

    X = df.drop(columns=['target'])
    y = df['target']
    print("[Python] Данные предобработаны.")
    return X, y

def train_model(X_train, y_train):
    """
    Обучает модель RandomForestClassifier.
    """
    model = RandomForestClassifier(n_estimators=100, random_state=42)
    model.fit(X_train, y_train)
    print("[Python] Модель обучена.")
    return model

def main():
    # Абсолютный путь к папке со скриптом:
    script_dir = os.path.dirname(os.path.abspath(__file__))  # .../python_ml/data

    # Предполагаем, что dataset.csv лежит в той же папке, что и train.py:
    data_path = os.path.join(script_dir, 'dataset.csv')

    # 1) Загрузка
    df = load_data(data_path)

    # 2) Предобработка
    X, y = preprocess(df)

    # 3) Делим на train/test
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42
    )
    print("[Python] Разделили на train/test.")

    # 4) Обучаем модель
    model = train_model(X_train, y_train)

    # 5) Сохраняем модель в папку models (создаём, если не существует)
    models_dir = os.path.join(script_dir, 'models')
    os.makedirs(models_dir, exist_ok=True)

    model_path = os.path.join(models_dir, 'rf_model.pkl')
    joblib.dump(model, model_path)
    print(f"[Python] Модель сохранена по пути: {model_path}")

    print("Обучение завершилось успешно.")

if __name__ == '__main__':
    main()
