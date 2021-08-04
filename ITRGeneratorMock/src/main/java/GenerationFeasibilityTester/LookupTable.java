package GenerationFeasibilityTester;

import java.util.Hashtable;

public class LookupTable {
	private Hashtable<String, DeviceType> table = new Hashtable<>();
	
	public void add(DeviceType input) {
		if (!table.containsKey(input.getName())) {
			this.table.put(input.getName(), input);
		} else {
			// Device already in table
			// TODO: Add error (learn java errors)
		}
	}
	
	public void remove(String name) {
		this.table.remove(name);
	}
	
	public DeviceType getDeviceTypeByName(String name) {
		return table.get(name);
	}

	public void populate() {
		DeviceType t1 = new DeviceType("laptop", DeviceType.Compatibility.YES);
		DeviceType t3 = new DeviceType("Nucleo-144", DeviceType.Compatibility.NO);
		DeviceType t2 = new DeviceType("raspberrypi", DeviceType.Compatibility.WITH_REQUIREMENT);
		Requirement r1 = new Requirement("model", Requirement.TypeEnum.EQUAL, 4);
		Requirement r2 = new Requirement("ram", Requirement.TypeEnum.EQUAL_OR_MORE, 4000000);
		t2.addRequirement(r1);
		t2.addRequirement(r2);

		DeviceType generic = new DeviceType("generic", DeviceType.Compatibility.WITH_REQUIREMENT);
		Requirement gr1 = new Requirement("ram", Requirement.TypeEnum.EQUAL_OR_MORE, 8000000);
		Requirement gr2 = new Requirement("cpu_speed", Requirement.TypeEnum.EQUAL_OR_MORE, 2500);
		generic.addRequirement(gr1);
		generic.addRequirement(gr2);

		this.add(t1);
		this.add(t2);
		this.add(t3);
		this.add(generic);
	}
	
	public static void main(String[] args) {
		// fast dirty test
		LookupTable tt = new LookupTable();
		
		DeviceType t1 = new DeviceType("test1", DeviceType.Compatibility.YES);
		DeviceType t2 = new DeviceType("test2", DeviceType.Compatibility.NO);
		DeviceType t3 = new DeviceType("test2", DeviceType.Compatibility.YES);
		DeviceType t4 = new DeviceType("test3", DeviceType.Compatibility.WITH_REQUIREMENT);
		Requirement r1 = new Requirement("ram", Requirement.TypeEnum.EQUAL_OR_MORE, 512);
		t4.addRequirement(r1);
		
		tt.add(t1);
		tt.add(t2);
		tt.add(t3);
		tt.add(t4);
		
		System.out.println("Test1 " + tt.getDeviceTypeByName("test1"));
		System.out.println("Test2 " + tt.getDeviceTypeByName("test2"));
		System.out.println("Test3 " + tt.getDeviceTypeByName("test3"));
	}
		
}

