package dto;

public class DeployJarRequestDTO {
    private String file;
    private int port;

    public DeployJarRequestDTO(final String file, final int port) {
        this.file = file;
        this.port = port;
    }

    public String getFile() {
        return this.file;
    }
     public int getPort() {
        return this.port;
     }
}
