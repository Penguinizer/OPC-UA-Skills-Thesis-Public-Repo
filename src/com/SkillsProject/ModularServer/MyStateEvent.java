/* Contains an extended version of the BaseEventTypeNode used to communicate server events to the control client.
 * 
 * Contains several variable objects in addition to the string message defined within the node manager method.
 * Contains a handful of utility methods for setting and reading the event object variables.
 * Said variables are presently largely there for testing as the event text is used to communicate relevant information.
 * 
 * Loosely based on the tutorial implementation provided with the Prosys OPC UA SDK.
 */

package com.SkillsProject.ModularServer;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.TypeDefinitionId;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.server.NodeManagerUaNode;
import com.prosysopc.ua.stack.builtintypes.LocalizedText;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.QualifiedName;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.types.opcua.server.BaseEventTypeNode;

//Custom event type which contains additional info fields.
@TypeDefinitionId(nsu = ModularNodeManager.NAMESPACE, i = MyStateEvent.MY_EVENT_ID)
public class MyStateEvent extends BaseEventTypeNode{
	public static final int MY_EVENT_ID = 10000;
	public static final UnsignedInteger MY_MACHINE_ID = UnsignedInteger.valueOf(10001);
	public static final String MY_MACHINE_NAME = "MyMachineName";
	public static final UnsignedInteger MY_STATE_ID = UnsignedInteger.valueOf(10002);
	public static final String MY_STATE_NAME = "MyStateNumber";
	
	protected MyStateEvent(NodeManagerUaNode nodeManager, NodeId nodeId, QualifiedName browseName, 
			LocalizedText displayName) {
		super (nodeManager, nodeId, browseName, displayName);
	}
	
	//Get the machine variable
	public UaVariable getMachineVariableNode() {
		UaVariable var = (UaVariable) getComponent(new QualifiedName(getNodeManager().getNamespaceIndex(), MY_MACHINE_NAME));
		return var;
	}
	//Get machine variable value
	public String getMachineNameVariable() {
		UaVariable varNode = getMachineVariableNode();
		if (varNode == null) {
			return null;
		}
		return (String) varNode.getValue().getValue().getValue();
	}
	//Set machine variable value
	public void setMachineNameVariable(String newName) throws StatusException {
		UaVariable varNode = getMachineVariableNode();
		if (varNode != null) {
			varNode.setValue(newName);
		}
		else {
			System.out.println("Bad variable, something broke.");
		}
	}
	
	//Get the state variable.
	public UaVariable getStateVariableNode() {
		UaVariable var = (UaVariable) getComponent(new QualifiedName(getNodeManager().getNamespaceIndex(), MY_STATE_NAME));
		return var;
	}
	//Get state variable value
	public String getStateNumVariable() {
		UaVariable varNode = getStateVariableNode();
		if (varNode == null) {
			return null;
		}
		return (String) varNode.getValue().getValue().getValue();
	}
	//Set state variable value
	public void setStateNumVariable(int stateNum) throws StatusException {
		UaVariable varNode = getStateVariableNode();
		if (varNode != null) {
			varNode.setValue(stateNum);
		}
		else {
			System.out.println("Bad variable, something broke.");
		}
	}
}
