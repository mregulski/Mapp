package ppt.reshi.mapp;


import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.maps.android.SphericalUtil;
import com.koushikdutta.ion.Ion;

import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    private final static String TAG = "MapsActivity";
    private GoogleMap mMap;
    private Polyline mLine;
    private List<City> mCities;
    private Map<City, Marker> mMarkers;


    private CitiesAdapter mCitiesAdapter;
    private Spinner mCitySelect;
    private Button mDrawingBtn;
    private TextView mLineLength;

    private ViewGroup mControls;
    private CardView mPlacesCard;

    private boolean mPlaceAddMode;
    private boolean mDrawMode;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "maps key: " + getResources().getText(R.string.google_maps_key));
        mMarkers = new HashMap<>();
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        SupportPlaceAutocompleteFragment placeSearchFragment = (SupportPlaceAutocompleteFragment) getSupportFragmentManager()
                .findFragmentById(R.id.place_autocomplete_fragment);
        placeSearchFragment.setFilter(new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_CITIES)
                .build());
        placeSearchFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Toast.makeText(MapsActivity.this, place.getName(), Toast.LENGTH_SHORT).show();
                addCity(place);
                placeSearchFragment.setText("");
            }

            @Override
            public void onError(Status status) {
                Log.e(TAG, "error searching places: " + status);
            }
        });


        mDrawingBtn = (Button) findViewById(R.id.btn_clearline);
        mDrawingBtn.setOnClickListener(v -> toggleDrawMode());
        mCitySelect = (Spinner) findViewById(R.id.city_select);
        mPlacesCard = (CardView) findViewById(R.id.place_autocomplete_card);
        mPlacesCard.setTranslationY(-300.0f);

        mControls = (ViewGroup) findViewById(R.id.map_controls);
        mLineLength = (TextView) findViewById(R.id.line_length);

        setupCities();
    }

    private void toggleDrawMode() {
        if (mDrawMode) {
            mLine.remove();
            mLine = mMap.addPolyline(new PolylineOptions());
            mLineLength.setVisibility(View.GONE);
            mLineLength.setText("");
            mDrawMode = false;
            mDrawingBtn.setText(R.string.draw_mode_activate);
        } else {
            mDrawMode = true;
            mDrawingBtn.setText(R.string.draw_mode_clear);
            mLineLength.setVisibility(View.VISIBLE);
            for (Map.Entry<City, Marker> entry : mMarkers.entrySet()) {
                entry.getValue().hideInfoWindow();
            }
        }
    }

    private void setupCities() {
        InputStreamReader inStrReader = new InputStreamReader(getResources().openRawResource(R.raw.cities));
        Gson gson = new GsonBuilder().create();
        Type citiesType = new TypeToken<ArrayList<City>>() {
        }.getType();
        mCities = gson.fromJson(new JsonReader(inStrReader), citiesType);
        Log.d(TAG, "loaded cities: " + mCities);
        mCitiesAdapter = new CitiesAdapter(this, R.layout.item_city, mCities);
        mCitiesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCitiesAdapter.setOnCityRemovedListener(city -> {
            Log.d(TAG, "removing marker for: " + city.getName());
            mMarkers.get(city).remove();
            mMarkers.remove(city);
        });
        mCitySelect.setAdapter(mCitiesAdapter);

        // cannot 'reselect'
        mCitySelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onItemSelected: #" + position);
                if (mMap == null) {
                    return;
                }
                if (position == mCitiesAdapter.getCount() - 1) {
                    togglePlaceSelect();
                } else {
                    setMapCameraPosition(mCities.get(position).getPosition());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Log.d(TAG, "onNothingSelected: noooooo");
                // actually... let's ignore this
            }
        });
    }

    private void setMapCameraPosition(LatLng location) {
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.builder()
                .target(location)
                .bearing(0)
                .zoom(6)
                .build()));
    }


    private void addCity(Place place) {
        Marker marker = mMap.addMarker(new MarkerOptions()
                .position(place.getLatLng())
                .title(place.getName().toString())
        );
        addSnippet(marker);
        LatLng pos = place.getLatLng();
        City city = new City(place.getName(), pos.longitude, pos.latitude);
        mMarkers.put(city, marker);
        mCitiesAdapter.add(city);
        setMapCameraPosition(place.getLatLng());
        togglePlaceSelect();
    }

    private void createMarkers() {
        if (mMap == null || mCities == null) {
            return;
        }
        for (City city : mCities) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(city.getPosition())
                    .title(city.getName())
            );
            marker.setTag(city);
            mMarkers.put(city, marker);
            addSnippet(marker);
        }
    }

    private void addSnippet(Marker marker) {
        String url = "";
        try {
            url = "https://pl.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro=&exchars=200&titles=";
            url += URLEncoder.encode(marker.getTitle(), "utf-8");
        } catch (UnsupportedEncodingException enc) {
            Log.e(TAG, "addSnippet: unsupported encoding in url parameter", enc);
        }
        Ion.with(MapsActivity.this)
                .load(url)
                .asJsonObject()
                .setCallback((e, result) -> {
                    JsonObject data = result.getAsJsonObject("query").getAsJsonObject("pages");
                    JsonObject page = null;
                    for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
                        // there is actually only a single entry - but it's name is the page's ID
                        page = entry.getValue().getAsJsonObject();
                    }
                    if (page != null) {
                        marker.setSnippet(page.get("extract").getAsString());
                        String pageId = page.get("pageid").getAsString();
                        marker.setTag(pageId);
                        addWikiUrl(marker);
                    } else {
                        Log.e(TAG, "error parsing json: 'extract' not found in page:" + data);
                    }

                });
    }

    private void addWikiUrl(Marker marker) {
        String url = "https://pl.wikipedia.org/w/api.php?action=query&format=json&prop=info&inprop=url&pageids=";
        if (marker.getTag() == null) {
            return;
        }
        Ion.with(MapsActivity.this)
                .load(url + marker.getTag()) // should be already set to pageid before
                .asJsonObject()
                .setCallback((e, result) -> {
                    JsonObject data = result.getAsJsonObject("query").getAsJsonObject("pages");
                    JsonObject page = null;
                    for (Map.Entry<String, JsonElement> entry : data.entrySet()) {
                        // there is actually only a single entry - but it's name is the page's ID
                        page = entry.getValue().getAsJsonObject();
                    }
                    if (page != null) {
                        marker.setTag(page.get("fullurl").getAsString());
                    }
                });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        createMarkers();
        mLine = mMap.addPolyline(new PolylineOptions());


        mMap.setOnMarkerClickListener(marker -> {
            if (mDrawMode) {
                List<LatLng> oldPoints = mLine.getPoints();
                if (oldPoints.size() > 0 && marker.getPosition().equals(oldPoints.get(oldPoints.size() - 1))) {
                    Log.d(TAG, "Clicked marker is the same as last: leaving current line");
                    return true;
                }
                mLine.remove();
                mLine = mMap.addPolyline(new PolylineOptions()
                        .addAll(oldPoints)
                        .add(marker.getPosition())
                        .geodesic(true));

                for (Map.Entry<City, Marker> m : mMarkers.entrySet()) {
                    m.getValue().setIcon(BitmapDescriptorFactory.defaultMarker());
                }
                marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

                List<LatLng> points = mLine.getPoints();
                double totalDist = 0;
                if (points.size() > 1) {
                    for (int i = 0; i < points.size() - 1; i++) {
                        totalDist += SphericalUtil.computeDistanceBetween(points.get(i), points.get(i + 1));
                    }
                    String formattedLength = NumberFormat.getInstance().format(Math.round(totalDist / 100));
                    mLineLength.setText(getResources().getString(R.string.line_length_tpl, formattedLength));
                }
                return true;
            }
            // not drawing
            Log.d(TAG, "onMapReady: infoWindow for marker: " + marker.getTitle()
                    + ": " + (marker.isInfoWindowShown() ? "visible" : "invisible"));

            setMapCameraPosition(marker.getPosition());
            if (marker.getSnippet() != null) {
                marker.showInfoWindow();
            }

            return true;
        });


        mMap.setOnMapClickListener(latLng -> {
            if (mPlaceAddMode) {
                togglePlaceSelect();
            }
        });


        mMap.setOnInfoWindowClickListener(marker -> {
            Intent wikiIntent = new Intent(Intent.ACTION_VIEW, Uri.parse((String) marker.getTag()));
            startActivity(wikiIntent);
        });


        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {
                View v = LayoutInflater.from(MapsActivity.this).inflate(R.layout.city_info, null);
                ((TextView) v.findViewById(R.id.city_info_snippet)).setText(Html.fromHtml(marker.getSnippet()));
                return v;
            }
        });

    }

    private void togglePlaceSelect() {
        if (mPlaceAddMode) {
            mPlacesCard.animate().translationY(-300.0f);
            mControls.animate().translationY(0);
            mPlaceAddMode = false;
        } else {
            mPlacesCard.setVisibility(View.VISIBLE);
            mPlacesCard.animate().translationY(0);
            mControls.animate().translationY(300.0f);
            mPlaceAddMode = true;
        }
    }
}
