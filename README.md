<h1 align="center">
    <a href="http://atomashpolskiy.github.io/bt/">Bt</a>
</h1>

<p align="center"><strong>
<sup>
A full-featured BitTorrent implementation in Java 8
<br/><a href="http://bittorrent.org/beps/bep_0011.html">peer exchange</a> | <a href="http://bittorrent.org/beps/bep_0009.html">magnet links</a> | <a href="http://bittorrent.org/beps/bep_0005.html">DHT</a> | <a href="http://wiki.vuze.com/w/Message_Stream_Encryption">encryption</a> | <a href="http://bittorrent.org/beps/bep_0027.html">private trackers</a> | <a href="http://bittorrent.org/beps/bep_0010.html">extended protocol</a>
</sup> 
</strong></p>

<p align="center">
    <img src="https://atomashpolskiy.github.io/static/img/bt-source-code.png" alt="Bt Example">
</p>

<p align="center">
    <img src="https://atomashpolskiy.github.io/static/img/bt-animated.gif" alt="Bt CLI">
</p>

<p align="center">
    <a href="https://travis-ci.org/atomashpolskiy/bt">
        <img src="https://img.shields.io/travis/atomashpolskiy/bt/master.svg?label=Linux%20build"
             alt="Linux build">
    </a>
    <a href="https://ci.appveyor.com/project/atomashpolskiy/bt">
        <img src="https://img.shields.io/appveyor/ci/atomashpolskiy/bt/master.svg?label=Windows%20build"
             alt="Linux build">
    </a>
    <a href="https://maven-badges.herokuapp.com/maven-central/com.github.atomashpolskiy/bt-core/">
        <img src="https://maven-badges.herokuapp.com/maven-central/com.github.atomashpolskiy/bt-core/badge.svg"
             alt="Maven Central">
    </a>
    <a href="http://atomashpolskiy.github.io/bt/javadoc/latest/">
        <img src="https://img.shields.io/badge/javadoc-latest-orange.svg"
             alt="JavaDoc">
    </a>
    <a href="https://codecov.io/gh/atomashpolskiy/bt">
        <img src="https://img.shields.io/codecov/c/github/atomashpolskiy/bt/master.svg"
             alt="Coverage">
    </a>
</p>

* **[CLI](https://github.com/atomashpolskiy/bt/tree/master/bt-cli)** – command-line downloader
* **[HOME](http://atomashpolskiy.github.io/bt/)** – website with documentation and tutorials
* **[FORUM](https://groups.google.com/forum/#!forum/bttorrent)** – Google group for support and feedback
* **[LICENSE](https://github.com/atomashpolskiy/bt/blob/master/LICENSE)** – licensed under Apache License 2.0

## Prerequisites

<sup>Currently, all peer connections are established via [encryption negotation protocol](http://wiki.vuze.com/w/Message_Stream_Encryption) (also called MSE handshake). Therefore, in order to be able to connect to peers you must install [Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html). The reason for this requirement is that the [MSE RC4 cipher](http://wiki.vuze.com/w/Message_Stream_Encryption) uses 160 bit keys, while default Java installation allows at most 128 bit keys.</sup>

## Usage

Most recent version available in Maven Central is **1.4**.

Declare the following dependencies in your project’s **pom.xml**:

```xml
<dependency>
    <groupId>com.github.atomashpolskiy</groupId>
    <artifactId>bt-core</artifactId>
    <version>${bt-version}</version>
</dependency>
<!-- for the sake of keeping the core with minimum number of 3-rd party 
     dependencies HTTP tracker support is shipped as a separate module;
     you may omit this dependency if only UDP trackers are going to be used -->
<dependency>
    <groupId>com.github.atomashpolskiy</groupId>
    <artifactId>bt-http-tracker-client</artifactId>
    <version>${bt-version}</version>
</dependency>
<dependency>
    <groupId>com.github.atomashpolskiy</groupId>
    <artifactId>bt-dht</artifactId>
    <version>${bt-version}</version>
</dependency>
```

## Building from source

```
git clone https://github.com/atomashpolskiy/bt.git
cd bt
mvn clean install -DskipTests
```

## Code sample

```java
// enable multithreaded verification of torrent data
Config config = new Config() {
    @Override
    public int getNumOfHashingThreads() {
        return Runtime.getRuntime().availableProcessors() * 2;
    }
};

// enable bootstrapping from public routers
Module dhtModule = new DHTModule(new DHTConfig() {
    @Override
    public boolean shouldUseRouterBootstrap() {
        return true;
    }
});

// get download directory
Path targetDirectory = new File("~/Downloads").toPath();

// create file system based backend for torrent data
Storage storage = new FileSystemStorage(targetDirectory);

// create client with a private runtime
BtClient client = Bt.client()
        .config(config)
        .storage(storage)
        .magnet("magnet:?xt=urn:btih:af0d9aa01a9ae123a73802cfa58ccaf355eb19f1")
        .autoLoadModules()
        .module(dhtModule)
        .build();

// launch
client.startAsync(state -> {
    if (state.getPiecesRemaining() == 0) {
        client.stop();
    }
}, 1000).join();
```

## What makes Bt stand out from the crowd

### Flexibility

Being built around the [Guice](https://github.com/google/guice) DI, **Bt** provides many options for tailoring the system for your specific needs. If something is a part of Bt, then it can be modified or substituted for your custom code.

### Custom backends

**Bt** is shipped with a standard file-system based backend (i.e. you can download the torrent file to a storage device). However, the backend details are abstracted from the message-level code. This means that you can use your own backend by providing a _storage unit_ implementation.

### Protocol extensions

One notable customization scenario is extending the standard BitTorrent protocol with your own messages. BitTorrent's [BEP-10](http://www.bittorrent.org/beps/bep_0010.html) provides a native support for protocol extensions, and implementation of this standard is already included in **Bt**. Contribute your own _Messages_, byte manipulating _MessageHandlers_, message _consumers_ and _producers_; supply any additional info in _ExtendedHandshake_.

### Test infrastructure

To allow you test the changes that you've made to the core, **Bt** ships with a specialized framework for integration tests. Create an arbitrary-sized _swarm_ of peers inside a simple _JUnit_ test, set the number of seeders and leechers and start a real torrent session on your localhost. E.g. create one seeder and many leechers to stress test the network overhead; use a really large file and multiple peers to stress test your newest laptop's expensive SSD storage; or just launch the whole swarm in _no-files_ mode and test your protocol extensions.

### Parallel downloads

**Bt** has out-of-the-box support for multiple simultaneous torrent sessions with minimal system overhead. 1% CPU and 32M of RAM should be enough for everyone!

### Java 8 CompletableFuture

Client API leverages the asynchronous `java.util.concurrent.CompletableFuture` to provide the most natural way for co-ordinating multiple torrent sessions. E.g. use `CompletableFuture.allOf(client1.startAsync(...), client2.startAsync(...), ...).join()`. Or create a more sophisticated processing pipeline.

### And much more...

* _**check out [Release Notes](https://github.com/atomashpolskiy/bt/blob/master/RELEASE-NOTES.md) for details!**_

## Support and feedback

Any thoughts, ideas, criticism, etc. are welcome, as well as votes for new features and BEPs to be added. You have the following options to share your ideas, receive help or report bugs:

* open a new [GitHub issue](https://github.com/atomashpolskiy/bt/issues)
* post your question on the [Bt forum](https://groups.google.com/forum/#!forum/bttorrent)

## List of supported BEPs

* [BEP-3: The BitTorrent Protocol Specification](http://bittorrent.org/beps/bep_0003.html)
* [BEP-5: DHT Protocol](http://bittorrent.org/beps/bep_0005.html)
* [BEP-9: Extension for Peers to Send Metadata Files](http://bittorrent.org/beps/bep_0009.html)
* [BEP-10: Extension Protocol](http://bittorrent.org/beps/bep_0010.html)
* [BEP-11: Peer Exchange (PEX)](http://bittorrent.org/beps/bep_0011.html)
* [BEP-12: Multitracker metadata extension](http://bittorrent.org/beps/bep_0012.html)
* [BEP-15: UDP Tracker Protocol](http://bittorrent.org/beps/bep_0015.html)
* [BEP-20: Peer ID Conventions](http://bittorrent.org/beps/bep_0020.html)
* [BEP-23: Tracker Returns Compact Peer Lists](http://bittorrent.org/beps/bep_0023.html)
* [BEP-27: Private Torrents](http://bittorrent.org/beps/bep_0027.html)
* [BEP-41: UDP Tracker Protocol Extensions](http://bittorrent.org/beps/bep_0041.html)
* [Message Stream Encryption](http://wiki.vuze.com/w/Message_Stream_Encryption)