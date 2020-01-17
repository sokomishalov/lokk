Lokk
========
~~Here should be some modern logo~~

[![Apache License 2](https://img.shields.io/badge/license-ASF2-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0.txt)
[![](https://jitpack.io/v/sokomishalov/lokk.svg)](https://jitpack.io/#sokomishalov/lokk)

## Overview
Kotlin/JVM coroutine-based distributed locks

## Distribution
Library with modules are available only from `jitpack` so far:
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

## Usage
Usage as simple as is - just instantiate required provider and use it like this:
```kotlin
val lokkProvider = /*...*/ 
lokkProvider.withLokk(atLeastFor = Duration.ofMinutes(10), atMostFor = Duration.ofHours(1)) {
    // do some magic
}
```

There are several jvm lokk implementations so far
- [lokk-redis-lettuce](#redis-with-reactive-Lettuce)
- [lokk-mongo-reactive-streams](#mongo-with-reactive-streams-driver)

## Redis with reactive Lettuce
Import a dep:
```xml
<dependency>
    <groupId>com.github.sokomishalov</groupId>
    <artifactId>lokk-redis-lettuce</artifactId>
    <version>${lokk.version}</version>
</dependency>
```
Then use [this implementation](./providers/redis/lokk-redis-lettuce/src/main/kotlin/ru/sokomishalov/lokk/provider/RedisLettuceLokkProvider.kt):
```kotlin
val lokkProvider = RedisLettuceLokkProvider(client = RedisClient.create())
```


## Mongo with reactive streams driver
Import a dep:
```xml
<dependency>
    <groupId>com.github.sokomishalov</groupId>
    <artifactId>lokk-mongo-reactive-streams</artifactId>
    <version>${lokk.version}</version>
</dependency>
```
Then use [this implementation](./providers/mongo/lokk-mongo-reactive-streams/src/main/kotlin/ru/sokomishalov/lokk/provider/MongoReactiveStreamsLokkProvider.kt):
```kotlin
val lokkProvider = MongoReactiveStreamsLokkProvider(client = MongoClients.create())
```