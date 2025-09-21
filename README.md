# FileHarvester

Утилита для рекурсивного сбора и копирования файлов из исходной директории в целевую с возможностью фильтрации по расширениям.

## Возможности

- **Рекурсивный обход** всех поддиректорий
- **Гибкая фильтрация** файлов по расширениям (include/exclude правила)
- **Плоская структура** в целевой директории (все файлы в одной папке)
- **Автоматическое разрешение конфликтов** имен файлов
- **Цветной консольный вывод** с прогрессом операций
- **Просмотр структуры директорий** до и после операции

## Быстрый старт

### Запуск

```bash
java -jar fileharvester.jar
```

### Интерактивный режим

Программа последовательно запросит:

1. **Исходную директорию** - откуда копировать файлы
2. **Целевую директорию** - куда копировать файлы
3. **Правила фильтрации** (опционально)
4. **Настройки копирования**

### Примеры фильтров

```
Только изображения:
+jpg,+png,+gif,+bmp

Документы без временных файлов:
+pdf,+doc,+docx,-tmp,-bak

Все файлы кроме системных:
-sys,-tmp,-log

Без фильтра (все файлы):
[Enter - пустая строка]
```

## Правила фильтрации

- **`+ext`** - включить файлы с расширением `ext`
- **`-ext`** - исключить файлы с расширением `ext`
- **`+noext`** - включить файлы без расширения
- **`-noext`** - исключить файлы без расширения

### Логика работы

- Если есть **include** правила (`+`) → разрешены только указанные типы
- Если только **exclude** правила (`-`) → разрешено всё кроме указанного
- Можно комбинировать: `+jpg,+png,-tmp`

## Программное использование

### Базовая конфигурация

```java
HarvesterConfig config = HarvesterConfig.builder()
    .sourceDirectory(Paths.get("/source/directory"))
    .targetDirectory(Paths.get("/target/directory"))
    .createTargetIfNotExists(true)
    .overwriteExisting(false)
    .build();

FileHarvesterService harvester = new FileHarvesterServiceImpl();
HarvestResult result = harvester.harvest(config);
```

### С фильтрацией

```java
// Только изображения
FileFilterStrategy imageFilter = new FlexibleExtensionFilterStrategy(
    "+jpg", "+png", "+gif", "+bmp"
);

HarvesterConfig config = HarvesterConfig.builder()
    .sourceDirectory(sourceDir)
    .targetDirectory(targetDir) 
    .fileFilterStrategy(imageFilter)
    .build();
```

### Комбинирование фильтров

```java
FileFilterStrategy filter1 = new FlexibleExtensionFilterStrategy("+jpg", "+png");
FileFilterStrategy filter2 = new FlexibleExtensionFilterStrategy("-tmp");

// Логические операции
FileFilterStrategy combined = filter1.and(filter2);
FileFilterStrategy either = filter1.or(filter2);
FileFilterStrategy inverted = filter1.negate();
```

## Структура проекта

```
v.landau/
├── config/
│   └── HarvesterConfig.java          # Конфигурация
├── service/
│   ├── FileHarvesterService.java     # Интерфейс сервиса
│   └── impl/
│       └── FileHarvesterServiceImpl.java # Реализация
├── strategy/
│   ├── FileFilterStrategy.java       # Интерфейс фильтрации
│   ├── FileProcessingStrategy.java   # Интерфейс обработки
│   ├── ProgressListener.java         # Слушатель прогресса
│   └── impl/                         # Реализации стратегий
├── model/
│   ├── FileOperation.java           # Модель операции
│   └── HarvestResult.java           # Результат операции
├── util/
│   ├── ConsoleLogger.java           # Цветной логгер
│   ├── DirectoryTreePrinter.java    # Просмотр структуры
│   └── PathPrompter.java            # Промпты ввода
└── Main.java                        # Точка входа
```

## Примеры вывода

### Успешная операция
```
[12:34:56] [INFO] Found 156 files to process
[12:34:56] [SUCCESS] ✓ photo1.jpg
[12:34:56] [SUCCESS] ✓ document.pdf  
[12:34:56] [WARN] ⊘ backup.tmp - Filtered out
[12:34:57] [INFO] Processing complete!

=== Harvesting Complete ===
Files processed: 156
Files copied: 142
Files skipped: 14
Total size copied: 2.45 GB
Time elapsed: 1247 ms
```

### Структура директории
```
=== Source Directory Structure ===
/home/user/photos
📁 2023 [2d, 45f]
├── 📁 january [0d, 12f]
│   ├── 🖼️  img001.jpg (2.3 MB)
│   └── 🖼️  img002.png (1.8 MB)  
└── 📁 february [0d, 33f]
    ├── 🖼️  photo1.jpg (3.1 MB)
    └── 📄  notes.txt (245 B)
```

## Требования

- **Java 11+**
- Права на чтение исходной директории
- Права на запись в целевую директорию (или её родителя)

## Особенности

- **Безопасность**: проверка путей, предотвращение копирования в самого себя
- **Производительность**: эффективный обход файловой системы
- **Надежность**: обработка ошибок, валидация путей
- **Удобство**: интерактивный режим, цветной вывод, прогресс-бар