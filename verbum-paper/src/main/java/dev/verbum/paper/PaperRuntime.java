package dev.verbum.paper;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.interp.Trigger;
import dev.verbum.runtime.Location;
import dev.verbum.runtime.McRuntime;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Minecraft side of the Verbum bridge. Coordinates with specialized helpers
 * to execute actions and query world state. Delegates to focused helper classes
 * rather than implementing all logic directly.
 */
public final class PaperRuntime implements McRuntime {

    private final VerbumPaperPlugin plugin;
    private final PlayerTargetResolver playerResolver;
    private final AreaRegistry areaRegistry;
    private final PlayerActions playerActions;
    private final InventoryActions inventoryActions;
    private final WorldActions worldActions;
    private final EntityActions entityActions;
    private final QueryActions queryActions;

    public PaperRuntime(VerbumPaperPlugin plugin) {
        this.plugin = plugin;
        this.playerResolver = new PlayerTargetResolver();
        this.areaRegistry = new AreaRegistry();
        this.playerActions = new PlayerActions(playerResolver);
        this.inventoryActions = new InventoryActions(playerResolver);
        this.worldActions = new WorldActions();
        this.entityActions = new EntityActions(playerResolver);
        this.queryActions = new QueryActions(playerResolver, areaRegistry);
    }

    ScriptEngine engine() { return plugin.engine(); }

    /** Called on each move: fires enter / reach / leave triggers when crossing areas. */
    public void checkRegions(Player p) {
        areaRegistry.checkRegions(p, this::triggerEvent);
    }

    private void triggerEvent(String event, String player, String areaName) {
        engine().trigger(new Trigger(event, player).with("p", areaName));
    }

    // ---- messages --------------------------------------------------------

    @Override public void tell(String target, String m) { playerActions.tell(target, m); }
    @Override public void warn(String target, String m) { playerActions.warn(target, m); }
    @Override public void announce(String m) { playerActions.announce(m); }
    @Override public void title(String target, String title, String sub) { playerActions.title(target, title, sub); }
    @Override public void toast(String target, String m) { playerActions.toast(target, m); }
    @Override public void actionbar(String target, String m) { playerActions.actionbar(target, m); }

    // ---- inventory ---------------------------------------------------------

    @Override public void give(String target, String item, double count) { inventoryActions.give(target, item, count); }
    @Override public void take(String target, String item, double count) { inventoryActions.take(target, item, count); }
    @Override public void clearItem(String target, String item) { inventoryActions.clearItem(target, item); }
    @Override public void drop(String target, String item, double count) { inventoryActions.drop(target, item, count); }

    // ---- item metadata ---------------------------------------------------------

    @Override public void renameItem(String target, String item, String name) { inventoryActions.renameItem(target, item, name); }
    @Override public void setLore(String target, String item, String lore) { inventoryActions.setLore(target, item, lore); }
    @Override public void setCustomModelData(String target, String item, int data) { inventoryActions.setCustomModelData(target, item, data); }

    // ---- life ---------------------------------------------------------------

    @Override public void kill(String target) { playerActions.kill(target); }
    @Override public void damage(String target, double amount) { playerActions.damage(target, amount); }
    @Override public void heal(String target, double amount) { playerActions.heal(target, amount); }
    @Override public void healToFull(String target) { playerActions.healToFull(target); }
    @Override public void ignite(String target, int seconds) { playerActions.ignite(target, seconds); }
    @Override public void freeze(String target, int seconds) { playerActions.freeze(target, seconds); }
    @Override public void setFly(String target, boolean canFly) { playerActions.setFly(target, canFly); }
    @Override public void explode(String target, double power) { playerActions.explode(target, power); }
    @Override public void feed(String target, double food) { playerActions.feed(target, food); }
    @Override public void setOperator(String target, boolean op) { playerActions.setOperator(target, op); }
    @Override public void setInvisible(String target, boolean invisible) { playerActions.setInvisible(target, invisible); }
    @Override public void setGlowing(String target, boolean glowing) { playerActions.setGlowing(target, glowing); }
    @Override public void setGravity(String target, boolean gravity) { playerActions.setGravity(target, gravity); }
    @Override public void resetPlayer(String target) { playerActions.resetPlayer(target); }
    @Override public void setWalkSpeed(String target, double speed) { playerActions.setWalkSpeed(target, speed); }
    @Override public void setGamemode(String target, String gm) { playerActions.setGamemode(target, gm); }
    @Override public void giveXp(String target, double amount) { playerActions.giveXp(target, amount); }
    @Override public void giveLevels(String target, double amount) { playerActions.giveLevels(target, amount); }

    // ---- movement --------------------------------------------------------------

    @Override public void teleportTo(String target, String namedPlace) { playerActions.teleportTo(target, namedPlace, areaRegistry); }
    @Override public void teleportToCoords(String target, Location loc) { playerActions.teleportToCoords(target, loc); }
    @Override public Location locationOf(String target) { return playerActions.locationOf(target); }
    @Override public void executeCommand(String sender, String command) { playerActions.executeCommand(sender, command); }

    // ---- world -----------------------------------------------------------------

    @Override public void setWeather(String weather) { worldActions.setWeather(weather); }
    @Override public void setTime(String time) { worldActions.setTime(time); }
    @Override public void setBlock(String block, Location at) { worldActions.setBlock(block, at); }
    @Override public void breakBlock(Location at) { worldActions.breakBlock(at); }
    @Override public void playSound(String target, String sound) { worldActions.playSound(target, sound, playerResolver); }
    @Override public void playParticle(String target, String particle) { worldActions.playParticle(target, particle, playerResolver); }
    @Override public void lightningAt(String target) { worldActions.lightningAt(target, playerResolver); }

    // ---- entities ----------------------------------------------------------------

    @Override public void spawn(String mob, Location at, int count) { entityActions.spawn(mob, at, count); }
    @Override public void despawn(String mob) { entityActions.despawn(mob); }
    @Override public void setMobHealth(String mob, double health) { entityActions.setMobHealth(mob, health); }
    @Override public void setMobSpeed(String mob, double speed) { entityActions.setMobSpeed(mob, speed); }
    @Override public void setMobHostility(String mob, boolean hostile) { entityActions.setMobHostility(mob, hostile); }
    @Override public void enchant(String target, String enchant, String item, int level) { entityActions.enchant(target, enchant, item, level, playerResolver); }
    @Override public void unenchant(String target, String enchant) { entityActions.unenchant(target, enchant); }
    @Override public void giveEffect(String target, String effect, int seconds, int level) { entityActions.giveEffect(target, effect, seconds, level, playerResolver); }
    @Override public void strikeLightningAt(String target) { worldActions.lightningAt(target, playerResolver); }

    // ---- world state ----------------------------------------------------------------

    @Override public void openDoor(String place) { worldActions.toggleBlock(place, true); }
    @Override public void closeDoor(String place) { worldActions.toggleBlock(place, false); }
    @Override public void openGate(String place) { worldActions.toggleBlock(place, true); }
    @Override public void closeGate(String place) { worldActions.toggleBlock(place, false); }
    @Override public void winGame(String target) { playerActions.winGame(target); }
    @Override public void loseGame(String target) { playerActions.loseGame(target); }

    // ---- admin ------------------------------------------------------------------------

    @Override public void ban(String target) { playerActions.ban(target); }
    @Override public void kick(String target, String reason) { playerActions.kick(target, reason); }
    @Override public void kickPlayer(String target, String reason) { playerActions.kick(target, reason); }
    @Override public void givePermission(String target, String permission) { playerActions.givePermission(target, permission, plugin); }
    @Override public void removePermission(String target, String permission) { playerActions.removePermission(target, permission, plugin); }

    // ---- queries ----------------------------------------------------------------

    @Override public boolean hasItem(String target, String item, double atLeast) { return queryActions.hasItem(target, item, atLeast); }
    @Override public boolean isHolding(String target, String item) { return queryActions.isHolding(target, item); }
    @Override public double health(String target) { return queryActions.health(target); }
    @Override public double maxHealth(String target) { return queryActions.maxHealth(target); }
    @Override public double food(String target) { return queryActions.food(target); }
    @Override public int level(String target) { return queryActions.level(target); }
    @Override public boolean isIn(String target, String place) { return queryActions.isIn(target, place, areaRegistry); }
    @Override public boolean isNight() { return queryActions.isNight(); }
    @Override public boolean isDay() { return !isNight(); }
    @Override public boolean isRain() { return queryActions.isRain(); }
    @Override public boolean isStorm() { return queryActions.isStorm(); }
    @Override public int onlinePlayers() { return queryActions.onlinePlayers(); }
    @Override public boolean playerOnline(String name) { return queryActions.playerOnline(name); }
    @Override public double coord(String target, char axis) { return queryActions.coord(target, axis); }
    @Override public String dimension(String target) { return queryActions.dimension(target); }
    @Override public String biome(String target) { return queryActions.biome(target); }
    @Override public String gamemode(String target) { return queryActions.gamemode(target); }
    @Override public boolean isSneaking(String target) { return queryActions.isSneaking(target); }
    @Override public boolean isSprinting(String target) { return queryActions.isSprinting(target); }
    @Override public boolean isInVehicle(String target) { return queryActions.isInVehicle(target); }
    @Override public boolean isOnGround(String target) { return queryActions.isOnGround(target); }
    @Override public boolean isFlying(String target) { return queryActions.isFlying(target); }
    @Override public boolean isBurning(String target) { return queryActions.isBurning(target); }
    @Override public boolean isPoisoned(String target) { return queryActions.isPoisoned(target); }
    @Override public boolean hasEffect(String target, String effect) { return queryActions.hasEffect(target, effect); }
    @Override public boolean isOp(String target) { return queryActions.isOp(target); }
    @Override public boolean hasPermission(String target, String permission) { return queryActions.hasPermission(target, permission); }
    @Override public boolean playerAlive(String target) { return queryActions.playerAlive(target); }
    @Override public int mobCountNear(String target, String mob, double radius) { return queryActions.mobCountNear(target, mob, radius); }
    @Override public double getScore(String scoreboard) { return queryActions.getScore(scoreboard); }
    @Override public boolean isBossHalfHealth(String mob) { return queryActions.isBossHalfHealth(mob); }

    // ---- live vitals: player -----------------------------------------------------

    @Override public double experience(String target) { return queryActions.experience(target); }
    @Override public int xpToNextLevel(String target) { return queryActions.xpToNextLevel(target); }
    @Override public double xpPercent(String target) { return queryActions.xpPercent(target); }
    @Override public double saturation(String target) { return queryActions.saturation(target); }
    @Override public double absorption(String target) { return queryActions.absorption(target); }
    @Override public int air(String target) { return queryActions.air(target); }
    @Override public int maxAir(String target) { return queryActions.maxAir(target); }
    @Override public int fireTicks(String target) { return queryActions.fireTicks(target); }
    @Override public int freezeTicks(String target) { return queryActions.freezeTicks(target); }
    @Override public double walkSpeed(String target) { return queryActions.walkSpeed(target); }
    @Override public double flySpeed(String target) { return queryActions.flySpeed(target); }
    @Override public int ping(String target) { return queryActions.ping(target); }
    @Override public String worldName(String target) { return queryActions.worldName(target); }
    @Override public double yaw(String target) { return queryActions.yaw(target); }
    @Override public double pitch(String target) { return queryActions.pitch(target); }
    @Override public String facing(String target) { return queryActions.facing(target); }
    @Override public boolean isGlowing(String target) { return queryActions.isGlowing(target); }
    @Override public boolean isInvisible(String target) { return queryActions.isInvisible(target); }
    @Override public String holdingItem(String target) { return queryActions.holdingItem(target); }
    @Override public int heldSlot(String target) { return queryActions.heldSlot(target); }
    @Override public int emptySlots(String target) { return queryActions.emptySlots(target); }
    @Override public String teamOf(String target) { return queryActions.teamOf(target); }

    // ---- live vitals: world & server -----------------------------------------------------

    @Override public String difficulty() { return queryActions.difficulty(); }
    @Override public long worldTime() { return queryActions.worldTime(); }
    @Override public long dayCount() { return queryActions.dayCount(); }
    @Override public long worldSeed() { return queryActions.worldSeed(); }
    @Override public double worldBorder() { return queryActions.worldBorder(); }
    @Override public Location spawnPoint() { return queryActions.spawnPoint(); }
    @Override public int maxPlayers() { return queryActions.maxPlayers(); }
    @Override public double tps() { return queryActions.tps(); }

    // ---- inventory & slots ------------------------------------------------------------

    @Override public void setSlot(String target, int slot, String item) { inventoryActions.setSlot(target, slot, item); }
    @Override public void swapHands(String target) { inventoryActions.swapHands(target); }
    @Override public void clearInventory(String target) { inventoryActions.clearInventory(target); }
    @Override public void setItemAmount(String target, String item, int amount) { inventoryActions.setItemAmount(target, item, amount); }
    @Override public void setItemUnbreakable(String target, String item, boolean unbreakable) { inventoryActions.setItemUnbreakable(target, item, unbreakable); }
    @Override public void setSkullOwner(String target, String item, String owner) { inventoryActions.setSkullOwner(target, item, owner); }

    // ---- player tuning ------------------------------------------------------------

    @Override public void setFlySpeed(String target, double speed) { playerActions.setFlySpeed(target, speed); }
    @Override public void setAttackSpeed(String target, double speed) { playerActions.setAttackSpeed(target, speed); }
    @Override public void setSaturation(String target, double value) { playerActions.setSaturation(target, value); }
    @Override public void setAir(String target, int ticks) { playerActions.setAir(target, ticks); }
    @Override public void setFlying(String target, boolean flying) { playerActions.setFlying(target, flying); }
    @Override public void setGliding(String target, boolean gliding) { playerActions.setGliding(target, gliding); }
    @Override public void setArmorSlot(String target, String armor, String piece) { playerActions.setArmorSlot(target, armor, piece); }
    @Override public void setDisplayName(String target, String name) { playerActions.setDisplayName(target, name); }
    @Override public void setPlayerListName(String target, String name) { playerActions.setPlayerListName(target, name); }
    @Override public void setGlowColor(String target, String color) { playerActions.setGlowColor(target, color); }
    @Override public void setRespawnPoint(String target) { playerActions.setRespawnPoint(target); }
    @Override public void launch(String target, double power) { playerActions.launch(target, power); }
    @Override public void removeAllEffects(String target) { playerActions.removeAllEffects(target); }

    // ---- world effects ------------------------------------------------------------

    @Override public void dropExperience(Location at, int amount) { worldActions.dropExperience(at, amount); }
    @Override public void dropItemAt(Location at, String item, int count) { worldActions.dropItemAt(at, item, count); }
    @Override public void lightningAt(Location at) { worldActions.lightningAt(at); }
    @Override public void playSoundAt(Location at, String sound) { worldActions.playSoundAt(at, sound); }
    @Override public void stopAllSounds(String target) { worldActions.stopAllSounds(target, playerResolver); }
    @Override public void playMusicDisc(String target, String disc) { worldActions.playMusicDisc(target, disc, playerResolver); }
    @Override public void fillRegion(Location a, Location b, String block) { worldActions.fillRegion(a, b, block); }
    @Override public void giveRandomItem(String target) { inventoryActions.giveRandomItem(target); }

    // ---- depth batch: jailing / homes / riding / repair / holograms / whitelist -----

    @Override public void jail(String target) { playerActions.jail(target); }
    @Override public void unjail(String target) { playerActions.unjail(target); }
    @Override public boolean isJailed(String target) { return playerActions.isJailed(target); }
    @Override public void setHome(String target) { playerActions.setHome(target); }
    @Override public void teleportHome(String target) { playerActions.teleportHome(target); }
    @Override public boolean hasHome(String target) { return playerActions.hasHome(target); }
    @Override public boolean isRiding(String target) { return queryActions.isRiding(target); }
    @Override public void mount(String target) { playerActions.mount(target); }
    @Override public void dismount(String target) { playerActions.dismount(target); }
    @Override public void repairItem(String target, boolean all) { inventoryActions.repairItem(target, all); }
    @Override public void spawnHologram(String target, String name, String text) { playerActions.spawnHologram(target, name, text); }
    @Override public void removeHologram(String target, String name) { playerActions.removeHologram(target, name); }
    @Override public boolean isWhitelisted(String target) { return queryActions.isWhitelisted(target); }
    @Override public void setWhitelisted(String target, boolean on) { playerActions.setWhitelisted(target, on); }

    // ---- area registry ---------------------------------------------------------------

    @Override public void defineArea(String name, Location ca, Location cb) {
        areaRegistry.defineArea(name, ca, cb);
    }

    @Override public java.util.List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }
}
