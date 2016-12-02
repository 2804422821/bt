package bt.torrent.messaging;

import bt.BtException;
import bt.net.Peer;
import bt.protocol.Have;
import bt.protocol.Message;
import bt.protocol.Piece;
import bt.torrent.IPieceManager;
import bt.torrent.annotation.Consumes;
import bt.torrent.annotation.Produces;
import bt.torrent.data.BlockWrite;
import bt.torrent.data.DataWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

/**
 * Consumes blocks, received from the remote peer.
 *
 * @since 1.0
 */
public class PieceConsumer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PieceConsumer.class);

    private IPieceManager pieceManager;
    private DataWorker dataWorker;
    private ConcurrentLinkedQueue<BlockWrite> completedBlocks;

    public PieceConsumer(IPieceManager pieceManager, DataWorker dataWorker) {
        this.pieceManager = pieceManager;
        this.dataWorker = dataWorker;
        this.completedBlocks = new ConcurrentLinkedQueue<>();
    }

    @Consumes
    public void consume(Piece piece, MessageContext context) {
        Peer peer = context.getPeer();
        ConnectionState connectionState = context.getConnectionState();

        // check that this block was requested in the first place
        assertBlockIsExpected(peer, connectionState, piece);

        addBlock(peer, connectionState, piece).whenComplete((block, error) -> {
            if (error != null) {
                throw new RuntimeException("Failed to perform request to write block", error);
            } else if (block.getError().isPresent()) {
                throw new RuntimeException("Failed to perform request to write block", block.getError().get());
            }
            if (block.isRejected()) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Request to write block could not be completed: " + piece);
                }
            } else {
                block.getVerificationFuture().get().whenComplete((verified, error1) -> {
                    if (error1 != null) {
                        throw new RuntimeException("Failed to verify block", error1);
                    }
                    completedBlocks.add(block);
                });
            }
        });
    }

    private void assertBlockIsExpected(Peer peer, ConnectionState connectionState, Piece piece) {
        Object key = Mapper.mapper().buildKey(piece.getPieceIndex(), piece.getOffset(), piece.getBlock().length);
        if (!connectionState.getPendingRequests().remove(key)) {
            throw new BtException("Received unexpected block " + piece + " from peer: " + peer);
        }
    }

    private CompletableFuture<BlockWrite> addBlock(Peer peer, ConnectionState connectionState, Piece piece) {
        int pieceIndex = piece.getPieceIndex(),
                offset = piece.getOffset();

        byte[] block = piece.getBlock();

        connectionState.incrementDownloaded(block.length);

        CompletableFuture<BlockWrite> future = dataWorker.addBlock(peer, pieceIndex, offset, block);
        connectionState.getPendingWrites().put(
                Mapper.mapper().buildKey(pieceIndex, offset, block.length), future);
        return future;
    }

    @Produces
    public void produce(Consumer<Message> messageConsumer) {
        BlockWrite block;
        while ((block = completedBlocks.poll()) != null) {
            int pieceIndex = block.getPieceIndex();
            if (pieceManager.checkPieceVerified(pieceIndex)) {
                messageConsumer.accept(new Have(pieceIndex));
            }
        }
    }
}
