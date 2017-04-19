[![Build Status](https://travis-ci.org/atomashpolskiy/bt.svg?branch=master)](https://travis-ci.org/atomashpolskiy/bt) [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.atomashpolskiy/bt-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.atomashpolskiy/bt-core/) [![Javadoc](https://img.shields.io/badge/javadoc-latest-blue.svg)](http://atomashpolskiy.github.io/bt/javadoc/latest/) [![Join the chat at https://gitter.im/bt-java/general](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/bt-java/general?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

**Bt** is a lightweight framework for P2P-lovers and enthusiastic BitTorrent researchers, perfect choice for light enterprise and home usage and experimentation. It offers good performance, reliability and is highly customizable. With Bt you can create a production-grade BitTorrent client in a matter of minutes. Bt is still in its' early days, but is actively developed and designed with stability and maintainability in mind.

## Quick Links

[Website](http://atomashpolskiy.github.io/bt/)

[Introduction](http://atomashpolskiy.github.io/bt/intro/) (contains a brief overview of **Bt** design and components)

[JavaDoc](http://atomashpolskiy.github.io/bt/javadoc/latest/) (based on the latest commit in _**master**_)

[CLI Launcher](https://github.com/atomashpolskiy/bt/tree/master/bt-cli)

## Release 1.1

Release 1.1 includes a number of major performance and algorithmic improvements, critical bug fixes and API enhancements. Full list is available in [release notes](https://github.com/atomashpolskiy/bt/blob/master/RELEASE-NOTES.txt). **It's strongly recommended for all users to switch to 1.1.**

## Support for BEP-5: DHT Protocol

Available in [dht-experimental](https://github.com/atomashpolskiy/bt/tree/dht-experimental) branch. It's stable and already includes all changes from the 1.1 version.

## Building from source

1) Clone the git repo:
```
git clone https://github.com/atomashpolskiy/bt.git
```

2) Fetch and checkout dht-experimental (if you'd like to have DHT support)
```
git fetch
git checkout dht-experimental
```

3) Build
 ```
 mvn clean install -Plgpl -DskipTests=true
 ```
 
4) Download with CLI wrapper or use as a library
```
java -Xmx64m -jar bt-cli/target/bt-launcher.jar -f <path-to-torrent-file> -d <download-dir>
```

## Usage

Declare the following dependencies in your project’s **pom.xml**:

```xml
<dependency>
    <groupId>com.github.atomashpolskiy</groupId>
    <artifactId>bt-core</artifactId>
    <version>1.1</version>
</dependency>
<!-- for the sake of keeping the core with minimum number of 3-rd party 
     dependencies HTTP tracker support is shipped as a separate module;
     you may omit this dependency if only UDP trackers are going to be used -->
<dependency>
    <groupId>com.github.atomashpolskiy</groupId>
    <artifactId>bt-http-tracker-client</artifactId>
    <version>1.1</version>
</dependency>
<!-- bt-dht will be available if you've built the project manually 
     from dht-experimental branch-->
<dependency>
    <groupId>com.github.atomashpolskiy</groupId>
    <artifactId>bt-dht</artifactId>
    <version>1.2-SNAPSHOT</version>
</dependency>
```

## Code sample

```java
// enable multithreaded verification of torrent data
Config config = new Config() {
    @Override
    public int getNumOfHashingThreads() {
        return 8;
    }
};

// enable bootstrapping from public routers
Module dhtModule = new DHTModule(new DHTConfig() {
    @Override
    public boolean shouldUseRouterBootstrap() {
        return true;
    }
});

// get torrent file URL and download directory
URL torrentUrl = getTorrentUrl();
File targetDirectory = getTargetDirectory();

// create file system based backend for torrent data
Storage storage = new FileSystemStorage(targetDirectory);

// create client with a private runtime
BtClient client = Bt.client()
        .config(config)
        .storage(storage)
        .torrent(torrentUrl)
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

* _**check out [Release Notes](https://github.com/atomashpolskiy/bt/blob/master/RELEASE-NOTES.txt) for details!**_

## List of supported BEPs

* [BEP-3: The BitTorrent Protocol Specification](http://bittorrent.org/beps/bep_0003.html)
* [BEP-5: DHT Protocol](http://bittorrent.org/beps/bep_0005.html) (available in [dht-experimental](https://github.com/atomashpolskiy/bt/tree/dht-experimental) branch)
* [BEP-10: Extension Protocol](http://bittorrent.org/beps/bep_0010.html)
* [BEP-11: Peer Exchange (PEX)](http://bittorrent.org/beps/bep_0011.html)
* [BEP-12: Multitracker metadata extension](http://bittorrent.org/beps/bep_0012.html)
* [BEP-15: UDP Tracker Protocol](http://bittorrent.org/beps/bep_0015.html)
* [BEP-20: Peer ID Conventions](http://bittorrent.org/beps/bep_0020.html)
* [BEP-23: Tracker Returns Compact Peer Lists](http://bittorrent.org/beps/bep_0023.html)
* [BEP-27: Private Torrents](http://bittorrent.org/beps/bep_0027.html)
* [BEP-41: UDP Tracker Protocol Extensions](http://bittorrent.org/beps/bep_0041.html)

## Feedback

Any thoughts, ideas, criticism, etc. are welcome, as well as votes for new features and BEPs to be added. Please don't hesitate to post an issue or contact me personally!
