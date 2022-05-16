/* The OPC UA Device Server Node Manager class
 *
 * Contains functionality for the creation and definition of all address space objects and their hierarchy.
 * In addition contains some other utility functionality such as sending event messages.
 */

package com.SkillsProject.ModularServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.UaBrowsePath;
import com.prosysopc.ua.UaQualifiedName;
import com.prosysopc.ua.ValueRanks;
import com.prosysopc.ua.nodes.UaMethod;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaNodeFactoryException;
import com.prosysopc.ua.nodes.UaObjectType;
import com.prosysopc.ua.nodes.UaReference;
import com.prosysopc.ua.nodes.UaType;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.server.CallableListener;
import com.prosysopc.ua.server.MethodManagerUaNode;
import com.prosysopc.ua.server.ModellingRule;
import com.prosysopc.ua.server.NodeManagerUaNode;
import com.prosysopc.ua.server.UaServer;
import com.prosysopc.ua.server.instantiation.TypeDefinitionBasedNodeBuilderConfiguration;
import com.prosysopc.ua.server.nodes.PlainMethod;
import com.prosysopc.ua.server.nodes.PlainProperty;
import com.prosysopc.ua.server.nodes.PlainVariable;
import com.prosysopc.ua.server.nodes.UaObjectNode;
import com.prosysopc.ua.server.nodes.UaObjectTypeNode;
import com.prosysopc.ua.stack.builtintypes.ByteString;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.LocalizedText;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.QualifiedName;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.core.Argument;
import com.prosysopc.ua.stack.core.Identifiers;
import com.prosysopc.ua.types.opcua.server.BaseObjectTypeNode;
import com.prosysopc.ua.types.opcua.server.FiniteStateMachineTypeNode;
import com.prosysopc.ua.types.opcua.server.InitialStateTypeNode;
import com.prosysopc.ua.types.opcua.server.StateTypeNode;
import com.prosysopc.ua.types.opcua.server.TransitionTypeNode;
import com.prosysopc.ua.types.di.server.ConfigurableObjectTypeNode;
import com.prosysopc.ua.types.di.server.DeviceTypeNode;
import com.prosysopc.ua.types.di.server.DiServerInformationModel;
import com.prosysopc.ua.types.di.server.FunctionalGroupTypeNode;
import com.prosysopc.ua.types.di.server.SoftwareTypeNode;
import com.prosysopc.ua.types.plc.server.CtrlConfigurationTypeNode;
import com.prosysopc.ua.types.plc.server.PlcServerInformationModel;

public class ModularNodeManager extends NodeManagerUaNode{
	//Definitions and declarations.
	public static final String NAMESPACE = "SkillsThesisNamespace";
	//public static final String NAMESPACE = "http://opcfoundation.org/UA/DI/";
	private static final Logger logger = LoggerFactory.getLogger(ModularNodeManager.class);
	private String serverType;
	private CallableListener myDevicetMethodManagerListener;
	private DeviceEventManagerListener myDeviceEventManagerListener = new DeviceEventManagerListener();
	protected NodeManagerUaNode beltNodeManager;
	UaServer server;
	private int portIterator = 0; 
	
	//Initialize the map that will be populated by the type nodes to be created.
	Map <String, UaObjectType> typeMap = new HashMap <String, UaObjectType>();
	
	public MyStateEvent testNode;
	public Map<String,UaObjectNode> spaceObjects;
	public Map<String,UaVariable> spaceVariables;
	public Map<String,UaMethod> spaceMethods;
	public Map<String, UaObjectNode> spaceSkills;
	public TcpIOServer spaceServer;
	//TODO: Update with multiple bridge functionality.
	//public Map<String, Bridge> bridgeMap = new HashMap<String, Bridge>();
	public Bridge spaceBridge;
	public Boolean bridgeReady = false;
	
	public ModularNodeManager(UaServer inServ, String Uri, String serverType) {
		super(inServ, Uri);
		this.server=getServer();
		this.serverType = serverType;
		//Register the DI model so that DeviceType and SoftwareType can be used.
		server.registerModel(DiServerInformationModel.MODEL);
		server.registerModel(PlcServerInformationModel.MODEL);
		try {
			server.getAddressSpace().loadModel(DiServerInformationModel.class.getResource("Opc.Ua.Di.NodeSet2.xml").toURI());
			server.getAddressSpace().loadModel(PlcServerInformationModel.class.getResource("Opc.Ua.Plc.NodeSet2.xml").toURI());
		}catch(Exception e) {
			System.out.println("Error in loading models: " + e);
			logger.error("Error in loading models: " + e);
		}
	}
	@Override
	protected void init() throws StatusException, UaNodeFactoryException {
		super.init();
	}

	//Address space creation related functions
	protected void createAddressSpace() throws StatusException {
		//Set the event manager listener
		this.getEventManager().setListener(myDeviceEventManagerListener);
		
		//Get the namespace index for creating future NodeID objects.
		int ns = getNamespaceIndex();
		
		//Call function responsible for mapping types used in the address space.
		Map<String, UaObjectType> typeMap = createTypeNodes(this.server, ns);
		
		//Call function for creating the address space.
		createAddressSpace(this.server, ns, typeMap, serverType);
		
		//Create the DeviceSet object.
		final UaNode objFolder = server.getNodeManagerRoot().getObjectsFolder();
		UaReference[] comps = objFolder.getReferences(Identifiers.Organizes,false);
		UaNode deviceSet = null;
		for (UaReference comp:comps) {
			if (comp.getTargetNode().getBrowseName().getName().equals("DeviceSet")) {
				deviceSet = comp.getTargetNode();
			}
		}
		
		//Finally, crate the state event node used for sending events.
		createStateEventNode("StateEventTest", ns, deviceSet);
		logger.info("Address space created");
	}
	
	//Utility function for creating and organizing the various types used in the address space.
	//Stored in a map for easier access in other functions.
	private Map<String, UaObjectType> createTypeNodes(UaServer server, int ns) throws StatusException {
		//Retrieve various base types for easier access.
		final UaType baseObjectType = server.getNodeManagerRoot().getType(Identifiers.BaseObjectType);
		UaType topType = getSubType(baseObjectType, "TopologyElementType");
		UaType compType = getSubType(topType, "ComponentType");
		UaType deviceType = getSubType(compType, "DeviceType");
		UaType softwareType = getSubType(compType,"SoftwareType");
		
		//Create a list of the names of all types to be created.
		List<String> typeNames = List.of("SkillBridge","SoftwareComponent", "ConveyorDevice", "ManipulatorDevice", "Iceblock"
						, "BeltComponent", "PistonComponent", "GrippingComponent", "SensorComponent", "SkillObjectType");
		
		//Loop through the list of types, using a switch-case to handle differing base types.
		for (String name:typeNames) {
			switch(name) {
			case "SoftwareComponent":
			case "SkillBridge":
				typeMap.put(name, createObjectType(server,ns,softwareType,name));
				break;
			case "ConveyorDevice":
			case "ManipulatorDevice":
			case "BeltComponent":
			case "PistonComponent":
			case "GrippingComponent":
			case "SensorComponent":
			case "Iceblock":
				typeMap.put(name, createObjectType(server,ns,deviceType,name));
				break;
			case "SkillObjectType":
				final NodeId skillTypeId = new NodeId (ns, "SkillObjectType");
				UaObjectType skillType = new UaObjectTypeNode(this, skillTypeId, "SkillObjectType", Locale.ENGLISH);
				this.addNodeAndReference(baseObjectType, skillType, Identifiers.HasSubtype);
				typeMap.put(name, skillType);
				break;
			default:
				System.out.println("Generic type triggered. Un-case'd type detected: " + name);
				typeMap.put(name,createObjectType(server,ns,baseObjectType,name));
				break;
			}
		}
		logger.info("Type map created.");
		return typeMap;
	}
	
	// A sub function for the task of creating the type node.
	//To be called from inside the for loop.
	//Creating a subtype
	UaObjectType createObjectType(UaServer server, int ns, UaType baseObjType, String typeName){
		final NodeId tempTypeId = new NodeId (ns, typeName);
		UaObjectType tempType = new UaObjectTypeNode(this, tempTypeId, typeName, Locale.ENGLISH);
		
		try {
			this.addNodeAndReference(baseObjType, tempType, Identifiers.HasSubtype);
			return tempType;
		} catch(StatusException e) {
			System.out.println("Something went wrong with the type creation: " + e);
			return null;
		}
	}
	//An overloaded function specifically for creating types for which the parent
	// type node has been created earlier within the loop.
	UaObjectType createObjectType(UaServer server, int ns, ModularNodeManager nodeManager, UaObjectType parentType, String typeName) {
		final NodeId tempTypeId = new NodeId (ns, typeName);
		UaObjectType tempType = new UaObjectTypeNode(nodeManager, tempTypeId, typeName, Locale.ENGLISH);
		
		try {
			nodeManager.addNodeAndReference(parentType, tempType, Identifiers.HasSubtype);
			return tempType;
		} catch(StatusException e) {
			System.out.println("Something went wrong with the type creation: " + e);
			return null;
		}
	}

	//Main function for address space creation.
	//Creates the objects themselves using types defined earlier.
	private void createAddressSpace (UaServer server, int ns, Map<String, UaObjectType> typeMap, String serverType) throws StatusException {
		//Fetch a few necessary types that aren't in the map of custom types.
		//Primarily the FunctionalGroupType, MethodSet, etc.
		final UaType baseObjectType = server.getNodeManagerRoot().getType(Identifiers.BaseObjectType);
		final UaType hasComponentRef = server.getNodeManagerRoot().getType(Identifiers.HasComponent);
		UaType boolType = server.getNodeManagerRoot().getType(Identifiers.Boolean);
		UaType folderType = server.getNodeManagerRoot().getType(Identifiers.FolderType);
		UaType funcGroupType = getSubType(folderType, "FunctionalGroupType");
		UaType hasInputsType = getSubType(hasComponentRef ,"HasInputVars");
		UaType hasOutputsType = getSubType(hasComponentRef ,"HasOutputVars");
		UaType organizesType = server.getNodeManagerRoot().getType(Identifiers.Organizes);
		
		//General address space creation prior to the creation of the devices.
		//Get the objects folder to act as a parent.
		final UaNode objFolder = server.getNodeManagerRoot().getObjectsFolder();
		//Get the deviceSet object, similarly for use as a parent
		UaReference[] comps = objFolder.getReferences(Identifiers.Organizes,false);
		UaNode deviceSet = null;
		for (UaReference comp:comps) {
			if (comp.getTargetNode().getBrowseName().getName().equals("DeviceSet")) {
				deviceSet = comp.getTargetNode();
			}
		}
		//Make the device set the object folder's child in the hierarchy.
		objFolder.addReference(deviceSet, Identifiers.Organizes, false);
		objFolder.addReference(deviceSet, Identifiers.HasNotifier, false);
		
		//Create a list of servers to create and arrays to populate to iterate through for creation.
		//In server list first part is the server name, the second is the server type.
		Map<String, String> serverList = new HashMap<String, String>();
		Map<String,String> parentList = new HashMap <String, String>();
		Map<String,String> componentList = new HashMap <String, String>();
		Map<String,String> skillList = new HashMap <String, String>();
		//Create a map of newly created device objects so that we can add them as parents down the line.
		Map<String,UaObjectNode> createdObjects = new HashMap <String, UaObjectNode>();
		Map<String,UaVariable> createdVariables = new HashMap <String, UaVariable>();
		Map<String,UaMethod> createdMethods = new HashMap <String, UaMethod>();
		Map<String, UaObjectNode> createdSkills = new HashMap <String, UaObjectNode>();
		
		//For the sake of creating the combined server create a list of server address spaces to create.
		if (serverType.equals("CombinedServer")) {
			serverList.put("Belt_1", "BeltServer");
			serverList.put("Belt_2", "BeltServer");
			serverList.put("BeltGripper_1", "BeltandGripperServer");
			serverList.put("BeltGripper_2", "BeltandGripperServer");
			serverList.put("JackSledge_1", "JackandSledgeServer");
			serverList.put("JackSledge_2", "JackandSledgeServer");
			serverList.put("VirtualReactionVessel", "VirtualTankServer");
		} else {
			serverList.put(serverType, serverType);
		}
		
		//Create the topParent variable to target either a deviceSet or other organization object.
		UaNode topParent = null;
		
		//Iterate through server list to create multiple servers if need be.
		for (Map.Entry<String,String> serverInst: serverList.entrySet()) {
		//If creating a singular server do nothing
		//If creating a combined server create the subDeviceSets that organize/separate the subservers. 
			if (serverType.equals("CombinedServer")) {
				NodeId topParentId= new NodeId(ns, serverInst.getKey()+"_Set");
				topParent = new UaObjectNode(this, topParentId, serverInst.getKey()+"_Set", Locale.ENGLISH);
				deviceSet.addReference(topParent, Identifiers.Organizes, false);
				deviceSet.addReference(topParent, Identifiers.HasNotifier, false);
			}else {
				topParent = deviceSet;
			}

		//Create a new map that will contain the name of the object and its type.
		//Used to list out all the components of the address space model.
		//This list will then be iterated in order to later create all the objects.
		//Various things taken from addressspace models (see drive)
		//Types:
		//"SkillBridge","SoftwareComponent", "ConveyorDevice", "ManipulatorDevice", 
		//"SkillStateMachine" ,"BeltComponent", "PistonComponent", "GrippingComponent", 
		//"SensorComponent", Method, Variable
			parentList.clear();
			componentList.clear();
			skillList.clear();
			switch (serverInst.getValue()) {
				case "BeltServer":
					//Components: "BeltIceBlock", "Belt", "SkillBridge", "MotorBelt", "BeltMotionSkills", "BeltRFIDSensor"
					//Vars: MotorOutput
					//Methods: StartBelt, StopBelt
					//Skill state machines:
					parentList.put(serverInst.getKey()+"_BeltController", "PLC");
					parentList.put(serverInst.getKey()+"_Belt", "ConveyorDevice");
					parentList.put(serverInst.getKey()+"_SkillBridgeNode", "SkillBridge");
					parentList.put(serverInst.getKey()+"_RFIDSensor", "SensorComponent");
					parentList.put(serverInst.getKey()+"_OpticalSensor", "SensorComponent");
					componentList.put(serverInst.getKey()+"_MotorBelt", "BeltComponent");
					componentList.put(serverInst.getKey()+"_DeviceSkills", "SoftwareComponent");
					componentList.put(serverInst.getKey()+"_MotorOutput", "outVar");
					componentList.put(serverInst.getKey()+"_ReportCapabilities", "Method");
					componentList.put(serverInst.getKey()+"_ToggleSensorReporting", "Method");
					componentList.put(serverInst.getKey()+"_SomethingBySensor", "inVar");
					skillList.put(serverInst.getKey()+"_BeltPauseOnSensor","BeltSensPause");
					skillList.put(serverInst.getKey()+"_BeltRunWithoutPauses", "BeltRunWOPause");
					break;
				case "BeltandGripperServer":
					//Components: "BeltGripperIceBlock", "SkillBridge", "Belt", "Grip", "MotorBelt", "BeltMotionSkills"
					// cont: "PistonGripper", "HorzPiston", "Gripper", "PistonGripperSensors", "PistonGripperSkills", "RFIDSensor"
					//Vars Out: MotorOutput
					//Vars In: Top, Bottom, Grip
					//Methods: StartBelt, StopBelt, PickAndHold, PutDown, PickAndPutDown
					//Skill state machines:
					parentList.put(serverInst.getKey()+"_BeltGripperController", "PLC");
					parentList.put(serverInst.getKey()+"_SkillBridgeNode", "SkillBridge");
					parentList.put(serverInst.getKey()+"_Belt", "ConveyorDevice");
					parentList.put(serverInst.getKey()+"_PistonGripper", "ManipulatorDevice");
					parentList.put(serverInst.getKey()+"_OpticalSensor", "SensorComponent");
					parentList.put(serverInst.getKey()+"_RFIDSensor", "SensorComponent");
					componentList.put(serverInst.getKey()+"_Gripper", "GrippingComponent");
					componentList.put(serverInst.getKey()+"_MotorBelt", "BeltComponent");
					componentList.put(serverInst.getKey()+"_HorzPiston", "PistonComponent");
					componentList.put(serverInst.getKey()+"_PistonGripperSensors", "gripSensorComponent");
					componentList.put(serverInst.getKey()+"_DeviceSkills", "SoftwareComponent");
					componentList.put(serverInst.getKey()+"_MotorOutput", "beltOutVar");
					componentList.put(serverInst.getKey()+"_Grip", "gripOutVar");
					componentList.put(serverInst.getKey()+"_Top", "gripInVar");
					componentList.put(serverInst.getKey()+"_Bottom", "gripInVar");
					componentList.put(serverInst.getKey()+"_ReportCapabilities", "Method");
					componentList.put(serverInst.getKey()+"_ToggleSensorReporting", "Method");
					componentList.put(serverInst.getKey()+"_SomethingBySensor", "sensInVar");
					componentList.put(serverInst.getKey()+"_Down", "gripOutVar");
					skillList.put(serverInst.getKey()+"_BeltPauseOnSensor","BeltSensPause");
					skillList.put(serverInst.getKey()+"_BeltRunWithoutPauses", "BeltRunWOPause");
					skillList.put(serverInst.getKey()+"_GripperGripAndHold", "GripGripAndHold");
					skillList.put(serverInst.getKey()+"_GripperGripAndPutDown", "GripGripPutDown");
					break;
				case "JackandSledgeServer":
					//Components: "JackSledgeIceBlock", "SkillBridge", "JackSledge", "VertPistons", "HorzPiston"
					// cont: "SledgePiston", "VaccumPump", "JackSledgeSensors", "JackSledgeSkills"
					//Vars Out: Extend, Down, Vacuum, Sledge
					//Vars In: Top, Bottom, Extended, Retracted
					//Methods: Jack_Place, Jack_Remove
					//Skill state machines:
					parentList.put(serverInst.getKey()+"_JackSledgeController", "PLC");
					parentList.put(serverInst.getKey()+"_SkillBridgeNode", "SkillBridge");
					parentList.put(serverInst.getKey()+"_JackSledge", "ManipulatorDevice");
					componentList.put(serverInst.getKey()+"_VertPiston", "PistonComponent");
					componentList.put(serverInst.getKey()+"_HorzPiston", "PistonComponent");
					componentList.put(serverInst.getKey()+"_SledgePiston", "PistonComponent");
					componentList.put(serverInst.getKey()+"_VacuumPump", "GrippingComponent");
					componentList.put(serverInst.getKey()+"_JackSledgeSensors", "SensorComponent");
					componentList.put(serverInst.getKey()+"_DeviceSkills", "SoftwareComponent");
					componentList.put(serverInst.getKey()+"_Extend", "outVar");
					componentList.put(serverInst.getKey()+"_Down", "outVar");
					componentList.put(serverInst.getKey()+"_Vacuum", "outVar");
					componentList.put(serverInst.getKey()+"_Sledge", "outVar");
					componentList.put(serverInst.getKey()+"_Top", "inVar");
					componentList.put(serverInst.getKey()+"_Bottom", "inVar");
					componentList.put(serverInst.getKey()+"_Extended", "inVar");
					componentList.put(serverInst.getKey()+"_Retracted", "inVar");
					componentList.put(serverInst.getKey()+"_ReportCapabilities", "Method");
					skillList.put(serverInst.getKey()+"_JackFromBeltToSledgeOne", "JnSBtSOne");
					skillList.put(serverInst.getKey()+"_JackFromBeltToSledgeTwo", "JnSBtSTwo");
					skillList.put(serverInst.getKey()+"_JackFromSledgeOneToBelt", "JnSStBOne");
					skillList.put(serverInst.getKey()+"_JackFromSledgeTwoToBelt", "JnSStBTwo");
					skillList.put(serverInst.getKey()+"_BeltPauseOnSensor","BeltSensPause");
					skillList.put(serverInst.getKey()+"_BeltRunWithoutPauses", "BeltRunWOPause");
					break;
				//For creating the digital only tank server if there is time to implement that.
				case "VirtualTankServer":
					parentList.put(serverInst.getKey()+"_VirtualTankController", "PLC");
					parentList.put(serverInst.getKey()+"_ReactionTank", "ManipulatorDevice");
					componentList.put(serverInst.getKey()+"_OutletTankValve", "ManipulatorDevice");
					componentList.put(serverInst.getKey()+"_InletTankValve", "ManipulatorDevice");
					componentList.put(serverInst.getKey()+"_DispenseMaterial", "Method");
					break;
				default:
					System.out.println("An attempt to create a server with an invalid type was made.");
			}
			
			//Iterate through list of parents that should've been saved in maps.
			//Parents and their children are iterated separately due to strange behavior with timing and objects being created in the wrong order.
			//Create to create objects and add the requisite relations.
			for(Map.Entry<String,String> entry: parentList.entrySet()) {
				switch(entry.getValue()) {
					case "PLC":
						createdObjects.put(entry.getKey(), createPLC(entry.getKey(), topParent, ns, baseObjectType, funcGroupType));
						break;
					case "SkillBridge":
						//TODO: Update for multiple bridge support
						//bridgeMap.put(entry.getKey(), createSkillBridge(entry.getKey(), ns));
						spaceServer = createSkillBridge(entry.getKey(), ns, topParent);
						break;
					case "SoftwareComponent":
						createdObjects.put(entry.getKey(),createSoftwareComponent(entry.getKey(), topParent, typeMap.get(entry.getValue()), baseObjectType, organizesType, funcGroupType, ns, true));
						break;
					case "SensorComponent":
					case "ConveyorDevice":
					case "ManipulatorDevice":
						createdObjects.put(entry.getKey(),createDevice(entry.getKey(), topParent, typeMap.get(entry.getValue()), baseObjectType, funcGroupType, ns, true));
						break;
					default:
						System.out.println("A parent with an unintended type was attempted to be created.");
						logger.error("An attempt was made to create a parent object with an invalid type.");
						break;
				}
			}
			
			//Iterate through the list of child components and create them, adding references to parents
			//Done this way due to weird behavior with timing and the parent object being null.
			//Switch to select the server type that is created with the interior switch for creating the correct type of object.
			switch(serverInst.getValue()) {
			case "BeltServer":
				for(Map.Entry<String,String> entry: componentList.entrySet()) {
					switch(entry.getValue()) {
						case "BeltComponent":
							createdObjects.put(entry.getKey(),createDevice(entry.getKey(), createdObjects.get(serverInst.getKey()+"_Belt"), typeMap.get(entry.getValue()), baseObjectType, funcGroupType, ns, true));
							break;
						case "SoftwareComponent":
							if (entry.getKey().contains("DeviceSkills")) {
								createdObjects.put(entry.getKey(),createSoftwareComponent(entry.getKey(), topParent, typeMap.get(entry.getValue()), baseObjectType, organizesType, funcGroupType, ns, false));
								break;
							}
							else {
								createdObjects.put(entry.getKey(),createSoftwareComponent(entry.getKey(), topParent, typeMap.get(entry.getValue()), baseObjectType, organizesType, funcGroupType, ns, true));
								break;
							}
						case "outVar":
							createdVariables.put(entry.getKey(), createVariable(entry.getKey(), createdObjects.get(serverInst.getKey()+"_Belt"), boolType, hasOutputsType, ns));
							break;
						case "inVar":
							if (entry.getKey().equals("SomethingBySensor")) {
								createdVariables.put(entry.getKey(), createVariable(entry.getKey(), createdObjects.get(serverInst.getKey()+"_OpticalSensor"), boolType, hasInputsType, ns));
							}
							else {
								createdVariables.put(entry.getKey(), createVariable(entry.getKey(), createdObjects.get(serverInst.getKey()+"_Belt"), boolType, hasInputsType, ns));
							}
							break;
						case "Method":
							if(entry.getKey().equals("ToggleSensorReporting")) {
								createdMethods.put(entry.getKey(), createMethod(entry.getKey(), entry.getKey(), createdObjects.get(serverInst.getKey()+"_OpticalSensor"), ns, true));
							}
							else {
								createdMethods.put(entry.getKey(), createMethod(entry.getKey(), entry.getKey(), createdObjects.get(serverInst.getKey()+"_Belt"), ns, true));
							}
							break;
						default:
							System.out.println("\n\n A child with an invalid type was made: " + entry.getValue() + "\n\n");
							logger.error("An attempt was made to create a child with an invalid type.");
							break;
					}
				}
				break;
			case "JackandSledgeServer":
				for(Map.Entry<String,String> entry: componentList.entrySet()) {
					switch(entry.getValue()) {
						case "SensorComponent":
						case "GrippingComponent":
						case "PistonComponent":
						case "Sensorcomponent":
							createdObjects.put(entry.getKey(),createDevice(entry.getKey(), createdObjects.get(serverInst.getKey()+"_JackSledge"), typeMap.get(entry.getValue()), baseObjectType, funcGroupType, ns, true));
							break;
						case "SoftwareComponent":
						//createdObjects.put(entry.getKey(),createSoftwareComponent(entry.getKey(), createdObjects.get("JackSledge"), typeMap.get(entry.getValue()), baseObjectType, organizesType, funcGroupType, ns));
							if (entry.getKey().contains("DeviceSkills")) {
								createdObjects.put(entry.getKey(),createSoftwareComponent(entry.getKey(), topParent, typeMap.get(entry.getValue()), baseObjectType, organizesType, funcGroupType, ns, false));
								break;
							}
							else {
								createdObjects.put(entry.getKey(),createSoftwareComponent(entry.getKey(), topParent, typeMap.get(entry.getValue()), baseObjectType, organizesType, funcGroupType, ns, true));
								break;
							}
						case "outVar":
							createdVariables.put(entry.getKey(), createVariable(entry.getKey(), createdObjects.get(serverInst.getKey()+"_JackSledge"), boolType, hasOutputsType, ns));
							break;
						case "inVar":
							createdVariables.put(entry.getKey(), createVariable(entry.getKey(), createdObjects.get(serverInst.getKey()+"_JackSledge"), boolType, hasInputsType, ns));
							break;
						case "Method":
							createdMethods.put(entry.getKey(), createMethod(entry.getKey(), entry.getKey(), createdObjects.get(serverInst.getKey()+"_JackSledge"), ns, true));
							break;
						default:
							System.out.println("\n\n A child with an invalid type was made: " + entry.getValue() + "\n\n");
							logger.error("An attempt was made to create a child with an invalid type.");
							break;
					}
				}
				break;
			case "BeltandGripperServer":
				for(Map.Entry<String,String> entry: componentList.entrySet()) {
					switch(entry.getValue()) {
						case "gripSensorComponent":
							createdObjects.put(entry.getKey(),createDevice(entry.getKey(), createdObjects.get(serverInst.getKey()+"_PistonGripper"), typeMap.get("SensorComponent"), baseObjectType, funcGroupType, ns, true));
							break;
						case "PistonComponent":
						case "GrippingComponent":
							createdObjects.put(entry.getKey(),createDevice(entry.getKey(), createdObjects.get(serverInst.getKey()+"_PistonGripper"), typeMap.get(entry.getValue()), baseObjectType, funcGroupType, ns, true));
							break;
						case "beltSoftwareComponent":
							createdObjects.put(entry.getKey(),createSoftwareComponent(entry.getKey(), createdObjects.get(serverInst.getKey()+"_Belt"), typeMap.get("SoftwareComponent"), baseObjectType, organizesType, funcGroupType, ns, true));
							break;
						case "gripSoftwareComponent":
							createdObjects.put(entry.getKey(),createSoftwareComponent(entry.getKey(), createdObjects.get(serverInst.getKey()+"_PistonGripper"), typeMap.get("SoftwareComponent"), baseObjectType, organizesType, funcGroupType, ns, true));
							break;
						case "SoftwareComponent":
							if (entry.getKey().contains("DeviceSkills")) {
								createdObjects.put(entry.getKey(),createSoftwareComponent(entry.getKey(), topParent, typeMap.get(entry.getValue()), baseObjectType, organizesType, funcGroupType, ns, false));
								break;
							}
							else {
								createdObjects.put(entry.getKey(),createSoftwareComponent(entry.getKey(), topParent, typeMap.get(entry.getValue()), baseObjectType, organizesType, funcGroupType, ns, true));
								break;
							}
						case "BeltComponent":
							createdObjects.put(entry.getKey(),createDevice(entry.getKey(), createdObjects.get(serverInst.getKey()+"_Belt"), typeMap.get(entry.getValue()), baseObjectType, funcGroupType, ns, true));
							break;
						case "beltInVar":
							createdVariables.put(entry.getKey(), createVariable(entry.getKey(), createdObjects.get(serverInst.getKey()+"_Belt"), boolType, hasInputsType, ns));
							break;
						case "beltOutVar":
							createdVariables.put(entry.getKey(), createVariable(entry.getKey(), createdObjects.get(serverInst.getKey()+"_Belt"), boolType, hasOutputsType, ns));
							break;
						case "beltMethod":
							createdMethods.put(entry.getKey(), createMethod(entry.getKey(), entry.getKey(),createdObjects.get(serverInst.getKey()+"_Belt"), ns, true));
							break;
						case "gripInVar":
							createdVariables.put(entry.getKey(), createVariable(entry.getKey(), createdObjects.get(serverInst.getKey()+"_PistonGripper"), boolType, hasInputsType, ns));
							break;
						case "gripOutVar":
							createdVariables.put(entry.getKey(), createVariable(entry.getKey(), createdObjects.get(serverInst.getKey()+"_PistonGripper"), boolType, hasOutputsType, ns));
							break;
						case "gripMethod":
							createdMethods.put(entry.getKey(), createMethod(entry.getKey(), entry.getKey(), createdObjects.get(serverInst.getKey()+"_PistonGripper"), ns, true));
							break;
						case "sensMethod":
							createdMethods.put(entry.getKey(), createMethod(entry.getKey(), entry.getKey(),createdObjects.get(serverInst.getKey()+"_OpticalSensor"), ns, true));
							break;
						case "sensInVar":
							//System.out.println(entry.getKey());
							createdVariables.put(entry.getKey(), createVariable(entry.getKey(), createdObjects.get(serverInst.getKey()+"_OpticalSensor"), boolType, hasInputsType, ns));
							break;
						case "Method":
							createdMethods.put(entry.getKey(), createMethod(entry.getKey(), entry.getKey(),createdObjects.get(serverInst.getKey()+"_BeltGripperController"), ns, true));
							break;
						default:
							System.out.println("\n\n A child with an invalid type was made: " + entry.getValue() + "\n\n");
							logger.error("An attempt was made to create a child with an invalid type.");
							break;
					}
				}
				break;
			case "VirtualTankServer":
				for(Map.Entry<String,String> entry: componentList.entrySet()) {
					switch(entry.getValue()) {
					case "ManipulatorDevice":
						createdObjects.put(entry.getKey(),createDevice(entry.getKey(), createdObjects.get(serverInst.getKey()+"_ReactionTank"), typeMap.get(entry.getValue()), baseObjectType, funcGroupType, ns, true));
						break;
					case "Method":
						createdMethods.put(entry.getKey(), createMethod(entry.getKey(), entry.getKey(), createdObjects.get(serverInst.getKey()+"_ReactionTank"), ns, true));
						break;
					default:
						System.out.println("\n\n A child with an invalid type was made: " + entry.getValue() + "\n\n");
						logger.error("An attempt was made to create a child with an invalid type.");
						break;
					}
				}
				break;
			}
			//Make the skills separately due to additional issues with null references.
			for(Map.Entry<String,String> entry: skillList.entrySet()) {			
				createdSkills.put(entry.getValue(), createSkillObject(entry.getKey(), entry.getValue(), createdObjects.get(serverInst.getKey()+"_DeviceSkills"),typeMap.get("SkillObjectType"),ns));
			}		
			//Make the final references that could not be made during the loops because of fuckery.
			//Making sure variables are also linked to the relevant sub-device.
			//Without this step they would only be linked to the main device's functional group and paremeter set.
			//This would not reflect the reality and would lead to an incomplete model.
			for(Map.Entry<String,UaVariable> entry: createdVariables.entrySet()){
				switch(entry.getKey()) {
					case "MotorOutput":
						entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_MotorBelt"),"ParameterSet"), hasOutputsType.getNodeId(), true);
						entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_MotorBelt"),"FuncGroup"), Identifiers.Organizes, true);
						if (serverInst.getValue().equals("BeltServer")) {
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_BeltController"),"ParameterSet"), hasOutputsType.getNodeId(), true);
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_BeltController"),"FuncGroup"), Identifiers.Organizes, true);
						}
						else if (serverInst.getValue().equals("BeltandGripperServer")) {
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_BeltGripperController"),"ParameterSet"), hasOutputsType.getNodeId(), true);
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_BeltGripperController"),"FuncGroup"), Identifiers.Organizes, true);
						}
						break;
					case "Extend":
						entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_VertPiston"),"ParameterSet"), hasOutputsType.getNodeId(), true);
						entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_VertPiston"),"FuncGroup"), Identifiers.Organizes, true);
						if (serverInst.getValue().equals("JackandSlegeServer")) {
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_JackSledgeController"),"ParameterSet"), hasOutputsType.getNodeId(), true);
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_JackSledgeController"),"FuncGroup"), Identifiers.Organizes, true);
						}
						break;
					case "Down":
						entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_HorzPiston"),"ParameterSet"), hasOutputsType.getNodeId(), true);
						entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_HorzPiston"),"FuncGroup"), Identifiers.Organizes, true);
						if (serverInst.getValue().equals("BeltandGripperServer")) {
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_BeltGripperController"),"ParameterSet"), hasOutputsType.getNodeId(), true);
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_BeltGripperController"),"FuncGroup"), Identifiers.Organizes, true);
						}
						else if (serverInst.getValue().equals("JackandSlegeServer")) {
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_JackSledgeController"),"ParameterSet"), hasOutputsType.getNodeId(), true);
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_JackSledgeController"),"FuncGroup"), Identifiers.Organizes, true);
						}
						break;
					case "Grip":
						entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_Gripper"),"ParameterSet"), hasOutputsType.getNodeId(), true);
						entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_Gripper"),"FuncGroup"), Identifiers.Organizes, true);
						if (serverInst.getValue().equals("BeltandGripperServer")) {
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_BeltGripperController"),"ParameterSet"), hasOutputsType.getNodeId(), true);
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_BeltGripperController"),"FuncGroup"), Identifiers.Organizes, true);
						}
						break;
					case "Vacuum":
						entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_VacuumPump"),"ParameterSet"), hasOutputsType.getNodeId(), true);
						entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_VacuumPump"),"FuncGroup"), Identifiers.Organizes, true);
						if (serverInst.getValue().equals("JackandSlegeServer")) {
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_JackSledgeController"),"ParameterSet"), hasOutputsType.getNodeId(), true);
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_JackSledgeController"),"FuncGroup"), Identifiers.Organizes, true);
						}
						break;
					case "Sledge":
						entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_SledgePiston"),"ParameterSet"), hasOutputsType.getNodeId(), true);
						entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_SledgePiston"),"FuncGroup"), Identifiers.Organizes, true);
						if (serverInst.getValue().equals("JackandSlegeServer")) {
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_JackSledgeController"),"ParameterSet"), hasOutputsType.getNodeId(), true);
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_JackSledgeController"),"FuncGroup"), Identifiers.Organizes, true);
						}
						break;
					case "Top":
					case "Bottom":
						if(serverInst.getValue().equals("BeltandGripperServer")) {
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_PistonGripperSensors"),"ParameterSet"), hasInputsType.getNodeId(), true);
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_PistonGripperSensors"),"FuncGroup"), Identifiers.Organizes, true);
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_BeltGripperController"),"ParameterSet"), hasInputsType.getNodeId(), true);
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_BeltGripperController"),"FuncGroup"), Identifiers.Organizes, true);
						}
					case "Extended":
					case "Retracted":
						if(serverInst.getValue().equals("JackandSledgeServer")) {
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_JackSledgeSensors"),"ParameterSet"), hasInputsType.getNodeId(), true);
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_JackSledgeSensors"),"FuncGroup"), Identifiers.Organizes, true);
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_JackSledgeController"),"ParameterSet"), hasInputsType.getNodeId(), true);
							entry.getValue().addReference(getSubObject(createdObjects.get(serverInst.getKey()+"_JackSledgeController"),"FuncGroup"), Identifiers.Organizes, true);
						}
						break;
				}
			}
		}
		//Assign the created maps to their relevant places so they can be accessed by methods from other modules.
		this.spaceMethods = createdMethods;
		this.spaceObjects = createdObjects;
		this.spaceVariables = createdVariables;
		this.spaceSkills = createdSkills;
	}
	
	//Utility function pile
	//First set of utility functions for creating various types of address space object.
	private UaObjectNode createDevice(String name, UaNode parent, UaObjectType objType, UaType baseObjType, 
			UaType funcGroupType, int ns, boolean makeGroups) {
			//Create the device ID, device object and add a reference to the deviceset parent.
			final NodeId newDeviceId = new NodeId(ns, name);
			DeviceTypeNode newDevice = this.createInstance(DeviceTypeNode.class, 
					newDeviceId, new QualifiedName(ns, name),new LocalizedText(name,Locale.ENGLISH));
			newDevice.setTypeDefinition(objType);
			//Add relation between the new device and its parent
			try {
				if (parent.getBrowseName().getName().equals("DeviceSet")) {
					parent.addReference(newDevice, Identifiers.Organizes, false);
					parent.addReference(newDevice, Identifiers.HasNotifier, false);
				}
				else {
					parent.addReference(newDevice, Identifiers.HasComponent, false);
				}
			}catch (Exception e) {
				logger.info("Something fucky happened: " + e);
				System.out.println("Something happened: " + e);
			}
			//Create the functional group, methodset and parameterset
			if (makeGroups) {
				final NodeId funcGroupId = new NodeId(ns, name+"FuncGroup");
				UaObjectNode funcGroup = new UaObjectNode(this,funcGroupId,name+"FuncGroup",Locale.ENGLISH);
				funcGroup.setTypeDefinition(funcGroupType);
				final NodeId paramSetId = new NodeId(ns, name+"ParameterSet");
				final NodeId methodSetId = new NodeId(ns, name+"MethodSet");
				UaObjectNode paramSet = new UaObjectNode(this, paramSetId, "ParameterSet",Locale.ENGLISH);
				UaObjectNode methodSet = new UaObjectNode(this, methodSetId,"MethodSet",Locale.ENGLISH);
				paramSet.setTypeDefinition(baseObjType);
				methodSet.setTypeDefinition(baseObjType);
				try {
					newDevice.addReference(funcGroup, Identifiers.HasComponent, false);
					newDevice.addReference(paramSet, Identifiers.HasComponent, false);
					newDevice.addReference(methodSet, Identifiers.HasComponent, false);
				}catch(Exception e) {
					logger.info("Something fucky happened: " + e);
					System.out.println("Something happened: " + e);
				}
			}
			//Set the device info for the device.
			newDevice.setManufacturer(new LocalizedText("Aalto Project",Locale.ENGLISH));
			newDevice.setModel(new LocalizedText(name,Locale.ENGLISH));
			newDevice.setSerialNumber("1111");
			newDevice.setHardwareRevision("1.0");
			newDevice.setSoftwareRevision("1.0");
			newDevice.setDeviceRevision("1.0");
		return newDevice;
	}
	private UaObjectNode createSoftwareComponent(String name, UaNode parent, UaObjectType objType, UaType baseObjType, 
			UaType parentRelation, UaType funcGroupType, int ns, Boolean createSets) {
		//Create a new nodeId for the software component and then create the node itself
		final NodeId newSoftwareId = new NodeId(ns, name);
		SoftwareTypeNode newSoftwareNode = this.createInstance(SoftwareTypeNode.class, 
				newSoftwareId, new QualifiedName(ns, name),new LocalizedText(name,Locale.ENGLISH));
		newSoftwareNode.setTypeDefinition(objType);
		//add a relation between the software node and its parent
		try {
			if (parent.getBrowseName().getName().equals("DeviceSet")) {
				parent.addReference(newSoftwareNode, Identifiers.Organizes, false);
				parent.addReference(newSoftwareNode, Identifiers.HasNotifier, false);
			}
			else {
				parent.addReference(newSoftwareNode, Identifiers.HasComponent, false);
			}
		}catch (Exception e) {
			logger.info("Something fucky happened: " + e);
			System.out.println("Something happened: " + e);
		}
		if (createSets) {
			//Create functional group, method set and parameter set and add relations
			final NodeId funcGroupId = new NodeId(ns, name+"FuncGroup");
			UaObjectNode funcGroup = new UaObjectNode(this,funcGroupId,name+"FuncGroup",Locale.ENGLISH);
			funcGroup.setTypeDefinition(funcGroupType);
			final NodeId paramSetId = new NodeId(ns, name+"ParameterSet");
			final NodeId methodSetId = new NodeId(ns, name+"MethodSet");
			UaObjectNode paramSet = new UaObjectNode(this, paramSetId, "ParameterSet",Locale.ENGLISH);
			UaObjectNode methodSet = new UaObjectNode(this, methodSetId,"MethodSet",Locale.ENGLISH);
			paramSet.setTypeDefinition(baseObjType);
			methodSet.setTypeDefinition(baseObjType);
			try {
				newSoftwareNode.addReference(funcGroup, Identifiers.HasComponent, false);
				newSoftwareNode.addReference(paramSet, Identifiers.HasComponent, false);
				newSoftwareNode.addReference(methodSet, Identifiers.HasComponent, false);
			}catch(Exception e) {
				logger.info("Something fucky happened: " + e);
				System.out.println("Something happened: " + e);
			}
		}
		//Set the software info
		newSoftwareNode.setManufacturer(new LocalizedText("Aalto Project", Locale.ENGLISH));
		newSoftwareNode.setSoftwareRevision("1.0");
		newSoftwareNode.setModel(new LocalizedText(name+" Skills Container", Locale.ENGLISH));
		return newSoftwareNode;
	}
	private UaVariable createVariable(String name, UaNode parent, UaType varType, UaType relationType, int ns) {
		//Create the NodeId and the node.
		final NodeId varId = new NodeId(ns, name);
		PlainVariable<Boolean> newVar = new PlainVariable<Boolean>(this, varId, name, Locale.ENGLISH);
		//Set the data type for the variable. As all variable in the system are boolean we can just assume this.
		newVar.setDataTypeId(Identifiers.Boolean);
		newVar.setTypeDefinition(varType);
		//Add references to the parameter set and functional group
		newVar.addReference(getSubObject(parent,"ParameterSet").getNodeId(), relationType.getNodeId(), true);
		newVar.addReference(getSubObject(parent,"FuncGroup").getNodeId(), Identifiers.Organizes, true);
		return newVar;
	}
	private UaMethod createMethod(String fullName, String simpleName, UaNode parent, int ns, boolean hasSets) throws StatusException {
		//Create the NodeId and new method node
		final NodeId methodId = new NodeId(ns, fullName);
		PlainMethod newMethod = new PlainMethod(this, methodId, simpleName, Locale.ENGLISH);
		//Add input and output arguments.
		Argument[] inputs = new Argument[1];
		inputs[0] = new Argument();
		inputs[0].setName(fullName + "_InputInfo");
		inputs[0].setDataType(Identifiers.String);
		inputs[0].setValueRank(ValueRanks.Any);
		inputs[0].setArrayDimensions(null);
	    inputs[0].setDescription(new LocalizedText("Any potential input information.", Locale.ENGLISH));
	    newMethod.setInputArguments(inputs);
		Argument[] outputs = new Argument[1];
		outputs[0] = new Argument();
		outputs[0].setName(fullName + "_ReturnInfo");
		outputs[0].setDataType(Identifiers.String);
		outputs[0].setValueRank(ValueRanks.Any);
		outputs[0].setArrayDimensions(null);
		outputs[0].setDescription(new LocalizedText("Any resulting info from server method handler.", Locale.ENGLISH));
		newMethod.setOutputArguments(outputs);
		//Create the listener to hand
		myDevicetMethodManagerListener = new ModularDeviceMethodManagerListener(newMethod, this);
		MethodManagerUaNode m = (MethodManagerUaNode) this.getMethodManager();
		m.addCallListener(myDevicetMethodManagerListener);
		//Add references to the parameter set and functional group.
		if (hasSets) {
			newMethod.addReference(getSubObject(parent,"MethodSet").getNodeId(), Identifiers.HasComponent, true);
			newMethod.addReference(getSubObject(parent,"FuncGroup").getNodeId(), Identifiers.Organizes, true);
		}
		else {
			newMethod.addReference(parent, Identifiers.HasComponent, true);
		}
		//Set the method to be executable
		newMethod.setUserExecutable(true);
		return newMethod;
	}
	private void createStateEventNode(String name, int ns, UaNode source) throws StatusException {
		//Create the event type since it is more involved than other types.
		final NodeId eventTypeId = new NodeId(ns, MyStateEvent.MY_EVENT_ID);
		UaObjectTypeNode stateEventType = new UaObjectTypeNode (this, eventTypeId, "MyStateEvent", LocalizedText.NO_LOCALE);
		getServer().getNodeManagerRoot().getType(Identifiers.BaseEventType).addSubType(stateEventType);
		
		//Create the state machine name variable
		NodeId myMachineVarId = new NodeId(ns, MyStateEvent.MY_MACHINE_ID);
		PlainVariable<String> myMachineVar = new PlainVariable<String>(this, myMachineVarId,
				MyStateEvent.MY_MACHINE_NAME, LocalizedText.NO_LOCALE);
		myMachineVar.setDataTypeId(Identifiers.String);
		myMachineVar.addModellingRule(ModellingRule.Mandatory);
		stateEventType.addComponent(myMachineVar);
		
		//Create the state number variable
		NodeId myStateVarId = new NodeId(ns, MyStateEvent.MY_STATE_ID);
		PlainVariable<Integer> myStateVar = new PlainVariable<Integer>(this, myStateVarId,
				MyStateEvent.MY_STATE_NAME, LocalizedText.NO_LOCALE);
		myStateVar.setDataTypeId(Identifiers.Integer);
		myStateVar.addModellingRule(ModellingRule.Mandatory);
		stateEventType.addComponent(myStateVar);
		
		//Register the class
		getServer().registerClass(MyStateEvent.class, eventTypeId);
		
		//Configure the nodebuilder
		//I have no idea why or how but it fixed opc ua crying about fatal errors.
		ExpandedNodeId expEventId = server.getNamespaceTable().toExpandedNodeId(eventTypeId);
		TypeDefinitionBasedNodeBuilderConfiguration.Builder conf = TypeDefinitionBasedNodeBuilderConfiguration.builder();
		conf.addOptional(UaBrowsePath.from(expEventId, UaQualifiedName.standard("EventId")));
		conf.addOptional(UaBrowsePath.from(expEventId, UaQualifiedName.standard(MyStateEvent.MY_MACHINE_NAME)));
		conf.addOptional(UaBrowsePath.from(expEventId, UaQualifiedName.standard(MyStateEvent.MY_STATE_NAME)));
		this.setNodeBuilderConfiguration(conf.build());
		
		//Add the type to the general map of types
		typeMap.put("MyStateEvent", stateEventType);
	}
	private UaObjectNode createPLC(String name, UaNode parent, int ns, UaType baseObjectType, UaType funcGroupType) {
		//This function creates the address space objects for the PLC.
		//The common nodes (MethodSet, ParameterSet, various info nodes) aren't created
		//in a loop simply because the structure would be a pain.
		//The block nodes representations are however.
		
		//Create some base line connections and objects
		//This basically covers the left half of the PLC diagram (see drive)
		//parent.addReference(deviceSet, Identifiers.HasNotifier, false);
		
		NodeId cfgId = new NodeId(ns, name+"_CtrlConfigurationType");
		//CtrlConfigurationTypeNode newNode = new CtrlConfigurationTypeNode(this, tempId, new QualifiedName(ns, "CtrlConfigurationType"), new LocalizedText("CtrlConfigurationType", Locale.ENGLISH));
		CtrlConfigurationTypeNode ctrlCfgNode = this.createInstance(CtrlConfigurationTypeNode.class, cfgId, 
				new QualifiedName(ns, name+"_CtrlConfigurationType"), new LocalizedText(name+"_CtrlConfigurationType", Locale.ENGLISH));
		parent.addReference(ctrlCfgNode, Identifiers.HasNotifier, false);
		
		NodeId cfgPId= new NodeId(ns, name+"_CfgParameterSet");
		BaseObjectTypeNode cfgParamSet = this.createInstance(BaseObjectTypeNode.class, cfgPId, 
				new QualifiedName(ns, name+"_CfgParameterSet"), new LocalizedText (name+"_CfgParameterSet", Locale.ENGLISH));
		ctrlCfgNode.addReference(cfgParamSet, Identifiers.HasComponent, false);
		
		NodeId cfgVarsId = new NodeId(ns, name+"_ConfigVars");
		FunctionalGroupTypeNode cfgVars = this.createInstance(FunctionalGroupTypeNode.class, cfgVarsId, 
				new QualifiedName(ns, name+"_ConfigVars"), new LocalizedText(name+"_ConfigVars", Locale.ENGLISH));
		ctrlCfgNode.addReference(cfgVars, Identifiers.HasComponent, false);
		
		NodeId resId = new NodeId(ns, name+"_Resources");
		ConfigurableObjectTypeNode resource = this.createInstance(ConfigurableObjectTypeNode.class, resId,
				new QualifiedName(ns, name+"_Resources"), new LocalizedText(name+"_Resources", Locale.ENGLISH));
		ctrlCfgNode.addReference(resource, Identifiers.HasComponent, false);
		
		//Create the device type object for the PLC itself
		UaObjectNode plc = createDevice(name+"_PLC", parent, typeMap.get("Iceblock"), baseObjectType, funcGroupType, ns, false);
		plc.addReference(resource, Identifiers.HasComponent, false);
		//Create the PLC Methodset, Parameterset and configuration group.
		final NodeId funcGroupId = new NodeId(ns, name+"_FuncGroup");
		UaObjectNode funcGroup = new UaObjectNode(this,funcGroupId,name+"_FuncGroup",Locale.ENGLISH);
		funcGroup.setTypeDefinition(funcGroupType);
		final NodeId paramSetId = new NodeId(ns, name+"_ParameterSet");
		final NodeId methodSetId = new NodeId(ns, name+"_MethodSet");
		UaObjectNode paramSet = new UaObjectNode(this, paramSetId, name+"_ParameterSet",Locale.ENGLISH);
		UaObjectNode methodSet = new UaObjectNode(this, methodSetId,name+"_MethodSet",Locale.ENGLISH);
		paramSet.setTypeDefinition(baseObjectType);
		methodSet.setTypeDefinition(baseObjectType);
		try {
			plc.addReference(funcGroup, Identifiers.HasComponent, false);
			plc.addReference(paramSet, Identifiers.HasComponent, false);
			plc.addReference(methodSet, Identifiers.HasComponent, false);
		}catch(Exception e) {
			logger.info("Something fucky happened: " + e);
			System.out.println("Something happened: " + e);
		}
		
		//Create the Blocks parent object
		NodeId blocksId = new NodeId(ns, name+"_Blocks");
		ConfigurableObjectTypeNode blocks = this.createInstance(ConfigurableObjectTypeNode.class, blocksId,
				new QualifiedName(ns, name+"_Blocks"), new LocalizedText(name+"_Blocks", Locale.ENGLISH));
		plc.addReference(blocks, Identifiers.HasComponent, false);
		
		//Create the specific contents for those sets based on which PLC is in question.
		//Will eventually also set the alarm type nodes and configvars
		//Probably, those are not known at this second.
		try {	
			createMethod(name+"_StartPLC", name+"_StartPLC", plc, ns, true);
			createMethod(name+"_StopPLC", name+"_StopPLC", plc, ns, true);
			createMethod(name+"_ResetPLC", name+"_ResetPLC", plc, ns, true);
		}catch(Exception e) {
			logger.info("Something fucky happened: " + e);
			System.out.println("Something happened: " + e);
		}
		
		//Create all the blocks themselves
		//TODO: Forgot how to get DCSFunctionBlockType put in
		
		return plc;
	}
	private TcpIOServer createSkillBridge(String name, int ns, UaNode parent) {
		int tempPort = 5000+portIterator;
		//One option is to allow the ports for the tcp io server sockets to be set manually.
		//Alternatively they can just be set here while hoping for the best.
		
		System.out.print("Enter a port for the TCP IO Server: ");
		try {
			tempPort = Integer.parseInt(readInput());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		TcpIOServer tempServer =  new TcpIOServer(tempPort, this);
		tempServer.start();
		portIterator+=1;
		return tempServer;
	}
	private UaObjectNode createSkillObject(String fullName, String simpleName, UaNode parent, UaObjectType objType, int ns) throws StatusException{
		//Create the skill's nodeId and the skill object itself.
		final NodeId tempSkillId = new NodeId(ns, fullName);
		//SkillObject  tempSkill = this.createInstance(typeMap.get("SkillObjectType"), tempSkillId, new QualifiedName (ns, name), new LocalizedText(name, Locale.ENGLISH));
		UaObjectNode tempSkill = new UaObjectNode (this, tempSkillId, new QualifiedName (ns, simpleName),
				new LocalizedText (simpleName, Locale.ENGLISH));
		tempSkill.setTypeDefinition(objType);
		
		//Add a reference to the parent DeviceSkills object
		parent.addReference(tempSkill, Identifiers.HasComponent, false);
		
		//Create the method nodes as per architecture proposal
		createMethod(fullName + "_Start", simpleName + "_Start", tempSkill, ns, false);
		createMethod(fullName + "_Suspend", simpleName + "_Suspend", tempSkill, ns, false);
		createMethod(fullName + "_Resume", simpleName + "_Resume", tempSkill, ns, false);
		createMethod(fullName + "_Stop", simpleName + "_Stop", tempSkill, ns, false);
		createMethod(fullName + "_Reset", simpleName + "_Reset", tempSkill, ns, false);
		
		//Create the property for showing the state of the skill.
		createProperty(ns, fullName+"_SkillState", simpleName+"_SkillState", tempSkill, "Ready");
		
		return tempSkill;
	}
	
	//Utility function for sending an event to clients.
	//Overloaded so that either state change, call return or generic messages can be sent.
	public void sendEvent(String skillName, String stateString) throws StatusException{
		//Create the event and set the information.
		final NodeId myEventNodeId = new NodeId(this.getNamespaceIndex(), "test.Events");
		MyStateEvent newEvent = this.createInstance(MyStateEvent.class,myEventNodeId,
				new QualifiedName(this.getNamespaceIndex(),"test.Events"), new LocalizedText ("test.Events", Locale.ENGLISH));
		newEvent.setTypeDefinition(typeMap.get("MyStateEvent"));
		newEvent.setMessage(new LocalizedText("StateChange;"+skillName+";"+stateString));
		//newEvent.setStateNumVariable(stateNum);
		newEvent.setMachineNameVariable(skillName);
		
		//Set the information for the event
		final DateTime time = DateTime.currentTime();
		ByteString newEventId = myDeviceEventManagerListener.getNextUserEventId();
		newEvent.triggerEvent(time, time, newEventId);
	}
	public void sendEvent(String skillName, String callType, String callResult) throws StatusException {
		final NodeId myEventNodeId = new NodeId(this.getNamespaceIndex(), "test.Events");
		MyStateEvent newEvent = this.createInstance(MyStateEvent.class,myEventNodeId,
				new QualifiedName(this.getNamespaceIndex(),"test.Events"), new LocalizedText ("test.Events", Locale.ENGLISH));
		newEvent.setTypeDefinition(typeMap.get("MyStateEvent"));
		newEvent.setMessage(new LocalizedText("CallReturn;"+skillName+";"+callType+";"+callResult));
		newEvent.setMachineNameVariable(skillName);
		
		//Set the information for the event
		final DateTime time = DateTime.currentTime();
		ByteString newEventId = myDeviceEventManagerListener.getNextUserEventId();
		newEvent.triggerEvent(time, time, newEventId);
	}
	public void sendEvent(String message) throws StatusException{
		final NodeId myEventNodeId = new NodeId(this.getNamespaceIndex(), "test.Events");
		MyStateEvent newEvent = this.createInstance(MyStateEvent.class,myEventNodeId,
				new QualifiedName(this.getNamespaceIndex(),"test.Events"), new LocalizedText ("test.Events", Locale.ENGLISH));
		newEvent.setTypeDefinition(typeMap.get("MyStateEvent"));
		newEvent.setMessage(new LocalizedText("ServerMessage;"+message));
		newEvent.setMachineNameVariable("Null");
		
		//Set the information for the event
		final DateTime time = DateTime.currentTime();
		ByteString newEventId = myDeviceEventManagerListener.getNextUserEventId();
		newEvent.triggerEvent(time, time, newEventId);
	}
	
	//Utility methods for creating properties for objects.
	//Overloaded in order to allow the PlainProperty variable to be typed correctly.
	private PlainProperty<UnsignedInteger> createProperty(int ns, String name, UaNode parent, UnsignedInteger value) {
		final NodeId propId = new NodeId(ns,name+parent.getBrowseName().getName());
		PlainProperty<UnsignedInteger> newProperty = new PlainProperty<UnsignedInteger>(this,propId,name,Locale.ENGLISH);
		newProperty.setCurrentValue(value);
		newProperty.addReference(parent, Identifiers.HasProperty, true);
		return newProperty;
	}
	private PlainProperty<Boolean> createProperty(int ns, String name, UaNode parent, Boolean value) {
		final NodeId propId = new NodeId(ns,name+parent.getBrowseName().getName());
		PlainProperty<Boolean> newProperty = new PlainProperty<Boolean>(this,propId,name,Locale.ENGLISH);
		newProperty.setCurrentValue(value);
		newProperty.addReference(parent, Identifiers.HasProperty, true);
		return newProperty;
	}
	private PlainProperty<Integer> createProperty(int ns, String name, UaNode parent, Integer value) {
		final NodeId propId = new NodeId(ns,name+parent.getBrowseName().getName());
		PlainProperty<Integer> newProperty = new PlainProperty<Integer>(this,propId,name,Locale.ENGLISH);
		newProperty.setCurrentValue(value);
		newProperty.addReference(parent, Identifiers.HasProperty, true);
		return newProperty;
	}
	@SuppressWarnings("unused")
	private PlainProperty<String> createProperty(int ns, String name, UaNode parent, String value) {
		final NodeId propId = new NodeId(ns,name+parent.getBrowseName().getName());
		PlainProperty<String> newProperty = new PlainProperty<String>(this,propId,name,Locale.ENGLISH);
		newProperty.setCurrentValue(value);
		newProperty.addReference(parent, Identifiers.HasProperty, true);
		return newProperty;
	}
	private PlainProperty<String> createProperty(int ns, String fullName ,String simpleName, UaNode parent, String value) {
		final NodeId propId = new NodeId(ns,fullName+"_"+parent.getBrowseName().getName());
		PlainProperty<String> newProperty = new PlainProperty<String>(this,propId,simpleName,Locale.ENGLISH);
		newProperty.setCurrentValue(value);
		newProperty.addReference(parent, Identifiers.HasProperty, true);
		return newProperty;
	}
	
	//A small utility function for fetching a subtype of a type
	private static UaType getSubType(UaType baseType, String target) {
		UaReference[] refs = baseType.getReferences(Identifiers.HasSubtype, false);
		
		for (UaReference tempRef:refs) {
			UaNode type = tempRef.getTargetNode();
			if (type.getBrowseName().getName().equals(target)) {
				return (UaType) type;
			}
		}
		logger.error("Failed to find target subtype.");
		return null;
	}
	//Small utility functions for doing things. Identical to similar utility functions elsewhere
	private static UaObjectNode getSubObject(UaNode baseObject, String targetName) {
		UaReference[] refs = baseObject.getReferences();
		
		for (UaReference tempRef:refs) {
			UaNode target = tempRef.getTargetNode();
			if (target.getBrowseName().getName().contains(targetName)) {
				return (UaObjectNode) target;
			}
		}
		logger.error("Failed to find target subnode.");
		System.out.println("Failed to find target subnode.");
		return null;
	}
	private static String readInput() {
	    BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
	    String s = null;
	    do {
	      try {
	        s = stdin.readLine();
	      } catch (IOException e) {
	        e.printStackTrace();
	      }
	    } while ((s == null) || (s.length() == 0));
	    return s;
	  }
}
