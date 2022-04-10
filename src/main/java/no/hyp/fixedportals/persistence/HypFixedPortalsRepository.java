package no.hyp.fixedportals.persistence;

import no.hyp.fixedportals.Link;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.*;

public class HypFixedPortalsRepository implements FixedPortalsRepository {

    FixedPortalsDatabase database;

    public HypFixedPortalsRepository(FixedPortalsDatabase database) {
        this.database = database;
    }

    /*
     * Transaction
     */

    @Override
    public void transaction() throws RepositoryException {
        try {
            database.transaction();
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public void commit() throws RepositoryException {
        try {
            database.commit();
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public void rollback() throws RepositoryException {
        try {
            database.rollback();
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public void mutableTransaction() throws RepositoryException {
        try {
            database.mutableTransaction();
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public void commitImmediateTransaction() throws RepositoryException {
        try {
            database.commitImmediateTransaction();
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public void rollbackImmediateTransaction() throws RepositoryException {
        try {
            database.rollbackImmediateTransaction();
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    /*
     * Repository
     */

    public void upgradeRepository() throws RepositoryException {
        try {
            database.upgradeDatabase();
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    /*
     * Link
     */

    @Override
    public Optional<Link> loadLink(UUID worldUid, int x, int y, int z) throws RepositoryException {
        try {
            @Nullable FixedPortalsDatabase.SerialisedLink serialisedLink = database.selectLink(worldUid.getMostSignificantBits(), worldUid.getLeastSignificantBits(), x, y, z).orElse(null);
            if (serialisedLink == null) return Optional.empty();
            return Optional.of(deserialiseLink(serialisedLink));
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public void saveLink(Link link) throws RepositoryException {
        try {
            UUID worldUid = link.worldUid();
            long worldKey = loadOrGenerateWorldKey(worldUid.getMostSignificantBits(), worldUid.getLeastSignificantBits());
            UUID destinationWorldUid = link.destinationWorldUid();
            long destinationWorldKey = loadOrGenerateWorldKey(destinationWorldUid.getMostSignificantBits(), destinationWorldUid.getLeastSignificantBits());
            database.upsertLink(
                    worldKey, link.x(), link.y(), link.z(),
                    destinationWorldKey, link.destinationX(), link.destinationY(), link.destinationZ()
            );
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    @Override
    public void deleteLink(UUID worldUid, int x, int y, int z) throws RepositoryException {
        try {
            @Nullable Long worldKey = database.selectWorldKey(worldUid.getMostSignificantBits(), worldUid.getLeastSignificantBits()).orElse(null);
            if (worldKey == null) return;
            database.deleteLink(worldKey, x, y, z);
        } catch (SQLException e) {
            throw new RepositoryException(e);
        }
    }

    /*
     * Utility
     */

    private Link deserialiseLink(FixedPortalsDatabase.SerialisedLink serialisedLink) {
        return new Link(
                new UUID(serialisedLink.upperWorldUid(), serialisedLink.lowerWorldUid()),
                serialisedLink.x(), serialisedLink.y(), serialisedLink.z(),
                new UUID(serialisedLink.upperDestinationWorldUid(), serialisedLink.lowerDestinationWorldUid()),
                serialisedLink.destinationX(), serialisedLink.destinationY(), serialisedLink.destinationZ()
        );
    }

    long loadOrGenerateWorldKey(long mostSignificantBits, long leastSignificantBits) throws SQLException {
        @Nullable Long worldKey = database.selectWorldKey(mostSignificantBits, leastSignificantBits).orElse(null);
        if (worldKey != null) return worldKey;
        return database.generateWorld(mostSignificantBits, leastSignificantBits);
    }

    @Override
    public void close() throws Exception { }

}
