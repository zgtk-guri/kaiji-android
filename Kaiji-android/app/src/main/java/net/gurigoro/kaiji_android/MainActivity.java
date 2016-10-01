package net.gurigoro.kaiji_android;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);


    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }



    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_setting: {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.nav_dev_mode: {
                Intent intent = new Intent(MainActivity.this, DeveloperModeActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.nav_stat: {
                StatFragment fragment = new StatFragment();
                getFragmentManager().beginTransaction()
                        .replace(R.id.main_container, fragment, StatFragment.TAG)
                        .commit();
                break;
            }
            case R.id.nav_chat: {
                ChatFragment fragment = new ChatFragment();
                getFragmentManager().beginTransaction()
                        .replace(R.id.main_container, fragment, ChatFragment.TAG)
                        .commit();
                break;
            }
            case R.id.nav_add_user: {
                AddUserFragment fragment = new AddUserFragment();
                getFragmentManager().beginTransaction()
                        .replace(R.id.main_container, fragment, AddUserFragment.TAG)
                        .commit();
                break;
            }
            case R.id.nav_users_list: {
                UsersListFragment fragment = new UsersListFragment();
                getFragmentManager().beginTransaction()
                        .replace(R.id.main_container, fragment, UsersListFragment.TAG)
                        .commit();
                break;
            }
            case R.id.nav_poker: {
                PokerFragment fragment = new PokerFragment();
                getFragmentManager().beginTransaction()
                        .replace(R.id.main_container, fragment, PokerFragment.TAG)
                        .commit();
                break;
            }
            case R.id.nav_blackjack: {
                BlackjackFragment fragment = new BlackjackFragment();
                getFragmentManager().beginTransaction()
                        .replace(R.id.main_container, fragment, BlackjackFragment.TAG)
                        .commit();
                break;
            }
            case R.id.nav_baccarat: {
                BaccaratFragment fragment = new BaccaratFragment();
                getFragmentManager().beginTransaction()
                        .replace(R.id.main_container, fragment, BaccaratFragment.TAG)
                        .commit();
                break;
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
