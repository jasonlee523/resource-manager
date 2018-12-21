package banker_lab;

import java.util.ArrayList;

public class Task {
	int finishTime;
	int waitingTime;
	int[] claims;
	int[] currentResources;
	int computeTime;
	int currentCompute;
	boolean unblock = false;
	boolean abort = false;
	//int maxAdditional;
	ArrayList<String> Activities = new ArrayList<String>();
	
	
	
	public Task() {
	}
	
	public Task(int resources) {
		this.currentResources = new int[resources];
	}
}
