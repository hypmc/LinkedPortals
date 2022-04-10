INSERT INTO [Link] (
    [world_key],
    [x],
    [y],
    [z],
    [destination_world_key],
    [destination_x],
    [destination_y],
    [destination_z]
)
VALUES ( ?, ?, ?, ?, ?, ?, ?, ? )
ON CONFLICT ( [world_key], [x], [y], [z] )
DO UPDATE SET
[destination_world_key] = [Excluded].[destination_world_key],
[destination_x] = [Excluded].[destination_x],
[destination_y] = [Excluded].[destination_y],
[destination_z] = [Excluded].[destination_z];
