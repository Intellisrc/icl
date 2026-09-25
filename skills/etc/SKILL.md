---
name: etc
description: Use when a Groovy/Java/Kotlin project uses the intelliSource ICL common library (com.intellisrc) and needs in-memory cache with expiry (Cache, CacheObj), embedded key-value stores (BerkeleyDB, Redis/Jedis wrapper), JSON or YAML encode/decode, zip/gzip/brotli compression, hardware monitoring (CPU, memory, disk, GPU), MIME type detection, byte utilities, or self-documenting runtime configuration with persistence and web UI (AutoConfig, ConfigAuto). Triggers on com.intellisrc.etc imports in an ICL project. Included by db, net, serial and web modules.
---

# ICL etc Module

Extra utilities used by nearly every ICL project. Includes core. Bundles Jedis (`redis.clients:jedis`) and BerkeleyDB (`com.sleepycat:je`) transitively.

```groovy
implementation 'com.intellisrc:etc:2.10.5'   // + core + groovy
```

Package: `com.intellisrc.etc` (AutoConfig in `etc.config`).

## Cache — thread-safe in-memory cache with expiry

```groovy
Cache<String> cache = new Cache<>(timeout: 300, extend: true)  // seconds; extend life on access
// timeout: Cache.FOREVER (-1, default) | Cache.DISABLED (0) | seconds

String page = cache.get("bbc", {          // 2nd arg = closure computed on miss
    "https://bbc.com".toURL().text
})
cache.set("key", value)                   // optional: set(key, value, onStore, time)
cache.del("key") ; cache.contains("key") ; cache.expired("key")
cache.clear("match") ; cache.clear(~/regex/) ; cache.clear()
cache.size() ; cache.keys() ; cache.values() ; cache.garbageCollect()

CacheObj.instance                          // ready-to-use shared singleton Cache<Object>
```

## BerkeleyDB — embedded key-value store (no server)

```groovy
BerkeleyDB db = new BerkeleyDB("main")            // .berkeley/ dir; or new BerkeleyDB(new File(path), prefix)
db.set("key", "value") ; db.set("key", bytes)     // String or byte[] values
String v = db.get("key", "default")
Optional<byte[]> b = db.getBytes("key")
db.exists("key") ; db.delete("key") ; db.keys ; db.size ; db.isEmpty()
db.sync() ; db.close() ; db.destroy()
```

Prefer over Redis when there is no server, or values are binary (e.g. encrypted). Thread-safe.

## Redis — Jedis wrapper

Reads `redis.host` (default localhost) and `redis.port` (default 6379) from config.

```groovy
Redis redis = new Redis("myprefix")   // prefixes isolate keys; new Redis() for none
redis.set("k", "v") ; redis.get("k", "default")
redis.set("list", ["a","b"]) ; redis.getList("list", [])
redis.set("map", [a:1])      ; redis.getMap("map", [:]) ; redis.getSet("s", [] as Set)
redis.incr("counter") ; redis.incrBy("counter", 10) ; redis.expire("k", 60) ; redis.ttl("k")
redis.lpush("q", x) ; redis.rpush("q", y) ; redis.lpop("q") ; redis.lrange("q", 0, -1)
redis.hset("h", "field", v) ; redis.hget("h", "field")
redis.exists("k") ; redis.delete("k") ; redis.keys ; redis.close()   // close == quit
```

On connection failure it logs the error and returns the default value instead of throwing.

## JSON / YAML

```groovy
String json = JSON.encode(obj)              // pretty: JSON.encode(obj, true); handles LocalDateTime etc.
Map data    = JSON.decode(json)             // editable Map/List
Map lazy    = JSON.decodeLazy(bigJson)      // read-only LazyMap; largeSize: decode(json, true)
String yml  = YAML.encode(obj)
Map cfg     = YAML.decode(yamlString)
```

## Zip — compression

```groovy
Zip.gzip(file) ; Zip.gunzip(file)                      // single files (.gz)
Zip.compressDir(srcDir, zipFile)                       // Windows-safe since 2.10.5
Zip.decompressZip(zipFile, destDir)
Zip.gzip(bytes) ; Zip.gunzip(bytes)                    // byte[] variants
Zip.brotliCompress(bytes) ; Zip.brotliDecode(bytes)    // needs jvmbrotli deps
Zip.deflate(bytes) ; Zip.inflate(bytes)
Zip.zip([name.txt: bytes]) ; Zip.unzip(inputStream)    // in-memory zip as Map
```

## AutoConfig / ConfigAuto — self-documenting runtime config

Annotate static fields; changes are detected, documented, persisted (BerkeleyDB by default) and can be exposed through a web UI (`web` module `AutoConfigService` at `/cfg`).

```groovy
import com.intellisrc.etc.config.AutoConfig

@AutoConfig(description = "Server Settings")
class Settings {
    @AutoConfig(description = "HTTP port", userFriendly = true)
    static int port = 8080
    @AutoConfig(key = "custom.key")                   // default key: field name lowercase
    static boolean verbose = false
}
```

Attributes: `prefix`, `key`, `description`, `export` (default true), `userFriendly` (editable via web). Supported types: primitives, byte[], enums, LocalTime/Date/DateTime, List/Set/Queue, Map, URI, URL, InetAddress, File.

```groovy
ConfigAuto cfg = new ConfigAuto("com.example")            // base package to scan
// or: new ConfigAuto("com.example", new BerkeleyDB("main", "config"))
cfg.update()                       // persist changed fields (or schedule ConfigAutoTask from thread module)
cfg.getCurrentValues() ; cfg.getInitialValues() ; cfg.getChanged()
cfg.importValues(new File("import.properties")) ; cfg.exportValues(new File("export.properties"))
cfg.close()                        // optional exportOnExit
```

Fields must be `static` and non-final.

## Hardware — monitoring (Linux-focused)

```groovy
Hardware.getCpuUsage { double pct -> }          // callback on change; also getCpuTemp (needs sensors)
Hardware.getMemoryUsage { } ; Hardware.getRuntimeMemoryUsage { }   // RAM / JVM heap %
Hardware.getHddSpace { } ; Hardware.getDriveSpace(rootFile) { } ; Hardware.getTmpSpace { }
Hardware.getGpuMem { } ; Hardware.getGpuTemp { }                   // need nvidia-smi
Hardware.getOpenFiles { } ; Hardware.getOpenFilesPct { }
Hardware.totalSpace ; Hardware.freeSpace ; Hardware.usedSpace       // root or per-File
Hardware.screenOn = false ; Hardware.disableKeyboard() ; Hardware.enablePointer()
```

Windows mostly returns 0/false. Values are tracked as `Metric`s (`Metric.get("cpu.usage").text`).

## Smaller utilities

- `Bytes`: `fromString/fromChars/toHex/fromHex/toString/toChars/concat` (UTF-8 byte[] conversions).
- `Pack`: big/little-endian conversions between byte[] and int/long.
- `Mime`: 100+ constants (`Mime.PNG`, `Mime.JSON`...); `getType(File|name|InputStream)`, `isImage/isVideo/isAudio/isText` predicates.
- `JarResource`: `getAsString(Class, path)`, `getAsBytes`, `getAsTempFile` for resources inside a JAR.
- `Calc`: `stdDeviation(values)`.

## Gotchas

- `CacheObj.instance` is JVM-global; create dedicated `Cache<T>` instances for isolation.
- BerkeleyDB default dir `.berkeley/` must be writable; call `close()` when done.
- `AutoConfig` + `ConfigAutoTask` (thread module) is the thread-safe alternative to writing `Config` from multiple threads.
- `Hardware` is Linux-oriented; on Windows expect zeros and warnings.
