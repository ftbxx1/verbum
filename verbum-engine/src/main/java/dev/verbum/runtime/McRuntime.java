package dev.verbum.runtime;

import dev.verbum.error.VerbumError;

/**
 * The bridge between Verbum and Minecraft.
 *
 * This interface is implemented twice:
 *   - {@link dev.verbum.runtime.MockMcRuntime}  — an in-memory fake world for
 *     offline tests and the acceptance demo.
 *   - {@code dev.verbum.paper.PaperRuntime}     — the real Paper (Bukkit) plugin
 *     that maps these calls to the actual server.
 *
 * "target" is a player name, or the special values "all" / "everyone".
 */
public interface McRuntime {

    String ALL = "all";

    // ----------------------------------------------------------- regions
    /** Defines a named area between two corners so "player reaches Area X" works. */
    void defineArea(String name, Location cornerA, Location cornerB);

    // ----------------------------------------------------------- messages
    void tell(String target, String message);
    void warn(String target, String message);
    void announce(String message);
    void title(String target, String title, String subtitle);
    void toast(String target, String message);
    void actionbar(String target, String message);

    // ----------------------------------------------------------- inventory
    void give(String target, String item, double count);
    void take(String target, String item, double count);
    void clearItem(String target, String item);
    void drop(String target, String item, double count);

    // ----------------------------------------------------------- item metadata
    /** Rename an item in a player's inventory (rename player's sword to Epic Sword). */
    default void renameItem(String target, String item, String name) { }
    /** Set the lore lines of an item in a player's inventory. */
    default void setLore(String target, String item, String lore) { }
    /** Set the custom model data int of an item in a player's inventory. */
    default void setCustomModelData(String target, String item, int data) { }

    // ----------------------------------------------------------- life & body
    void kill(String target);
    void damage(String target, double amount);
    void heal(String target, double amount);
    void healToFull(String target);
    void ignite(String target, int seconds);
    void freeze(String target, int seconds);
    void explode(String target, double power);
    void feed(String target, double food);
    void setOperator(String target, boolean op);
    void setInvisible(String target, boolean invisible);
    void setGlowing(String target, boolean glowing);
    void setGravity(String target, boolean gravity);
    void resetPlayer(String target);
    void setFly(String target, boolean canFly);
    void setWalkSpeed(String target, double speed);
    void setGamemode(String target, String gamemode);
    void giveXp(String target, double amount);
    void giveLevels(String target, double amount);

    // ----------------------------------------------------------- movement
    void teleportTo(String target, String namedPlace);
    void teleportToCoords(String target, Location loc);
    /** The live location of a player (used by  set block at player ). */
    default Location locationOf(String target) { return Location.at("world", 0, 64, 0); }

    // ----------------------------------------------------------- commands
    /** Ask a player (or "all") to run a command, e.g.  make player execute command /warp. */
    default void executeCommand(String sender, String command) { }

    // ----------------------------------------------------------- world
    void setWeather(String weather);
    void setTime(String time);
    void setBlock(String block, Location at);
    void breakBlock(Location at);
    void playSound(String target, String sound);
    void playParticle(String target, String particle);
    void lightningAt(String target);

    // ----------------------------------------------------------- entities
    void spawn(String mob, Location at, int count);
    void despawn(String mob);
    void setMobHealth(String mob, double health);
    void setMobSpeed(String mob, double speed);
    void setMobHostility(String mob, boolean hostile);
    void enchant(String target, String enchant, String item, int level);
    void unenchant(String target, String enchant);
    void giveEffect(String target, String effect, int seconds, int level);
    default void removeEffect(String target, String effect) { }
    default void setHealth(String target, double amount) { }
    default void setMaxHealth(String target, double amount) { }
    default void setFood(String target, double amount) { }
    default void setLevel(String target, double amount) { }
    default void setInvulnerable(String target, boolean invulnerable) { }
    void strikeLightningAt(String target);

    // ----------------------------------------------------------- world state
    void openDoor(String place);
    void closeDoor(String place);
    void openGate(String place);
    void closeGate(String place);
    void winGame(String target);
    void loseGame(String target);

    // ----------------------------------------------------------- scoreboards
    /** Creates (or clears) a named scoreboard objective; reset() clears scores. */
    default void createScoreboard(String objective, String displayName) { }
    default void setScore(String objective, String player, double value) { }
    default void addScore(String objective, String player, double value) { }
    default void removeScore(String objective, String player, double value) { }
    default double score(String objective, String player) { return 0; }
    default void deleteScoreboard(String objective) { }
    default void setScoreboardDisplay(String objective) { }

    // ----------------------------------------------------------- teams
    default void createTeam(String name) { }
    default void teamAdd(String team, String player) { }
    default void teamRemove(String team, String player) { }
    default void teamColor(String team, String color) { }
    default void teamPrefix(String team, String prefix) { }
    default void teamSuffix(String team, String suffix) { }
    default java.util.List<String> teamMembers(String team) { return java.util.List.of(); }

    // ----------------------------------------------------------- boss bar
    default void createBossBar(String name, String title) { }
    default void bossBarTitle(String name) { }
    default void bossBarProgress(String name, double progress) { }
    default void bossBarColor(String name, String color) { }
    default void bossBarStyle(String name, String style) { }
    default void bossBarVisible(String name, boolean visible) { }
    default void removeBossBar(String name) { }

    // ----------------------------------------------------------- world extras
    default void setWorldBorder(double radius) { }
    default void setDifficulty(String difficulty) { }
    default void setSpawnPoint(Location at) { }
    default void setMobLimit(int limit) { }
    default void setWorldRule(String rule, boolean enabled) { }
    default void setRedstone(String place, boolean powered) { }

    // ----------------------------------------------------------- item & armor
    default void setItemFlag(String target, String item, String flag, boolean on) { }
    default void giveArmor(String target, String armor) { }
    default void setHeldItem(String target, String item) { }
    default void setOffhandItem(String target, String item) { }

    // ----------------------------------------------------------- guis
    default void openMenu(String target, String menuName) { }
    default void openAnvil(String target) { }
    default void openWorkbench(String target) { }
    default void openShop(String target) { }

    // ----------------------------------------------------------- projectiles
    default void shoot(String target, String projectile) { }
    default void throwItem(String target, String item) { }
    default void shootFirework(String target) { }

    // ----------------------------------------------------------- mob extras
    default void setMobAge(String mob, int age) { }
    default void setMobSize(String mob, double size) { }
    default void setMobPitch(String mob, double pitch) { }
    default void tameMob(String mob) { }
    default void nameMob(String mob, String name) { }

    // ----------------------------------------------------------- server extras
    default void sendToServer(String target, String server) { }
    default void setTabList(String target, String name) { }
    default void setClickCommand(String target, String command) { }
    default void setSignText(Location at, String text) { }
    default void setVillagerPrice(String villager, String item, double price) { }
    default void setMute(String target, boolean muted) { }
    default boolean isMuted(String target) { return false; }
    default void setFlag(String name, boolean value) { }
    default boolean getFlag(String name) { return false; }
    default void startGame(String gameName) { }
    default void endGame(String gameName) { }
    default void setRound(int round) { }
    default void nextRound() { }

    // ----------------------------------------------------------- quests
    default void setQuest(String quest, double value) { }
    default double questProgress(String quest) { return 0; }
    default void completeQuest(String quest) { }
    default boolean questDone(String quest) { return false; }

    // ----------------------------------------------------------- server admin
    void ban(String target);
    void kick(String target, String reason);
    void givePermission(String target, String permission);
    void removePermission(String target, String permission);
    void kickPlayer(String target, String reason);

    // ----------------------------------------------------------- queries
    boolean hasItem(String target, String item, double atLeast);
    boolean isHolding(String target, String item);
    double health(String target);
    double maxHealth(String target);
    double food(String target);
    int level(String target);
    boolean isIn(String target, String place);
    boolean isNight();
    boolean isDay();
    boolean isRain();
    boolean isStorm();
    int onlinePlayers();
    boolean playerOnline(String name);
    double coord(String target, char axis);          // 'x', 'y' or 'z'
    String dimension(String target);
    String biome(String target);
    String gamemode(String target);
    boolean isSneaking(String target);
    boolean isSprinting(String target);
    boolean isInVehicle(String target);
    boolean isOnGround(String target);
    boolean isFlying(String target);
    boolean isBurning(String target);
    boolean isPoisoned(String target);
    /** Extra body sensors used by the big condition library. */
    default boolean isSwimming(String target) { return false; }
    default boolean isGliding(String target) { return false; }
    default boolean isInWater(String target) { return false; }
    default boolean isFalling(String target) { return false; }
    default boolean isClimbing(String target) { return false; }
    default boolean isWearing(String target, String armor) { return false; }
    /** "noon", "midnight", "day" or "night". */
    default String timeOfDay() { return isDay() ? "day" : "night"; }
    boolean hasEffect(String target, String effect);
    boolean isOp(String target);
    boolean hasPermission(String target, String permission);
    boolean playerAlive(String target);
    int mobCountNear(String target, String mob, double radius);
    double getScore(String scoreboard);
    boolean isBossHalfHealth(String mob);

    // ----------------------------------------------------------- chat & messages
    default void setJoinMessage(String message) { }
    default void setQuitMessage(String message) { }
    default void setPublicChat(boolean enabled) { }
    default void setPrivateChat(boolean enabled) { }
    default void clearChat(String target) { }
    default void sendHoverMessage(String target, String text, String hover) { }
    default void sendClickMessage(String target, String text, String command) { }

    // ----------------------------------------------------------- player meta & state
    default void setSneaking(String target, boolean sneaking) { }
    default void setSprinting(String target, boolean sprinting) { }
    default void setHideFrom(String target, String other) { }
    default void setShowTo(String target, String other) { }
    default void setArmor(String target, double points) { }
    default void setAbsorption(String target, double hearts) { }
    default void cancelFallDamage(String target, boolean cancel) { }
    default void setInvincible(String target, boolean invincible) { }
    default void setCooldown(String target, String action, double seconds) { }
    default boolean hasCooldown(String target, String action) { return false; }

    // ----------------------------------------------------------- inventory & slots
    /** Put an item into a specific inventory slot (1..36). */
    default void setSlot(String target, int slot, String item) { }
    /** Swap the main and off hand items. */
    default void swapHands(String target) { }
    /** Empty a player's inventory. */
    default void clearInventory(String target) { }
    /** Change the stack size of an item the player holds. */
    default void setItemAmount(String target, String item, int amount) { }
    /** Toggle the "unbreakable" tag on a held item. */
    default void setItemUnbreakable(String target, String item, boolean unbreakable) { }
    /** Set the skull owner of a skull item the player holds. */
    default void setSkullOwner(String target, String item, String owner) { }

    // ----------------------------------------------------------- player tuning
    /** Paper fly speed (default ~0.05). */
    default void setFlySpeed(String target, double speed) { }
    /** Attack speed attribute in hits per second. */
    default void setAttackSpeed(String target, double speed) { }
    default void setSaturation(String target, double value) { }
    /** Remaining air/breath ticks. */
    default void setAir(String target, int ticks) { }
    /** Actually toggle flight state (not just allow-flight). */
    default void setFlying(String target, boolean flying) { }
    /** Toggle elytra gliding. */
    default void setGliding(String target, boolean gliding) { }
    /** Put armor in a specific slot: helmet / chestplate / leggings / boots / offhand. */
    default void setArmorSlot(String target, String armor, String piece) { }
    default void setDisplayName(String target, String name) { }
    /** The name shown in tab and above the head (name tag). */
    default void setPlayerListName(String target, String name) { }
    /** Glow outline color for a player. */
    default void setGlowColor(String target, String color) { }
    default void setTabListHeaderFooter(String target, String header, String footer) { }
    /** Set the player's respawn (bed spawn) point to their current position. */
    default void setRespawnPoint(String target) { }
    /** Launch a player up with a velocity power (0..10). */
    default void launch(String target, double power) { }
    default void removeAllEffects(String target) { }

    // ----------------------------------------------------------- world effects
    /** Drop XP orbs at a location. */
    default void dropExperience(Location at, int amount) { }
    /** Drop an item entity at a location. */
    default void dropItemAt(Location at, String item, int count) { }
    /** Lightning at a coordinate. */
    default void lightningAt(Location at) { }
    default void playSoundAt(Location at, String sound) { }
    default void stopAllSounds(String target) { }
    /** Play a music disc (item name) for a player. */
    default void playMusicDisc(String target, String disc) { }
    /** Fill a cuboid between two corners with a block. */
    default void fillRegion(Location a, Location b, String block) { }
    /** Give one random item (from a small curated pool). */
    default void giveRandomItem(String target) { }

    // ----------------------------------------------------------- world & environment
    default void setWeatherDuration(double seconds) { }
    default void setStorm(boolean storm) { }
    default void setThunder(boolean thunder) { }
    default void setTimeSpeed(double multiplier) { }
    default void setPlayerLimit(int slots) { }
    default void spawnStructure(String name, String location) { }

    // ----------------------------------------------------------- entities & mobs
    default void setMobAi(String mob, boolean enabled) { }
    default void setMobGravity(String mob, boolean gravity) { }
    default void setMobFlying(String mob, boolean flying) { }
    default void setMobBreeding(String mob, boolean breeding) { }
    default void setMobCustomDrop(String mob, String item) { }
    default void setMobFollow(String mob, String target) { }
    default void setMobTarget(String mob, String target) { }
    default void setMobNameVisible(String mob, boolean visible) { }
    default void setMobPersistent(String mob, boolean persistent) { }

    // ----------------------------------------------------------- systems & logic
    default void setSidebarTitle(String title) { }
    default void setSidebarLine(int line, String text) { }

    // ----------------------------------------------------------- extended queries
    default double distance(String a, String b) { return 0; }
    default boolean isInLava(String target) { return false; }
    default boolean isInBed(String target) { return false; }
    default boolean isUnderSky(String target) { return false; }
    default String weapon(String target) { return ""; }
    default double armor(String target) { return 0; }
    default double healthPercent(String target) { return 100; }
    default int killCount(String target) { return 0; }
    default int deathCount(String target) { return 0; }
    default int killStreak(String target) { return 0; }
    default void addKill(String target) { }
    default void addDeath(String target) { }
    default void giveEnchanted(String target, String item, String enchant, int level) { }
    default void shootColoredFirework(String target, String color) { }

    // ----------------------------------------------------------- live: player vitals
    /** Total experience points (0..), not levels. */
    default double experience(String target) { return 0; }
    /** Experience points needed to reach the next level. */
    default int xpToNextLevel(String target) { return 0; }
    /** Progress to the next level, 0..1. */
    default double xpPercent(String target) { return 0; }
    default double saturation(String target) { return 0; }
    default double absorption(String target) { return 0; }
    /** Remaining air / breath ticks. */
    default int air(String target) { return 0; }
    default int maxAir(String target) { return 0; }
    default int fireTicks(String target) { return 0; }
    default int freezeTicks(String target) { return 0; }
    /** Paper walk/fly speed on the player scale (default ~0.1 / ~0.05). */
    default double walkSpeed(String target) { return 0.1; }
    default double flySpeed(String target) { return 0.05; }
    /** Network latency in milliseconds. */
    default int ping(String target) { return 0; }
    default String worldName(String target) { return "world"; }
    default double yaw(String target) { return 0; }
    default double pitch(String target) { return 0; }
    /** Compass direction: north / south / east / west. */
    default String facing(String target) { return "north"; }
    default boolean isGlowing(String target) { return false; }
    default boolean isInvisible(String target) { return false; }
    /** The item in the main hand (material name). */
    default String holdingItem(String target) { return ""; }
    /** Main-hand hotbar slot, 0..8. */
    default int heldSlot(String target) { return 0; }
    /** Number of empty inventory slots. */
    default int emptySlots(String target) { return 0; }
    /** Name of the scoreboard team the player is on ("" when none). */
    default String teamOf(String target) { return ""; }

    // ----------------------------------------------------------- live: world & server
    default String difficulty() { return "normal"; }
    /** World time in ticks. */
    default long worldTime() { return 6000; }
    /** Whole in-game days elapsed. */
    default long dayCount() { return 0; }
    default long worldSeed() { return 0; }
    /** World border size in blocks (0 when unset). */
    default double worldBorder() { return 0; }
    default Location spawnPoint() { return Location.at("world", 0, 64, 0); }
    /** Server player limit. */
    default int maxPlayers() { return 20; }
    /** Ticks per second, usually 20. */
    default double tps() { return 20; }

    /** Simple persistence hook (SQLite/YAML). Concrete runtimes implement as they can. */
    default Object loadPersistent(String key) { return null; }
    default void savePersistent(String key, Object value) { }

    /** Two corners from comma-free text, e.g. corners Spawn and Corner2. */
    static Location[] parseCorners(String a, String b) {
        throw new VerbumError("I could not understand those location words.");
    }
}
