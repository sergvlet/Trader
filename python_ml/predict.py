import argparse

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--model', required=True)
    parser.add_argument('--features', required=True)
    args = parser.parse_args()

    # Здесь вы загружаете модель и делаете инференс
    # Для теста просто возвращаем 0.72
    print(f"PREDICT=0.72")

if __name__ == '__main__':
    main()
