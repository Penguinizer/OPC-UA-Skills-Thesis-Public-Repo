/* The Device Event Manager Listener is a mandatory address space object.
 * Based primarily on the tutorial version provided with the Prosys OPC UA SDK.
 * 
 * getNextUserEventID function was added in additon to required function stubs.
 */

package com.SkillsProject.ModularServer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.server.EventManager;
import com.prosysopc.ua.server.EventManagerListener;
import com.prosysopc.ua.server.MonitoredEventItem;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.server.Subscription;
import com.prosysopc.ua.stack.builtintypes.ByteString;
import com.prosysopc.ua.stack.builtintypes.LocalizedText;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.core.EventFilter;
import com.prosysopc.ua.stack.core.EventFilterResult;
import com.prosysopc.ua.types.opcua.server.AcknowledgeableConditionTypeNode;
import com.prosysopc.ua.types.opcua.server.AlarmConditionTypeNode;
import com.prosysopc.ua.types.opcua.server.ConditionTypeNode;
import com.prosysopc.ua.types.opcua.server.ShelvedStateMachineTypeNode;

public class DeviceEventManagerListener implements EventManagerListener{
	//Internal counter for ensuring eventId:s are unique
	private int eventId = 0;
	
	//Logger
	private static Logger logger = LoggerFactory.getLogger(ModularDeviceMethodManagerListener.class);

	@Override
	public boolean onAcknowledge(ServiceContext serviceContext, AcknowledgeableConditionTypeNode condition,
			ByteString eventId, LocalizedText comment) throws StatusException {
		logger.info("Event acknowledged: " + eventId + ", Comment: " + comment);
		System.out.println("Event acknowledged: " + eventId + ", Comment: " + comment);
		return true;
	}

	@Override
	public boolean onAddComment(ServiceContext serviceContext, ConditionTypeNode condition, ByteString eventId,
			LocalizedText comment) throws StatusException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onAfterCreateMonitoredEventItem(ServiceContext serviceContext, Subscription subscription,
			MonitoredEventItem item) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAfterDeleteMonitoredEventItem(ServiceContext serviceContext, Subscription subscription,
			MonitoredEventItem item) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAfterModifyMonitoredEventItem(ServiceContext serviceContext, Subscription subscription,
			MonitoredEventItem item) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConditionRefresh(ServiceContext serviceContext, Subscription subscription) throws StatusException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConditionRefresh2(ServiceContext serviceContext, MonitoredEventItem item) throws StatusException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onConfirm(ServiceContext serviceContext, AcknowledgeableConditionTypeNode condition,
			ByteString eventId, LocalizedText comment) throws StatusException {
		logger.info("Event confirmed: " + eventId + ", Comment: " + comment);
		System.out.println("Event confirmed: " + eventId + ", Comment: " + comment);
		return true;
	}

	@Override
	public void onCreateMonitoredEventItem(ServiceContext serviceContext, NodeId nodeId, EventFilter eventFilter,
			EventFilterResult eventFilterResult) throws StatusException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDeleteMonitoredEventItem(ServiceContext serviceContext, Subscription subscription,
			MonitoredEventItem monitoredItem) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onDisable(ServiceContext serviceContext, ConditionTypeNode condition) throws StatusException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onEnable(ServiceContext serviceContext, ConditionTypeNode condition) throws StatusException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void onModifyMonitoredEventItem(ServiceContext serviceContext, Subscription subscription,
			MonitoredEventItem monitoredItem, EventFilter eventFilter, EventFilterResult eventFilterResult)
			throws StatusException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean onOneshotShelve(ServiceContext serviceContext, AlarmConditionTypeNode condition,
			ShelvedStateMachineTypeNode stateMachine) throws StatusException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onTimedShelve(ServiceContext serviceContext, AlarmConditionTypeNode condition,
			ShelvedStateMachineTypeNode stateMachine, double shelvingTime) throws StatusException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onUnshelve(ServiceContext serviceContext, AlarmConditionTypeNode condition,
			ShelvedStateMachineTypeNode stateMachine) throws StatusException {
		// TODO Auto-generated method stub
		return false;
	}
	
	//Gets the next free event ID in order to avoid duplicates IDs which would cause issues. 
	ByteString getNextUserEventId() throws RuntimeException {
	    return EventManager.createEventId(eventId++);
	  }

}
