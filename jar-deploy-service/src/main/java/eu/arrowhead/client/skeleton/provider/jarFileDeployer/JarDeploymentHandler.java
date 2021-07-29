package eu.arrowhead.client.skeleton.provider.jarFileDeployer;

import dto.DeployJarResponseDTO;
import eu.arrowhead.client.skeleton.provider.ProviderApplicationInitListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.util.http.fileupload.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

public class JarDeploymentHandler {
    private static class RunnerHolder {
        int id;
        JarRunner runner;
        Thread thread;

        RunnerHolder(int id, JarRunner runner, Thread thread) {
            this.id = id;
            this.runner = runner;
            this.thread = thread;
        }
    }


    private String jarFilesDirectory;
    private int idCounter = 0;
    private List<RunnerHolder> runners = new LinkedList<>();

    private final Logger logger = LogManager.getLogger(ProviderApplicationInitListener.class);

    private static Integer noDeployed;

    public JarDeploymentHandler(String jarFilesDirectory) {
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
    }

    public DeployJarResponseDTO deploy(String base64JarFile, int port) {
        logger.info("Deploying jarfile.");

        Integer id = this.idCounter++;

        File f = new File(this.jarFilesDirectory + File.separator + ("translator"+id.toString()+".jar"));

        JarRunner runner = new JarRunner(id, this.jarFilesDirectory,
                "/home/s7rul/tmp-log.log",
                ("translator"+id.toString()+".jar"),
                this);
        Thread thread = new Thread(runner);
        this.runners.add(new RunnerHolder(id, runner, thread));

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
            return new DeployJarResponseDTO(DeployJarResponseDTO.Status.CRASH_ON_START, id, port);
        }

        try {
            logger.info("Waiting for 2 sec to see if ITR is still up.");
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (runner.isRunning()) {
            logger.info("It is still up all ok.");
            return new DeployJarResponseDTO(DeployJarResponseDTO.Status.INITIAL_OK, id, port);
        } else {
            logger.info("It has terminated.");
            return new DeployJarResponseDTO(DeployJarResponseDTO.Status.CRASH_ON_START, id, port);
        }
    }

    private synchronized void stop(int id) {
        for (RunnerHolder n: this.runners) {
            if (n.id == id) {
                n.runner.stop();
            }
        }
    }

    public synchronized void stopAll() {
        for (RunnerHolder n: this.runners) {
            n.runner.stop();
        }
    }

    void stopped(int id) {
        synchronized (this) {
            RunnerHolder holder = null;
            int ind = -1;
            for (int i = 0; i < this.runners.size(); i++) {
                RunnerHolder n = this.runners.get(i);
                if (n.id == id) {
                    holder = n;
                    ind = i;
                    break;
                }
            }
            if (holder != null) {
                holder.thread.interrupt();
                this.runners.remove(ind);
            }
            logger.info("Runner signaled it stopped running.");
        }
    }
}