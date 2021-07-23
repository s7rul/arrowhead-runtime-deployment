package eu.arrowhead.client.skeleton.provider.jarFileDeployer;

import eu.arrowhead.client.skeleton.provider.ProviderApplicationInitListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tomcat.Jar;

import java.io.File;

public class JarRunner implements Runnable {
    File workingDir;
    File log;
    String jarName;
    Process proc;
    JarDeploymentHandler handler;

    private final Logger logger = LogManager.getLogger(ProviderApplicationInitListener.class);

    public JarRunner(String workingDir, String logPath, String jarName, JarDeploymentHandler handler) {
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
            this.proc = pb.start();
        } catch (Exception e) {
            System.out.println(e);
        }
        try {
            this.proc.waitFor();
            logger.info("Jar stopped running.");
            this.proc = null;
            this.handler.stopped();
        } catch (InterruptedException e) {
            this.handler.stopped();
            e.printStackTrace();
        }
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
