package bt.torrent.data;

import bt.data.DataDescriptor;
import bt.data.digest.Digester;
import bt.service.IRuntimeLifecycleBinder;

/**
 *<p><b>Note that this class implements a service.
 * Hence, is not a part of the public API and is a subject to change.</b></p>
 */
public class DataWorkerFactory implements IDataWorkerFactory {

    private IRuntimeLifecycleBinder lifecycleBinder;
    private Digester digester;
    private int maxIOQueueSize;

    public DataWorkerFactory(IRuntimeLifecycleBinder lifecycleBinder, Digester digester, int maxIOQueueSize) {
        this.lifecycleBinder = lifecycleBinder;
        this.digester = digester;
        this.maxIOQueueSize = maxIOQueueSize;
    }

    @Override
    public DataWorker createWorker(DataDescriptor dataDescriptor) {
        return new DefaultDataWorker(lifecycleBinder, dataDescriptor, digester, maxIOQueueSize);
    }
}
