package eu.arrowhead.client.skeleton.provider;

public class LocalConstants {

    public static final String JAR_DEPLOY_URL = "/deploy_jar";
    public static final String JAR_DEPLOY_SERVICE_DEFINITION = "deploy_jar";
    public static final String JAR_FILES_DIR = "/home/s7rul/jar_files_tmp";

    public static final String INTERFACE_SECURE = "HTTP-SECURE-JSON";
    public static final String INTERFACE_INSECURE = "HTTP-INSECURE-JSON";
    //public static final String INTERFACE_INSECURE = "http";
    public static final String HTTP_METHOD = "http-method";

    private LocalConstants() {
        throw new UnsupportedOperationException();
    }
}
