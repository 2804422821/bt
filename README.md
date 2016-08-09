[![Build Status](https://travis-ci.org/atomashpolskiy/bt.svg?branch=master)](https://travis-ci.org/atomashpolskiy/bt)

**Bt** aims to be the ultimate BitTorrent software created in Java. It offers good performance, reliability and is highly customizable. With Bt you can create a production-grade BitTorrent client in a matter of minutes. Bt is still in its' early days, but is actively developed and designed with stability and maintainability in mind.

## Quick Links

[Examples](https://github.com/atomashpolskiy/bt/tree/master/examples)

## What makes Bt stand out from the crowd

### Flexibility

Being built around the [Guice](https://github.com/google/guice) DI, **Bt** provides many options for tailoring the system for your specific needs. If something is a part of Bt, then it can be modified or substituted for your custom code.

### Custom backends

**Bt** is shipped with a standard file-system based backend (i.e. you can download the torrent file to a storage device). However, the backend details are abstracted from the message-level code. This means that you can use your own backend by providing a _data access_ implementation. E.g. use a libVLC wrapper and build a home-brewed stream media player.

### Protocol extensions

One notable customization scenario is extending the standard BitTorrent protocol with your own messages. BitTorrent's [BEP-10](http://www.bittorrent.org/beps/bep_0010.html) provides a native support for protocol extensions, and implementation of this standard is already included in **Bt**. Contribute your own _Messages_, byte manipulating _MessageHandlers_, message _consumers_ and _producers_; supply any additional info in _ExtendedHandshake_.

### Test infrastructure

To allow you test the changes that you've made to the core, **Bt** ships with a specialized framework for integration tests. Create an arbitrary-sized _swarm_ of peers inside a simple _JUnit_ test, set the number of seeders and leechers and start a real torrent session on your localhost. E.g. create one seeder and many leechers to stress test the network overhead; use a really large file and multiple peers to stress test your newest laptop's expensive SSD storage; or just launch the whole swarm in _no-files_ mode and test your protocol extensions.

### Parallel downloads

**Bt** has out-of-the-box support for multiple simultaneous torrent sessions with minimal system overhead. 1% CPU and 32M of RAM should be enough for everyone!

### Java 8 CompletableFuture

Client API leverages the asynchronous `java.util.concurrent.CompletableFuture` to provide the most natural way for co-ordinating multiple torrent sessions. E.g. use `CompletableFuture.allOf(client1.startAsync(...), client2.startAsync(...), ...).join()`. Or create a more sophisticated processing pipeline.

### And much more...

* Client's Peer ID customization
* YAML schemas for all bencoded objects: metainfo files, messages, tracker responses, etc. -- with support for automatic validation
* Torrent session's state with some useful runtime info, accessible via scheduled listener callbacks
* IPv6 support
* ...
* _**check it out yourself!**_

## List of supported BEPs

* BEP-3: The BitTorrent Protocol Specification
* BEP-10: Extension Protocol
* BEP-11: Peer Exchange (PEX)
* BEP-20: Peer ID Conventions
* BEP-23: Tracker Returns Compact Peer Lists

## A really ascetic code sample

```java
public static void main(String[] args) {

  // get metainfo file url and download location from the program arguments
  URL metainfoUrl = ...;
  File targetDirectory = ...;

  // create default shared runtime without extensions
  BtRuntime runtime = BtRuntimeBuilder.defaultRuntime();
  
  // create file system based backend for torrent contents
  DataAccessFactory daf = new FileSystemDataAccessFactory(targetDirectory);
  
  // create client
  BtClient client = Bt.client(runtime).url(metainfoUrl).build(daf);
  
  // launch
  client.startAsync(state -> {
      if (state.getPiecesRemaining() == 0) {
          client.stop();
      }
  }, 1000).join();
}
```

## Feedback

Any thoughts, ideas, criticism, etc. are welcome, as well as votes for new features and BEPs to be added. Please don't hesitate to post an issue or contact me personally!
