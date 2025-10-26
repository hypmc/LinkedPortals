# LinkedPortals

Link portals so that they always teleport players and entities to the same
destination, even when new portals are built nearby.

## Compatibility

Tested for Spigot 1.21.

## Features and usage

- When a Nether portal (that is not linked) is used, the portal is linked to its
  destination. A linked portal will always teleport to the same destination, even
  when new portals are created nearby.

- Automatic linking can be turned off, in case you want to only link portals
  manually with commands.

- A portal may be linked to any other portal, in any world (even the same world).

## Commands

| Command                                 | Description                                         | Permission           |
|-----------------------------------------|-----------------------------------------------------|----------------------|
| `netherportal view`                     | View the linked destination of a Nether portal.     | `linkedPortals.link` |
| `netherportal link <world> <x> <y> <z>` | Link a Nether portal to a given destination portal. | `linkedPortals.link` |
