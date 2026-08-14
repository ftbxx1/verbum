# Verbum

> **Tell Minecraft what to do — in English.**

Verbum is an original, beginner-friendly **English scripting language** for
Minecraft. No braces, no quotes, no plus signs. Just readable sentences and
indentation. A fifth-grader can read a Verbum script and know instantly what it
does.

```
when player joins the world
    welcome player with Welcome to the server

when player touches water
    kill player

when player breaks diamond ore
    give player 5 diamonds
```

## Why Verbum is different

It is **not** "Minecraft Python" or "Minecraft JavaScript". It is a brand-new
language with its own consistent grammar — a DAG of *events → conditions →
actions*, all written as real words. Keywords like `give`, `kill`, `spawn` are
words, never symbols.

| Identity | Value |
|----------|-------|
| Name     | **Verbum** |
| File     | `.vb` (also `.mcscript`) |
| Motto    | "Tell the game what to do." |
| v1 platform | Paper (server plugin), with an offline mock world for testing |

## What's in the box

```
verbum-engine/      The language core (pure Java, no Minecraft needed)
  src/main/java/dev/verbum/
    lex/            tokenizer
    parser/         hand-written parser
    ast/            syntax tree node definitions
    interp/         interpreter, variables, conditions, actions, math
    runtime/        McRuntime interface + MockMcRuntime (fake world)
    engine/         ScriptEngine facade
    cli/            VerbumCli (check / run / demo)
  src/main/resources/scripts/
    acceptance.vb   the end-to-end acceptance test
    game.mcscript   the flagship example
    examples/       10 more example scripts
  src/test/java/    lexer, parser & interpreter unit tests
  docs/             SPEC, GUIDE, COMMANDS
verbum-paper/       The real Paper plugin (bridge to live Minecraft)
  docs/INSTALL.md   install in 5 steps
```

## Architecture

```
 .vb file
   -> Tokenizer   split into words/numbers, keep indentation
   -> Parser      hand-written -> AST
   -> AST         events, actions, conditions, loops
   -> Interpreter executes
   -> McRuntime   the Minecraft bridge (interface)
        |-- MockMcRuntime   offline fake world (tests + demo)
        `-- PaperRuntime    real Paper server -> plugin -> Minecraft
```

## Quick start (offline)

```
mvn -pl verbum-engine -am package

java -jar verbum-engine/target/verbum-engine-1.0.0.jar demo
# -> ACCEPTANCE TEST: PASS
```

## The Big Library

Beyond the basic *give / kill / teleport / announce* verbs, Verbum ships a large
built-in library of Minecraft systems — scoreboards, teams, boss bars, quests,
flags, mutes, gamemodes, dimensions, and body sensors — all spelled in plain
sentences inside conditions and actions:

```
if score of kills is at least 8
    announce Big score!

set score for Alex in kills to 5
add score for Alex in kills by 3
create team red
add Alex to team red
create boss bar Wither with title The Wither
set boss bar Wither's progress to 0.5
set flag safe-zone to true

set quest main to 3
complete quest side-mission

if player is in team red
if player is muted
if player is in the nether
if player is swimming
if player is in creative
if it is noon
```

The same verbs are both **conditions** (questions asked of the live world) and
**actions** (changes to it), so a growing script reads like a natural-language
rulebook rather than code.

Newer batch (chat, player state, world, mobs, systems):

```
set join message to Welcome everyone
send hover message Alex Hello with tooltip Click me
send clickable text Alex Click with command /tp
hide chat

set sneaking Alex        vanish Alex        set invincible Alex
set cooldown Alex attack for 5 seconds
set armor points Alex to 8

make storm               set weather duration 300
set time speed 3         set player limit 20
spawn structure castle at Spawn

set mob ai zombie off    set mob follow zombie to Alex
set mob drop zombie to diamond

set sidebar title Welcome
set sidebar line 1 to Hello

when player enters a portal     when player gets damaged by a creeper
when player consumes a potion
```

Round three (live stats, environment & gear, enchanted items, more events):

```
if player has 5 kills            if player kill streak is at least 3
if player armor is at least 8    if player health percent is more than 50
if player is in lava             if player is under the open sky
if player weapon is diamond sword
if distance from Alex to Bob is at least 5

add a kill Alex                  add a death Alex
give player a diamond sword with sharpness 5
shoot a red firework

when player first joins         when it starts raining
when player takes fall damage   when day starts
when player right clicks on a villager     when player presses a button
when player goes to bed         when player wakes up
```

## Quick start (real server)

Build the plugin, drop it in `plugins/`, add files to
`plugins/Verbum/scripts/`, run `/verbum reload`. Full steps: see
`verbum-paper/docs/INSTALL.md`.

## Tests

```
mvn -pl verbum-engine test        # lexer, parser, interpreter + mock runtime
powershell -File run-tests.ps1    # acceptance demo + full test suite in one go
```

## Learn more

- [Full language specification](verbum-engine/docs/SPEC.md)
- [Beginner guide](verbum-engine/docs/GUIDE.md)
- [Command reference](verbum-engine/docs/COMMANDS.md)
- [Install on a server](verbum-paper/docs/INSTALL.md)
