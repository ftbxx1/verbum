package dev.verbum.paper;

import dev.verbum.ast.MenuBlock;
import dev.verbum.engine.ScriptEngine;
import dev.verbum.error.VerbumError;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Turns Verbum menus into real clickable inventories. On each tick it opens (or
 * refreshes) the menu the script asked for, and it runs the matching button body
 * when a player clicks one.
 */
public final class VerbumGui implements Listener {

    private final VerbumPaperPlugin plugin;
    private final Map<UUID, String> playerMenu = new HashMap<>();
    private final Map<UUID, Map<Integer, String>> slotLabels = new HashMap<>();

    public VerbumGui(VerbumPaperPlugin plugin) {
        this.plugin = plugin;
    }

    /** Called on the 1-second tick: show any menu the script just told the player to open. */
    public void openPendingMenus() {
        ScriptEngine engine = plugin.engine();
        if (engine == null) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            String want = engine.interpreter().lastOpenedMenu(p.getName());
            if (want == null) continue;
            if (want.equals(playerMenu.get(p.getUniqueId()))) continue;
            MenuBlock menu = engine.interpreter().menu(want);
            if (menu == null) continue;
            show(p, menu);
        }
    }

    private void show(Player p, MenuBlock menu) {
        int rows = Math.max(1, (menu.buttons().size() + 8) / 9);
        Inventory inv = Bukkit.createInventory(null, rows * 9, menu.name());
        Map<Integer, String> labels = new HashMap<>();
        int slot = 0;
        for (MenuBlock.Button b : menu.buttons()) {
            ItemStack item = new ItemStack(Material.EMERALD);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) { meta.setDisplayName(b.label); item.setItemMeta(meta); }
            inv.setItem(slot, item);
            labels.put(slot, b.label);
            slot++;
        }
        playerMenu.put(p.getUniqueId(), menu.name());
        slotLabels.put(p.getUniqueId(), labels);
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String menuName = playerMenu.get(p.getUniqueId());
        if (menuName == null) return;
        e.setCancelled(true);
        int slot = e.getRawSlot();
        Map<Integer, String> labels = slotLabels.get(p.getUniqueId());
        if (labels == null) return;
        String label = labels.get(slot);
        if (label == null) return;
        // clicking a button closes the menu, then runs its action
        p.closeInventory();
        playerMenu.remove(p.getUniqueId());
        try {
            plugin.engine().interpreter().clickButton(menuName, label, p.getName());
        } catch (VerbumError err) {
            p.sendMessage("[Verbum] " + err.getMessage());
        }
    }
}
