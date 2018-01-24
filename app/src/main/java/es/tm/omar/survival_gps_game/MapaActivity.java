package es.tm.omar.survival_gps_game;

import android.Manifest;
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

import java.text.DecimalFormat;
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

    DecimalFormat df;
    Calendar timeFormat;

    private SqliteManager sqlite;

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

///////////////////////////SQL//////////////////
        //Cargamos la clase para el sqlite
        sqlite = new SqliteManager(this, "sgg.db", null, 1);

        //Ahora podemos ejecutar comandos sobre la bd
        System.out.println("SQLite path: " + sqlite.db.getPath() + " , " + sqlite.getDatabaseName());
        /*
        Comandos sql de pruebas
         */
        //ejecutar la primera vez que se carga la app
        try {
            //sqlite.db.execSQL("drop table usuarios");
            //sqlite.db.execSQL("drop table testRuta");
            sqlite.db.execSQL("create table usuarios(id integer,nombre text, lastKnownLatitude real,lastKnownLongitude real)");
            sqlite.db.execSQL("insert into usuarios values(77,'Omar', 0.0,0.0)");
        }catch(SQLiteException es){

        }
        //creamos el cursor con la busqueda que queremos realizar
        Cursor c = sqlite.db.rawQuery("select name from sqlite_master where type='table'", null);

        //con estos dos comandos obtenemos el numero de filas y columnas
        /*
        int filas=c.getCount();
        int columnas= c.getColumnCount();

        Toast.makeText(this, "filas: "+filas, Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "columnas: "+columnas, Toast.LENGTH_SHORT).show();
        */


        /*
        el cursor empieza en la posicion -1 por lo que hay que moverlo a la posicion 0 (que se corresponde con la
        primera fila, en caso de haberla)
        Como solo hay una columna en la busqueda, accedemos a ella con la posicion 0 (columnIndex:0)
        */
        for (int i = 0; i < c.getCount(); i++) {
            System.out.println(c.getPosition());
            Toast.makeText(this, "Posicion del cursor: " + c.getPosition(), Toast.LENGTH_SHORT).show();
            c.moveToNext();
            System.out.println(c.getString(0));
            Toast.makeText(this, c.getString(0), Toast.LENGTH_SHORT).show();
        }

        ///////////////////////Comprobando las coordenadas almacenadas///////////////////////
        Cursor d = sqlite.db.rawQuery("select * from usuarios", null);
        System.out.println("FILAS DE USUARIO ENCONTRADAS: " + d.getCount());

        /*
        el cursor empieza en la posicion -1 por lo que hay que moverlo a la posicion 0 (que se corresponde con la
        primera fila, en caso de haberla)
        Como solo hay una columna en la busqueda, accedemos a ella con la posicion 0 (columnIndex:0)
        */
        for (int i = 0; i < d.getCount(); i++) {
            d.moveToNext();
            currentLocation = new Location("");
            currentLocation.setLatitude(d.getDouble(2));
            currentLocation.setLongitude(d.getDouble(3));
            Toast.makeText(this, "Lat: " + d.getDouble(2) + " Lon: " + d.getDouble(3), Toast.LENGTH_SHORT).show();
        }


/////////////////////SQL END/////////////////////

        rutaOptions = sqlite.ultimaRuta();

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
        sqlite.close();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //guardo la ultima localizacion
        float lat = (float) currentLocation.getLatitude();
        float lon = (float) currentLocation.getLongitude();
        String consulta = "update usuarios set lastKnownLatitude=" + lat + ", lastKnownLongitude=" + lon + " where id=77";
        sqlite.db.execSQL(consulta);
        System.out.println("Localizacion guardada en onPause()");
        //Guardo la ruta
        if(ruta!=null) {
            List<LatLng> linea = ruta.getPoints();
            int i = 0;
            String dropTable = "drop table testRuta";
            String crearTabla = "create table testRuta (position integer primary key autoincrement,lat real ,lng real)";
            sqlite.db.execSQL(dropTable);
            sqlite.db.execSQL(crearTabla);
            for (LatLng lt : linea) {
                String consulta2 = "insert into testRuta values(" + i + "," + lt.latitude + "," + lt.longitude + ")";
                sqlite.db.execSQL(consulta2);
                i++;
            }

        }
        stopLocationUpdates();

    }

    @Override
    protected void onResume() {
        super.onResume();
        startLocationUpdates();
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

        LatLng latLng=new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));

        startLocationUpdates();

    }

    protected void createLocationRequest() {
        /*
        Todo hacer pruebas con diferentes intervalos y desplazamientos. Segun la documentacion el fastest interval es aprox 6x veces el interval
         */
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(30000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(0);
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
}
