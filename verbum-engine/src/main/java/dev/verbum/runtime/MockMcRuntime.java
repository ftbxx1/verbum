package dev.verbum.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An in-memory fake Minecraft world used for offline tests and the acceptance
 * demo. It behaves like the real server for everything Verbum's examples need:
 * inventories, health, positions, areas, weather, time and spawning.
 */
public final class MockMcRuntime implements McRuntime {

    /** A named rectangular area (used for "reaches area" / "is in"). */
    public static final class Area {
        public final String name;
        public final Location a, b;
        public Area(String name, Location a, Location b) {
            this.name = name;
            this.a = a;
            this.b = b;
        }
        public boolean contains(Location p) {
            return p.x() >= min(a.x(), b.x()) && p.x() <= max(a.x(), b.x())
                && p.z() >= min(a.z(), b.z()) && p.z() <= max(a.z(), b.z());
        }
        public Location middle() {
            return Location.at(a.world(), (a.x()+b.x())/2, (a.y()+b.y())/2, (a.z()+b.z())/2);
        }
    }

    /** A live (fake) player. */
    public static final class MockPlayer {
        public String name;
        public boolean online = true;
        public final Map<String, Double> inventory = new HashMap<>();
        public String holding = "";
        public final List<String> wearing = new ArrayList<>();
        public double hp = 20;
        public double food = 20;
        public int level = 0;
        public double xp = 0;
        public Location loc = Location.at("world", 0, 64, 0);
        public String dimension = "overworld";
        public String biome = "plains";
        public String gamemode = "survival";
        public boolean sneaking, sprinting, inVehicle, onGround = true, flying, burning, poisoned;
        public boolean swimming, gliding, inWater, falling, climbing;
        public boolean inLava, inBed, underSky;
        public String weapon = "";
        public double armorValue = 0;
        public double maxHp = 20;
        public boolean op = false;
        public final List<String> effects = new ArrayList<>();
        public final List<String> permissions = new ArrayList<>();
        public boolean alive = true;
    }

    public final Map<String, Area> areas = new HashMap<>();
    public final Map<String, Location> namedPlaces = new HashMap<>();
    public final Map<String, MockPlayer> players = new HashMap<>();
    public final Map<String, List<Location>> mobs = new HashMap<>();       // type -> spawn points
    public final Map<String, Double> mobHealth = new HashMap<>();
    public final Map<String, Boolean> mobHostile = new HashMap<>();
    public final Map<String, Boolean> doors = new HashMap<>();            // place -> open
    public final Map<String, Double> scores = new HashMap<>();
    public final List<String> chatter = new ArrayList<>();                // every tell/announce
    public final List<String> log = new ArrayList<>();                    // every action

    public String weather = "sunny";
    public long timeTicks = 6000;   // midday

    // ---- MockPlayer helpers -------------------------------------------------

    public MockPlayer player(String name) {
        return players.computeIfAbsent(name, n -> {
            MockPlayer p = new MockPlayer();
            p.name = n;
            return p;
        });
    }

    private List<String> targets(String target) {
        List<String> out = new ArrayList<>();
        if (target == null) return out;
        if (target.equalsIgnoreCase(ALL) || target.equalsIgnoreCase("everyone")
                || target.equalsIgnoreCase("players")) {
            for (MockPlayer p : players.values()) if (p.online) out.add(p.name);
        } else {
            out.add(target);
        }
        return out;
    }

    // ---- regions -------------------------------------------------------------

    @Override public void defineArea(String name, Location a, Location b) {
        areas.put(normalize(name), new Area(normalize(name), a, b));
        namedPlaces.put(normalize(name), new Area(normalize(name), a, b).middle());
    }

    @Override public void teleportToCoords(String target, Location loc) {
        for (String t : targets(target)) {
            player(t).loc = loc;
            log("teleport " + t + " to " + loc);
        }
    }

    @Override public Location locationOf(String target) {
        return player(target).loc;
    }

    @Override public void executeCommand(String sender, String command) {
        log("execute " + sender + " " + command);
    }

    @Override public void teleportTo(String target, String namedPlace) {
        String n = normalize(namedPlace);
        Location loc = namedPlaces.get(n);
        if (loc == null) loc = areas.containsKey(n) ? areas.get(n).middle() : null;
        if (loc == null && players.containsKey(n)) {
            loc = players.get(n).loc;   // teleport to another player
        }
        if (loc == null) {
            // treat as coords text (not supported here)
            throw new dev.verbum.error.VerbumError("I do not know the place  " + namedPlace + "\nDefine it with  define area " + namedPlace);
        }
        for (String t : targets(target)) {
            player(t).loc = loc;
            log("teleport " + t + " to " + n);
        }
    }

    // ---- messages ------------------------------------------------------------

    @Override public void tell(String target, String message) {
        for (String t : targets(target)) { chatter.add("tell " + t + ": " + message); log("tell " + t + " " + message); }
    }
    @Override public void warn(String target, String message) {
        for (String t : targets(target)) { chatter.add("warn " + t + ": " + message); }
    }
    @Override public void announce(String message) {
        chatter.add("announce: " + message); log("announce " + message);
    }
    @Override public void title(String target, String title, String subtitle) {
        for (String t : targets(target)) chatter.add("title " + t + ": " + title + " / " + subtitle);
    }
    @Override public void toast(String target, String message) {
        for (String t : targets(target)) chatter.add("toast " + t + ": " + message);
    }
    @Override public void actionbar(String target, String message) {
        for (String t : targets(target)) chatter.add("actionbar " + t + ": " + message);
    }

    // ---- inventory -----------------------------------------------------------

    @Override public void give(String target, String item, double count) {
        for (String t : targets(target)) {
            MockPlayer p = player(t);
            p.inventory.merge(normalize(item), count, Double::sum);
            log("give " + t + " " + count + " " + item);
        }
    }
    @Override public void take(String target, String item, double count) {
        for (String t : targets(target)) {
            MockPlayer p = player(t);
            p.inventory.computeIfPresent(normalize(item), (k, v) -> Math.max(0, v - count));
        }
    }
    @Override public void clearItem(String target, String item) {
        for (String t : targets(target)) player(t).inventory.remove(normalize(item));
    }
    @Override public void drop(String target, String item, double count) {
        String c = (count == Math.rint(count)) ? String.valueOf((long) count) : String.valueOf(count);
        for (String t : targets(target)) log("drop " + c + " " + item + " at " + t);
    }

    // ---- item metadata -------------------------------------------------------

    @Override public void renameItem(String target, String item, String name) {
        for (String t : targets(target)) log("rename " + t + " " + item + " to " + name);
    }
    @Override public void setLore(String target, String item, String lore) {
        for (String t : targets(target)) log("lore " + t + " " + item + " to " + lore);
    }
    @Override public void setCustomModelData(String target, String item, int data) {
        for (String t : targets(target)) log("modeldata " + t + " " + item + " to " + data);
    }

    // ---- life -----------------------------------------------------------------

    @Override public void kill(String target) {
        for (String t : targets(target)) { player(t).hp = 0; player(t).alive = false; log("kill " + t); }
    }
    @Override public void damage(String target, double amount) {
        for (String t : targets(target)) { player(t).hp = Math.max(0, player(t).hp - amount); }
    }
    @Override public void heal(String target, double amount) {
        for (String t : targets(target)) { player(t).hp = Math.min(20, player(t).hp + amount); }
    }
    @Override public void healToFull(String target) {
        for (String t : targets(target)) player(t).hp = 20;
    }
    @Override public void ignite(String target, int seconds) {
        for (String t : targets(target)) player(t).burning = seconds > 0;
    }
    @Override public void freeze(String target, int seconds) {
        for (String t : targets(target)) { /* freezing is cosmetic in the mock */ }
    }
    @Override public void setFly(String target, boolean canFly) {
        for (String t : targets(target)) player(t).flying = canFly;
    }
    @Override public void explode(String target, double power) {
        for (String t : targets(target)) log("explode " + t + " power " + power);
    }
    @Override public void feed(String target, double food) {
        for (String t : targets(target)) { player(t).food = Math.min(20, food <= 0 ? 20 : food); }
    }
    @Override public void setOperator(String target, boolean op) {
        for (String t : targets(target)) { player(t).op = op; log("op " + t + " " + op); }
    }
    @Override public void setInvisible(String target, boolean invisible) {
        for (String t : targets(target)) log("invisible " + t + " " + invisible);
    }
    @Override public void setGlowing(String target, boolean glowing) {
        for (String t : targets(target)) log("glowing " + t + " " + glowing);
    }
    @Override public void setGravity(String target, boolean gravity) {
        for (String t : targets(target)) log("gravity " + t + " " + gravity);
    }
    @Override public void resetPlayer(String target) {
        for (String t : targets(target)) {
            MockPlayer p = player(t);
            p.inventory.clear();
            p.effects.clear();
            p.hp = 20;
            p.food = 20;
            p.xp = 0;
            p.level = 0;
            log("reset " + t);
        }
    }
    @Override public void setWalkSpeed(String target, double speed) {
        for (String t : targets(target)) { /* cosmetic in mock */ }
    }
    @Override public void setGamemode(String target, String gamemode) {
        for (String t : targets(target)) player(t).gamemode = normalize(gamemode);
    }
    @Override public void giveXp(String target, double amount) {
        for (String t : targets(target)) player(t).xp += amount;
    }
    @Override public void giveLevels(String target, double amount) {
        for (String t : targets(target)) player(t).level += (int) amount;
    }

    // ---- world -----------------------------------------------------------------

    @Override public void setWeather(String weather) { this.weather = normalize(weather); }
    @Override public void setTime(String time) {
        switch (normalize(time)) {
            case "day": timeTicks = 6000; break;
            case "night": timeTicks = 18000; break;
            case "noon": timeTicks = 6000; break;
            case "midnight": timeTicks = 18000; break;
            default: timeTicks = 6000;
        }
    }
    @Override public void setBlock(String block, Location at) {
        log("set block " + block + " at " + at);
    }
    @Override public void breakBlock(Location at) { log("break block at " + at); }
    @Override public void playSound(String target, String sound) {
        for (String t : targets(target)) log("sound " + t + " " + sound);
    }
    @Override public void playParticle(String target, String particle) {
        for (String t : targets(target)) log("particle " + t + " " + particle);
    }
    @Override public void lightningAt(String target) {
        for (String t : targets(target)) log("lightning " + t);
    }

    // ---- entities ----------------------------------------------------------------

    @Override public void spawn(String mob, Location at, int count) {
        String m = normalize(mob);
        mobs.computeIfAbsent(m, k -> new ArrayList<>());
        for (int i = 0; i < count; i++) mobs.get(m).add(at);
        mobHealth.putIfAbsent(m, 20.0);
        log("spawn " + count + " " + mob + " at " + at);
    }
    @Override public void despawn(String mob) {
        mobs.remove(normalize(mob));
    }
    @Override public void setMobHealth(String mob, double health) { mobHealth.put(normalize(mob), health); }
    @Override public void setMobSpeed(String mob, double speed) { /* cosmetic */ }
    @Override public void setMobHostility(String mob, boolean hostile) { mobHostile.put(normalize(mob), hostile); }
    @Override public void enchant(String target, String enchant, String item, int level) {
        for (String t : targets(target)) log("enchant " + t + " " + item + " " + enchant + " " + level);
    }
    @Override public void unenchant(String target, String enchant) {
        for (String t : targets(target)) log("unenchant " + t + " " + enchant);
    }
    @Override public void giveEffect(String target, String effect, int seconds, int level) {
        for (String t : targets(target)) player(t).effects.add(normalize(effect));
    }
    @Override public void strikeLightningAt(String target) {
        for (String t : targets(target)) log("lightning " + t);
    }

    // ---- world state ---------------------------------------------------------------

    @Override public void openDoor(String place) { doors.put(normalize(place), true); log("open door " + place); }
    @Override public void closeDoor(String place) { doors.put(normalize(place), false); log("close door " + place); }
    @Override public void openGate(String place) { doors.put(normalize(place), true); log("open gate " + place); }
    @Override public void closeGate(String place) { doors.put(normalize(place), false); log("close gate " + place); }
    @Override public void winGame(String target) { for (String t : targets(target)) { player(t).alive = true; log("win " + t); } }
    @Override public void loseGame(String target) { for (String t : targets(target)) log("lose " + t); }

    // ---- server admin -----------------------------------------------------------------

    @Override public void ban(String target) { for (String t : targets(target)) player(t).online = false; }
    @Override public void kick(String target, String reason) { for (String t : targets(target)) player(t).online = false; }
    @Override public void kickPlayer(String target, String reason) { for (String t : targets(target)) player(t).online = false; }
    @Override public void givePermission(String target, String permission) {
        for (String t : targets(target)) player(t).permissions.add(normalize(permission));
    }
    @Override public void removePermission(String target, String permission) {
        for (String t : targets(target)) player(t).permissions.remove(normalize(permission));
    }

    // ---- queries -----------------------------------------------------------------------

    @Override public boolean hasItem(String target, String item, double atLeast) {
        return targets(target).stream()
                .anyMatch(t -> player(t).inventory.getOrDefault(normalize(item), 0.0) >= atLeast);
    }
    @Override public boolean isHolding(String target, String item) {
        return targets(target).stream().anyMatch(t -> normalize(item).equals(normalize(player(t).holding)));
    }
    @Override public double health(String target) { return player(target).hp; }
    @Override public double maxHealth(String target) { return 20; }
    @Override public double food(String target) { return player(target).food; }
    @Override public int level(String target) { return player(target).level; }
    @Override public boolean isIn(String target, String place) {
        if (targets(target).isEmpty()) return false;
        String p = normalize(place);
        MockPlayer pl = player(target);
        if (p.equals("nether")) return pl.dimension.equalsIgnoreCase("nether");
        if (p.equals("the end") || p.equals("end")) return pl.dimension.equalsIgnoreCase("end");
        if (p.equals("overworld")) return pl.dimension.equalsIgnoreCase("overworld");
        Area a = areas.get(p);
        return a != null && a.contains(pl.loc);
    }
    @Override public boolean isNight() { return timeTicks >= 13000 && timeTicks <= 23000; }
    @Override public boolean isDay() { return !isNight(); }
    @Override public boolean isRain() { return weather.equalsIgnoreCase("rain") || weather.equalsIgnoreCase("storm"); }
    @Override public boolean isStorm() { return weather.equalsIgnoreCase("storm"); }
    @Override public int onlinePlayers() { return (int) players.values().stream().filter(p -> p.online).count(); }
    @Override public boolean playerOnline(String name) { MockPlayer p = players.get(normalize(name)); return p != null && p.online; }
    @Override public double coord(String target, char axis) {
        MockPlayer p = player(target);
        return switch (axis) { case 'x' -> p.loc.x(); case 'y' -> p.loc.y(); default -> p.loc.z(); };
    }
    @Override public String dimension(String target) { return player(target).dimension; }
    @Override public String biome(String target) { return player(target).biome; }
    @Override public String gamemode(String target) { return player(target).gamemode; }
    @Override public boolean isSneaking(String target) { return player(target).sneaking; }
    @Override public boolean isSprinting(String target) { return player(target).sprinting; }
    @Override public boolean isInVehicle(String target) { return player(target).inVehicle; }
    @Override public boolean isOnGround(String target) { return player(target).onGround; }
    @Override public boolean isFlying(String target) { return player(target).flying; }
    @Override public boolean isBurning(String target) { return player(target).burning; }
    @Override public boolean isPoisoned(String target) { return player(target).poisoned; }
    @Override public boolean hasEffect(String target, String effect) { return player(target).effects.contains(normalize(effect)); }
    @Override public boolean isOp(String target) { return targets(target).stream().allMatch(t -> player(t).op); }
    @Override public boolean hasPermission(String target, String permission) { return targets(target).stream().allMatch(t -> player(t).permissions.contains(normalize(permission))); }
    @Override public boolean playerAlive(String target) { return player(target).alive && player(target).hp > 0; }
    @Override public int mobCountNear(String target, String mob, double radius) {
        return mobs.getOrDefault(normalize(mob), List.of()).size();
    }
    @Override public double getScore(String scoreboard) { return scores.getOrDefault(normalize(scoreboard), 0.0); }
    @Override public boolean isBossHalfHealth(String mob) {
        Double h = mobHealth.get(normalize(mob));
        return h != null && h <= 10;
    }

    @Override public boolean isSwimming(String target) { return targets(target).stream().anyMatch(t -> player(t).swimming); }
    @Override public boolean isGliding(String target) { return targets(target).stream().anyMatch(t -> player(t).gliding); }
    @Override public boolean isInWater(String target) { return targets(target).stream().anyMatch(t -> player(t).inWater); }
    @Override public boolean isFalling(String target) { return targets(target).stream().anyMatch(t -> player(t).falling); }
    @Override public boolean isClimbing(String target) { return targets(target).stream().anyMatch(t -> player(t).climbing); }
    @Override public boolean isWearing(String target, String armor) { return targets(target).stream().anyMatch(t -> player(t).wearing.contains(normalize(armor))); }
    @Override public String timeOfDay() {
        long t = timeTicks;
        if (t == 6000) return "noon";
        if (t == 18000) return "midnight";
        return (t >= 0 && t < 12000) ? "day" : "night";
    }

    // ---- persistence -------------------------------------------------------------------

    private final Map<String, Object> persistent = new HashMap<>();
    @Override public Object loadPersistent(String key) { return persistent.get(key); }
    @Override public void savePersistent(String key, Object value) { persistent.put(key, value); }

    // ---- helpers ------------------------------------------------------------------------

    private void log(String s) { log.add(s); }

    // ---- the big library: recorded, testable state ---------------------------------

    public final Map<String, List<String>> teams = new HashMap<>();
    public final Map<String, Double> bossBars = new HashMap<>();
    public final Map<String, String> bossBarColors = new HashMap<>();
    public final Map<String, Boolean> mutes = new HashMap<>();
    public final Map<String, Boolean> flags = new HashMap<>();
    public final Map<String, Double> quests = new HashMap<>();
    public final Map<String, Boolean> questsDone = new HashMap<>();

    // ---- chat & messages -----------------------------------------------------
    public String joinMessage;
    public String quitMessage;
    public Boolean publicChat;
    public Boolean privateChat;
    public final List<String> clearedChat = new ArrayList<>();
    public final List<String> hoverMessages = new ArrayList<>();
    public final List<String> clickMessages = new ArrayList<>();

    // ---- player meta & state --------------------------------------------------
    public final Map<String, Boolean> sneakingState = new HashMap<>();
    public final Map<String, Boolean> sprintingState = new HashMap<>();
    public final Map<String, List<String>> hiddenFrom = new HashMap<>();
    public final Map<String, Boolean> noFallDamage = new HashMap<>();
    public final Map<String, Boolean> invinciblePlayers = new HashMap<>();
    public final Map<String, Double> cooldownSeconds = new HashMap<>();

    // ---- world & environment ---------------------------------------------------
    public double weatherDuration = 0;
    public boolean storm = false;
    public boolean thunder = false;
    public double timeSpeed = 1;
    public int playerLimit = 0;
    public final List<String> structures = new ArrayList<>();

    // ---- entities & mobs --------------------------------------------------------
    public final Map<String, Boolean> mobAi = new HashMap<>();
    public final Map<String, Boolean> mobGravity = new HashMap<>();
    public final Map<String, Boolean> mobFlying = new HashMap<>();
    public final Map<String, Boolean> mobBreeding = new HashMap<>();
    public final Map<String, String> mobCustomDrops = new HashMap<>();
    public final Map<String, String> mobFollows = new HashMap<>();
    public final Map<String, String> mobTargets = new HashMap<>();
    public final Map<String, Boolean> mobNameVisible = new HashMap<>();
    public final Map<String, Boolean> mobPersistent = new HashMap<>();

    // ---- systems & logic ---------------------------------------------------------
    public String sidebarTitle = "";
    public final List<String> sidebarLines = new ArrayList<>();

    // ---- live stats ---------------------------------------------------------------
    public final Map<String, Integer> killCounts = new HashMap<>();
    public final Map<String, Integer> deathCounts = new HashMap<>();
    public final Map<String, Integer> killStreaks = new HashMap<>();

    private String scoreKey(String objective, String player) { return normalize(objective) + ":" + normalize(player); }

    @Override public void createScoreboard(String objective, String displayName) { log("scoreboard " + objective + " = " + displayName); }
    @Override public void setScoreboardDisplay(String objective) { log("scoreboard display " + objective); }
    @Override public void deleteScoreboard(String objective) { log("scoreboard delete " + objective); }
    @Override public void setScore(String objective, String player, double value) { scores.put(scoreKey(objective, player), value); log("score " + player + " " + objective + " = " + value); }
    @Override public void addScore(String objective, String player, double value) { scores.merge(scoreKey(objective, player), value, Double::sum); log("score add " + player + " " + objective + " +" + value); }
    @Override public void removeScore(String objective, String player, double value) { scores.merge(scoreKey(objective, player), -value, Double::sum); log("score remove " + player + " " + objective + " -" + value); }
    @Override public double score(String objective, String player) { return scores.getOrDefault(scoreKey(objective, player), 0.0); }

    @Override public void createTeam(String name) { teams.computeIfAbsent(normalize(name), k -> new ArrayList<>()); log("team create " + name); }
    @Override public void teamAdd(String team, String player) { teams.computeIfAbsent(normalize(team), k -> new ArrayList<>()).add(player); log("team add " + player + " " + team); }
    @Override public void teamRemove(String team, String player) { var l = teams.get(normalize(team)); if (l != null) l.remove(player); log("team remove " + player + " " + team); }
    @Override public List<String> teamMembers(String team) { return teams.getOrDefault(normalize(team), List.of()); }
    @Override public void teamColor(String team, String color) { log("team color " + team + " " + color); }
    @Override public void teamPrefix(String team, String prefix) { log("team prefix " + team + " " + prefix); }
    @Override public void teamSuffix(String team, String suffix) { log("team suffix " + team + " " + suffix); }

    @Override public void createBossBar(String name, String title) { bossBars.put(normalize(name), 1.0); log("bossbar " + name + " = " + title); }
    @Override public void bossBarTitle(String name) { log("bossbar title " + name); }
    @Override public void bossBarProgress(String name, double progress) { bossBars.put(normalize(name), progress); log("bossbar progress " + name + " " + progress); }
    @Override public void bossBarColor(String name, String color) { bossBarColors.put(normalize(name), color); log("bossbar color " + name + " " + color); }
    @Override public void bossBarStyle(String name, String style) { log("bossbar style " + name + " " + style); }
    @Override public void bossBarVisible(String name, boolean visible) { log("bossbar visible " + name + " " + visible); }
    @Override public void removeBossBar(String name) { bossBars.remove(normalize(name)); log("bossbar remove " + name); }

    @Override public void setMute(String target, boolean muted) { for (String t : targets(target)) mutes.put(t.toLowerCase(), muted); }
    @Override public boolean isMuted(String target) { return mutes.getOrDefault(target.toLowerCase(), false); }

    @Override public void setFlag(String name, boolean value) { flags.put(normalize(name), value); }
    @Override public boolean getFlag(String name) { return flags.getOrDefault(normalize(name), false); }

    @Override public void setQuest(String quest, double value) { quests.put(normalize(quest), value); }
    @Override public double questProgress(String quest) { return quests.getOrDefault(normalize(quest), 0.0); }
    @Override public void completeQuest(String quest) { questsDone.put(normalize(quest), true); }
    @Override public boolean questDone(String quest) { return questsDone.getOrDefault(normalize(quest), false); }

    @Override public void startGame(String gameName) { log("game start " + gameName); }
    @Override public void endGame(String gameName) { log("game end " + gameName); }
    @Override public void setRound(int round) { log("round " + round); }
    @Override public void nextRound() { log("round next"); }

    @Override public void removeEffect(String target, String effect) { for (String t : targets(target)) { player(t).effects.remove(effect.toLowerCase()); log("uneffect " + t + " " + effect); } }
    @Override public void setHealth(String target, double amount) { for (String t : targets(target)) { player(t).hp = amount; log("set health " + t + " " + amount); } }
    @Override public void setMaxHealth(String target, double amount) { for (String t : targets(target)) log("set max health " + t + " " + amount); }
    @Override public void setFood(String target, double amount) { for (String t : targets(target)) { player(t).food = amount; log("set food " + t + " " + amount); } }
    @Override public void setLevel(String target, double amount) { for (String t : targets(target)) { player(t).level = (int) amount; log("set level " + t + " " + amount); } }
    @Override public void setInvulnerable(String target, boolean invulnerable) { for (String t : targets(target)) log("invulnerable " + t + " " + invulnerable); }

    @Override public void setItemFlag(String target, String item, String flag, boolean on) { for (String t : targets(target)) log("itemflag " + t + " " + item + " " + flag + " " + on); }
    @Override public void giveArmor(String target, String armor) { for (String t : targets(target)) log("armor " + t + " " + armor); }
    @Override public void setHeldItem(String target, String item) { for (String t : targets(target)) { player(t).holding = item; log("held " + t + " " + item); } }
    @Override public void setOffhandItem(String target, String item) { for (String t : targets(target)) log("offhand " + t + " " + item); }

    @Override public void openMenu(String target, String menuName) { for (String t : targets(target)) log("menu " + t + " " + menuName); }
    @Override public void openAnvil(String target) { for (String t : targets(target)) log("anvil " + t); }
    @Override public void openWorkbench(String target) { for (String t : targets(target)) log("workbench " + t); }
    @Override public void openShop(String target) { for (String t : targets(target)) log("shop " + t); }

    @Override public void shoot(String target, String projectile) { for (String t : targets(target)) log("shoot " + t + " " + projectile); }
    @Override public void throwItem(String target, String item) { for (String t : targets(target)) log("throw " + t + " " + item); }
    @Override public void shootFirework(String target) { for (String t : targets(target)) log("firework " + t); }

    @Override public void setMobAge(String mob, int age) { log("mob age " + mob + " " + age); }
    @Override public void setMobSize(String mob, double size) { log("mob size " + mob + " " + size); }
    @Override public void setMobPitch(String mob, double pitch) { log("mob pitch " + mob + " " + pitch); }
    @Override public void tameMob(String mob) { log("tame " + mob); }
    @Override public void nameMob(String mob, String name) { log("name mob " + mob + " " + name); }

    @Override public void sendToServer(String target, String server) { for (String t : targets(target)) log("send " + t + " to " + server); }
    @Override public void setTabList(String target, String name) { for (String t : targets(target)) log("tablist " + t + " " + name); }
    @Override public void setClickCommand(String target, String command) { for (String t : targets(target)) log("clickcmd " + t + " " + command); }
    @Override public void setSignText(Location at, String text) { log("sign at " + at + " " + text); }
    @Override public void setVillagerPrice(String villager, String item, double price) { log("villagerprice " + villager + " " + item + " " + price); }
    @Override public void setWorldBorder(double radius) { log("border " + radius); }
    @Override public void setDifficulty(String difficulty) { log("difficulty " + difficulty); }
    @Override public void setSpawnPoint(Location at) { log("spawnpoint " + at); }
    @Override public void setMobLimit(int limit) { log("moblimit " + limit); }
    @Override public void setWorldRule(String rule, boolean enabled) { log("rule " + rule + " " + enabled); }
    @Override public void setRedstone(String place, boolean powered) { log("redstone " + place + " " + powered); }

    // ---- chat & messages -----------------------------------------------------
    @Override public void setJoinMessage(String message) { joinMessage = message; log("join message " + message); }
    @Override public void setQuitMessage(String message) { quitMessage = message; log("quit message " + message); }
    @Override public void setPublicChat(boolean enabled) { publicChat = enabled; log("public chat " + enabled); }
    @Override public void setPrivateChat(boolean enabled) { privateChat = enabled; log("private chat " + enabled); }
    @Override public void clearChat(String target) { clearedChat.add(target); log("clear chat " + target); }
    @Override public void sendHoverMessage(String target, String text, String hover) {
        hoverMessages.add(text + " | " + hover);
        for (String t : targets(target)) log("hover " + t + " " + text + " | " + hover);
    }
    @Override public void sendClickMessage(String target, String text, String command) {
        clickMessages.add(text + " | " + command);
        for (String t : targets(target)) log("clickmsg " + t + " " + text + " | " + command);
    }

    // ---- player meta & state --------------------------------------------------
    @Override public void setSneaking(String target, boolean sneaking) {
        for (String t : targets(target)) { player(t).sneaking = sneaking; sneakingState.put(t.toLowerCase(), sneaking); }
    }
    @Override public void setSprinting(String target, boolean sprinting) {
        for (String t : targets(target)) { player(t).sprinting = sprinting; sprintingState.put(t.toLowerCase(), sprinting); }
    }
    @Override public void setHideFrom(String target, String other) {
        for (String t : targets(target)) hiddenFrom.computeIfAbsent(t.toLowerCase(), k -> new ArrayList<>()).add(other);
    }
    @Override public void setShowTo(String target, String other) {
        for (String t : targets(target)) {
            List<String> h = hiddenFrom.get(t.toLowerCase());
            if (h != null) h.remove(other);
        }
    }
    @Override public void setArmor(String target, double points) { for (String t : targets(target)) log("armor points " + t + " " + points); }
    @Override public void setAbsorption(String target, double hearts) { for (String t : targets(target)) log("absorption " + t + " " + hearts); }
    @Override public void cancelFallDamage(String target, boolean cancel) { for (String t : targets(target)) noFallDamage.put(t.toLowerCase(), cancel); }
    @Override public void setInvincible(String target, boolean invincible) {
        for (String t : targets(target)) { invinciblePlayers.put(t.toLowerCase(), invincible); log("invincible " + t + " " + invincible); }
    }
    @Override public void setCooldown(String target, String action, double seconds) {
        cooldownSeconds.put(target.toLowerCase() + ":" + normalize(action), seconds);
    }
    @Override public boolean hasCooldown(String target, String action) {
        return cooldownSeconds.containsKey(target.toLowerCase() + ":" + normalize(action));
    }

    // ---- world & environment ---------------------------------------------------
    @Override public void setWeatherDuration(double seconds) { weatherDuration = seconds; log("weather duration " + seconds); }
    @Override public void setStorm(boolean storm) { this.storm = storm; log("storm " + storm); }
    @Override public void setThunder(boolean thunder) { this.thunder = thunder; log("thunder " + thunder); }
    @Override public void setTimeSpeed(double multiplier) { timeSpeed = multiplier; log("time speed " + multiplier); }
    @Override public void setPlayerLimit(int slots) { playerLimit = slots; log("playerlimit " + slots); }
    @Override public void spawnStructure(String name, String location) {
        structures.add(name + " @ " + location);
        log("structure " + name + " at " + location);
    }

    // ---- entities & mobs --------------------------------------------------------
    @Override public void setMobAi(String mob, boolean enabled) { mobAi.put(normalize(mob), enabled); log("mob ai " + mob + " " + enabled); }
    @Override public void setMobGravity(String mob, boolean gravity) { mobGravity.put(normalize(mob), gravity); log("mob gravity " + mob + " " + gravity); }
    @Override public void setMobFlying(String mob, boolean flying) { mobFlying.put(normalize(mob), flying); log("mob flying " + mob + " " + flying); }
    @Override public void setMobBreeding(String mob, boolean breeding) { mobBreeding.put(normalize(mob), breeding); log("mob breeding " + mob + " " + breeding); }
    @Override public void setMobCustomDrop(String mob, String item) { mobCustomDrops.put(normalize(mob), item); log("mob drop " + mob + " " + item); }
    @Override public void setMobFollow(String mob, String target) { mobFollows.put(normalize(mob), target); log("mob follow " + mob + " " + target); }
    @Override public void setMobTarget(String mob, String target) { mobTargets.put(normalize(mob), target); log("mob target " + mob + " " + target); }
    @Override public void setMobNameVisible(String mob, boolean visible) { mobNameVisible.put(normalize(mob), visible); log("mob name " + mob + " " + visible); }
    @Override public void setMobPersistent(String mob, boolean persistent) { mobPersistent.put(normalize(mob), persistent); log("mob persistent " + mob + " " + persistent); }

    // ---- systems & logic ---------------------------------------------------------
    @Override public void setSidebarTitle(String title) { sidebarTitle = title; log("sidebar title " + title); }
    @Override public void setSidebarLine(int line, String text) {
        if (line < 0) line = 0;
        while (sidebarLines.size() <= line) sidebarLines.add("");
        sidebarLines.set(line, text);
        log("sidebar line " + (line + 1) + " " + text);
    }

    // ---- extended queries ---------------------------------------------------------
    @Override public double distance(String a, String b) {
        Location la = namedPlaces.containsKey(normalize(a)) ? namedPlaces.get(normalize(a)) : locationOf(a);
        Location lb = namedPlaces.containsKey(normalize(b)) ? namedPlaces.get(normalize(b)) : locationOf(b);
        double dx = la.x() - lb.x(), dy = la.y() - lb.y(), dz = la.z() - lb.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    @Override public boolean isInLava(String target) { for (String t : targets(target)) if (player(t).inLava) return true; return false; }
    @Override public boolean isInBed(String target) { for (String t : targets(target)) if (player(t).inBed) return true; return false; }
    @Override public boolean isUnderSky(String target) { for (String t : targets(target)) if (player(t).underSky) return true; return false; }
    @Override public String weapon(String target) { return player(target).weapon; }
    @Override public double armor(String target) { return player(target).armorValue; }
    @Override public double healthPercent(String target) { return player(target).hp / player(target).maxHp * 100.0; }
    @Override public int killCount(String target) { return killCounts.getOrDefault(target.toLowerCase(), 0); }
    @Override public int deathCount(String target) { return deathCounts.getOrDefault(target.toLowerCase(), 0); }
    @Override public int killStreak(String target) { return killStreaks.getOrDefault(target.toLowerCase(), 0); }
    @Override public void addKill(String target) {
        for (String t : targets(target)) {
            killCounts.merge(t.toLowerCase(), 1, Integer::sum);
            killStreaks.merge(t.toLowerCase(), 1, Integer::sum);
            log("kill " + t);
        }
    }
    @Override public void addDeath(String target) {
        for (String t : targets(target)) {
            deathCounts.merge(t.toLowerCase(), 1, Integer::sum);
            killStreaks.put(t.toLowerCase(), 0);
            log("death " + t);
        }
    }
    @Override public void giveEnchanted(String target, String item, String enchant, int level) {
        for (String t : targets(target)) log("enchanted " + t + " " + item + " " + enchant + " " + level);
    }
    @Override public void shootColoredFirework(String target, String color) {
        for (String t : targets(target)) log("firework " + t + " " + color);
    }

    static String normalize(String s) { return s == null ? "" : s.trim().toLowerCase().replaceAll("\\s+", " "); }

    private static double min(double a, double b) { return a < b ? a : b; }
    private static double max(double a, double b) { return a > b ? a : b; }
}
