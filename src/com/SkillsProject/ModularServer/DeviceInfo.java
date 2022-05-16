/* Deprecated type, replaced with a function to create a set of PlainVariable objects to store same information.
 * Kept just in case its absence would cause unforeseen issues.
 */

package com.SkillsProject.ModularServer;

//Just a simple class for containing the information property for devices.
public class DeviceInfo {
	public final String manufacturer;
	public final String model;
	public final String serialNo;
	public final String revisionNo;
	public final String info;
	
	public DeviceInfo(String Manuf, String Model, String SerialNo, String RevisionNo, String Info) {
		this.manufacturer = Manuf;
		this.model = Model;
		this.serialNo = SerialNo;
		this.revisionNo = RevisionNo;
		this.info = Info;
	}
	
	public String toString() {
		String infoString = "Manufacturer: " + manufacturer + ", Model: " + model + ", Serial Number: " +
				serialNo + ", Revision Number: " + revisionNo + ", Additional information: " + info;
		return infoString;
	}
	
	public String returnManuf() {
		return manufacturer;
	}
	
	public String returnModel() {
		return model;
	}
}
