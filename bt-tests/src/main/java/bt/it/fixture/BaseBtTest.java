package bt.it.fixture;

import bt.metainfo.MetadataService;
import org.junit.After;
import org.junit.BeforeClass;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Collections;

/**
 * Base class for Bt integration tests.
 *
 * @since 1.0
 */
public class BaseBtTest {

    private static final File ROOT = new File("target/it");

    private static final String FILE_NAME = "file.txt";
    private static final URL FILE_URL = BaseBtTest.class.getResource(FILE_NAME);
    private static final URL METAINFO_URL = BaseBtTest.class.getResource(FILE_NAME + ".torrent");

    private static byte[] SINGLE_FILE_CONTENT;

    @BeforeClass
    public static void setUpClass() {
        try {
            File singleFile = new File(FILE_URL.toURI());
            byte[] content = new byte[(int) singleFile.length()];
            try (FileInputStream fin = new FileInputStream(singleFile)) {
                fin.read(content);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file: " + singleFile.getPath(), e);
            }
            SINGLE_FILE_CONTENT = content;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Unexpected error", e);
        }
    }

    @After
    public void tearDown() {
        try {
            deleteRecursive(getTestRoot());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteRecursive(File file) throws IOException {
        if (!file.exists()) {
            return;
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }

        Files.delete(file.toPath());
    }

    /**
     * Create a swarm builder.
     *
     * @return Swarm builder
     * @since 1.0
     */
    protected SwarmBuilder buildSwarm() {
        SwarmBuilder builder = new SwarmBuilder(getTestRoot(), getSingleFile());
        builder.module(new TestExecutorModule());
        builder.torrentSupplier(() -> new MetadataService().fromUrl(METAINFO_URL));
        return builder;
    }

    private File getTestRoot() {
        return new File(ROOT, this.getClass().getName());
    }

    private static TorrentFiles getSingleFile() {
        return new TorrentFiles(Collections.singletonMap(FILE_NAME, SINGLE_FILE_CONTENT));
    }
}
