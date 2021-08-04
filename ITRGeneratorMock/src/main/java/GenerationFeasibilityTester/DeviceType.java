package GenerationFeasibilityTester;

import java.util.LinkedList;
import java.util.List;

public class DeviceType {
	public enum Compatibility {
		YES,
		NO,
		WITH_REQUIREMENT
	}
	
	final private String name;
	final private List<Requirement> requirements;
	final private Compatibility compatible;
	
	public DeviceType(String name, Compatibility compatible) {
		this.name = name;
		this.compatible = compatible;
		this.requirements = new LinkedList<>();
	}
	
	public void addRequirement(Requirement x) {
		this.requirements.add(x);
	}
	
	public void removeRequirementByName(String name) {
		for (int i = 0; i < this.requirements.size(); i++) {
			if (this.requirements.get(i).name.equals(name)) {
				//noinspection SuspiciousListRemoveInLoop
				this.requirements.remove(i);
			}
		}
	}
	
	public List<Requirement> getRequirements() {
		return this.requirements;
	}
	
	public String getName() {
		return this.name;
	}
	
	public Compatibility getCompatibility() {
		return this.compatible;
	}
	
	public Boolean validate(List<Pair<String, Integer>> values) {
		switch (this.compatible) {
			case YES:
				return true;
			case NO:
				return false;
			case WITH_REQUIREMENT:
				return this.validateRequirements(values);
		}
		return false;
		// should never ever get here
	}
	
	private Boolean validateRequirements(List<Pair<String, Integer>> values) {
		Boolean noKey;
		for (Requirement n: this.getRequirements()) {
		    noKey = true;
			for (Pair<String, Integer> x: values) {
				if (x.key.equals(n.name)) {
					noKey = false;
					if(!n.validate(x.value)) {
						return false;
					} else {
						break;
					}
				}
			}

			if (noKey) {
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public String toString() {
		StringBuilder ret = new StringBuilder();
		ret.append("Name: ").append(this.name).append("\nCompatible: ").append(this.compatible.toString()).append("\nRequirements:\n");
		for (Requirement n: this.requirements) {
			ret.append(n.toString()).append("\n");
		}
		return ret.toString();
	}
	
	
	
	public static void main(String[] args) {
		// Small simple tests
		
		DeviceType device = new DeviceType("Test", Compatibility.WITH_REQUIREMENT);
		Requirement r1 = new Requirement("ram", Requirement.TypeEnum.EQUAL_OR_MORE, 2048);
		Requirement r2 = new Requirement("cpu_Hz", Requirement.TypeEnum.MORE_THEN, 1000);
		device.addRequirement(r1);
		device.addRequirement(r2);
		
		List<Requirement> rList = device.getRequirements();
		System.out.println("Printing out requirements");
		for (Requirement n: rList) {
			System.out.println(n.toString());
		}
		System.out.println("Removing r1");
		device.removeRequirementByName("ram");
		System.out.println("Printing out requirements");
		for (Requirement n: rList) {
			System.out.println(n.toString());
		}
	}
}