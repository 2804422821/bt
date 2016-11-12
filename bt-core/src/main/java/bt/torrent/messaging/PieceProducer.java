package bt.torrent.messaging;

import bt.BtException;
import bt.net.Peer;
import bt.protocol.InvalidMessageException;
import bt.protocol.Message;
import bt.protocol.Piece;
import bt.torrent.data.BlockRead;
import bt.torrent.data.IDataWorker;

import java.util.function.Consumer;

public class PieceProducer implements MessageProducer {

    private IDataWorker dataWorker;

    public PieceProducer(IDataWorker dataWorker) {
        this.dataWorker = dataWorker;
    }

    @Override
    public void produce(Peer peer, ConnectionState connectionState, Consumer<Message> messageConsumer) {
        BlockRead block;
        while ((block = dataWorker.getCompletedBlockRequest(peer)) != null) {
            try {
                messageConsumer.accept(new Piece(block.getPieceIndex(), block.getOffset(), block.getBlock()));
            } catch (InvalidMessageException e) {
                throw new BtException("Failed to send PIECE", e);
            }
        }
    }
}
