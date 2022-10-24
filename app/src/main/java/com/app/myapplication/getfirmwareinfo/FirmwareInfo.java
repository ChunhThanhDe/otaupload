package com.app.myapplication.getfirmwareinfo;

import java.util.Date;

public class FirmwareInfo {

	private String firmwareNanme;
	private String firmwareVersion;
	private String firmwareUrl; 
	private String firmwareAttribute;
	private String firmwareMd5;
	private int firmwareSize;
	private Date firmwareDate;
	private String firmwareDescription;

	//nctmanh: 02112016 - for new ota flow- @{ 
	private String fwBasicUrl;
	private String fwBasicMd5;
	private int fwBasicSize;
	//@}
	
	public FirmwareInfo(String firmwareName, String firmwareVersion, String firmwareUrl, String firmwareAttribute, 
			String firmwareMd5, int firmwareSize, Date firmwareDate, String firmwareDescription, String fwBasicUrl, String fwBasicMd5, int fwBasicSize) {
		this.firmwareNanme = firmwareName;
		this.firmwareVersion = firmwareVersion;
		this.firmwareUrl = firmwareUrl;
		this.firmwareAttribute = firmwareAttribute;
		this.firmwareMd5 = firmwareMd5;
		this.firmwareSize = firmwareSize;
		this.firmwareDate = firmwareDate;
		this.firmwareDescription = firmwareDescription;
		this.fwBasicUrl = fwBasicUrl;
		this.fwBasicMd5 = fwBasicMd5;
		this.fwBasicSize = fwBasicSize;
	}

	public String getFirmwareName() {
		return firmwareNanme;
	}

	public String getFirmwareVersion() {
		return firmwareVersion;
	}

	public String getFirmwareUrl() {
		return firmwareUrl;
	}

	public String getFirmwareAttribute() {
		return firmwareAttribute;
	}

	public String getFirmwareMd5() {
		return firmwareMd5;
	}

	public int getFirmwareSize() {
		return firmwareSize;
	}

	public Date getFirmwareDate() {
		return firmwareDate;
	}

	public String getFirmwareDescription() {
		return firmwareDescription;
	}
	
	//nctmanh: 02112016 - for new ota flow - @{
	public String getFwBasicUrl() {
		return fwBasicUrl;
	}
	
	public String getFwBasicMd5() {
		return fwBasicMd5;
	}
	public int getFwBasicSize() {
		return fwBasicSize;
	}
	//@}
}
