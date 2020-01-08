package fau.amoracen2017.archangel;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.muddzdev.styleabletoastlibrary.StyleableToast;

import java.util.Objects;

/**
 * MainActivity is responsible for registration and Login
 * @author Alejandro Moracen
 * @author Alicia Mitchell
 */
public class MainActivity extends AppCompatActivity {

    //declare variables
    private EditText emailId, pwd;
    private FirebaseAuth mFireBaseAuth;
    private CheckBox checkBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mFireBaseAuth = FirebaseAuth.getInstance();
        emailId = findViewById(R.id.emailEditText);
        pwd = findViewById(R.id.passwordEditText);
        final Button signUpbtn = findViewById(R.id.signUpbtn);
        final Button signInbtn = findViewById(R.id.signInbtn);
        TextView signInTextView = findViewById(R.id.signInTextView);
        TextView signUpTextView = findViewById(R.id.signUpTextView);
        checkBox = findViewById(R.id.checkBox);
        //Button Animation
        final Animation myAnim = AnimationUtils.loadAnimation(this,R.anim.bounce);

        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked) {
                    //Show Password
                    pwd.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                }else{
                    //Hide Password
                    pwd.setTransformationMethod(PasswordTransformationMethod.getInstance());
                }
            }
        });

        signInTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInbtn.setVisibility(View.VISIBLE);
                signUpbtn.setVisibility(View.INVISIBLE);
            }
        });
        signUpTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signInbtn.setVisibility(View.INVISIBLE);
                signUpbtn.setVisibility(View.VISIBLE);
            }
        });

        // Sign up button that creates an account for you in Firebase with conditions to check
        // if you are signing up properly
        signUpbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailId.getText().toString();
                String password = pwd.getText().toString();
                if (email.isEmpty()) {
                    emailId.setError("Please enter your email address");
                    emailId.setText("");
                    emailId.requestFocus();
                } else if (password.isEmpty()) {
                    pwd.setError("Please enter your password");
                    pwd.setText("");
                    pwd.requestFocus();
                } else {
                    mFireBaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                StyleableToast.makeText(MainActivity.this,"Sign Up Unsuccessful \n Please Try Again",R.style.myToastNegative).show();
                                MainActivity.this.finish();
                                MainActivity.this.startActivity(MainActivity.this.getIntent());
                            } else {
                                StyleableToast.makeText(MainActivity.this,"Account Created",R.style.myToastPositive).show();
                                Intent inToContacts = new Intent(getApplicationContext(), SecondActivityContacts.class);
                                startActivity(inToContacts);
                                finish();
                            }
                        }
                    });
                }
            }
        });

        //Button with conditional checks to ensure you properly entered
        signInbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailId.getText().toString();
                String password = pwd.getText().toString();
                if (email.isEmpty()) {
                    emailId.setError("Please enter your email address");
                    emailId.requestFocus();
                } else if (password.isEmpty()) {
                    pwd.setError("Please enter your password");
                    pwd.requestFocus();
                } else {
                    mFireBaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                StyleableToast.makeText(MainActivity.this,"Login Unsuccessful \n Please Try Again",R.style.myToastNegative).show();
                                MainActivity.this.finish();
                                MainActivity.this.startActivity(MainActivity.this.getIntent());
                            } else {
                                StyleableToast.makeText(MainActivity.this,"Login Successful",R.style.myToastPositive).show();
                                Intent inToDashboard = new Intent(getApplicationContext(), Dashboard.class);
                                startActivity(inToDashboard);
                                finish();
                            }
                        }
                    });
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mFireBaseAuth.getCurrentUser();
        if (currentUser != null) {
            //Go to Dashboard
            Intent inToDashboard = new Intent(getApplicationContext(), Dashboard.class);
            startActivity(inToDashboard);
            finish();
        }
    }
}
