[![Build Status](https://travis-ci.org/chernser/QueryExecutor.png?branch=develop)](https://travis-ci.org/chernser/QueryExecutor)

QueryExecutor
=============

Alternative approach of working with SQL databases 

Preface
-------------

Connection pools are not connection-leaks free. But they provide direct access to connection, what
  is required for complex things like transactions and prepared statements.

QueryExecutor uses poll of workers with attached connections to them, so your application
  should not care about freeing them.


How to use
-------------

QueryExecutor is facade class which provides main API to library. Each instance of query executor
is bound to custom data source.

You need to implement queryexecutor.inf.DataSource interface to provide connection creation method.


```java

    public class MainDS implements DataSource {

        @Override
        Connection createConnection() throws SQLException {
            String connectionUrl = System.getProperty("DB_URL");
            Connection connection = DriverManager.getConnection(connectionUrl);
            if (connection == null) {
                throw new SQLException("Failed to get connection");
            }
            return connection;
        }
    }


    QueryExecutor mainExecutor = new QueryExecutor(POOL_SIZE, new MainDS());

    ResultSet rs = null;
    try {

        rs = mainExecutor.executeQuery("SELECT * FROM users");
        return mapResultSetToListOfUsers(rs);
    } finally {
        QueryExecutor.closeResultSet(rs);
    }

```
