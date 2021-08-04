package dto;

public class GenerateITRResponseDTO {
    public enum Status {
        UNABLE_TO_GENERATE, // Generation not possible or not appropriate.
        GENERATION_DONE // The ITR is up and running and usable.
    }

    private final Status status;
    private final String addressITR;
    private final int portITR;
    private final String urlITR;

    public GenerateITRResponseDTO(Status status, String addressITR, int portITR, String urlITR) {
        this.status = status;
        this.addressITR = addressITR;
        this.portITR = portITR;
        this.urlITR = urlITR;
    }

    public Status getStatus() {
        return status;
    }

    public String getAddressITR() {
        return addressITR;
    }

    public int getPortITR() {
        return portITR;
    }

    public String getUrlITR() {
        return urlITR;
    }

}
