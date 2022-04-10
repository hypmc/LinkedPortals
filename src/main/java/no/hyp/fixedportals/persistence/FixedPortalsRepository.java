package no.hyp.fixedportals.persistence;

import no.hyp.fixedportals.Link;

import java.util.Optional;
import java.util.UUID;

public interface FixedPortalsRepository extends AutoCloseable {

    /*
     * Transaction
     */

    void transaction() throws RepositoryException;

    void commit() throws RepositoryException;

    void rollback() throws RepositoryException;

    void mutableTransaction() throws RepositoryException;

    void commitImmediateTransaction() throws RepositoryException;

    void rollbackImmediateTransaction() throws RepositoryException;

    /*
     * Repository
     */

    void upgradeRepository() throws RepositoryException;

    /*
     * Link
     */

    Optional<Link> loadLink(UUID worldUid, int x, int y, int z) throws RepositoryException;

    void saveLink(Link link) throws RepositoryException;

    void deleteLink(UUID worldUid, int x, int y, int z) throws RepositoryException;

    class RepositoryException extends Exception {

        public RepositoryException(Throwable t) {
            super(t);
        }

    }

}
