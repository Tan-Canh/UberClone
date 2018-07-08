package canh.tan.nguye.uberclone;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

import canh.tan.nguye.uberclone.model.User;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    FirebaseDatabase database;
    DatabaseReference userRef;

    FirebaseAuth auth;

    Button btnSignIn, btnRegister;

    RelativeLayout layout_register, layout_sign_in;

   /* @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                                        .setDefaultFontPath("fonts/arkhip_font.ttf")
                                        .setFontAttrId(R.attr.fontPath)
                                        .build());*/

        setContentView(R.layout.activity_main);

        database = FirebaseDatabase.getInstance();
        userRef = database.getReference("Users");
        auth = FirebaseAuth.getInstance();

        btnSignIn = findViewById(R.id.btn_sign_in);
        btnRegister = findViewById(R.id.btn_register);


        layout_register = findViewById(R.id.layout_register);
        layout_sign_in = findViewById(R.id.layout_sign_in);
        btnRegister.setOnClickListener(this);
        btnSignIn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        final Dialog builder = new Dialog(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        final View view;


        switch (v.getId()){
            case R.id.btn_register:

                view = inflater.inflate(R.layout.dialog_register, null, false);

                builder.setContentView(view);
                builder.setTitle("Register");
                builder.setCancelable(false);

                final MaterialEditText edit_email, edit_password, edit_name, edit_phone;
                Button btnCancel, btnRegister_register;

                edit_email = view.findViewById(R.id.email_edit_register);
                edit_password = view.findViewById(R.id.edit_password_register);
                edit_name = view.findViewById(R.id.edit_name_register);
                edit_phone = view.findViewById(R.id.edit_phone_register);

                btnCancel = view.findViewById(R.id.btn_no_register);
                btnRegister_register = view.findViewById(R.id.btn_register);

                btnCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        builder.dismiss();
                    }
                });


                btnRegister_register.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (TextUtils.isEmpty(edit_email.getText().toString()) || TextUtils.isEmpty(edit_password.getText().toString())
                                || TextUtils.isEmpty(edit_name.getText().toString()) || TextUtils.isEmpty(edit_phone.getText().toString())){

                            if (TextUtils.isEmpty(edit_email.getText().toString())){
                                edit_email.setErrorColor(Color.RED);
                                edit_email.setError("cannot be empty!");
                            }

                            if (TextUtils.isEmpty(edit_password.getText().toString())){
                                edit_password.setErrorColor(Color.RED);
                                edit_password.setError("cannot be empty!");
                            }

                            if (TextUtils.isEmpty(edit_name.getText().toString())){
                                edit_name.setErrorColor(Color.RED);
                                edit_name.setError("cannot be empty!");
                            }

                            if (TextUtils.isEmpty(edit_phone.getText().toString())){
                                edit_phone.setErrorColor(Color.RED);
                                edit_phone.setError("cannot be empty!");
                            }
                        }else {
                            auth.createUserWithEmailAndPassword(edit_email.getText().toString(), edit_password.getText().toString())
                                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                        @Override
                                        public void onSuccess(AuthResult authResult) {

                                            User user = new User(edit_email.getText().toString()
                                                    , edit_name.getText().toString()
                                                    , edit_phone.getText().toString());

                                            userRef.child(authResult.getUser().getUid()).setValue(user)
                                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                        @Override
                                                        public void onSuccess(Void aVoid) {
                                                            Snackbar.make(view, "Register successfully", Snackbar.LENGTH_SHORT).show();
                                                            builder.dismiss();
                                                        }
                                                    }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Snackbar.make(view, "Register failed!", Snackbar.LENGTH_SHORT).show();
                                                }
                                            });
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {

                                            Toast.makeText(MainActivity.this, "User existed!\nPlease! choose email difference", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    }
                });

                builder.show();
                break;
            case R.id.btn_sign_in:
                //AlertDialog.Builder builder = new AlertDialog.Builder(this);

                view = inflater.inflate(R.layout.dialog_sign_in, null);

                builder.setContentView(view);
                builder.setTitle("Sign In");
                builder.setCancelable(false);

                final MaterialEditText edit_email_sign_in, edit_password_sign_in;
                Button btnCancelSignIn, btnSignIn_SignIn;

                edit_email_sign_in = view.findViewById(R.id.edit_email_sign_in);
                edit_password_sign_in = view.findViewById(R.id.edit_password_sign_in);
                btnCancelSignIn = view.findViewById(R.id.btn_no_sign_in);
                btnSignIn_SignIn = view.findViewById(R.id.btn_sign_in);

                btnCancelSignIn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        builder.dismiss();
                    }
                });

                btnSignIn_SignIn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (TextUtils.isEmpty(edit_email_sign_in.getText().toString()) || TextUtils.isEmpty(edit_password_sign_in.getText().toString())){

                            if (TextUtils.isEmpty(edit_email_sign_in.getText().toString())){
                                edit_email_sign_in.setErrorColor(Color.RED);
                                edit_email_sign_in.setError("cannot be empty!");
                            }

                            if (TextUtils.isEmpty(edit_password_sign_in.getText().toString())){
                                edit_password_sign_in.setErrorColor(Color.RED);
                                edit_password_sign_in.setError("cannot be empty!");
                            }
                        }else {
                            auth.signInWithEmailAndPassword(edit_email_sign_in.getText().toString(), edit_password_sign_in.getText().toString())
                                    .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                        @Override
                                        public void onSuccess(AuthResult authResult) {
                                            Toast.makeText(MainActivity.this, "Sign in successfully", Toast.LENGTH_SHORT).show();
                                            builder.dismiss();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(MainActivity.this, "Usert not exits!", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }
                });

                builder.show();
                break;
        }
    }
}
