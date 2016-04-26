package com.commit451.gitlab.activity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.afollestad.appthemeengine.customizers.ATEActivityThemeCustomizer;
import com.commit451.gitlab.LabCoatApp;
import com.commit451.gitlab.R;
import com.commit451.gitlab.api.GitLabClient;
import com.commit451.gitlab.event.CloseDrawerEvent;
import com.commit451.gitlab.fragment.FeedFragment;
import com.squareup.otto.Subscribe;

import butterknife.Bind;
import butterknife.ButterKnife;
import timber.log.Timber;

/**
 * Displays the current users projects feed
 */
public class ActivityActivity extends BaseActivity implements ATEActivityThemeCustomizer {

    private static final String TAG_FEED_FRAGMENT = "feed_fragment";

    @Override
    public int getActivityTheme() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean("dark_theme", true) ?
                R.style.Activity_Activity : R.style.ActivityLight_Activity;
    }

    public static Intent newInstance(Context context) {
        Intent intent = new Intent(context, ActivityActivity.class);
        return intent;
    }

    @Bind(R.id.drawer_layout) DrawerLayout mDrawerLayout;
    @Bind(R.id.toolbar) Toolbar mToolbar;

    EventReceiver mEventReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity);
        ButterKnife.bind(this);

        mEventReceiver = new EventReceiver();
        LabCoatApp.bus().register(mEventReceiver);

        mToolbar.setTitle(R.string.nav_activity);
        mToolbar.setNavigationIcon(R.drawable.ic_menu_24dp);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
        });

        FeedFragment feedFragment = (FeedFragment) getSupportFragmentManager().findFragmentByTag(TAG_FEED_FRAGMENT);
        if (feedFragment == null) {
            Uri feedUri = GitLabClient.getAccount().getServerUrl();

            feedUri = feedUri.buildUpon()
                    .appendPath("dashboard")
                    .appendPath("projects.atom")
                    .build();
            Timber.d("Showing activity feed for: %s", feedUri.toString());

            feedFragment = FeedFragment.newInstance(feedUri);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.root_fragment, feedFragment, TAG_FEED_FRAGMENT)
                    .commit();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LabCoatApp.bus().unregister(mEventReceiver);
    }

    private class EventReceiver {

        @Subscribe
        public void onCloseDrawerEvent(CloseDrawerEvent event) {
            mDrawerLayout.closeDrawers();
        }
    }
}