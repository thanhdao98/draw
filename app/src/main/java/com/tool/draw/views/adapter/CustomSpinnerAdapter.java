package com.tool.draw.views.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.tool.draw.R;

public class CustomSpinnerAdapter extends ArrayAdapter<String> {
    private int selectedPosition = -1;

    public CustomSpinnerAdapter(Context context, String[] items) {
        super(context, R.layout.spinner_item, items);
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, parent, true); // Dropdown view
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, parent, false);
    }

    @SuppressLint("SetTextI18n")
    private View getCustomView(int position, ViewGroup parent, boolean isDropdown) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View row = inflater.inflate(R.layout.spinner_item, parent, false);
        TextView textView = row.findViewById(R.id.textViewItem);
        View divider = row.findViewById(R.id.divider);

        if (isDropdown) {
            textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            textView.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
            if (position == selectedPosition) {
                textView.setText("✓︎   " + getItem(position));
            } else {
                textView.setText(getItem(position));
            }

            if (position == getCount() - 1) {
                divider.setVisibility(View.GONE);
            } else {
                divider.setVisibility(View.VISIBLE);
            }
        } else {
            textView.setText(getItem(position));
            textView.setGravity(Gravity.CENTER);
            divider.setVisibility(View.GONE);
        }

        return row;
    }
}
