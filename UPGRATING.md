# Upgrading from 2.9 to 2.10

1. You will need to compile your project with Java 21+. 
2. use gradle 8+
3. WebSockets, and web services may need adjustments
4. Database drivers may need updated 
5. After 2.10.3, Derby,Firebird and HyperSQL are no longer supported. If you are using them, you can import the old classes into your project to make them work (may need a few modifications).

# Upgrading from 2.10.4 to 2.10.5 (Groovy 4 to Groovy 5)

1. Use Groovy 5+
2. When using File objects as boolean: if(file) { ... } now it also checks that the file exists (before it was only if file was null). Use this prompt to update:

> Based on Groovy 5 changes (via issue GROOVY-11996), review my Groovy codebase and refactor any conditional statements or boolean checks involving `java.io.File` or `java.nio.file.Path` objects (or inheritance):

> a. Identify instances where `if (myFile)`, `? :` ternary operators, or logical operations (`&&`, `||`) use a `File` or `Path` object directly.
> b. In Groovy 5, a non-existent file now evaluates to `false` (falsy). If the existing code intended to check strictly for **nullness** (i.e., whether the file reference was assigned), update it to an explicit null check: `file != null`.
> c. If the code intended to check if the file **actually exists** on disk, refactor it to use explicit existence checks (e.g., `file?.exists()` or `Files.exists(path)`) or keep the shorthand `if (file)` while adding a comment confirming the intentional Groovy 5 behavior.


