package systemRegistryDummy;

import java.util.Hashtable;

public class SystemRegistry {
    private Hashtable<Long, Long> table = new Hashtable<>();

    public SystemRegistry() {
        this.table.put(9l, 2l); // consumer is on raspberry pi 4
        this.table.put(16l, 2l); // jar deployer on /\ (can shange look up)

        // core systems on laptop
        this.table.put(4l, 1l);
        this.table.put(5l, 1l);
        this.table.put(7l, 1l);
    }

    public Long getDeviceBySystemID(long systemID) {
        return this.table.get(systemID);
    }
}
