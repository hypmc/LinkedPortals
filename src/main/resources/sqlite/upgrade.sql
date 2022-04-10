PRAGMA user_version = 1;

CREATE TABLE [World] (
    [key] INTEGER NOT NULL PRIMARY KEY,
    [upper_uid] INTEGER NOT NULL,
    [lower_uid] INTEGER NOT NULL,
    UNIQUE ( [upper_uid], [lower_uid] )
);

CREATE TABLE [Link] (
    [world_key] INTEGER NOT NULL,
    [x] INTEGER NOT NULL,
    [y] INTEGER NOT NULL,
    [z] INTEGER NOT NULL,
    [destination_world_key] INTEGER NOT NULL,
    [destination_x] INTEGER NOT NULL,
    [destination_y] INTEGER NOT NULL,
    [destination_z] INTEGER NOT NULL,
    PRIMARY KEY ( [world_key], [x], [y], [z] )
) WITHOUT ROWID;
