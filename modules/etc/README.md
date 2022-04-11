# ETC Module (ICL.etc)

Extra functionality which is usually very useful in any project. 
For example, monitoring Hardware, compressing or decompressing data, 
store data in memory cache, manage system configuration in a 
multithreading safe environment (BerkeleyDB or Redis), simple 
operations with bytes, etc.

[JavaDoc](https://gl.githack.com/intellisrc/common/raw/master/modules/etc/docs/)

## Usage

Follow the instructions on the last published version in [maven repository](https://mvnrepository.com/artifact/com.intellisrc/etc)

## Cache

Storing objects in memory (thread safe) is as simple as:
```groovy
Cache<URL> urlCache = new Cache<URL>()
urlCache.set("google", "https://google.com".toURL())
// To Read:
URL url = urlCache.get("google")
```
Another useful way to use it is by setting a default value:
```groovy
Cache<String> pages = new Cache<String>(timeout : 600, extend: true)
String page = pages.get("bbc", "https://bbc.com".toUrl().text)
```
In the above case, if the requested resource is not found
will be stored, otherwise will be retrieved from memory 
(similar to a cache proxy server).

### Options:
* `timeout` : the object will expire in memory, and
it will be removed by the garbage collector. 
* `extend` : if the value is read, extend its life.
* `gcInterval` : How often to check for expired values

## CacheObj

This is a `Cache<Object>` implementation that you can use to
store any kind of object.

## AutoConfig

It would be cool if you could annotate a field and automatically
store it in a database and document it in a config file, right?

That is what `@AutoConfig` is about. For example:

```groovy
class Account {
	@AutoConfig
	static boolean internal = false
}
```
By doing so, `config.account.internal` key will store the boolean value in 
a BerkeleyDB or Redis database.
If you export the configuration (it can be done automatically), it
will generate something like:

```properties
##########################################
# (account) Account
##########################################
# (boolean)
# account.internal=false
```
With a little more effort:

```groovy
@AutoConfig(description = "Configuration for Customer's Accounts")
class Account {
    @AutConfig(description = "Internal Customer")
    static boolean internal = false
}
```
The configuration file now looks like:

```properties
############################################################
# (account) Account : Configuration for Customer's Accounts
############################################################
# (boolean) Internal Customer
# account.private=false
```
When the description is too long, you can write multiple lines, 
and they will be wrapped automatically.

```groovy
@AutoConfig(description = """
	Configuration for Customer's Accounts.
	Most of the Accounts are direct
	customers, but there are few of them
	which are from within the company.""")
```
```properties
##########################################################
# (account) Account : Configuration for Customer's Accounts
# Most of the Accounts are direct customers, but there
# are few of them which are from within the company.
##########################################################
```
By default, `@AutoConfig` will be imported on start and
exported on exit. Any changes in the values will be stored
directly on the database. 

**NOTE** : Only values different from the defined in the field will be stored 
(to keep database clean).

### Initialization

In order to use `@AutoConfig`, you need to initialize it,
for example:

```groovy
// Example using Redis as database:
ConfigAuto configAuto = new ConfigAuto(packageName, new Redis("config"))

// Example using BerkeleyDB as database: ("db" = database name)
ConfigAuto configAuto = new ConfigAuto(packageName, new BerkeleyDB("db","config")) 
```

The `packageName` is the package from which `ConfigAuto` will start 
looking for `@AutoConfig` annotations. The "config" value is the prefix used in
all the keys we are going to store.

### AutoConfig Options:

* `description` : Describe the field
* `key` : Use this as key instead of the auto-generated key
* `export` Export to configuration (if false, will not be imported from file)
* `userFriendly` : flag to specify which are expected to be changed by end-user (used by other classes)

### Supported types to use in `@AutoConfig`

(all types supported by `PropertiesRW` in `core` module)

* All primitives are supported 
* `byte[]`
* `enum` 
* `LocalTime`, `LocalDate` and `LocalDateTime`
* `Collection` (`List`, `Set`, `Queue`, ...)
* `Map` (`HashMap`, `Properties`, ...)
* `URI` and `URL`
* `InetAddress`, `Inet4Address`, `Inet6Address`
* `File`
* `Field`

### Settings

There are some default settings that you can change:

```properties
# file : config.properties

# Name of file to export settings to:
config.auto.file=system.properties
# Wrap text on documentation to N chars:
config.auto.width=75
# Import properties file on start:
config.auto.import=true
# Export to file on update (not recommended)
config.auto.export=false
# Remove missing keys automatically
config.auto.remove=true
# keys to ignore (do not remove automatically if missing)
config.auto.ignore=[]
```

### Full integration

#### Interval update

For technical limitations, there is still no way to update automatically
the database when a `@AutoConfig` field is updated directly at runtime.
For that reason, we have prepared an implementation of a background
process that will check and update those values regularly. In order to 
use it, you will need to include the `thread` module to your dependencies
and add it as a `Task`:

```groovy
ConfigAuto configAuto = new ConfigAuto(packageName) //Using BerkeleyDB with default values
Tasks.add(new ConfigAutoTask(configAuto, 10000)) //Update every 10,000ms (default is 1000)
```
If you don't want to include the `thread` module dependency, you can call
`configAuto.update()` to push the changes into the database.

#### Web Service

If you are using a web service, you can also use an already implemented 
service to allow end-users to update `userFriendly` settings. In order
to use it, you need to include the `web` module to your dependencies (more
likely you are already using it), and add the service:

```groovy
ConfigAuto configAuto = new ConfigAuto(packageName) //Using BerkeleyDB with default values

WebServer webServer = new WebServer(port: 1111)
// The second argument is the path in which the service will listen, by default is 'cfg'
webServer.add(new AutoConfigService(configAuto, "api/v1"))

webServer.start()
```

That service provides ways to get all the `userFriendly` keys, and update them.
For example (following the previous code):

```bash
# List all available keys:
http://localhost:1111/api/v1

# Get value of key:
http://localhost:1111/api/v1/users.max

# Update several keys with values: [Method: PUT]
http://localhost:1111/api/v1

# Update key: [Method: GET]
http://localhost:1111/api/v1/users.max/6000

# Update key (List or Map): [Method: PUT]
http://localhost:1111/api/v1/list/users.emails
http://localhost:1111/api/v1/map/users.settings
```

## Redis

This class is a wrapper around `Jedis`. The main advantage is that this class supports
many class types (all supported by `PropertiesRW` in `core` module) and convert 
automatically lists and maps accordingly.

### Example

```groovy
Redis redis = new Redis()
redis.set("my.str", "Hello")
redis.set("my.dbl", 12.3334d)
redis.set("my.list", (1..100))
redis.set("my.map", [ a : 1, b : 2, c : 3])
redis.set("my.url", new URL("..."))
redis.set("my.file", new File("..."))
redis.set("my.binary", "content".bytes)

String str  = redis.get("my.str")      //If not found, will return "" 
double dbl  = redis.get("my.dbl", 0d)  //Using default value
List list1  = redis.get("my.list", []) //Using default value
List list2  = redis.getList("my.list") //If not found, will return []
Map map     = redis.getMap("my.map")   //If not found, will return [:]
Optional<URL> url       = redis.getURL("my.url")
Optional<byte[]> bytes  = redis.getBytes("my.binary")

// Remove one key:
redis.delete("my.key")
// Remove all keys:
redis.clear()
// close:
redis.close()
```

## BerkeleyDB

The implementation of Redis and BerkeleyDB are very close. One 
exception is that when you create an instance, you need to specify the database name:

```groovy
BerkeleyDB db = new BerkeleyDB("mydb")
// The rest is the same as in the above examples with redis
```

For those not familiar with BerkeleyDB, the main difference between them are summarized
in this table:

| Database   | Performance | Requires Server                    | Store data as | Migration     | Native support for lists and hashes | Command line client
|------------|-------------|------------------------------------|---------------|---------------|-------------------------------------|---------------------   
| Redis      | very good   | Yes (installed separately)         | String        | Export/Import | Yes                                 | Yes
| BerkeleyDB | good        | No (nothing needs to be installed) | Binary        | File copy     | No                                  | No

Both are multithreading safe (many clients at the same time), so you don't need to care about locking, 
etc. Also, both keep data in memory and are NO-SQL databases.

In my opinion, if you want simplicity, use BerkeleyDB. If you need better performance, use Redis. 
Also, if your project requires storing data in binary format (like encrypted data), use BerkeleyDB
as it additionally have methods to directly store and retrieve key/values as binary, for example:

```groovy
BerkeleyDB db = new BerkeleyDB("mydb")
byte[] key = "encrypted key".bytes
byte[] value = "encrypted data".bytes
db.set(key, value)
byte[] readVal = db.get(key)
// get all data as bytes:
Map<byte[], byte[]> data = db.getAllByteMap()

// close:
db.close()
```

## Zip

This class provide implementations to create and decode Zip files and compress and decompress files and data with GZip.

### Example

```groovy
Zip.gzip(file) //will create *.gz file
Zip.gunzip(gzFile) // decompress gz file

byte[] compressed = Zip.gzip(bytes) //compress byte[]
byte[] decompressed = Zip.gunzip(compressed)

compressDir(/* File */ srcDir, /* File */ zipFile)
decompressZip(/* File */ zipFile, /* File */ dstDir)
```

## Hardware

Monitor hardware and report changes on it:

### Example

```groovy
Hardware.getMemoryUsage({
    double val ->
        Log.i("Memory usage: %.2f", val)
})
```
This is the list of methods you can use to get those stats:
* getCpuUsage
* getCpuTemp
* getMemoryUsage
* getRuntimeMemoryUsage
* getGpuMem
* getGpuTemp
* getHddSpace
* getTotalSpace
* getFreeSpace
* getUsedSpace
* getDriveSpace
* getTmpSpace
* getOpenFiles
* getOpenFilesPct

**NOTE** : For the moment, most of these only work under LINUX.

Additionally, it also provides ways to interact with the hardware, for
example:

```groovy
// Manage Display:
Log.i("Is display ON? %s", Hardware.screenOn ? "YES" : "NO")
Hardware.screenOn = true // Turn screen on/off

// Manage other input devices:
Hardware.enablePointer()
Hardware.disablePointer()
Hardware.enableKeyboard()
Hardware.disableKeyboard()
Hardware.enableInputDevice("...") //Other devices
```

## Mime

This class is to provide or guess the mime type of files, 
file extensions or streams:

```groovy
Mime.JPG             // Most common types are provided directly
Mime.getType(file)   // based on content or filename
Mime.getType("jpg")  // based on extension
Mime.getType(stream) // based on byte headers
```
You can add in your `config.properties` any mime type that
is not found or mistaken:

```properties
mime.cust=unknown/custom
```

If you use the `web` module, you can set the "content-type" header like this:

```groovy
new Service(
    path : "/pdf/:id",
    contentType : Mime.PDF,
    action : {
        Request request -> 
            int id = request.params("id") as int
            return File.get("private", "documents", id)
    }
)
```

## YAML / JSON

YAML wraps `snakeyaml` and JSON wraps `Groovy Json`.
Both classes are meant to be used almost in the same way:

```groovy
// YAML Example:
String yamlStr = YAML.encode(["a","b","c"])
List yamlList  = YAML.decode(yamlStr) as List

// JSON Example:
boolean pretty = true // Return a pretty formatted JSON string (otherwise, will be compacted)
String jsonStr = JSON.encode([a:1, b:2, c:3], pretty)
Map jsonMap    = JSON.decode(encoded) as Map
```

## Bytes

This class contains a rewrite of some methods without using String class.
The reason is that String class keep values in memory and thus poses a security risk according to some sources.
While all methods in this class exists natively in Groovy/Java, those require to change values to String, e.g:

```groovy
char[] password = new char[50]
/* ... */
bytes[] bytesPass = password.toString().getBytes()
```
This class solves that issue by not using `String` as middleman, e.g:
```groovy
char[] password = new char[50]
/* ... */
bytes[] bytesPass = Bytes.fromChars(password)
```

## Pack

Imported from: [org.bouncycastle.util](https://github.com/bcgit/bc-java/blob/master/core/src/main/java/org/bouncycastle/util/Pack.java),
it is a class composed of utility methods for converting byte arrays into ints and longs, and back again.
The reason it was included in this module is to provide that functionality without having
to include `BouncyCastle` dependency (as a complement of `Bytes` class).
