package ppt.reshi.mapp;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Marcin Regulski on 27.05.2017.
 */

public class CitiesAdapter extends ArrayAdapter<City> {
    private final static String TAG = "CitiesAdapter";
    @LayoutRes
    private int mLayout;

    private OnCityRemovedListener mCityRemovedListener;


    public CitiesAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<City> objects) {
        super(context, android.R.layout.simple_spinner_item, objects);
        mLayout = resource;
    }

    public void setOnCityRemovedListener(OnCityRemovedListener listener) {
        mCityRemovedListener = listener;
    }

    @Override
    public int getCount() {
        return super.getCount() + 1;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View v = super.getView(0, convertView, parent);
        if (position == getCount() - 1) {
            ((TextView) v).setText(R.string.add_city);
        } else {
            ((TextView) v).setText(getItem(position).getName());
        }
        return v;

    }

    @NonNull
    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(mLayout, parent, false);
            holder = new ViewHolder();
            holder.cityName = (TextView) convertView.findViewById(R.id.city_name);
            holder.deleteBtn = (ImageButton) convertView.findViewById(R.id.city_remove);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (position == getCount() - 1) {
            Log.d(TAG, "city #" + position + ": <add new>");
            holder.cityName.setText(R.string.add_city);
            holder.deleteBtn.setVisibility(View.GONE);
        } else {
            Log.d(TAG, "city #" + position + ": " + getItem(position).getName());
            holder.cityName.setText(getItem(position).getName());
            holder.deleteBtn.setVisibility(View.VISIBLE);
            holder.deleteBtn.setOnClickListener(v -> {
                City deleted = getItem(position);
                Log.d(TAG, "removing city #" + position + ": " + deleted.getName());
                remove(deleted);
                performCityRemoved(deleted);
            });
        }
        return convertView;
    }

    private void performCityRemoved(City city) {
        if (mCityRemovedListener == null) {
            return;
        }
        mCityRemovedListener.onCityRemoved(city);
    }

    private class ViewHolder {
        TextView cityName;
        ImageButton deleteBtn;
    }

    public interface OnCityRemovedListener {
        void onCityRemoved(City city);
    }
}
