/*The main function of the modular OPC UA server.
 * Contains functionality for configuring information regarding the server.
 * In addition creates the needed server objects.
 * Actual server functionality is handled by the specific object.
 * 
 * Handles basic user inputs for turning the server off and potential other functionality.
 * 
 * Functionality related to security and certificates used as from the Prosys SDK tutorial as
 * that functionality is not the focus of the project.
 */

package com.SkillsProject.ModularServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import com.prosysopc.ua.ApplicationIdentity;
import com.prosysopc.ua.SecureIdentityException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.UaApplication;
import com.prosysopc.ua.UserTokenPolicies;
import com.prosysopc.ua.UaApplication.Protocol;
import com.prosysopc.ua.samples.server.MyCertificateValidationListener;
import com.prosysopc.ua.server.UaServer;
import com.prosysopc.ua.server.UaServerException;
import com.prosysopc.ua.server.UserValidator;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.LocalizedText;
import com.prosysopc.ua.stack.cert.DefaultCertificateValidator;
import com.prosysopc.ua.stack.cert.DefaultCertificateValidatorListener;
import com.prosysopc.ua.stack.cert.PkiDirectoryCertificateStore;
import com.prosysopc.ua.stack.core.ApplicationDescription;
import com.prosysopc.ua.stack.core.ApplicationType;
import com.prosysopc.ua.stack.core.MessageSecurityMode;
import com.prosysopc.ua.stack.transport.security.HttpsSecurityPolicy;
import com.prosysopc.ua.stack.transport.security.KeyPair;
import com.prosysopc.ua.stack.transport.security.SecurityMode;
import com.prosysopc.ua.stack.transport.security.SecurityPolicy;
import com.prosysopc.ua.stack.utils.EndpointUtil;
import com.prosysopc.ua.types.opcua.server.BuildInfoTypeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModularServerMain {
	//Declaring the variables for basic server info.
	protected static String APP_NAME;
	protected static String serverType;
	protected static int port;
	protected static int httpsPort;
	
	//Declaring the variables for various server components.
	protected UaServer myServer;
	protected ModularNodeManager serverNodeManager;
	protected serverNodeManagerListener myNodeManagerListener = new serverNodeManagerListener();
	protected static int serverTypeInt;
	
	//Creating the logger
	private static Logger logger = LoggerFactory.getLogger(ModularServerMain.class);
	
	//Security stuff. Directly ported from SDK examples as it's sort of a black box for me.
	protected UserValidator userValidator;
	protected final DefaultCertificateValidatorListener validationListener = new MyCertificateValidationListener();
	protected final DefaultCertificateValidatorListener userCertificateValidationListener =
		      new MyCertificateValidationListener(); 
	
	//Utility functions for easier functioning.
	public static void printException(Exception er) {
		System.out.println(er.toString());
		logger.error("Exception encountered: " + er.toString());
		if (er.getCause() != null) {
			System.out.println("Caused by: " + er.getCause());
			logger.error("Caused by: " + er.getCause());
		}
	}
	private static String readInput() {
	    BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
	    String s = null;
	    do {
	      try {
	        s = stdin.readLine();
	      } catch (IOException e) {
	        printException(e);
	      }
	    } while ((s == null) || (s.length() == 0));
	    return s;
	  }
	protected static boolean parseArgs(String [] args) {
		return false;
	}
	public static String returnServerType() {
		return serverType;
	}
	public static Logger returnLogger() {
		return logger;
	}
	//Calls the address space creation function after creating the node manager.
	protected void createAddressSpace() throws StatusException {
		serverNodeManager = new ModularNodeManager(myServer, ModularNodeManager.NAMESPACE, serverType);
		serverNodeManager.addListener(myNodeManagerListener);
		serverNodeManager.getIoManager().addListeners(new DeviceIoManagerListener());
		serverNodeManager.createAddressSpace();
		logger.info("Address space created");
	}
	
	//Initializes info regarding the build.
	//Saves relevant info about the build version, manufacturer and so on.
	//Based partially on the example,
	//however it's very much a basic function so it's probably ok.
	protected void buildInfo() {
		final BuildInfoTypeNode buildInfo =
		        myServer.getNodeManagerRoot().getServerData().getServerStatusNode().getBuildInfoNode();
		
		buildInfo.setProductName(APP_NAME);
		
		final String implementationVersion = UaApplication.getSdkVersion();
		if (implementationVersion != null) {
		      int splitIndex = implementationVersion.lastIndexOf("-");
		      final String softwareVersion = splitIndex == -1 ? "thesisProject" : implementationVersion.substring(0, splitIndex);
		      String buildNumber = splitIndex == -1 ? "thesisProject" : implementationVersion.substring(splitIndex + 1);

		      buildInfo.setManufacturerName("AYY Thesis Project");
		      buildInfo.setSoftwareVersion(softwareVersion);
		      buildInfo.setBuildNumber(buildNumber);
		}
		
		final URL classFile = UaServer.class.getResource("/com/SkillsProject/BeltServer/BeltServerMain.class");
	    if (classFile != null && classFile.getFile() != null) {
	      final File mfFile = new File(classFile.getFile());
	      GregorianCalendar c = new GregorianCalendar();
	      c.setTimeInMillis(mfFile.lastModified());
	      buildInfo.setBuildDate(new DateTime(c));
	    }
	    logger.info("Build Info set");
	}
	
	//Initialize server with all info required for it to function.
	//Also initializes the app description.
	//Also initializes security relevant information
	protected void initializeServer (int port, int httpsPort, String appName) throws UaServerException, IOException, SecureIdentityException {
		//Create server
		myServer = new UaServer();
		myServer.setEnableIPv6(true);
		
		//Initialize appdescription
		ApplicationDescription appDescription = new ApplicationDescription();
		appDescription.setApplicationName(new LocalizedText(appName + "@hostname"));
		appDescription.setApplicationUri("urn:hostname:OPCUA:" + appName);
	    appDescription.setProductUri("urn:AYYSkillsThesisProject.com:OPCUA:" + appName);
	    appDescription.setApplicationType(ApplicationType.Server);
	    
	    //Set the server name, TCP port and HTTPS port.
	    myServer.setPort(Protocol.OpcTcp, port);
	    myServer.setPort(Protocol.OpcHttps, httpsPort);
	    myServer.setServerName("OPCUA/" + appName);
	    myServer.setBindAddresses(EndpointUtil.getInetAddresses(myServer.isEnableIPv6()));
	    
	    //Set cert stores and cert validator.
	    //NOTE: Taken directly from the SDK dev example.
	    //I have no clue how this works, it is outside the scope of the project.
	    //Also when talking with someone from Prosys they suggested that if I wasn't sure how
	    // something works or how to make it work that I should work off of the samples provided.
	    final PkiDirectoryCertificateStore applicationCertificateStore = new PkiDirectoryCertificateStore("PKI/CA");
	    final PkiDirectoryCertificateStore applicationIssuerCertificateStore =
	        new PkiDirectoryCertificateStore("PKI/CA/issuers");
	    final DefaultCertificateValidator applicationCertificateValidator =
	        new DefaultCertificateValidator(applicationCertificateStore, applicationIssuerCertificateStore);
	    myServer.setCertificateValidator(applicationCertificateValidator);
	    // ...and react to validation results with a custom handler
	    applicationCertificateValidator.setValidationListener(validationListener);

	    // Handle user certificates
	    final PkiDirectoryCertificateStore userCertificateStore = new PkiDirectoryCertificateStore("USERS_PKI/CA");
	    final PkiDirectoryCertificateStore userIssuerCertificateStore =
	        new PkiDirectoryCertificateStore("USERS_PKI/CA/issuers");

	    final DefaultCertificateValidator userCertificateValidator =
	        new DefaultCertificateValidator(userCertificateStore, userIssuerCertificateStore);

	    userValidator = new MyUserValidator(userCertificateValidator);
	    // ...and react to validation results with a custom handler
	    userCertificateValidator.setValidationListener(userCertificateValidationListener);
	    //Certs
	    File privatePath = new File(applicationCertificateStore.getBaseDir(), "private");
	    KeyPair issuerCertificate =
	            ApplicationIdentity.loadOrCreateIssuerCertificate("ProsysSampleCA", privatePath, "opcua", 3650, false);
	    int[] keySizes = null;
	    final ApplicationIdentity identity = ApplicationIdentity.loadOrCreateCertificate(appDescription,
	            "Sample Organisation", /* Private Key Password */"opcua", /* Key File Path */privatePath,
	            /* Issuer Certificate & Private Key */null,
	            /* Key Sizes for instance certificates to create */keySizes,
	            /* Enable renewing the certificate */true);
	    String hostName = ApplicationIdentity.getActualHostName();
	    identity.setHttpsCertificate(ApplicationIdentity.loadOrCreateHttpsCertificate(appDescription, hostName, "opcua",
	        issuerCertificate, privatePath, true));
	    Set<SecurityPolicy> supportedSecurityPolicies = new HashSet<SecurityPolicy>();
	    
	    //This policy does not support any security. Should only be used in isolated networks.
	    supportedSecurityPolicies.add(SecurityPolicy.NONE);
	    // Modes defined in previous versions of the specification
	    supportedSecurityPolicies.addAll(SecurityPolicy.ALL_SECURE_101);
	    supportedSecurityPolicies.addAll(SecurityPolicy.ALL_SECURE_102);
	    supportedSecurityPolicies.addAll(SecurityPolicy.ALL_SECURE_103);
	    supportedSecurityPolicies.addAll(SecurityPolicy.ALL_SECURE_104);
	    
	    Set<MessageSecurityMode> supportedMessageSecurityModes = new HashSet<MessageSecurityMode>();
	    supportedMessageSecurityModes.add(MessageSecurityMode.None);
	    supportedMessageSecurityModes.add(MessageSecurityMode.Sign);
	    supportedMessageSecurityModes.add(MessageSecurityMode.SignAndEncrypt);
	    
	    myServer.getSecurityModes()
        	.addAll(SecurityMode.combinations(supportedMessageSecurityModes, supportedSecurityPolicies));
	    
	    myServer.getHttpsSecurityModes().addAll(SecurityMode
	            .combinations(EnumSet.of(MessageSecurityMode.None, MessageSecurityMode.Sign), supportedSecurityPolicies));

	    // The TLS security policies to use for HTTPS
	    Set<HttpsSecurityPolicy> supportedHttpsSecurityPolicies = new HashSet<HttpsSecurityPolicy>();
	    // (HTTPS was defined starting from OPC UA Specification 1.02)
	    supportedHttpsSecurityPolicies.addAll(HttpsSecurityPolicy.ALL_102);
	    supportedHttpsSecurityPolicies.addAll(HttpsSecurityPolicy.ALL_103);
	    // Only these are recommended by the 1.04 Specification
	    supportedHttpsSecurityPolicies.addAll(HttpsSecurityPolicy.ALL_104);
	    myServer.getHttpsSettings().setHttpsSecurityPolicies(supportedHttpsSecurityPolicies);

	    // Number of threads to reserve for the HTTPS server, default is 10
	    // server.setHttpsWorkerThreadCount(10);

	    // Define the certificate validator for the HTTPS certificates;
	    // we use the same validator that we use for Application Instance Certificates
	    myServer.getHttpsSettings().setCertificateValidator(applicationCertificateValidator);

	    // Define the supported user authentication methods
	    myServer.addUserTokenPolicy(UserTokenPolicies.ANONYMOUS);
	    myServer.addUserTokenPolicy(UserTokenPolicies.SECURE_USERNAME_PASSWORD);
	    myServer.addUserTokenPolicy(UserTokenPolicies.SECURE_CERTIFICATE);

	    // Define a validator for checking the user accounts
	    myServer.setUserValidator(userValidator);
	    
	    myServer.setApplicationIdentity(identity);
	    
	    myServer.init();
	    buildInfo();
	    
	    myServer.getSessionManager().setMaxSessionCount(500);
	    myServer.getSessionManager().setMaxSessionTimeout(3600000); // one hour
	    myServer.getSubscriptionManager().setMaxSubscriptionCount(50);
	    
	    logger.info("Server initialized");
	}
	
	//The menu function for the server.
	//Handles user input. Presently primarily to shut the server down.
	//Other functionality may be implemented as necessary.
	protected void menu() {
		do {
			System.out.println ("input \"test\" to shut down");
			try {
				System.out.println("address: opc.tcp://localhost:"+port+"/"+myServer.getServerName());
				String action = readInput();
				if (action.equals("test")) {
					
					return;
				}
				else {
					continue;
				}
			} catch (Exception e) {
				printException(e);
			}
		}while(true);
	}
	//The function for running the server.
	protected void run () throws UaServerException {
		logger.info("Starting server");
		//Start the server
		myServer.start();
		//Start the main menu
		menu();
		//Notify clients about shutdown:
		System.out.println("Shutting down the server.");
		try {
			serverNodeManager.sendEvent("OPC UA Server shut down.");
		} catch (StatusException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		myServer.shutdown(5, new LocalizedText("Server shutting down", Locale.ENGLISH));
	}
	
	//Simple utility function for getting and (on an incredibly basic level) verifying user inputs
	//Gets the server type and ports, then sets the APP_NAME based on the input.
	protected static void getUserConfigInputs() {
		int tempInt;
		logger.info("Configuring Server");
		do {
			System.out.print("Enter the port for the server: ");
			try {
				port = Integer.parseInt(readInput());
			} catch (Exception e) {
				printException(e);
			}
			System.out.print("\nEnter the HTTPS port for the server: ");
			try {
				httpsPort = Integer.parseInt(readInput());
			} catch (Exception e) {
				printException(e);
			}
			
			System.out.print("\nEnter the name of the server without spaces: ");
			try {
				APP_NAME = readInput();
			}catch(Exception e) {
				printException(e);
			}
			
			System.out.print("\nEnter the server type:\n"+ "1. Belt Server\n"
				+ "2. Belt and Gripper Server\n" + "3. Jack and Sledge Server\n"
					+ "4. Virtual Reaction Tank \n" +  "5. Combined Server\n");
			try {
				tempInt = Integer.parseInt(readInput());
				
				if (tempInt<=5 && tempInt>=1) {
					serverTypeInt = tempInt;
					
					switch(serverTypeInt) {
					case 1:
						serverType = "BeltServer";
						break;
					case 2:
						serverType = "BeltandGripperServer";
						break;
					case 3:
						serverType = "JackandSledgeServer";
						break;
					case 4:
						serverType = "VirtualTankServer";
						break;
					case 5:
						serverType = "CombinedServer";
					}
					return;
				} else {
					System.out.println("Invalid Inputs");
					continue;
				}
			} catch(Exception e) {
				printException(e);
			}
		}while(true);
	}
	
	//Main function for doing things.
	//Handles server setup etc
	public static void main(String [] args) throws StatusException, UaServerException, IOException, SecureIdentityException, InterruptedException {
		//Check if arguments have been submitted which include necessary information.
		//If they have not or the arguments are invalid gather necessary information from user.
		try {
			if (!parseArgs(args)) {
				getUserConfigInputs();
			}
		} catch (IllegalArgumentException e) {
			getUserConfigInputs();
		}
		
		//Create the new server and initialize it
		ModularServerMain  myDeviceServer= new ModularServerMain();
		myDeviceServer.initializeServer(port, httpsPort, APP_NAME);
		myDeviceServer.createAddressSpace();
		
		//Run the server.
		logger.info("Starting server");
		myDeviceServer.run();
	}
}
