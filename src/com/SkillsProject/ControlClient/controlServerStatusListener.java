package com.SkillsProject.ControlClient;

import com.prosysopc.ua.client.ServerStatusListener;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.stack.builtintypes.LocalizedText;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.core.ServerState;
import com.prosysopc.ua.stack.core.ServerStatusDataType;

public class controlServerStatusListener implements ServerStatusListener {

	@Override
	public void onShutdown(UaClient arg0, long arg1, LocalizedText arg2) {
		// TODO Auto-generated method stub
		System.out.println("Serfver shutdown in " + arg1 + " because : " + arg2);
	}

	@Override
	public void onStateChange(UaClient arg0, ServerState arg1, ServerState arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChange(UaClient arg0, ServerStatusDataType arg1, StatusCode arg2) {
		// TODO Auto-generated method stub

	}

}
