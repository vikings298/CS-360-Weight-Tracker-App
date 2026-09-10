package com.example.mcnaneyprojectone;

import android.app.Activity;
import android.content.Intent;
import android.widget.LinearLayout;
import android.widget.Toast;

public class NavigationBar {

    public static final int HOME = 0;
    public static final int PROGRESS = 1;
    public static final int ACCOUNT = 2;

    public static void setupBottomNav(Activity activity, int currentPage, int userId) {

        LinearLayout homeNav = activity.findViewById(R.id.homeNav);
        LinearLayout progressNav = activity.findViewById(R.id.progressNav);
        LinearLayout accountNav = activity.findViewById(R.id.accountNav);

        homeNav.setOnClickListener(v -> {
            if (currentPage == HOME) {
                Toast.makeText(activity, "You are already on Home", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(activity, Home.class);
                intent.putExtra("USER_ID", userId);
                activity.startActivity(intent);
            }
        });

        progressNav.setOnClickListener(v -> {
            if (currentPage == PROGRESS) {
                Toast.makeText(activity, "You are already on Progress", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(activity, Progress.class);
                intent.putExtra("USER_ID", userId);
                activity.startActivity(intent);
            }
        });

        accountNav.setOnClickListener(v -> {
            if (currentPage == ACCOUNT) {
                Toast.makeText(activity, "You are already on Account", Toast.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(activity, Account.class);
                intent.putExtra("USER_ID", userId);
                activity.startActivity(intent);
            }
        });
    }
}