package com.app.myapplication.service;

interface IResponseQueryNewVersion {
	void haveNewVersion(String firmwareVersion, String firmwareName, String firmwareDate);
	void ErrorVersion(int response);
}
