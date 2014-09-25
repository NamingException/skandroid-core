package com.samknows.ui2.activity;

import android.animation.LayoutTransition;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.samknows.measurement.SKApplication;
import com.samknows.ska.activity.SKAAboutActivity;
import com.samknows.ska.activity.SKAPreferenceActivity;
import com.samknows.ska.activity.SKASettingsActivity;
import com.samknows.ska.activity.SKATermsOfUseActivity;
import com.samknows.libui2.R;

/**
 * This activity is responsible for:
 * * Host the view pager and manage it
 * * Hosting the backgrounds and changing its colours
 * 
 * All rights reserved SamKnows
 * @author pablo@samknows.com
 */


public class FragmentActivityMain extends SamKnowsBaseFragmentActivity
{
	// *** CONSTANTS *** //
	private static final int C_NUMBER_OF_TABS = 3;	
	
	// *** VARIABLES *** //	
	// UI elements
	private LinearLayout layout_ll_background_middle, layout_ll_background_top;		// Linear layouts in the UI	
	private Typeface typeface_Roboto_Regular;										// The type face to be used in the action bar title

	// Other Classes
	private ViewPager viewPager;						// The view pager
	private MyAdapter adapter_ViewPager;				// The view pager adapter
	
	// Fragments
	private static Fragment fragmentArchivedResults;	// Represents the archived results fragment
	private static Fragment fragmentRunTest;			// Represents the run test fragment
	private static Fragment fragmentSummary;			// Represents the summary fragment


	// *** FRAGMENT ACTIVITY LIFECYCLE *** //
	// Called when the activity is starting. This is where most initialisation should go.
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);		
		setContentView(R.layout.fragment_main);
		
		// Bind and set up the resources
		setUpResources();
	}
	
	// *** INNER CLASSES *** //
	/**
	 * Implementation of PagerAdapter that represents each page as a Fragment that is persistently kept in the fragment manager as long as the user can return to the page.
	 */
	public static class MyAdapter extends FragmentPagerAdapter
	{
        public MyAdapter(FragmentManager fm)
        {
            super(fm);
        }

        // Get the number of tabs
        @Override
        public int getCount()
        {
            return C_NUMBER_OF_TABS;
        }

        // Get a fragment depending on the position of the view pager
        @Override
        public Fragment getItem(int position)
        {
            switch (position)
            {
            	// Case first tab, archived results fragment
	            case 0:
					if (fragmentRunTest == null)
					{
						fragmentRunTest = new FragmentRunTest();						
					}
					
					return fragmentRunTest;
					
				// Case second tab, run test fragment
	            case 1:
					if (fragmentArchivedResults == null)
					{
						fragmentArchivedResults = new FragmentArchivedResults();												
					}
					
					return fragmentArchivedResults;				
					
				// Case third tab, summary fragment
				case 2:
					if (fragmentSummary == null)
					{
						fragmentSummary = new FragmentSummary();						
					}
					
					return fragmentSummary;
					
				// Case default, run test fragment
				default:
					if (fragmentRunTest == null)
					{
						fragmentRunTest = new FragmentRunTest();						
					}
					
					return fragmentRunTest;
			}
        }
        
        // Get the page title
        @Override
        public CharSequence getPageTitle(int position)
        {        	
        	switch (position)
        	{
        		case 0:					
        			return "Speed Test";
        			
				case 1:
					return "Archive Results";
					
				case 2:					
					return "Summary";
					
				default:
					return "Speed Test";
			}        	
        }
    }
	
	// *** CUSTOM METHODS *** //
	/**
	 * Bind the resources with the objects in this class and set up them
	 */
	private void setUpResources()
	{
		// Set the font type to the action bat title
		typeface_Roboto_Regular = Typeface.createFromAsset(getAssets(), "fonts/roboto_regular.ttf");
		
		int titleId = getResources().getIdentifier("action_bar_title", "id", "android");
	    TextView tv_actionbar_title = (TextView) findViewById(titleId);	    
	    tv_actionbar_title.setTypeface(typeface_Roboto_Regular);
	    //tv_actionbar_title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

		// Different background layers
		layout_ll_background_middle = (LinearLayout) findViewById(R.id.background_main_fragment_activity_middle);
		layout_ll_background_top = (LinearLayout) findViewById(R.id.background_main_fragment_activity_top);
	    
        // Note that this assumes a LayoutTransition is set on the container, which is the case here because the container has the attribute "animateLayoutChanges" set to true
        // in the layout file. You can also call setLayoutTransition(new LayoutTransition()) in code to set a LayoutTransition on any container.
		// This is on to help us to achieve a smooth progress animation on the progress bar layout (progress background)
        LayoutTransition transition = ((RelativeLayout)findViewById(R.id.main_Fragment_Activity)).getLayoutTransition();
        
        // New capability as of Jellybean; monitor the container for *all* layout changes (not just add/remove/visibility changes) and animate these changes as well.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
        {
            transition.enableTransitionType(LayoutTransition.CHANGING);                
        }
        
		// Initialise the ViewPager and set an adapter
		adapter_ViewPager = new MyAdapter(getSupportFragmentManager());        
		viewPager = (ViewPager)findViewById(R.id.viewpager);		
		viewPager.setAdapter(adapter_ViewPager);		
		viewPager.setOffscreenPageLimit(2);

	    // Get the action bar
	    final ActionBar actionBar = getActionBar();
	    
		// Specify that tabs should be displayed in the action bar.
	    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

	    // Create a tab listener that is called when the user changes tabs.
	    // Callback interface invoked when a tab is focused, unfocused, added, or removed.
	    ActionBar.TabListener tabListener = new ActionBar.TabListener()
	    {
	    	// Called when a tab enters the selected state
			@Override
			public void onTabSelected(Tab tab, FragmentTransaction ft)
			{
				//Show the given tab. When the tab is selected, switch to the corresponding page in the ViewPager.
				viewPager.setCurrentItem(tab.getPosition());
				
				// Depending on the tab position, modify the background visibility
				switch (tab.getPosition())
				{
					// Case first tab, archived results
					case 0:
						layout_ll_background_middle.setAlpha(0.0f);
						break;
					// Case second tab, run test fragment
					case 1:
						layout_ll_background_middle.setAlpha(1.0f);
						layout_ll_background_top.setAlpha(0.0f);
						break;
					// Case third tab, summary fragment
					case 2:												
						layout_ll_background_top.setAlpha(1.0f);
						break;
					// Case default
					default:
						break;
				}
			}

			// Called when a tab exits the selected state
			@Override
			public void onTabUnselected(Tab tab, FragmentTransaction ft)
			{
				// Hide the given tab				
			}

			// Called when a tab that is already selected is chosen again by the user
			@Override
			public void onTabReselected(Tab tab, FragmentTransaction ft)
			{
				// Ignore this event for now				
			}
	    	
	    };
	    
	    // Set a listener that will be invoked whenever the page changes or is incrementally scrolled.
	    // This listener is used for changing smoothly the colour of the background, this is modifying the visibility of the different layers
	    viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener()
	    {
	    	// Called when the scroll state changes
	    	@Override
	    	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
	    	{
	    		super.onPageScrolled(position, positionOffset, positionOffsetPixels);
	    		
	    		switch (position)
	    		{
					case 0:
						layout_ll_background_middle.setAlpha(positionOffset);
						break;
						
					case 1:						
						layout_ll_background_top.setAlpha(positionOffset);
						break;
	
					default:
						break;
				}	    		
	    	}
	    	
	    	// This method will be invoked when a new page becomes selected
	    	@Override
	        public void onPageSelected(int position)
	    	{	    		
	            getActionBar().setSelectedNavigationItem(position);					// When swiping between pages, select the corresponding tab
	            actionBar.setTitle(adapter_ViewPager.getPageTitle(position));		// Set the title
            }	    	
        });
	    
	    // Adding tabs, specifying the tab's text and TabListener
	    for (int i = 0; i < C_NUMBER_OF_TABS; i++)
	    {
	    	Tab tab = actionBar.newTab();
	    	
	    	switch (i)
	    	{
	    		// Case first tab
		    	case 0:
					tab.setIcon(R.drawable.ic_action_home);							// Add the icon
					actionBar.addTab(tab.setTabListener(tabListener), 0, true);		// Add the tab and listener
					break;
				
				// Case second tab
		    	case 1:
					tab.setIcon(R.drawable.ic_action_list);							// Add the icon
					actionBar.addTab(tab.setTabListener(tabListener), 1, false);	// Add the tab and listener
					break;
				
				// Case third tab
				case 2:				
					tab.setIcon(R.drawable.ic_action_chart);						// Add the icon
					actionBar.addTab(tab.setTabListener(tabListener), 2 , false);	// Add the tab and listener
					break;
				// Case default
				default:
					break;
			}
	    }
	}
	
	// *** MENUS *** //
	/**
	 * Create the options menu that displays the refresh and about options
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_fragment_activity_main, menu);
	
		MenuItem item = menu.findItem(R.id.menu_force_background_test);
		if (item != null) {
			item.setVisible(SKApplication.getAppInstance().isForceBackgroundMenuItemSupported());
		}
		
		return true;
	}
	
	/**
	 * Handle menu options
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle item selection		
		int itemId = item.getItemId();
		
		if (itemId == R.id.menu_item_fragment_activity_main_settings) {
			Intent intent_menu_settings = new Intent(this, SKASettingsActivity.class);
			startActivity(intent_menu_settings);
			
			return true;
		}
		
		if (itemId == R.id.menu_item_fragment_activity_main_terms_and_conditions) {
			Intent intent_terms_and_conditions = new Intent(this, SKATermsOfUseActivity.class);
			startActivity(intent_terms_and_conditions);
			
			return true;
		}
	
		if (itemId == R.id.menu_item_fragment_activity_main_about) {
    		// TODO - which about screen to show?!
			//Intent intent = new Intent(this, ActivityAbout.class);
			Intent intent = new Intent(this, SKAAboutActivity.class);
			startActivity(intent);
			
			return true;
		}
		
		if (itemId == R.id.menu_settings){
			startActivity(new Intent(this, SKASettingsActivity.class));
			
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
	

	// Pressing back in this screen, always allows back to close the
	// application.
	public boolean forceBackToAllowClose() {
		return true;
	}

	public boolean wouldBackButtonReturnMeToTheHomeScreen() {
		return true;
	}

}