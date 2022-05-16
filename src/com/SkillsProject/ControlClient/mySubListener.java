package com.SkillsProject.ControlClient;

import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.MonitoredEventItem;
import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.SubscriptionNotificationListener;
import com.prosysopc.ua.samples.client.SampleConsoleClient;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.DiagnosticInfo;
import com.prosysopc.ua.stack.builtintypes.ExtensionObject;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.builtintypes.Variant;
import com.prosysopc.ua.stack.core.NotificationData;

public class mySubListener implements SubscriptionNotificationListener {

	@Override
	public void onBufferOverflow(Subscription arg0, UnsignedInteger arg1, ExtensionObject[] arg2) {
		// TODO Auto-generated method stub
		System.out.println("BUFFER OVERFLOW BAD");
	}

	@Override
	public void onDataChange(Subscription arg0, MonitoredDataItem arg1, DataValue arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onError(Subscription arg0, Object arg1, Exception arg2) {
		// TODO Auto-generated method stub
		System.out.println("Error: " + arg2);
	}

	@Override
	public void onEvent(Subscription arg0, MonitoredEventItem arg1, Variant[] arg2) {
		// TODO Auto-generated method stub

	}

	@Override
	public long onMissingData(Subscription arg0, UnsignedInteger arg1, long arg2, long arg3, StatusCode arg4) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void onNotificationData(Subscription subscription, NotificationData notification) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChange(Subscription subscription, StatusCode oldStatus, StatusCode newStatus,
			DiagnosticInfo diagnosticInfo) {
		// TODO Auto-generated method stub

	}

}
