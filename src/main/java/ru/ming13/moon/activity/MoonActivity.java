package ru.ming13.moon.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.fitness.Fitness;
import com.squareup.otto.Subscribe;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import icepick.Icepick;
import icepick.Icicle;
import ru.ming13.moon.R;
import ru.ming13.moon.bus.ActivityDistancesLoadedEvent;
import ru.ming13.moon.bus.BusProvider;
import ru.ming13.moon.model.FitnessActivityDistances;
import ru.ming13.moon.storage.FitnessActivityDistancesStorage;
import ru.ming13.moon.model.FitnessActivity;
import ru.ming13.moon.model.FitnessActivityDistance;
import ru.ming13.moon.util.Animations;
import ru.ming13.moon.util.DistanceCalculator;
import ru.ming13.moon.util.Formatter;
import ru.ming13.moon.util.GoogleServices;
import ru.ming13.moon.util.Intents;

public class MoonActivity extends ActionBarActivity implements
	GoogleApiClient.ConnectionCallbacks,
	GoogleApiClient.OnConnectionFailedListener
{
	static final class FitnessActivityViewHolder
	{
		@InjectView(R.id.image_activity)
		ImageView activityIcon;

		@InjectView(R.id.text_activity_title)
		TextView activityTitle;

		@InjectView(R.id.text_activity_description)
		TextView activityDescription;

		public FitnessActivityViewHolder(View activityView) {
			ButterKnife.inject(this, activityView);
		}
	}

	@InjectView(R.id.toolbar)
	Toolbar toolbar;

	@InjectView(R.id.progress)
	ProgressBar progressBar;

	@InjectView(R.id.button_share)
	ImageButton shareButton;

	@InjectView(R.id.layout_stats)
	ViewGroup statsLayout;

	@InjectView(R.id.layout_connection)
	ViewGroup connectionLayout;

	private FitnessActivityViewHolder walkingActivityViewHolder;
	private FitnessActivityViewHolder runningActivityViewHolder;
	private FitnessActivityViewHolder bikingActivityViewHolder;

	private GoogleApiClient googleApiClient;

	private boolean isGoogleApiRepeatingConnection;

	@Icicle
	FitnessActivityDistances fitnessActivityDistances;

	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_moon);

		setUpInjections();

		setUpState(state);

		setUpToolbar();

		setUpActivityStats();
	}

	private void setUpInjections() {
		ButterKnife.inject(this);

		walkingActivityViewHolder = new FitnessActivityViewHolder(findViewById(R.id.layout_activity_walk));
		runningActivityViewHolder = new FitnessActivityViewHolder(findViewById(R.id.layout_activity_run));
		bikingActivityViewHolder = new FitnessActivityViewHolder(findViewById(R.id.layout_activity_bike));
	}

	private void setUpState(Bundle state) {
		Icepick.restoreInstanceState(this, state);
	}

	private void setUpToolbar() {
		setSupportActionBar(toolbar);

		getSupportActionBar().setDisplayShowTitleEnabled(false);
	}

	private void setUpActivityStats() {
		if (fitnessActivityDistances == null) {
			setUpGoogleApiClient();
			setUpGoogleApiConnection();
		} else {
			setUpFitnessActivities();
		}
	}

	private void setUpGoogleApiClient() {
		this.googleApiClient = new GoogleApiClient.Builder(this)
			.addApi(Fitness.API)
			.addScope(Fitness.SCOPE_ACTIVITY_READ)
			.addScope(Fitness.SCOPE_LOCATION_READ)
			.addConnectionCallbacks(this)
			.addOnConnectionFailedListener(this)
			.build();
	}

	private void setUpGoogleApiConnection() {
		googleApiClient.connect();
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		FitnessActivityDistancesStorage.with(googleApiClient).readActivityDistances();
	}

	@Subscribe
	public void onActivityDistancesLoaded(ActivityDistancesLoadedEvent event) {
		this.fitnessActivityDistances = event.getActivityDistances();

		setUpFitnessActivities();
	}

	private void setUpFitnessActivities() {
		setUpFitnessActivity(walkingActivityViewHolder, fitnessActivityDistances.getWalkingDistance());
		setUpFitnessActivity(runningActivityViewHolder, fitnessActivityDistances.getRunningDistance());
		setUpFitnessActivity(bikingActivityViewHolder, fitnessActivityDistances.getBikingDistance());

		Animations.exchange(progressBar, statsLayout);
		Animations.scaleUp(shareButton);
	}

	private void setUpFitnessActivity(FitnessActivityViewHolder activityViewHolder, FitnessActivityDistance activityDistance) {
		activityViewHolder.activityIcon.setImageResource(
			getFitnessActivityIcon(activityDistance.getActivity()));

		activityViewHolder.activityTitle.setText(
			getString(R.string.mask_activity_title, Formatter.formatDistance(activityDistance.getDistance())));

		activityViewHolder.activityDescription.setText(
			getString(R.string.mask_activity_description, Formatter.formatPercent(DistanceCalculator.calculateMoonDistancePercentage(activityDistance.getDistance()))));
	}

	@DrawableRes
	private int getFitnessActivityIcon(FitnessActivity activity) {
		switch (activity) {
			case WALKING:
				return R.drawable.ic_description_walk;

			case RUNNING:
				return R.drawable.ic_description_run;

			case BIKING:
				return R.drawable.ic_description_bike;

			default:
				return android.R.color.transparent;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode != RESULT_OK) {
			tearDownGoogleApiConnection();
			return;
		}

		if (requestCode == Intents.Requests.GOOGLE_CONNECTION) {
			setUpGoogleApiConnection();
		}
	}

	private void tearDownGoogleApiConnection() {
		if (isGoogleApiClientConnected()) {
			googleApiClient.disconnect();
		}
	}

	private boolean isGoogleApiClientConnected() {
		return (googleApiClient != null) && (googleApiClient.isConnecting() || googleApiClient.isConnected());
	}

	@Override
	public void onConnectionSuspended(int cause) {
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		if (!isGoogleApiRepeatingConnection) {
			isGoogleApiRepeatingConnection = true;

			Animations.exchange(progressBar, connectionLayout);

			return;
		}

		if (connectionResult.hasResolution()) {
			GoogleServices.with(this).showResolutionAction(connectionResult);
		} else {
			GoogleServices.with(this).showResolutionError(connectionResult);

			Animations.exchange(progressBar, connectionLayout);
		}
	}

	@OnClick(R.id.button_connect)
	public void startGoogleConnection() {
		Animations.exchange(connectionLayout, progressBar);

		googleApiClient.connect();
	}

	@OnClick(R.id.button_share)
	public void startStatsSharing() {
		Intent intent = Intents.Builder.with(this).buildShareIntent("Moon!");
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.action_moon, menu);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		switch (menuItem.getItemId()) {
			case R.id.menu_rate_application:
				startApplicationRating();
				return true;

			case R.id.menu_send_feedback:
				startFeedbackSending();
				return true;

			default:
				return super.onOptionsItemSelected(menuItem);
		}
	}

	private void startApplicationRating() {
		try {
			Intent intent = Intents.Builder.with(this).buildGooglePlayAppIntent();
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			Intent intent = Intents.Builder.with(this).buildGooglePlayWebIntent();
			startActivity(intent);
		}
	}

	private void startFeedbackSending() {
		Intent intent = Intents.Builder.with(this).buildFeedbackIntent();

		try {
			startActivity(intent);
		} catch (ActivityNotFoundException e) {
			startActivity(Intent.createChooser(intent, null));
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		BusProvider.getBus().register(this);
	}

	@Override
	protected void onPause() {
		super.onPause();

		BusProvider.getBus().unregister(this);
	}

	@Override
	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);

		tearDownState(state);
	}

	private void tearDownState(Bundle state) {
		Icepick.saveInstanceState(this, state);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		tearDownGoogleApiConnection();
	}
}
