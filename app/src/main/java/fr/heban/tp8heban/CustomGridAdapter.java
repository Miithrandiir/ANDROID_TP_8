package fr.heban.tp8heban;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CustomGridAdapter extends ArrayAdapter {

    Activity context;
    Uri[] uris;

    public CustomGridAdapter(@NonNull Activity context, Uri[] uris) {
        super(context, R.layout.grid_list_view,uris);
        this.context = context;
        this.uris=uris;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View row = convertView;
        LayoutInflater inflater = context.getLayoutInflater();
        if(convertView == null) {
            row = inflater.inflate(R.layout.grid_list_view, null,true);
        }
        //On ajoute des les uris à la view
        ImageView iv = row.findViewById(R.id.smallImage);
        iv.setImageURI(uris[position]);

        return row;
    }
}
