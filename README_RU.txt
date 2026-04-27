# PathDLC Baritone Digger — AutoWarden + Fast Clan

В этой версии:
- полностью убран Copy/Paste;
- добавлен AutoWarden;
- `.clan` больше не имеет delay/tick-настройки и работает в fast mode;
- старые `.pos`, `.fill`, `.dig baritone`, `.apple` оставлены.

## Команды

```text
.pos 1
.pos 2
.pos 1 look
.pos 2 look
.pos clear

.fill
.dig baritone

.apple set
.apple set look
.apple clear
.apple start
.apple stop
.apple status
.apple radius 16
.apple bonemeal on/off

.clan start
.clan stop
.clan status

.warden start
.warden stop
.warden status
.warden radius <8-96>
.warden vertical <4-32>
```

## AutoWarden

`.warden start`:
- сканирует рядом сундуки / trapped chests / barrels;
- ищет timer-текст рядом через armor stand/custom-name entities;
- выбирает chest с самым маленьким timer;
- через Baritone идёт к нему;
- ждёт, если timer ещё не закончился;
- открывает и quick-move лутает контейнер;
- после закрытия ищет следующий сундук.

## Clan

`.clan start`:
- берёт redstone dust из инвентаря;
- если redstone wire под игроком уже стоит — сразу ломает;
- если wire нет — сразу ставит;
- искусственная задержка удалена.

## Убрано

```text
.c
.copy
.paste
```

Папка `src/main/java/com/pathdlc/digger/copy` удалена, все ссылки на Copy/Paste из entrypoint удалены.

## Сборка

```bat
gradlew.bat clean build
```

Jar:

```text
build/libs/pathdlc-baritone-digger-1.0.0.jar
```
