/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package queryexecutor.inf;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interface which library client should implement to provide
 *  connection creation factory method
 *
 */
public interface DataSource {

    /**
     * Method should return new connection to db servers.
     *
     * @return Connection
     * @throws SQLException
     */
    Connection createConnection() throws SQLException;
}
