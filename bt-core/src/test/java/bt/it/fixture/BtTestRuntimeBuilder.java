package bt.it.fixture;

import bt.BtRuntime;
import bt.BtRuntimeBuilder;
import bt.service.INetworkService;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;

public class BtTestRuntimeBuilder {

    private InetAddress address;
    private int port;

    private BtRuntimeBuilder builder;
    private Collection<BtTestRuntimeFeature> features;

    protected BtTestRuntimeBuilder(InetAddress address, int port) {

        this.address = address;
        this.port = port;

        builder = BtRuntimeBuilder.builder();
        builder.adapter(binder -> {
            binder.bind(INetworkService.class).toInstance(new INetworkService() {
                @Override
                public InetAddress getInetAddress() {
                    return address;
                }

                @Override
                public int getPort() {
                    return port;
                }
            });
        });
    }

    public BtTestRuntimeBuilder feature(BtTestRuntimeFeature feature) {
        if (features == null) {
            features = new ArrayList<>();
        }
        features.add(feature);
        return this;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public BtRuntime build() {

        builder.shutdownService(BtTestRuntimeFactory.OnDemandShutdownService.class);

        if (features != null) {
            builder.adapter(binder -> {
                features.forEach(feature -> {
                    feature.contributeToRuntime(this, binder);
                });
            });
        }

        return builder.build();
    }
}
