/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package queryexecutor;

import queryexecutor.inf.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  Core class which is holder for workers pool.
 *  Is facade for library
 *
 *  How to use:
 *      1. create instance of query executor with desired number of connections
 *      2. call {QueryExecutor#start} to initialise connections
 *      3. perform updates/queries
 *      4. call {QueryExecutor#stop} to terminate connections and free resources
 */
public class QueryExecutor {

    private ExecutorService workers;

    private ArrayBlockingQueue<Connection> connections;

    private ArrayList<Connection> allocatedConnections;

    private DataSource dataSource;

    private AtomicBoolean started;

    /**
     * Create instance of query executor
     * @param size - number of connection to pre-allocate
     * @param dataSource - custom data source
     */
    public QueryExecutor(int size, DataSource dataSource) {
        workers = Executors.newFixedThreadPool(size);
        connections = new ArrayBlockingQueue<Connection>(size);
        allocatedConnections = new ArrayList<Connection>(size);
        this.dataSource = dataSource;
        started = new AtomicBoolean(false);
    }

    /**
     * Starts query executor
     *
     * @throws SQLException
     */
    public void start()
            throws SQLException
    {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        try {

            while (connections.remainingCapacity() > 0) {
                Connection connection = dataSource.createConnection();
                connections.add(connection);
                allocatedConnections.add(connection);
            }
        } catch (Exception ex) {
            throw new SQLException("Failed to initialise connections", ex);
        }
    }

    /**
     * Stops query executor. All scheduled and waiting tasks are terminated.
     *
     */
    public void stop() {
        if (!started.compareAndSet(true, false)) {
            return;
        }

        try {
            workers.shutdownNow();
        } finally {
            for (Connection connection : allocatedConnections) {
                try {
                    connection.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    /**
     * Executes SQL query which expects fetching some data from DB
     *
     * @param query select SQL query
     * @return ResultSet result set for query
     * @throws SQLException
     */
    public ResultSet executeQuery(String query)
            throws SQLException
    {
        QueryRequest request = new QueryRequest(query);
        Future<ResultSet> future = workers.submit(request);

        try {
            return future.get();
        } catch (ExecutionException ex) {
            throw new SQLException("Failed to execute query: " + query, ex.getCause());
        } catch (InterruptedException ex) {
            throw new SQLException("Failed to execute query: " + query, ex);
        }
    }

    /**
     * Executes database update which doesn't expect any result except number of affected rows
     *
     * @param query update SQL query
     * @return UpdateResult
     * @throws SQLException
     */
    public UpdateResult executeUpdate(String query)
            throws SQLException
    {
        UpdateRequest request = new UpdateRequest(query);
        Future<UpdateResult> future = workers.submit(request);

        try {
            return future.get();
        } catch (ExecutionException ex) {
            throw new SQLException("Failed to execute query: " + query, ex.getCause());
        } catch (InterruptedException ex) {
            throw new SQLException("Failed to execute query: " + query, ex);
        }
    }

    /**
     * Helper methods to close result sets
     *
     * @param resultSet
     */
    public static void closeResultSet(ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.getStatement().close();
            } catch (SQLException ex) {
            }
        }
    }

    private class QueryRequest implements Callable<ResultSet> {

        private final String query;

        public QueryRequest(String query) {
            this.query = query;
        }

        @Override
        public ResultSet call()
                throws Exception
        {

            Connection connection = QueryExecutor.this.connections.poll();
            try {
                Statement stmt = connection.createStatement();
                return stmt.executeQuery(query);
            } finally {
                QueryExecutor.this.connections.add(connection);
            }
        }
    }

    private class UpdateRequest implements Callable<UpdateResult> {

        private final String query;

        public UpdateRequest(String query) {
            this.query = query;
        }

        @Override
        public UpdateResult call()
                throws Exception
        {
            Connection connection = QueryExecutor.this.connections.poll();
            Statement stmt = null;
            try {
                stmt = connection.createStatement();
                stmt.executeUpdate(query);
                return new UpdateResult(stmt.getUpdateCount());
            } finally {
                if (stmt != null) {
                    stmt.close();
                }
                QueryExecutor.this.connections.add(connection);
            }
        }
    }
}
