package org.tensorflow.demo.env;
import android.app.Activity;
import android.app.ProgressDialog;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.BatteryManager;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseError;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.tensorflow.demo.DetectorActivity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import static android.content.ContentValues.TAG;
import static org.tensorflow.demo.env.Contadores.GPSIDactual;
import static org.tensorflow.demo.env.Contadores.fotos_a_entrenar;
import static org.tensorflow.demo.env.Contadores.frec_fotos;

/**
 * Created by martin on 07-01-18.
 */


public class senddata extends Activity {


 //    FirebaseDatabase.getInstance().setPersistenceEnabled(true);
 private static final Logger LOGGER = new Logger();
    public FirebaseStorage storage;
    public StorageReference storageRef;



    public void getMyRef(String campo ,String text) {

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(campo);
        myRef.setValue(text);

        return;
    }

    public void CarDetect(final String GPSID, final String FECHAID, final String OBJETO, final int CANTIDAD){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String currentDateandTime = sdf.format(new Date());

        if(Contadores.ContRevisados){
        int contadors ;
        if (!Contadores.ListaObjetos.contains(OBJETO)) {
            Contadores.ListaObjetos.add(OBJETO);
            Contadores.CuentaObjetos.add(1);
            contadors = CANTIDAD;
            LOGGER.i("aqui objeto nuevo: "+ OBJETO+ " #: "+ contadors);
        }else {
            int indice = Contadores.ListaObjetos.indexOf(OBJETO);
            contadors = Contadores.CuentaObjetos.get(indice) + 1;
            Contadores.CuentaObjetos.set(indice, contadors);

            LOGGER.i("aqui objeto antiguo: "+ OBJETO+ " #: "+ contadors + " en indice: "+ indice);
        }


        DatabaseReference database = FirebaseDatabase.getInstance().getReference();

        database.child(GPSID).child(String.valueOf(FECHAID)).child(OBJETO).setValue(contadors);
            database.child(GPSIDactual).child("Fecha").setValue(currentDateandTime);


    }
    }

    public void SetGPSID(){
        if (GPSIDactual == null){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String currentDateandTime = sdf.format(new Date());


            GPSIDactual = "lat:"+ Contadores.lat
                    +"@lon:"+Contadores.lon;
            DatabaseReference database = FirebaseDatabase.getInstance().getReference();
            database.child(GPSIDactual).child("lat").setValue(Contadores.lat);
            database.child(GPSIDactual).child("lon").setValue(Contadores.lon);
            database.child(GPSIDactual).child("Fecha").setValue(currentDateandTime);


            // martin coloca el flag foto en 1 para subir la primera foto
            database.child(GPSIDactual).child("foto").setValue(1);
            Log.i(TAG, "SetGPSID: ");

        } else {
        }

        return;
    }

    public void CheckContadores(){

        if(Contadores.ListaObjetos.isEmpty()){
            Contadores.CuentaObjetos.clear();
            Contadores.CuentaObjetosFireB.clear();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            final String currentDateandTime = sdf.format(new Date());

            LOGGER.i(GPSIDactual+ " aqui lista vacia");
            DatabaseReference database = FirebaseDatabase.getInstance().getReference(GPSIDactual+"/"+currentDateandTime);
            database.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // ...

                    LOGGER.i("aqui Count " +dataSnapshot.getChildrenCount());
                    for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {

                        LOGGER.i("Get Key: "+ postSnapshot.getKey() + " aqui Get Valor: " + postSnapshot.getValue());
                        Contadores.ListaObjetos.add(postSnapshot.getKey());
                        Contadores.CuentaObjetosFireB.add(String.valueOf(postSnapshot.getValue()));

                    }


                    /*for (int indice = Contadores.CuentaObjetosFireB.size()-1; indice >= 0 ; indice--) {

                     Contadores.CuentaObjetos.add(Integer.parseInt(Contadores.CuentaObjetosFireB.get(indice)));
                       logger.i("Cargando " +Contadores.ListaObjetos.get(indice)+ " #"+ Contadores.CuentaObjetos.get(indice) + " indice:"+ indice + " aqui" );

                    }*/

                    for (String contstr : Contadores.CuentaObjetosFireB){
                        Contadores.CuentaObjetos.add(Integer.parseInt(contstr));
                        LOGGER.i("aqui cargarndo #"+ contstr);
                    }
                    for (int contint : Contadores.CuentaObjetos){
//                        logger.i("aqui cargaron "+ Contadores.CuentaObjetos.get(contint)+" en #"+ contint);
                    }
                    //if (!dataSnapshot.exists()){
                      //  Contadores.ContRevisados = false;
                    //}else {
                        Contadores.ContRevisados = true;
                    //}

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // ...
                }
            });


        } else {LOGGER.i(GPSIDactual+ " aqui lista llena "+ Contadores.ListaObjetos.size());}
    }

//martin sube imagen a Firebase
    public void uploadImage(final String GPSIDactual) {
        //martin Saca la foto


        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference(GPSIDactual).child("foto");

        // Read from the database revisa si el flag de foto está en 1 sube la foto
        // y lo pondrá en 0 nuevamente.
        ValueEventListener valueEventListener = myRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String valfoto =  String.valueOf(dataSnapshot.getValue());
                LOGGER.i("mierdita  " + dataSnapshot.getChildrenCount() + " " + GPSIDactual + " key:" + dataSnapshot.getKey() + " val:" + dataSnapshot.getValue()+" oie  "+valfoto);

                 if (Integer.valueOf(valfoto)==1){
                    final String filePath = "sdcard/DCIM/recdata/" + GPSIDactual + ".png";
                    LOGGER.i("grabada " + filePath);
                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    StorageReference storageRef = storage.getReference();
                    if (filePath != null) {
                        Uri file = Uri.fromFile(new File(filePath));
                        StorageReference riversRef = storageRef.child("images/" + file.getLastPathSegment());
                        UploadTask uploadTask = riversRef.putFile(file);


                        // Register observers to listen for when the download is done or if it fails
                        uploadTask.addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                // Handle unsuccessful uploads
                                LOGGER.i("no sube foto grabada_ "+ exception+ "!!!");

                            }
                        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                                LOGGER.i("si sube foto grabada");
                                DatabaseReference database = FirebaseDatabase.getInstance().getReference();
                                database.child(GPSIDactual).child("foto").setValue(0);
                                // ...
                            }
                        });
                    }
                 }

                // martin sube el set de fotos para entrenar
                if (valfoto == "2") {
                    for (int i = 0; i < fotos_a_entrenar.size(); i++){
                        final String filePath = "sdcard/DCIM/recdata/" + fotos_a_entrenar.get(i);
                        LOGGER.i("grabada para entrenar " + filePath);
                        FirebaseStorage storage = FirebaseStorage.getInstance();
                        StorageReference storageRef = storage.getReference();
                        if (filePath != null) {
                            Uri file = Uri.fromFile(new File(filePath));
                            StorageReference riversRef = storageRef.child("images/" + file.getLastPathSegment());
                            UploadTask uploadTask = riversRef.putFile(file);


                            // Register observers to listen for when the download is done or if it fails
                            uploadTask.addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle unsuccessful uploads
                                    LOGGER.i("no sube foto grabada para entrenar "+filePath);

                                }
                            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                                    LOGGER.i("si sube foto grabada entrenar");
                                    DatabaseReference database = FirebaseDatabase.getInstance().getReference();
                                    database.child(GPSIDactual).child("foto").setValue(0);
                                   // final String fname = filename;
                                    final File file = new File(filePath);
                                    if (file.exists()) {
                                        file.delete();
                                        fotos_a_entrenar.clear();
                                    }
                                    LOGGER.i("archivo "+filePath+" borrado");
                                    // ...
                                }
                            });
                        }
                    }
                }
                // Revisa la frecuencia en la toma de fotos y luego cambia a 0 el flag
                if(Integer.valueOf(valfoto) > Integer.valueOf("999")){
                     frec_fotos = Integer.valueOf(valfoto);

                    DatabaseReference database = FirebaseDatabase.getInstance().getReference();
                    database.child(GPSIDactual).child("foto").setValue(0);

                }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                LOGGER.i("Failed to read value.", error.toException());
            }
        });


    }

    //martin Alarma corriente desconectada:

    public void estado(String estado_d){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd.HH.mm.ss");
        String currentDateandTime = sdf.format(new Date());
        DatabaseReference database = FirebaseDatabase.getInstance().getReference();
        database.child(GPSIDactual).child("Estado").setValue(estado_d+" "+currentDateandTime);
    }

    public void onCancelled(DatabaseError databaseError) {
        Log.w(TAG, "postComments:onCancelled", databaseError.toException());

    }



}



