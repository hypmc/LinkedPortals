package no.hyp.fixedportals;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.UUID;

/**
 * Represents a linked nether portal.
 */
public class Link {

    final UUID worldUid;

    final int x;

    final int y;

    final int z;

    final UUID destinationWorldUid;

    final int destinationX;

    final int destinationY;

    final int destinationZ;

    public Link(UUID worldUid, int x, int y, int z, UUID destinationWorldUid, int destinationX, int destinationY, int destinationZ) {
        this.worldUid = worldUid;
        this.x = x;
        this.y = y;
        this.z = z;
        this.destinationWorldUid = destinationWorldUid;
        this.destinationX = destinationX;
        this.destinationY = destinationY;
        this.destinationZ = destinationZ;
    }

    public Link(Block originZero, Block originDestination) {
        this(
                originZero.getWorld().getUID(), originZero.getX(), originZero.getY(), originZero.getZ(),
                originDestination.getWorld().getUID(), originDestination.getX(), originDestination.getY(), originDestination.getZ()
        );
    }

    public UUID worldUid() {
        return worldUid;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int z() {
        return z;
    }

    public UUID destinationWorldUid() {
        return destinationWorldUid;
    }

    public int destinationX() {
        return destinationX;
    }

    public int destinationY() {
        return destinationY;
    }

    public int destinationZ() {
        return destinationZ;
    }

    public Location destination(Location playerLocation) {
        World world = Bukkit.getWorld(destinationWorldUid);
        if (world == null) throw new IllegalArgumentException(); // ToDo
        Location location = world.getBlockAt(destinationX, destinationY, destinationZ).getLocation().add(0.5, 0, 0.5);
        location.setPitch(playerLocation.getPitch());
        location.setYaw(playerLocation.getYaw());
        return location;
    }

    public Block destinationBlock() {
        World world = Bukkit.getWorld(destinationWorldUid);
        if (world == null) throw new IllegalArgumentException(); // ToDo
        return world.getBlockAt(destinationX, destinationY, destinationZ);
    }

}
