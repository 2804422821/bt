package bt.it.fixture;

import bt.BtRuntime;
import bt.BtRuntimeBuilder;
import bt.service.INetworkService;
import bt.service.OnDemandShutdownService;

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

    public BtRuntime build() {

        builder.shutdownService(OnDemandShutdownService.class);

        if (features != null) {
            BtTestRuntimeConfiguration configuration = new BtTestRuntimeConfiguration() {
                @Override
                public InetAddress getAddress() {
                    return address;
                }

                @Override
                public int getPort() {
                    return port;
                }
            };
            features.forEach(feature -> {
                feature.contributeToRuntime(configuration, builder);
            });
        }

        return builder.build();
    }
}
