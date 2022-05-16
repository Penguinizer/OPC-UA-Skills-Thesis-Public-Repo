package com.SkillsProject.ControlClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.client.AddressSpaceException;
import com.prosysopc.ua.client.MonitoredEventItem;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.QualifiedName;
import com.prosysopc.ua.stack.builtintypes.Variant;

public class MasterControlProgram implements Runnable{
	protected ControlClientMain base;
	protected QualifiedName[] fieldNames;
	protected Stack<String[]> messageQueue = new Stack<String[]>();
	protected int currentState;
	protected Map<UaClient,Integer> deviceStates = new HashMap<UaClient,Integer>();
	protected Boolean shutdown = false;
	protected int maxState = 5;
	protected String currentRecipe="none";
	
	//List of variables for state transition conditions that've been met.
	
	public MasterControlProgram(ControlClientMain base) {
		this.base = base;
		this.currentState = 1;
	}
	
	//Pushes messages in array form into a stack.
	public void passEvent(MonitoredEventItem monitoredItem, Variant[] eventFields) throws ServiceException, AddressSpaceException {
		System.out.println("MCP - Event Listened to: " + monitoredItem.getNodeId() + " Length: " + eventFields.length);
		String[] message = eventFields[1].toString().replaceAll("[() ]", "").split(";");
		System.out.println("Message: " + Arrays.toString(message));
		messageQueue.push(message);
	}
	public void userInput(String userInput) {
		String[] message = userInput.replaceAll("[() ]", "").split(";");
		System.out.println("Message: " + Arrays.toString(message));
		messageQueue.push(message);
	}
	public void shutdownMCP() {
		shutdown=true;
	}
	
	public void passClients() {
		//Populate list of device client objects, setting them to their starting state.
		for(Map.Entry<Integer, UaClient> clientEntry: base.multiClientMap.entrySet()) {
			//System.out.println("Client: " + clientEntry);
			deviceStates.put(clientEntry.getValue(), 1);
		}
		//System.out.println("Length: " + deviceStates);
	}

	//Implements the control loop.
	@Override
	public void run(){
		// TODO Auto-generated method stub
		
		//Variable declarations for the control loop
		String[] message;
		
		//The control loop itself.
		do {		
			//Check if there are messages in the queue, parse properly.
			if (!messageQueue.empty()) {
				message = messageQueue.pop();
				System.out.println("MESSAGE FROM ANOTHER THREAD: " + Arrays.toString(message));
				if (message[0].equals("Shutdown")) {
					//Shutdown command received
					break;
				}
				else if (message[0].equals("ChangeControlState")) {
					//Check that the state is valid, if is transition, else report bad state.
					if (Integer.parseInt(message[1]) >= 1 && Integer.parseInt(message[1]) <= maxState) {
						currentState = Integer.parseInt(message[1]);
					}
					else {
						System.out.println("Invalid target state");
					}
				}
				else if(message[0].equals("SkillRecipe")) {
					if(currentRecipe.equals("none")) {
						currentRecipe=message[1];
					}
				}
				else {
					continue;
				}
			}/*
			else {
				System.out.println("Nothing in queue.");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}*/
			
			//Control state transition checks
			//The states of the control logic itself
			//Iterate through the list of state machines, transitioning states according to controller state.
			for (Map.Entry<UaClient, Integer> machine: deviceStates.entrySet()) {
				switch(currentState) {
				case 1:
					break;
				case 2:
					break;
				case 3:
					break;
				case 4:
					break;
				case 5:
					break;
				}
			}
		}while(!shutdown);
		System.out.println("Main control loop shut down.");
	}
}
