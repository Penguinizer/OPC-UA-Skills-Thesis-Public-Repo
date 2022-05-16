package com.SkillsProject.ControlClient;

import com.prosysopc.ua.client.ConnectException;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.client.UaClientListener;
import com.prosysopc.ua.stack.application.Session;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.core.PublishRequest;
import com.prosysopc.ua.stack.core.PublishResponse;
import com.prosysopc.ua.stack.core.RepublishResponse;

public class controlClientListener implements UaClientListener {
	// One hour in milliseconds
	private static final long ALLOWED_PUBLISHTIME_DIFFERENCE = 3600000;
	
	@Override
	public void onAfterCreateSessionChannel(UaClient arg0, Session arg1) throws ConnectException {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBeforePublishRequest(UaClient arg0, PublishRequest arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean validatePublishResponse(UaClient client, PublishResponse response) {
		// TODO Auto-generated method stub
		return validatePublishTime(response.getNotificationMessage().getPublishTime());
	}

	@Override
	public boolean validateRepublishResponse(UaClient client, RepublishResponse response) {
		// TODO Auto-generated method stub
		return validatePublishTime(response.getNotificationMessage().getPublishTime());
	}
	private boolean validatePublishTime(DateTime publishTime) {
		return true;
	}

}
