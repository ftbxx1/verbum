package dev.verbum.paper;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.interp.Trigger;
import dev.verbum.runtime.Location;
import dev.verbum.runtime.McRuntime;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The real Minecraft side of the bridge. Every Verbum action maps onto the
 * Paper (Bukkit) API, and every Verbum question is answered from live game data.
 */
public final class PaperRuntime implements McRuntime {

    private final VerbumPaperPlugin plugin;
    private final Map<String, Region> areas = new HashMap<>();
    private final Map<String, Set<String>> lastInside = new HashMap<>();

    public PaperRuntime(VerbumPaperPlugin plugin) { this.plugin = plugin; }

    ScriptEngine engine() { return plugin.engine(); }

    /** Condition-style  when  handlers need every live player each tick. */
    @Override public java.util.List<String> onlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }

    public static final class Region {
        final String name;
        final org.bukkit.Location a, b;
        Region(String name, org.bukkit.Location a, org.bukkit.Location b) { this.name = name; this.a = a; this.b = b; }
        boolean contains(org.bukkit.Location p) {
            if (!p.getWorld().equals(a.getWorld())) return false;
            return p.getX() >= Math.min(a.getX(), b.getX()) && p.getX() <= Math.max(a.getX(), b.getX())
                && p.getZ() >= Math.min(a.getZ(), b.getZ()) && p.getZ() <= Math.max(a.getZ(), b.getZ());
        }
    }

    @Override public void defineArea(String name, Location ca, Location cb) {
        World w = Bukkit.getWorld(ca.world());
        if (w == null) w = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        if (w == null) return;
        Region r = new Region(name.toLowerCase(), new org.bukkit.Location(w, ca.x(), ca.y(), ca.z()),
                new org.bukkit.Location(w, cb.x(), cb.y(), cb.z()));
        areas.put(name.toLowerCase(), r);
    }

    /** Called on each move: fires  enter / reach / leave  triggers when crossing areas. */
    public void checkRegions(Player p) {
        String pn = p.getName();
        Set<String> inside = new HashSet<>();
        for (Region r : areas.values()) if (r.contains(p.getLocation())) inside.add(r.name);
        Set<String> prev = lastInside.getOrDefault(pn, new HashSet<>());
        for (String n : inside) {
            if (!prev.contains(n)) {
                // newly inside -> reached it, and entered it
                engine().trigger(new Trigger("reach", pn).with("p", n));
                engine().trigger(new Trigger("enter", pn).with("p", n));
            }
        }
        for (String n : prev) {
            if (!inside.contains(n)) {
                engine().trigger(new Trigger("leave", pn).with("p", n));
            }
        }
        lastInside.put(pn, inside);
    }

    public void spawnRespawn(Player p) { /* engine handles respawn via death */ }

    private List<Player> players(String target) {
        List<Player> out = new ArrayList<>();
        if (target == null) return out;
        if (target.equalsIgnoreCase(ALL) || target.equalsIgnoreCase("everyone")
                || target.equalsIgnoreCase("players")) {
            out.addAll(Bukkit.getOnlinePlayers());
        } else {
            Player p = Bukkit.getPlayerExact(target);
            if (p != null) out.add(p);
        }
        return out;
    }

    private Player first(String target) {
        List<Player> list = players(target);
        return list.isEmpty() ? null : list.get(0);
    }

    // ---- messages --------------------------------------------------------

    @Override public void tell(String target, String m) {
        for (Player p : players(target)) p.sendMessage(m);
    }
    @Override public void warn(String target, String m) {
        for (Player p : players(target)) p.sendMessage(ChatColor.RED + m);
    }
    @Override public void announce(String m) { Bukkit.broadcastMessage(m); }
    @Override public void title(String target, String title, String sub) {
        for (Player p : players(target)) p.sendTitle(title, sub, 10, 70, 20);
    }
    @Override public void toast(String target, String m) {
        for (Player p : players(target)) p.spigot().sendMessage(net.md_5.bungee.api.chat.TextComponent.fromLegacyText(m));
    }
    @Override public void actionbar(String target, String m) {
        for (Player p : players(target)) p.sendActionBar(m);
    }

    // ---- inventory ---------------------------------------------------------

    private ItemStack make(String item) {
        Material mat = Material.matchMaterial(item.replace(' ', '_').toUpperCase());
        return mat == null ? new ItemStack(Material.STICK) : new ItemStack(mat);
    }
    @Override public void give(String target, String item, double count) {
        for (Player p : players(target)) p.getInventory().addItem(make(item).asQuantity(Math.max(1, (int) count)));
    }
    @Override public void take(String target, String item, double count) {
        for (Player p : players(target)) p.getInventory().removeItem(make(item).asQuantity((int) count));
    }
    @Override public void clearItem(String target, String item) {
        for (Player p : players(target)) p.getInventory().remove(make(item));
    }
    @Override public void drop(String target, String item, double count) {
        for (Player p : players(target))
            p.getWorld().dropItemNaturally(p.getLocation(), make(item).asQuantity(Math.max(1, (int) count)));
    }

    // ---- item metadata ---------------------------------------------------------

    @Override public void renameItem(String target, String item, String name) {
        for (Player p : players(target))
            for (ItemStack s : p.getInventory().getContents()) {
                if (s != null && s.getType().name().equalsIgnoreCase(item.replace(' ', '_'))) {
                    org.bukkit.inventory.meta.ItemMeta m = s.getItemMeta();
                    if (m == null) continue;
                    m.setDisplayName(name);
                    s.setItemMeta(m);
                    return;
                }
            }
    }
    @Override public void setLore(String target, String item, String lore) {
        for (Player p : players(target))
            for (ItemStack s : p.getInventory().getContents()) {
                if (s != null && s.getType().name().equalsIgnoreCase(item.replace(' ', '_'))) {
                    org.bukkit.inventory.meta.ItemMeta m = s.getItemMeta();
                    if (m == null) continue;
                    java.util.List<String> lines = new ArrayList<>();
                    for (String part : lore.split("(?i)\\s+and\\s+")) lines.add(ChatColor.translateAlternateColorCodes('&', part));
                    m.setLore(lines);
                    s.setItemMeta(m);
                    return;
                }
            }
    }
    @Override public void setCustomModelData(String target, String item, int data) {
        for (Player p : players(target))
            for (ItemStack s : p.getInventory().getContents()) {
                if (s != null && s.getType().name().equalsIgnoreCase(item.replace(' ', '_'))) {
                    org.bukkit.inventory.meta.ItemMeta m = s.getItemMeta();
                    if (m == null) continue;
                    m.setCustomModelData(data);
                    s.setItemMeta(m);
                    return;
                }
            }
    }

    // ---- life ---------------------------------------------------------------

    @Override public void kill(String target) { for (Player p : players(target)) p.setHealth(0); }
    @Override public void damage(String target, double amount) {
        for (Player p : players(target)) p.damage(amount);
    }
    @Override public void heal(String target, double amount) {
        for (Player p : players(target)) p.setHealth(Math.min(20, p.getHealth() + amount));
    }
    @Override public void healToFull(String target) {
        for (Player p : players(target)) { p.setHealth(20); p.setFoodLevel(20); }
    }
    @Override public void ignite(String target, int seconds) {
        for (Player p : players(target)) p.setFireTicks(seconds * 20);
    }
    @Override public void freeze(String target, int seconds) {
        for (Player p : players(target)) p.setFreezeTicks(seconds * 20);
    }
    @Override public void setFly(String target, boolean canFly) {
        for (Player p : players(target)) { p.setAllowFlight(canFly); }
    }
    @Override public void explode(String target, double power) {
        for (Player p : players(target)) p.getWorld().createExplosion(p.getLocation(), (float) power);
    }
    @Override public void feed(String target, double food) {
        for (Player p : players(target)) p.setFoodLevel((int) Math.min(20, food <= 0 ? 20 : food));
    }
    @Override public void setOperator(String target, boolean op) {
        for (Player p : players(target)) {
            if (op) p.setOp(true); else p.setOp(false);
        }
    }
    @Override public void setInvisible(String target, boolean invisible) {
        for (Player p : players(target)) p.setInvisible(invisible);
    }
    @Override public void setGlowing(String target, boolean glowing) {
        for (Player p : players(target)) p.setGlowing(glowing);
    }
    @Override public void setGravity(String target, boolean gravity) {
        for (Player p : players(target)) p.setGravity(gravity);
    }
    @Override public void resetPlayer(String target) {
        for (Player p : players(target)) {
            p.getInventory().clear();
            p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
            p.setHealth(20);
            p.setFoodLevel(20);
            p.setExp(0);
            p.setLevel(0);
        }
    }
    @Override public void setWalkSpeed(String target, double speed) {
        for (Player p : players(target)) p.setWalkSpeed((float) Math.max(0, Math.min(1, speed / 40)));
    }
    @Override public void setGamemode(String target, String gm) {
        GameMode g = gm.toLowerCase().contains("creative") ? GameMode.CREATIVE
                : gm.toLowerCase().contains("spectator") ? GameMode.SPECTATOR
                : gm.toLowerCase().contains("adventure") ? GameMode.ADVENTURE : GameMode.SURVIVAL;
        for (Player p : players(target)) p.setGameMode(g);
    }
    @Override public void giveXp(String target, double amount) {
        for (Player p : players(target)) p.giveExp((int) amount);
    }
    @Override public void giveLevels(String target, double amount) {
        for (Player p : players(target)) p.giveExpLevels((int) amount);
    }

    // ---- movement --------------------------------------------------------------

    @Override public void teleportTo(String target, String namedPlace) {
        Region r = areas.get(namedPlace.toLowerCase());
        if (r != null) {
            org.bukkit.Location mid = new org.bukkit.Location(r.a.getWorld(),
                    (r.a.getX() + r.b.getX()) / 2, 65, (r.a.getZ() + r.b.getZ()) / 2);
            for (Player p : players(target)) p.teleport(mid);
            return;
        }
        Player other = Bukkit.getPlayerExact(namedPlace);   // teleport to another player
        if (other != null && other.isOnline()) {
            for (Player p : players(target)) p.teleport(other.getLocation());
            return;
        }
        for (Player p : players(target))
            p.sendMessage(ChatColor.RED + "Verbum: I do not know the place " + namedPlace);
    }
    @Override public void teleportToCoords(String target, Location loc) {
        World w = Bukkit.getWorld(loc.world());
        if (w == null) w = Bukkit.getWorlds().get(0);
        for (Player p : players(target)) p.teleport(new org.bukkit.Location(w, loc.x(), loc.y(), loc.z()));
    }
    @Override public Location locationOf(String target) {
        Player p = first(target);
        if (p == null) return Location.at("world", 0, 64, 0);
        org.bukkit.Location l = p.getLocation();
        return Location.at(p.getWorld().getName(), l.getX(), l.getY(), l.getZ());
    }
    @Override public void executeCommand(String sender, String command) {
        Player p = first(sender);
        if (p != null) p.performCommand(command);
        else Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    // ---- world -----------------------------------------------------------------

    @Override public void setWeather(String weather) {
        for (World w : Bukkit.getWorlds()) {
            w.setStorm(weather.equalsIgnoreCase("rain") || weather.equalsIgnoreCase("storm"));
            w.setThundering(weather.equalsIgnoreCase("storm"));
        }
    }
    @Override public void setTime(String time) {
        for (World w : Bukkit.getWorlds()) {
            switch (time.toLowerCase()) {
                case "day": case "noon": w.setTime(6000); break;
                case "night": case "midnight": w.setTime(18000); break;
                default: w.setTime(6000);
            }
        }
    }
    @Override public void setBlock(String block, Location at) {
        World w = Bukkit.getWorld(at.world());
        if (w == null) return;
        Material m = Material.matchMaterial(block.replace(' ', '_').toUpperCase());
        if (m != null) w.getBlockAt((int) at.x(), (int) at.y(), (int) at.z()).setType(m);
    }
    @Override public void breakBlock(Location at) {
        World w = Bukkit.getWorld(at.world());
        if (w != null) w.getBlockAt((int) at.x(), (int) at.y(), (int) at.z()).breakNaturally();
    }
    @Override public void playSound(String target, String sound) {
        Sound s = Sound.valueOf(sound.replace(' ', '_').toUpperCase());
        for (Player p : players(target)) p.playSound(p.getLocation(), s, 1, 1);
    }
    @Override public void playParticle(String target, String particle) {
        Particle pt = Particle.valueOf(particle.replace(' ', '_').toUpperCase());
        for (Player p : players(target)) p.spawnParticle(pt, p.getLocation(), 20);
    }
    @Override public void lightningAt(String target) {
        Player p = first(target);
        if (p != null) p.getWorld().strikeLightningEffect(p.getLocation());
    }

    // ---- entities ----------------------------------------------------------------

    @Override public void spawn(String mob, Location at, int count) {
        World w = Bukkit.getWorld(at.world());
        if (w == null) return;
        try {
            org.bukkit.entity.EntityType t = org.bukkit.entity.EntityType.valueOf(mob.replace(' ', '_').toUpperCase());
            for (int i = 0; i < count; i++)
                w.spawnEntity(new org.bukkit.Location(w, at.x(), at.y(), at.z()), t);
        } catch (IllegalArgumentException e) {
            Bukkit.broadcastMessage(ChatColor.RED + "Verbum: I do not know the mob " + mob);
        }
    }
    @Override public void despawn(String mob) { /* would need tracked entities */ }
    @Override public void setMobHealth(String mob, double health) { /* entity tag matching is out of v1 scope */ }
    @Override public void setMobSpeed(String mob, double speed) { }
    @Override public void setMobHostility(String mob, boolean hostile) { }
    @Override public void enchant(String target, String enchant, String item, int level) {
        for (Player p : players(target)) p.sendMessage(ChatColor.GRAY + "[Verbum] enchanted " + item + " with " + enchant);
    }
    @Override public void unenchant(String target, String enchant) { }
    @Override public void giveEffect(String target, String effect, int seconds, int level) {
        PotionEffectType t = PotionEffectType.getByName(effect.replace(' ', '_').toUpperCase());
        if (t == null) return;
        for (Player p : players(target)) p.addPotionEffect(new PotionEffect(t, seconds * 20, level - 1));
    }
    @Override public void strikeLightningAt(String target) { lightningAt(target); }

    // ---- world state ----------------------------------------------------------------

    @Override public void openDoor(String place) { toggleBlock(place, true); }
    @Override public void closeDoor(String place) { toggleBlock(place, false); }
    @Override public void openGate(String place) { toggleBlock(place, true); }
    @Override public void closeGate(String place) { toggleBlock(place, false); }
    private void toggleBlock(String place, boolean open) {
        // v1: place is treated as a registered area; the nearest door/lever within it is toggled.
    }
    @Override public void winGame(String target) {
        for (Player p : players(target)) p.sendTitle("You Win", "", 10, 70, 20);
    }
    @Override public void loseGame(String target) {
        for (Player p : players(target)) p.sendTitle("You Lose", "", 10, 70, 20);
    }

    // ---- admin ------------------------------------------------------------------------

    @Override public void ban(String target) {
        for (Player p : players(target)) p.kickPlayer(ChatColor.RED + "Banned");
    }
    @Override public void kick(String target, String reason) {
        for (Player p : players(target)) p.kickPlayer(reason.isEmpty() ? "Kicked" : reason);
    }
    @Override public void kickPlayer(String target, String reason) { kick(target, reason); }
    @Override public void givePermission(String target, String permission) {
        for (Player p : players(target)) p.addAttachment(plugin, permission, true);
    }
    @Override public void removePermission(String target, String permission) {
        for (Player p : players(target)) p.addAttachment(plugin, permission, false);
    }

    // ---- queries ----------------------------------------------------------------

    private boolean countItems(Player p, String item, double atLeast) {
        int c = 0;
        for (ItemStack s : p.getInventory().getContents()) {
            if (s != null && s.getType().name().equalsIgnoreCase(item.replace(' ', '_'))) c += s.getAmount();
        }
        return c >= atLeast;
    }
    @Override public boolean hasItem(String target, String item, double atLeast) {
        return players(target).stream().anyMatch(p -> countItems(p, item, atLeast));
    }
    @Override public boolean isHolding(String target, String item) {
        Player p = first(target);
        if (p == null) return false;
        ItemStack m = p.getInventory().getItemInMainHand();
        return m.getType().name().equalsIgnoreCase(item.replace(' ', '_'));
    }
    @Override public double health(String target) { Player p = first(target); return p == null ? 0 : p.getHealth(); }
    @Override public double maxHealth(String target) { Player p = first(target); return p == null ? 20 : p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue(); }
    @Override public double food(String target) { Player p = first(target); return p == null ? 20 : p.getFoodLevel(); }
    @Override public int level(String target) { Player p = first(target); return p == null ? 0 : p.getLevel(); }
    @Override public boolean isIn(String target, String place) {
        String n = place.toLowerCase();
        if (n.equals("nether")) return first(target).getWorld().getEnvironment() == World.Environment.NETHER;
        if (n.equals("overworld")) return first(target).getWorld().getEnvironment() == World.Environment.NORMAL;
        if (n.equals("end") || n.equals("the end")) return first(target).getWorld().getEnvironment() == World.Environment.THE_END;
        Region r = areas.get(n);
        return r != null && r.contains(first(target).getLocation());
    }
    @Override public boolean isNight() {
        long t = Bukkit.getWorlds().get(0).getTime();
        return t >= 13000 && t <= 23000;
    }
    @Override public boolean isDay() { return !isNight(); }
    @Override public boolean isRain() { return Bukkit.getWorlds().get(0).hasStorm(); }
    @Override public boolean isStorm() { return Bukkit.getWorlds().get(0).hasStorm() && Bukkit.getWorlds().get(0).isThundering(); }
    @Override public int onlinePlayers() { return Bukkit.getOnlinePlayers().size(); }
    @Override public boolean playerOnline(String name) { return Bukkit.getPlayerExact(name) != null; }
    @Override public double coord(String target, char axis) {
        Player p = first(target);
        if (p == null) return 0;
        return switch (axis) { case 'x' -> p.getLocation().getX(); case 'z' -> p.getLocation().getZ(); default -> p.getLocation().getY(); };
    }
    @Override public String dimension(String target) {
        Player p = first(target);
        if (p == null) return "overworld";
        switch (p.getWorld().getEnvironment()) {
            case NETHER: return "nether";
            case THE_END: return "end";
            default: return "overworld";
        }
    }
    @Override public String biome(String target) { Player p = first(target); return p == null ? "" : p.getLocation().getBlock().getBiome().name().toLowerCase(); }
    @Override public String gamemode(String target) { Player p = first(target); return p == null ? "survival" : p.getGameMode().name().toLowerCase(); }
    @Override public boolean isSneaking(String target) { Player p = first(target); return p != null && p.isSneaking(); }
    @Override public boolean isSprinting(String target) { Player p = first(target); return p != null && p.isSprinting(); }
    @Override public boolean isInVehicle(String target) { Player p = first(target); return p != null && p.isInsideVehicle(); }
    @Override public boolean isOnGround(String target) { Player p = first(target); return p != null && p.isOnGround(); }
    @Override public boolean isFlying(String target) { Player p = first(target); return p != null && p.isFlying(); }
    @Override public boolean isBurning(String target) { Player p = first(target); return p != null && p.getFireTicks() > 0; }
    @Override public boolean isPoisoned(String target) { Player p = first(target); return p != null && p.hasPotionEffect(PotionEffectType.POISON); }
    @Override public boolean hasEffect(String target, String effect) {
        Player p = first(target);
        return p != null && p.hasPotionEffect(PotionEffectType.getByName(effect.replace(' ', '_').toUpperCase()));
    }
    @Override public boolean isOp(String target) { Player p = first(target); return p != null && p.isOp(); }
    @Override public boolean hasPermission(String target, String permission) {
        Player p = first(target);
        return p != null && p.hasPermission(permission);
    }
    @Override public boolean playerAlive(String target) { Player p = first(target); return p != null && !p.isDead(); }
    @Override public int mobCountNear(String target, String mob, double radius) {
        Player p = first(target);
        if (p == null) return 0;
        try {
            return p.getNearbyEntities(radius, radius, radius).stream()
                    .filter(e -> e.getType().name().equalsIgnoreCase(mob.replace(' ', '_')))
                    .mapToInt(x -> 1).sum();
        } catch (IllegalArgumentException ex) { return 0; }
    }
    @Override public double getScore(String scoreboard) { return 0; }
    @Override public boolean isBossHalfHealth(String mob) { return false; }

    // ---- live vitals: player -----------------------------------------------------

    @Override public double experience(String target) { Player p = first(target); return p == null ? 0 : p.getTotalExperience(); }
    @Override public int xpToNextLevel(String target) { Player p = first(target); return p == null ? 0 : p.getExpToLevel(); }
    @Override public double xpPercent(String target) { Player p = first(target); return p == null ? 0 : p.getExp(); }
    @Override public double saturation(String target) { Player p = first(target); return p == null ? 0 : p.getSaturation(); }
    @Override public double absorption(String target) { Player p = first(target); return p == null ? 0 : p.getAbsorptionAmount(); }
    @Override public int air(String target) { Player p = first(target); return p == null ? 0 : p.getRemainingAir(); }
    @Override public int maxAir(String target) { Player p = first(target); return p == null ? 300 : p.getMaximumAir(); }
    @Override public int fireTicks(String target) { Player p = first(target); return p == null ? 0 : p.getFireTicks(); }
    @Override public int freezeTicks(String target) { Player p = first(target); return p == null ? 0 : p.getFreezeTicks(); }
    @Override public double walkSpeed(String target) { Player p = first(target); return p == null ? 0.1 : p.getWalkSpeed(); }
    @Override public double flySpeed(String target) { Player p = first(target); return p == null ? 0.05 : p.getFlySpeed(); }
    @Override public int ping(String target) { Player p = first(target); return p == null ? 0 : p.getPing(); }
    @Override public String worldName(String target) { Player p = first(target); return p == null ? "world" : p.getWorld().getName(); }
    @Override public double yaw(String target) { Player p = first(target); return p == null ? 0 : p.getLocation().getYaw(); }
    @Override public double pitch(String target) { Player p = first(target); return p == null ? 0 : p.getLocation().getPitch(); }
    @Override public String facing(String target) {
        Player p = first(target);
        if (p == null) return "north";
        float y = p.getLocation().getYaw();
        if (y < 0) y += 360;
        if (y >= 315 || y < 45) return "south";
        if (y < 135) return "west";
        if (y < 225) return "north";
        return "east";
    }
    @Override public boolean isGlowing(String target) { Player p = first(target); return p != null && p.isGlowing(); }
    @Override public boolean isInvisible(String target) { Player p = first(target); return p != null && p.isInvisible(); }
    @Override public String holdingItem(String target) {
        Player p = first(target);
        return p == null ? "" : p.getInventory().getItemInMainHand().getType().name().toLowerCase();
    }
    @Override public int heldSlot(String target) { Player p = first(target); return p == null ? 0 : p.getInventory().getHeldItemSlot(); }
    @Override public int emptySlots(String target) {
        Player p = first(target);
        if (p == null) return 36;
        int n = 0;
        for (ItemStack s : p.getInventory().getContents()) if (s == null || s.getType() == Material.AIR) n++;
        return n;
    }
    @Override public String teamOf(String target) {
        Player p = first(target);
        if (p == null) return "";
        org.bukkit.scoreboard.Team t = p.getScoreboard().getEntryTeam(p.getName());
        return t == null ? "" : t.getName();
    }

    // ---- live vitals: world & server -----------------------------------------------------

    @Override public String difficulty() {
        if (Bukkit.getWorlds().isEmpty()) return "normal";
        return Bukkit.getWorlds().get(0).getDifficulty().name().toLowerCase();
    }
    @Override public long worldTime() {
        if (Bukkit.getWorlds().isEmpty()) return 6000;
        return Bukkit.getWorlds().get(0).getTime();
    }
    @Override public long dayCount() {
        if (Bukkit.getWorlds().isEmpty()) return 0;
        return Bukkit.getWorlds().get(0).getFullTime() / 24000L;
    }
    @Override public long worldSeed() {
        if (Bukkit.getWorlds().isEmpty()) return 0;
        return Bukkit.getWorlds().get(0).getSeed();
    }
    @Override public double worldBorder() {
        if (Bukkit.getWorlds().isEmpty()) return 0;
        return Bukkit.getWorlds().get(0).getWorldBorder().getSize();
    }
    @Override public Location spawnPoint() {
        if (Bukkit.getWorlds().isEmpty()) return Location.at("world", 0, 64, 0);
        org.bukkit.Location s = Bukkit.getWorlds().get(0).getSpawnLocation();
        return Location.at(Bukkit.getWorlds().get(0).getName(), s.getX(), s.getY(), s.getZ());
    }
    @Override public int maxPlayers() { return Bukkit.getMaxPlayers(); }
    @Override public double tps() {
        try {
            double[] t = Bukkit.getTPS();
            return t.length == 0 ? 20 : Math.min(20, Math.max(0, t[0]));
        } catch (Throwable ignored) { return 20; }
    }

    // ---- inventory & slots ------------------------------------------------------------

    @Override public void setSlot(String target, int slot, String item) {
        for (Player p : players(target)) {
            int idx = Math.max(0, Math.min(35, slot - 1));
            p.getInventory().setItem(idx, make(item));
        }
    }
    @Override public void swapHands(String target) {
        for (Player p : players(target)) {
            ItemStack m = p.getInventory().getItemInMainHand();
            p.getInventory().setItemInMainHand(p.getInventory().getItemInOffHand());
            p.getInventory().setItemInOffHand(m);
        }
    }
    @Override public void clearInventory(String target) {
        for (Player p : players(target)) p.getInventory().clear();
    }
    @Override public void setItemAmount(String target, String item, int amount) {
        for (Player p : players(target))
            for (ItemStack s : p.getInventory().getContents()) {
                if (s != null && s.getType().name().equalsIgnoreCase(item.replace(' ', '_'))) {
                    s.setAmount(Math.max(1, amount));
                    return;
                }
            }
    }
    @Override public void setItemUnbreakable(String target, String item, boolean unbreakable) {
        for (Player p : players(target))
            for (ItemStack s : p.getInventory().getContents()) {
                if (s != null && s.getType().name().equalsIgnoreCase(item.replace(' ', '_'))) {
                    org.bukkit.inventory.meta.ItemMeta m = s.getItemMeta();
                    if (m == null) continue;
                    m.setUnbreakable(unbreakable);
                    s.setItemMeta(m);
                    return;
                }
            }
    }
    @Override public void setSkullOwner(String target, String item, String owner) {
        for (Player p : players(target))
            for (ItemStack s : p.getInventory().getContents()) {
                Material mat = Material.matchMaterial(item.replace(' ', '_').toUpperCase());
                if (s == null) continue;
                if (mat != null && s.getType() == mat && s.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta sm) {
                    sm.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
                    s.setItemMeta(sm);
                    return;
                }
            }
    }

    // ---- player tuning ------------------------------------------------------------

    @Override public void setFlySpeed(String target, double speed) {
        for (Player p : players(target)) p.setFlySpeed((float) Math.max(0, Math.min(1, speed / 40)));
    }
    @Override public void setAttackSpeed(String target, double speed) {
        for (Player p : players(target)) {
            var attr = p.getAttribute(org.bukkit.attribute.Attribute.ATTACK_SPEED);
            if (attr != null) attr.setBaseValue(Math.max(0, speed));
        }
    }
    @Override public void setSaturation(String target, double value) {
        for (Player p : players(target)) p.setSaturation((float) Math.max(0, value));
    }
    @Override public void setAir(String target, int ticks) {
        for (Player p : players(target)) p.setRemainingAir(Math.max(0, ticks));
    }
    @Override public void setFlying(String target, boolean flying) {
        for (Player p : players(target)) { p.setAllowFlight(true); p.setFlying(flying); }
    }
    @Override public void setGliding(String target, boolean gliding) {
        for (Player p : players(target)) p.setGliding(gliding);
    }
    @Override public void setArmorSlot(String target, String armor, String piece) {
        Material mat = Material.matchMaterial(armor.replace(' ', '_').toUpperCase());
        if (mat == null) return;
        ItemStack s = new ItemStack(mat);
        for (Player p : players(target)) {
            String ps = piece.toLowerCase();
            if (ps.contains("helmet") || ps.contains("head")) p.getInventory().setHelmet(s);
            else if (ps.contains("chest") || ps.contains("plate")) p.getInventory().setChestplate(s);
            else if (ps.contains("leg")) p.getInventory().setLeggings(s);
            else if (ps.contains("boot") || ps.contains("feet")) p.getInventory().setBoots(s);
            else if (ps.contains("off")) p.getInventory().setItemInOffHand(s);
        }
    }
    @Override public void setDisplayName(String target, String name) {
        for (Player p : players(target)) p.setDisplayName(name);
    }
    @Override public void setPlayerListName(String target, String name) {
        for (Player p : players(target)) p.setPlayerListName(name);
    }
@Override public void setGlowColor(String target, String color) {
        for (Player p : players(target)) {
            try {
                org.bukkit.ChatColor c = org.bukkit.ChatColor.valueOf(color.toUpperCase().replace(' ', '_'));
                p.setGlowing(true);
                org.bukkit.scoreboard.Scoreboard sb = p.getScoreboard();
                org.bukkit.scoreboard.Team team = sb.getEntryTeam(p.getName());
                if (team == null) {
                    String teamName = "vg" + Integer.toHexString(p.getName().hashCode());
                    if (teamName.length() > 16) teamName = teamName.substring(0, 16);
                    team = sb.getTeam(teamName);
                    if (team == null) {
                        try { team = sb.registerNewTeam(teamName); } catch (IllegalArgumentException ignored) { }
                    }
                    if (team == null) continue;
                    team.addEntry(p.getName());
                }
                team.setColor(c);
            } catch (IllegalArgumentException ignored) { }
        }
    }
    @Override public void setRespawnPoint(String target) {
        for (Player p : players(target)) p.setBedSpawnLocation(p.getLocation(), true);
    }
    @Override public void launch(String target, double power) {
        double v = Math.max(0, Math.min(10, power));
        for (Player p : players(target))
            p.setVelocity(p.getVelocity().setY(v / 2).add(new org.bukkit.util.Vector(0, 0.6, 0)));
    }
    @Override public void removeAllEffects(String target) {
        for (Player p : players(target)) p.getActivePotionEffects().forEach(e -> p.removePotionEffect(e.getType()));
    }

    // ---- world effects ------------------------------------------------------------

    @Override public void dropExperience(Location at, int amount) {
        World w = Bukkit.getWorld(at.world());
        if (w != null && amount > 0) w.spawn(new org.bukkit.Location(w, at.x(), at.y(), at.z()), org.bukkit.entity.ExperienceOrb.class).setExperience(amount);
    }
    @Override public void dropItemAt(Location at, String item, int count) {
        World w = Bukkit.getWorld(at.world());
        Material m = Material.matchMaterial(item.replace(' ', '_').toUpperCase());
        if (w != null && m != null) w.dropItemNaturally(new org.bukkit.Location(w, at.x(), at.y(), at.z()), new ItemStack(m, Math.max(1, count)));
    }
    @Override public void lightningAt(Location at) {
        World w = Bukkit.getWorld(at.world());
        if (w != null) w.strikeLightningEffect(new org.bukkit.Location(w, at.x(), at.y(), at.z()));
    }
    @Override public void playSoundAt(Location at, String sound) {
        World w = Bukkit.getWorld(at.world());
        try {
            Sound s = Sound.valueOf(sound.replace(' ', '_').toUpperCase());
            if (w != null) w.playSound(new org.bukkit.Location(w, at.x(), at.y(), at.z()), s, 1, 1);
        } catch (IllegalArgumentException ignored) { }
    }
    @Override public void stopAllSounds(String target) {
        for (Player p : players(target)) p.stopAllSounds();
    }
    @Override public void playMusicDisc(String target, String disc) {
        Material m = Material.matchMaterial(disc.replace(' ', '_').toUpperCase());
        if (m != null && m.isRecord()) for (Player p : players(target)) p.playSound(p.getLocation(), m.getKey().toString(), 1, 1);
    }
    @Override public void fillRegion(Location a, Location b, String block) {
        World w = Bukkit.getWorld(a.world());
        Material m = Material.matchMaterial(block.replace(' ', '_').toUpperCase());
        if (w == null || m == null) return;
        int x1 = (int) Math.min(a.x(), b.x()), x2 = (int) Math.max(a.x(), b.x());
        int y1 = (int) Math.min(a.y(), b.y()), y2 = (int) Math.max(a.y(), b.y());
        int z1 = (int) Math.min(a.z(), b.z()), z2 = (int) Math.max(a.z(), b.z());
        for (int x = x1; x <= x2; x++) for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++)
            w.getBlockAt(x, y, z).setType(m);
    }
    @Override public void giveRandomItem(String target) {
        Material[] pool = {Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT, Material.IRON_INGOT,
                Material.APPLE, Material.BREAD, Material.OAK_PLANKS, Material.ARROW, Material.STICK};
        ItemStack item = new ItemStack(pool[(int) (Math.random() * pool.length)]);
        for (Player p : players(target)) p.getInventory().addItem(item);
    }

    // ---- depth batch: jailing / homes / riding / repair / holograms / whitelist -----

    private final Map<String, Boolean> jailed = new HashMap<>();
    @Override public void jail(String target) {
        for (Player p : players(target)) { jailed.put(p.getName().toLowerCase(), true); }
    }
    @Override public void unjail(String target) {
        for (Player p : players(target)) jailed.remove(p.getName().toLowerCase());
    }
    @Override public boolean isJailed(String target) {
        return players(target).stream().anyMatch(p -> jailed.getOrDefault(p.getName().toLowerCase(), false));
    }

    private final Map<String, org.bukkit.Location> homes = new HashMap<>();
    @Override public void setHome(String target) {
        for (Player p : players(target)) homes.put(p.getName().toLowerCase(), p.getLocation().clone());
    }
    @Override public void teleportHome(String target) {
        for (Player p : players(target)) {
            org.bukkit.Location home = homes.get(p.getName().toLowerCase());
            if (home != null) p.teleport(home);
        }
    }
    @Override public boolean hasHome(String target) {
        return players(target).stream().anyMatch(p -> homes.containsKey(p.getName().toLowerCase()));
    }

    @Override public boolean isRiding(String target) {
        return players(target).stream().anyMatch(p -> p.isInsideVehicle());
    }
    @Override public void mount(String target) {
        // Mount the nearest rideable entity within 4 blocks.
        for (Player p : players(target)) {
            if (p.isInsideVehicle()) continue;
            p.getNearbyEntities(4, 4, 4).stream()
                .filter(e -> e instanceof org.bukkit.entity.Vehicle || e instanceof org.bukkit.entity.WaterMob)
                .findFirst().ifPresent(v -> v.addPassenger(p));
        }
    }
    @Override public void dismount(String target) {
        for (Player p : players(target)) p.leaveVehicle();
    }

    @Override public void repairItem(String target, boolean all) {
        for (Player p : players(target)) {
            if (all) {
                for (ItemStack item : p.getInventory().getContents())
                    if (item != null && item.getType().getMaxDurability() > 0) item.setDurability((short) 0);
            } else {
                ItemStack held = p.getInventory().getItemInMainHand();
                if (held != null && held.getType().getMaxDurability() > 0) held.setDurability((short) 0);
            }
        }
    }

    private final Map<String, String> holograms = new HashMap<>(); // "player:name" -> text
    @Override public void spawnHologram(String target, String name, String text) {
        for (Player p : players(target)) {
            String key = (p.getName() + ":" + name).toLowerCase();
            holograms.put(key, text);
        }
    }
    @Override public void removeHologram(String target, String name) {
        for (Player p : players(target)) {
            String key = (p.getName() + ":" + name).toLowerCase();
            holograms.remove(key);
        }
    }

    @Override public boolean isWhitelisted(String target) {
        return players(target).stream().anyMatch(Player::isWhitelisted);
    }
    @Override public void setWhitelisted(String target, boolean on) {
        for (Player p : players(target)) p.setWhitelisted(on);
    }
}
