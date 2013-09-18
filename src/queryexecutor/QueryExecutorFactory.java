/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package queryexecutor;

import queryexecutor.inf.DataSource;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

public class QueryExecutorFactory {

    private static final ConcurrentHashMap<String, QueryExecutor> instances;

    private static int executorPoolSize = 5;

    static {
        instances = new ConcurrentHashMap<String, QueryExecutor>();
    }

    public static QueryExecutor getQueryExecutorAndStart(DataSource dataSource)
    {
        String key = dataSource.getClass().getName();
        QueryExecutor instance = instances.get(key);

        if (instance == null) {
            instance = new QueryExecutor(executorPoolSize, dataSource);
            QueryExecutor oldInstance = instances.putIfAbsent(key, instance);
            instance = oldInstance == null ? instance : oldInstance;
            try {
                instance.start();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
        return instance;
    }

    public static void setExecutorPoolSize(int size) {
        executorPoolSize = size;
    }

    public static void terminateExecutors() {
        for (QueryExecutor executor : instances.values()) {
            executor.stop();
        }
    }
}
