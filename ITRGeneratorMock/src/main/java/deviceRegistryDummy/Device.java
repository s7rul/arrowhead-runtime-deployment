package deviceRegistryDummy;

import GenerationFeasibilityTester.Pair;

import java.util.List;

public class Device {
	private Long deviceID;
	private String deviceType;
	private List<Pair<String, Integer>> metaData;
	
	public Device(Long deviceID, String deviceType, List<Pair<String, Integer>> metaData) {
		this.deviceID = deviceID;
		this.deviceType = deviceType;
		this.metaData = metaData;
	}
	
	public Long getDeviceID() {
		return this.deviceID;
	}

	public String getDeviceType() {
		return this.deviceType;
	}

	public List<Pair<String, Integer>> getMetaData() {
		return this.metaData;
	}

	@Override
	public String toString() {
		String ret = ("ID: " + this.deviceID.toString() + " Type: " + this.deviceType + "\n");
		ret += "meta data:\n";
		for(Pair<String, Integer> n: this.metaData) {
			ret += ("Key: " + n.key + "\t\tvalue: " + n.value + "\n");
		}
		return ret;
	}
}
 