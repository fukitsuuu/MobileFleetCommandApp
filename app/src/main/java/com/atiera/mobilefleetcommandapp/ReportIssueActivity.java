package com.atiera.mobilefleetcommandapp;

import android.os.Bundle;

public class ReportIssueActivity extends DashboardActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_issue);
        
        // Re-initialize drawer components after setting content view
        initializeDrawerComponents();
        
        // Set Report Issue as checked since this is the Report Issue page
        if (navigationView != null) {
            navigationView.setCheckedItem(R.id.nav_report_issue);
        }
    }
}
