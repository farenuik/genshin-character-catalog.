# Каталог персонажів Genshin Impact

Консольний і графічний застосунок на Java для перегляду каталогу персонажів, імпорту даних із Genshin DB API та локального збереження профілів.

## Запуск готового застосунку

Потрібна **Java 21 або новіша**. Maven для запуску готового JAR не потрібен.

```shell
java -jar app/target/app-1.1.0.jar
```

Без аргументів застосунок покаже меню режимів. Режим можна вибрати аргументом:

```shell
java -jar app/target/app-1.1.0.jar --chars
java -jar app/target/app-1.1.0.jar --gui
java -jar app/target/app-1.1.0.jar --game
java -jar app/target/app-1.1.0.jar --text
```

Графічні режими потребують середовища з підтримкою Swing. Каталог (`--gui`) завантажує актуальні дані й портрети з Genshin DB API (`genshin-db-api.vercel.app`); якщо API недоступне, показується офлайн-демо. Гра (`--game`) використовує локальний каталог. Створені персонажі зберігаються у `%USERPROFILE%\.genshin-cli\characters.json` на Windows або `~/.genshin-cli/characters.json` на Linux/macOS. Кеш портретів зберігається поруч у каталозі `portraits`.

У режимі `--chars` можна створити або імпортувати двох персонажів, а потім почати матч:

```text
game start
turn attack
turn defend
game status
game history
```

`game start` покаже доступних персонажів і попросить вибрати учасників. Також їх можна передати одразу: `game start <id-1> <id-2>`. Кожен хід виконується від імені поточного учасника; підтримуються `attack`, `defend`, `heal` і `ability`. Механіки та формули описані в [DESIGN.md](./DESIGN.md).
Стан і журнал останнього матчу автоматично зберігаються після кожного ходу у `%USERPROFILE%\.genshin-cli\last-game.json` (Windows) або `~/.genshin-cli/last-game.json` (Linux/macOS). Для відновлення виконайте `game load`; команди `game save` і `game history` доступні в режимі персонажів.

## Збірка з початкового коду

Для збірки потрібні **JDK 21 або новіший** і **Apache Maven 3.9+**. У корені репозиторію виконайте:

```shell
mvn clean package
```

Збірка запускає тести модулів і створює автономний executable JAR:

```text
app/target/app-1.1.0.jar
```

У JAR включено застосунок, потрібні модулі проєкту та бібліотеку Jackson. Для передавання достатньо цього одного файлу; користувачу потрібна Java 21+, але не Maven. Для запуску в Windows можна використати ті самі команди `java -jar` у PowerShell або командному рядку.
