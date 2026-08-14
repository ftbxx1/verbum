# Verbum — full language specification

**Verbum** is an original, beginner-first English scripting language for
Minecraft. It is not Python, JavaScript, Skript, or any existing language with
commands bolted on. You write ordinary English sentences, indented to show which
lines belong to which block, and the server does what you said.

```
when player touches water
    kill player

when player collects emerald
    add 1 to player's emeralds

when player has 10 emeralds
    announce Player Wins
```

---

## 1. Language identity

| Thing         | Value                          |
|---------------|--------------------------------|
| Name          | **Verbum**                     |
| File extension| `.vb` (also accepts `.mcscript`) |
| Philosophy    | *"Tell the game what to do."*  |
| Paradigm      | Event-driven, indentation-based, word-only |
| Platform v1   | Paper (Spigot API) server plugin |

The core engine is 100% platform-independent Java and can run against a mock
world with no server at all (used by the automated tests and the `demo` tool).

---

## 2. The grammar at a glance

A Verbum program is a **DAG**: **Events** → **Conditions** → **Actions**.

```
program    := (event-block | action-block)*
event-block:= header block-body
header     := "when" <condition>
            | "every" <amount> ("second"|"minute"|"hour") ("s")?
            | "on" "server" ("start"|"stop")
action-block:= "action" <name> (<param>)*
block-body := <indented statement>*
statement  := action-call
            | if-statement
            | loop-statement
            | "break" | "continue" | "stop"
```

- **Blocks are defined by indentation.** A line indented deeper than the previous
  descriptive line belongs to it.
- **The word `end` is always optional** and not needed. No braces, ever.
- **No punctuation required.** No quotes, no commas, no `+`. Text joins with words
  ("combine A with B"), never with `+`.

---

## 3. Minimal symbols rule

User code avoids these characters as much as possible:
`+ _ { } " ' \ | ~ ^`

- Join text with words: `combine hello with player's name` instead of `+`.
- Blocks by indentation or the word `begin`/`end` — never `{}`.
- Quotes optional for chat; phrasing determines structure:
  `when player types (home)`.

Numbers keep working nicely because the lexer understands `1,000` and `1.5`.

---

## 4. Events

Events make something happen. Two kinds:

### 4.1 Immediate events (`when ...`)
Fire the moment the matching situation occurs in the world:

```
when player joins              when player quits
when player dies               when player gets killed by a zombie
when player touches water      when player touches lava
when player touches fire       when player is struck by lightning
when player falls into void    when player catches fire
when player freezes            when player heals
when player drinks a potion    when player gets hurt
when player breaks diamond ore when player places a block
when player collects emerald   when player picks up a diamond
when player reaches area castle
when player enters area boss room
when player leaves area arena
when player moves to area hub  when player starts sprinting
when player starts swimming    when player jumps
when player falls from a high place
when player drowns             when player starts sneaking
when player rides a horse      when player dismounts
when player wakes up           when player goes to bed
when player eats a golden apple
when player levels up          when player gains experience
when player chats              when player says hello
when player types (home)
when player uses command feed
when player teleports to Spawn
when player trades with villager
when player shears a sheep     when player milks a cow
when player opens a chest      when player closes a door
when player enchants a sword   when player fishes
when player right clicks       when player left clicks
when player wins game          when player loses game
when player throws a pearl     when player shoots a bow
when player hits a zombie      when player gets shot by an arrow
when player blocks an attack   when a creeper explodes near player
when player kills a mob        when mob dies
when player primes a tnt       when a player's tnt explodes
when player uses an item       when player switches item slot
when player opens inventory
when player plants a seed      when player harvests crops
when player tills farmland
when boss dies                 when boss spawns
```

The condition after `when` is the same language used by `if` (section 6), so you
can also write reactive statements:

```
when player has 10 emeralds
when player health is below 5
when it is nighttime
when player is in the nether
```

Add-ons can contribute entirely new "situation words" (section on plugins),
and any of the above words (say `quits`, `teleports to`) can be extended by
`priority high | priority low` to order handlers:

```
when player joins priority high
    announce A VIP has arrived
when player joins
    announce someone joined
when player joins priority low
    send title to player Welcome
```

Handlers run highest priority first; `cancel event` stops every remaining
handler for that event.

### 4.2 Timed events (`every ...`)
Run on a clock:

```
every 5 seconds
every 10 minutes
every hour
```

### 4.3 Server events (`on ...`)

```
on server start
on server stop
```

---

## 5. Custom actions (functions)

Give a group of lines a name, then call it anywhere by writing its name.

```
action reward player
    give player 10 diamonds
    give player 100 XP
    tell player You received a reward

when player completes quest
    reward player
```

Parameters are the bare words after the name (`player` above). Calls include the
same words. `player` automatically means the acting player.

---

## 6. Conditions

Conditions connect ideas with **and**, **or**, and **not**.

```
if player has 5 diamonds or player has gold
if player health is below 5 and it is nighttime
if not player is in the nether
if player is sprinting
if player is sneaking
if player is on ground
if player is airborne
if player is in vehicle
if player is holding a torch
if player is in the nether
if player is above y 60
if player is below y 10
if player name contains Hero
if server has more than 10 players online
if it is nighttime
if it is daytime
if it is raining
if it is a storm
if player has potion effect strength
```

Comparisons use every-day words:

```
is at least     /  at least
is more than    /  more than   /  is greater than
is less than    /  less than   /  is below
is equal to     /  equals      /  is exactly
is at most      /  no more than
```

---

## 7. Variables

A real variable system with friendly English, and several scopes:

| Scope      | How you write it          | Where it lives          |
|------------|---------------------------|-------------------------|
| player     | `player's coins`          | per player, permanent   |
| world      | `world highscore`         | whole server            |
| global     | `coins` (no prefix)       | whole server            |
| temp       | `temp score`              | only for this run       |

```
set player's coins to 100
add 10 to player's coins
remove 5 from player's coins
multiply player's coins by 2
divide player's coins by 4
set world highscore to 1000
set temp best to 5
```

Values can be numbers, text, true or false, lists, or maps.

### Persistence (survives restarts)

```
save player's coins to database
load player's coins
```

---

## 8. Loops and flow

```
repeat 10 times
    spawn zombie

repeat while player has less than 5 diamonds
    give player 1 diamond

until player reaches castle
    keep spawning enemies

for each online player
    give player 1 emerald

for each item in inventory
    check item

break      # leave the loop
continue   # skip to the next turn
stop       # stop the whole handler
```

`if` / `else if` / `else` are plain words:

```
every 5 seconds
    if player has 5 diamonds
        give player diamond
    else if player has 3 diamonds
        give player emerald
    else
        give player stick
```

---

## 9. Math and text, all in words

```
add 10 to player's coins
subtract 5 from player's coins
multiply player's coins by 2
divide player's coins by 4
increase player's coins by 1
decrease player's coins by 1
set player's coins to 0
set player's score to random number between 1 and 100
combine hello with player's name
```

---

## 10. Standard action library

| Area   | Actions |
|--------|---------|
| Chat   | `tell`, `warn`, `announce`, `title`, `toast`, `actionbar`, `welcome` |
| Items  | `give`, `take`, `clear item` |
| Life   | `kill`, `damage`, `heal`, `heal to full`, `ignite`, `freeze` |
| Move   | `teleport player to place`, `teleport player to x y z`, `set fly`, `set walk speed` |
| World  | `set weather to rain`, `set time to night`, `set block`, `break block`, `play sound`, `play particle`, `lightning` |
| Creatures | `spawn zombie`, `despawn zombie`, `set mob health`, `set mob speed`, `give effect`, `enchant` |
| Doors  | `open door`, `close door`, `open gate`, `close gate` |
| Game   | `win game`, `lose game`, `give XP`, `give levels`, `set gamemode` |
| Admin  | `ban`, `kick`, `give permission`, `remove permission` |
| Regions| `define area name from x1 z1 to x2 z2`, `teleport player to area` |
| Store  | `save X to database`, `load X` |

---

## 11. Comments

A line whose content starts with `note`, or `#`, is ignored.

```
note this explains something
# so does this
```

---

## 12. Errors are friendly

Verbum never says "Unexpected token at line 4". It says:

```
Problem on line 4:
  give diamonds player
I expected the player first.
Try:
  give player 5 diamonds
```

Every error tells you exactly what to write instead.

---

## 13. The pipeline

```
 .vb file
   -> Tokenizer (splits words/numbers, keeps indentation)
   -> Parser   (hand-written, builds the AST)
   -> AST      (events, actions, conditions, loops)
   -> Interpreter (runs against a runtime)
   -> McRuntime interface
        |-- MockMcRuntime   (offline fake world, for tests/demo)
        `-- PaperRuntime    (real Paper server) -> Paper plugin -> Minecraft
```

---

## 14. Acceptance test

The plugin/CLI ships with `acceptance.vb`, which must work end to end:

```
on server start
    define area victory area 0 0 200 200

when player touches water
    kill player

when player collects emerald
    add 1 to player's emeralds

when player has 10 emeralds
    announce Player Wins
    teleport player to victory area

when boss dies
    give all players 1 dragon egg
```

Run it offline: `java -jar verbum-engine.jar demo`   →   `ACCEPTANCE TEST: PASS`
