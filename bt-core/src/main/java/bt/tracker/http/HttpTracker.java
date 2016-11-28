package bt.tracker.http;

import bt.BtException;
import bt.metainfo.Torrent;
import bt.service.IdService;
import bt.service.INetworkService;
import bt.service.NetworkService;
import bt.tracker.SecretKey;
import bt.tracker.Tracker;
import bt.tracker.TrackerRequestBuilder;
import bt.tracker.TrackerResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Simple implementation of an HTTP tracker client.
 *
 * @since 1.0
 */
public class HttpTracker implements Tracker {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpTracker.class);

    enum TrackerRequestType {
        START, STOP, COMPLETE, QUERY
    }

    private URI baseUri;
    private IdService idService;
    private INetworkService networkService;
    private HttpClient httpClient;
    private CommonsHttpResponseHandler httpResponseHandler;

    private ConcurrentMap<URI, byte[]> trackerIds;

    /**
     * @param trackerUrl Tracker URL
     * @param idService Identity service
     * @since 1.0
     */
    public HttpTracker(String trackerUrl, IdService idService) {
        try {
            this.baseUri = new URI(trackerUrl);
        } catch (URISyntaxException e) {
            throw new BtException("Invalid URL: " + trackerUrl, e);
        }

        this.idService = idService;
        this.networkService = new NetworkService();
        this.httpClient = HttpClients.createMinimal();
        this.httpResponseHandler = new CommonsHttpResponseHandler(new bt.tracker.http.HttpResponseHandler());

        this.trackerIds = new ConcurrentHashMap<>();
    }

    @Override
    public TrackerRequestBuilder request(Torrent torrent) {
        return new TrackerRequestBuilder(torrent.getTorrentId()) {
            @Override
            public TrackerResponse start() {
                return sendEvent(TrackerRequestType.START, this);
            }

            @Override
            public TrackerResponse stop() {
                return sendEvent(TrackerRequestType.STOP, this);
            }

            @Override
            public TrackerResponse complete() {
                return sendEvent(TrackerRequestType.COMPLETE, this);
            }

            @Override
            public TrackerResponse query() {
                return sendEvent(TrackerRequestType.QUERY, this);
            }
        };
    }

    private TrackerResponse sendEvent(TrackerRequestType eventType, TrackerRequestBuilder requestBuilder) {

        String requestUri;
        try {
            String query = buildQuery(eventType, requestBuilder);

            String baseUrl = baseUri.toASCIIString();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            URL requestUrl = new URL(baseUrl + (baseUri.getRawQuery() == null? "?" : "&") + query);
            requestUri = requestUrl.toURI().toString();

        } catch (Exception e) {
            throw new BtException("Failed to build tracker request", e);
        }

        HttpGet request = new HttpGet(requestUri);
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Executing tracker HTTP request of type " + eventType.name() +
                        "; request URL: " + requestUri);
            }
            return httpClient.execute(request, httpResponseHandler);
        } catch (IOException e) {
            return TrackerResponse.exceptional(e);
        }
    }

    private String buildQuery(TrackerRequestType eventType, TrackerRequestBuilder requestBuilder) throws Exception {

        StringBuilder buf = new StringBuilder();

        buf.append("info_hash=");
        buf.append(urlEncode(requestBuilder.getTorrentId().getBytes()));

        buf.append("&peer_id=");
        buf.append(urlEncode(idService.getLocalPeerId().getBytes()));

        InetAddress inetAddress = networkService.getInetAddress();
        if (inetAddress != null) {
            buf.append("&ip=");
            buf.append(inetAddress.getHostAddress());
        }

        int port = networkService.getPort();
        buf.append("&port=");
        buf.append(port);

        buf.append("&uploaded=");
        buf.append(requestBuilder.getUploaded());

        buf.append("&downloaded=");
        buf.append(requestBuilder.getDownloaded());

        buf.append("&left=");
        buf.append(requestBuilder.getLeft());

        buf.append("&compact=1");
        buf.append("&numwant=50");

        Optional<SecretKey> secretKey = idService.getSecretKey();
        if (secretKey.isPresent()) {
            buf.append("&key=");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            secretKey.get().writeTo(bos);
            buf.append(urlEncode(bos.toByteArray()));
        }

        byte[] trackerId = trackerIds.get(baseUri);
        if (trackerId != null) {
            buf.append("&trackerid=");
            buf.append(urlEncode(trackerId));
        }

        switch (eventType) {
            case START: {
                buf.append("&event=started");
                break;
            }
            case STOP: {
                buf.append("&event=stopped");
                break;
            }
            case COMPLETE: {
                buf.append("&event=completed");
                break;
            }
            case QUERY: {
                // do not specify event
                break;
            }
            default: {
                throw new BtException("Unexpected event type: " + eventType.name().toLowerCase());
            }
        }

        return buf.toString();
    }

    private String urlEncode(byte[] bytes) {

        StringBuilder buf = new StringBuilder();
        for (byte b : bytes) {
            char c = (char) b;
            if   ( (c >= 48 && c <= 57) // 0-9
                || (c >= 65 && c <= 90) // A-Z
                || (c >= 97 && c <= 122) // a-z
                ||  c == 45  // -
                ||  c == 46  // .
                ||  c == 95  // _
                ||  c == 126 // ~
            ) {
                buf.append(c);
            } else {
                buf.append("%");
                String hex = Integer.toHexString(b & 0xFF).toUpperCase();
                if (hex.length() == 1) {
                    buf.append("0");
                }
                buf.append(hex);
            }
        }
        return buf.toString();
    }

}
