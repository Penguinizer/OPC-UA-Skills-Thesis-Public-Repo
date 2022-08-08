Code developed for the Control, Robotics and Autonomous Systems line thesis project. All code provided as is with no real guarantees. Implemented utilizing the Prosys OPC UA Java SDK, see their licensing.
-----
This project requires the Prosys OPC UA Java SDK. In addition the types for the di and plc companion specifications must be generated as per instructions supplied with said SDK.
-----
How to use:
As it stands the programs are not precompiled. To this end the program should be opened in the user's Java setup of choice.
In order to execute the program the user should just run the java file with "Main" in the name for the components. The device server has to be run once per PLC that the user wants to use.

Steps to run:
1. Check the local network address of the computer. Enter this as the address in the IEC-61499 function block inputs field.
2. Make sure the ENaS demonstrator is ready for use, this includes checking that the lab computer is connected to the correct network.
3. Execute the "ModularServerMain" once for each PLC you wish to use.
4. During the initialization of those servers initialize 2 Belt Servers, 2 JackandSledge servers and 2 BeltandGripperServers. Remember the ports chosen for the sockets of each.
5. Enter the ports into the function blocks for those specific IEC-61499 programs.
6. Use EcoStruxures to run the IEC-61499 application on the PLCs.
7. If everything works, start the control client.
8. If something doesn't work, i'm very sorry. It worked for me the last time I tested so I probably haven't encountered potential issues.
For information regarding use of the ENaS demonstrator please talk to the FotF crew. They know more about it than I do.
-----
Information regarding code and files:
Code developed for the project is contained in the "src" folder. Within this folder "simple server", "interfaceclienttest" and "codeexperiments" contain initial proof of concept prototypes for project concepts. They are largely not relevant for the project itself.
"src/com/SkillsProject" contains the device control servers as well as the central control client files which make up the core of the project. Within this are the two primary software components. These are the "ModularServer" and "ControlClient" in their respective sub-folders.
Please see comments in individual files for specifics of how code functions.

"src/com/SkillsProject/ModularServer" contains the device servers which are responsible for handling the invocation of skills as well as communication with the IEC-61499 program. In addition this server is responsible for communicating device capabilities to the control client.
This part of the project contains the following modules:
- Bridge
	*Contains the Skill Bridge component which is responsible for communication with the IEC-61499 program using TCP. This component was based on the work of the Tracing Products project (see thesis text).
	*Handles updating the server information regarding state based on parsing packets from the controller.
	*The Skill Bridge module is initialized upon a connection being made to the TCP IO Server.
	*Skillbridge functionality is invoked by the device method manager.

- TcpIOServer
	*Contains the TCP communication functionality used by the Bridge.
	*The TCP IO Server is initialized upon server address space creation by the Modular Node Manager component.
	*Opens a TCP socket. Upon a connection being formed the Skill Bridge component is initialized.

- The Listener modules: DeviceEventManagerListener, DeviceIoManagerListener, ModularDeviceMethodManagerListener, MyCertificateValidationListener and serverNodeManagerListener
	*These are files containing event handling and are required by OPC UA.
	*ModularDeviceMethodManagerListener handles the invocation of the skill methods as well as other utility methods invoked by the user through a client.

- ModularNodeManager
	*Manages the nodes in the server's OPC UA address space. In addition this module handles the creation of the address space structure.
	*Invoked by the main loop when server is first started.
	*Addres space created by populating a list of nodes to create before iterating through the list.
	*Presently only supports ENaS demonstrator components but lists of nodes could be read from a file with some extra work that was judged to not be relevant to this project.

- ModularServerMain
	*Contains the main loop of the device server while also handling assorted management inputs from a user interacting with the application itself.
	*Also handles user inputs, for example for gracefully turning off the server.
	*Handles initializing server information outside of the addres space creation.
	*Invokes the Modular Node Manager function as well as creating the various listeners.

- DeviceInfo
	*Contains a data type for storing information regarding the device name, manufacturer and other information.

- MyStateEvent
	*Contains a data structure used for communicating events from the server to the client.

- MyUserValidator
	*Required by OPC UA to function. Not necessary for the project.
	
"src/com/SkillsProject/ControlClient" contains the central control client which communicates with the device servers.
- ControlClientMain
	*Contains the main loop of the control client.
	*Upon initialization asks the user to input the number of servers to connect to and their IP addresses/ports.
	*In addition the user can choose if they want the skills to be invoked automatically or manually.
		NOTE: As it stands manual operation is preferred as the automatic mode was not working right.
	*After this the user can choose one of the servers the program has connected to and choose a skill to invoke.

- MasterControlProgram
	*Contains the somewhat functional automatic control loop.
	*Runs in its own thread if the user chooses to run the client in auto-mode during main loop initialization.

- ControlEventListener
	*An event listener which handles events sent by the device servers.
	*Logs the event information and in case of automatic mode passes along the event to the thread.
	*Initialized during client initialization by main loop module.

- ControlClientListener
	*Contains OPC UA required functionality. Not used for the project.
	*Initialized during client initialization by main loop module.

- ControlServerStatusListener
	*Handles informing if one of the device servers shuts down.
	*Initialized during client initialization by main loop module.

- mySubAliveListener and mySubListener
	*Contain functionality related to device server events.
	*Stubs related to OPC UA core functionality.
	*Initialized during client initialization by main loop module.
-----
IEC-61499 application is presently saved at the Factory of the Future lab. If I remember to add it here it will be in its own labeled folder.
In order to use this program the IP address and port of the TCP-sockets must be manually entered as variables for the function blocks alongside any potential function name changes. After this the correct programs must be uploaded to the controllers and executed. 
NOTE: The OPC UA server programs should be running and configured before the IEC-61499 applications are executed. This is because if the IEC-61499 function block attempts to handshake a non-existent port it will enter an error state.
ALSO NOTE: This is assuming the ENaS demonstrator configuration is at the state it was in 12/2021. Potential changes to PLC IO or hardware have not been accounted for.
 
The OPC UA server will inform in the log if everything is working correctly. After this the control client can be used to execute skills.
