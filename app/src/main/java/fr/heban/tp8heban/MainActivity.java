package fr.heban.tp8heban;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    Uri uri = null;
    ExifInterface exif = null;

    public final static String INTENT_SEND_REF_IMAGE = "fr.heban.tp8heban.INTENT_SEND_REF_IMAGE";
    public final static String INTENT_SEND_LAT = "fr.heban.tp8heban.INTENT_SEND_LAT";
    public final static String INTENT_SEND_LON = "fr.heban.tp8heban.INTENT_SEND_LON";
    public final static String INTENT_SEND_MAX = "fr.heban.tp8heban.INTENT_SEND_MAX";

    /**
     * Callback pour le chargement de l'image
     */
    private ActivityResultLauncher<String> mGetImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            result -> {
                ImageView iv = findViewById(R.id.imageView);
                iv.setImageURI(result);
                loadMetaData(result);
                uri = result;
            }
    );

    /**
     * Méthode permettant d'afficher les meta data d'une uri
     *
     * @param uri
     */
    private void loadMetaData(Uri uri) {
        try {
            //On prend le content resolver pour pouvoir créer un input stream
            ContentResolver contentResolver = getContentResolver();

            //on charge le stream dans l'interface
            exif = new ExifInterface(contentResolver.openInputStream(uri));
            //On récupère les textView de la latitude et longitude
            TextView lat = findViewById(R.id.textview_lat);
            TextView lon = findViewById(R.id.textView_long);
            //On récupère la latitude et longitude de l'image
            float[] output = new float[2];
            //Récupérer la latitude et la longitude
            boolean flag = exif.getLatLong(output);
            if (flag) {
                //On met les valeurs correspondantes
                lat.setText(String.valueOf(output[0]));
                lon.setText(String.valueOf(output[1]));
            } else {
                lat.setText(R.string.app_unknown);
                lon.setText(R.string.app_unknown);
            }
        } catch (IOException e) {
            System.out.println("Error");
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton button = findViewById(R.id.imageButton);
        //souscription à l'événement
        button.setOnClickListener(this::onClickedLoadImage);

        ImageButton gpsLocation = findViewById(R.id.gpsLocationButton);
        //souscription à l'événement
        gpsLocation.setOnClickListener(this::onClickedGpsLocationClicked);

        ImageButton listPhotoButton = findViewById(R.id.listPhotoButton);
        //souscription à l'événement
        listPhotoButton.setOnClickListener(this::onListButtonClicked);

        ImageButton filteredImageButton = findViewById(R.id.mapFilteredButton);
        //souscription à l'événement
        filteredImageButton.setOnClickListener(this::onImageFilteredButtonClicked);

    }

    /**
     * Méthode appelé quand on clique sur le bouton de chargement d'une image
     *
     * @param view
     */
    public void onClickedLoadImage(View view) {
        mGetImage.launch("image/*");
    }

    public void onClickedGpsLocationClicked(View view) {
        //on vérifie que les données nécessaires existent
        if (exif == null || uri == null)
            return;
        //On crée un tableau vide de 2 éléments
        float[] output = new float[2];
        //On récupère la latitude & longitude
        boolean flag = exif.getLatLong(output);
        //Si le flag est vrai, c-a-d que les coordonnées ont été trouvées
        if (flag) {
            Uri geoUri = Uri.parse("geo:" + output[0] + "," + output[1]);
            Intent intent = new Intent(Intent.ACTION_VIEW, geoUri);
            startActivity(intent);
        } else {
            //Sinon on affiche un message d'erreur
            Toast.makeText(this, R.string.app_unknown_location, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Événement lors du clique sur le bouton pour afficher la liste
     * @param view
     */
    public void onListButtonClicked(View view) {
        //si on a pas de fichier sélectionné on quitte
        if (uri == null || exif == null) {
            return;
        }

        try {
            //On prend le content resolver pour pouvoir créer un input stream
            ContentResolver contentResolver = getContentResolver();


            //on charge le stream dans l'interface
            exif = new ExifInterface(contentResolver.openInputStream(uri));

            //On récupère la latitude et longitude de l'image
            float[] output = new float[2];
            //Lat = output[0]
            //Lon = output[1]
            boolean flag = exif.getLatLong(output);
            //Si le flag est vrai, c-a-d que les coordonnées ont été trouvées
            if (flag) {
                //Ouverture de l'intention
                Intent intent = new Intent(this, ListNearImageActivity.class);
                intent.putExtra(INTENT_SEND_REF_IMAGE, uri);
                intent.putExtra(INTENT_SEND_LAT, output[0]);
                intent.putExtra(INTENT_SEND_LON, output[1]);

                startActivity(intent);
            } else {
                //Sinon on affiche un message d'erreur
                Toast.makeText(this, R.string.app_unknown_location, Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            System.out.println("Error");
        }
    }

    /**
     * Événement lors du clique du bouton pour afficher les images, filtrées selon la distance
     * @param ignored
     */
    public void onImageFilteredButtonClicked(View ignored) {
        //Création d'une boite de dialog
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        //Ajout du titre
        alert.setTitle(R.string.app_search);
        //Ajout du layout
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.activity_input_image_filtration, null);
        //ajout de la vue au modal
        alert.setView(view);
        //Ajout du callback pour le bouton de validation
        alert.setPositiveButton(android.R.string.ok, ((dialogInterface, i) -> {
            //On récupère la valeur maximal de la distance
            EditText value = view.findViewById(R.id.input_max_bounds);
            //On parse la valeur
            int dist = Integer.parseInt(value.getText().toString());
            //On crée notre intention
            Intent intent = new Intent(this, GridViewImageActivity.class);
            intent.putExtra(INTENT_SEND_REF_IMAGE, uri);
            intent.putExtra(INTENT_SEND_MAX, dist);
            //Récupération de latitude & longitude
            Pair<Float, Float> latlon = getLatLon(uri);
            if (latlon == null)
                return;
            intent.putExtra(INTENT_SEND_LON, latlon.second);
            intent.putExtra(INTENT_SEND_LAT, latlon.first);
            //Affichage de l'activité
            startActivity(intent);

        }));
        //affichage du modal
        alert.show();

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
}