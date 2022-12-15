# DB Module (ICL.db)

Manage databases, such as MySQL, SQLite, Postgresql. 
Create, store and perform CRUD operations to data without having 
to use SQL (a light-weight implementation as alternative to Hibernate).

There are mainly two ways to use this module:
1. Fluid query instructions (SQL generator)
2. Model based operations (Automatic CRUD operations)

[JavaDoc](https://intellisrc.gitlab.io/common/#db)

## Usage

Follow the instructions on the last published version in [maven repository](https://mvnrepository.com/artifact/com.intellisrc/crypt)

## Configuration

In order to connect to a database, we need to specify
the corresponding settings:

```properties
# file: config.properties

# Available types: mysql, mariadb, sqlite, postgresql, sqlserver, oracle, firebird, derby
db.type=mysql
# Optional: (if not set, default will be used)
#db.name=users_db
#db.user=myuser
#db.pass=mypass
#db.host=10.0.0.20
#db.port=3306

# Other options (with default values):

# The time (in sec) before a connection is discarded if it is not returned to the pool (usually it means close() is missing).
#db.timeout=60
# Max life of a connection (in sec). Once it expires, a new connection should be created.
#db.expire=600
# Disable database cache
#db.cache=false
```

## Fluid Query Instructions

Features
* Auto Primary Key detection
* SELECT
  * Limit, Offset
  * GroupBy
* INSERT/REPLACE (UPSERT) 
  * Last ID request
* UPDATE
* DELETE
  * TRUNCATE
* Raw SQL execution

> NOTE: This method (Fluid Query) won't create the table for you. 
> If you want this library to create and update your tables for you, use the `Model` approach.

To connect and query a database is easy:

```groovy
DB db = Database.default.connect() // Get connection from "default" pool
String passwordHash = db.table("users").field("password").get(userId).toString()
db.close() // Return connection to the pool
```

> NOTE: You can use `Database.default.waitForConnection()` if the database delays to start (for example, during
> a server restart).

### Connecting to a secondary database / Alternative method

You can connect to any supported database using their classes (look inside db/jdbc/) in this way
(single connection):

```groovy
DB db = new Oracle(
            name : "users",         // 'dbname' and 'database' are aliases
            user : "admin",         // 'username' is an alias
            pass : "secret",        // 'password' is an alias
            host : "server.remote", // 'hostname' is an alias
            //... check the class source code for more parameters ...
        ).connect()
//... do something ...
db.close()
```

other way to connect (if the database type is set in runtime) is:

```groovy
DB db = JDBC.fromSettings(
        type : "oracle",
        name : "users",         // 'dbname' and 'database' are aliases
        user : "admin",         // 'username' is an alias
        pass : "secret",        // 'password' is an alias
        host : "server.remote", // 'hostname' is an alias
        //... check the class source code for more parameters ...
).connect()
//... do something ...
db.close()
```

The previous example is useful if you have few connections to a database.
To improve the connection performance (when expecting a high number of connections),
you can create a pool for that connection in this way:

```groovy
Database oracle = new Database(new Oracle(/* ... */))

void thisMethodWillBeCalledManyTimes() {
    DB db = oracle.connect()
    //... do something ...
    db.close() // This will return the connection to the pool to be reused
}

oracle.quit() // Close all connections
```

#### Notes
 * The pool has no limit number of connections (those are controlled usually by
the database server). The connections will be released automatically.

### Handling Exceptions

Either you are using a single connection or a pool of connections, by default,
all database related exceptions are caught and logged. For example, if you
want to retrieve many rows from the database (as a `List` object), but your
have a mistake in your parameters or there is an exception thrown, it will 
be reported into your logs, but it will return an empty list. In that way, you 
don't need to catch those exceptions in your code. Still, there may be cases
in which you want to handle such exceptions in your code, you can do it like this:

```groovy
ErrorHandler handler = {
  Throwable th ->
    // Handle the exception or error:
    switch (th) {
      case SQLException: break // errors during connection, prepare, etc.
      case SQLSyntaxErrorException: break // syntax errors
      case Exception: break // other exceptions
      case AssertionError: break // validations from this library
    }
} as ErrorHandler

// Using default:
Database.default.onError = handler

// Using single connection:
MySQL mysql = new MySQL(
        onError = handler
)
// or:
mysql.onError = handler

// Using pool:
Database oracle = new Database(new Oracle(/* ... */))
oracle.onError = handler
```

### Common Examples

```groovy
// Get all Users:
db.table("users").get().toListMap() 

// Get a list of usernames:
db.table("users").key("uid").field("username").get([100, 200, 300]).toList()

// Get a single user:
db.table("users").key("uid").get(500).toMap()

// Insert User:
db.table("users").insert([
   name     : "Jessy",
   email    : "jessy@example.com",
   age      : 68     
])
int id = db.lastID()

// Update User using id:
db.table("users").key("uid").update(100, [age : 43])

// Delete User:
db.table("users").key("uid").delete(100)

// Advanced search:
db.table("users").fields("name", "email")
        .where("name LIKE '%?%'", searchName)
        .order("age", Query.SortOrder.DESC)
        .group("area")
        .limit(10)
        .get()
        .toListMap()

// Executing query (e.g. drop all tables in MySQL)
db.getSQL("SHOW TABLES").toList().each { // You can also use: db.get(new Query(...))
    if(db.setSQL("DROP TABLE ?", it)) {  // You can also use: db.set(new Query(...))
      println "Table was dropped"
    }
}
```

> NOTE: For complex queries, we recommend you to create views or stored procedures (to keep your code simple).

## Model Based Operations

Instead of (or additionally to) the Fluid Query Instructions, you can
use a `Model` object (with `Columns`) and a `Table` to perform operations
on it, similar to Hibernate and JPA (but much more simple).

The goal is to accomplish most of the common operations with a database
while keeping it as simple as possible without over-complicated 
instructions or implementation designs.

Features
* Table creation (fields with types)
* Foreign keys creation
* CRUD operations using Java objects

> NOTE: Databases and permissions are not created automatically.

### Example:

The first step is to create your `Model` and `Table` classes:

#### Model class

A `Model` class is the data description of what we want to store in a table.
It must extend: `Model` and in most cases, they will need an `int id` field
as primary key (except in `many-to-many` relationships).

Each field that we want to create in the table, we need to annotate with 
`@Column` (see `Column` annotation for more details on how to use it).

```groovy
enum MyColor {
    WHITE, RED, GREEN, BLUE, YELLOW, BLACK
}

class User extends Model {
    @Column(primary = true, autoincrement = true)
    int id
    @Column(nullable = false, length = 100)
    String name = ""
    @Column(key = true, unique = true)
    String email
    @Column(unsigned = true)
    int age
    @Column
    boolean active = true
    @Column // For 'enum', nothing special is needed (* see note below)
    MyColor color = MyColor.BLACK
}
```
`Enum` columns are automatically converted into `ENUM` in the database for convenience. This make your data easy to
read without using more storage than needed. Everytime you update the values of an `Enum` used as column, you
will need to update the Model version or recreate it again. 

If you are planning to update the model, we recommend to use `@ModelMeta`, for
example:

```groovy
@ModelMeta(version = 2)
class User extends Model { /* ... */ }
```

When declared version is different from the one stored in the database, 
the table will be updated automatically (by default). You can turn off
that behaviour and execute the update on your terms.
You can read more about it next, as the update process is taken care by the `Table` class.

#### Table class

A `Table` class is what we are going to use to interact with the data (CRUD
operations, search data, etc). It must be of type `Table<Model>`. 

`Model` classes can exist without a `Table` class (as normal classes), but
`Table` classes are required if you want to keep your `Model` classes in
a database. `Table` classes are used to create tables (based on their name) 
and manage your `Model` objects (search, insert, update, delete, etc). 

Tables will be created during initialization (only once). In other words,
the table (in the database) won't be created until you initialize your `Table` class.
If the table already exists, it will compare your declaration in `Model` class against
your database. If there are changes, it will try to automatically update your table
(more on this below). 

Minimal representation:
```groovy
class Users extends Table<User> {}
```

You can implement your own methods to make things easier:
```groovy
class Users extends Table<User> {
    User findByEmail(String userEmail) {
        return findAll(email : userEmail)
    }
}
```

> NOTE: When implementing your own methods, you can use the [Fluid SQL Instructions](#fluid-query-instructions),
> however, field names should be converted into "database names", for example:

```groovy
class Reservation extends Model {
    @Column(primary = true, autoincrement = true)
    int id
    @Column(key = true)
    LocalDateTime dayTime
    @Column(key = true)
    User user
}
```
```groovy
class Reservations extends Table<Reservation> {
    List<Reservation> findByUser(User user) {
        return findAll(user_id : user.id)
    }
    List<Reservation> findByDay(LocalDate day) {
        return table.where("DATE(day_time) = ?", day.YMD).get()
    }
}
```

In the above example, when the `reservations` table is created, `User` field is translated into `user_id` column
(as well the foreign key). That is why, we use `user_id` instead of `user`.

In the same way, `dateTime` is translated into `date_time`. Inside `findByDay`, we are using the
[Fluid SQL Instructions](#fluid-query-instructions) in order to search by day.

> NOTE: You can always access the [Fluid SQL Instructions](#fluid-query-instructions) 
> using `table` or `getTable()` in your `Table` class, for example:

```groovy
// This will get only the reservation time for a given user:
int uid = 1
LocalTime time = reservations.table.field("dayTime").get(uid).toString().toDateTime().toLocalTime()
println time.HHmmss
```

I recommend you to keep a static instance of your `Table` classes, something like:
```groovy
class Instances {
  static Users users = new Users()
  static Reservations reservations = new Reservations()
}
```

> NOTE: If your `Model` class depends on other `Model` classes (e.g. `Reservation` Model contains a `User` Model field),
> those other classes tables must be initialized first (as it will create the foreign key constraint). 
> In this example, `Users` initialization must be before `Reservations`.

```groovy
// In other class:
import static com.example.myproject.Instances.*

class Printer {
  /**
   * It will print all reservations
   */
  void printReservations() {
     println reservations.all.sort { it.dayTime }.collect { String.format("%s : %s", it.dayTime.YMDHms, it.user.name) }.join(SysInfo.newLine)
  }
  /**
   * It will print all users (name and email)
   */
  void printUsers() {
    println users.all.collect { String.format("%d : %s <%s>", it.id, it.name, it.email) }.join(SysInfo.newLine)
  }
}
```

If you want to change the table declaration, you can use `@TableMeta`, for example:
```groovy
@TableMeta(name = 'my_table_name')
class Users extends Table<User> { /* ... */ }
```

By default, `Table` comes with many already implemented methods that you can use, 
for example (using `User` example):

```groovy
Users users = new Users()
User user = users.get(100)
User userByName = users.find(name: "John")

List<User> userList = users.get(1..10)
List<User> allUsers = users.getAll()
List<User> someUsers = users.getAll(limit: 100, sort: "age")
List<User> findUsers = users.findAll("age", 20)

assert users.delete(user)
assert users.delete(100)
assert users.delete(1..10)
assert users.delete(active : false)

assert users.insert(user)
assert users.replace(user)
assert users.update(user)
assert users.count() > 0
assert users.count(active : true) > 0
```

When the table does not exist, it will be created automatically. If your `Model` version
changes (either increases or decreases), it will update the table automatically, but if you 
want to decide when to do it, you can turn it off by setting its `autoUpdate` property to 
`false` (also available through the `@TableMeta` annotation).

To update it on command, you can execute:

```groovy
// update now in specific order:
TableUpdater.update([
        new Table1(), 
        new Table2()
]) 
```

**IMPORTANT**

If you perform small changes in your `Model`, for example changing
a `smallint` column into `int`, adding indices, adding elements into an ENUM
or adding/dropping a column, most likely can be automatically updated.

However, if your data changed considerably, for example changing a column name,
changing a `TEXT` column for a `DECIMAL` (in which data conversion is required), etc,
you will need to check and fix the data by code.

If the `Table` auto-update feature fails, override the method `execOnUpdate` and
return `true` (see example below). Then, override the method `onUpdate`, and process
all the data returning it modified (this process is expensive and may take a long
time depending on your data).

```groovy
class MyTable extends Table<MyModel> {
    @Override
    List<Map> onUpdate(List<Map> records) {
            // Fix the old data...
            return records // new data to be inserted
    }
    
    @Override
    boolean execOnUpdate(DB oldTable, int dbVersion, int codeVersion) {
        // check the current data and decide if table requires to be manually updated, for example:
        Map firstRow = oldTable.limit(1).get()
        return firstRow.containsKey("old_column") // if true, `onUpdate` will be executed if exists (default : true)
    }
}
```

### Joining Tables / Foreign keys

For `one-to-many` and `many-to-one` relations, you can simply add a `Model` 
field, for example:

```groovy
class Group extends Model {
    @Column(primary = true)
    int id
    @Column
    String name
}
```

```groovy
class User extends Model {
    @Column(primary = true, autoincrement = true)
    int id
    
    @Column
    String login
    
    @Column
    Group group
    /* ... */
}
```

This will create a foreign key. You can specify an action for `ondelete` inside `@Column` annotation.

There are some cases in which you don't need foreign keys and want to keep you code simple. For those scenarios,
you can use for example: `List<Model>`:

```groovy
class Group extends Model {
    @Column(primary = true, autoincrement = true)
    int id
    
    @Column
    String name
    
    @Column
    List<User> users = []
}
```

```groovy
class User extends Model {
    @Column(primary = true, autoincrement = true)
    int id
    
    @Column
    String login
    /* ... */
}
```

In the above example, the table `groups` will contain a column named `users`, in which all users ids are
store as an array of integers. You don't need to know that, as everytime you use `group.users` it
will contain a `List<User>`. By default, any `User` which doesn't exist anymore, will be omitted from the list
and will be removed from it when the Model is updated. If you want non-existing Models to be returned as `null`
you can specify `ondelete = NULL` (inside your `@Column` annotation). If you specify `ondelete = RESTRICT`,
only a warning will be printed each time a `Model` is `null`.

For `many-to-many` relations, you will need to add one extra `Model` class (using multiple columns as primary key)
as follows:

```groovy
class UserGroup extends Model {
    @Column(primary = true, ondelete = CASCADE)
    User user
    @Column(primary = true, ondelete = CASCADE)
    Group group
    @Column
    LocalDateTime updated
}
```

`uniqueGroup` is used to group several columns as unique constraint.
`ondelete` is to specify the action to perform when the resource is removed.

#### Other properties in `@Column` :

`type` : specify a different column type, for example: "DECIMAL(5,2)"
`columnDefinition` : specify a special column definition (full replacement).

#### Supported `@Column` types inside a `Model`:

* All primitives are supported (boolean is automatically converted depending on database support)
* `Model` classes
* `enum`
* `LocalTime`, `LocalDate` and `LocalDateTime`
* `Collection` (List, Set, Queue, etc), including `Collection<Model>`
* `Map` (HashMap, Properties, etc)
* `URI` and `URL`
* `InetAddress`, `Inet4Address`, `Inet6Address`

Any other class, will be converted using `toString()` / `(static) fromString()`.

### Cache

By default, table listings and columns are stored in cache to improve performance. You can disable it from
configuration:

```properties
db.cache=false
```

or programmatically:

```groovy
DB.enableCache = false
```

Additionally, you can clear cache at any time:

```groovy
DB.clearCache()
```

It is recommended to clear the cache if you add tables or modify their structure on runtime.

Select queries (get actions) are stored in cache if the following configuration is set (by default is disabled):
```properties
# Store queries for 1 hour
db.cache.get=3600
```
or programmatically:

```groovy
DB.cache = 3600
```

Note: None of the set action (insert, update, etc) are cached.

You can clear cache on any set action (automaticalyl) if you set (default is false):

```properties
db.cache.clear=true
```
or programmatically:

```groovy
DB.clearCache = true
```

### Adding support to other databases (Advanced)

Other than the [supported types](#comparison-with-libraries-with-similar-functionality), you can connect to any JDBC database
by providing a connection string (however Fluid SQL may not work as expected). 

#### Adding Fluid SQL support

In order to make your database to work with `Fluid SQL`, you need to extend `JDBC` (for standalone databases like `SQLite`) 
or `JDBCServer` (for databases which uses TCP services) classes:

```groovy
class SuperSQLServer extends JDBCServer {
    // The following fields are only default values
    // Use configuration file to set specific usage
    String dbname = ""
    String user = "root"
    String password = ""
    String hostname = "localhost"
    int port = 1111
    String driver = "com.supersql.server.jdb.connector"

    /**
     * Get connection string to your database
     * NOTE: user/password are handled separately on connection
     *       no need to include them in the Connection String
     * @return
     */
    @Override
    String getConnectionString() {
        // return your connection string here
        return "super://$hostname:$port/$dbname"
    }
}
```

Your class may need to override some other methods to work without issues. I recommend you to look at the source code for other 
classes extending `JDBCServer` for more detailed implementations.

One common method to override is `getTablesQuery` (to get the list of tables for that specific database). 

##### Troubleshooting

* If your class is unable to retrieve table columns, override the methods: `getSchemaSearchName`, `getCatalogSearchName` and `getTableSearchName`.
* If your database doesn't support `REPLACE`, override the method `getSupportsReplace` to return `false`
* If the `last inserted id` is not correct, override: `getLastIdQuery`

#### Adding `Model`/`Table` support

In order to use `Model` to interact with your database, your class will need to implement the `AutoJDBC` interface, and implement
the required code to create tables (with column definitions and version) and other methods. Be sure you execute the unit tests
to ensure the basic usage compatibility.

### Comparison with libraries with similar functionality

| Feature                   | ICL (this library)                                                                                                                             | Hibernate                                                                                                            | jOOQ (free)                                                                                                         |
|---------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| SQL Mode                  | Automatic / Fluid / Raw Query                                                                                                                  | Automatic / Raw Query                                                                                                | Fluid / Raw Query                                                                                                   |
| Supported Databases       | MySQL / MariaDB / Percona <br>PostgreSQL<br>Oracle<br>SQL Server<br>SQLite<br>Derby Apache (JavaDB)<br>HSQLDB (HyperSQL)<br>H2<br>Firebird SQL | MySQL / MariaDB<br>PostgreSQL<br>Oracle<br>SQL Server<br>Sybase SQL<br>Informix<br>FrontBase<br>HSQLDB<br>DB2/NT<br> | MySQL / MariaDB<br>PostgreSQL<br>SQLite<br>Firebird SQL<br>Derby Apache<br>H2<br>HSQLDB<br>YugabyteDB<br>Ignite<br> |
| Dependencies              | None (simple JDBC)                                                                                                                             | Spring Framework                                                                                                     | None (simple JDBC)                                                                                                  |                                                                                                        |  
| Clean Database identities | Yes                                                                                                                                            | No                                                                                                                   | Yes                                                                                                                 |
| SQL Injection protection  | Yes                                                                                                                                            | Yes                                                                                                                  | Yes                                                                                                                 |
| Complexity                | Low                                                                                                                                            | High                                                                                                                 | Medium                                                                                                              |

### Database comparison and feature support in this library

| Database        | Model | Fluid <br>SQL | TCP <br/>Server | Standalone | Embedded <br>in JAR | In<br>Memory | Native <br>Encryption | Performance * |
|-----------------|-------|---------------|-----------------|------------|---------------------|--------------|-----------------------|---------------|
| MySQL / MariaDB | Yes   | Yes           | Yes             | No         | No                  | No           | No                    | Fast          |
| PostgreSQL      | Yes   | Yes           | Yes             | No         | No                  | No           | No                    | Very Fast     |
| SQLite          | Yes   | Yes           | No              | Yes        | No                  | Yes          | No                    | Fast          |
| Derby           | Yes * | Yes           | Yes             | Yes        | Yes                 | Yes          | Yes                   | Very Fast     |
| HSQLDB          | No    | Yes           | Yes             | Yes        | No                  | No           | No                    | Very Fast     |
| H2              | No    | Yes           | Yes             | Yes        | No                  | No           | No                    | Very Fast     |
| Oracle          | No    | Yes           | Yes             | No         | No                  | No           | No                    | Slow          |
| SQLServer       | No    | Yes           | Yes             | No         | No                  | No           | No                    | Fast          |
| Firebird        | No    | Yes           | Yes             | No         | No                  | No           | No                    | Very Slow     |

> NOTE: As Derby doesn't allow to turn off `Foreign keys`, it can not be used together with `AutoUpdate`,
> which means, you have to decide if you will update manually your tables, or you want to use `Foreign keys`.

> NOTE: Performance was decided based on elapsed time to finish unit tests in the development environment. 
> It might not reflect the performance in a production environment.

## Looking for something else ?

These libraries provide only query execution to databases:

* Apache DbUtils
* Apache Spark SQL
* Fluent JDBC
* jasync-sql
* JDBI
* Jodd DbQuery
* rxjava2-jdbc
* R2DBC
* Sql2o
* Vert.x SQL clients
