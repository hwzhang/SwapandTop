package com.example.tapiwa.swapandtop;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.concurrent.TimeUnit;

public class LoginScreen extends AppCompatActivity {

    private EditText firstNameTxtV, lastNameTxtV, phoneNumberTxtV;
    private ProgressDialog verificationProgress;
    private Button sendVerificationBtn;
    private String TAG = "lOGIN_SCREEN";
    public static PhoneAuthCredential phoneCredential;
    private FirebaseAuth mAuth;
    private DatabaseReference usersDbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);

        firstNameTxtV = findViewById(R.id.login_firstName);
        lastNameTxtV = findViewById(R.id.login_lastName);
        phoneNumberTxtV = findViewById(R.id.login_phonenumber);
        sendVerificationBtn = findViewById(R.id.send_verification_btn);
        verificationProgress = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();

        setListeners();
    }


    private void setListeners() {

        phoneNumberTxtV.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if(b) {
                    phoneNumberTxtV.setHint("+263 711 111 222");
                } else {
                    phoneNumberTxtV.setHint(getString(R.string.login_phone_number));
                }
            }
        });


        sendVerificationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendVerificationCode();
            }
        });

    }


    //check to see that the name has been entered correctly
    private boolean checkFirstName() {

        boolean valid = true;

        if(firstNameTxtV.getText().toString().trim().length() > 0) {
            return valid;
        } else {
            firstNameTxtV.setError(getString(R.string.invalid_first_name));
            return !valid;
        }
    }

    private boolean checkLastName() {
        boolean valid = true;

        if(lastNameTxtV.getText().toString().trim().length() > 0) {
            return valid;
        } else {
            lastNameTxtV.setError(getString(R.string.invalid_last_name));
            return !valid;
        }
    }

    private boolean checkPhoneNumber() {

      boolean valid = true;

      if(phoneNumberTxtV.getText().toString().trim().length() == 0) {
          phoneNumberTxtV.setError(getString(R.string.invalid_phone_number));
          return !valid;
      }

        //check that the first character is a +
      if(!phoneNumberTxtV.getText().toString().substring(0,1).equals("+")) {
          phoneNumberTxtV.setError(getString(R.string.invalid_country_code));
          return !valid;
      }

      return valid;
    }


    private void sendVerificationCode() {

      if(checkFirstName() && checkLastName() && checkPhoneNumber()) {

          //show progress bar waiting to automatically detect code sent
          verificationProgress.setMessage(getString(R.string.waiting_to_detect_msg));
          verificationProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
          verificationProgress.setIndeterminate(true);
          verificationProgress.show();


          PhoneAuthProvider.getInstance().verifyPhoneNumber(
                  phoneNumberTxtV.getText().toString(),        // Phone number to verify
                  60,                 // Timeout duration
                  TimeUnit.SECONDS,   // Unit of timeout
                  this,               // Activity (for callback binding)
                  new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                      @Override
                      public void onVerificationCompleted(PhoneAuthCredential phoneAuthCredential) {
                            verificationProgress.dismiss();
                            signInWithPhoneAuthCredential(phoneAuthCredential);
                      }

                      @Override
                      public void onVerificationFailed(FirebaseException e) {
                        verificationProgress.dismiss();
                        //show message that account registration failed

                      }
                  });
      }

    }


    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {


                            String firstName, lastName, phoneNumber, uid;

                            firstName = firstNameTxtV.getText().toString().trim();
                            lastName = lastNameTxtV.getText().toString().trim();
                            phoneNumber = phoneNumberTxtV.getText().toString().trim();
                            uid = mAuth.getUid().toString();


                            //compile user data .
                            User newUser = new User();
                            newUser.setFirstName(firstName);
                            newUser.setLastName(lastName);
                            newUser.setPhoneNumber(phoneNumber);
                            newUser.setUid(uid);

                            //add user details to the sharedPreferences
                            SharedPreferences sharedPreferences =
                                    getSharedPreferences(getString(R.string.shared_preferences_file),
                                            Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("full_name", firstName + " " + lastName);
                            editor.putString("phoneNumber", phoneNumber);
                            editor.putString("uid", uid);
                            editor.commit();

                            //add user to database
                            FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
                            usersDbRef = firebaseDatabase.getReference(getString(R.string.users_db_ref));

                            usersDbRef
                                    .child(mAuth.getUid().toString())
                                    .setValue(newUser)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    Toast.makeText(
                                            getApplicationContext(),
                                            getString(R.string.account_creation_successful),
                                            Toast.LENGTH_SHORT).show();

                                    //Move to the account
                                }
                            });


                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");

                            FirebaseUser user = task.getResult().getUser();
                            // ...
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                            }
                        }
                    }
                });
    }










    //check to see that the last name has been entered correctly
    //check to see that the phone number has been entered correctly
    //waiting to automatically detect code sent to your phone
    //if 1:30 seconds elapses then ask user to manually enter code by themselves





}
