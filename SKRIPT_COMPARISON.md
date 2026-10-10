# Verbum vs Skript: Detailed Comparison

A brutally honest comparison for people deciding between them.

## Scoring (out of 10)

| Category | Skript | Verbum | Winner |
|----------|--------|--------|--------|
| **Syntax Readability** | 7 | 9 | Verbum |
| **Ease of Learning** | 6 | 8 | Verbum |
| **Documentation** | 8 | 7 | Skript |
| **Community Size** | 9 | 3 | Skript |
| **Shared Scripts** | 9 | 3 | Skript |
| **Ecosystem** | 8 | 4 | Skript |
| **Performance** | 7 | 9 | Verbum |
| **Stability** | 8 | 7 | Skript |
| **Extensibility** | 6 | 8 | Verbum |
| **Error Messages** | 6 | 9 | Verbum |
| **Variable System** | 5 | 8 | Verbum |
| **Type Safety** | 4 | 8 | Verbum |
| **Real-world Use** | 9 | 6 | Skript |
| **Active Development** | 6 | 9 | Verbum |
| **Learning Resources** | 8 | 5 | Skript |
| **Server Compatibility** | 8 | 8 | Tie |

**Overall:** Skript 7.1/10, Verbum 6.9/10 *(too close to call, depends on what you value)*

---

## Detailed comparison

### 1. Syntax readability (Verbum wins 9 vs 7)

**Example: Welcome and give items**

Skript:
```skript
on join:
    send "Welcome, %player%!" to player
    give player 1 diamond
    give player 1 emerald
```

Verbum:
```vb
when player joins
    welcome player with Welcome
    give player 1 diamond
    give player 1 emerald
```

**Why Verbum wins:**
- No colons (cleaner)
- No percent signs for variables
- `welcome` is a real word, not a side effect of `send`
- Reads more like instructions

**Why Skript is still good:**
- Reasonably readable if you know it
- Quotes make strings explicit
- Familiar to programmers

---

### 2. Ease of learning (Verbum wins 8 vs 6)

**For a non-programmer:**
- **Skript**: "It looks like code. What's `:` for? What's `%`?"
- **Verbum**: "It looks like English. I can guess what it does."

**For a programmer:**
- **Skript**: "Why is this not JavaScript? It's confusing."
- **Verbum**: "OK, this is a DSL. Clean."

**Verdict:** Verbum is gentler for beginners, Skript is easier for programmers.

---

### 3. Documentation (Skript wins 8 vs 7)

**Skript:**
- Tons of tutorials online
- Stack Overflow has answers
- Multiple wikis and communities
- Years of accumulated knowledge

**Verbum:**
- Clear, focused documentation
- Well-organized guides
- Honest, beginner-friendly
- Growing example library
- Less online searchable content

**Verdict:** Skript has more, Verbum's is clearer. Skript wins on quantity.

---

### 4. Community (Skript wins 9 vs 3)

**Skript:**
- Massive Discord (10k+ members)
- Tons of shared scripts
- Forum support
- Years of Q&A
- Multiple competing implementations

**Verbum:**
- Small but active Discord (100+ members)
- Growing script library
- GitHub discussions
- Direct access to developers
- Clean, single implementation

**Verdict:** Skript dominates. If you get stuck on Skript, you'll find help fast. Verbum is newer.

---

### 5. Performance (Verbum wins 9 vs 7)

**Skript:**
- Interpreted at runtime
- Can be slow on very large scripts
- Event handling is reasonably fast
- Memory overhead per script

**Verbum:**
- Java-compiled AST
- Faster event handling
- Better memory efficiency
- Native Java performance

**Real-world:** Both are fast enough for 99% of servers. Verbum will scale better if you have 100+ scripts.

---

### 6. Variables (Verbum wins 8 vs 5)

**Skript:**
```skript
set {player.coins.%player%} to 100
set {global.highscore} to {global.highscore} + 1
delete {player.coins.%player%}
```

Problems:
- Uses string keys, not real variables
- No scoping system
- `%player%` substitution is error-prone
- No type system

**Verbum:**
```vb
set player's coins to 100
add 1 to world highscore
set coins to 0  # Just reset it
```

Benefits:
- Real variable names
- Automatic scoping (player/world/global)
- Type-aware (number, text, list, map)
- Safer, cleaner syntax
- Built-in persistence

**Verdict:** Verbum's variable system is a huge upgrade.

---

### 7. Extensibility (Verbum wins 8 vs 6)

**Skript:**
- Add-ons exist but are less structured
- No type safety
- Harder to write custom verbs
- No official add-on SDK

**Verbum:**
- Typed add-on API
- Clear interfaces for actions/conditions
- Easy to register custom verbs
- Official add-on guide
- Better separation of concerns

**Verdict:** Verbum is easier to extend, but Skript has more existing add-ons.

---

### 8. Error messages (Verbum wins 9 vs 6)

**Skript error:**
```
ERROR: 'giveplayer 1 diamond' is not a valid action in an if-block.
```

(You have to guess what went wrong.)

**Verbum error:**
```
Problem on line 5:
    give 1 diamond to player

I expected the player first, then the item.
Try:
    give player 1 diamond
```

(It tells you exactly what to write.)

**Verdict:** Verbum's error messages are much more helpful.

---

### 9. Stability (Skript wins 8 vs 7)

**Skript:**
- Years of production use
- Very stable
- Known edge cases and workarounds
- Large plugin ecosystem tested with it

**Verbum:**
- Actively maintained
- Well-tested (good test coverage)
- Designed for stability
- Fewer real-world edge cases yet

**Verdict:** Skript has more proven stability. Verbum is close but newer.

---

### 10. Real-world usage (Skript wins 9 vs 6)

**Skript:**
- Used on thousands of servers
- Proven in production
- Community scripts you can copy
- Plugin compatibility tested with many

**Verbum:**
- Works well on production servers
- Fewer existing public scripts
- Growing real-world usage
- Clean plugin compatibility

**Verdict:** Skript has proven track record. Verbum is production-ready but less proven.

---

## When to choose each

### Choose **Skript** if:
- You want proven stability and years of track record
- You need to copy existing scripts from the community
- You're running a large server with lots of complex logic
- You want tons of online tutorials and help
- You're not willing to be an early adopter
- You need deep ecosystem support (lots of add-ons)

### Choose **Verbum** if:
- You want cleaner, more readable syntax
- You're teaching scripting to non-programmers
- You want better error messages and learning experience
- You like being part of something new
- You value code quality and architecture
- You're starting a small-to-medium server
- You want a better variable system
- You're willing to contribute examples/add-ons

### Use **both** if:
- You have a large server with many projects
- Some scripts are critical (use Skript)
- Some scripts are new (try Verbum)
- You want to experiment

---

## Honest take

**For a brand new server:** Verbum is the better choice. Cleaner, easier to learn, better error messages.

**For migrating from Skript:** Worth trying on a test server. You'll write cleaner code, but you'll lose community scripts.

**For mission-critical logic:** Skript is safer (more proven). Verbum is close.

**For learning:** Verbum is better. It reads like English, not code.

**For community support:** Skript is way ahead. Verbum is catching up.

---

## The future

**Skript** will remain stable and proven. It's the safe choice.

**Verbum** is designed to become the next generation. In 2 years, it will likely be a serious competitor.

Right now (2026), it's not "Skript vs Verbum". It's "proven tool vs promising future".

Pick Verbum if you're willing to help build that future. Pick Skript if you need proven stability today.
