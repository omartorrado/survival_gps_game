package es.tm.omar.survival_gps_game;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.location.Location;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Array;
import java.sql.SQLOutput;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MapaActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private TextView textViewAccuracy;
    private TextView textViewLat;
    private TextView textViewLng;
    private TextView textViewDist;
    private TextView textViewBearing;
    private TextView textViewSpeed;
    private TextView textViewAltitude;
    private TextView textViewTime;

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    private Location currentLocation;
    private Polyline ruta = null;
    private PolylineOptions rutaOptions;
    private LatLng posicionActual;

    private SharedPreferences sharedPreferences;
    private String ultimaRuta;

    DecimalFormat df;
    Calendar timeFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mapa);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        textViewAccuracy = findViewById(R.id.textViewAccuracy);
        textViewLat = findViewById(R.id.textViewLat);
        textViewLng = findViewById(R.id.textViewLng);
        textViewDist = findViewById(R.id.textViewDistancia);
        textViewBearing = findViewById(R.id.textViewBearing);
        textViewSpeed = findViewById(R.id.textViewSpeed);
        textViewAltitude = findViewById(R.id.textViewAltitude);
        textViewTime = findViewById(R.id.textViewTime);

        df = new DecimalFormat("0.0000");
        timeFormat = Calendar.getInstance();

        //Inicializamos las sharedPreferences
        sharedPreferences=getSharedPreferences("rutas", Context.MODE_PRIVATE);
        String rutaGuardada=sharedPreferences.getString("ultimaRuta","");

        ///////Aqui cargamos la ruta guardada en caso de haberla(o no, ya que se carga en onresume

        rutaOptions=new PolylineOptions();
        rutaOptions.color(Color.RED);
        /*
        if(!rutaGuardada.equals("")){
            List l=cargarRuta();
            for(Object coords:l) {
                String[] temp=coords.toString().split(",");
                Double lat=Double.parseDouble(temp[0].substring(1));
                Double lng=Double.parseDouble(temp[1].substring(0,temp[1].length()-1));
                //System.out.println(temp[0]+"..."+temp[1]);
                //System.out.println(lat+"..."+lng);
                rutaOptions.add(new LatLng(lat,lng));
            }
        }
        */

        //cargamos el listener de la localizacion
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        final Location testLocation = new Location("");

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    //inicializo la ruta que vamos a mostrar cargando los datos de la db

                    ruta=mMap.addPolyline(rutaOptions);

                    /*
                    Todo Aqui tengo que hacer los calculos para en lugar de mostrar la posicion real, mostrar la posicion ingame
                    y que el desplazamiento sea respecto a esta
                     */

                    timeFormat.setTimeInMillis(location.getTime());

                    textViewAccuracy.setText("Accu: " + location.getAccuracy());
                    textViewLat.setText("Lat: " + df.format(location.getLatitude()));
                    textViewLng.setText("Lng: " + df.format(location.getLongitude()));
                    textViewDist.setText("Dist: " + location.distanceTo(testLocation) + "m");
                    textViewBearing.setText("Bearing: " + location.getBearing());
                    textViewSpeed.setText("Speed: " + location.getSpeed());
                    textViewAltitude.setText("Alt: " + df.format(location.getAltitude()));
                    textViewTime.setText("Time: " + timeFormat.getTime());

                    currentLocation = location;
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

                    //El primer if probablemente sea innecesario (el else if no)
                    if (ruta == null && currentLocation.getAccuracy() <= 50) {
                        rutaOptions.add(latLng);
                        rutaOptions.color(Color.RED);
                        ruta = mMap.addPolyline(rutaOptions);
                    } else if (currentLocation.getAccuracy() <= 50) {
                        List linea = ruta.getPoints();
                        linea.add(latLng);
                        ruta.setPoints(linea);

                    } else {
                        Toast.makeText(MapaActivity.this, "Precision insuficiente", Toast.LENGTH_SHORT).show();
                    }

                }
            }
        };

        createLocationRequest();

        currentLocation=testLocation;

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //guardo la ultima localizacion
        if(ruta!=null) {
            guardarRuta(ruta.getPoints());
        }
        stopLocationUpdates();

    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
        System.out.println("Pasamos por on Resume");
        String rutaGuardada=sharedPreferences.getString("ultimaRuta","");
        if(!rutaGuardada.equals("")){
            List l=cargarRuta();
            rutaOptions=new PolylineOptions();
            rutaOptions.color(Color.RED);
            for(Object coords:l) {
                String[] temp=coords.toString().split(",");
                Double lat=Double.parseDouble(temp[0].substring(1));
                Double lng=Double.parseDouble(temp[1].substring(0,temp[1].length()-1));
                rutaOptions.add(new LatLng(lat,lng));
            }
        }
        if(ruta!=null) {
            ruta.setPoints(rutaOptions.getPoints());
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mMap.setMyLocationEnabled(true);

        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapa_style_json));

        startLocationUpdates();

        posicionActual=new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(posicionActual,15));



    }

    protected void createLocationRequest() {
        /*
        Todo hacer pruebas con diferentes intervalos y desplazamientos. Segun la documentacion el fastest interval es aprox 6x veces el interval
         */
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(30000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(30);
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                mLocationCallback,
                null /* Looper */);
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    public void guardarRuta(List<LatLng> ruta){
        List listaTemp = new ArrayList();
        for(LatLng l:ruta){
            listaTemp.add(new double[]{l.latitude, l.longitude});
        }
        String json=new Gson().toJson(listaTemp);
        System.out.println("Ruta Guardada: "+json);
        //Cargamos el editor de SharedPreferences en una variable
        SharedPreferences.Editor prefEdit= sharedPreferences.edit();
        //Guardamos cada una de las preferencias pasandole un nombre y el valor
        prefEdit.putString("ultimaRuta",json);
        //realizamos un commit para que guarde los cambios, espere a terminar y continue con el codigo
        prefEdit.commit();
        /*
        con prefEdit.apply(); tb guardaria los cambios, pero en lugar de esperar a guardarlo tudo
        continua ejecutando el codigo mientras las guarda
        */
    }

    public List cargarRuta(){
        String json=sharedPreferences.getString("ultimaRuta","");
        List lll=new Gson().fromJson(json,List.class);
        System.out.println("Ruta Cargada: "+lll);
        return lll;
    }
}
