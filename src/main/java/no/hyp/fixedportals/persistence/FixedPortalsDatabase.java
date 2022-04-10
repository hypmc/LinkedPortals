package no.hyp.fixedportals.persistence;

import java.sql.SQLException;
import java.util.Optional;

public interface FixedPortalsDatabase {

    /*
     * Transaction
     */

    void transaction() throws SQLException;

    void mutableTransaction() throws SQLException;

    void commit() throws SQLException;

    void rollback() throws SQLException;

    void commitImmediateTransaction() throws SQLException;

    void rollbackImmediateTransaction() throws SQLException;

    /*
     * Database
     */

    void upgradeDatabase() throws SQLException;

    /*
     * Link
     */

    Optional<SerialisedLink> selectLink(long upperWorldUid, long lowerWorldUid, int x, int y, int z) throws SQLException;

    void upsertLink(long worldKey, int x, int y, int z, long destinationWorldKey, int destinationX, int destinationY, int destinationZ) throws SQLException;

    void deleteLink(long worldKey, int x, int y, int z) throws SQLException;

    class SerialisedLink {

        long upperWorldUid;

        long lowerWorldUid;

        int x;

        int y;

        int z;

        long upperDestinationWorldUid;

        long lowerDestinationWorldUid;

        int destinationX;

        int destinationY;

        int destinationZ;

        public SerialisedLink(long upperWorldUid, long lowerWorldUid, int x, int y, int z, long upperDestinationWorldUid, long lowerDestinationWorldUid, int destinationX, int destinationY, int destinationZ) {
            this.upperWorldUid = upperWorldUid;
            this.lowerWorldUid = lowerWorldUid;
            this.x = x;
            this.y = y;
            this.z = z;
            this.upperDestinationWorldUid = upperDestinationWorldUid;
            this.lowerDestinationWorldUid = lowerDestinationWorldUid;
            this.destinationX = destinationX;
            this.destinationY = destinationY;
            this.destinationZ = destinationZ;
        }

        public long upperWorldUid() {
            return upperWorldUid;
        }

        public long lowerWorldUid() {
            return lowerWorldUid;
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

        public long upperDestinationWorldUid() {
            return upperDestinationWorldUid;
        }

        public long lowerDestinationWorldUid() {
            return lowerDestinationWorldUid;
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

    }

    /*
     * World
     */

    long generateWorld(long upperUid, long lowerUid) throws SQLException;

    Optional<Long> selectWorldKey(long upperUid, long lowerUid) throws SQLException;

}
