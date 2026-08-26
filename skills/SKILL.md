---
name: icl
description: Overview of the intelliSource Common Library (ICL, com.intellisrc), a Groovy-first modular library for backend services in Groovy/Java/Kotlin - configuration, logging, SQL and NoSQL databases, web services (Jetty), tasks/threads, networking, cryptography, terminal apps, serial ports and images. Use as the entry point when a project depends on any com.intellisrc artifact, to identify which ICL modules a project uses and route to the matching per-module skill (core, log, etc, db, net, serial, web, crypt, thread, term, img).
---

# ICL — intelliSource Common Library

ICL (`com.intellisrc:*`, version 2.10.x) is a modular library that wraps best-of-breed libraries (Jetty, BouncyCastle, Jedis, BerkeleyDB, JLine, JSSC...) behind small, consistent APIs, designed for backend services. Groovy-first but usable from Java/Kotlin. Requires Java 21+ and Groovy 5.x (version chosen by the client project).

**How to use these skills:** check the project's build file for `com.intellisrc:<module>` dependencies, then load the skill for each module in use. `core` is always in use (every module includes it). You do NOT need the library's own documentation or examples — each module skill contains the verified API.

```
dependencies {
    implementation 'com.intellisrc:web:2.10.5'   // module artifact
    implementation 'org.codehaus.groovy:groovy-all:5.0.8'  // required by every module
}
```

Maven: groupId `com.intellisrc`, artifactId = module name.

## Module map

| Skill | Artifact | For | Key classes |
|---|---|---|---|
| `core` | core | config, logging, entry points, commands, time | Config, Log, SysMain, SysService, Cmd, SysClock, Millis, Secs, AnsiColor |
| `log` | log | SLF4J provider behind Log: files, rotation, colors | FileLogger, PrintLogger, StringLogger, CommonLogger |
| `etc` | etc | cache, NoSQL, JSON/YAML, zip, hardware, runtime config | Cache, BerkeleyDB, Redis, JSON, YAML, Zip, AutoConfig, Hardware |
| `db` | db | SQL: fluid query builder + ORM-lite | Database, DB, Data, Table, Model, View |
| `net` | net | SMTP email, ping/ports, TCP/UDP, FTP, subnet math | Smtp, Host, LocalHost, Network, TCPServer, FtpClient |
| `web` | web | HTTP/HTTPS REST + SSE + WebSocket services (Jetty 12) | WebService, Service, Serviciable*, WebClient, ServerSentEvent |
| `crypt` | crypt | hashing, passwords, AES/PGP, certs | Hash, PasswordHash, AES, PGP, LpCode, KeyStoreGenerator |
| `thread` | thread | managed tasks, priorities, timeouts, monitoring | Tasks, Task, IntervalTask, ServiceTask, ParallelTask |
| `term` | term | interactive terminal apps, tables, progress | Console, Consolable, TableMaker, Progress |
| `serial` | serial | serial ports (JSSC) | Serial, SerialDummy |
| `img` | img | image ops without OpenCV | BuffImgTools, FileImgTools, Converter, FrameShot, Metry |

Module includes-chain (transitive, no need to add twice):
`web` -> net -> crypt -> etc -> core; `db` -> etc -> core; `serial` -> etc -> core; `log`, `thread`, `term`, `img` -> core.

Extra client-side dependencies: JDBC drivers for `db` (sqlite-jdbc, mysql-connector-j, postgresql, h2, ojdbc, mssql-jdbc); optional Brotli natives for `web`. Everything else (Jetty, BouncyCastle, Jedis, BerkeleyDB, commons-net, mail, JLine, JSSC, Thumbnailator) is bundled.

## Cross-cutting conventions

- **config.properties** in the working directory is the configuration hub. Every module reads from it through `Config` / `Config.any` (env vars override: `UNDER_SCORE` maps to `dot.case`). Keys are prefixed by module (`log.`, `db.`, `web.`, `redis.`, `mail.smtp.`).
- **Logging** is always `Log.i("msg %s", arg)` (printf/SLF4J styles) — never System.out. Levels v/d/i/w/e.
- **Time**: use `Millis`/`Secs` constants, never magic numbers; get "now" from `SysClock` (testable), not `LocalDateTime.now()`.
- **Processes** run through the `Tasks` registry (thread module) — background, interval, delayed, parallel and service tasks with monitoring; `SysService` is the daemon entry point.
- **GroovyExtend** (`com.intellisrc:groovy-extend`, bundled with every module) adds the extension methods used throughout ICL examples, e.g. `"2000-01-01".toDateTime()`, `"0.0.0.0".toInet4Address()`, `File.get(path)`. Assume they exist when writing ICL code.
- **Errors**: constructors/methods that can fail at startup throw; runtime failures usually log and return defaults (e.g. Redis) — check per-module gotchas.

## Choosing modules for a task

- REST API / web server / SSE / WebSockets -> `web` (+ `db` for storage, `crypt` for certs/passwords)
- CLI tool with commands -> `term` (+ `core`), entry via `SysMain`
- Daemon/service -> `core` (SysService) + `thread` (ServiceTask), monitored with `etc` Hardware/AutoConfig
- Data pipeline -> `db` + `thread` (ParallelTask) + `etc` (Cache/Zip)
- Device integration -> `serial` or `net` (TCP/UDP)

When several apply, load all the relevant module skills before writing code.
