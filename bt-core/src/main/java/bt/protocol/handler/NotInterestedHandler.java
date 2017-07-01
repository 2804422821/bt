package bt.protocol.handler;

import bt.protocol.DecodingContext;
import bt.protocol.EncodingContext;
import bt.protocol.NotInterested;

import java.nio.ByteBuffer;

import static bt.protocol.Protocols.verifyPayloadHasLength;

public final class NotInterestedHandler extends UniqueMessageHandler<NotInterested> {

    public NotInterestedHandler() {
        super(NotInterested.class);
    }

    @Override
    public int doDecode(DecodingContext context, ByteBuffer buffer) {
        verifyPayloadHasLength(NotInterested.class, 0, buffer.remaining());
        context.setMessage(NotInterested.instance());
        return 0;
    }

    @Override
    public boolean doEncode(EncodingContext context, NotInterested message, ByteBuffer buffer) {
        return true;
    }
}
