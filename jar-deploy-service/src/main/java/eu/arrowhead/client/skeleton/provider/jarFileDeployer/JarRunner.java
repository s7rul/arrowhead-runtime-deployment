package eu.arrowhead.client.skeleton.provider.jarFileDeployer;

import eu.arrowhead.client.skeleton.provider.ProviderApplicationInitListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.Jar;

import java.io.File;

public class JarRunner implements Runnable {
    private int id;
    private Boolean isRunning = false;
    private File workingDir;
    private File log;
    private String jarName;
    private Process proc;
    private JarDeploymentHandler handler;


    private final Logger logger = LogManager.getLogger(ProviderApplicationInitListener.class);

    public JarRunner(int id,String workingDir, String logPath, String jarName, JarDeploymentHandler handler) {
        this.id = id;
        this.workingDir = new File(workingDir);
        this.log = new File(logPath);
        this.jarName = jarName;
        this.proc = null;
        this.handler = handler;
    }

    @Override
    public void run() {
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", this.jarName);
        pb.directory(this.workingDir); // set working directory
        pb.redirectErrorStream(true);
        pb.redirectOutput(log); // set log file
        try {
            synchronized (this) {
                this.proc = pb.start();
                this.isRunning = true;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        try {
            this.proc.waitFor();
            logger.info("Jar stopped running.");
            synchronized (this) {
                this.proc = null;
                this.isRunning = false;
            }
            this.handler.stopped(this.id);
        } catch (InterruptedException e) {
            this.handler.stopped(this.id);
            e.printStackTrace();
        }
    }

    public synchronized Boolean isRunning() {
        return this.isRunning;
    }

    public synchronized void stop() {
        if (this.proc != null) {
            this.proc.destroy();
        }
    }

    public synchronized void forceStop() {
        if (this.proc != null) {
            this.proc.destroyForcibly();
        }
    }
}
