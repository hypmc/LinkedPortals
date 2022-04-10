package no.hyp.fixedportals.persistence;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.Collectors;

public class HypFixedPortalsSqliteDatabase implements FixedPortalsDatabase {

    final Connection connection;

    public HypFixedPortalsSqliteDatabase(Connection connection) {
        this.connection = connection;
    }

    /*
     * Transaction
     */

    @Override
    public void transaction() throws SQLException {
        connection.setAutoCommit(false);
    }

    @Override
    public void commit() throws SQLException {
        connection.commit();
        connection.setAutoCommit(true);
    }

    @Override
    public void mutableTransaction() throws SQLException {

    }

    @Override
    public void rollback() throws SQLException {
        connection.rollback();
        connection.setAutoCommit(true);
    }

    @Override
    public void commitImmediateTransaction() throws SQLException {

    }

    @Override
    public void rollbackImmediateTransaction() throws SQLException {

    }

    /*
     * Database
     */

    @Override
    public void upgradeDatabase() throws SQLException {
        long version = selectVersion();
        if (version == 0) {
            String[] sqls = loadSql("/sqlite/upgrade.sql").split(";");
            for (String sql : sqls) {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.executeUpdate();
                }
            }
            version = selectVersion();
        }
        if (version == 1) {
            return;
        }
        throw new SQLException();
    }

    long selectVersion() throws SQLException {
        String sql = "PRAGMA user_version;";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    long version = result.getLong(1);
                    if (!result.next()) {
                        return version;
                    } else {
                        throw new SQLException();
                    }
                } else {
                    throw new SQLException();
                }
            }
        }
    }

    /*
     * Link
     */

    @Override
    public Optional<SerialisedLink> selectLink(long upperWorldUid, long lowerWorldUid, int x, int y, int z) throws SQLException {
        String sql = loadSql("/sqlite/select_link.sql");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, upperWorldUid);
            statement.setLong(2, lowerWorldUid);
            statement.setInt(3, x);
            statement.setInt(4, y);
            statement.setInt(5, z);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    long upperDestinationWorldUid = result.getLong("upper_destination_world_uid");
                    long lowerDestinationWorldUid = result.getLong("lower_destination_world_uid");
                    int destinationX = result.getInt("destination_x");
                    int destinationY = result.getInt("destination_y");
                    int destinationZ = result.getInt("destination_z");
                    if (!result.next()) {
                        return Optional.of(new SerialisedLink(
                                upperWorldUid, lowerWorldUid, x, y, z,
                                upperDestinationWorldUid, lowerDestinationWorldUid,
                                destinationX, destinationY, destinationZ
                        ));
                    } else {
                        throw new SQLException();
                    }
                } else {
                    return Optional.empty();
                }
            }
        }
    }

    @Override
    public void upsertLink(long worldKey, int x, int y, int z, long destinationWorldKey, int destinationX, int destinationY, int destinationZ) throws SQLException {
        String sql = loadSql("/sqlite/upsert_link.sql");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, worldKey);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);
            statement.setLong(5, destinationWorldKey);
            statement.setInt(6, destinationX);
            statement.setInt(7, destinationY);
            statement.setInt(8, destinationZ);
            statement.executeUpdate();
        }
    }

    @Override
    public void deleteLink(long worldkey, int x, int y, int z) throws SQLException {
        String sql = loadSql("/sqlite/delete_link.sql");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, worldkey);
            statement.setInt(2, x);
            statement.setInt(3, y);
            statement.setInt(4, z);
            statement.executeUpdate();
        }
    }

    /*
     * World
     */

    public Optional<Long> selectWorldKey(long upperWorldUid, long lowerWorldUid) throws SQLException {
        String sql = loadSql("/sqlite/select_world_key.sql");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, upperWorldUid);
            statement.setLong(2, lowerWorldUid);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    long worldKey = result.getLong("key");
                    if (!result.next()) {
                        return Optional.of(worldKey);
                    } else {
                        throw new SQLException();
                    }
                } else {
                    return Optional.empty();
                }
            }
        }
    }

    public long generateWorld(long upperWorldUid, long lowerWorldUid) throws SQLException {
        String sql = loadSql("/sqlite/generate_world.sql");
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, upperWorldUid);
            statement.setLong(2, lowerWorldUid);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    long worldKey = generatedKeys.getLong(1);
                    if (!generatedKeys.next()) {
                        return worldKey;
                    } else {
                        throw new SQLException();
                    }
                } else {
                    throw new SQLException();
                }
            }
        }
    }

    /*
     * Utility
     */

    // ToDo: Replace all SQL files with text blocks (Java 15)
    String loadSql(String path) {
        InputStream stream = this.getClass().getResourceAsStream(path);
        return new BufferedReader(new InputStreamReader(stream)).lines().collect(Collectors.joining("\n"));
    }

}
