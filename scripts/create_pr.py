import os
import requests
import sys

def main():
    # Load token from environment or .env
    token = os.environ.get("GITHUB_TOKEN")
    
    if not token:
        # Try loading from .env manually
        env_path = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), ".env")
        if os.path.exists(env_path):
            with open(env_path, "r", encoding="utf-8") as f:
                for line in f:
                    if "=" in line:
                        parts = line.strip().split("=", 1)
                        if len(parts) == 2 and parts[0] == "GITHUB_TOKEN":
                            token = parts[1]
                            break
                            
    if not token:
        print("Error: GITHUB_TOKEN environment variable or configuration in .env not found.")
        sys.exit(1)

    owner = "devDaniilNovikov"
    repo = "Andrew-Application"
    
    headers = {
        "Authorization": f"token {token}",
        "Accept": "application/vnd.github.v3+json"
    }
    
    # Check if PR already exists
    url_list = f"https://api.github.com/repos/{owner}/{repo}/pulls"
    params = {
        "state": "open",
        "head": f"{owner}:fix",
        "base": "dev"
    }
    
    try:
        response = requests.get(url_list, headers=headers, params=params)
        response.raise_for_status()
        prs = response.json()
        
        if prs:
            print(f"PR already exists: {prs[0]['html_url']}")
            return
            
        # Create new PR
        data = {
            "title": "Реализация: Этап 1. Слой данных (Room, DAO, Repository, TypeConverters и Тесты)",
            "head": "fix",
            "base": "dev",
            "body": "Этот Пулл-Реквест содержит полную реализацию **Этапа 1. Слой данных**:\n\n"
                    "1. **Бизнес-перечисления (1.1)**: Добавлены `RequestStatus`, `EquipmentType`, `ActionType` с поддержкой русской локализации через строковые ресурсы.\n"
                    "2. **Сущность Room Request (1.2)**: Спроектирована и создана таблица `requests` со всеми обязательными и опциональными полями.\n"
                    "3. **Конвертеры типов (1.3)**: Реализован `Converters` для конвертации дат (ISO-8601) и enum'ов.\n"
                    "4. **Интерфейс DAO (1.4)**: Написаны SQL-запросы для вставки, удаления, обновления и реактивной выборки активных заявок (сортировка по дате) и истории (по дате или статусу).\n"
                    "5. **База данных AppDatabase (1.5)**: Реализован Singleton-билдер базы данных Room с автоматическими миграциями.\n"
                    "6. **Репозиторий и DI (1.6)**: Написаны `RequestRepository` и `RequestRepositoryImpl` с гарантированным выполнением I/O-операций на `Dispatchers.IO`, и осуществлена интеграция в `DependencyProvider`.\n"
                    "7. **Модульное тестирование (1.7)**: Написан полный набор тестов `RequestRepositoryTest` для проверки сортировок, смены статусов и заполнения временных меток.\n\n"
                    "_Автоматически создано и проверено мультиагентной системой Antigravity._"
        }
        
        response_create = requests.post(url_list, headers=headers, json=data)
        response_create.raise_for_status()
        new_pr = response_create.json()
        print(f"Successfully created PR: {new_pr['html_url']}")
        
    except Exception as e:
        print(f"Error managing PR: {e}")
        sys.exit(1)

if __name__ == "__main__":
    main()
