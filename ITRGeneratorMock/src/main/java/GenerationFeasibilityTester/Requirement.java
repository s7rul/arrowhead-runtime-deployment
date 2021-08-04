package GenerationFeasibilityTester;



public class Requirement {
	public enum TypeEnum{
		EQUAL, MORE_THEN, LESS_THEN, EQUAL_OR_LESS, EQUAL_OR_MORE
	}
	
	public String name;
	public TypeEnum type;
	public int value;
	
	public Requirement(String name, TypeEnum type, int value) {
		this.name = name;
		this.type = type;
		this.value = value;
	}
	
	Boolean validate(int compValue) {
		switch(this.type) {
			case EQUAL:
				return (compValue == this.value);
			case MORE_THEN:
				return (compValue > this.value);
			case LESS_THEN:
				return (compValue < this.value);
			case EQUAL_OR_LESS:
				return (compValue <= this.value);
			case EQUAL_OR_MORE:
				return (compValue >= this.value);
		}
		// should never ever get here but compiler did not understand that i think
		// Just returns false if something mysterious happen
		return false;
	}
	
	@Override
	public String toString() {
		return ("name: " + this.name + " type: " + this.type.toString() + " value: " + this.value);
	}
}
