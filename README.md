# ffastrings
f-fast ascii strings utilility (maybe not)

## Build instructions
0) Builds with Java 8 in Eclipse Luna or with Gradle 2.3 on Ubuntu 15.04, all others YMMV.

1) Install gradle 2.3, 2.4 or Eclipse with gradle plugins

2) On Linux CLI, cd into the project directory and execute ```gradle clean build fatJar```, and fini

3) In Eclipse, right-click on the project, ```Grade->Tasks Quick Launcher```, type in ```fatJar```, and fini.

## How to use it

*WARNING* this is not a clone of strings.  I have not compared the output in gory detail to make sure this utility outputs the same content (char for char) as Strings, nor have I sat down and desk checked the performance.  This utility will only recognize ASCII and WCHAR strings.  The ability to extend this is pretty easy by adding the recognition type.

f-fastrings is intended for systems where disk I/O is the precious and memory is plentiful.  The utility works by spliting up chunks of a *large* binary file and delegating string recognition to each thread.  Unlike strings, the entire file will be read into memory in large chunks (based on the number of threads), so that I/O cielings have a minimal impact on the analysis. 

Pretty easy to use.  The ```binaryFile``` is the target file, and then the output files can be specified with ```basicOutput``` or ```outputWithByteLength```.

```
java -jar build/libs/ffastrings-all-1.0.jar -help
usage: ffastrings
 -basicOutput <file>            print output the format: filename:
                                hex_offset data_string
 -binaryFile <file>             binary memory dump file
 -help                          print the help message
 -liveUpdate                    produce a live update on each hit
 -minStringLen <num_chars>      number (in hex) of characters required for
                                recognitions, default 4
 -numThreads <num_threads>      number of threads for scanning (specified
                                in hex), 1 is the default
 -outputWithByteLength <file>   print output the format: filename:
                                hex_offset hex_strlen_in_bytes data_string
 -startOffset <offset>          start at given offset (specified in hex),
                                0 is Default

```

Example output with outputWithByteLength:
```
file.dump: 1000069e0 1f Magrathea: Glacier signing keyw
file.dump: 100006c20 1b ~Module signature appended~
file.dump: 10000d016 c defer_create
file.dump: 10000d023 c defer_lookup
file.dump: 10000d070 12 WAIT_FOR_CLEARANCE
file.dump: 10000d0b0 c WAIT_FOR_CMD
file.dump: 10000d110 f WAIT_FOR_PARENT
file.dump: 10000d150 d WAIT_FOR_INIT
file.dump: 10000d190 b OBJECT_DEAD
file.dump: 10000d1d0 b DROP_OBJECT
```

Example output with basicOutput:
```
file.dump: 1000069e0 Magrathea: Glacier signing keyw
file.dump: 100006c20 ~Module signature appended~
file.dump: 10000d016 defer_create
file.dump: 10000d023 defer_lookup
file.dump: 10000d070 WAIT_FOR_CLEARANCE
file.dump: 10000d0b0 WAIT_FOR_CMD
file.dump: 10000d110 WAIT_FOR_PARENT
file.dump: 10000d150 WAIT_FOR_INIT
file.dump: 10000d190 OBJECT_DEAD
file.dump: 10000d1d0 DROP_OBJECT
```
