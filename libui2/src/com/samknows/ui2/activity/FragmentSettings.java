package com.samknows.ui2.activity;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.telephony.NeighboringCellInfo;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.samknows.libcore.SKLogger;
import com.samknows.libcore.SKConstants;
import com.samknows.measurement.SK2AppSettings;
import com.samknows.measurement.CachingStorage;
import com.samknows.measurement.MainService;
import com.samknows.measurement.SKApplication;
import com.samknows.libui2.R;

import com.samknows.measurement.activity.BaseLogoutActivity;
import com.samknows.measurement.activity.components.ButtonWithRightArrow;
import com.samknows.measurement.activity.components.FontFitTextView;
import com.samknows.measurement.activity.components.Util;
import com.samknows.measurement.environment.CellTowersData;
import com.samknows.measurement.environment.CellTowersDataCollector;
import com.samknows.measurement.environment.NetworkData;
import com.samknows.measurement.environment.NetworkDataCollector;
import com.samknows.measurement.environment.PhoneIdentityData;
import com.samknows.measurement.environment.PhoneIdentityDataCollector;
import com.samknows.measurement.schedule.ScheduleConfig;
import com.samknows.measurement.storage.DBHelper;
import com.samknows.measurement.util.DCSConvertorUtil;
import com.samknows.measurement.util.SKDateFormat;
import com.samknows.measurement.util.SKGsmSignalStrength;
import com.samknows.ska.activity.SKAActivationActivity;
import com.samknows.ska.activity.SKAMainResultsActivity;
import com.samknows.ska.activity.SKAPreferenceActivity;

public class FragmentSettings extends Fragment{

	private static final String TAG = FragmentSettings.class.getSimpleName();
    
    void showAlertForTestWithMessageBody (String bodyMessage) {

    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    	builder.setTitle(R.string.running_test);
    	builder.setMessage(bodyMessage)
    	.setCancelable(false)
    	.setPositiveButton(R.string.ok_dialog, new DialogInterface.OnClickListener() {
    		public void onClick(DialogInterface dialog, int id) {
    			dialog.dismiss();
    		}
    	});
    	builder.create().show();

    }
	
    // Shown when the background test is already running, so we cannot clear
    // data at the moment...
    void showAlertCannotClearDataAsBackgroundTaskIsRunning () {
      showAlertForTestWithMessageBody (getString(R.string.must_wait_for_test_to_run));
    }
    
	
	private boolean checkIfIsConnectedAndIfNotShowAnAlert() {
		
		if (NetworkDataCollector.sGetIsConnected() == true) {
			return true;
		}
	
		// We're not connected - show an alert if possible, and return false!
		if ((getActivity() != null) && !getActivity().isFinishing()) {
			new AlertDialog.Builder(getActivity())
			.setMessage(R.string.Offline_message)
			.setPositiveButton(R.string.ok_dialog, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
				}
			}).show();
		}
		
		return false;
	}
	
	@SuppressLint("InlinedApi")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Create the view
		View view = inflater.inflate(R.layout.ska_settings_activity, container, false); 
		SKLogger.sAssert(getClass(), view != null);
	
		Activity activity = getActivity();
		SKLogger.sAssert(getClass(), activity != null);
		//Util.initializeFonts(getActivity());
		//Util.overrideFonts(getActivity(), view.findViewById(android.R.id.content));
		
		String versionName="";
		try {
			versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0 ).versionName;
		} catch (NameNotFoundException e) {
			SKLogger.sAssert(getClass(), false);
		}
		if (view.findViewById(R.id.version) != null) {
			TextView tv=(TextView) view.findViewById(R.id.version);
			tv.setText(getString(R.string.version)+ " " + versionName);
		}

		final ButtonWithRightArrow clearAllResultsButton = (ButtonWithRightArrow) view.findViewById(R.id.settings_clearallresults_button);
		
		if (clearAllResultsButton != null) {
			clearAllResultsButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					//Log.w(TAG, "TODO: clear all results clicked...");

					Builder builder = new AlertDialog.Builder(FragmentSettings.this.getActivity());
					builder.setTitle(getString(R.string.Settings_ClearAllResults_Title));
					builder.setMessage(getString(R.string.Settings_ClearAllResults_Message));
					builder.setPositiveButton(getString(R.string.ok_dialog),
							new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {

							if (MainService.isExecuting()) {
								showAlertCannotClearDataAsBackgroundTaskIsRunning();
								return;
							}

							// Clear all results!
							// ... and let the main screen respond to this!
							SKApplication.sSetUpdateAllDataOnScreen(true);

							DBHelper db = new DBHelper(FragmentSettings.this.getActivity());
							db.emptyTheDatabase();

							dialog.dismiss();
						}
					});
					builder.setNegativeButton(getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
					AlertDialog alert = builder.create();
					alert.show();
				}
			});
		}
		
		ButtonWithRightArrow activateButton = (ButtonWithRightArrow) view.findViewById(R.id.settings_activate_button);
		if (activateButton != null) {
			activateButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (FragmentSettings.this.checkIfIsConnectedAndIfNotShowAnAlert() == true) {
						SKAActivationActivity.sDoShowActivation(FragmentSettings.this.getActivity());
					}
				}
			});
		}
		
		ButtonWithRightArrow preferencesButton = (ButtonWithRightArrow) view.findViewById(R.id.settings_preferences_button);
		preferencesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentSettings.this.startActivity(new Intent(FragmentSettings.this.getActivity(), SKAPreferenceActivity.class));
			}
		});	
    	if (SKApplication.getAppInstance().canViewDataCapInSettings() == false)
    	{
    		preferencesButton.setVisibility(View.GONE);
    	}
    	
		ButtonWithRightArrow aboutUsButton = (ButtonWithRightArrow) view.findViewById(R.id.settings_aboutus_button);
		if (aboutUsButton != null) {
			aboutUsButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					SKApplication.getAppInstance().showAbout(FragmentSettings.this.getActivity());
				}
			});	
		}
		
		ButtonWithRightArrow termsButton = (ButtonWithRightArrow) view.findViewById(R.id.settings_termsandconditions);
		if (termsButton != null) {
			termsButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					SKApplication.getAppInstance().showTermsAndConditions(FragmentSettings.this.getActivity());
				}
			});	
		}
		
		ButtonWithRightArrow exportResultsButton = (ButtonWithRightArrow) view.findViewById(R.id.settings_exportresults_button);
		exportResultsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				SKAMainResultsActivity.sExportMenuItemSelected(FragmentSettings.this.getActivity(), getActivity().getCacheDir());
			}
		});	
		
		FontFitTextView locationServicesTypeButtonText = null;
		try {
			locationServicesTypeButtonText = (FontFitTextView) view.findViewById(R.id.settings_location_services_type_text);
		} catch (NoSuchFieldError e) { }
		
		ButtonWithRightArrow locationServicesTypeButton = null;
		try {
    		locationServicesTypeButton = (ButtonWithRightArrow) view.findViewById(R.id.settings_location_services_type);
		} catch (NoSuchFieldError e) { }
		
		String theValue = SK2AppSettings.getSK2AppSettingsInstance().getLocationTypeAsString();
		if (locationServicesTypeButtonText != null) {
			locationServicesTypeButtonText.setText(getString(R.string.location_type_title) + ": " + theValue);
		}
		if (locationServicesTypeButton != null) {
     		final FontFitTextView finalLocationServicesTypeButtonText = locationServicesTypeButtonText;
			locationServicesTypeButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					final String the_array_spinner[] = new String[2];
					the_array_spinner[0] = getString(R.string.GPS);
					the_array_spinner[1] = getString(R.string.MobileNetwork);

					Builder builder = new AlertDialog.Builder(FragmentSettings.this.getActivity());
					builder.setTitle(getString(R.string.location_type_title));

					builder.setItems(the_array_spinner, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {

							dialog.dismiss();

							String theValue = the_array_spinner[which];

							if(theValue.equals(getString(R.string.GPS))) {
							} else if (theValue.equals(getString(R.string.MobileNetwork))) {
							} else {
								SKLogger.sAssert(FragmentSettings.class, false);
							}

							SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(FragmentSettings.this.getActivity());
							prefs.edit().putString(SKConstants.PREF_LOCATION_TYPE, theValue).commit();

							String theValue2 = SK2AppSettings.getSK2AppSettingsInstance().getLocationTypeAsString();
							finalLocationServicesTypeButtonText.setText(getString(R.string.location_type_title) + ": " + theValue);
						}
					});
					builder.setNegativeButton(getString(R.string.cancel),
							new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
					AlertDialog alert = builder.create();
					alert.show();
				}
			});	
		}
		
		return view;
	}
	
	@Override
	public void onResume() {
		View view = getView();
		populateInfo(view);
		super.onResume();
	}

	private void populateInfo(View view) {
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
		
		try {
			((TextView)view.findViewById(R.id.tv_service_activated_value)).setText(value);
		} catch (NoSuchFieldError e) { }
		
		if(SKApplication.getAppInstance().getIsBackgroundTestingEnabledInUserPreferences()){
			value = getString(R.string.enabled);
		}else{
			value = getString(R.string.disabled);
		}
		
		try {
			((TextView)view.findViewById(R.id.tv_service_autotesting_value)).setText(value);
		} catch (NoSuchFieldError e) { }
		
		try {
			((TextView)view.findViewById(R.id.tv_service_status_value)).setText(getString(SK2AppSettings.getSK2AppSettingsInstance().getState().sId));
		} catch (NoSuchFieldError e) { }

		ScheduleConfig config = CachingStorage.getInstance().loadScheduleConfig();
		String schedule_version = config == null ? "" : config.getConfigVersion(); 
		
		try {
			((TextView)view.findViewById(R.id.schedule_version)).setText(schedule_version);
		} catch (NoSuchFieldError e) { }

		String nextTestScheduled = "";
		if (MainService.isExecuting()) {
			nextTestScheduled = getString(R.string.executing_now);
		} else {
			long nextRunTime = SK2AppSettings.getInstance().getNextRunTime();
			if (nextRunTime == SKConstants.NO_NEXT_RUN_TIME) {
				nextTestScheduled = getString(R.string.none);
			} else {
				nextTestScheduled = new SKDateFormat(getActivity()).UITime(nextRunTime);
			}
		}
		try {
			((TextView)view.findViewById(R.id.tv_scheduledFor_value)).setText(nextTestScheduled);
		} catch (NoSuchFieldError e) { }

		if (SKApplication.getAppInstance().getIsBackgroundProcessingEnabledInTheSchedule() == false) {
			// Background processing disabled in the schedule!
			try {
				view.findViewById(R.id.autotesting_row).setVisibility(View.GONE);
			} catch (NoSuchFieldError e) { }
			try {
				view.findViewById(R.id.next_test_scheduled_for_row).setVisibility(View.GONE);
			} catch (NoSuchFieldError e) { }
		}

		PhoneIdentityData phoneData = new PhoneIdentityDataCollector(getActivity()).collect();
		if (!SK2AppSettings.getSK2AppSettingsInstance().anonymous){
			try {
				((TextView)view.findViewById(R.id.tv_imei_value)).setText(phoneData.imei + "");
			} catch (NoSuchFieldError e) { }
			try {
				((TextView)view.findViewById(R.id.tv_imsi_value)).setText(phoneData.imsi + "");
			} catch (NoSuchFieldError e) { }
			try {
				((TextView)view.findViewById(R.id.tv_unitId_value)).setText(SK2AppSettings.getInstance().getUnitId());
			} catch (NoSuchFieldError e) { }
		}

		value = phoneData.manufacturer + "\n\r" + phoneData.model;
		try {
			((TextView)view.findViewById(R.id.tv_phone_value)).setText(value);
		} catch (NoSuchFieldError e) { }
		value = phoneData.osType + " v" + phoneData.osVersion;
		try {
			((TextView)view.findViewById(R.id.tv_os_value)).setText(value);
		} catch (NoSuchFieldError e) { }

		NetworkData networkData = new NetworkDataCollector(getActivity()).collect();
		value = DCSConvertorUtil.convertPhoneType(networkData.phoneType);
		try {
			((TextView)view.findViewById(R.id.tv_phone_type_value)).setText(value);
		} catch (NoSuchFieldError e) { }
		value = getString(DCSConvertorUtil.networkTypeToStringId(networkData.networkType));
		try {
			((TextView)view.findViewById(R.id.tv_network_type_value)).setText(value);
		} catch (NoSuchFieldError e) { }
		value = networkData.networkOperatorCode + "/" + networkData.networkOperatorName;
		try {
			((TextView)view.findViewById(R.id.tv_network_operator_value)).setText(value);
		} catch (NoSuchFieldError e) { }
		
		if(networkData.isRoaming){
			value = getString(R.string.yes);
		}else{
			value = getString(R.string.no);
		}
		try {
			((TextView)view.findViewById(R.id.tv_roaming_value)).setText(value);
		} catch (NoSuchFieldError e) { }

		Location loc1 = ((LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE)).getLastKnownLocation(LocationManager.GPS_PROVIDER);
		Location loc2 = ((LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE)).getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		Location loc = null;
		if (loc1 != null && loc2 != null) {
			loc = loc1.getTime() > loc2.getTime() ? loc1 : loc2;
		} else {
			loc = loc1 == null ? loc2 : loc1;
		}
		if (loc != null) {
			try {
				((TextView)view.findViewById(R.id.tv_loc_date_value)).setText(new SKDateFormat(getActivity()).UITime(loc.getTime()));
			} catch (NoSuchFieldError e) { }
			try {
				((TextView)view.findViewById(R.id.tv_loc_provider_value)).setText(loc.getProvider());
			} catch (NoSuchFieldError e) { }
			try {
				((TextView)view.findViewById(R.id.tv_loc_long_value)).setText(String.format("%1.5f", loc.getLongitude()));
			} catch (NoSuchFieldError e) { }
			try {
				((TextView)view.findViewById(R.id.tv_loc_lat_value)).setText(String.format("%1.5f", loc.getLatitude()));
			} catch (NoSuchFieldError e) { }
			try {
				((TextView)view.findViewById(R.id.tv_loc_acc_value)).setText(loc.getAccuracy() + " m");
			} catch (NoSuchFieldError e) { }
		}


		//Cells
		CellTowersData cellData = new CellTowersDataCollector(getActivity()).collect();
		if (cellData.getCellLocation() == null) {
			// No location information currently available!
		} else if (cellData.getCellLocation() instanceof GsmCellLocation) {
			GsmCellLocation gsmLocation = (GsmCellLocation) cellData.getCellLocation();
			try {
				((TextView)view.findViewById(R.id.tv_cell_tower_type_value)).setText("GSM");
			} catch (NoSuchFieldError e) { }
			try {
				((TextView)view.findViewById(R.id.tv_cell_id_value)).setText("" + gsmLocation.getCid());
			} catch (NoSuchFieldError e) { }
			try {
				((TextView)view.findViewById(R.id.tv_area_code_value)).setText("" + gsmLocation.getLac());
			} catch (NoSuchFieldError e) { }
		} else if (cellData.getCellLocation() instanceof CdmaCellLocation) {
			try {
				((TextView)view.findViewById(R.id.tv_cell_tower_type_value)).setText("CDMA");
			} catch (NoSuchFieldError e) { }
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

		try {
			((TextView) view.findViewById(R.id.tv_signal_value)).setText(value);
		} catch (NoSuchFieldError e) { }
		// Note: neighbors might be NULL...
		if (cellData.getNeighbors() != null) {
			for (NeighboringCellInfo info : cellData.getNeighbors()) {
				appendNeighborCellInfo(info);
			}
		}

		Util.initializeFonts(getActivity());
		Util.overrideFonts(getActivity(), view.findViewById(android.R.id.content));
	}
	
	public void appendNeighborCellInfo(NeighboringCellInfo data) {
		
		View view = getView();
		
		try {
			if (view.findViewById(R.id.info_table) == null) {
				return;
			}
		} catch (NoSuchFieldError e) {
			return;
		}
		
		TableRow tr = new TableRow(getActivity());
		int color = Color.parseColor("#000000");
		TextView label = new TextView(getActivity());
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
		
		if (view.findViewById(R.id.info_table) == null) {
			return;
		}
		((TableLayout)view.findViewById(R.id.info_table)).addView(tr, params);
		
		tr = new TableRow(getActivity());
		
		label = new TextView(getActivity());
		label.setLayoutParams(params);
		label.setText("Network type ");
		label.setTextSize(18);
		label.setTextColor(color);
		
		
		tr.setBackgroundResource(R.drawable.black_alpha);
		tr.addView(label);
		
		label = new TextView(getActivity());
		label.setTextSize(18);
		label.setLayoutParams(params2);
		label.setText(getString(DCSConvertorUtil.networkTypeToStringId(data.getNetworkType())));
		label.setTextColor(color);
		tr.addView(label);
		
		
		if (view.findViewById(R.id.info_table) == null) {
			return;
		}
		((TableLayout)view.findViewById(R.id.info_table)).addView(tr,params);
		tr.setLayoutParams(params);
		
		tr = new TableRow(getActivity());
		label = new TextView(getActivity());
		label.setTextSize(18);
		label.setTextColor(color);
		tr.setBackgroundResource(R.drawable.black_alpha);
		label.setLayoutParams(params);
		label.setText("PSC ");
		tr.addView(label);
		
		
		label = new TextView(getActivity());
		label.setText(data.getPsc() + "");
		label.setLayoutParams(params2);
		label.setTextColor(color);
		tr.addView(label);
		if (view.findViewById(R.id.info_table) == null) {
			return;
		}
		((TableLayout)view.findViewById(R.id.info_table)).addView(tr, params);
		
		tr.setLayoutParams(params);
		
		tr = new TableRow(getActivity());
		label = new TextView(getActivity());
		label.setLayoutParams(params);
		label.setText("Cell id ");
		label.setTextSize(18);
		label.setTextColor(color);
		tr.setBackgroundResource(R.drawable.black_alpha);
		tr.addView(label);
		
		
		label = new TextView(getActivity());
		label.setLayoutParams(params);
		label.setText(data.getCid() + "");
		label.setTextColor(color);
		label.setLayoutParams(params2);
		tr.addView(label);
		if (view.findViewById(R.id.info_table) == null) {
			return;
		}
		((TableLayout)view.findViewById(R.id.info_table)).addView(tr, params);
		tr.setLayoutParams(params);
		
		tr = new TableRow(getActivity());
		label = new TextView(getActivity());
		label.setLayoutParams(params);
		label.setText("Area code ");
		label.setTextSize(18);
		label.setTextColor(color);
		tr.setBackgroundResource(R.drawable.black_alpha);
		tr.addView(label);
		
		
		label = new TextView(getActivity());
		label.setText(data.getLac() + "");
		label.setTextColor(color);
		label.setLayoutParams(params2);
		tr.addView(label);
		if (view.findViewById(R.id.info_table) == null) {
			return;
		}
		((TableLayout)view.findViewById(R.id.info_table)).addView(tr,params);
		tr.setLayoutParams(params);
		
		tr = new TableRow(getActivity());
		label = new TextView(getActivity());
		label.setLayoutParams(params);
		label.setText("Signal Strength ");
		label.setTextSize(18);
		label.setTextColor(color);
		tr.setBackgroundResource(R.drawable.black_alpha);
		tr.addView(label);
		
		
		label = new TextView(getActivity());
		label.setLayoutParams(params);
		label.setText(data.getRssi() + "");
		label.setTextColor(color);
		label.setLayoutParams(params2);
		tr.addView(label);
		if (view.findViewById(R.id.info_table) == null) {
			return;
		}
		((TableLayout)view.findViewById(R.id.info_table)).addView(tr, params);
		tr.setLayoutParams(params);
	}

}
