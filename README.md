# FixedPortals

Linked portals will always teleport players and entities to the same destination.

## Compatibility

Tested for Spigot 1.18.

## Features and usage

When a player teleports in a Nether portal, the portal is linked. A linked portal will always teleport to the same
destination, even when new portals are created close by.

Automatic linking can be turned off, in case you want to only link portals manually.

Portals can be linked to any portal, in any world. They do not have to be in the Nether or the Overworld.

## Commands

| Command                                 | Description                                         | Permission     |
|-----------------------------------------|-----------------------------------------------------|----------------|
| `netherportal view`                     | View the linked destination of a Nether portal.     | `fixedPortals` |
| `netherportal link <world> <x> <y> <z>` | Link a Nether portal to a given destination portal. | `fixedPortals` |
