/* The Device IO Manager Listener is a mandatory server component which is not used within the scope of the project.
 * As such the tutorial version provided with the Prosys OPC UA SDK was used.
 * 
 * Credit to Prosys.
 */

package com.SkillsProject.ModularServer;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaMethod;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaValueNode;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.server.io.IoManagerListener;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.core.AccessLevelType;
import com.prosysopc.ua.stack.core.AttributeWriteMask;
import com.prosysopc.ua.stack.core.TimestampsToReturn;
import com.prosysopc.ua.stack.utils.NumericRange;

public class DeviceIoManagerListener implements IoManagerListener{

	@Override
	public AccessLevelType onGetUserAccessLevel(ServiceContext arg0, NodeId arg1, UaVariable arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean onGetUserExecutable(ServiceContext serviceContext, NodeId nodeId, UaMethod node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AttributeWriteMask onGetUserWriteMask(ServiceContext serviceContext, NodeId nodeId, UaNode node) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean onReadNonValue(ServiceContext serviceContext, NodeId nodeId, UaNode node,
			UnsignedInteger attributeId, DataValue dataValue) throws StatusException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onReadValue(ServiceContext serviceContext, NodeId nodeId, UaValueNode variable,
			NumericRange indexRange, TimestampsToReturn timestampsToReturn, DateTime minTimestamp, DataValue dataValue)
			throws StatusException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onWriteNonValue(ServiceContext serviceContext, NodeId nodeId, UaNode node,
			UnsignedInteger attributeId, DataValue dataValue) throws StatusException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean onWriteValue(ServiceContext serviceContext, NodeId nodeId, UaValueNode valueNode,
			NumericRange indexRange, DataValue dataValue) throws StatusException {
		// TODO Auto-generated method stub
		return false;
	}

}
