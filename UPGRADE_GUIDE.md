# Upgrading from Skript to Verbum

If you're using Skript and thinking about trying Verbum, this guide helps you translate your knowledge.

## The 1-minute comparison

| Feature | Skript | Verbum |
|---------|--------|--------|
| **Learning curve** | Moderate (code-like) | Very gentle (English) |
| **Syntax noise** | Medium (some symbols) | None (pure words) |
| **Performance** | Good | Excellent (Java-native) |
| **Ecosystem** | Massive (years old) | Growing (new project) |
| **Community** | Huge | Small but active |
| **Shared scripts** | Thousands | Growing library |
| **Support** | Lots of tutorials | Clear, focused docs |
| **Add-ons** | Many | Clean, typed API |
| **Server compatibility** | Very good | Very good |
| **Real-world use** | Battle-tested | Production-ready |

## Side-by-side examples

### Example 1: Welcome player

**Skript:**
```skript
on join:
    send "Welcome %player%!" to player
    give player 1 diamond
```

**Verbum:**
```vb
when player joins
    welcome player with Welcome!
    give player 1 diamond
```

**Translation:**
- `on join:` → `when player joins` (shorter, clearer)
- `send ... to player` → `welcome player with ...` or `tell player ...`
- No colons. Indentation says what belongs where.

---

### Example 2: Check player health

**Skript:**
```skript
every 5 seconds:
    if player's health < 5:
        send "You are dying!" to player
```

**Verbum:**
```vb
every 5 seconds
    if player health is below 5
        warn player You are dying
```

**Translation:**
- `every 5 seconds:` → `every 5 seconds` (no colon)
- `player's health < 5` → `player health is below 5` (full English, no `<`)
- `send ... to player` → `warn player ...` (color and formatting automatic)

---

### Example 3: Give reward

**Skript:**
```skript
command /reward:
    give player 10 diamonds
    add 100 to {player.coins.%player%}
    broadcast "%player% got a reward!"
```

**Verbum:**
```vb
command reward
    give player 10 diamonds
    add 100 to player's coins
    announce [player] got a reward
```

**Translation:**
- `command /reward:` → `command reward` (no slash, no colon)
- `{player.coins.%player%}` → `player's coins` (real variable system, not string keys)
- `broadcast` → `announce` (shorter, same meaning)
- `%player%` → `[player]` (cleaner variable syntax)

---

### Example 4: Multiple conditions

**Skript:**
```skript
if player is holding a diamond sword and player's health < 10:
    send "You have a sword but low health!" to player
else if player is in creative mode:
    send "Creative player detected" to player
else:
    send "Normal player" to player
```

**Verbum:**
```vb
if player is holding a diamond sword and player health is below 10
    tell player You have a sword but low health
else if player is in creative
    tell player Creative player detected
else
    tell player Normal player
```

**Translation:**
- `and` stays `and`, `or` stays `or` (natural logic)
- `player is in creative mode` → `player is in creative` (shorter)
- No punctuation anywhere
- Cleaner, easier to read

---

### Example 5: Loops

**Skript:**
```skript
loop all players:
    give loop-player 1 emerald
    send "You got an emerald" to loop-player
```

**Verbum:**
```vb
for each online player
    give player 1 emerald
    tell player You got an emerald
```

**Translation:**
- `loop all players:` → `for each online player`
- `loop-player` → `player` (cleaner, automatic in context)
- No colon, indentation only

---

## Key differences

### 1. Syntax philosophy

**Skript says:** "Like programming, but with words"
**Verbum says:** "Like English, pure and simple"

Skript still has:
- Colons (`:`) at the end of lines
- Curly braces for variables (`{variable.name}`)
- Percent signs for substitution (`%player%`)
- Some operator symbols (`<`, `>`)

Verbum removes all of that:
- No colons
- No braces
- Variables are just names (`player's coins`)
- Operators are words (`is below`, `is more than`)

### 2. Variables

**Skript:**
```skript
set {player.coins.%player%} to 100
add 10 to {player.coins.%player%}
```

**Verbum:**
```vb
set player's coins to 100
add 10 to player's coins
```

Verbum's variable system is:
- Cleaner syntax
- Automatic scoping (`player's` = per-player, `world` = global)
- Type-aware (numbers, text, lists, maps)
- Persistent (built-in save/load)

### 3. Events

**Skript:**
```skript
on player move:
    # Something happened

on right click holding diamond sword:
    # Custom condition

on player chat:
    cancel event  # Stop the chat
```

**Verbum:**
```vb
when player moves
    # Something happened

when player right clicks with diamond sword
    # Automatic condition parsing

when player chats
    cancel event  # Same
```

Verbum events:
- Read more naturally
- Automatic condition parsing (no extra `on ... where` syntax)
- Same event cancellation

### 4. Functions/Actions

**Skript:**
```skript
command /spawn_boss:
    function spawn_the_boss

function spawn_the_boss:
    spawn a zombie at player
    broadcast "Boss spawned!"
```

**Verbum:**
```vb
command spawn_boss
    spawn the boss

action spawn the boss
    spawn a zombie at player
    announce Boss spawned
```

Verbum actions:
- Simpler syntax
- Clearer intent (`action` vs `function`)
- No naming conflicts

## Common Skript patterns in Verbum

### Setting and checking values

**Skript:** `set {score} to 0` / `if {score} = 5`
**Verbum:** `set score to 0` / `if score is equal to 5` or `if score equals 5`

### Removing values

**Skript:** `delete {score}`
**Verbum:** `set score to 0` (or remove from list/map)

### Conditional broadcasts

**Skript:** `broadcast "Message" to player`
**Verbum:** `tell player Message` (for one player) or `announce Message` (everyone)

### Math operations

**Skript:**
```skript
add 5 to {coins}
subtract 3 from {coins}
multiply {coins} by 2
divide {coins} by 2
```

**Verbum:**
```vb
add 5 to coins
subtract 3 from coins
multiply coins by 2
divide coins by 2
```

## What Verbum does better

1. **Readability**: No symbols, pure English
2. **Variable scoping**: Automatic (`player's`, `world`, global)
3. **Type system**: Numbers, text, lists, maps with clear semantics
4. **Error messages**: Tells you exactly what to write
5. **Clean architecture**: Easier to extend with add-ons
6. **Performance**: Java-native, not scripted

## What Skript does better (for now)

1. **Community size**: Way more people, way more scripts to copy
2. **Ecosystem**: Mature plugins, tons of examples
3. **Flexibility**: More ways to do the same thing
4. **Production trust**: Years of battle-testing
5. **Documentation**: Tons of tutorials and Stack Overflow answers

## Migration checklist

- [ ] Read the [Verbum beginner guide](verbum-engine/docs/GUIDE.md)
- [ ] Look at [example scripts](verbum-engine/src/main/resources/scripts/examples/)
- [ ] Try the [offline demo](README.md#quick-start-offline)
- [ ] Install the plugin on a test server
- [ ] Convert one small Skript to Verbum (try `welcome.vb` first)
- [ ] Test with `/verbum reload`
- [ ] Check the [full command reference](verbum-engine/docs/COMMANDS.md)
- [ ] Join the [Discord](https://discord.gg/qnpHBEmbUC) for help

## My honest take

**If you know Skript:** Verbum will feel cleaner and more natural, but you'll lose the huge community. Start with a small project.

**If you're new to scripting:** Verbum is easier to learn and reads more naturally.

**If you need it today:** Stick with Skript. It's proven.

**If you're willing to be part of growing something:** Try Verbum. The language design is better, and the community is welcoming.
