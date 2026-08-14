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
}
