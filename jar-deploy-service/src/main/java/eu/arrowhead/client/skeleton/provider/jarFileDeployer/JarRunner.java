package eu.arrowhead.client.skeleton.provider.jarFileDeployer;

import org.apache.tomcat.Jar;

import java.io.File;

public class JarRunner implements Runnable {
    File workingDir;
    File log;
    String jarName;
    Process proc;
    JarDeploymentHandler handler;

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
            synchronized (pb) {
                pb.wait();
                this.proc = null;
            }
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
