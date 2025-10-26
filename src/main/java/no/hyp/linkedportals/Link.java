package no.hyp.linkedportals;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.UUID;

/**
 * Represents a linked nether portal.
 */
public class Link {

    final int i;

    final int j;

    final int k;

    final UUID destinationWorldUid;

    final int destinationX;

    final int destinationY;

    final int destinationZ;

    public Link(int i, int j, int k, UUID destinationWorldUid, int destinationX, int destinationY, int destinationZ) {
        this.i = i;
        this.j = j;
        this.k = k;
        this.destinationWorldUid = destinationWorldUid;
        this.destinationX = destinationX;
        this.destinationY = destinationY;
        this.destinationZ = destinationZ;
    }

    public Link(Block originZero, Block originDestination) {
        this(
                originZero.getX() % 16, originZero.getY(), originZero.getZ() % 16,
                originDestination.getWorld().getUID(), originDestination.getX(), originDestination.getY(), originDestination.getZ()
        );
    }

    public int i() {
        return i;
    }

    public int j() {
        return j;
    }

    public int k() {
        return k;
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
