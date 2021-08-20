# DB Module

Manage databases, such as MySQL, SQLite, Postgresql. 
Create, store and perform CRUD operations to data without having 
to use SQL (a light-weight implementation as alternative to Hibernate).

There are mainly two ways to use this module:
1. Fluid query instructions
2. Model based operations

## Configuration

In order to connect to a database, we need to specify
the corresponding settings:

```properties
# file: config.properties

# Available types: mysql, sqlite, postgresql
db.type=mysql
db.name=users_db
db.user=myuser
db.pass=mypass

# Optional:
#db.host=10.0.0.20
#db.port=3306
```

Other than the supported types, you can connect to any JDBC database
by providing a connection string:

```groovy
class SQLServer extends JDBC {
    SQLServer() {
        super("jdbc:sqlserver://localhost;integratedSecurity=true")
    }
}
```

## Fluid Query Instructions

To connect and query a database is easy:

```groovy
DB db = Database.default.connect() // Get connection from pool
String passwordHash = db.table("users").field("password").get(userId).toString()
db.close() // Return connection to the pool
```

More examples are:

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
db.exec(new Query("SHOW TABLES")).toList().each {
    db.exec(new Query("DROP TABLE ?", it))
}
```

## Model Based Operations

Instead of (or additionally to) the Fluid Query Instructions, you can
use a `Model` object (with `Columns`) and a `Table` to perform operations
on it, similar to Hibernate and JPA (but much more simple).

The goal is to accomplish most of the common operations with a database
while keeping it as simple as possible without over-complicated 
instructions or implementation designs.

### Example:

The first step is to create your `Model` and `Table` classes:

#### Model class

A `Model` class is the data description of what we want to store in a table.
It must be of type: `Model<Table>`.

Each field that we want to create in the table, we need to annotate with 
`@Column` (see `Column` annotation for more details on how to use it).

```groovy
import javax.swing.table.TableColumn

enum MyColor {
    WHITE, RED, GREEN, BLUE, YELLOW, BLACK
}

class User extends Model<Users> {
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
class User extends Model<Users> { /* ... */ }
```

When declared version is higher than the one stored in the database, 
the table will be updated automatically (by default). You can turn off
that behaviour and execute the update on your terms.
You can read more about it next, as the update process is taken care by the `Table` class.

#### Table class

A `Table` class is what we are going to use to interact with the data (CRUD
operations, search data, etc). It must be of type `Table<Model>` and the
`Model` class must match the `Table` type. 

```groovy
class Users extends Table<User> {
    User findByEmail(String userEmail) {
        return findAll(email : userEmail)
    }
}
```

If you want to change the table declaration, you can use `@TableMeta`, for example:
```groovy
@TableMeta(name = 'my_table_name')
class Users extends Table<User> { /* ... */ }
```

By default, `Table` comes with
many already implemented methods that you can use, for example (using `User`
example):

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
```

When the table does not exist, it will be created automatically. If your `Model` version
changes, it will update the table automatically, but if you want to decide when to do it,
you can turn it off by setting its `autoUpdate` property to `false` (also available through
the `@TableMeta` annotation).

To update it on command, you can execute:

```groovy
// update now in specific order:
TableUpdater.update([
        new Table1(), 
        new Table2()
]) 
```
If you perform small changes in your `Model`, for example changing
a `smallint` column into `int`, adding indices, adding elements into an ENUM
or adding/dropping a column, most likely can be automatically updated.

However, if your data changed considerably, for example changing a column name,
changing a `TEXT` column for a `DECIMAL` (in which data conversion is required), etc,
you will need to check and fix the data by code.

In such cases, you can set the `onUpdate` interface with either a `RecordUpdater` or 
a `RecordsUpdater` (the latter is plural):

##### RecordsUpdater : all at once
```groovy
class MyTable extends Table<MyModel> {
    MyTable() {
        onUpdate = {
            List<Map> records ->
                // Fix the old data...
                return records // new data to be inserted
        }
    }
}
```
This option will try to insert all records at once. If it fails, it will
roll back the update process. 

##### RecordUpdater : one by one (the slowest method)
```groovy
class MyTable extends Table<MyModel> {
    MyTable() {
        onUpdate = {
            Map record ->
                // Fix the old row...
                return record // new row to be inserted
        }
    }
}
```
This option will try to insert all records one by one, but it won't
stop if one fails (it will only log a warning). 

As fixing the data this way will result in a lower performance, you can
decide when an update is needed by overriding the method `forceUpdate`:

```groovy
class MyTable extends Table<MyModel> {
    @Override
    boolean manualUpdate(DB oldTable) {
        // check the current data and decide if it requires to be updated, for example:
        Map firstRow = oldTable.limit(1).get()
        return firstRow.containsKey("old_column") // if true, `onUpdate` will be executed if exists (default : true)
    }
}
```

### Joining Tables

For `one-to-many` and `many-to-one` relations, you can simply add a `Model` 
field, for example:

```groovy
class Group extends Table<Groups> {
    @Column(primary = true)
    int id
    @Column
    String name
}
```

```groovy
class User extends Model<Users> {
    /* ... */
    @Column
    Group group = null
}
```

For `many-to-many` relations, you will need to add one extra `Model` class
as follows:

```groovy
class UserGroup extends Model<UserGroupRel> {
    @Column(nullable = false, uniqueGroup = "usrgrp", ondelete = CASCADE)
    User user
    @Column(nullable = false, uniqueGroup = "usrgrp", ondelete = CASCADE)
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

* All primitives are supported (boolean is converted to ENUM)
* `Model` classes
* `enum`
* `LocalTime`, `LocalDate` and `LocalDateTime`
* `Collection` (List, Set, Queue, etc)
* `Map` (HashMap, Properties, etc)
* `URI` and `URL`
* `InetAddress`, `Inet4Address`, `Inet6Address`

Any other class, will be converted using `toString()`.