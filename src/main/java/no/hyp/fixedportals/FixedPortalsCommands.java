package no.hyp.fixedportals;

import no.hyp.fixedportals.persistence.FixedPortalsRepository;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FixedPortalsCommands implements TabExecutor {

    final ChatColor colour = ChatColor.DARK_PURPLE;

    FixedPortalsPlugin plugin;

    public FixedPortalsCommands(FixedPortalsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] arguments) {
        if (!command.getName().equalsIgnoreCase("netherportal")) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Player only command.");
            return true;
        }
        Player player = (Player) sender;
        @Nullable Block target = player.getTargetBlockExact(6);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "No targetted block.");
            return true;
        }
        if (target.getType() != Material.NETHER_PORTAL) {
            sender.sendMessage(ChatColor.RED + "The targetted block is not a nether portal.");
            return true;
        }
        if (arguments.length >= 1) {
            String subcommand = arguments[0];
            if (subcommand.equalsIgnoreCase("view")) {
                if (arguments.length == 1) {
                    return view(target, player);
                } else {
                    sender.sendMessage(ChatColor.RED + "/" + label + " view");
                    return true;
                }
            } else if (subcommand.equalsIgnoreCase("link")) {
                if (arguments.length == 5) {
                    return link(target, player, arguments);
                } else {
                    sender.sendMessage(ChatColor.RED + "/" + label + " link <world> <x> <y> <z>");
                    return true;
                }
            } else {
                sender.sendMessage(ChatColor.RED + "/" + label + " <view | link> ...");
                return true;
            }
        } else {
            return view(target, player);
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("netherportal")) return null;
        if (args.length == 1) {
            return List.of("view", "link");
        } else if (args.length >= 2) {
            if (args[0].equalsIgnoreCase("link")) {
                if (args.length == 2) {
                    return plugin.getServer().getWorlds().stream().map(x -> x.getName()).toList();
                } else if (args.length == 3 || args.length == 4 || args.length == 5) {
                    return List.of("0");
                }
            }
        }
        return List.of();
    }

    boolean link(Block target, Player player, String[] arguments) {
        String worldNameArgument = arguments[1];
        String xArgument = arguments[2];
        String yArgument = arguments[3];
        String zArgument = arguments[4];
        @Nullable World world = Bukkit.getWorld(worldNameArgument);
        if (world == null) {
            player.sendMessage(String.format(ChatColor.RED + "World " + ChatColor.WHITE + "%s" + ChatColor.RED + " not found.", worldNameArgument));
            return true;
        }
        int x;
        int y;
        int z;
        try {
            x = Integer.parseInt(xArgument);
            y = Integer.parseInt(yArgument);
            z = Integer.parseInt(zArgument);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid coordinate integer argument.");
            return true;
        }
        Block root = plugin.portalRoot(target).orElse(null);
        if (root == null) {
            player.sendMessage(ChatColor.RED + "Root not found.");
            return true;
        }
        Block destinationRoot = plugin.portalRoot(world.getBlockAt(x, y, z)).orElse(null);
        if (destinationRoot == null) {
            player.sendMessage(ChatColor.RED + "Destination is not a nether portal.");
            return true;
        }
        try {
            plugin.saveLink(plugin.repository(), new Link(root, destinationRoot));
        } catch (FixedPortalsRepository.RepositoryException e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "Error occurred.");
            return true;
        }
        player.sendMessage(colour + "Linked portal.");
        return true;
    }

    boolean view(Block target, Player player) {
        Block root = plugin.portalRoot(target).orElse(null);
        if (root == null) {
            player.sendMessage(ChatColor.RED + "Root not found.");
            return true;
        }
        try {
            @Nullable Link link = plugin.loadLink(plugin.repository(), root).orElse(null);
            if (link != null) {
                @Nullable World world = plugin.getServer().getWorld(link.destinationWorldUid());
                if (world != null) {
                    player.sendMessage(String.format(colour + "Portal is linked to " + ChatColor.WHITE + "%s" + colour + ", " + ChatColor.WHITE + "%d" + colour + ", " + ChatColor.WHITE + "%d" + colour + ", " + ChatColor.WHITE + "%d" + colour + ".", world.getName(), link.destinationX(), link.destinationY(), link.destinationZ()));
                    return true;
                } else {
                    player.sendMessage(String.format(ChatColor.RED + "Linked world " + ChatColor.WHITE + "%s" + ChatColor.RED + " is not loaded.", link.destinationWorldUid));
                    return true;
                }
            } else {
                player.sendMessage(colour + "Portal is not linked.");
                return true;
            }
        } catch (FixedPortalsRepository.RepositoryException e) {
            e.printStackTrace();
            player.sendMessage(ChatColor.RED + "Error occurred.");
            return true;
        }
    }

}
