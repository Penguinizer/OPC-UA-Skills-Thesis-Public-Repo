package com.SkillsProject.ControlClient;

import java.util.Arrays;

import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.client.AddressSpaceException;
import com.prosysopc.ua.client.MonitoredEventItem;
import com.prosysopc.ua.client.MonitoredEventItemListener;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.stack.builtintypes.Variant;

public class controlEventListener implements MonitoredEventItemListener{
	private final ControlClientMain base;
	private boolean autoRunOn;

	public controlEventListener (ControlClientMain main) {
		this.base = main;
	}

	public void toggleAutoState (boolean autoState) {
		this.autoRunOn = autoState;
	}
	
	@Override
	public void onEvent(MonitoredEventItem monitoredItem, Variant[] eventFields) {
		//System.out.println("LISTENER - Event Listened to: " + monitoredItem.getNodeId() + ", length: " + eventFields.length);
		try {
			if(autoRunOn) {
				base.MCP.passEvent(monitoredItem, eventFields);
			}
			else {
				String[] message = eventFields[1].toString().replaceAll("[() ]", "").split(";");
				System.out.println("Message: " + Arrays.toString(message));
			}
		} catch (Exception e) {
			System.out.println("Catch in listener. Something wrong. " + e);
		}
	}
	
}
