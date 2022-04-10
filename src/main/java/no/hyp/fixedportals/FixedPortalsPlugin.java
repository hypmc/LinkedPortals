package no.hyp.fixedportals;

import no.hyp.fixedportals.persistence.HypFixedPortalsRepository;
import no.hyp.fixedportals.persistence.HypFixedPortalsSqliteDatabase;
import no.hyp.fixedportals.persistence.FixedPortalsDatabase;
import no.hyp.fixedportals.persistence.FixedPortalsRepository;
import no.hyp.fixedportals.persistence.FixedPortalsRepository.RepositoryException;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;
import org.jetbrains.annotations.*;

public class FixedPortalsPlugin extends JavaPlugin implements Listener {

    boolean debug;

    Connection sqliteConnection;

    FixedPortalsCommands commands;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        debug = getConfig().getBoolean("debug");
        getServer().getPluginManager().registerEvents(this, this);
        try {
            sqliteConnection = DriverManager.getConnection(String.format("jdbc:sqlite:%s", getConfig().getString("databasePath")));
            repository().upgradeRepository();
        } catch (SQLException | RepositoryException e) {
            getLogger().severe("Error initialising database.");
            e.printStackTrace();
        }
        commands = new FixedPortalsCommands(this);
        getCommand("netherportal").setExecutor(commands);
        getCommand("netherportal").setTabCompleter(commands);
    }

    @Override
    public void onDisable() {
        try {
            sqliteConnection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    FixedPortalsDatabase sqliteDatabase() {
        return new HypFixedPortalsSqliteDatabase(sqliteConnection);
    }

    FixedPortalsRepository repository() {
        return new HypFixedPortalsRepository(sqliteDatabase());
    }

    /**
     * This event is called after portal teleportation. It gives the origin and destination of a Nether portal. If the
     * origin Nether portal is not linked, it will be linked in this listener.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPortalTravelled(PlayerTeleportEvent event) {
        if (!getConfig().getBoolean("autolink")) return;
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;
        if (debug) getLogger().info(String.format("Monitor PlayerTeleportEvent, From: %s, To: %s, Player: %s, Cause: %s", event.getFrom(), event.getTo(), event.getPlayer().getName(), event.getCause()));
        @Nullable Block originRoot = portalRoot(event.getPlayer()).orElse(null);
        if (originRoot == null) return;
        try (FixedPortalsRepository repository = repository()) {
            @Nullable Link originLink = loadLink(repository, originRoot).orElse(null);
            if (originLink == null) { // A portal is linked when a player teleports, and it is not already linked.
                @Nullable Block vanillaDestination = event.getTo().getBlock();
                if (debug) getLogger().info("Linked portal");
                saveLink(repository, new Link(originRoot, vanillaDestination));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called when a player steps into a portal and will be teleported. If a portal is linked, the to parameter will be
     * set to the linked destination so Minecraft finds it. If the destination portal is destroyed, the link is deleted.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPortalTravel(PlayerPortalEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) return;
        if (debug) getLogger().info(String.format("PlayerPortalEvent, From: %s, To: %s, Player: %s, Cause: %s", event.getFrom(), event.getTo(), event.getPlayer().getName(), event.getCause()));
        try (FixedPortalsRepository repository = repository()) {
            @Nullable Block originRoot = portalRoot(event.getPlayer()).orElse(null);
            if (originRoot == null) return;
            @Nullable Link originLink = loadLink(repository, originRoot).orElse(null);
            if (originLink != null) {
                if (originLink.destinationBlock().getType() == Material.NETHER_PORTAL) {
                    // The destination must be a nether portal.
                    if (debug) getLogger().info("Redirecting to linked destination");
                    event.setTo(originLink.destination(event.getPlayer().getLocation()));
                } else {
                    // Delete the link if it does not link to a nether portal.
                    if (debug) getLogger().info(String.format("Linked destination %d %d %d not a Nether portal but a %s. Deleting link.", originLink.destinationBlock().getX(), originLink.destinationBlock().getY(), originLink.destinationBlock().getX(), originLink.destinationBlock().getType()));
                    deleteLink(repository, originRoot);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPortalTravel(EntityPortalEvent event) {
        @Nullable var touchedPortal = touchedPortal(event.getEntity()).orElse(null);
        if (touchedPortal == null) return;
        if (debug) getLogger().info(String.format("EntityPortalEvent, From: %s, To: %s", event.getFrom(), event.getTo()));
        try (FixedPortalsRepository repository = repository()) {
            @Nullable Block originRoot = portalRoot(event.getEntity()).orElse(null);
            if (originRoot == null) return;
            @Nullable Link originLink = loadLink(repository, originRoot).orElse(null);
            @Nullable Location entityLocation = event.getEntity().getLocation();
            if (originLink != null) {
                if (originLink.destinationBlock().getType() == Material.NETHER_PORTAL) {
                    // The destination must be a nether portal.
                    if (debug) getLogger().info("Redirecting to linked destination");
                    event.setTo(originLink.destination(entityLocation));
                } else {
                    // Delete the link if it does not link to a nether portal.
                    if (debug) getLogger().info(String.format("Linked destination %d %d %d not a Nether portal but a %s. Deleting link.", originLink.destinationBlock().getX(), originLink.destinationBlock().getY(), originLink.destinationBlock().getX(), originLink.destinationBlock().getType()));
                    deleteLink(repository, originRoot);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            event.setCancelled(true);
        }
    }

    /**
     * When a portal is broken, it is removed from the database. This only triggers when the portal root is destroyed by
     * a player directly.
     *
     * This currently works poorly because Bukkit does not have the Paper event BlockDestroyEvent.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPortalBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.NETHER_PORTAL) return;
        try {
            deleteLink(repository(), block);
            if (debug) getLogger().info("Portal broken, removed potential link.");
        } catch (RepositoryException e) {
            e.printStackTrace();
            event.setCancelled(true);
        }
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

    /*
     * Repository
     */

    Optional<Link> loadLink(FixedPortalsRepository repository, Block block) throws FixedPortalsRepository.RepositoryException {
        return repository.loadLink(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    void saveLink(FixedPortalsRepository repository, Link link) throws RepositoryException {
        repository.saveLink(link);
    }

    void deleteLink(FixedPortalsRepository repository, Block portalRoot) throws FixedPortalsRepository.RepositoryException {
        repository.deleteLink(portalRoot.getWorld().getUID(), portalRoot.getX(), portalRoot.getY(), portalRoot.getZ());
    }

}
