package com.thegalos.quarantinecircle.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.maps.android.SphericalUtil;
import com.thegalos.quarantinecircle.R;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static androidx.core.content.ContextCompat.checkSelfPermission;

public class Main extends Fragment implements
        OnMapReadyCallback,
        LocationListener,
        GoogleMap.OnMapClickListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMyLocationButtonClickListener {

    private final int LOCATION_PERMISSION_REQUEST = 999;
    final int REQUEST_CHECK_SETTINGS = 123;
    GoogleMap gMap;
    SupportMapFragment mapFragment;
    int radius;
    LatLng latLng;
    LatLng mapClickLatLng;
    LatLng userLatLng;
    SharedPreferences sp;
    AutocompleteSupportFragment autocompleteSupportFragment;
    LocationCallback locationCallback;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationClient;
    Marker homeMarker, mapClickMarker;
    Circle circle;
    Polyline polyline;

    public Main() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        init();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
                galosLocation();
            } else {
//                XXX
//                shouldShowRequestPermissionRationale()
            }
        }
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        showUserMarker();
    }

    @SuppressLint("PotentialBehaviorOverride")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
//        if user user night mode change map colors
        if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
            googleMap.setMapStyle(new MapStyleOptions(getResources().getString(R.string.style_json)));
        }
        gMap.setOnInfoWindowClickListener(this);
        gMap.setOnMapClickListener(this);
        gMap.setOnMyLocationButtonClickListener(this);

        addHomeMarker();
        addRadius();
        setZoom();
        enableMyLocation();
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Address address = latLngToAddress(latLng);
        String str = "";
        if (address != null)
            str = address.getSubThoroughfare() + " " + address.getThoroughfare() + ", " + address.getLocality();
        Log.d(" map_temp", str);
        if (mapClickMarker != null)
            mapClickMarker.remove();
        mapClickMarker = gMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(getString(R.string.set_new_home))
                .snippet(getString(R.string.click_marker_to_set))
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
        mapClickMarker.showInfoWindow();
        EditText etPlace = autocompleteSupportFragment.getView().findViewById(R.id.places_autocomplete_search_input);
        autocompleteSupportFragment.setText("");
        autocompleteSupportFragment.setHint(str);
        if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)
            etPlace.setHintTextColor(Color.WHITE);
        mapClickLatLng = latLng;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        latLng = mapClickLatLng;
        sp.edit().putString("Latitude", String.valueOf(latLng.latitude))
                .putString("Longitude", String.valueOf(latLng.longitude)).apply();
        if (mapClickMarker != null)
            mapClickMarker.remove();
//        XXX
        addRadius();
        addHomeMarker();
        showUserMarker();
        autocompleteSupportFragment.setText("");
        autocompleteSupportFragment.setHint(getString(R.string.search));
    }

    private void init() {
        /////////////////////////////////////////////////////


        /////////////////////////////////////////////////////
        sp = PreferenceManager.getDefaultSharedPreferences(requireContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        radius = sp.getInt("Radius", 500);
        latLng = new LatLng(Double.parseDouble(sp.getString("Latitude", "32.07424050507271")),
                Double.parseDouble(sp.getString("Longitude", "34.79215988825444")));
        mapClickLatLng = latLng;
        setRadiusText();

        //initialize map and go to onMapReady function
        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // initialize autocomplete
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyBKt1doHJIpMH3r3NtuyFsE-LE0xTGQkGI");
        }
        autocompleteSupportFragment = (AutocompleteSupportFragment) getChildFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        if (autocompleteSupportFragment != null) {
            EditText etPlace = autocompleteSupportFragment.requireView().findViewById(R.id.places_autocomplete_search_input);
            etPlace.setHint(R.string.search);
            if ((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                etPlace.setHintTextColor(android.R.attr.textColorPrimary);
                etPlace.setTextColor(android.R.attr.textColorPrimary);
            }
            autocompleteSupportFragment.setPlaceFields(Arrays.asList(Place.Field.LAT_LNG, Place.Field.NAME));

            autocompleteSupportFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                @Override
                public void onPlaceSelected(@NonNull Place place) {
                    Log.d("Place", "Place: " + place.getName() + ", " + place.getId() + ",lat: " + place.getLatLng());
                    latLng = place.getLatLng();
                    sp.edit().putString("Latitude", String.valueOf(latLng.latitude))
                            .putString("Longitude", String.valueOf(latLng.longitude)).apply();
                    addRadius();
                    addHomeMarker();
                    showUserMarker();
                }

                @Override
                public void onError(@NonNull Status status) {
                    Log.d("Place", "error: " + status);
                }
            });
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null)
                    return;
                for (Location location : locationResult.getLocations()) {
                    userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    showUserMarker();
                    fusedLocationClient.removeLocationUpdates(locationCallback);
                }
            }
        };

        // On startup if FINE_LOCATION granted show user location and polyline.
        if (checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            galosLocation();


//        Button Listeners
        requireView().findViewById(R.id.ivInfo).setOnClickListener(v -> showAboutDialog());
        requireView().findViewById(R.id.ivGPS).setOnClickListener(v -> checkedPermission());

        requireView().findViewById(R.id.ivAdd).setOnClickListener(v -> {
            radius += 100;
            sp.edit().putInt("Radius", radius).apply();
            setRadiusText();
            addRadius();
            showUserMarker();
        });

        requireView().findViewById(R.id.ivSub).setOnClickListener(v -> {
            if (radius == 100)
                return;
            radius -= 100;
            sp.edit().putInt("Radius", radius).apply();
            setRadiusText();
            addRadius();
            showUserMarker();
        });
    }

    private void showUserMarker() {
        if (checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            float[] results = new float[10];
            if (latLng == null || userLatLng == null) {
                return;
            }
            double heading =  SphericalUtil.computeHeading(latLng, userLatLng);
            LatLng radiusLatLng = SphericalUtil.computeOffset(latLng, radius-0.5, heading);

            Location.distanceBetween(latLng.latitude, latLng.longitude, userLatLng.latitude, userLatLng.longitude, results);

            if (polyline != null)
                polyline.remove();
            String str;
            if (radius > results[0]) {
                str = getString(R.string.you_are_within_the_quarantine) + "\n" + getString(R.string.good_job);
                circle.setStrokeColor(Color.GREEN);
            } else {
                Location.distanceBetween(radiusLatLng.latitude, radiusLatLng.longitude, userLatLng.latitude, userLatLng.longitude, results);

                str = (results[0]>=1000 ?
                        String.format(Locale.ENGLISH, "%.1f", results[0]/1000)
                                + " " + getString(R.string.km)
                        : (int) results[0] + " " + getString(R.string.meters))+ " " + getString(R.string.away_from_quarantine_circle)
                            + "\n" + getString(R.string.please_go_back);

                circle.setStrokeColor(Color.RED);
                polyline = gMap.addPolyline(new PolylineOptions().add(radiusLatLng, userLatLng)
                        .color(Color.RED)
                        .visible(true));
            }
            TextView tvUserInfo = requireView().findViewById(R.id.tvUserInfo);
            tvUserInfo.setVisibility(View.VISIBLE);
            tvUserInfo.setText(str);
        }
    }

    private void setZoom() {
        if (radius <= 200)
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.2f));
        else if (radius <= 400)
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.8f));
        else if (radius <= 700)
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.2f));
        else
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14.5f));

    }

    /**
     * Check if user granted FINE_LOCATION permission
     * True => turn on MyLocationEnabled (Google Maps Blue dot)
     * False => requesting FINE_LOCATION permission
     */
    private void checkedPermission() {
        if (checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            gMap.setMyLocationEnabled(true);
        else
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
    }


    private void addRadius(){
        if (circle != null)
            circle.remove();
        circle = gMap.addCircle(new CircleOptions().center(latLng)
                .radius(radius)
                .fillColor(Color.TRANSPARENT)
                .strokeColor(Color.RED).strokeWidth(8).visible(true));
    }

    private void addHomeMarker(){
        if (homeMarker != null)
            homeMarker.remove();
        Address address = latLngToAddress(latLng);
        String str = "";
        if (address != null)
            str = address.getSubThoroughfare() + " " + address.getThoroughfare() + ", " + address.getLocality();
        homeMarker = gMap.addMarker(new MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.home_marker_60))
                .title(str));
        gMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    /**
     * Request location, callback with FusedLocation
     */
    public void galosLocation() {
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
        SettingsClient client = LocationServices.getSettingsClient(requireActivity());
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(requireActivity(), locationSettingsResponse -> {
            // All location settings are satisfied. The client can initialize
            // location requests here.
            if (getContext()==null)
                return;
            if (checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
                return;
            }
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        });

        task.addOnFailureListener(requireActivity(), e -> {
            if (e instanceof ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    ResolvableApiException resolvable = (ResolvableApiException) e;
                    resolvable.startResolutionForResult(requireActivity(),
                            REQUEST_CHECK_SETTINGS);
                } catch (IntentSender.SendIntentException sendEx) {
                    // Ignore the error.
                }
            }
        });
    }

    /**
     * Only relevant if user give fine location permission
     * When user click on MyLocationButton (top right corner)
     * Return false so that we don't consume the event and the default behavior still occurs
     * (the camera animates to the user's current position).
     */
    @Override
    public boolean onMyLocationButtonClick() {
        galosLocation();
        return false;
    }

    /**
     *  Enable GoogleMap MyLocation
     */
    private void enableMyLocation() {
        if (checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            requireView().findViewById(R.id.ivGPS).setVisibility(View.GONE);
            gMap.setMyLocationEnabled(true);
        }
    }

    /**
     *  Set Bottom editText to look better
     */
    private void setRadiusText() {
        String str;
        double a;
        if (radius >= 1000) {
            a = radius / 1000d;
            str = ((float) radius / 1000) + " " + getString(R.string.km);
            Log.d("testing radius", "a double is: " + a + "str is: " + str);
        } else
            str = radius + " " + getString(R.string.m);
        TextView tvDistance = requireView().findViewById(R.id.tvDistance);
        tvDistance.setText(str);
    }

    /**
     * Open about fragment
     */
    public void showAboutDialog() {
        final Dialog dialog = new Dialog(requireContext(), R.style.Theme_Dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.popup_about);
        dialog.findViewById(R.id.ivLinkedGal).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.linkedin.com/in/gal-reshef-s-computer-science-software-developer/"))));

        dialog.findViewById(R.id.ivGithub).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/galsreshef"))));

        dialog.findViewById(R.id.constraintLayoutPlayStore).setOnClickListener(v ->
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/developer?id=Gal+Reshef"))));

        dialog.findViewById(R.id.ivClose).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    /**
     * Convert LatLng object oto address object using GeoCoder
     * @param latLng - the object to convert
     * @return An Address Object
     */
    private Address latLngToAddress(LatLng latLng) {
        Geocoder geocoder = new Geocoder(getActivity().getApplicationContext(), Locale.getDefault());
        List<Address> addresses;
        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 5);
            if (addresses != null)
                return addresses.get(0);
            else
                return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}