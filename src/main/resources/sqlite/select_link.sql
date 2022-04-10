SELECT
    [D].[upper_uid] AS [upper_destination_world_uid],
    [D].[lower_uid] AS [lower_destination_world_uid],
    [L].[destination_x],
    [L].[destination_y],
    [L].[destination_z]
FROM [Link] AS [L]
JOIN [World] AS [W]
ON [W].[key] = [L].[world_key]
JOIN [World] AS [D]
ON [D].[key] = [L].[destination_world_key]
WHERE [W].[upper_uid] = ? AND [W].[lower_uid] = ? AND [L].[x] = ? AND [L].[y] = ? AND [L].[z] = ?;
