package com.mubin.tvshows.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.mubin.tvshows.R;
import com.mubin.tvshows.adapters.EpisodesAdapter;
import com.mubin.tvshows.adapters.ImageSliderAdapter;
import com.mubin.tvshows.databinding.ActivityTvShowDetailsBinding;
import com.mubin.tvshows.databinding.LayoutEpisodesBottomSheetBinding;
import com.mubin.tvshows.models.TVShow;
import com.mubin.tvshows.viewmodels.TvShowDetailsViewModel;

import java.util.Locale;

import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class TvShowDetailsActivity extends AppCompatActivity {

    private ActivityTvShowDetailsBinding activityTvShowDetailsBinding;
    private TvShowDetailsViewModel tvShowDetailsViewModel;
    private BottomSheetDialog episodesBottomSheetDialog;
    private LayoutEpisodesBottomSheetBinding layoutEpisodesBottomSheetBinding;
    private TVShow tvShow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityTvShowDetailsBinding = DataBindingUtil.setContentView(this, R.layout.activity_tv_show_details);
        doInitialization();
    }

    private void doInitialization() {
        tvShowDetailsViewModel = new ViewModelProvider(this).get(TvShowDetailsViewModel.class);
        activityTvShowDetailsBinding.imageBack.setOnClickListener(v -> onBackPressed());
        tvShow = (TVShow) getIntent().getSerializableExtra("tvShow");
        getTvShowDetails();
    }

    private void getTvShowDetails() {
        activityTvShowDetailsBinding.setIsLoading(true);
        String tvShowId = String.valueOf(tvShow.getId());
        tvShowDetailsViewModel.getTvShowDetails(tvShowId).observe(
                this, tvShowDetailsResponse -> {
                    activityTvShowDetailsBinding.setIsLoading(false);
                    if (tvShowDetailsResponse.getTvShowDetails() != null) {
                        if (tvShowDetailsResponse.getTvShowDetails().getPictures() != null) {
                            loadImageSlider(tvShowDetailsResponse.getTvShowDetails().getPictures());
                        }
                        activityTvShowDetailsBinding.setTvShowImageURL(
                                tvShowDetailsResponse.getTvShowDetails().getImagePath()
                        );
                        activityTvShowDetailsBinding.imageTvShow.setVisibility(View.VISIBLE);
                        activityTvShowDetailsBinding.setDescription(
                                String.valueOf(
                                        HtmlCompat.fromHtml(
                                                tvShowDetailsResponse.getTvShowDetails().getDescription(),
                                                HtmlCompat.FROM_HTML_MODE_LEGACY
                                        )
                                )
                        );
                        activityTvShowDetailsBinding.textDescription.setVisibility(View.VISIBLE);
                        activityTvShowDetailsBinding.textReadMore.setVisibility(View.VISIBLE);
                        activityTvShowDetailsBinding.textReadMore.setOnClickListener(v -> {
                            if (activityTvShowDetailsBinding.textReadMore.getText().toString().equals("Read More")) {
                                activityTvShowDetailsBinding.textDescription.setMaxLines(Integer.MAX_VALUE);
                                activityTvShowDetailsBinding.textDescription.setEllipsize(null);
                                activityTvShowDetailsBinding.textReadMore.setText(R.string.read_less);
                            } else {
                                activityTvShowDetailsBinding.textDescription.setMaxLines(4);
                                activityTvShowDetailsBinding.textDescription.setEllipsize(TextUtils.TruncateAt.END);
                                activityTvShowDetailsBinding.textReadMore.setText(R.string.read_more);
                            }
                        });
                        activityTvShowDetailsBinding.setRating(
                                String.format(
                                        Locale.getDefault(),
                                        "%.2f",
                                        Double.parseDouble(tvShowDetailsResponse.getTvShowDetails().getRating())
                                )
                        );
                        if (tvShowDetailsResponse.getTvShowDetails().getGenres() != null) {
                            activityTvShowDetailsBinding.setGenre(tvShowDetailsResponse.getTvShowDetails().getGenres()[0]);
                        } else {
                            activityTvShowDetailsBinding.setGenre("N/A");
                        }
                        activityTvShowDetailsBinding.setRuntime(tvShowDetailsResponse.getTvShowDetails().getRuntime() + " Min");
                        activityTvShowDetailsBinding.viewDivider1.setVisibility(View.VISIBLE);
                        activityTvShowDetailsBinding.layoutMisc.setVisibility(View.VISIBLE);
                        activityTvShowDetailsBinding.viewDivider2.setVisibility(View.VISIBLE);
                        activityTvShowDetailsBinding.buttonWebsite.setOnClickListener(v -> {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(tvShowDetailsResponse.getTvShowDetails().getUrl()));
                            startActivity(intent);
                        });
                        activityTvShowDetailsBinding.buttonWebsite.setVisibility(View.VISIBLE);
                        activityTvShowDetailsBinding.buttonEpisodes.setVisibility(View.VISIBLE);
                        activityTvShowDetailsBinding.buttonEpisodes.setOnClickListener(v -> {
                            if (episodesBottomSheetDialog == null) {
                                episodesBottomSheetDialog = new BottomSheetDialog(TvShowDetailsActivity.this);
                                layoutEpisodesBottomSheetBinding = DataBindingUtil.inflate(
                                        LayoutInflater.from(TvShowDetailsActivity.this),
                                        R.layout.layout_episodes_bottom_sheet,
                                        findViewById(R.id.episodeContainer),
                                        false
                                );
                                episodesBottomSheetDialog.setContentView(layoutEpisodesBottomSheetBinding.getRoot());
                                layoutEpisodesBottomSheetBinding.episodesRecyclerView.setAdapter(
                                        new EpisodesAdapter(tvShowDetailsResponse.getTvShowDetails().getEpisodes())
                                );
                                layoutEpisodesBottomSheetBinding.textTitle.setText(
                                        String.format("Episodes | %s", tvShow.getName())
                                );
                                layoutEpisodesBottomSheetBinding.imageClose.setOnClickListener(v1 -> episodesBottomSheetDialog.dismiss());
                            }

                            //Optional
                            FrameLayout frameLayout = episodesBottomSheetDialog.findViewById(
                                    com.google.android.material.R.id.design_bottom_sheet
                            );
                            if (frameLayout != null) {
                                BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(frameLayout);
                                bottomSheetBehavior.setPeekHeight(Resources.getSystem().getDisplayMetrics().heightPixels);
                                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                            }
                            //Optional End

                            episodesBottomSheetDialog.show();

                        });

                        activityTvShowDetailsBinding.imageWatchlist.setOnClickListener(v ->
                                new CompositeDisposable().add(tvShowDetailsViewModel.addToWatchlist(tvShow)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(() -> {
                                            activityTvShowDetailsBinding.imageWatchlist.setImageResource(R.drawable.ic_check);
                                            Toast.makeText(getApplicationContext(), "Added to Watchlist", Toast.LENGTH_SHORT).show();
                                        })
                                ));
                        activityTvShowDetailsBinding.imageWatchlist.setVisibility(View.VISIBLE);
                        loadBasicTvShowDetails();
                    }
                }
        );
    }

    private void loadImageSlider(String[] sliderImages) {
        activityTvShowDetailsBinding.sliderViewPager.setOffscreenPageLimit(1);
        activityTvShowDetailsBinding.sliderViewPager.setAdapter(new ImageSliderAdapter(sliderImages));
        activityTvShowDetailsBinding.sliderViewPager.setVisibility(View.VISIBLE);
        activityTvShowDetailsBinding.viewFadingEdge.setVisibility(View.VISIBLE);
        setupSliderIndicators(sliderImages.length);
        activityTvShowDetailsBinding.sliderViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentSliderIndicator(position);
            }
        });

    }

    private void setupSliderIndicators(int count) {
        ImageView[] indicators = new ImageView[count];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(8, 0, 8, 0);
        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(getApplicationContext());
            indicators[i].setImageDrawable(ContextCompat.getDrawable(
                    getApplicationContext(),
                    R.drawable.background_slider_indicator_inactive
            ));
            indicators[i].setLayoutParams(layoutParams);
            activityTvShowDetailsBinding.layoutSliderIndicator.addView(indicators[i]);
        }
        activityTvShowDetailsBinding.layoutSliderIndicator.setVisibility(View.VISIBLE);
        setCurrentSliderIndicator(0);
    }

    private void setCurrentSliderIndicator(int position) {
        int childCount = activityTvShowDetailsBinding.layoutSliderIndicator.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) activityTvShowDetailsBinding.layoutSliderIndicator.getChildAt(i);
            if (i == position) {
                imageView.setImageDrawable(
                        ContextCompat.getDrawable(getApplicationContext(), R.drawable.background_slider_indicator_active)
                );
            } else {
                imageView.setImageDrawable(
                        ContextCompat.getDrawable(getApplicationContext(), R.drawable.background_slider_indicator_inactive)
                );
            }
        }
    }

    private void loadBasicTvShowDetails() {
        activityTvShowDetailsBinding.setTvShowName(tvShow.getName());
        activityTvShowDetailsBinding.setNetworkCountry(
                tvShow.getNetwork() + " (" +
                        tvShow.getCountry() + ")"
        );
        activityTvShowDetailsBinding.setStatus(tvShow.getStatus());
        activityTvShowDetailsBinding.setStartedDate(tvShow.getStartDate());
        activityTvShowDetailsBinding.textName.setVisibility(View.VISIBLE);
        activityTvShowDetailsBinding.textNetworkCountry.setVisibility(View.VISIBLE);
        activityTvShowDetailsBinding.textStatus.setVisibility(View.VISIBLE);
        activityTvShowDetailsBinding.textStarted.setVisibility(View.VISIBLE);
    }

}