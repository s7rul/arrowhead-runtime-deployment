package GenerationFeasibilityTester;

import deviceRegistryDummy.Device;
import deviceRegistryDummy.DeviceRegistry;

public class GenerationFeasibilityTester {

    final private LookupTable table;
    final private DeviceRegistry registry;
    private Boolean guessAllowed;

    public GenerationFeasibilityTester() {
        this.table = new LookupTable();
        this.registry = new DeviceRegistry();

        // Only for testing remove when integrating with the real device registry
        this.registry.populate();
        this.table.populate();

        this.guessAllowed = true;
    }

    public Boolean generationFeasibilityByDeviceID(Long deviceID) {
        // TODO: add proper error handling
        Device d = registry.getDeviceByID(deviceID);
        if (d == null) {
            return false;
        }

        DeviceType type = table.getDeviceTypeByName(d.getDeviceType());
        if (type == null) {
            if (guessAllowed) {
                DeviceType genericType = table.getDeviceTypeByName("generic");

                // check that generic device type exist
                if (!(genericType == null)) {
                    return genericType.validate(d.getMetaData());
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return type.validate(d.getMetaData());
        }
    }
}