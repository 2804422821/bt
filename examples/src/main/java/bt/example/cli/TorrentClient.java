package bt.example.cli;

import bt.Bt;
import bt.BtClient;
import bt.BtRuntime;
import bt.BtRuntimeBuilder;
import bt.data.DataAccessFactory;
import bt.data.file.FileSystemDataAccessFactory;
import bt.metainfo.Torrent;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import joptsimple.OptionException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class TorrentClient {

    public static void main(String[] args) throws IOException {

        Options options;
        try {
            options = Options.parse(args);
        } catch (OptionException e) {
            Options.printHelp(System.out);
            return;
        }

        DataAccessFactory dataAccess = new FileSystemDataAccessFactory(options.getTargetDirectory());

        BtRuntime runtime = BtRuntimeBuilder.defaultRuntime();
        BtClient client = Bt.client(runtime)
                .url(toUrl(options.getMetainfoFile()))
                .build(dataAccess);

        SessionStatePrinter printer = createPrinter(client.getSession().getTorrent());
        try {
            client.startAsync(state -> {
                printer.print(state);
                if (!options.shouldSeedAfterDownloaded() && state.getPiecesRemaining() == 0) {
                    client.stop();
                }
            }, 1000).thenRun(() -> System.exit(0));

        } catch (Exception e) {
            // in case the start request to the tracker fails
            printer.shutdown();
            printAndShutdown(e);
        }
    }

    private static URL toUrl(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Unexpected error", e);
        }
    }

    private static SessionStatePrinter createPrinter(Torrent torrent) {

        return new SessionStatePrinter(torrent) {

            private Thread t;

            {
                t = new Thread(() -> {
                    while (!isShutdown()) {
                        try {
                            KeyStroke keyStroke = readKeyInput();
                            if (keyStroke.isCtrlDown() && keyStroke.getKeyType() == KeyType.Character
                                    && keyStroke.getCharacter().equals('c')) {
                                shutdown();
                                System.exit(0);
                            }
                        } catch (Throwable e) {
                            e.printStackTrace(System.out);
                            System.out.flush();
                        }
                    }
                });
                t.setDaemon(true);
                t.start();

                Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
            }

            @Override
            public void shutdown() {
                if (!isShutdown()) {
                    super.shutdown();
                    t.interrupt();
                }
            }
        };
    }

    private static void printAndShutdown(Throwable e) {
        e.printStackTrace(System.out);
        System.out.flush();
        System.exit(1);
    }
}
