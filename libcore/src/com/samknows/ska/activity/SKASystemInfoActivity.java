package com.samknows.ska.activity;

import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.NeighboringCellInfo;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.CachingStorage;
import com.samknows.measurement.MainService;
import com.samknows.libcore.R;

import com.samknows.measurement.activity.BaseLogoutActivity;
import com.samknows.measurement.activity.components.Util;
import com.samknows.measurement.environment.CellTowersData;
import com.samknows.measurement.environment.CellTowersDataCollector;
import com.samknows.measurement.environment.NetworkData;
import com.samknows.measurement.environment.NetworkDataCollector;
import com.samknows.measurement.environment.PhoneIdentityData;
import com.samknows.measurement.environment.PhoneIdentityDataCollector;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.util.DCSConvertorUtil;
import com.samknows.measurement.util.SKDateFormat;
import com.samknows.measurement.util.SKGsmSignalStrength;

public class SKASystemInfoActivity extends BaseLogoutActivity{

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.ska_system_info_activity);
		
		Util.initializeFonts(this);
		Util.overrideFonts(this, findViewById(android.R.id.content));
	}
	
	@Override
	public void onResume() {
		populateInfo();
		super.onResume();
	}

	private void populateInfo() {
		String value;
		if (MainService.isExecuting()) {
			value = getString(R.string.executing_now); 
		} else {
			if(SK2AppSettings.getInstance().isServiceActivated()){
				value = getString(R.string.yes);
			}else{
				value = getString(R.string.no);
			}
		}
		((TextView)findViewById(R.id.tv_service_activated_value)).setText(value);
		if(SK2AppSettings.getSK2AppSettingsInstance().getIsBackgroundTestingEnabledInUserPreferences()){
			value = getString(R.string.enabled);
		}else{
			value = getString(R.string.disabled);
		}
		((TextView)findViewById(R.id.tv_service_autotesting_value)).setText(value);
		((TextView)findViewById(R.id.tv_service_status_value)).setText(getString(SK2AppSettings.getSK2AppSettingsInstance().getState().sId));
		
		String versionName="";
		try {
			versionName = this.getPackageManager().getPackageInfo(this.getPackageName(), 0 ).versionName;
		} catch (NameNotFoundException e) {
			SKLogger.e(this, "Error in getting app version name.", e);
		}
		
		((TextView)findViewById(R.id.version)).setText(versionName);
		
		ScheduleConfig config = CachingStorage.getInstance().loadScheduleConfig();
		String schedule_version = config == null ? "" : config.getConfigVersion(); 
		((TextView)findViewById(R.id.schedule_version)).setText(schedule_version);
		
		String nextTestScheduled = "";
		if (MainService.isExecuting()) {
			nextTestScheduled = getString(R.string.executing_now);
		} else {
			long nextRunTime = SK2AppSettings.getInstance().getNextRunTime();
			if (nextRunTime == SKConstants.NO_NEXT_RUN_TIME) {
				nextTestScheduled = getString(R.string.none);
			} else {
				nextTestScheduled = new SKDateFormat(this).UITime(nextRunTime);
			}
		}
		((TextView)findViewById(R.id.tv_scheduledFor_value)).setText(nextTestScheduled);
	
		if (SK2AppSettings.getInstance().getIsBackgroundProcessingEnabledInTheSchedule() == false) {
			// Background processing disabled in the schedule!
			findViewById(R.id.autotesting_row).setVisibility(View.GONE);
			findViewById(R.id.next_test_scheduled_for_row).setVisibility(View.GONE);
		}

		PhoneIdentityData phoneData = new PhoneIdentityDataCollector(this).collect();
		if (!SK2AppSettings.getSK2AppSettingsInstance().anonymous){
			((TextView)findViewById(R.id.tv_imei_value)).setText(phoneData.imei + "");
			((TextView)findViewById(R.id.tv_imsi_value)).setText(phoneData.imsi + "");
			((TextView)findViewById(R.id.tv_unitId_value)).setText(SK2AppSettings.getInstance().getUnitId());
		}
		
		value = phoneData.manufacturer + "\n\r" + phoneData.model;
		((TextView)findViewById(R.id.tv_phone_value)).setText(value);
		value = phoneData.osType + " v" + phoneData.osVersion;
		((TextView)findViewById(R.id.tv_os_value)).setText(value);
		
		NetworkData networkData = new NetworkDataCollector(this).collect();
		value = DCSConvertorUtil.convertPhoneType(networkData.phoneType);
		((TextView)findViewById(R.id.tv_phone_type_value)).setText(value);
		value = getString(DCSConvertorUtil.networkTypeToStringId(networkData.networkType));
		((TextView)findViewById(R.id.tv_network_type_value)).setText(value);
		value = networkData.networkOperatorCode + "/" + networkData.networkOperatorName;
		((TextView)findViewById(R.id.tv_network_operator_value)).setText(value);
		if(networkData.isRoaming){
			value = getString(R.string.yes);
		}else{
			value = getString(R.string.no);
		}
		((TextView)findViewById(R.id.tv_roaming_value)).setText(value);
		
		Location loc1 = ((LocationManager)getSystemService(LOCATION_SERVICE)).getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Location loc2 = ((LocationManager)getSystemService(LOCATION_SERVICE)).getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location loc = null;
		if (loc1 != null && loc2 != null) {
			loc = loc1.getTime() > loc2.getTime() ? loc1 : loc2;
		} else {
			loc = loc1 == null ? loc2 : loc1;
		}
		if (loc != null) {
			((TextView)findViewById(R.id.tv_loc_date_value)).setText(new SKDateFormat(this).UITime(loc.getTime()));
			((TextView)findViewById(R.id.tv_loc_provider_value)).setText(loc.getProvider());
			((TextView)findViewById(R.id.tv_loc_long_value)).setText(String.format("%1.5f", loc.getLongitude()));
			((TextView)findViewById(R.id.tv_loc_lat_value)).setText(String.format("%1.5f", loc.getLatitude()));
			((TextView)findViewById(R.id.tv_loc_acc_value)).setText(loc.getAccuracy() + " m");
		}
		
		
		//Cells
		CellTowersData cellData = new CellTowersDataCollector(this).collect();
		if (cellData.getCellLocation() == null) {
			// No location information currently available!
		} else if (cellData.getCellLocation() instanceof GsmCellLocation) {
			GsmCellLocation gsmLocation = (GsmCellLocation) cellData.getCellLocation();
			((TextView)findViewById(R.id.tv_cell_tower_type_value)).setText("GSM");
			((TextView)findViewById(R.id.tv_cell_id_value)).setText("" + gsmLocation.getCid());
			((TextView)findViewById(R.id.tv_area_code_value)).setText("" + gsmLocation.getLac());
		} else if (cellData.getCellLocation() instanceof CdmaCellLocation) {
			((TextView)findViewById(R.id.tv_cell_tower_type_value)).setText("CDMA");
//			CdmaCellLocation cdmaLocation = (CdmaCellLocation) cellLocation;
//			builder.append(CDMA);
//			builder.append(time/1000);
//			builder.append(cdmaLocation.getBaseStationId());
//			builder.append(cdmaLocation.getBaseStationLatitude());
//			builder.append(cdmaLocation.getBaseStationLongitude());
//			builder.append(cdmaLocation.getNetworkId());
//			builder.append(cdmaLocation.getSystemId());
		}
		
		
	
		if (cellData.getSignal() == null) {
			// No signal information currently available!
		} else if (cellData.getSignal().isGsm()) {
			int signalStrength = SKGsmSignalStrength.getGsmSignalStrength(cellData.getSignal());
			value = DCSConvertorUtil.convertGsmSignalStrength(signalStrength);
		} else {
			value  = cellData.getSignal().getCdmaDbm() + " dBm";
		}
	
		((TextView) findViewById(R.id.tv_signal_value)).setText(value);
		// Note: neighbors might be NULL...
		if (cellData.getNeighbors() != null) {
			for (NeighboringCellInfo info : cellData.getNeighbors()) {
				appendNeighborCellInfo(info);
			}
		}
		
		Util.initializeFonts(this);
		Util.overrideFonts(this, findViewById(android.R.id.content));
	}
	
	public void appendNeighborCellInfo(NeighboringCellInfo data) {
		
		TableRow tr = new TableRow(this);
		int color = Color.parseColor("#000000");
		TextView label = new TextView(this);
		TableRow.LayoutParams params = new TableRow.LayoutParams();
		params.span = 2;
		params.leftMargin=10;
		params.rightMargin=10;
		params.bottomMargin=10;
		
		TableRow.LayoutParams params2 = new TableRow.LayoutParams();
		params2.weight=1;
		//params2.gravity=16;
		
		label.setLayoutParams(params);
		label.setText("Neighbor Cell Tower");
		label.setTextSize(20);
		label.setTextColor(Color.parseColor("#909090"));
		//label.setTypeface(null, Typeface.BOLD);
		//label.setGravity(Gravity.CENTER);
		//label.setPadding(0, 10, 0, 0);
		tr.addView(label);
		tr.setLayoutParams(params);
		
		((TableLayout)findViewById(R.id.info_table)).addView(tr, params);
		
		tr = new TableRow(this);
		
		label = new TextView(this);
		label.setLayoutParams(params);
		label.setText("Network type ");
		label.setTextSize(18);
		label.setTextColor(color);
		
		
		tr.setBackgroundResource(R.drawable.black_alpha);
		tr.addView(label);
		
		label = new TextView(this);
		label.setTextSize(18);
		label.setLayoutParams(params2);
		label.setText(getString(DCSConvertorUtil.networkTypeToStringId(data.getNetworkType())));
		label.setTextColor(color);
		tr.addView(label);
		
		
		((TableLayout)findViewById(R.id.info_table)).addView(tr,params);
		tr.setLayoutParams(params);
		
		tr = new TableRow(this);
		label = new TextView(this);
		label.setTextSize(18);
		label.setTextColor(color);
		tr.setBackgroundResource(R.drawable.black_alpha);
		label.setLayoutParams(params);
		label.setText("PSC ");
		tr.addView(label);
		
		
		label = new TextView(this);
		label.setText(data.getPsc() + "");
		label.setLayoutParams(params2);
		label.setTextColor(color);
		tr.addView(label);
		((TableLayout)findViewById(R.id.info_table)).addView(tr, params);
		
		tr.setLayoutParams(params);
		
		tr = new TableRow(this);
		label = new TextView(this);
		label.setLayoutParams(params);
		label.setText("Cell id ");
		label.setTextSize(18);
		label.setTextColor(color);
		tr.setBackgroundResource(R.drawable.black_alpha);
		tr.addView(label);
		
		
		label = new TextView(this);
		label.setLayoutParams(params);
		label.setText(data.getCid() + "");
		label.setTextColor(color);
		label.setLayoutParams(params2);
		tr.addView(label);
		((TableLayout)findViewById(R.id.info_table)).addView(tr, params);
		tr.setLayoutParams(params);
		
		tr = new TableRow(this);
		label = new TextView(this);
		label.setLayoutParams(params);
		label.setText("Area code ");
		label.setTextSize(18);
		label.setTextColor(color);
		tr.setBackgroundResource(R.drawable.black_alpha);
		tr.addView(label);
		
		
		label = new TextView(this);
		label.setText(data.getLac() + "");
		label.setTextColor(color);
		label.setLayoutParams(params2);
		tr.addView(label);
		((TableLayout)findViewById(R.id.info_table)).addView(tr,params);
		tr.setLayoutParams(params);
		
		tr = new TableRow(this);
		label = new TextView(this);
		label.setLayoutParams(params);
		label.setText("Signal Strength ");
		label.setTextSize(18);
		label.setTextColor(color);
		tr.setBackgroundResource(R.drawable.black_alpha);
		tr.addView(label);
		
		
		label = new TextView(this);
		label.setLayoutParams(params);
		label.setText(data.getRssi() + "");
		label.setTextColor(color);
		label.setLayoutParams(params2);
		tr.addView(label);
		((TableLayout)findViewById(R.id.info_table)).addView(tr, params);
		tr.setLayoutParams(params);
	}

}
