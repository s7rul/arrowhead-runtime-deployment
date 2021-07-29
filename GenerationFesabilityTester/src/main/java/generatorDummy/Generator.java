package generatorDummy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Generator {
    private class JarFileHolder {
        long consumerId;
        long providerSystemId;
        File jarFile;

        JarFileHolder(long consumerId, long providerSystemId, File jarFile) {
            this.consumerId = consumerId;
            this.providerSystemId = providerSystemId;
            this.jarFile = jarFile;
        }
    }

    private List<JarFileHolder> jarFiles = new ArrayList<>();

    public Generator() {
        this.jarFiles.add(new JarFileHolder(9L, 101L, new File("../ITR_8089.jar")));
        this.jarFiles.add(new JarFileHolder(9L, 102L, new File("../ITR_8090.jar")));
    }
}
