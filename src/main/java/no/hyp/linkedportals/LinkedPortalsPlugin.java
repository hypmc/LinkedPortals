package no.hyp.linkedportals;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.*;

public class LinkedPortalsPlugin extends JavaPlugin implements Listener {

    public NamespacedKey key;

    boolean debug;

    boolean backLink;

    LinkedPortalsCommands commands;

    @Override
    public void onEnable() {
        key = NamespacedKey.fromString("linked-portals", this);
        saveDefaultConfig();
        debug = getConfig().getBoolean("debug");
        backLink = getConfig().getBoolean("backlink");
        getServer().getPluginManager().registerEvents(this, this);
        commands = new LinkedPortalsCommands(this);
        getCommand("netherportal").setExecutor(commands);
        getCommand("netherportal").setTabCompleter(commands);
    }

    /**
     * This event is called after portal teleportation. It gives the origin and
     * destination of a Nether portal. If theorigin Nether portal is not linked,
     * it will be linked in this listener.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPortalTravelled(PlayerTeleportEvent event) {
        if (!getConfig().getBoolean("autolink")) return;
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;
        if (debug) getLogger().info(String.format("Monitor PlayerTeleportEvent, From: %s, To: %s, Player: %s, Cause: %s", event.getFrom(), event.getTo(), event.getPlayer().getName(), event.getCause()));
        @Nullable Block originRoot = portalRoot(event.getPlayer()).orElse(null);
        if (originRoot == null) return;
        @Nullable Link originLink = loadLink(originRoot).orElse(null);
        if (originLink != null) return;
        // A portal is linked when a player teleports and it is not already linked.
        @Nullable Block vanillaDestination = event.getTo().getBlock();
        if (debug) getLogger().info("Linked portal");
        saveLink(originRoot.getChunk(), new Link(originRoot, vanillaDestination));
        if (backLink) {
            var destinationRoot = portalRoot(vanillaDestination).orElse(null);
            if (destinationRoot == null) return;
            var destinationLink = loadLink(destinationRoot).orElse(null);
            if (destinationLink != null) return;
            saveLink(destinationRoot.getChunk(), new Link(destinationRoot, originRoot));
            if (debug) getLogger().info("Linked portal back");
        }
    }

    /**
     * Called when a player steps into a portal and will be teleported. If a
     * portal is linked, the to parameter will be set to the linked destination
     * so Minecraft finds it. If the destination portal is destroyed, the link
     * is deleted.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPortalTravel(PlayerPortalEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;
        if (debug) getLogger().info(String.format("PlayerPortalEvent, From: %s, To: %s, Player: %s, Cause: %s", event.getFrom(), event.getTo(), event.getPlayer().getName(), event.getCause()));
        @Nullable Block originRoot = portalRoot(event.getPlayer()).orElse(null);
        if (originRoot == null) return;
        @Nullable Link originLink = loadLink(originRoot).orElse(null);
        if (originLink != null) {
            if (originLink.destinationBlock().getType() == Material.NETHER_PORTAL) {
                // The destination must be a nether portal.
                if (debug) getLogger().info("Redirecting to linked destination");
                event.setTo(originLink.destination(event.getPlayer().getLocation()));
            } else {
                // Delete the link if it does not link to a nether portal.
                if (debug) getLogger().info(String.format("Linked destination %d %d %d not a Nether portal but a %s. Deleting link.", originLink.destinationBlock().getX(), originLink.destinationBlock().getY(), originLink.destinationBlock().getX(), originLink.destinationBlock().getType()));
                deleteLink(originRoot);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPortalTravel(EntityPortalEvent event) {
        @Nullable var touchedPortal = touchedPortal(event.getEntity()).orElse(null);
        if (touchedPortal == null) return;
        if (debug) getLogger().info(String.format("EntityPortalEvent, From: %s, To: %s", event.getFrom(), event.getTo()));
        @Nullable Block originRoot = portalRoot(event.getEntity()).orElse(null);
        if (originRoot == null) return;
        @Nullable Link originLink = loadLink(originRoot).orElse(null);
        @Nullable Location entityLocation = event.getEntity().getLocation();
        if (originLink != null) {
            if (originLink.destinationBlock().getType() == Material.NETHER_PORTAL) {
                // The destination must be a nether portal.
                if (debug) getLogger().info("Redirecting to linked destination");
                event.setTo(originLink.destination(entityLocation));
            } else {
                // Delete the link if it does not link to a nether portal.
                if (debug) getLogger().info(String.format("Linked destination %d %d %d not a Nether portal but a %s. Deleting link.", originLink.destinationBlock().getX(), originLink.destinationBlock().getY(), originLink.destinationBlock().getX(), originLink.destinationBlock().getType()));
                deleteLink(originRoot);
            }
        }
    }

    /**
     * When a portal is broken, remove the link too. This only triggers when the
     * portal root is destroyed by a player directly.
     *
     * TODO: This currently works poorly because Bukkit does not have the Paper event BlockDestroyEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPortalBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.NETHER_PORTAL) return;
        deleteLink(block);
        if (debug) getLogger().info("Portal broken, removed potential link.");
    }

    /*
     * Portal root
     */

    Optional<Block> portalRoot(Iterable<BlockState> states) {
        @Nullable Block zero = null;
        for (BlockState state : states) {
            if (state.getType() != Material.NETHER_PORTAL) continue;
            if (zero == null) zero = state.getBlock();
            if (state.getY() < zero.getY()) {
                zero = state.getBlock();
            } else if (state.getY() == zero.getY()) {
                if (state.getX() < zero.getX()) {
                    zero = state.getBlock();
                } else if (state.getX() == zero.getX()) {
                    if (state.getZ() < zero.getZ()) {
                        zero = state.getBlock();
                    }
                }
            }
        }
        return Optional.ofNullable(zero);
    }

    Optional<Block> portalRoot(Block portalBlock) {
        if (portalBlock.getType() != Material.NETHER_PORTAL) return Optional.empty();
        Block searchBlock = portalBlock;
        while (true) {
            Block down = searchBlock.getRelative(BlockFace.DOWN);
            if (down.getType() == Material.NETHER_PORTAL) {
                searchBlock = down;
            } else {
                break;
            }
        }
        while (true) {
            Block north = searchBlock.getRelative(BlockFace.NORTH);
            if (north.getType() == Material.NETHER_PORTAL) {
                searchBlock = north;
            } else {
                break;
            }
        }
        while (true) {
            Block west = searchBlock.getRelative(BlockFace.WEST);
            if (west.getType() == Material.NETHER_PORTAL) {
                searchBlock = west;
            } else {
                break;
            }
        }
        return Optional.of(searchBlock);
    }

    Optional<Block> touchedPortal(Entity entity) {
        World world = entity.getWorld();
        BoundingBox box = entity.getBoundingBox();
        int x = (int) Math.round(Math.floor(box.getMinX()));
        int upperX = (int) Math.round(Math.ceil(box.getMaxX()));
        while (x < upperX) {
            int z = (int) Math.round(Math.floor(box.getMinZ()));
            int upperZ = (int) Math.round(Math.ceil(box.getMaxZ()));
            while (z < upperZ) {
                int y = (int) Math.round(Math.floor(box.getMinY()));
                int upperY = (int) Math.round(Math.ceil(box.getMaxY()));
                while (y < upperY) {
                    Block block = world.getBlockAt(x, y, z);
                    if (block.getType() == Material.NETHER_PORTAL) return Optional.of(block);
                    y++;
                }
                z++;
            }
            x++;
        }
        return Optional.empty();
    }

    Optional<Block> portalRoot(Entity entity) {
        @Nullable Block touchingBlock = touchedPortal(entity).orElse(null);
        if (touchingBlock == null) return Optional.empty();
        return portalRoot(touchingBlock);
    }

    /**
     * Read the list of links from YAML.
     */
    List<Link> readPortals(PersistentDataContainer container) {
        List<Link> links = new ArrayList<>();
        if (container.has(key, PersistentDataType.STRING)) {
            var serda = container.get(key, PersistentDataType.STRING);
            var config = new YamlConfiguration();
            try {
                config.loadFromString(serda);
            } catch (InvalidConfigurationException e) {
                throw new RuntimeException(e);
            }
            if (debug) {
                System.out.println("Read:");
                System.out.println(config.saveToString());
            }
            var list = config.getMapList("links");
            for (var l : list) {
                ConfigurationSection section = new YamlConfiguration();
                section = section.createSection("temp", l);
                var chunkX = section.getInt("i");
                var chunkY = section.getInt("j");
                var chunkZ = section.getInt("k");
                var destinationUid = UUID.fromString(section.getString("destinationUid"));
                var destinationX = section.getInt("destinationX");
                var destinationY = section.getInt("destinationY");
                var destinationZ = section.getInt("destinationZ");
                links.add(new Link(chunkX, chunkY, chunkZ, destinationUid, destinationX, destinationY, destinationZ));
            }
        }
        return links;
    }

    /**
     * Write the list of links to YAML.
     */
    void writePortals(PersistentDataContainer container, List<Link> links) {
        if (!links.isEmpty()) {
            var config = new YamlConfiguration();
            var list = new ArrayList<>();
            for (var l : links) {
                var section = new YamlConfiguration();
                section.set("i", l.i);
                section.set("j", l.j);
                section.set("k", l.k);
                section.set("destinationUid", l.destinationWorldUid.toString());
                section.set("destinationX", l.destinationX);
                section.set("destinationY", l.destinationY);
                section.set("destinationZ", l.destinationZ);
                list.add(section);
            }
            config.set("links", list);
            container.set(key, PersistentDataType.STRING, config.saveToString());
            if (debug) {
                System.out.println("Write:");
                System.out.println(config.saveToString());
            }
        } else {
            container.remove(key);
        }
    }

    Optional<Link> loadLink(Block block) {
        var chunk = block.getChunk();
        var data = chunk.getPersistentDataContainer();
        var links = readPortals(data);
        for (var link : links) {
            if (link.i == block.getX() % 16 && link.j == block.getY() && link.k == block.getZ() % 16) {
                return Optional.of(link);
            }
        }
        return Optional.empty();
    }

    void saveLink(Chunk chunk, Link link) {
        var data = chunk.getPersistentDataContainer();
        var links = readPortals(data);
        // Remove the link already at this location if there is one.
        var i = 0;
        while (i < links.size()) {
            var l = links.get(i);
            if (l.i == link.i && l.j == link.j && l.k == link.k) {
                links.remove(i);
                break;
            }
            i++;
        }
        links.add(link);
        writePortals(data, links);
    }

    void deleteLink(Block block) {
        var chunk = block.getChunk();
        var data = chunk.getPersistentDataContainer();
        var links = readPortals(data);
        var i = 0;
        while (i < links.size()) {
            var l = links.get(i);
            if (l.i == block.getX() % 16 && l.j == block.getY() && l.k == block.getZ() % 16) {
                links.remove(i);
                break;
            }
            i++;
        }
        writePortals(data, links);
    }

}
