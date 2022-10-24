package com.app.myapplication.getfirmwareinfo;

import android.content.Intent;

import com.app.myapplication.common.VnptOtaUtils;


public class FirmwareInfoUtils {

	public static final int NETWORK_NOT_CONNECT = 93;
	public static final int ERROR_CLIENT_PROTOCOL = 94;
	public static final int ERROR_PARSER_XML = 95;
	public static final int UNKNOWN_ERROR = 96;
	public static final int ERROR_GET_XML = 97;
	public static final int ERROR_PARSER_FIRMWARE_INFO = 98;

	public static final int NO_FIRMWARE_UPDATE = 100;
	public static final int DEVICE_NOT_MATCH = 101;

	private static final String FIRMWARE_NAME = "firmware_name";
	private static final String FIRMWARE_VERSION = "firmware_version";
	private static final String FIRMWARE_URL = "firmware_url";
	private static final String FIRMWARE_ATTRIBUTE = "firmware_attribute";
	private static final String FIRMWARE_MD5 = "firmware_md5";
	private static final String FIRMWARE_SIZE = "firmware_size";
	private static final String FIRMWARE_DATE = "firmware_date";
	private static final String FIRMWARE_DESC = "firmware_desc";
	//nctmanh: 02112016 - for new ota flow - @{
	private static final String FW_BASIC_URL = "fw_basic_url";
	private static final String FW_BASIC_MD5 = "fw_basic_md5";
	private static final String FW_BASIC_SIZE = "fw_basic_size";
	//@}

	public static String getCurRomVersion() {
		return (android.os.Build.DISPLAY);
	}


	public static boolean isAfterFirmware(FirmwareInfo fmrOri,FirmwareInfo fmrComp) {
		if ((fmrOri == null) || (fmrComp == null)) {
			return true;
		}
		try {
			//Nếu date của fmrOri sau date của Comp thì return true 
			return fmrOri.getFirmwareDate().after(fmrComp.getFirmwareDate());
		} catch (Exception e){
			return false;
		}
	}

	public static boolean isFirmwareAvailable(String firmwareVersion) {

		String bufCurVer = getCurRomVersion();
		if (bufCurVer == null || firmwareVersion == null)
			return false;

		// kiểm tra xem có hỗ trợ ro.product.otadowngrade không 
		if(VnptOtaUtils.supportDowngradeFW()) {
			if(!bufCurVer.equals(firmwareVersion))
				return true;
		} else {
			try {
				String romVer = firmwareVersion;
				if(firmwareVersion.lastIndexOf("_V") > 0)
					romVer = firmwareVersion.substring(firmwareVersion.lastIndexOf('V') + 1);
				String[] romSeparated = romVer.split("\\.");

				int romMajor = Integer.parseInt(romSeparated[0]);
				int romSubMajor = Integer.parseInt(romSeparated[1]);
				int romMinor = Integer.parseInt(romSeparated[2]);

				String curVer = bufCurVer;

				if(bufCurVer.lastIndexOf("_V") > 0)
					curVer = bufCurVer.substring(bufCurVer.lastIndexOf("V") + 1);
				String[] curRomSeparated = curVer.split("\\.");
				
				int curMajor = Integer.parseInt(curRomSeparated[0]);
				int curSubMajor = Integer.parseInt(curRomSeparated[1]);
				int curMinor = Integer.parseInt(curRomSeparated[2]);

				if (romMajor > curMajor) {
					return true;
				} else if (romMajor == curMajor) {
					if (romSubMajor > curSubMajor) {
						return true;
					} else if (romSubMajor == curSubMajor) {
						if (romMinor > curMinor)
							return  true;
					}
				}
			} catch (Exception e) {
				return false;
			}
		}

		return false;
	}

	public static FirmwareInfo getFirmwareInfoFromIntent(Intent i) {
		return new FirmwareInfo(i.getStringExtra(FIRMWARE_NAME), 
				i.getStringExtra(FIRMWARE_VERSION), 
				i.getStringExtra(FIRMWARE_URL), 
				i.getStringExtra(FIRMWARE_ATTRIBUTE), 
				i.getStringExtra(FIRMWARE_MD5), 
				i.getIntExtra(FIRMWARE_SIZE, -1), 
				VnptOtaUtils.convetStringToDate(i.getStringExtra(FIRMWARE_DATE)), 
				i.getStringExtra(FIRMWARE_DESC),
				i.getStringExtra(FW_BASIC_URL),	//nctmanh: 02112016 - for new ota flow
				i.getStringExtra(FW_BASIC_MD5),	//nctmanh: 02112016 - for new ota flow
				i.getIntExtra(FW_BASIC_SIZE, -1));	//nctmanh: 02112016 - for new ota flow
				
	}

	public static void addFirmwareInfoToIntent(Intent i, FirmwareInfo info) {
		i.putExtra(FIRMWARE_NAME, info.getFirmwareName());
		i.putExtra(FIRMWARE_VERSION, info.getFirmwareVersion());
		i.putExtra(FIRMWARE_URL, info.getFirmwareUrl());
		i.putExtra(FIRMWARE_ATTRIBUTE, info.getFirmwareAttribute());
		i.putExtra(FIRMWARE_MD5, info.getFirmwareMd5());
		i.putExtra(FIRMWARE_SIZE, info.getFirmwareSize());
		i.putExtra(FIRMWARE_DATE, VnptOtaUtils.convetDateToStringFullInfo(info.getFirmwareDate()));
		i.putExtra(FIRMWARE_DESC, info.getFirmwareDescription());
		i.putExtra(FW_BASIC_URL, info.getFwBasicUrl());	//nctmanh: 02112016 - for new ota flow
		i.putExtra(FW_BASIC_MD5, info.getFwBasicMd5());	//nctmanh: 02112016 - for new ota flow
		i.putExtra(FW_BASIC_SIZE, info.getFwBasicSize());	//nctmanh: 02112016 - for new ota flow
	}
}






















