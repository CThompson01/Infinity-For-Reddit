package ml.docilealligator.infinityforreddit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.Snackbar;
import com.libRG.CustomTextView;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import SubredditDatabase.SubredditRoomDatabase;
import butterknife.BindView;
import butterknife.ButterKnife;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;
import pl.droidsonroids.gif.GifImageView;
import retrofit2.Retrofit;

public class PostTextActivity extends AppCompatActivity implements FlairBottomSheetFragment.FlairSelectionCallback {

    static final String EXTRA_SUBREDDIT_NAME = "ESN";

    private static final String SUBREDDIT_NAME_STATE = "SNS";
    private static final String SUBREDDIT_ICON_STATE = "SIS";
    private static final String SUBREDDIT_SELECTED_STATE = "SSS";
    private static final String SUBREDDIT_IS_USER_STATE = "SIUS";
    private static final String LOAD_SUBREDDIT_ICON_STATE = "LSIS";
    private static final String FLAIR_STATE = "FS";
    private static final String IS_SPOILER_STATE = "ISS";
    private static final String IS_NSFW_STATE = "INS";

    private static final int SUBREDDIT_SELECTION_REQUEST_CODE = 0;

    @BindView(R.id.coordinator_layout_post_text_activity) CoordinatorLayout coordinatorLayout;
    @BindView(R.id.subreddit_icon_gif_image_view_post_text_activity) GifImageView iconGifImageView;
    @BindView(R.id.subreddit_name_text_view_post_text_activity) TextView subreditNameTextView;
    @BindView(R.id.rules_button_post_text_activity) Button rulesButton;
    @BindView(R.id.flair_custom_text_view_post_text_activity) CustomTextView flairTextView;
    @BindView(R.id.spoiler_custom_text_view_post_text_activity) CustomTextView spoilerTextView;
    @BindView(R.id.nsfw_custom_text_view_post_text_activity) CustomTextView nsfwTextView;
    @BindView(R.id.post_title_edit_text_post_text_activity) EditText titleEditText;
    @BindView(R.id.post_text_content_edit_text_post_text_activity) EditText contentEditText;

    private String iconUrl;
    private String subredditName;
    private boolean subredditSelected = false;
    private boolean subredditIsUser;
    private boolean loadSubredditIconSuccessful = true;

    private String flair = null;
    private boolean isSpoiler = false;
    private boolean isNSFW = false;

    private RequestManager mGlide;
    private Locale mLocale;
    private FlairBottomSheetFragment flairSelectionBottomSheetFragment;

    @Inject
    @Named("oauth")
    Retrofit mOauthRetrofit;

    @Inject
    @Named("auth_info")
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_text);

        ButterKnife.bind(this);

        ((Infinity) getApplication()).getmAppComponent().inject(this);

        ActionBar actionBar = getSupportActionBar();
        Drawable upArrow = getResources().getDrawable(R.drawable.ic_arrow_back_white_24dp);
        actionBar.setHomeAsUpIndicator(upArrow);

        mGlide = Glide.with(this);
        mLocale = getResources().getConfiguration().locale;

        if(savedInstanceState != null) {
            subredditName = savedInstanceState.getString(SUBREDDIT_NAME_STATE);
            iconUrl = savedInstanceState.getString(SUBREDDIT_ICON_STATE);
            subredditSelected = savedInstanceState.getBoolean(SUBREDDIT_SELECTED_STATE);
            subredditIsUser = savedInstanceState.getBoolean(SUBREDDIT_IS_USER_STATE);
            loadSubredditIconSuccessful = savedInstanceState.getBoolean(LOAD_SUBREDDIT_ICON_STATE);
            flair = savedInstanceState.getString(FLAIR_STATE);
            isSpoiler = savedInstanceState.getBoolean(IS_SPOILER_STATE);
            isNSFW = savedInstanceState.getBoolean(IS_NSFW_STATE);

            if(subredditName != null) {
                subreditNameTextView.setText(subredditName);
                flairTextView.setVisibility(View.VISIBLE);
                if(!loadSubredditIconSuccessful) {
                    loadSubredditIcon();
                }
            }
            displaySubredditIcon();

            if(flair != null) {
                flairTextView.setText(flair);
                flairTextView.setBackgroundColor(getResources().getColor(R.color.backgroundColorPrimaryDark));
            }
            if(isSpoiler) {
                spoilerTextView.setBackgroundColor(getResources().getColor(R.color.backgroundColorPrimaryDark));
            }
            if(isNSFW) {
                nsfwTextView.setBackgroundColor(getResources().getColor(R.color.colorAccent));
            }
        } else {
            if(getIntent().hasExtra(EXTRA_SUBREDDIT_NAME)) {
                loadSubredditIconSuccessful = false;
                subredditName = getIntent().getExtras().getString(EXTRA_SUBREDDIT_NAME);
                subreditNameTextView.setText(subredditName);
                flairTextView.setVisibility(View.VISIBLE);
                loadSubredditIcon();
            } else {
                mGlide.load(R.drawable.subreddit_default_icon)
                        .apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(72, 0)))
                        .into(iconGifImageView);
            }
        }

        iconGifImageView.setOnClickListener(view -> {
            Intent intent = new Intent(this, SubredditSelectionActivity.class);
            startActivityForResult(intent, SUBREDDIT_SELECTION_REQUEST_CODE);
        });

        subreditNameTextView.setOnClickListener(view -> {
            Intent intent = new Intent(this, SubredditSelectionActivity.class);
            startActivityForResult(intent, SUBREDDIT_SELECTION_REQUEST_CODE);
        });

        rulesButton.setOnClickListener(view -> {
            if(subredditName == null) {
                Snackbar.make(coordinatorLayout, R.string.select_a_subreddit, Snackbar.LENGTH_SHORT).show();
            } else {
                Intent intent = new Intent(this, RulesActivity.class);
                if(subredditIsUser) {
                    intent.putExtra(RulesActivity.EXTRA_SUBREDDIT_NAME, "u_" + subredditName);
                } else {
                    intent.putExtra(RulesActivity.EXTRA_SUBREDDIT_NAME, subredditName);
                }
                startActivity(intent);
            }
        });

        flairTextView.setOnClickListener(view -> {
            if(flair == null) {
                flairSelectionBottomSheetFragment = new FlairBottomSheetFragment();
                Bundle bundle = new Bundle();
                if(subredditIsUser) {
                    bundle.putString(FlairBottomSheetFragment.EXTRA_SUBREDDIT_NAME, "u_" + subredditName);
                } else {
                    bundle.putString(FlairBottomSheetFragment.EXTRA_SUBREDDIT_NAME, subredditName);
                }
                flairSelectionBottomSheetFragment.setArguments(bundle);
                flairSelectionBottomSheetFragment.show(getSupportFragmentManager(), flairSelectionBottomSheetFragment.getTag());
            } else {
                flairTextView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                flairTextView.setText(getString(R.string.flair));
                flair = null;
            }
        });

        spoilerTextView.setOnClickListener(view -> {
            if(!isSpoiler) {
                spoilerTextView.setBackgroundColor(getResources().getColor(R.color.backgroundColorPrimaryDark));
                isSpoiler = true;
            } else {
                spoilerTextView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                isSpoiler = false;
            }
        });

        nsfwTextView.setOnClickListener(view -> {
            if(!isNSFW) {
                nsfwTextView.setBackgroundColor(getResources().getColor(R.color.colorAccent));
                isNSFW = true;
            } else {
                nsfwTextView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                isNSFW = false;
            }
        });
    }

    private void displaySubredditIcon() {
        if(iconUrl != null && !iconUrl.equals("")) {
            mGlide.load(iconUrl)
                    .apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(72, 0)))
                    .error(mGlide.load(R.drawable.subreddit_default_icon)
                            .apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(72, 0))))
                    .into(iconGifImageView);
        } else {
            mGlide.load(R.drawable.subreddit_default_icon)
                    .apply(RequestOptions.bitmapTransform(new RoundedCornersTransformation(72, 0)))
                    .into(iconGifImageView);
        }
    }

    private void loadSubredditIcon() {
        new LoadSubredditIconAsyncTask(SubredditRoomDatabase.getDatabase(this).subredditDao(),
                subredditName, iconImageUrl -> {
            iconUrl = iconImageUrl;
            displaySubredditIcon();
            loadSubredditIconSuccessful = true;
        }).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.post_text_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_send_post_text_activity:
                if(!subredditSelected) {
                    Snackbar.make(coordinatorLayout, R.string.select_a_subreddit, Snackbar.LENGTH_SHORT).show();
                    return true;
                }

                item.setEnabled(false);
                item.getIcon().setAlpha(130);
                Snackbar postingSnackbar = Snackbar.make(coordinatorLayout, R.string.posting, Snackbar.LENGTH_INDEFINITE);
                postingSnackbar.show();

                String subredditName;
                if(subredditIsUser) {
                    subredditName = "u_" + subreditNameTextView.getText().toString();
                } else {
                    subredditName = subreditNameTextView.getText().toString();
                }

                SubmitPost.submitTextOrLinkPost(mOauthRetrofit, sharedPreferences, mLocale, subredditName,
                        titleEditText.getText().toString(), contentEditText.getText().toString(),
                        flair, isSpoiler, isNSFW, RedditUtils.KIND_SELF, new SubmitPost.SubmitPostListener() {
                            @Override
                            public void submitSuccessful(Post post) {
                                Intent intent = new Intent(PostTextActivity.this, ViewPostDetailActivity.class);
                                intent.putExtra(ViewPostDetailActivity.EXTRA_POST_DATA, post);
                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void submitFailed(@Nullable String errorMessage) {
                                postingSnackbar.dismiss();
                                item.setEnabled(true);
                                item.getIcon().setAlpha(255);
                                if(errorMessage == null) {
                                    Snackbar.make(coordinatorLayout, R.string.post_failed, Snackbar.LENGTH_SHORT).show();
                                } else {
                                    Snackbar.make(coordinatorLayout, errorMessage, Snackbar.LENGTH_SHORT).show();
                                }
                            }
                        });
                return true;
        }

        return false;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SUBREDDIT_NAME_STATE, subredditName);
        outState.putString(SUBREDDIT_ICON_STATE, iconUrl);
        outState.putBoolean(SUBREDDIT_SELECTED_STATE, subredditSelected);
        outState.putBoolean(SUBREDDIT_IS_USER_STATE, subredditIsUser);
        outState.putBoolean(LOAD_SUBREDDIT_ICON_STATE, loadSubredditIconSuccessful);
        outState.putString(FLAIR_STATE, flair);
        outState.putBoolean(IS_SPOILER_STATE, isSpoiler);
        outState.putBoolean(IS_NSFW_STATE, isNSFW);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == SUBREDDIT_SELECTION_REQUEST_CODE) {
            if(resultCode == RESULT_OK) {
                subredditName = data.getExtras().getString(SubredditSelectionActivity.EXTRA_RETURN_SUBREDDIT_NAME_KEY);
                iconUrl = data.getExtras().getString(SubredditSelectionActivity.EXTRA_RETURN_SUBREDDIT_ICON_URL_KEY);
                subredditSelected = true;
                subredditIsUser = data.getExtras().getBoolean(SubredditSelectionActivity.EXTRA_RETURN_SUBREDDIT_IS_USER_KEY);

                subreditNameTextView.setTextColor(getResources().getColor(R.color.primaryTextColor));
                subreditNameTextView.setText(subredditName);
                displaySubredditIcon();

                flairTextView.setVisibility(View.VISIBLE);
                flairTextView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                flairTextView.setText(getString(R.string.flair));
                flair = null;
            }
        }
    }

    @Override
    public void flairSelected(String flair) {
        this.flair = flair;
        flairTextView.setText(flair);
        flairTextView.setBackgroundColor(getResources().getColor(R.color.backgroundColorPrimaryDark));
        flairSelectionBottomSheetFragment.dismiss();
    }
}
