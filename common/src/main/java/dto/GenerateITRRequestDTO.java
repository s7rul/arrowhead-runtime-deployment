package dto;

public class GenerateITRRequestDTO {
    private final long providerServiceId;
    private final long consumerSystemId;

    public GenerateITRRequestDTO(long providerServiceId, long consumerSystemId) {
        this.providerServiceId = providerServiceId;
        this.consumerSystemId = consumerSystemId;
    }

    public long getConsumerSystemId() {
        return consumerSystemId;
    }

    public long getProviderServiceId() {
        return providerServiceId;
    }
}
