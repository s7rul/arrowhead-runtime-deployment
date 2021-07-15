package eu.arrowhead.client.skeleton.provider.jarFileDeployer;

import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

public class JarDeploymentHandler {
    private String jarFilesDirectory;
    private Boolean isDeployed;
    private JarRunner deployment;

    private static List<JarDeploymentHandler> handlers = new LinkedList<>();

    public JarDeploymentHandler(String jarFilesDirectory) {
        this.isDeployed = false;
        this.jarFilesDirectory = jarFilesDirectory;

        File directory = new File(this.jarFilesDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        try {
            FileUtils.cleanDirectory(directory);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.handlers.add(this);
    }

    public synchronized void deploy(String base64JarFile) {
        if (isDeployed) {
            return;
        }
        this.isDeployed = true;
        File f = new File(this.jarFilesDirectory + File.separator + "translator.jar");
        try {
            byte[] byteJarFile = Base64.getDecoder().decode(base64JarFile);
            org.apache.commons.io.FileUtils.writeByteArrayToFile(f, byteJarFile);
            this.deployment = new JarRunner(this.jarFilesDirectory,
                    "/home/s7rul/tmp-log.log",
                    "translator.jar",
                    this);
            Thread thread = new Thread(this.deployment);
            thread.start();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private synchronized void stop() {
        this.deployment.stop();
    }

    public static synchronized void stopAll() {
        for (JarDeploymentHandler n: handlers) {
            n.stop();
        }
    }

    synchronized void stopped() {
        this.isDeployed = false;
    }
}