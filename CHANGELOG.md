# Main Changes

## 2.9 Main

### 2.9.0 (status : Stable)
* Web module no longer uses [Spark](https://sparkjava.com/). Now it uses [Jetty](https://www.eclipse.org/jetty/) directly (web) ** Breaking change **
* Updated Jetty to version 11 (web)
* Accept multiple resource directories (web)
* Added path policy (web)
* Added SSE (Server Send Events) (web)
* Added better error handling to SMTP (net)
* Added Triplet (core)
* Added FTPS support in FtpClient
* Replaced toMap() for toDB() (db)
* Improved Cache usability (etc)
* Improved output of Throwable (log)

 **Update Notes**: Changes in the `web` module may need to be manually fixed (like websockets usage, etc).

### 2.8.12 (branch, status : Stable)
* Updated Jetty to version 11. Implemented in Spark (web) ** Breaking change **
* Accept multiple resource directories. Implemented in Spark (web)

**Update Notes**: Most of the Jetty classes changed to jakarta package, you may need to modify their `import` statement.

### 2.8.11 (branch, status : Stable)
* DB module package changed (db) ** Breaking change **
* Added `unlimited` to `@Column` (db)
* Added support for `View` (db)
* Keep old log upon restart (log)
* Fixed issue #52 "The last packet successully received..." (db)
* Added "Accept-Ranges" header (web)
* Fixed connections no properly closed (db)
* Added Email.isValid (net)

**Update Notes**: Remove missing `import` packages (related to db module) and reimport them again to update their `import` statement. 

### 2.8.10
* Added stream and chunks support in `LpCode` (crypt)
* Added block export/import in `LpCode` (crypt)
* Improved ETag headers handling (web)
* Added support for Map in `TableMaker` (term)
* Added `Model`/`Table` support for `PostgreSQL` (db)
* Added `Fluid SQL` support for `H2` and `HSQLDB` (db)

### 2.8.9
* Added missing type in `Table` methods (db)
* Fixed output length when unicode characters were used in text responses (web)
* Fixed upload detection when using custom fields with multipart data (web)
* Fixed foreign keys references after update (db)
* Unsuccessful connections were stored in pool (db)
* Updated groovy-extends which includes new ceil and floor methods for Float, Double and BigDecimal
* Improved `LpCode` code. Now it supports many blocks and 2nd plane of Unicode (crypt)
* Allow rollback version in `Model` (db)
* Added count() and count(condition) in `Table` (db)
* Added Map constructor to `Data` (db)
* Derby no longer can automatically update and use foreign keys at the same time (db)
* Added `Database.waitForConnection` to wait for delayed database startup (db)
* `Network` class can now be used to store IP addresses (net)
* Many improvements to `Network` class (net)
* Removed `LocalHost.getLocalNetworkForIP` as it didn't belong there (net)
* Exposed `Cache.garbageCollect()` so it can be called on demand (etc)
* Added config hardware.warn to disable warning coming from `Hardware` (etc)
* Replaced List for Collection in most all public methods (all modules)

### 2.8.8
* Database bulk operations improved and fixed (db)
* Improved multiple column primary key support (db)
* Fixed issue with Kotlin and `id` in Model (db)
* Fixed issues with Derby and Firebird (db)
* Performance improved using cache for tables and columns (db)
* Fixed concurrency problem when using `Table` as static
* Added `Log.stackTrace()` (core)
* Added `Secs` similar to `Millis` (core)
* Slf4j updated (log)
* Better handling of unicode characters in `TableMaker` (term)

### 2.8.7
* Moved CV module to its [own project](https://gitlab.com/intellisrc/icv) (cv)
* Improved Table methods `getAll` (db)
* Improved performance on large datasets by getting data by chunks (db)
* Added automatic commit/rollback on bulk operations (db)
* Fixed : When using multiple databases JDBC, sometimes db type was mistaken (db)
* Improved `WebServer` HTTPS/WSS support (web)
* Added HTTP2 support or `WebServer` (web)
* Added regex support in paths (web)
* Improved `Console` by allowing to add a list of `Consolables` (term)

### 2.8.6
* Simplified use of `Model` : Changed `Model<Table>` to `Model` (db)
* Added support for SQLite and Derby for `Model` (Auto) (db)
* Added support for multiple-column primary keys (db)
* Improved support for NULL and Boolean in databases (db)
* Improved stability for many classes in `db` module
* Updated Spark version to fix vulnerabilities and add features (web)

### 2.8.5
* Improved WebSocket behaviour (web)
* Improved and fixed minor issues in `Table` (db)
* Fixed `Zip` when using subdirectories and added charset.  (etc)
* Improved Windows compatibility in `Hardware` (etc)
* Fixed `Cmd` when command contained spaces (core)
* Added `Cmd.succeed` (core)
* Improved `Mime` detection priority (etc)

### 2.8.4
* Moved `SysInfo.getFile` to `File.get()`
* `Cmd` recoded and added new features like reading output line by line
* Added methods to `AnsiColor`
* Added `TableMaker` into `term` module
* Added `Millis` class
* Added `Config.any` and `Config.env`
* Added `Host` and `LocalHost`
* Improved `Network` and `NetFace`
* Improved database classes

### 2.8.3
* Database module was improved
* Added support for Oracle, SQLServer, Derby and Firebird (Fluid SQL Builder)

### 2.8.2
* AutoConfig prefix is no longer "config" by default
* Fixed NULL issue in logs
* LpCode added to crypt module
* JDBC allows empty database name
* Add support for streams in Zip and SMTP
* Added limits to WebSocket

### 2.8.1
* JSON moved to `etc` package (and removed GSON dependency).
* Added YAML into `etc`.
* Simplified the use of JSON and YAML

## 2.8
* Groovy version is no longer enforced. Compiled with Groovy 3, but any version can theoretically be used.
* Some dependencies are no longer included by default (e.g: `db` drivers)
* Added package: `log`, which replaces `Log` class in core with SLF4J support
* Added @AutoConfig and related classes in `etc`
* Added Model and related classes to automatically create tables and provide easy access to databases.
* BerkeleyDB moved to `etc`
* Created `Redis` class
* Added support to upload multiples files in `web` and simplified its usage
* All Properties set/get got standardized and moved into `core`

## 2.7 (Groovy 2.5.6)

* Added modules `thread`, `serial`, `img`
* Added classes: core.SysClock, etc.Hardware, etc.Metric
* Improved compatibility with Java
* Log, WebServer were improved
* SysInfo tmpDir, homeDir, etc. are now File instead of String

## 2.6 (Groovy 2.5.6+, Java 11)

* Added module `cv`
* `tools` module renamed to `term`
* Implemented better cache management in WebService
    - added `maxAge`,`etag`,`isPrivate`,`noStore` into `Service`
    - added `Cache.FOREVER`
    - modified behavior of `cacheTime` when its zero (before was forever, now its off)
* Added support for log files with color. Add: `color=true` in config.properties
* Added `Progress` into `term`
* Added default values argument in `Config` and `Config.Props`

## 2.5 (Groovy 2.5.5)

* Added Config.Props to manage .properties files
* Added color.invert in Logs
* Added serial module

## 2.4
* Added BerkeleyDB
* Added Console
* Added UDP in `net`
* Added PGP in `crypt`
* Moved SysMain and SysService into `core`
* Added JarResource

## 2.3
* Replaced `Date` with `LocalDate*`

## 2.2 (Groovy 1.6.4+)

* Added export log to file
* Added SMTP class in `net`
* Added Log and Login services in `web`
* Added cache to WebService
* Improved Command and replaced by Cmd
* Automatic version detection
* Removed dependency on Android
* Added JSON class
* Added automatic ZIP compression in Logs