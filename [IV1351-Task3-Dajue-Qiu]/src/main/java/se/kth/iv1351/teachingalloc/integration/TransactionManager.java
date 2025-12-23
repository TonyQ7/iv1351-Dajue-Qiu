package se.kth.iv1351.teachingalloc.integration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Manages database transactions for the teaching allocation application.
 * Handles connection lifecycle, commit, and rollback.
 * 
 * DAO instances are created per-transaction and never reused.
 */
public class TransactionManager {
    private static final String DATABASE_URL = "jdbc:postgresql://localhost:5432/dsp_university";
    private static final String DATABASE_USER = "postgres";
    private static final String DATABASE_PASSWORD = "IV1351";

    /**
     * Functional interface for transactional operations.
     * 
     * @param <T> The return type of the operation.
     */
    @FunctionalInterface
    public interface TransactionalOperation<T> {
        T Execute(TeachingDAO dao) throws Exception;
    }

    /**
     * Functional interface for void transactional operations.
     */
    @FunctionalInterface
    public interface TransactionalVoidOperation {
        void Execute(TeachingDAO dao) throws Exception;
    }

    /**
     * Executes a transactional operation that returns a result.
     * Creates a fresh DAO per transaction, commits on success, rolls back on
     * failure.
     *
     * @param <T>       The return type.
     * @param operation The operation to execute.
     * @return The result of the operation.
     * @throws Exception If the operation fails.
     */
    public <T> T Execute(TransactionalOperation<T> operation) throws Exception {
        Connection connection = null;
        try {
            connection = OpenConnection();
            TeachingDAO dao = new TeachingDAO(connection);
            T result = operation.Execute(dao);
            Commit(connection);
            return result;
        } catch (Exception e) {
            Rollback(connection);
            throw e;
        } finally {
            CloseConnection(connection);
        }
    }

    /**
     * Executes a void transactional operation.
     * Creates a fresh DAO per transaction, commits on success, rolls back on
     * failure.
     *
     * @param operation The operation to execute.
     * @throws Exception If the operation fails.
     */
    public void ExecuteVoid(TransactionalVoidOperation operation) throws Exception {
        Connection connection = null;
        try {
            connection = OpenConnection();
            TeachingDAO dao = new TeachingDAO(connection);
            operation.Execute(dao);
            Commit(connection);
        } catch (Exception e) {
            Rollback(connection);
            throw e;
        } finally {
            CloseConnection(connection);
        }
    }

    /**
     * Opens a new database connection with autocommit disabled.
     */
    private Connection OpenConnection() throws TeachingDBException {
        try {
            Connection connection = DriverManager.getConnection(
                    DATABASE_URL, DATABASE_USER, DATABASE_PASSWORD);
            connection.setAutoCommit(false);
            return connection;
        } catch (SQLException e) {
            throw new TeachingDBException("Could not connect to database", e);
        }
    }

    /**
     * Commits the current transaction.
     */
    private void Commit(Connection connection) throws TeachingDBException {
        if (connection != null) {
            try {
                connection.commit();
            } catch (SQLException e) {
                throw new TeachingDBException("Failed to commit transaction", e);
            }
        }
    }

    /**
     * Rolls back the current transaction.
     */
    private void Rollback(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                // Log but don't throw - we're already handling an error
                System.err.println("Failed to rollback: " + e.getMessage());
            }
        }
    }

    /**
     * Closes the database connection.
     */
    private void CloseConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // Log but don't throw
                System.err.println("Failed to close connection: " + e.getMessage());
            }
        }
    }
}
