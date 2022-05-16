/**
 * The main file for the control client.
 * Contains the setup, initialization and client main.
 * Actual control logic and event message parsing handled by own class.
 * 
 * This implementation is based on the principles demonstrated in the sample code provided
 * with the SDK as that was suggested as a way of guaranteeing things work.
 */
package com.SkillsProject.ControlClient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prosysopc.ua.ApplicationIdentity;
import com.prosysopc.ua.ContentFilterBuilder;
import com.prosysopc.ua.SecureIdentityException;
import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.client.AddressSpace;
import com.prosysopc.ua.client.MonitoredEventItem;
import com.prosysopc.ua.client.ServerStatusListener;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.SubscriptionAliveListener;
import com.prosysopc.ua.client.SubscriptionNotificationListener;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.client.UaClientListener;
import com.prosysopc.ua.nodes.UaMethod;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaReference;
import com.prosysopc.ua.server.nodes.UaObjectNode;
import com.prosysopc.ua.stack.builtintypes.ExpandedNodeId;
import com.prosysopc.ua.stack.builtintypes.LocalizedText;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.QualifiedName;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.builtintypes.Variant;
import com.prosysopc.ua.stack.cert.PkiDirectoryCertificateStore;
import com.prosysopc.ua.stack.common.NamespaceTable;
import com.prosysopc.ua.stack.common.ServiceResultException;
import com.prosysopc.ua.stack.core.ApplicationDescription;
import com.prosysopc.ua.stack.core.ApplicationType;
import com.prosysopc.ua.stack.core.Attributes;
import com.prosysopc.ua.stack.core.EventFilter;
import com.prosysopc.ua.stack.core.Identifiers;
import com.prosysopc.ua.stack.core.ReferenceDescription;
import com.prosysopc.ua.stack.core.SimpleAttributeOperand;
import com.prosysopc.ua.stack.transport.security.SecurityMode;

public class ControlClientMain {
	//Initialize the logger object for diagnostics logging matters.
	private static final Logger logger = LoggerFactory.getLogger(ControlClientMain.class);
	
	//Set the application name
	protected static String APP_NAME = "CentralControlClient";
	
	//Implementation as per provided examples.
		protected final QualifiedName[] eventFieldNames =
		      {new QualifiedName("EventType"), new QualifiedName("Message"), new QualifiedName("SourceName"),
		          new QualifiedName("Time"), new QualifiedName("Severity"), new QualifiedName("ActiveState/Id")};
	
	//Maps of things
	protected Map<Integer, UaClient> multiClientMap = new HashMap <Integer, UaClient>();
	protected Map<Integer, String> clientNumAddMap = new HashMap <Integer, String>();
	protected Map<Integer, Subscription> subMap = new HashMap <Integer, Subscription>();
	protected Map<Integer, SubscriptionAliveListener> subAliveMap = new HashMap <Integer, SubscriptionAliveListener>();
	protected Map<Integer, SubscriptionNotificationListener> subNotMap = new HashMap<Integer, SubscriptionNotificationListener>();
	//protected List<String> monitoredItems = new ArrayList<String>();
	//Other variables and things
	protected int sessionCount = 0;
	protected int serverCount;
	protected MasterControlProgram MCP = new MasterControlProgram(this);
	protected Thread MCPThread = new Thread(MCP);
	final PkiDirectoryCertificateStore certStore = new PkiDirectoryCertificateStore();
	protected EventFilter baseFilter;
	protected static Boolean autopilot;
	
	//Listener and monitor objects
	protected controlEventListener myEventListener = new controlEventListener(this);
	protected UaClientListener myClientListener = new controlClientListener();
	protected ServerStatusListener myServerStatusListener = new controlServerStatusListener();
	//protected SubscriptionAliveListener subAliveListener = new mySubAliveListener();
	//protected SubscriptionNotificationListener subListener = new mySubListener();
	//TODO: MonitoredDataItemListener
	//TODO: ServerStatusListener
	//TODO: SubscriptionListener
	//TODO: SubscriptionAliveListener
	
	//The main function and other primary functions.
	public static void main(String[] args) throws Exception {
		ControlClientMain ctrlClient = new ControlClientMain();
		
		do {
			System.out.println (" Input 1 for controller auto-run \n Input 2 for manual skill control");
			try {
				String action = readInput();
				if (action.toLowerCase().equals("1")) {
					autopilot = true;
					break;
				}
				if (action.toLowerCase().equals("2")) {
					autopilot = false;
					break;
				}
			} catch (Exception e) {
				System.out.println(e);
			}
			
		}while(true);
		//Initializes everything after which it enters one of the control loops.
		ctrlClient.initialize(autopilot);
		if (autopilot) {
			ctrlClient.autoLoop();
		}
		else if (!autopilot) {
			ctrlClient.manualLoop();
		}
		else {
			System.out.println("Something bad happened and neither control option started.");
			throw(new Exception("Fuckery happened"));
		}
		
		System.out.println("Closing: " + APP_NAME);
		logger.info("Control client " + APP_NAME + " closed.");
	}
	protected void initialize(Boolean autoRun) throws SecureIdentityException, IOException, ServiceException, StatusException {
		logger.info("Initializing control client");
		//Get the server number and addresses.
		getUserConfigInputs();
		
		//Set the application description.
		//This app description is sent to the server.
		ApplicationDescription appDesc = new ApplicationDescription();
		appDesc.setApplicationName(new LocalizedText(APP_NAME + "@localhost", Locale.ENGLISH));
		appDesc.setApplicationUri("urn:localhost:SkillsProject:"+APP_NAME);
		appDesc.setProductUri("urn:prosysopc.com:OPCUA:"+APP_NAME);
		appDesc.setApplicationType(ApplicationType.Client);
		
		File privatePath = new File(certStore.getBaseDir(), "private");
	    int[] keySizes = null;
	    final ApplicationIdentity identity = ApplicationIdentity.loadOrCreateCertificate(appDesc,
	            "Sample Organisation", /* Private Key Password, optional */"opcua", /* Key File Path */privatePath,
	            /* CA certificate & private key, optional */null,
	            /* Key Sizes for instance certificates to create, optional */keySizes,
	            /* Enable renewing the certificate */true);
	    
	    /*if (sub == null) {
			sub = new Subscription();
			sub.addAliveListener(subAliveListener);
			sub.addNotificationListener(subListener);
		}*/
	    
		//Identity matters. Taken directly from samples.
		//Taken directly because when asking about this they just said to do it like they did
		// in order to make sure it works. Also not in strict scope for the thesis so simple it is.
		
		//Create and configure the client objects and put them into a map for later access.
		UaClient tempClient;
		Subscription tempSub;
		SubscriptionAliveListener tempSubAlive;
		SubscriptionNotificationListener tempSubNot;
		for(Map.Entry<Integer,  String> entry : clientNumAddMap.entrySet()) {
			tempClient = new UaClient(entry.getValue());
			tempSub = new Subscription();
			tempSubAlive = new mySubAliveListener();
			tempSubNot = new mySubListener();
			tempSub.addAliveListener(tempSubAlive);
			tempSub.addNotificationListener(tempSubNot);
			
			subMap.put(entry.getKey(), tempSub);
			subNotMap.put(entry.getKey(), tempSubNot);
			subAliveMap.put(entry.getKey(), tempSubAlive);
			
			tempClient.setListener(myClientListener);
			tempClient.setLocale(Locale.ENGLISH);
			tempClient.setApplicationIdentity(identity);
			tempClient.setTimeout(30000);
			tempClient.setStatusCheckTimeout(10000);
			tempClient.addServerStatusListener(myServerStatusListener);
			tempClient.setSecurityMode(SecurityMode.NONE);
			tempClient.addSubscription(tempSub);
			
			multiClientMap.put(entry.getKey(), tempClient);
		}
		//Start the controller thread.
		myEventListener.toggleAutoState(autoRun);
		if (autoRun) {
			MCPThread.start();
		}
	}
	protected void autoLoop() throws ServiceException, StatusException, ServiceResultException {
		//Do all the connecting an initial nonsense.
		for(Map.Entry<Integer, UaClient> entry: multiClientMap.entrySet()) {
			connect(entry.getValue());
		}
		//Monitor the servers for events
		subToServerEvents();
		
		//Pass clients to control program
		MCP.passClients();
		
		//Handle user inputs over the program as well as the general looping through of it.
		do {
			System.out.println ("input \"Test\" to shut down. \n"
					+ "Input \"Boop\" to shut down just the control worker thread. \n"
					+ "Input \"SkillRecipe,MakeProductOne\" or \"SkillRecipe;MakeProductTwo\" to activate skill recipes.");
			try {
				String action = readInput();
				if (action.toLowerCase().equals("test")) {
					System.out.println("Shutting everything down.");
					MCP.shutdownMCP();
					break;
				}
				else if (action.toLowerCase().equals("boop")){
					MCP.userInput("Shutdown");
				}
				else if (action.replaceAll("[() ]", "").split(",").length == 2 || action.replaceAll("[() ]", "").split(";").length == 3) {
					MCP.userInput(action);
				}
				else {
					System.out.println("Invalid input");
					continue;
				}
			} catch (Exception e) {
				System.out.println(e);
			}
		}while (true);
		for(Map.Entry<Integer, UaClient> entry: multiClientMap.entrySet()) {
			disconnect(entry.getValue());
		}
		//In case something happened and the loop broke out somehow unintentionally.
		MCP.shutdownMCP();
	}
	protected void manualLoop() throws Exception {
		//Used to track what state the manual control machine is in.
		//1: Start/top level, select the server and get skills
		//2: Select a skill on the server
		int manualState=1;
		UaClient tmpClient = null;
		AddressSpace tmpSpace = null;
		List<ReferenceDescription> refDescs = null;
		List<UaMethod> skillMethods = null;
		ArrayList<ReferenceDescription> skillRefs = null;
		NamespaceTable tmpTable = null;
		UaObjectNode tmpNode;
		NodeId tmpId = null;
		NodeId skillId = null;
		UaMethod methodToCall = null;
		
		for(Map.Entry<Integer, UaClient> entry: multiClientMap.entrySet()) {
			//Connect to the chosen server
			tmpClient = entry.getValue();
			connect(tmpClient);
			tmpClient.getAddressSpace().setMaxReferencesPerNode(1000);
			tmpClient.getAddressSpace().setReferenceTypeId(Identifiers.HierarchicalReferences);
		}
		
		//Subscribe for events
		subToServerEvents();
		
		System.out.println("Enter commands for manual control as a string.");
		do {
			try {
				System.out.println("Enter \"boop\" to exit program.\nEnter \"back\" to go up a layer in the tree.");
				//Switch for what information to print.
				switch (manualState) {
					case 1:
						System.out.println("Select a server from the following:");
						for(Map.Entry<Integer, UaClient> entry: multiClientMap.entrySet()) {
							System.out.println(entry.getKey() + " - " +entry.getValue().getAddress());
						}
						break;
					case 2:
						System.out.println("Select a Skill from the following:");
						for(ReferenceDescription tempRef:skillRefs) {
							System.out.println(skillRefs.indexOf(tempRef) + " : " + tempRef.getDisplayName());
							//System.out.println(tempRef.getDisplayName());
						}
						break;
					case 3:
						System.out.println("Select a method from the following:");
						for (UaMethod tempMeth:skillMethods) {
							System.out.println(skillMethods.indexOf(tempMeth) + " : " + tempMeth.getDisplayName());
							//System.out.println(tempMeth.getDisplayName());
						}
						break;
					default:
						System.out.println("Invalid state for some reason.");
						break;
				}
				
				//System.out.println("State: " + manualState);
				//Read the user input for action to take.
				String action = readInput();
				
				//For going up a layer.
				if (action.toLowerCase().equals("back")) {
					if (manualState>=2) {
						manualState -= 1;
					}
				}
				//For breaking out of the manual loop and closing the program.
				else if (action.toLowerCase().equals("boop")) {
					System.out.println("Breaking out of manual loop");
					tmpClient.disconnect();
					MCP.shutdownMCP();
					break;
				}
				else {
					//Parsing the input based on present state.
					switch (manualState) {
						case 1:
							try {
								//Connect to the chosen server
								tmpClient = multiClientMap.get(Integer.parseInt(action));
								
								//Browse the address space to get the namespace table and device skills
								tmpSpace = tmpClient.getAddressSpace();
								refDescs = tmpSpace.browse(Identifiers.ObjectsFolder);
								tmpTable = tmpSpace.getNamespaceTable();
								//System.out.println("con1");
								//Iterate to find the device Set
								for(ReferenceDescription tempRef:refDescs) {
									//System.out.println(tempRef.getBrowseName());
									if(tempRef.getDisplayName().getText().equals("DeviceSet")) {
										tmpId = tmpTable.toNodeId(tempRef.getNodeId());
									}
								}
								//System.out.println("con2");
								//Iterate again to find Device Skills
								refDescs = tmpSpace.browse(tmpId);
								for(ReferenceDescription tempRef:refDescs) {
									//System.out.println(tempRef.getBrowseName());
									if(tempRef.getDisplayName().getText().contains("DeviceSkills")) {
										tmpId = tmpTable.toNodeId(tempRef.getNodeId());
									}
								}
								//System.out.println("con3");
								//Get refs from DeviceSkills to build list of skills
								skillRefs = new ArrayList<ReferenceDescription>();
								refDescs = tmpSpace.browse(tmpId);
								for(ReferenceDescription tempRef:refDescs) {
									//System.out.println(tempRef.getBrowseName());
									//System.out.println(tempRef.getNodeClass());
									if(tempRef.getNodeClass().toString().equals("Object")) {
										skillRefs.add(tempRef);
									}
								}
								//System.out.println("con4");
								//Set the state
								manualState = 2;
							} catch(NumberFormatException e) {
								System.out.println("BadInput: " + e);
								tmpClient.disconnect();
								MCP.shutdownMCP();
								break;
							} catch(Exception e) {
								System.out.println("A really bad thing happened.");
								tmpClient.disconnect();
								MCP.shutdownMCP();
								throw(new Exception(e));
							}
							break;
						case 2:
							//Print selected skill name.
							//System.out.println("Selected skill: " + skillRefs.get(Integer.parseInt(action)).getDisplayName());
							
							//Browse the methods
							for (ReferenceDescription tempRef:skillRefs) {
								if (skillRefs.indexOf(tempRef)==Integer.parseInt(action)) {
								//if (tempRef.getDisplayName().getText().contains(action)) {
									//tmpId = tmpTable.toNodeId(tempRef.getNodeId());
									//skillId = tmpId;
									skillId = tmpTable.toNodeId(tempRef.getNodeId());
									skillMethods = tmpSpace.getMethods(skillId);
								}
							}
							//tmpId = tmpTable.toNodeId(skillRefs.get(Integer.parseInt(action)).getNodeId());
							//skillMethods = tmpSpace.getMethods(tmpId);
							
							//Set the state
							manualState = 3;
							break;
						case 3:
							//Navigate to get the right shit from the server
							//methodToCall = tmpSpace.getMethod(skillMethods.get(Integer.parseInt(action)).getNodeId());
							//skillId = tmpSpace.getNode(tmpId).getNodeId();
							NodeId testId=null;
							for(UaMethod tempSkill:skillMethods) {
								if(skillMethods.indexOf(tempSkill)==Integer.parseInt(action)) {
								//if (tempSkill.getDisplayName().getText().contains(action)) {
									//methodToCall = tempSkill;
									testId = tempSkill.getNodeId();
								}
							}
							
							//Call the method requested by user
							System.out.println("tmpId: " + skillId + " \nMethod Id : " + testId + "\n Enter input arguments or other parameters:");
							
							//Get any potential input arguments from user.
							String UserArgumentString = readInput();
							
							//Variant[] outputs = tmpClient.call(skillId,methodToCall.getNodeId());
							Variant[] outputs = tmpClient.call(tmpClient.getAddressSpace().getNode(
									tmpTable.toExpandedNodeId(skillId)).getNodeId(),tmpClient.getAddressSpace().getNode(
											tmpTable.toExpandedNodeId(testId)).getNodeId(), new Variant(UserArgumentString));
							System.out.println("Method output: " + outputs[0]);
							break;
						default:
							System.out.println("Invalid state for some reason.");
							break;
					}
				}
			}
			catch (Exception e) {
				System.out.println(e);
				tmpClient.disconnect();
				MCP.shutdownMCP();
				break;
			}
		}while(true);
		tmpClient.disconnect();
		MCP.shutdownMCP();
	}
	
	//Various utility functions
	//Gets user input strings to be parsed by whatever requires them.
	protected static String readInput() {
		BufferedReader stdIn = new BufferedReader (new InputStreamReader(System.in));
		String s = null;
		do {
			try {
				s = stdIn.readLine();
			}catch (IOException e) {
				System.out.println("Some kind of error happened: " + e);
			}
		} while ((s == null) || (s.length() == 0));
		return s;
	}
	//Gets all the server configuration information from the user.
	protected void getUserConfigInputs() {
		logger.info("Getting server count and addresses.");
		int tempInt = -1;
		//Get the amount of servers to connect to
		do {
			System.out.print("Enter the amount of servers to connect to (1-6): ");
			try {
				tempInt = Integer.parseInt(readInput());
			}catch (Exception e) {
				System.out.println("Some kind of error happened: " + e);
			}
			if (tempInt>=1 && tempInt<=6) {
				this.serverCount = tempInt;
				break;
			} else {
				System.out.println("Faulty input");
			}
		}while(true);
		//Compile a list of server addresses
		System.out.println("Enter the addresses for each server in turn.");
		for(int x=0; x<this.serverCount; x++) {
			System.out.print("Server " + x + ": ");
			clientNumAddMap.put(x, readInput());
		}
	}
	//Fetche the event type from the OPC UA server address space by traversing it.
	protected NodeId getEventType(UaClient client) throws ServiceException, StatusException, ServiceResultException {
		client.getAddressSpace().setMaxReferencesPerNode(1000);
		AddressSpace servSpace = client.getAddressSpace();
		List<ReferenceDescription> topList = servSpace.browse(Identifiers.TypesFolder);
		for (ReferenceDescription refX: topList) {
			if (refX.getBrowseName().getName().equals("EventTypes")) {
				List<ReferenceDescription> evTypeRefs = servSpace.browse(refX.getNodeId());
				for (ReferenceDescription refY: evTypeRefs) {
					if(refY.getBrowseName().getName().equals("BaseEventType")) {
						List<ReferenceDescription> baseEventTypeRefs = servSpace.browse(refY.getNodeId());
						for (ReferenceDescription refZ: baseEventTypeRefs) {
							if (refZ.getBrowseName().getName().equals("MyStateEvent")) {
								NamespaceTable table = client.getNamespaceTable();
								NodeId targetId = table.toNodeId(refY.getNodeId());
								System.out.println("Found NodeId: " + targetId);
								return targetId;
							}
						}
					}
				}
			}
		}
		return null;
	}
	protected EventFilter buildEventFilter(UaClient client) throws ServiceException, StatusException, ServiceResultException {
		//Build the configuraiton variables for requesting event fiels and information from the server.
		//TAken from tutorial for the sake of making sure it works right.
		QualifiedName[] browsePath;
		UnsignedInteger evAttId = Attributes.Value;
		NodeId eventTypeId = getEventType(client);
		String indexRange = null;
		SimpleAttributeOperand[] selectClauses = new SimpleAttributeOperand[this.eventFieldNames.length+1];
		for (int x=0; x<this.eventFieldNames.length;x++) {
			if (this.eventFieldNames[x].getName().contains("/")) {
				 browsePath = new QualifiedName[] {this.eventFieldNames[x]};
			}
			else {
				int nsIndex = this.eventFieldNames[x].getNamespaceIndex();
				String[] names = this.eventFieldNames[x].getName().split("/");
				browsePath = new QualifiedName[names.length];
				for (int y=0; y<names.length; y++) {
					browsePath[y] = new QualifiedName(nsIndex, names[y]);
				}
			}
			selectClauses[x] = new SimpleAttributeOperand(eventTypeId, browsePath, evAttId, indexRange);
		}
		EventFilter filter = new EventFilter();
		filter.setSelectClauses(selectClauses);
		return filter;
	}
	//Subscribes to server events so the server can communicate 
	//Called after connecting to all the servers as this just handles each one at once.
	protected void subToServerEvents() throws ServiceException, StatusException, ServiceResultException {
		AddressSpace servSpace;
		//NamespaceTable addTable;
		for (Map.Entry<Integer,UaClient> entry: multiClientMap.entrySet()) {
			this.baseFilter = buildEventFilter(entry.getValue()); 
			entry.getValue().getAddressSpace().setMaxReferencesPerNode(1000);
			servSpace = entry.getValue().getAddressSpace();
			List<ReferenceDescription> topList = servSpace.browse(Identifiers.ObjectsFolder);
			for (ReferenceDescription ref: topList) {
				//System.out.println("Ref: " + ref);
				if(ref.getBrowseName().getName().equals("Server")) {
					//addTable = servSpace.getNamespaceTable();
					//subItems(addTable.toNodeId(ref.getNodeId()));
					subItems(ref.getNodeId(), entry.getKey());
					System.out.println("Found server " + ref.getNodeId() + " for " + entry.getValue().getAddress());
					System.out.println(servSpace.getClient() + "  vs.  " + entry.getValue());
				}
			}
		}
	}
	//Subscribes to items based on nodeId or ExpandedNodeId provided.
	protected void subItems(NodeId nodeId, int subIndex) throws ServiceException, StatusException {
		MonitoredEventItem eventItem = new MonitoredEventItem(nodeId, this.baseFilter);
		eventItem.setEventListener(myEventListener);
		subMap.get(subIndex).addItem(eventItem);
		System.out.println("Subbing to node: " + nodeId);
	}
	protected void subItems(ExpandedNodeId nodeId, int subIndex) throws ServiceException, StatusException {
		MonitoredEventItem eventItem = new MonitoredEventItem(nodeId, this.baseFilter);
		eventItem.setEventListener(myEventListener);
		subMap.get(subIndex).addItem(eventItem);
		System.out.println("Subbing to node: " + nodeId);
	}
	//Handles conneting to an individual server based on the "client" structure.
	protected void connect(UaClient client) {
		if (!client.isConnected()) {
			try {
				client.setSessionName(String.format("%s@%s Session%d", APP_NAME, 
						ApplicationIdentity.getActualHostNameWithoutDomain(), ++this.sessionCount));
				client.connect();
				System.out.println("Connected to server: " +client.getAddress());
				logger.info("Connecting client: " + APP_NAME + " to server: " +client.getAddress());
				try {
			          System.out.println("ServerStatus: " + client.getServerStatus());
			        } catch (StatusException e) {
			          System.out.println("Error: " + e);
			          logger.error("Error: " + e);
			        }
			} catch(Exception e) {
				System.out.println("Fuckery happened: " + e);
				logger.error("Something bad happened: " + e);
			}
		}
	}
	//Disconnects from the specific server.
	protected void disconnect(UaClient client) {
		client.disconnect();
		logger.info("Disconnecting client: " + APP_NAME);
	}
	//Gets a subobject of a target object by name. Used to traverse the address space.
	private static UaObjectNode getSubObject(UaNode baseObject, String targetName) {
		//System.out.println(baseObject + "  " + targetName);
		UaReference[] refs = baseObject.getReferences();
		
		for (UaReference tempRef:refs) {
			UaNode target = tempRef.getTargetNode();
			//System.out.println(target);
			if (target.getBrowseName().getName().contains(targetName)) {
				//System.out.println("Returning target subnode.");
				return (UaObjectNode) target;
			}
		}
		logger.error("Failed to find target subnode.");
		System.out.println("Failed to find target subnode.");
		return null;
	}
}
