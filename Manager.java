package banker_lab;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

public class Manager {
	static ArrayList<Task> tasks = new ArrayList<Task>();
	static ArrayList<Task> tasks_ = new ArrayList<Task>();
	static ArrayList<Resource> resources = new ArrayList<Resource>();
	static ArrayList<Resource> resources_ = new ArrayList<Resource>();
	
	public static void main(String[] args) throws Exception {
		// Read and store input
		Scanner sc = new Scanner(new FileReader(args[0]));
		int num_Tasks = sc.nextInt();
		int num_Resources = sc.nextInt();
		for(int i=0; i<num_Resources; i++) {
			Resource r = new Resource(sc.nextInt()); Resource r_ = r;
			resources.add(r);
			resources_.add(r_);
		}
		for(int i=0; i<num_Tasks; i++) {
			tasks.add(new Task(resources.size()));
			tasks_.add(new Task(resources.size()));
		}
		while(sc.hasNextLine()) {
			String activity = sc.nextLine();
			while(activity.equals(""))
				activity = sc.nextLine();
			String temp = activity;
			String[] tempSplit = temp.trim().split("\\s+");
			tasks.get(Integer.parseInt(tempSplit[1])-1).Activities.add(activity);
			tasks_.get(Integer.parseInt(tempSplit[1])-1).Activities.add(activity);
		}
		sc.close();

		ORM(tasks_, resources_);
		banker(tasks, resources);		
	}
	// Banker's algo to avoid deadlocks
	public static boolean bankersAlgo(ArrayList<Resource> resources, int[][] max, int[][] allocated, int n) {
		// initialize data structures
		int[] work = new int[resources.size()];
		int[][] need = new int[n][resources.size()];
		boolean[] finish = new boolean[n];
		for(int i=0; i<work.length; i++) {
			work[i] = resources.get(i).available;
		}
		for(int i=0; i<finish.length; i++) {
			finish[i] = false;
		}
		for(int i=0; i<n; i++) {
			for(int j=0; j<resources.size(); j++) {
				need[i][j] = max[i][j] - allocated[i][j];
			}
		}
		
		int count = 0;
		// while all process are not finished
		while(count < n) {
			boolean found = false;
			// find a process that is not finished and whose needs can be satisfied with the current resources
			for(int i=0; i<n; i++) {
				// if a process is not finished
				if(!finish[i]) {
					int j;
					// check that all resources that task i needs is currently available
					for(j=0; j<resources.size(); j++) {
						if(need[i][j] > work[j])
							break;
					}
					// if all the needs of task i are met
					if(j == resources.size()) {
						// make task i's resources available and mark as finished
						for(int k=0; k<resources.size(); k++)
							work[k] += allocated[i][k];
						finish[i] = true;
						found = true;
						count++;
					}
				}
			}
			// there is no safe space
			if(found == false)
				return false;
		}
		return true;
	}
	// Banker as the manager
	public static void banker(ArrayList<Task> taskList, ArrayList<Resource> resourceList) {
		System.out.println("BANKER'S");
		// initialize data structures
		int n = taskList.size(); int m = resourceList.size();
		ArrayList<Task> blocked = new ArrayList<Task>(); // blocked list of tasks
		ArrayList<Task> completed = new ArrayList<Task>(); // completed list of tasks
		ArrayList<Task> computing = new ArrayList<Task>(); // tasks that are computing
		int[][] max = new int[n][m]; // claims of each task for each resource
		int[][] allocated = new int[n][m]; // the resources currently allocated to each task
		
		// this algo runs until all processes terminate / abort
		while(completed.size() < n) {
			// initialize resources
			int[] resourcesToRelease = new int[resourceList.size()]; // resources to release at end of cycle
			ArrayList<Task> toRemoveBlocked = new ArrayList<Task>(); // tasks to remove from blocked list	
			ArrayList<Task> toRemove = new ArrayList<Task>(); // tasks to remove from task list
			ArrayList<Task> toRemoveComputing = new ArrayList<Task>(); // tasks to removing from computing list
			
			// Check if any blocked tasks can be satisfied
			for(Task t : blocked) {
				String[] activity = t.Activities.get(0).split("\\s+");
				int resourceRequested = Integer.parseInt(activity[2]);
				int amountRequested = Integer.parseInt(activity[3]);
				t.waitingTime++;
				t.finishTime++;
				// if a request can be satisfied, pretend to give the task those resources
				if(amountRequested <= resourceList.get(resourceRequested-1).available) {
					allocated[Integer.parseInt(activity[1])-1][resourceRequested-1] += amountRequested;
					resourceList.get(resourceRequested-1).available -= amountRequested;
					// run the banker's algo to check if the space is still safe after granting resources
					if(bankersAlgo(resourceList, max, allocated, n)) {
						t.currentResources[resourceRequested-1] += amountRequested;
						t.Activities.remove(0);
						t.unblock = true;
					}
					// if the space is not safe, do not grant the resources
					else {
						allocated[Integer.parseInt(activity[1])-1][resourceRequested-1] -= amountRequested;
						resourceList.get(resourceRequested-1).available += amountRequested;
					}
				}
			}

			// update computing tasks list
			ArrayList<Task> addTaskList = new ArrayList<Task>();
			for(Task t : computing) {
				t.currentCompute++;
				t.finishTime++;
				if(t.currentCompute >= t.computeTime) {
					toRemoveComputing.add(t);
					addTaskList.add(t);
				}
			}

			// Iterate through tasks not blocked or computing
			for(Task t : taskList) {
				String[] activity = t.Activities.get(0).split("\\s+");
				// initiate
				if(activity[0].equals("initiate")) {
					t.Activities.remove(0);
					int resource = Integer.parseInt(activity[2]);
					int claim = Integer.parseInt(activity[3]);
					t.finishTime++;
					// check that claim is within the max amount of resources
					if(claim > resourceList.get(resource-1).totalUnits) {
						// abort the task if the claim is not safe
						System.out.println("\tWarning: Task "+activity[1]+" is aborted: claim for resource "+resource+" exceeds total in system ("+resourceList.get(resource-1).totalUnits+")");
						t.abort = true;
						completed.add(t);
						toRemove.add(t);
					}
					else {
						max[Integer.parseInt(activity[1])-1][resource-1] = claim;
					}
				}
				// request
				else if(activity[0].equals("request")) {
					int resource = Integer.parseInt(activity[2]);
					int amount = Integer.parseInt(activity[3]);
					t.finishTime++;
					// check if amount requested is within the claim
					if(amount <= max[Integer.parseInt(activity[1])-1][resource-1] - allocated[Integer.parseInt(activity[1])-1][resource-1]) {
						// check if amount is within the units of resource available
						if(amount <= resourceList.get(resource-1).available) {
							resourceList.get(resource-1).available -= amount;
							allocated[Integer.parseInt(activity[1])-1][resource-1] += amount;
							// check through the banker's algo if, after granting resources, the space is still safe
							if(bankersAlgo(resourceList, max, allocated, n)) {
								t.currentResources[resource-1] += amount;
								t.Activities.remove(0);
							}
							// if the space is not safe, do not grant the resources and send to blocked list
							else {
								resourceList.get(resource-1).available += amount;
								allocated[Integer.parseInt(activity[1])-1][resource-1] -= amount;
								toRemove.add(t);
								blocked.add(t);
							}
						}
						// send to blocked list
						else {
							blocked.add(t);
							toRemove.add(t);
						}
					}
					// abort (request exceeds claim)
					else {
						System.out.println("Task "+activity[1]+" is aborted: its request for resource "+resource+" ("+amount+") exceeds its claim ("+max[Integer.parseInt(activity[1])-1][resource-1]+")");
						t.abort = true;
						completed.add(t);
						toRemove.add(t);
						// restore resources
						for(int i=0; i<resourceList.size(); i++) {
							resourcesToRelease[i] += t.currentResources[i];
						}
					}
				}
				// release resources
				else if(activity[0].equals("release")) {
					int resourceReleased = Integer.parseInt(activity[2]);
					int amountReleased = Integer.parseInt(activity[3]);
					resourcesToRelease[resourceReleased-1] += amountReleased;
					t.currentResources[resourceReleased-1] -= amountReleased;
					allocated[Integer.parseInt(activity[1])-1][resourceReleased-1] -= amountReleased;
					t.Activities.remove(0);
					t.finishTime++;
				}
				// send to computing tasks list
				else if(activity[0].equals("compute")) {
					t.computeTime = Integer.parseInt(activity[2]);
					t.currentCompute = 0;
					computing.add(t);
					toRemove.add(t);
					t.Activities.remove(0);
				}
				// terminate
				else if(activity[0].equals("terminate")) {
					completed.add(t);
					toRemove.add(t);
				}
				else {
					System.out.println("error");
				}
			}			
			taskList.addAll(addTaskList);

			// remove from blocked list
			for(Task t : blocked) {
				if(t.unblock) {
					toRemoveBlocked.add(t);
					taskList.add(t);
					t.unblock = false;
				}
			}

			blocked.removeAll(toRemoveBlocked);
			computing.removeAll(toRemoveComputing);
			taskList.removeAll(toRemove);	

			// release resources
			for(int i=0; i<resourcesToRelease.length; i++) {
				resourceList.get(i).available += resourcesToRelease[i];
			}		
		}
		print(completed);
	}

	// Optimistic Resource Manager
	public static void ORM(ArrayList<Task> taskList, ArrayList<Resource> resourceList) {
		int n = taskList.size();
		ArrayList<Task> blocked = new ArrayList<Task>(); // list for blocked tasks
		ArrayList<Task> completed = new ArrayList<Task>(); // list for completed tasks
		ArrayList<Task> computing = new ArrayList<Task>(); // list for computing tasks
		
		// keep processing until all tasks are completed / aborted
		while(completed.size() < n) {
			int[] resourcesToRelease = new int[resourceList.size()]; // resources to release at end of cycle
			ArrayList<Task> toRemoveBlocked = new ArrayList<Task>(); // tasks to remove from blocked list	
			ArrayList<Task> toRemove = new ArrayList<Task>(); // tasks to remove from task list
			ArrayList<Task> toRemoveComputing = new ArrayList<Task>(); // tasks to remove from compute list
			
			// Detect and Solve Deadlock
			ArrayList<Task> orderedBlocked = new ArrayList<Task>();
			orderedBlocked.addAll(blocked);
			// sort blocked list according to lowest task #
			Collections.sort(orderedBlocked, new Comparator<Task>() {
				@Override
				public int compare(Task a, Task b) {
					int a_ = Integer.parseInt(a.Activities.get(0).split("\\s+")[1]);
					int b_ = Integer.parseInt(b.Activities.get(1).split("\\s+")[1]);
					return a_ - b_;
				}
			});
			int[] resourcesAvailable = new int[resourceList.size()];
			for(int i=0; i<resourceList.size(); i++) {
				resourcesAvailable[i] = resourceList.get(i).available;
			}
			/**
			 * If all tasks are blocked, there is a deadlock
			 */
			outerloop: while(blocked.size() > 1 && taskList.size() <= 0 && computing.size() <= 0) {
				// abort the lowest numbered task
				Task temp = orderedBlocked.get(0);
				temp.abort = true;
				completed.add(temp);
				orderedBlocked.remove(0);
				blocked.remove(temp);
				// free the aborted task's resources (they become available the next cycle, but the info is needed now)
				for(int i=0; i<temp.currentResources.length; i++) {
					resourcesAvailable[i] += temp.currentResources[i];
					resourcesToRelease[i] += temp.currentResources[i];
				}
				// check if a blocked tasks' request can be satisfied
				for(int i=0; i<blocked.size(); i++) {
					int amtRequested = Integer.parseInt(blocked.get(i).Activities.get(0).split("\\s+")[3]);
					int resRequested = Integer.parseInt(blocked.get(i).Activities.get(0).split("\\s+")[2]);
					if(amtRequested <= resourcesAvailable[resRequested-1]) {
						blocked.get(i).waitingTime--;
						blocked.get(i).finishTime--;
						break outerloop;
					}
				}
			}
			
			// Check if any blocked tasks can be satisfied
			for(Task t : blocked) {
				String[] activity = t.Activities.get(0).split("\\s+");
				int resourceRequested = Integer.parseInt(activity[2]);
				int amountRequested = Integer.parseInt(activity[3]);
				t.waitingTime++;
				t.finishTime++;
				// if a request can be satisfied, give the task those resources
				if(amountRequested <= resourceList.get(resourceRequested-1).available) {
					resourceList.get(resourceRequested-1).available -= amountRequested;
					t.currentResources[resourceRequested-1] += amountRequested;
					t.Activities.remove(0);
					t.unblock = true;
				}
			}
			
			// update computing tasks list
			ArrayList<Task> addTaskList = new ArrayList<Task>();
			for(Task t : computing) {
				t.currentCompute++;
				t.finishTime++;
				if(t.currentCompute >= t.computeTime) {
					toRemoveComputing.add(t);
					addTaskList.add(t);
				}
			}

			// Iterate through tasks not blocked or computing
			for(Task t : taskList) {
				String[] activity = t.Activities.get(0).split("\\s+");
				if(activity[0].equals("initiate")) {
					t.Activities.remove(0);
					t.finishTime++;
				}
				else if(activity[0].equals("request")) {
					int resourceRequested = Integer.parseInt(activity[2]);
					int amountRequested = Integer.parseInt(activity[3]);
					// satisfy request
					if(amountRequested <= resourceList.get(resourceRequested-1).available) {
						resourceList.get(resourceRequested-1).available -= amountRequested;
						t.currentResources[resourceRequested-1] += amountRequested;
						t.Activities.remove(0);
						t.finishTime++;
					}
					// send to blocked list
					else {
						blocked.add(t);
						toRemove.add(t);
						t.finishTime++;
					}
				}
				// release resources
				else if(activity[0].equals("release")) {
					int resourceReleased = Integer.parseInt(activity[2]);
					int amountReleased = Integer.parseInt(activity[3]);
					resourcesToRelease[resourceReleased-1] += amountReleased;
					t.currentResources[resourceReleased-1] -= amountReleased;
					t.Activities.remove(0);
					t.finishTime++;
				}
				// send to computing tasks list
				else if(activity[0].equals("compute")) {
					t.computeTime = Integer.parseInt(activity[2]);
					t.currentCompute = 0;
					computing.add(t);
					toRemove.add(t);
					t.Activities.remove(0);
				}
				// terminate
				else if(activity[0].equals("terminate")) {
					completed.add(t);
					toRemove.add(t);
				}
				else {
					System.out.println("error");
				}
			}			
			taskList.addAll(addTaskList);
		
			// remove from blocked list
			for(Task t : blocked) {
				if(t.unblock) {
					toRemoveBlocked.add(t);
					taskList.add(t);
					t.unblock = false;
				}
			}
			
			blocked.removeAll(toRemoveBlocked);
			computing.removeAll(toRemoveComputing);
			taskList.removeAll(toRemove);	
			
			// release resources
			for(int i=0; i<resourcesToRelease.length; i++) {
				resourceList.get(i).available += resourcesToRelease[i];
			}		
		}
		System.out.println("FIFO");
		print(completed);
	}
	
	// print the output		
	public static void print(ArrayList<Task> taskList) {
		DecimalFormat numberFormat = new DecimalFormat("#");
		int totalTime = 0;
		int totalWait = 0;
		double totalPercentage;
		Collections.sort(taskList, new Comparator<Task>() {
			@Override
			public int compare(Task a, Task b) {
				int a_ = Integer.parseInt(a.Activities.get(0).split("\\s+")[1]);
				int b_ = Integer.parseInt(b.Activities.get(0).split("\\s+")[1]);
				return a_ - b_;
			}
		});

		for(int i=0; i<taskList.size(); i++) {
			if(taskList.get(i).abort) {
				System.out.println("Task "+(i+1)+"\taborted");
			}
			else {
				totalTime += taskList.get(i).finishTime;
				totalWait += taskList.get(i).waitingTime;
				double percentage = ((double)taskList.get(i).waitingTime/(double)taskList.get(i).finishTime)*100;
				System.out.println("Task "+ (i+1) + "\t" + taskList.get(i).finishTime + "\t" + taskList.get(i).waitingTime + "\t" + numberFormat.format(percentage)+"%");		
			}
		}
		totalPercentage = ((double)totalWait/(double)totalTime)*100;
		System.out.println("Total\t" + totalTime + "\t" + totalWait+ "\t" + numberFormat.format(totalPercentage)+"%\n");
	}
}
