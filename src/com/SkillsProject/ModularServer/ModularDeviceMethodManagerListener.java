/* The method listener node handles the methods on the server which are invoked by clients.
 * Handles parsing if the method is valid, if it can be called and then implements
 * the functionality of the method in question.
 * 
 * In this case primarily focuses on communication with the IEC-61499 program over the Skill Bridge.
*/
package com.SkillsProject.ModularServer;

import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaMethod;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.server.CallableListener;
import com.prosysopc.ua.server.NodeManagerUaNode;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.server.nodes.PlainMethod;
import com.prosysopc.ua.stack.builtintypes.DiagnosticInfo;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.builtintypes.Variant;

public class ModularDeviceMethodManagerListener implements CallableListener{

	private static Logger logger = LoggerFactory.getLogger(ModularDeviceMethodManagerListener.class);
	private final UaNode myMethod;
	private ModularNodeManager nodeMan;

	//Add the method node to handler
	public ModularDeviceMethodManagerListener(UaNode myMethod, ModularNodeManager test) {
		super();
		this.myMethod = myMethod;
		this.nodeMan=test;
	}

	//Primary method that is called when the client invokes one of the OPC UA Method objects.
	//Parses which method is in question and invokes relevant address space methods.
	//Primarily for communicating with the IEC-61499 program over the skill bridge.
	@Override
	public boolean onCall(ServiceContext serviceContext, NodeId objectId, UaNode object, NodeId methodId, UaMethod method,
		      final Variant[] inputArguments, final StatusCode[] inputArgumentResults,
		      final DiagnosticInfo[] inputArgumentDiagnosticInfos, final Variant[] outputs) throws StatusException {
				if (methodId.equals(myMethod.getNodeId())) {
					logger.info("Method called: " + myMethod.getBrowseName() + ", Method ID: " + myMethod.getNodeId());
					System.out.println("Method called: " + myMethod.getBrowseName().getName() + ", Method ID: " + myMethod.getNodeId());

					//Temporarily here
					String[] skillInfo = myMethod.getBrowseName().getName().split("_");
					System.out.println("SkillInfo: " + Arrays.toString(skillInfo));
					if(nodeMan.bridgeReady) {
						//Parsing based solution instead of hard coded skill+method names
						//First part is the method, the second is the skill that the call will be sent to.
						logger.info("Skill method invoked: " + skillInfo[1] + ", For skill: " + skillInfo[0]);
						switch (skillInfo[1]) {
							case "Start":
								try {
									if(nodeMan.spaceBridge.sendStart(skillInfo[0], inputArguments[0].toString())) {
										outputs[0] = new Variant(skillInfo[0] + " command "+ skillInfo[1] + " sent with input arguments: " + inputArguments[0].toString());
									} else {
										outputs[0] = new Variant("Failed to send command: " + skillInfo[1] + " due to an invalid state.");
									}
									return true;
								} catch (Exception e) {
									logger.error("Start method error: " + e);
									e.printStackTrace();
									outputs[0] = new Variant("Start method error: " + e);
									return false;
								}
							case "Stop":
								try {
									if(nodeMan.spaceBridge.sendStop(skillInfo[0], inputArguments[0].toString())) {
										outputs[0] = new Variant(skillInfo[0] + " command "+ skillInfo[1] + " sent with input arguments: " + inputArguments[0].toString());
									} else {
										outputs[0] = new Variant("Failed to send command: " + skillInfo[1] + " due to an invalid state.");
									}
									return true;
								} catch (Exception e) {
									logger.error("Stop method error: " + e);
									e.printStackTrace();
									outputs[0] = new Variant("Stop method error: " + e);
									return false;
								}
							case "Suspend":
								try {
									if(nodeMan.spaceBridge.sendSuspend(skillInfo[0], inputArguments[0].toString())) {
										outputs[0] = new Variant(skillInfo[0] + " command "+ skillInfo[1] + " sent with input arguments: " + inputArguments[0].toString());
									} else {
										outputs[0] = new Variant("Failed to send command: " + skillInfo[1] + " due to an invalid state.");
									}
									return true;
								} catch (Exception e) {
									logger.error("Suspend method error: " + e);
									e.printStackTrace();
									outputs[0] = new Variant("Suspend method error: " + e);
									return false;
								}
							case "Resume":
								try {
									if(nodeMan.spaceBridge.sendResume(skillInfo[0], inputArguments[0].toString())) {
										outputs[0] = new Variant(skillInfo[0] + " command "+ skillInfo[1] + " sent with input arguments: " + inputArguments[0].toString());
									} else {
										outputs[0] = new Variant("Failed to send command: " + skillInfo[1] + " due to an invalid state.");
									}
									return true;
								} catch (Exception e) {
									logger.error("Resume method error: " + e);
									e.printStackTrace();
									outputs[0] = new Variant("Resume method error: " + e);
									return false;
								}
							case "Reset":
								try {
									if(nodeMan.spaceBridge.sendReset(skillInfo[0], inputArguments[0].toString())) {
										outputs[0] = new Variant(skillInfo[0] + " command "+ skillInfo[1] + " sent with input arguments: " + inputArguments[0].toString());
									} else {
										outputs[0] = new Variant("Failed to send command: " + skillInfo[1] + " due to an invalid state.");
									}
									return true;
								} catch (Exception e) {
									logger.error("Reset method error: " + e);
									e.printStackTrace();
									outputs[0] = new Variant("Reset method error: " + e);
									return false;
								}
							case "ReportCapabilities":
								System.out.println("BAZINGA, It's a report. This isn't implemented yet.");
								logger.debug("Unimplemented method called: " + skillInfo[1]);
								outputs[0] = new Variant("Unimplemented Method called : ReportCapabilities");
								return true;
							case "ToggleSensorReporting":
								System.out.println("BAZINGA, It's a report. This isn't implemented yet.");
								logger.debug("Unimplemented method called: " + skillInfo[1]);
								outputs[0] = new Variant("Unimplemented method called: ToggleSensorReporting");
								return true;
						}
						//This shouldn't happen 
						System.out.println("Switch for methods didn't fire. Method manager resolved in bad way.");
						logger.error("The switch did a thing it's not meant to and the method resolved in an unintended way.");
						return false;
					} else if (skillInfo[1].equals("DispenseMaterial")) {
						outputs[0] = new Variant("Product Dispensed");
						try {
							TimeUnit.SECONDS.sleep(3);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						return true;
					} else {
						System.out.println("The SkillBridge isn't ready for some reason or your method was invalid.");
						logger.error("SkillBridge is not ready for an unknown reason or your method was invalid.");
						outputs[0] = new Variant("Skillbridge is not ready for a method, reason unknown.");
						return true;
					}
						
				}
				else {
					return false;
				}
	}
}
