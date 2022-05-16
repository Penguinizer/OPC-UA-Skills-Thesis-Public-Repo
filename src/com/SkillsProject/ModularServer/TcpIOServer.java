/* The TCP IO Server half of the Skill Bridge module.
 * Based on the implementation developed during the Tracing Products in Flexible Systems project.
 * See: https://wiki.aalto.fi/display/AEEproject/Tracing+products+in+flexible+production+systems
 * 
 * The TCP IO server handles establishing a connection with the IEC-61499 program on the PLC.
 * When the connection is established a Skill Bridge object is created to handle communication.
 */

package com.SkillsProject.ModularServer;

import java.io.IOException;
import java.net.ServerSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpIOServer extends Thread {
	//Declare variables
	private final int port;
	private final ModularNodeManager nodeManager;
	private ServerSocket servSock;
	private Bridge skillBridge = null;
	private Boolean operateServer=true;
	
	//Create the logger
	private Logger logger = LoggerFactory.getLogger(TcpIOServer.class);
	
	//Initialization
	public TcpIOServer(int port, ModularNodeManager nodeMan) {
		this.nodeManager=nodeMan;
		this.port = port;
		this.setName("TCPIOServerThread");
	}
	
	//Main thread functionality
	public void run() {
		try {
			//Start the socket and log
			servSock = new ServerSocket(port);
			logger.info("Starting TCP IO Server at: " + servSock.getLocalPort());
			
			//Core loop
			//Create a new Skill Bridge when a device server connects. Set up this way so that a consolidated OPC UA server works as intended.
			while (operateServer) {
				skillBridge = new Bridge(servSock.accept(), nodeManager);
				skillBridge.start();
				nodeManager.bridgeReady = true;
				nodeManager.spaceBridge = skillBridge;
			}
		}
		catch(Exception e) {
			logger.error("TCP IO Server error: " + e);
			System.out.println("TCP IO Server error: " + e);
		}
	}
	
	//Function for closing the socket
	public void close() {
		try {
			logger.info("Closing TCP IO Server");
			servSock.close();
			operateServer=false;
			
			if (skillBridge != null && skillBridge.isAlive()) {
				skillBridge.close();
			}
		} catch (IOException e) {
			logger.error("TCP Server Close Failed: " + e);
		}
	}
}
