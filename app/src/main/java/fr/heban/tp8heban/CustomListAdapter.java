package fr.heban.tp8heban;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.Locale;

public class CustomListAdapter extends ArrayAdapter {

    private Activity context;
    private Uri[] uris;
    private double[] distances;


    public CustomListAdapter(Activity context, Uri[] uris, double[] distances) {
        super(context, R.layout.row_list_view_item, uris);
        this.uris = uris;
        this.distances = distances;
        this.context = context;

    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View row = convertView;
        LayoutInflater inflater = context.getLayoutInflater();
        if(convertView == null) {
            row = inflater.inflate(R.layout.row_list_view_item, null,true);
        }
        NumberFormat numberFormat = NumberFormat.getInstance(Locale.FRANCE);
        numberFormat.setMaximumFractionDigits(2);
        TextView distance = row.findViewById(R.id.textViewDistance);
        ImageView imageView = row.findViewById(R.id.imageThumbnail);

        distance.setText(numberFormat.format(this.distances[position]) + " Km");
        imageView.setImageURI(this.uris[position]);

        return row;
    }
}