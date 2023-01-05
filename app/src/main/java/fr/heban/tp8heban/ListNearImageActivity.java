package fr.heban.tp8heban;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Pair;
import android.widget.ListView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ListNearImageActivity extends AppCompatActivity {

    List<Pair<Uri, Double>> distances = new ArrayList<>();

    float lat;
    float lon;
    Uri ref_image;
    int max_range;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_near_image);

        Intent intent = getIntent();
        if (intent == null) {
            setResult(RESULT_CANCELED);
            this.finish();
            return;
        }
        Uri ref_image = intent.getParcelableExtra(MainActivity.INTENT_SEND_REF_IMAGE);
        lat = intent.getFloatExtra(MainActivity.INTENT_SEND_LAT, 0);
        lon = intent.getFloatExtra(MainActivity.INTENT_SEND_LON, 0);
        max_range = intent.getIntExtra(MainActivity.INTENT_SEND_MAX, -1);
        this.ref_image = ref_image;
        //Check permission
        this.checkPermissions();

    }

    /**
     * Calcule la distance orthodromique entre deux points GPS en utilisant la formule de haversine.
     *
     * @param lat2 Latitude du point en degrés.
     * @param lon2 Longitude du point en degrés.
     * @return La distance en kilomètres entre les deux points.
     */
    public double distanceOrthodromique(double lat2, double lon2) {
        double lat1 = lat;
        double lon1 = lon;
        // Rayon de la Terre en kilomètres
        double EARTH_RADIUS = 6371.0;
        // Conversion des degrés en radians
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLatRad = Math.toRadians(lat2 - lat1);
        double deltaLonRad = Math.toRadians(lon2 - lon1);

        // Calcul de la distance en utilisant la formule de haversine
        double a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Conversion de la distance en radians en kilomètres
        return EARTH_RADIUS * c;
    }

    /**
     * Méthode permettant de charger les images
     */
    public void loadImages() {

        ContentResolver contentResolver = getContentResolver();

        // Requête pour récupérer toutes les images
        Uri imagesUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        String[] projection = {MediaStore.Images.Media._ID};
        Cursor imageCursor = contentResolver.query(imagesUri, projection, null, null, null);
        //S'il n'y a pas d'image on quitte
        if (!imageCursor.moveToFirst()) {
            return;
        }

        do {


            int colIndexImage = imageCursor.getColumnIndex(MediaStore.Images.Media._ID);
            if (colIndexImage < 0) {
                continue;
            }
            long imageId = imageCursor.getLong(colIndexImage);
            // Création de l'URI de l'image à partir de son ID
            Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId);

            imageUri = MediaStore.setRequireOriginal(imageUri);

            Pair<Float, Float> latLong = getLatLon(imageUri);
            if (latLong == null) {
                continue;
            }
            double distance = distanceOrthodromique(latLong.first, latLong.second);
            distances.add(new Pair<>(imageUri, distance));


        } while (imageCursor.moveToNext());

        //On trie de façon ascendant
        distances.sort(Comparator.comparing(p -> p.second));


        imageCursor.close();

        buildAdapter();
    }

    /**
     * @param uri
     * @return 0 => Lat 1 => Long
     */
    private Pair<Float, Float> getLatLon(Uri uri) {
        try {
            ExifInterface exif = null;
            ContentResolver contentResolver = getContentResolver();
            //on charge le stream dans l'interface
            exif = new ExifInterface(contentResolver.openInputStream(uri));
            //On récupère la latitude et longitude de l'image
            float[] output = new float[2];
            boolean flag = exif.getLatLong(output);
            if (flag) {
                return new Pair<>(output[0], output[1]);
            } else {
                return null;
            }
        } catch (IOException ignored) {
            return null;
        }
    }

    /**
     * Création de l'adaptateur
     */
    private void buildAdapter() {
        //Création du tableau des uris
        Uri[] uri = new Uri[distances.size()];
        //Création du tableau des distances
        double[] distance = new double[distances.size()];
        //On peuple les tableaux
        for (int i = 0; i < distances.size(); i++) {
            uri[i] = distances.get(i).first;
            distance[i] = distances.get(i).second;
        }
        //On crée notre customAdapter
        CustomListAdapter cla = new CustomListAdapter(this, uri, distance);
        ListView lv = findViewById(R.id.listView_images);
        //On le met sur le listview
        lv.setAdapter(cla);
    }

    /**
     * Méthode permettant de checker les permissions
     */
    public void checkPermissions() {
        //On check la permission de l'access media location
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_MEDIA_LOCATION);
        //Si on possède pas la permission on la demande
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // La permission n'est pas accordée, on la demande à l'utilisateur
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_MEDIA_LOCATION
                    },
                    1
            );
        } else {
            this.loadImages();
        }
    }

    /**
     * Callback pour les permissions
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (permissions.length > 0 && Objects.equals(permissions[0], Manifest.permission.ACCESS_MEDIA_LOCATION)) {
                //Permissions acceptées
                loadImages();
            } else {
                //Permissions non accepetées, on quitte !
                this.finish();
            }
        }
    }
}