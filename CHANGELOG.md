# Main Changes

### 2.8.7
* Moved CV module to its [own project](https://gitlab.com/intellisrc/icv) (cv)
* Improved Table methods `getAll` (db)
* Improved performance on large datasets by getting data by chunks (db)
* Fixed : When using multiple databases JDBC, sometimes db type was mistaken (db)
* Improved `WebServer` : updated Spark version and added better SSL support (web)
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