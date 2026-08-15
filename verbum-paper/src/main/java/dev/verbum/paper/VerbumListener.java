package dev.verbum.paper;

import dev.verbum.engine.ScriptEngine;
import dev.verbum.interp.Trigger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.NotePlayEvent;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.SmithItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketEntityEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerEggThrowEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemBreakEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.raid.RaidFinishEvent;
import org.bukkit.event.raid.RaidTriggerEvent;
import org.bukkit.inventory.ItemStack;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;

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
        String itemName = e.getItem().getType().name();
        if (itemName.contains("POTION")) {
            engine.trigger(new Trigger("consume", e.getPlayer().getName()).with("p", item));
        } else {
            engine.trigger(new Trigger("eat", e.getPlayer().getName()).with("p", item));
        }
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
        } else if (entity instanceof Cow) {
            ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();
            if (hand != null && hand.getType() == Material.BUCKET) {
                engine.trigger(new Trigger("milk", e.getPlayer().getName()).with("p", "cow"));
            }
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

    // ===================== expansion batch: 40+ new events =====================

    @EventHandler
    public void onDamageCause(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        switch (e.getCause()) {
            case FALL: engine.trigger("fall", p.getName()); break;
            case DROWNING: engine.trigger("drown", p.getName()); break;
            case VOID: engine.trigger("void", p.getName()); break;
            case STARVATION: engine.trigger("starve", p.getName()); break;
            case LIGHTNING: engine.trigger("lightning", p.getName()); break;
            case ENTITY_EXPLOSION: case BLOCK_EXPLOSION: engine.trigger("explosion", p.getName()); break;
            case FIRE: case FIRE_TICK: engine.trigger("burn", p.getName()); break;
            case POISON: engine.trigger("poison", p.getName()); break;
            case WITHER: engine.trigger("wither", p.getName()); break;
            default: break;
        }
        engine.trigger("hurt", p.getName());
    }

    @EventHandler
    public void onCombust(EntityCombustEvent e) {
        if (e.getEntity() instanceof Player p) engine.trigger("ignite", p.getName());
    }

    @EventHandler
    public void onResurrect(EntityResurrectEvent e) {
        if (e.getEntity() instanceof Player p) engine.trigger("totem", p.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        engine.trigger(new Trigger("swap", e.getPlayer().getName())
                .with("p", e.getOffHandItem() != null ? e.getOffHandItem().getType().name().toLowerCase().replace('_', ' ') : "air"));
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        engine.trigger(new Trigger("bucketempty", e.getPlayer().getName())
                .with("p", e.getBucket().name().toLowerCase().replace('_', ' ')));
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent e) {
        engine.trigger(new Trigger("bucketfill", e.getPlayer().getName())
                .with("p", e.getBucket().name().toLowerCase().replace('_', ' ')));
    }

    @EventHandler
    public void onBucketEntity(PlayerBucketEntityEvent e) {
        engine.trigger(new Trigger("bucketcatch", e.getPlayer().getName())
                .with("p", e.getEntity().getType().name().toLowerCase().replace('_', ' ')));
    }

    @EventHandler
    public void onItemDamage(PlayerItemDamageEvent e) {
        engine.trigger(new Trigger("itemdamage", e.getPlayer().getName())
                .with("p", e.getItem().getType().name().toLowerCase().replace('_', ' ')));
    }

    @EventHandler
    public void onItemBreak(PlayerItemBreakEvent e) {
        engine.trigger(new Trigger("itembreak", e.getPlayer().getName())
                .with("p", e.getBrokenItem().getType().name().toLowerCase().replace('_', ' ')));
    }

    @EventHandler
    public void onClickInventory(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p) {
            engine.trigger(new Trigger("inventoryclick", p.getName())
                    .with("p", e.getCurrentItem() != null ? e.getCurrentItem().getType().name().toLowerCase().replace('_', ' ') : "air"));
        }
    }

    @EventHandler
    public void onItemHeld(PlayerItemHeldEvent e) {
        engine.trigger(new Trigger("switch", e.getPlayer().getName()).with("p",
                e.getPlayer().getInventory().getItem(e.getNewSlot()) != null
                        ? e.getPlayer().getInventory().getItem(e.getNewSlot()).getType().name().toLowerCase().replace('_', ' ') : "air"));
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        if (e.getEntity().getShooter() instanceof Player s) {
            engine.trigger(new Trigger("shoot", s.getName())
                    .with("p", e.getEntity().getType().name().toLowerCase().replace('_', ' ')));
        }
        if (e.getHitEntity() instanceof Player p) {
            if (e.getEntity().getShooter() instanceof Player) engine.trigger("arrow", p.getName());
            engine.trigger("projectilehit", p.getName());
        }
    }

    @EventHandler
    public void onPistonExtend(BlockPistonExtendEvent e) {
        Player p = nearestPlayer(e.getBlock(), 48);
        if (p != null) engine.trigger("piston", p.getName());
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent e) {
        Player p = nearestPlayer(e.getBlock(), 48);
        if (p != null) engine.trigger("pistonretract", p.getName());
    }

    @EventHandler
    public void onNote(NotePlayEvent e) {
        Player p = nearestPlayer(e.getBlock(), 32);
        if (p != null) engine.trigger("note", p.getName());
    }

    @EventHandler
    public void onRaidTrigger(RaidTriggerEvent e) {
        engine.trigger("raid", e.getPlayer().getName());
    }

    @EventHandler
    public void onRaidFinish(RaidFinishEvent e) {
        org.bukkit.Raid raid = e.getRaid();
        Player p = null;
        if (raid != null) {
            for (java.util.UUID id : raid.getHeroes()) {
                Player hero = Bukkit.getPlayer(id);
                if (hero != null) { p = hero; break; }
            }
        }
        World w = e.getWorld();
        if (p == null && w != null && !w.getPlayers().isEmpty()) p = w.getPlayers().get(0);
        if (p != null) engine.trigger("raidwin", p.getName());
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent e) {
        engine.trigger(new Trigger("gamemodechange", e.getPlayer().getName())
                .with("p", e.getNewGameMode().name().toLowerCase().replace('_', ' ')));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        engine.trigger(new Trigger("worldchange", e.getPlayer().getName())
                .with("p", e.getFrom().getName()));
    }

    @EventHandler
    public void onBreed(EntityBreedEvent e) {
        if (e.getBreeder() instanceof Player p) {
            engine.trigger(new Trigger("breed", p.getName())
                    .with("p", e.getEntity().getType().name().toLowerCase().replace('_', ' ')));
        }
    }

    @EventHandler
    public void onTame(EntityTameEvent e) {
        if (e.getOwner() instanceof Player p) {
            engine.trigger(new Trigger("tame", p.getName())
                    .with("p", e.getEntity().getType().name().toLowerCase().replace('_', ' ')));
        }
    }

    @EventHandler
    public void onAdvancement(PlayerAdvancementDoneEvent e) {
        engine.trigger(new Trigger("advancement", e.getPlayer().getName())
                .with("p", e.getAdvancement().getKey().getKey().replace('_', ' ')));
    }

    @EventHandler
    public void onArmorChange(PlayerArmorChangeEvent e) {
        engine.trigger(new Trigger("armorchange", e.getPlayer().getName())
                .with("p", e.getNewItem() != null ? e.getNewItem().getType().name().toLowerCase().replace('_', ' ') : "air"));
    }

    @EventHandler
    public void onArmorStandEdit(PlayerArmorStandManipulateEvent e) {
        engine.trigger(new Trigger("armorstand", e.getPlayer().getName())
                .with("p", e.getRightClicked().getType().name().toLowerCase().replace('_', ' ')));
    }

    @EventHandler
    public void onBookEdit(PlayerEditBookEvent e) {
        engine.trigger("bookedit", e.getPlayer().getName());
    }

    @EventHandler
    public void onShear(PlayerShearEntityEvent e) {
        engine.trigger(new Trigger("shear", e.getPlayer().getName())
                .with("p", e.getEntity().getType().name().toLowerCase().replace('_', ' ')));
    }

    @EventHandler
    public void onEggThrow(PlayerEggThrowEvent e) {
        engine.trigger("eggthrow", e.getPlayer().getName());
    }

    @EventHandler
    public void onKick(PlayerKickEvent e) {
        engine.trigger("kick", e.getPlayer().getName());
    }

    @EventHandler
    public void onCraftStart(PrepareItemCraftEvent e) {
        if (e.getView().getPlayer() instanceof Player p) engine.trigger("craftstart", p.getName());
    }

    @EventHandler
    public void onSmith(SmithItemEvent e) {
        if (e.getView().getPlayer() instanceof Player p) engine.trigger("smith", p.getName());
    }

    @EventHandler
    public void onSmelt(FurnaceExtractEvent e) {
        engine.trigger(new Trigger("smelt", e.getPlayer().getName())
                .with("p", e.getItemType().name().toLowerCase().replace('_', ' ')));
    }

    /** Nearest online player within `range` blocks of a block (for block-caused events). */
    private static Player nearestPlayer(Block b, double range) {
        if (b == null) return null;
        Player best = null;
        double bestD = Double.MAX_VALUE;
        for (Player pl : b.getWorld().getPlayers()) {
            double d = pl.getLocation().distance(b.getLocation());
            if (d < bestD) { bestD = d; best = pl; }
        }
        return best != null && bestD <= range ? best : null;
    }

    private static String killer(org.bukkit.entity.LivingEntity e) {
        if (e.getKiller() != null) return e.getKiller().getName();
        return "world";
    }
}
