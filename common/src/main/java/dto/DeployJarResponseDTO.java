package dto;

public class DeployJarResponseDTO {
    public enum Status {
        INITIAL_OK, FULL, CRASH_ON_START, PORT_COLLISION
    }

    private Status status;
    private Integer id;
    private Integer port;

    public DeployJarResponseDTO() {
    }

    public DeployJarResponseDTO(final Status status, final Integer id, final Integer port) {
        this.status = status;
        this.id = id;
        this.port = port;
    }

    public int getId() {
        return id;
    }

    public int getPort() {
        return port;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return ("Status: " + this.status.toString() + " ID: " + this.id.toString() + " Port: " + this.port.toString());
    }
}