package eu.arrowhead.client.skeleton.provider.jarFileDeployer;

import dto.DeployJarResponseDTO;
import eu.arrowhead.client.skeleton.provider.ProviderApplicationInitListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private final Logger logger = LogManager.getLogger(ProviderApplicationInitListener.class);

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

    public DeployJarResponseDTO.Status deploy(String base64JarFile) {
        logger.info("Deploying jarfile.");
        if (isDeployed) {
            logger.info("jar deployment handler is full.");
            return DeployJarResponseDTO.Status.FULL;
        }
        this.isDeployed = true;
        File f = new File(this.jarFilesDirectory + File.separator + "translator.jar");

        this.deployment = new JarRunner(this.jarFilesDirectory,
                "/home/s7rul/tmp-log.log",
                "translator.jar",
                this);
        Thread thread = new Thread(this.deployment);

        logger.info("Runner and thread created.");

        try {
            byte[] byteJarFile = Base64.getDecoder().decode(base64JarFile);
            org.apache.commons.io.FileUtils.writeByteArrayToFile(f, byteJarFile);

            thread.start(); // the ITR watchdog  and runner is started here
            // If this thread is still running then the process is still running
            logger.info("Thread started.");
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
            logger.info("Something went wrong writing file or starting thread.");
            return DeployJarResponseDTO.Status.CRASH_ON_START;
        }

        try {
            logger.info("Waiting for 2 sec to see if ITR is still up.");
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        synchronized (this){
            if (this.isDeployed) {
                logger.info("It is still up all ok.");
                return DeployJarResponseDTO.Status.INITIAL_OK;
            } else {
                logger.info("It has terminated.");
                return DeployJarResponseDTO.Status.CRASH_ON_START;
            }
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

    void stopped() {
        synchronized (this) {
            this.isDeployed = false;
            logger.info("Runner signaled it stopped running.");
        }
    }
}