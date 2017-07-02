package bt.processor.torrent;

import bt.metainfo.Torrent;
import bt.processor.BaseProcessingStage;
import bt.processor.ProcessingStage;

public class FetchTorrentStage extends BaseProcessingStage<TorrentContext> {

    public FetchTorrentStage(ProcessingStage<TorrentContext> next) {
        super(next);
    }

    @Override
    protected void doExecute(TorrentContext context) {
        Torrent torrent = context.getTorrentSupplier().get();
        context.setTorrentId(torrent.getTorrentId());
        context.setTorrent(torrent);
    }
}
