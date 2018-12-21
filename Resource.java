package banker_lab;

public class Resource {
	int totalUnits;
	int available;
	
	public Resource() {
		
	}
	
	public Resource(int units) {
		this.totalUnits = units;
		this.available = units;
	}
}
