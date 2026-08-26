---
name: db
description: Use when a Groovy/Java/Kotlin project uses the intelliSource ICL common library (com.intellisrc) for SQL databases - connecting to SQLite, MySQL, MariaDB, PostgreSQL, H2, Oracle or SQL Server, the DB fluid SQL query builder, CRUD without SQL via Table/Model/View with @Column/@TableMeta/@ModelMeta annotations, automatic schema creation and updates, connection pooling (DBPool), or the Dummy connector for unit tests. Lightweight alternative to Hibernate. Triggers on com.intellisrc.db imports or Database/DB class usage in an ICL project.
---

# ICL db Module

SQL database access for ICL: a fluid query builder plus ORM-lite (auto schema + CRUD from annotated classes). Includes core and etc. JDBC drivers are NOT bundled — add the one you need:

```groovy
implementation 'com.intellisrc:db:2.10.5'
// pick drivers:
implementation 'org.xerial:sqlite-jdbc'        // SQLite
implementation 'com.mysql:mysql-connector-j'   // MySQL
implementation 'org.mariadb.jdbc:mariadb-java-client'
implementation 'org.postgresql:postgresql'
implementation 'com.h2database:h2'
implementation 'com.oracle.database.jdbc:ojdbc11'
implementation 'com.microsoft.sqlserver:mssql-jdbc'
```

Packages: `com.intellisrc.db` (main), `db.auto` (Table/Model/View), `db.annot` (annotations), `db.jdbc` (connectors). Derby, Firebird and HSQL support was dropped in 2.10.3.

## Connect

```groovy
import com.intellisrc.db.*
import com.intellisrc.db.jdbc.SQLite

// Typical: keep one Database (it pools), connect per unit of work
Database database = new Database(new SQLite(dbname: "app.db"))
DB db = database.connect()
// ... queries ...
db.close()                 // returns connection to the pool
database.quit()            // shuts the pool down on app exit

// From config.properties (db.type, db.host, db.dbname, db.user, db.password):
DB db = Database.default.connect()

// Server DBs:
new Database(new MySQL(dbname: "shop", host: "localhost", user: "root", password: "x"))
// also MariaDB, PostgreSQL, H2, Oracle, SQLServer

database.waitForConnection()      // block until DB is ready (containers, slow starts)
```

## DB — fluid query builder

```groovy
// SELECT
db.table("users").get()                                  // all rows
db.table("users").get(100)                               // by key
db.table("users").get([100, 200])                        // by keys
db.table("users").get(email: "a@b.com")                  // by column values
db.field("name").table("users").get()                    // single column; fields("a,b") / fields(["a","b"])
db.table("users").count() ; db.count("id").table("users")// also max/min/avg(col)
db.table("users").where("age > ?", 18).get()             // parameterized raw conditions
db.table("users").order("age", Query.SortOrder.DESC).limit(10).get()
db.table("users").order([age: Query.SortOrder.DESC, name: Query.SortOrder.ASC]).limit(10, 20).get() // limit, offset
db.table("users").group("department").get()

// WRITE
db.table("users").insert(name: "John", age: 30)          // Map (or List of Maps for batch)
db.table("users").update([age: 31], 100)                 // by key (or key list)
db.table("users").delete(100)                            // by key, key list, or Map conditions: delete(age: 30)
db.table("users").replace(id: 100, name: "John")         // upsert
db.key("id")                                             // hint the builder for get/update/delete by key

// RAW SQL when the builder is not enough (joins, complex queries)
db.getSQL("SELECT u.name FROM users u JOIN orders o ON u.id = o.user_id WHERE o.total > ?", 100)
db.setSQL("UPDATE users SET active = ? WHERE id = ?", true, 5)
db.lastID()                                              // last autoincrement id
```

## Data — result access

```groovy
Data r = db.table("users").where("id = ?", 5).get()
r.toMap()          // first row as Map          r.toListMap()  // all rows as List<Map>
r.toList()         // first column as List      r.toString() / toInt() / toLong() / toBool()  // single value
r.isEmpty() ; r.hasValue()
for (row in db.table("users").get()) { println row["name"] }   // iterable; row["col"]
```

## Table / Model — CRUD without SQL

```groovy
import com.intellisrc.db.auto.*
import com.intellisrc.db.annot.*

@ModelMeta(version = 2)                    // bump version to trigger schema update
class User extends Model {
    @Column(primary = true, autoincrement = true) int id
    @Column(nullable = false, length = 100)         String name = ""
    @Column(unique = true)                          String email
    @Column                                          int age = 0
    @Column                                          boolean active = true
}

@TableMeta(name = "users")                  // optional: engine, key, etc.
class Users extends Table<User> { }

Users users = new Users(new Database(new SQLite(dbname: "app.db")))  // creates/updates table on first use
int id  = users.insert(new User(name: "John", email: "j@x.com"))      // returns generated key
User u  = users.get(1)
List<User> all = users.getAll()                                       // getAll(limit: 10, sort: "age")
User byEmail = users.find(email: "j@x.com")       // first match (or null); find("email", value)
List<User> adults = users.findAll("age", 18)
u.age = 31 ; users.update(u)
users.delete(u) ; users.delete(1) ; users.delete(active: false)
users.count() ; users.count(age: 30)
users.clear(true)                                // drop all rows (forces FK off)
```

Relations — declare a Model as a `@Column` to get a foreign key; join tables use composite primaries:

```groovy
class Order extends Model {
    @Column(primary = true, autoincrement = true) int id
    @Column User user                       // creates user_id FK
}
class UserGroup extends Model {             // many-to-many
    @Column(primary = true, ondelete = DeleteActions.CASCADE) User user
    @Column(primary = true, ondelete = DeleteActions.CASCADE) Group group
}
```

Views: extend `View<Model>` with `@ViewMeta(name = "active_users")` and override `getCreateSQL()`.

## Pooling and testing

- `Database` already pools connections internally (PoolConnector). Tune with config: `db.timeout` (idle seconds, default 60) and `db.expire` (max connection life, default 600).
- Disable result caching if stale: `Config.set("db.cache", false)` or `DB.enableCache = false`.
- Unit tests: `new Database(new Dummy())` — a connector that logs queries instead of executing them.
- Transactions via raw SQL: `db.setSQL("BEGIN")` ... `db.setSQL("COMMIT")` (rollback on exception).

## Gotchas

- Always `db.close()` when done; a `Database` instance is meant to be shared, not per-request.
- First connection failure throws `DatabaseConnectionException`; later failures are handled by the error handler (`database.onError { }`).
- Fields not annotated with `@Column` are ignored (2.10.3+).
- Boolean storage differs per DB (PostgreSQL native, SQLite TEXT, MySQL configurable) — don't compare raw boolean columns across engines.
- SQLite locks the whole file on writes; keep transactions short.
- Autoincrement keys with value 0 are excluded from inserts.
- SQLite in-memory doesn't support Table auto-updates; use H2 there.
