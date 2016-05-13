package com.commit451.gitlab.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.commit451.gitlab.R;
import com.commit451.gitlab.api.EasyCallback;
import com.commit451.gitlab.api.GitLabClient;
import com.commit451.gitlab.model.api.MergeRequest;
import com.commit451.gitlab.model.api.Project;
import com.commit451.gitlab.model.api.RepositoryCommit;
import com.commit451.gitlab.navigation.NavigationManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

/**
 * Intermediate activity when deep linking to another activity and things need to load
 */
public class LoadSomeInfoActivity extends AppCompatActivity {

    private static final String EXTRA_LOAD_TYPE = "load_type";
    private static final String EXTRA_PROJECT_NAMESPACE = "project_namespace";
    private static final String EXTRA_PROJECT_NAME = "project_name";
    private static final String EXTRA_COMMIT_SHA = "extra_commit_sha";
    private static final String EXTRA_MERGE_REQUEST = "merge_request";

    private static final int LOAD_TYPE_DIFF = 0;
    private static final int LOAD_TYPE_MERGE_REQUEST = 1;

    public static Intent newIntent(Context context, String namespace, String projectName, String commitSha) {
        Intent intent = new Intent(context, LoadSomeInfoActivity.class);
        intent.putExtra(EXTRA_PROJECT_NAMESPACE, namespace);
        intent.putExtra(EXTRA_PROJECT_NAME, projectName);
        intent.putExtra(EXTRA_COMMIT_SHA, commitSha);
        intent.putExtra(EXTRA_LOAD_TYPE, LOAD_TYPE_DIFF);
        return intent;
    }

    public static Intent newMergeRequestIntent(Context context, String namespace, String projectName, String mergeRequestId) {
        Intent intent = new Intent(context, LoadSomeInfoActivity.class);
        intent.putExtra(EXTRA_PROJECT_NAMESPACE, namespace);
        intent.putExtra(EXTRA_PROJECT_NAME, projectName);
        intent.putExtra(EXTRA_MERGE_REQUEST, mergeRequestId);
        intent.putExtra(EXTRA_LOAD_TYPE, LOAD_TYPE_MERGE_REQUEST);
        return intent;
    }

    @BindView(R.id.progress)
    View mProgress;

    private int mLoadType;

    private Project mProject;

    @OnClick(R.id.root)
    void onRootClicked() {
        finish();
    }

    private final EasyCallback<Project> mProjectCallback = new EasyCallback<Project>() {
        @Override
        public void onResponse(@NonNull Project response) {
            mProject = response;
            switch (mLoadType) {
                case LOAD_TYPE_DIFF:
                    String sha = getIntent().getStringExtra(EXTRA_COMMIT_SHA);
                    GitLabClient.instance().getCommit(response.getId(), sha).enqueue(mCommitCallback);
                    return;
                case LOAD_TYPE_MERGE_REQUEST:
                    String mergeRequestId = getIntent().getStringExtra(EXTRA_MERGE_REQUEST);
                    GitLabClient.instance().getMergeRequest(response.getId(), Long.valueOf(mergeRequestId)).enqueue(mMergeRequestCallback);
            }

        }

        @Override
        public void onAllFailure(Throwable t) {
            Timber.e(t, null);
            onError();
        }
    };

    private final EasyCallback<RepositoryCommit> mCommitCallback = new EasyCallback<RepositoryCommit>() {
        @Override
        public void onResponse(@NonNull RepositoryCommit response) {
            NavigationManager.navigateToDiffActivity(LoadSomeInfoActivity.this, mProject, response);
            finish();
        }

        @Override
        public void onAllFailure(Throwable t) {
            Timber.e(t, null);
            onError();
        }
    };

    private final EasyCallback<MergeRequest> mMergeRequestCallback = new EasyCallback<MergeRequest>() {
        @Override
        public void onResponse(@NonNull MergeRequest response) {
            NavigationManager.navigateToMergeRequest(LoadSomeInfoActivity.this, mProject, response);
            finish();
        }

        @Override
        public void onAllFailure(Throwable t) {
            Timber.e(t, null);
            onError();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
        ButterKnife.bind(this);
        mProgress.setVisibility(View.VISIBLE);
        mLoadType = getIntent().getIntExtra(EXTRA_LOAD_TYPE, -1);
        Timber.d("Loading some info type: %d", mLoadType);

        switch (mLoadType) {
            case LOAD_TYPE_DIFF:
            case LOAD_TYPE_MERGE_REQUEST:
                String namespace = getIntent().getStringExtra(EXTRA_PROJECT_NAMESPACE);
                String project = getIntent().getStringExtra(EXTRA_PROJECT_NAME);
                GitLabClient.instance().getProject(namespace, project).enqueue(mProjectCallback);
                break;
        }
    }

    private void onError() {
        Toast.makeText(LoadSomeInfoActivity.this, R.string.failed_to_load, Toast.LENGTH_SHORT)
                .show();
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.do_nothing, R.anim.fade_out);
    }
}
