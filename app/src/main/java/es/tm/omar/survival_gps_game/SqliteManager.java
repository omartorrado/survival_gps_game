package es.tm.omar.survival_gps_game;


import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Color;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;

/**
 * Creado por Omar Torrado Míguez el 14/01/2018
 */

public class SqliteManager extends SQLiteOpenHelper{

    SQLiteDatabase db;

    public SqliteManager(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        db=this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    /*
    AQUI IRAN LOS METODOS QUE MANEJEN LA BASE DE DATOS
    todo mover aqui los metodos relacionados con la db que estan en MapaActivity
     */

    //Esto no funciona/no esta terminado
    public PolylineOptions ultimaRuta(){
        PolylineOptions ruta=new PolylineOptions();
        ruta.color(Color.RED);
        ruta.add(new LatLng(0,0));
        try {
            db.execSQL("create table testRuta (position integer primary key autoincrement,lat real ,lng real)");
            Cursor polylinePoints = db.rawQuery("select position,lat,lng from testRuta order by position", null);
            //////////////////
            //System.out.println("Puntos= "+polylinePoints.getCount());
            for(int i =0;i<polylinePoints.getCount();i++) {
                polylinePoints.moveToNext();
                LatLng tempLatLong=new LatLng(polylinePoints.getDouble(1),polylinePoints.getDouble(2));
                ruta.add(tempLatLong);
        }
        }catch(SQLiteException ex) {
            Cursor polylinePoints = db.rawQuery("select position,lat,lng from testRuta order by position", null);
            //////////////////
            //System.out.println("Puntos= " + polylinePoints.getCount());
            for (int i = 0; i < polylinePoints.getCount(); i++) {
                polylinePoints.moveToNext();
                LatLng tempLatLong = new LatLng(polylinePoints.getDouble(1), polylinePoints.getDouble(2));
                ruta.add(tempLatLong);
            }
        }
        //System.out.println("Ruta::::::::::::::::::::::::::"+ruta.getPoints());
        return ruta;
    }
}
