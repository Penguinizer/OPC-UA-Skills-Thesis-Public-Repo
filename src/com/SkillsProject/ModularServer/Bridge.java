/* Skillbridge module
 * 
 * Modified version based on the version implemented during the Tracing Prodcuts in Flexible Production Systems project. 
 * See: https://wiki.aalto.fi/display/AEEproject/Tracing+products+in+flexible+production+systems
 * 
 * This implementation moves to a consolidated one Skill Bridge per device server design. In addition packet
 * structures were modified. Basic structure is similar otherwise.
 * 
 * The Skill Bridge module handles communication with the IEC-61499 implementatation running on the PLC via TCP packets.
 * The Skill Bridge handles sending messages via writing into the outbound buffer while parsing
 * messages from the server by reading from a buffered reader. 
 * 
 * Public methods invoked by other server components handle sending messages to the server.
 * Parsing packets from the server invokes methods from the Node Manager.
 */

package com.SkillsProject.ModularServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaReference;
import com.prosysopc.ua.server.nodes.PlainProperty;
import com.prosysopc.ua.server.nodes.UaObjectNode;
import com.prosysopc.ua.stack.core.StatusCodes;

public class Bridge extends Thread {
	//variables for the bridge object.
	private final Socket nxtSocket;
	private final ModularNodeManager nodeManager;
	private PrintWriter outBuffer;
	private BufferedReader inBuffer;
	private Boolean operateBridge = true;
	private int startRet = 0;
	private int suspendRet = 0;
	private int resumeRet = 0;
	private int stopRet = 0;
	private int resetRet = 0;
	
	//Set up the logger.
	private static Logger logger = LoggerFactory.getLogger(Bridge.class);
	
	
	public Bridge(Socket socket, ModularNodeManager nodeMan) {
		this.nxtSocket = socket;
		this.nodeManager = nodeMan;
		this.setName("SkillBridgeThread");
		logger.info("SkillBridge Created");
	}
	
	//The main loop for the skillbridge component
	public void run() {
		//Logging info
		logger.debug("Skill Bridge started");
		
		try {
			//Creating the buffers
			outBuffer = new PrintWriter(nxtSocket.getOutputStream(),true);
			inBuffer = new BufferedReader(new InputStreamReader(nxtSocket.getInputStream()));
			String input;
			
			//Core loop for the skillbridge
			while (operateBridge) {
				// Wait for strings and try to not break things by checking that the string
				// isn't null. Then parse that string by splitting it and using a switch.
				if ((input = inBuffer.readLine()) != null) {
					String[] data = input.split(";");
					
					//TODO REMOVE
					//System.out.println("operate bridge shit: " + Arrays.toString(data));
					
					//Parse the type of packet.
					switch (data[1]) {
						case "INIT":
							if (initStuff(data[0])) {
								outBuffer.println(data[0] + ";RET;OK");
							}
							else {
								outBuffer.println(data[0] + ";RET;FAILED");
							}
							break;
						case "UPDATE":
							updateSkill(data[0], data[2], data[3]);
							break;
						case "CALLRET":
							callReturn(data[0], data[2], data[3]);
							break;
						default:
							System.out.println("Skillbridge invalid formatted data: " + data);
							logger.error("Skillbridge invalid data formatting: " + data);
							break;
					}
				} else {
					logger.error("Readline returned null");
					System.out.println("Skillbridge readline returned null");
					nodeManager.spaceServer.close();
					break;
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			logger.warn("Skill bridge error: " + e);
			nodeManager.spaceServer.close();
		}
	}
	
	//Handles response from controller.
	//Checks if the result succeeded or failed.
	private void callReturn (String name, String callType, String callResult) throws StatusException {
		nodeManager.sendEvent(name,callType,callResult);
		//System.out.println("Parsing call return : " + name + ", " + callType + ", " + callResult+".");
		int tmpRes = 0;
		if (callResult.contentEquals("FAILED")) {
			tmpRes = -1;
		}
		else if (callResult.contentEquals("OK")) {
			tmpRes = 1;
		}
		switch (callType) {
			case "START":
				startRet = tmpRes;
				break;
			case "STOP":
				stopRet = tmpRes;
				break;
			case "SUSPEND":
				suspendRet = tmpRes;
				break;
			case "RESUME":
				resumeRet = tmpRes;
				break;
			case "RESET":
				resetRet = tmpRes;
				break;
			default:
				System.out.println("INVALID CALL TYPE: " + callType);
				logger.error("Invalid call type return: " + callType);
				break;
		}
	}
	//Handles initialization.
	//Currently a stub that is unused.
	//May be updated in the future.
	private boolean initStuff(String name) {
		return true;
	}
	
	//Handles updating skill object current state variable.
	private boolean updateSkill(String name, String CurrentState, String ReturnData) {
		try {
			if(nodeManager.spaceSkills.containsKey(name)) {
				nodeManager.spaceSkills.get(name).getProperties()[0].setValue(CurrentState);
			}
			nodeManager.sendEvent(name,CurrentState);
		} catch (StatusException e) {
			System.out.println("Issue with updating skill state.");
			e.printStackTrace();
		}
		return true;
	}
	
	//Handles shutting down the Skillbridge
	public void close() {
		try {
			nxtSocket.close();
			inBuffer.close();
			outBuffer.close();
			operateBridge = false;
		} catch (IOException e) {
			logger.error("Error closing down skillbridge");
			logger.error("Error closing skillbridge: {}", e.getMessage());
		}
	}
	
	//Functions for handling sending specific commands to trigger various transitions
	public boolean sendStart(String skillName, String startParameter) throws Exception {
		try {
			//Reset wait for return
			startRet = 0;
			if(!this.getSubObject(nodeManager.spaceSkills.get(skillName),
					skillName+"_SkillState").getCurrentValue().contains("Ready")) {
				System.out.println("The skill is in an invalid state:" + this.getSubObject(nodeManager.spaceSkills.get(skillName),
						skillName+"_SkillState").getCurrentValue());
				return false;
			}
			//Send call to PLC
			outBuffer.println(skillName+";CALL;START;"+startParameter);
			//Wait for a response
			do {
				sleep(50);
			} while (startRet == 0);
			if (startRet <0) {
				throw new StatusException("Automatic execution active. Remote control disabled.",
						StatusCodes.Bad_RequestNotAllowed);
			}
			return true;
		}
		catch (Exception e) {
			logger.error("Send start error: " + e);
			throw e;
		}
	}
	public boolean sendStop(String skillName, String startParameter) throws Exception {
		try {
			//Reset wait for return
			stopRet = 0;
			if(!this.getSubObject(nodeManager.spaceSkills.get(skillName),
					skillName+"_SkillState").getCurrentValue().contains("Exec") &&
					(!this.getSubObject(nodeManager.spaceSkills.get(skillName),
							skillName+"_SkillState").getCurrentValue().contains("Suspend"))) {
				System.out.println("The skill is in an invalid state:" + this.getSubObject(nodeManager.spaceSkills.get(skillName),
						skillName+"_SkillState").getCurrentValue());
				return false;
			}
			//Send call to PLC
			outBuffer.println(skillName+";CALL;STOP;"+startParameter);
			//Wait for a response
			do {
				sleep(50);
			} while (stopRet == 0);
			if (stopRet <0) {
				throw new StatusException("Automatic execution active. Remote control disabled.",
						StatusCodes.Bad_RequestNotAllowed);
			}
			return true;
		}
		catch (Exception e) {
			logger.error("Send stop error: " + e);
			throw e;
		}
	}
	public boolean sendReset(String skillName, String startParameter) throws Exception {
		try {
			//Reset wait for return
			resetRet = 0;
			if(!this.getSubObject(nodeManager.spaceSkills.get(skillName),
					skillName+"_SkillState").getCurrentValue().contains("Stop")) {
				System.out.println("The skill is in an invalid state:" + this.getSubObject(nodeManager.spaceSkills.get(skillName),
						skillName+"_SkillState").getCurrentValue());
				return false;
			}
			//Send call to PLC
			outBuffer.println(skillName+";CALL;RESET;"+startParameter);
			//Wait for a reaponse
			do {
				sleep(50);
			} while (resetRet == 0);
			if (resetRet <0) {
				throw new StatusException("Automatic execution active. Remote control disabled.",
						StatusCodes.Bad_RequestNotAllowed);
			}
			return true;
		}
		catch (Exception e) {
			logger.error("Send reset error: " + e);
			throw e;
		}
	}
	public boolean sendSuspend(String skillName, String startParameter) throws Exception {
		try {
			//Reset wait for return
			suspendRet = 0;
			if(!this.getSubObject(nodeManager.spaceSkills.get(skillName),
					skillName+"_SkillState").getCurrentValue().contains("Exec")) {
				System.out.println("\n\nBad State: " + this.getSubObject(nodeManager.spaceSkills.get(skillName),skillName+"_SkillState").getCurrentValue());
				return false;
			}
			//Send call to PLC
			outBuffer.println(skillName+";CALL;SUSPEND;"+startParameter);
			//Wait for a response
			do {
				sleep(50);
			} while (suspendRet == 0);
			if (suspendRet <0) {
				throw new StatusException("Automatic execution active. Remote control disabled.",
						StatusCodes.Bad_RequestNotAllowed);
			}
			return true;
		}
		catch (Exception e) {
			logger.error("Send suspend error: " + e);
			throw e;
		}
	}
	public boolean sendResume(String skillName, String startParameter) throws Exception {
		try {
			//Reset wait for return
			resumeRet = 0;
			if(!this.getSubObject(nodeManager.spaceSkills.get(skillName),
					skillName+"_SkillState").getCurrentValue().contains("Suspend")) {
				System.out.println("The skill is in an invalid state:" + this.getSubObject(nodeManager.spaceSkills.get(skillName),
						skillName+"_SkillState").getCurrentValue());
				return false;
			}
			//Send call to PLC
			outBuffer.println(skillName+";CALL;RESUME;"+startParameter);
			//Wait for a response
			do {
				sleep(50);
			} while (resumeRet == 0);
			if (resumeRet <0) {
				throw new StatusException("Automatic execution active. Remote control disabled.",
						StatusCodes.Bad_RequestNotAllowed);
			}
			return true;
		}
		catch (Exception e) {
			logger.error("Send resume error: " + e);
			throw e;
		}
	}
	
	//Small utility function for fetching target subnodes
	//Identical to similar function in ModularNodeManager. Reimplemented here for the sake of keeping both private.
	  private PlainProperty<String> getSubObject(UaNode baseObject, String targetName) {
		UaReference[] refs = baseObject.getReferences();
			
		for (UaReference tempRef:refs) {
			UaNode target = tempRef.getTargetNode();
			if (target.getBrowseName().getName().contains(targetName)) {
				return (PlainProperty<String>) target;
			}
		}
		logger.error("Failed to find target subnode.");
		System.out.println("Failed to find target subnode.");
		return null;
	}
}
