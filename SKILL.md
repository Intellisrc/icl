---
name: icl-library
description: Semantic router for ICL (IntelliSrc Common Library) - activate for tasks involving backend service development using Groovy/Java/Kotlin with the com.intellisrc.* modules. Triggers: configuration management (Config), database operations (DB, Database, Model), web services (WebService, Service), threading (Tasks, Task), cryptography (AES, PGP, PasswordHash), networking (Smtp, TCP/UDP clients), and terminal/console applications. Use when user mentions: "config", "database", "webservice", "encrypt", "hash", "smtp", "task", "thread", "log", or references com.intellisrc packages.
compatibility:
  - Java 21+ (version 2.10+)
  - Groovy 4.x
  - Kotlin compatible
  - Gradle 8.x build system
  - SLF4J logging implementation required
---

# ICL (IntelliSrc Common Library) Agent Skill

## Section 1: Clear Boundaries & Constraints

### What This Agent CANNOT Do

1. **Do NOT modify library source code without explicit user request** - This skill assists users in USING the library, not modifying it.
2. **Do NOT suggest alternative libraries** - When user is working with ICL, focus on ICL solutions first. Only mention alternatives if ICL cannot accomplish the task.
3. **Do NOT ignore version differences** - Version 2.9+ introduced breaking changes (Jetty migration to jakarta.*). Version 2.10+ requires Java 21. Always verify version context.
4. **Do NOT guess module dependencies** - Each module has specific dependencies. Check compatibility section before suggesting code.
5. **Do NOT use String for sensitive data** - Cryptography module uses char[] and byte[] for passwords/keys. Never convert to String.
6. **Do NOT ignore thread safety** - Config is NOT thread-safe. Use AutoConfig (from etc module) for concurrent access. Database connections are NOT thread-safe after pool return.
7. **Do NOT reuse closed DB connections** - After calling `db.close()`, always call `database.connect()` again. The connection is returned to pool and flagged as unusable.
8. **Do NOT skip database drivers** - Database drivers (MySQL, PostgreSQL, etc.) are NOT included. User must add them as dependencies.
9. **Do NOT mix up import statements** - Jetty classes moved to `jakarta.*` package in version 2.9+. Pre-2.9 uses `javax.*`.
10. **Do NOT create new Config instances for concurrent writes** - Use `AutoConfig` from etc module instead.

### Critical Environment Constraints

- **Java Version**: 2.10.x requires Java 21 (updated from Java 11)
- **Build Tool**: Gradle 8.x with Groovy 4.x compilation
- **Logging**: Requires SLF4J-compatible implementation
- **Database**: Requires separate JDBC driver dependencies
- **Configuration**: Default config file is `config.properties` in working directory

## Section 2: Structural Overview

### Module Architecture

```
com.intellisrc.*
├── core           (Config, Log, SysClock, Millis, Secs, Cmd, SysService)
├── log            (CommonLogger, PrintLogger, FileLogger - SLF4J integration)
├── etc            (AutoConfig, Cache, JSON, YAML, Redis, BerkeleyDB, Hardware)
├── db             (Database, DB, Model, Table, View, Query, DBPool)
├── web            (WebService, Service, Request, Response, WebSocketService, WebClient)
├── thread         (Task, IntervalTask, DelayedTask, ServiceTask, Tasks, TaskManager)
├── crypt          (AES, PGP, Hash, PasswordHash, Crypt)
├── net            (Smtp, Email, TCPClient/Server, UDPClient/Server, LocalHost)
├── term           (Console, Progress, TableMaker - JLine wrapper)
├── serial         (Serial, SerialReader - JSSC wrapper)
└── img            (BuffImgTools, FileImgTools, Converter, FrameShot)
```

### Core Classes Mental Map

**Configuration & Logging:**
- `Config` - Static access to config.properties (NOT thread-safe)
- `Config.Props` - Instance-based configuration (for custom files)
- `AutoConfig` - Thread-safe configuration with disk backup (etc module)
- `Log` - SLF4J logging wrapper

**Database (db module):**
- `Database` - Connection pool manager
- `DB` - Fluid SQL query builder
- `Model`/`Table`/`View` - Automatic CRUD with annotations
- `@TableMeta`/`@ModelMeta` - Configuration annotations

**Web Services (web module):**
- `WebService` - Main Jetty server class
- `Service` - Individual route definition
- `Request`/`Response` - HTTP request/response objects
- `WebSocketService` - WebSocket support
- `ServerSentEvent` - SSE support

**Threading (thread module):**
- `Tasks` - Task manager and reporter (entry point)
- `Task` - Base task class
- `IntervalTask` - Periodic execution
- `ServiceTask` - Long-running background tasks

**Cryptography (crypt module):**
- `AES` - Fixed-key encryption
- `PGP` - OpenPGP encryption
- `PasswordHash` - BCrypt/SCrypt/PBKDF2
- `Hash` - MD5, SHA variants

### Package Dependencies

- `core` - Base for all modules
- `log` - Extends core
- `etc` - Extends core
- `db` - Requires core, etc
- `web` - Requires core, etc, net
- `net` - Requires core, etc, crypt
- `thread` - Requires core
- `crypt` - Requires core, etc
- `term` - Requires core
- `serial` - Requires core, etc
- `img` - Requires core

## Section 3: Task-Oriented Workflows

### Configuration Management

**IF user needs to read configuration:**
```groovy
// Static access (default config.properties)
String value = Config.get("key", "defaultValue")
int number = Config.getInt("number", 10)
boolean flag = Config.getBool("flag", false)

// Environment variables
String envVar = Config.env.get("HOME")

// System properties
String javaHome = Config.system.get("java.home")
```

**IF user needs custom config file:**
```groovy
Config.Props custom = new Config.Props("myconfig.properties")
custom.set("key", "value")
custom.save() // Explicit save required
```

**IF user needs thread-safe configuration:**
```groovy
import com.intellisrc.etc.AutoConfig

AutoConfig config = new AutoConfig("myconfig")
config.set("key", "value") // Auto-saves, thread-safe
String value = config.get("key")
```

### Database Operations

**IF user needs to execute SQL:**
```groovy
Database database = new Database(new SQLite(dbname: "test.db"))
DB db = database.connect()

// Fluid query builder
String result = db
    .table("users")
    .field("name")
    .where("id = ?", userId)
    .get()
    .toString()

db.close()
database.close()
```

**IF user needs CRUD with Model:**
```groovy

class User extends Model {
    @Column(id = true)
    int id
    @Column
    String name
    @Column
    String email
}

class Users extends Table<Model> {
}

static Users users = new Users()

// Create
User user = new User(name: "John", email: "john@example.com")
user.id = users.insert()

// Read
User loaded = users.get(100) //ID

// Update
user.name = "Jane"
users.update(user)

// Delete
users.delete(user)
```

**IF user needs connection pool:**

Add in config.properties:

```properties
db.type=mariadb
#db.host=localhost
#db.port=
db.name=mydb
db.user=myuser
db.pass=secret
#db.timeout=600
#db.cache=false
#db.cache.get=3600
#db.cache.clear=true
```

or read environment variables:

```groovy
class Main extends SysService {
    static {
        // Transfer environment variables to config.properties:
        Set<String> dbKeys = Config.env.keys.findAll { it.startsWith("db.") }
        Log.i("DB Keys found: %s", dbKeys.toString())
        dbKeys.each {
            if(Config.env.exists(it)) {
                Config.set(it, Config.env.get(it))
            }
        }
        service = new Main()
    }
}

```

```groovy

DB db = database.connect()
// ... use db ...
db.close() // Returns to pool
database.close() // Closes all connections
```

**IMPORTANT:** Always check that database driver is in dependencies. Common drivers:
- MySQL: `mysql:mysql-connector-java`
- PostgreSQL: `org.postgresql:postgresql`
- SQLite: `org.xerial:sqlite-jdbc`
- MariaDB: `org.mariadb.jdbc:mariadb-java-client`

### Web Services

**IF user needs HTTP server:**
```groovy
WebService web = new WebService(
    port: 8080,
    resources: "res", // Static resource directory
    threads: 20,      // Thread pool size
    https: false      // Set true for HTTPS
)

// Simple service
web.add(new Service(
    path: "api/hello",
    action: { Request req, Response res ->
        return "Hello World"
    }
))

// Better way

class MyService extends ServiciableMultiple {
	@Override
    List<Service> getServices() {
		path: "example/",
		action: {
			// Do something
			return [] //return JSON
		}
	}8
}
web.add(new MyService())

web.start()
```

**IF user needs WebSocket:**
```groovy
class MyWebSocket extends WebSocketService {
    @Override
    void onMessage(String message) {
        send("Echo: " + message)
    }
}

web.add(new MyWebSocket(path: "ws"))
```

**IF user needs HTTP client:**
```groovy
WebClient client = new WebClient()
String response = client.get("https://api.example.com/data")
```

**IMPORTANT:** Version 2.9+ uses `jakarta.*` packages for Jetty. Pre-2.9 uses `javax.*`.

### Threading & Tasks

**IF user needs background task:**
```groovy
import com.intellisrc.thread.Tasks

Tasks.add({
    // Background work
    Log.i("Task running")
}, "TaskName")
```

**IF user needs periodic task:**
```groovy
IntervalTask task = IntervalTask.create({
    Log.i("Running every second")
}, "Monitor", Millis.SECOND, Millis.SECOND_10)
```

**IF user needs delayed task:**
```groovy
DelayedTask.create({
    Log.i("Runs once after 5 seconds")
}, "OneTime", Millis.SECOND_5)
```

**IF user needs long-running service:**
```groovy
ServiceTask.create({
    while(running) {
        // Service work
        Thread.sleep(1000)
    }
}, "ServiceName")
```

**IF user needs task report:**
```groovy
Tasks.report() // Print task status to console
Tasks.report("/path/to/report.json") // Save to file
```

### Cryptography

**IF user needs to hash password:**
```groovy
import com.intellisrc.crypt.PasswordHash

// BCrypt (default)
String hash = PasswordHash.hash("password")
boolean verified = PasswordHash.verify("password", hash)

// SCrypt
String scryptHash = PasswordHash.scrypt("password")
boolean scryptVerified = PasswordHash.verify("password", scryptHash)
```

**IF user needs AES encryption:**
```groovy
import com.intellisrc.crypt.AES

// Encryption
byte[] encrypted = AES.encrypt("plaintext".bytes, "16-byte-key-12345".bytes)

// Decryption
byte[] decrypted = AES.decrypt(encrypted, "16-byte-key-12345".bytes)
```

**IF user needs PGP encryption:**
```groovy
import com.intellisrc.crypt.PGP

byte[] encrypted = PGP.encrypt(data, publicKey)
byte[] decrypted = PGP.decrypt(encrypted, privateKey)
```

**IMPORTANT:** Use `char[]` and `byte[]` for passwords/keys. Never use String.

### Networking

**IF user needs to send email:**

Use config.properties to set SMTP constants (or read them from environment variables)

```groovy
import com.intellisrc.net.Smtp

Smtp smtp = new Smtp(
    host: "smtp.example.com",
    port: 587,
    user: "user@example.com",
    password: "password".toCharArray(), // char[] not String
    from: "sender@example.com"
)

smtp.send(
    to: ["recipient@example.com"],
    subject: "Test Email",
    body: "Email content",
    attachments: [new File("/path/to/file.pdf")]
)
```

**IF user needs TCP server:**
```groovy
import com.intellisrc.net.TCPServer

TCPServer server = new TCPServer(port: 9999)
server.onData = { data, client ->
    println "Received: ${new String(data)}"
    client.send("Response".bytes)
}
server.start()
```

### Terminal/Console

**IF user needs interactive console:**
```groovy
import com.intellisrc.term.Console

class MyApp implements Consolable {
    @Override
    void onCommand(String cmd) {
        switch(cmd) {
            case "help":
                println "Available commands: help, status, exit"
                break
            case "exit":
                System.exit(0)
                break
        }
    }
}

Console console = new Console(new MyApp())
console.start()
```

## Section 4: Failure Modes & Checkpoints

### Common Error Patterns

**Database Connection Errors:**
- **Error**: `ClassNotFoundException` for database driver
- **Fix**: Add appropriate JDBC driver to build.gradle
- **Prevention**: Always check module dependencies before using database

**Config Thread Safety Errors:**
- **Error**: ConcurrentModificationException or data corruption
- **Fix**: Use `AutoConfig` from etc module instead of `Config`
- **Prevention**: Never use static `Config` for concurrent writes

**DB Connection Reuse Errors:**
- **Error**: `SQLException: Connection closed` or similar
- **Fix**: Call `database.connect()` after `db.close()` - do NOT reuse closed connection
- **Prevention**: Always treat DB connection as single-use

**Jetty Import Errors (2.9+):**
- **Error**: `Package not found` for javax.servlet
- **Fix**: Change imports from `javax.*` to `jakarta.*`
- **Prevention**: Check version before suggesting imports

**Java Version Errors (2.10+):**
- **Error**: `Unsupported class file major version`
- **Fix**: Upgrade to Java 21
- **Prevention**: Verify Java version before using 2.10.x

**SLF4J Missing Errors:**
- **Error**: `No SLF4J implementation was found`
- **Fix**: Add SLF4J binding (e.g., logback-classic)
- **Prevention**: Include logging implementation in dependencies

### Checkpoint Checklist

Before suggesting code, verify:

1. **Version Context**: Which ICL version? (2.8.x vs 2.9.x vs 2.10.x)
2. **Java Version**: 2.10+ requires Java 21
3. **Module Dependencies**: Are required dependencies in build.gradle?
4. **Thread Safety**: Does this involve concurrent access? Use AutoConfig
5. **Security**: Are we handling passwords? Use char[], not String
6. **Connection Management**: Are DB connections properly closed?
7. **Import Packages**: Jetty uses jakarta.* in 2.9+
8. **Database Driver**: Is JDBC driver included separately?

### Recovery Procedures

**IF code fails with ClassNotFoundException:**
1. Identify missing dependency
2. Check if it's a database driver (add explicitly)
3. Check if it's a transitive dependency (add to build.gradle)

**IF database operations fail:**
1. Verify driver is present
2. Check connection parameters (host, port, credentials)
3. Verify database exists and is accessible
4. Check if connection was closed and needs reconnect

**IF web service fails to start:**
1. Check if port is already in use
2. Verify resources directory exists
3. Check jetty imports (javax vs jakarta)
4. Verify HTTPS configuration if using SSL

**IF configuration fails to load:**
1. Check if config.properties exists in working directory
2. Verify file permissions
3. For concurrent access, switch to AutoConfig

### Testing Requirements

When implementing ICL-based code, always suggest tests:

```groovy
// Database test with Dummy
import com.intellisrc.db.Dummy

def "Test DB operation"() {
    setup:
        Database database = new Database(new Dummy())
        DB db = database.connect()
    // ... test code ...
}

// Config test with temp file
def "Test Config"() {
    setup:
        File testCfg = new File(File.tempDir, "test.config")
        Config.Props cfg = new Config.Props(testCfg)
    // ... test code ...
    cleanup:
        testCfg.delete()
}
```

### Additional Resources

- Main Repository: https://gitlab.com/intellisrc/common/
- Issue Tracker: https://gitlab.com/intellisrc/common/-/work_items
- JavaDoc: https://intellisrc.gitlab.io/common/
- License: GPL v3.0

### Related Technologies

- **M2D2**: For web-based UI (mentioned in recommendations)
- **GroovyExtend**: Extended Groovy functionality (included in ICL)
- **Jetty 11/12**: Web server (wrapped by web module)
- **SLF4J**: Logging facade (wrapped by log module)
- **BouncyCastle**: Cryptography (wrapped by crypt module)
- **JLine**: Terminal/console (wrapped by term module)
