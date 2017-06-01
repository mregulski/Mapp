package ppt.reshi.mapp;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.annotations.SerializedName;

import java.util.Objects;

/**
 * Created by Marcin Regulski on 26.05.2017.
 */

public class City {
    private String name;
    @SerializedName("lon")
    private double longitude;
    @SerializedName("lat")
    private double latitude;

    public City(CharSequence name, double longitude, double latitude) {
        this.name = name.toString();
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getName() {
        return this.name;
    }

    public LatLng getPosition() {
        return new LatLng(latitude, longitude);
    }


    @Override
    public String toString() {
        return "City{" +
                "name='" + name + '\'' +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        City city = (City) o;
        return Double.compare(city.longitude, longitude) == 0 &&
                Double.compare(city.latitude, latitude) == 0 &&
                Objects.equals(name, city.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, longitude, latitude);
    }
}
