package com.commit451.gitlab.activity;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.commit451.gitlab.GitLabApp;
import com.commit451.gitlab.R;
import com.commit451.gitlab.adapter.MilestoneIssuesAdapter;
import com.commit451.gitlab.api.GitLabClient;
import com.commit451.gitlab.event.MilestoneChangedEvent;
import com.commit451.gitlab.model.api.Issue;
import com.commit451.gitlab.model.api.Milestone;
import com.commit451.gitlab.model.api.Project;
import com.commit451.gitlab.util.NavigationManager;
import com.squareup.otto.Subscribe;

import org.parceler.Parcels;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;
import timber.log.Timber;

public class MilestoneActivity extends BaseActivity {

    private static final String EXTRA_PROJECT = "extra_project";
    private static final String EXTRA_MILESTONE = "extra_milestone";

    public static Intent newInstance(Context context, Project project, Milestone milestone) {
        Intent intent = new Intent(context, MilestoneActivity.class);
        intent.putExtra(EXTRA_PROJECT, Parcels.wrap(project));
        intent.putExtra(EXTRA_MILESTONE, Parcels.wrap(milestone));
        return intent;
    }

    @Bind(R.id.root)
    View mRoot;
    @Bind(R.id.toolbar)
    Toolbar mToolbar;
    @Bind(R.id.swipe_layout)
    SwipeRefreshLayout mSwipeRefreshLayout;
    @Bind(R.id.list)
    RecyclerView mDiffRecyclerView;
    MilestoneIssuesAdapter mMilestoneIssuesAdapter;
    @Bind(R.id.message_text)
    TextView mMessageText;

    private Project mProject;
    private Milestone mMilestone;

    EventReceiver mEventReceiver;

    @OnClick(R.id.add)
    void onAddClick() {
        NavigationManager.navigateToAddIssue(MilestoneActivity.this, null, mProject);
    }

    @OnClick(R.id.edit)
    void onEditClicked(View fab) {
        NavigationManager.navigateToEditMilestone(MilestoneActivity.this, fab, mProject, mMilestone);
    }

    private final Callback<List<Issue>> mIssuesCallback = new Callback<List<Issue>>() {
        @Override
        public void onResponse(Response<List<Issue>> response, Retrofit retrofit) {
            mSwipeRefreshLayout.setRefreshing(false);

            if (!response.isSuccess()) {
                Timber.e("Issues response was not a success: %d", response.code());
                mMessageText.setVisibility(View.VISIBLE);
                mMessageText.setText(R.string.connection_error_issues);
                mMilestoneIssuesAdapter.setIssues(null);
                return;
            }

            if (!response.body().isEmpty()) {
                mMessageText.setVisibility(View.GONE);
            } else {
                Timber.d("No issues found");
                mMessageText.setVisibility(View.VISIBLE);
                mMessageText.setText(R.string.no_issues);
            }

            mMilestoneIssuesAdapter.setIssues(response.body());
        }

        @Override
        public void onFailure(Throwable t) {
            Timber.e(t, null);

            mSwipeRefreshLayout.setRefreshing(false);

            mMessageText.setVisibility(View.VISIBLE);
            mMessageText.setText(R.string.connection_error);
            mMilestoneIssuesAdapter.setIssues(null);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_milestone);
        ButterKnife.bind(this);
        mEventReceiver = new EventReceiver();
        GitLabApp.bus().register(mEventReceiver);

        mProject = Parcels.unwrap(getIntent().getParcelableExtra(EXTRA_PROJECT));
        mMilestone = Parcels.unwrap(getIntent().getParcelableExtra(EXTRA_MILESTONE));

        mToolbar.setNavigationIcon(R.drawable.ic_back_24dp);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        mMilestoneIssuesAdapter = new MilestoneIssuesAdapter(new MilestoneIssuesAdapter.Listener() {
            @Override
            public void onIssueClicked(Issue issue) {
                NavigationManager.navigateToIssue(MilestoneActivity.this, mProject, issue);
            }
        });
        bind(mMilestone);
        mDiffRecyclerView.setAdapter(mMilestoneIssuesAdapter);
        mDiffRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadData();
            }
        });

        loadData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        GitLabApp.bus().unregister(mEventReceiver);
    }

    private void bind(Milestone milestone) {
        mToolbar.setTitle(milestone.getTitle());
        mMilestoneIssuesAdapter.setMilestone(milestone);
    }

    private void loadData() {
        mMessageText.setVisibility(View.GONE);
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                if (mSwipeRefreshLayout != null) {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            }
        });
        GitLabClient.instance().getMilestoneIssues(mProject.getId(), mMilestone.getId()).enqueue(mIssuesCallback);
    }

    private class EventReceiver {

        @Subscribe
        public void onMilestoneChanged(MilestoneChangedEvent event) {
            if (mMilestone.getId() == event.mMilestone.getId()) {
                mMilestone = event.mMilestone;
                bind(mMilestone);
            }
        }
    }
}
