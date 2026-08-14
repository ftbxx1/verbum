package dev.verbum.paper;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.interp.Trigger;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Bridges Bukkit events into the Verbum language by calling engine.trigger(...).
 * Each event becomes a Trigger whose kind matches what Verbum conditions expect
 * (join, death, break, collect, eat, touch water, reach/enter/leave area, chat,
 * command, right click, trade, boss death ...).
 */
public final class VerbumListener implements Listener {

    private final ScriptEngine engine;
    private final PaperRuntime runtime;

    public VerbumListener(VerbumPaperPlugin plugin, ScriptEngine engine, PaperRuntime runtime) {
        this.engine = engine;
        this.runtime = runtime;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        engine.trigger("join", e.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        engine.trigger("quit", e.getPlayer().getName());
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        String killed = e.getEntity().getType().name().toLowerCase().replace('_', ' ');
        if (e.getEntity() instanceof Player p) {
            engine.trigger("death", p.getName());
            if (p.getKiller() != null) engine.trigger(new Trigger("kill", p.getKiller().getName()).with("p", "player"));
            return;
        }
        // boss / mob death
        if (e.getEntity() instanceof EnderDragon || e.getEntity() instanceof Wither) {
            engine.trigger(new Trigger("boss death", killer(e.getEntity())));
        } else {
            engine.trigger(new Trigger("mob death", killer(e.getEntity()))
                    .with("p", killed));
        }
        if (e.getEntity().getKiller() instanceof Player k) {
            engine.trigger(new Trigger("kill", k.getName()).with("p", killed));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent e) {
        String block = e.getBlock().getType().name().toLowerCase().replace('_', ' ');
        engine.trigger(new Trigger("break", e.getPlayer().getName()).with("p", block));
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        String item = e.getItem().getType().name().toLowerCase().replace('_', ' ');
        engine.trigger(new Trigger("eat", e.getPlayer().getName()).with("p", item));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent e) {
        engine.trigger(new Trigger("chat", e.getPlayer().getName()).with("p", e.getMessage()));
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent e) {
        engine.trigger(new Trigger("command", e.getPlayer().getName()).with("p", e.getMessage()));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        switch (e.getAction()) {
            case RIGHT_CLICK_BLOCK: case RIGHT_CLICK_AIR:
                engine.trigger("rightclick", e.getPlayer().getName()); break;
            case LEFT_CLICK_BLOCK: case LEFT_CLICK_AIR:
                engine.trigger("leftclick", e.getPlayer().getName()); break;
            default: break;
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        Entity entity = e.getRightClicked();
        if (entity instanceof Villager) {
            engine.trigger("trade", e.getPlayer().getName());
        }
    }

    @EventHandler
    public void onSleep(PlayerBedEnterEvent e) {
        engine.trigger("sleep", e.getPlayer().getName());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        // after death
        runtime.spawnRespawn(e.getPlayer());
        engine.trigger("respawn", e.getPlayer().getName());
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        String block = e.getBlockPlaced().getType().name().toLowerCase().replace('_', ' ');
        engine.trigger(new Trigger("place", e.getPlayer().getName()).with("p", block));
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) {
            String victim = e.getEntity().getType().name().toLowerCase().replace('_', ' ');
            engine.trigger(new Trigger("damage", p.getName()).with("p", victim));
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        String item = e.getItemDrop().getItemStack().getType().name().toLowerCase().replace('_', ' ');
        engine.trigger(new Trigger("drop", e.getPlayer().getName()).with("p", item));
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String item = e.getRecipe().getResult().getType().name().toLowerCase().replace('_', ' ');
        engine.trigger(new Trigger("craft", p.getName()).with("p", item));
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        engine.trigger("fish", e.getPlayer().getName());
    }

    @EventHandler
    public void onToggleSneak(PlayerToggleSneakEvent e) {
        engine.trigger("togglesneak", e.getPlayer().getName());
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent e) {
        engine.trigger("toggleflight", e.getPlayer().getName());
    }

    /** Handles movement: touching water/lava, and entering/reaching/leaving areas. */
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        Material ground = p.getLocation().getBlock().getType();
        if (ground == Material.WATER || ground == Material.LAVA
                || ground == Material.BUBBLE_COLUMN) {
            String kind = (ground == Material.LAVA) ? "touch lava" : "touch water";
            engine.trigger(kind, p.getName());
        }
        runtime.checkRegions(p);
    }

    @EventHandler
    public void onPickup(org.bukkit.event.entity.EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p) {
            ItemStack it = e.getItem().getItemStack();
            String item = it.getType().name().toLowerCase().replace('_', ' ');
            engine.trigger(new Trigger("collect", p.getName()).with("p", item).with("n", String.valueOf(it.getAmount())));
        }
    }

    private static String killer(org.bukkit.entity.LivingEntity e) {
        if (e.getKiller() != null) return e.getKiller().getName();
        return "world";
    }
}
