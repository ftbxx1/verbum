# Verbum Ecosystem

Verbum's library of built-in features, examples, and add-on hooks.

## Built-in verbs (273 total)

Every verb here works on live servers. Grouped by area:

### Messages (8)
- `tell`, `warn`, `announce`, `title`, `toast`, `actionbar`, `welcome`, `broadcast`

### Inventory (3)
- `give`, `take`, `clear`

### Life (3)
- `kill`, `damage`, `heal`

### Movement (1)
- `teleport`

### World/State (15+)
- `apply`, `despawn`, `effect`, `enchant`, `explode`, `feed`, `freeze`, `ignite`, `lightning`, `particle`, `playsound`, `setblock`, `spawn`, `strike`, `unenchant`, `weather`

### Admin (35+)
- `ban`, `cancel`, `deop`, `gamemode`, `givepermission`, `glowing`, `gravity`, `hide`, `invisible`, `kick`, `make`, `mute`, `op`, `removepermission`, `reset`, `setarmor`, `setcooldown`, `setfly`, `vanish`, etc.

### Teams & Scoreboards (12)
- `addtoteam`, `createteam`, `removefromteam`, `createscoreboard`, `setscore`, `addscore`, `removescore`

### Boss Bars (7)
- `createbossbar`, `setbossbar`, `removebossbar`, `showbossbar`, `hidebossbar`, `setbossbarprogress`, `setbossbarcolor`

### Flags & Quests (4)
- `setflag`, `toggleflag`, `setquest`, `completequest`

### And many more...

See the full [command reference](verbum-engine/docs/COMMANDS.md).

## Event types (113 total)

Every situation the server can react to:

### Player events
- `when player joins`, `quits`, `dies`, `gets killed by`, `first joins`
- `when player breaks X`, `places X`, `collects X`, `picks up X`
- `when player chats`, `types`, `uses command`
- `when player levels up`, `gains experience`, `gets hurt`, `heals`
- `when player right clicks`, `left clicks`, `sneaks`, `sprints`, `swims`

### World events
- `when it rains`, `starts raining`, `stops raining`
- `when day starts`, `night starts`, `it is noon`, `it is midnight`
- `when server starts`, `server stops`

### Custom areas
- `when player reaches area X`
- `when player enters area X`
- `when player leaves area X`

### Mob events
- `when mob dies`, `boss dies`, `boss spawns`

See the full [event reference](verbum-engine/docs/COMMANDS.md#event-words-what-when-player--understands).

## Example scripts (14)

All ship with the plugin in `src/main/resources/scripts/examples/`:

1. **welcome.vb** - Greet players, give starter items
2. **commands.vb** - Custom commands (home, spawn, etc.)
3. **conditions.vb** - Health checks, time checks, team checks
4. **functions.vb** - Reusable actions (rewards, punishments)
5. **lists.vb** - Working with lists of items or players
6. **loops.vb** - Repeating logic (spawn waves, broadcast timers)
7. **quests.vb** - Multi-step quests with state tracking
8. **money.vb** - Economy system with player coins
9. **pvp_arena.vb** - PvP game with teams and scoring
10. **boss_fight.vb** - Boss spawning, health tracking, loot
11. **village.vb** - NPC interactions and custom shops
12. **weather.vb** - Dynamic weather events
13. **mega_events.vb** - Large, complex server events
14. **skript_style.vb** - Patterns that map from Skript

Each example is ~50-100 lines and fully commented.

## Add-on API

Extend Verbum with custom verbs (actions, conditions, functions).

### For plugin developers

```java
public class MyAddon implements VerbumPlugin {
    public String name() { return "MyAddon"; }
    
    public void register(EngineRegistrar r) {
        // Register custom actions
        r.action("do custom thing", (ctx, args) -> {
            // Your logic
        });
        
        // Register custom conditions
        r.condition("is custom", (ctx, args) -> {
            return true; // Your check
        });
    }
}
```

Then depend on `dev.verbum:verbum-engine` in your `pom.xml`.

See [ADDONS.md](verbum-engine/docs/ADDONS.md) for the full guide.

## Data persistence

Verbum has a built-in JSON file storage system:

```vb
set player's coins to 100
save player's coins to database

load player's coins from database
if player's coins is more than 0
    announce You have [player's coins] coins
```

Data lives in `plugins/Verbum/data/` automatically.

## Performance characteristics

- **Script parsing**: ~10ms per 1000-line script
- **Event handling**: <1ms per handler call (average)
- **Tick loop**: ~0.5ms per tick (20 ticks/second)
- **Memory**: ~2MB per 100 scripts + data

Verbum is designed to scale from small hobby servers to large networks.

## Compatibility

- **Paper**: Full support (Spigot, Purpur, Airplane compatible)
- **Minecraft versions**: 1.20 and newer (easily backported)
- **Java**: 21+
- **Other plugins**: Full Bukkit event access via add-ons

## Roadmap

### Current (v1.0+)
- Core language and 273 verbs ✅
- Event system with priorities ✅
- Variables and persistence ✅
- Add-on API ✅
- Online testing framework ✅

### Near-term
- Better error recovery (continue on partial failures)
- More robust command registration
- Performance profiling tools
- GUI menu builder
- Easier add-on packaging

### Future
- Standard library (database connectors, HTTP, etc.)
- Visual debugger
- Live reload without `/verbum reload`
- Package manager (`verbum get my-script`)
- Multi-language translations

## Community resources

- **Discord**: https://discord.gg/qnpHBEmbUC
- **GitHub Issues**: Report bugs, request features
- **GitHub Discussions**: Share scripts, ask questions
- **Wiki** (coming soon): Community-written guides
- **Script repository** (coming soon): Share your scripts

## Contributing

Want to help? We need:
- **Example scripts**: Real-world use cases
- **Add-ons**: Custom verbs for your plugins
- **Documentation**: Tutorials and guides
- **Translations**: International support
- **Testing**: Edge cases and edge cases

See [CONTRIBUTING.md](CONTRIBUTING.md).
