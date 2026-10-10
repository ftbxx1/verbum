# Verbum

> **Tell Minecraft what to do — in English.**

Verbum is a **beginner-friendly English scripting language** for Minecraft. Write Minecraft behavior in plain English sentences. No braces. No quotes. No syntax noise. A fifth-grader can read a Verbum script and instantly understand what it does.

## Quick Look

```vb
when player joins the world
    welcome player with Welcome to the server

when player breaks diamond ore
    give player 5 diamonds

if player has 5 kills
    announce Big score!
```

**That's it.** No symbols. No operators. Just English.

---

## Why Verbum?

Existing Minecraft scripting languages are either:
- **Text-based code** (Skript, JavaScript, Python) — requires learning syntax, operators, and programming concepts.
- **Visual programming** (Scratch, Blockly) — fun, but limited and slow to write at scale.

**Verbum is different.** It's a real, grammar-based language that reads like a rulebook:

| Feature | Value |
|---------|-------|
| **Language** | English (context-aware keywords) |
| **File type** | `.vb` or `.mcscript` |
| **Platform** | Paper (Bukkit-compatible server) + offline testing |
| **Learn time** | Instant for English speakers |
| **Scope** | Events → Conditions → Actions |

---

## What Can You Do?

### Basics

```vb
when player joins the world
    welcome player with Welcome
    give player a diamond
    teleport player to spawn

when player breaks diamond ore
    give player 5 diamonds
    announce [player] found diamonds!
```

### Conditions

```vb
when player right clicks
    if player is in creative
        announce You are in creative mode
    
    if player has a diamond sword
        give player 64 emeralds

if score of kills is at least 8
    announce Wow!
```

### Teams, Scoreboards, Flags

```vb
create team red
add Alex to team red
set score for Alex in kills to 5
add score for Alex in kills by 1
set flag pvp-enabled to true

if player is in team red
    announce Team red player detected
```

### Loops & Functions

```vb
command give_all
    repeat 5 times
        announce Broadcast message
        wait 1 second

command spawn_wave
    for each player in everyone
        spawn a zombie at [player]
```

### Complex Scripting

```vb
when player first joins
    set quest main to 0
    wait 5 seconds
    send message Alex Please take the quest

when player takes quest side-mission
    set cooldown [player] teleport for 10 seconds
    spawn 5 zombies at [player]
    wait until [player] kills 5 mobs
    complete quest side-mission
    announce [player] won!
```

---

## How It Works

```
📄 Your .vb script
  ↓
🔤 Tokenizer → split into words, preserve indentation
  ↓
📋 Parser → hand-written, builds syntax tree
  ↓
🌳 AST → events, actions, conditions, loops, functions
  ↓
▶️ Interpreter → executes the tree
  ↓
🎮 McRuntime → bridge to Minecraft
    ├─ MockMcRuntime (for testing offline)
    └─ PaperRuntime (real Paper server)
```

---

## Getting Started

### 1️⃣ Install on a Paper Server (3 steps)

**Step 1:** Download the latest `.jar` from [Releases](https://github.com/ftbxx1/verbum/releases)

**Step 2:** Drop it in your server's `plugins/` folder

**Step 3:** Restart the server

```bash
/verbum reload  # Reload scripts anytime
```

### 2️⃣ Create Your First Script

Create a file in `plugins/Verbum/scripts/hello.vb`:

```vb
when player joins the world
    announce Welcome [player]!
```

Then:
```bash
/verbum reload
```

Watch the server welcome new players.

### 3️⃣ Test Offline (No Server)

```bash
mvn -pl verbum-engine -am package
java -jar verbum-engine/target/verbum-engine-1.0.0.jar demo
```

Runs the full acceptance test suite.

---

## The Language

### Events (What triggers code)

```vb
when player joins the world
when player breaks diamond ore
when player right clicks
when player gets damaged
when player enters area spawn
when it starts raining
when day starts
```

### Conditions (If this, then...)

```vb
if player is in creative
if player has a diamond sword
if player is in team red
if score of kills is at least 5
if it is night
if player is sneaking
```

### Actions (Do this)

```vb
# Messages
announce Hello everyone
tell player You are here
send message [player] Click me

# Inventory
give player a diamond sword
take player 64 dirt
clear inventory [player]

# Gameplay
kill [player]
teleport [player] to spawn
spawn 5 zombies
set cooldown [player] attack for 5 seconds

# World
set weather to rain
set time to night
set block at spawn to bedrock
```

### Control Flow

```vb
if condition
    action
    action
else
    action

repeat 5 times
    action

for each player in everyone
    announce [player]

wait 10 seconds
wait until condition
```

---

## Documentation

- **[Full Language Spec](verbum-engine/docs/SPEC.md)** — Grammar, all keywords, advanced features
- **[Beginner Guide](verbum-engine/docs/GUIDE.md)** — Learn step-by-step with examples
- **[Commands Reference](verbum-engine/docs/COMMANDS.md)** — All 200+ verbs and queries
- **[Installation](verbum-paper/docs/INSTALL.md)** — Detailed setup for servers
- **[Add-Ons](verbum-engine/docs/ADDONS.md)** — Extend Verbum with custom plugins

---

## Examples

See `/verbum-engine/src/main/resources/scripts/examples/` for real scenarios:

- **welcome.vb** — Greet players
- **commands.vb** — Custom commands
- **functions.vb** — Reusable code
- **loops.vb** — Repeat logic
- **quests.vb** — Multi-step events
- **pvp_arena.vb** — Game logic
- **money.vb** — Economy system
- **boss_fight.vb** — Complex interaction

---

## Tests

```bash
# Unit tests (lexer, parser, interpreter)
mvn -pl verbum-engine test

# Full test suite (includes acceptance test)
powershell -File run-tests.ps1
```

---

## Architecture

**verbum-engine/**
- Pure Java, no Minecraft dependencies
- Lexer, Parser, AST, Interpreter
- Runtime interface for testing/extensibility
- 20+ unit test classes

**verbum-paper/**
- Paper plugin that bridges engine to live Minecraft
- Loads scripts from `plugins/Verbum/scripts/`
- Executes actions via Bukkit API
- Queries world state

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

---

## Community

Have ideas, questions, or scripts to share?

**Join our Discord:** https://discord.gg/qnpHBEmbUC

---

## License

MIT License — see [LICENSE](LICENSE)
