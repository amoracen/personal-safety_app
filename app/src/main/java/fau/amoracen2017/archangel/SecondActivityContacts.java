package fau.amoracen2017.archangel;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.muddzdev.styleabletoastlibrary.StyleableToast;

import java.util.Objects;

/**
 * SecondActivity is responsible for the Contacts information
 * @author Alejandro Moracen
 * @author Alicia Mitchell
 */
public class SecondActivityContacts extends AppCompatActivity {

    private TextView nameSelected1TextView, phoneSelected1TextView;
    private TextView nameSelected2TextView, phoneSelected2TextView;
    private boolean formsCompleted = false;
    private boolean update = false;
    static final int PICK_CONTACT1 = 1;
    static final int PICK_CONTACT2 = 2;
    private String contact1,phoneNumber1;
    private String contact2,phoneNumber2;
    private static final int PERMISSION_REQUEST_CODE = 1;
    private String SentMessage = "HI! I am using the ArchAngel application. I added you as an emergency contact.";
    Contact myContacts = null;
    //Database
    FirebaseDatabase database;
    DatabaseReference myRef;
    // User
    FirebaseAuth mFireBaseAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second_contacts);
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkPermission()) {
                Log.e("permission", "Permission already granted.");
            } else {
                requestPermission();
            }
        }

        mFireBaseAuth = FirebaseAuth.getInstance();
        // Write to the database
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("Contacts/");
        //Buttons
        Button loadContact1Btn = this.findViewById(R.id.loadContact1Btn);
        Button loadContact2Btn = this.findViewById(R.id.loadContact2Btn);
        Button nextPageBtn = this.findViewById(R.id.nextPageBtn);
        //TextView
        nameSelected1TextView = findViewById(R.id.nameSelected1TextView);
        phoneSelected1TextView = findViewById(R.id.phoneSelected1TextView);
        nameSelected2TextView = findViewById(R.id.nameSelected2TextView);
        phoneSelected2TextView = findViewById(R.id.phoneSelected2TextView);
        EncUtil.generateKey(getApplicationContext());

        //Button Animation
        final Animation myAnim = AnimationUtils.loadAnimation(this,R.anim.bounce);

        loadContact1Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, PICK_CONTACT1);
            }
        });
        loadContact2Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, PICK_CONTACT2);
            }
        });
        nextPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Only when both forms are completed
                if(!isFormsCompleted()){
                    StyleableToast.makeText(SecondActivityContacts.this,"Both Contacts Required",R.style.myToastNegative).show();
                }else {
                    Intent inToDashboard = new Intent(getApplicationContext(), Dashboard.class);
                    startActivity(inToDashboard);
                    finish();
                }
            }
        });
    }

    /**
     * Return true if both contacts were selected
     * @return true if both forms are completed
     */
    public boolean isFormsCompleted() {
        return formsCompleted;
    }

    /**
     * Set Form completed
     * @param completed a boolean
     */
    public void setFormsCompleted(boolean completed){
        this.formsCompleted = completed;
    }
    /**
     * Check Permission to read contacts
     * @return true if the app has permission to access the contacts
     */
    public boolean checkPermission() {
        int ContactPermissionResult = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_CONTACTS);
        return ContactPermissionResult == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request Permission
     */
    private void requestPermission() {
        ActivityCompat.requestPermissions(SecondActivityContacts.this, new String[]{
                        Manifest.permission.READ_CONTACTS,}, PERMISSION_REQUEST_CODE);
    }

    /**
     * After choosing the contact
     * @param reqCode an integer
     * @param resultCode an integer
     * @param data contacts' data
     */
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);
        switch (reqCode) {
            case (PICK_CONTACT1):
                if (resultCode == Activity.RESULT_OK) {
                    update = true;
                    Uri contactData = data.getData();
                    getContactInfo(contactData,PICK_CONTACT1);
                    phoneSelected1TextView.setText(EncUtil.decrypt(getApplicationContext(),phoneNumber1));
                    nameSelected1TextView.setText(EncUtil.decrypt(getApplicationContext(),contact1));
                }
                break;
            case(PICK_CONTACT2):
                if (resultCode == Activity.RESULT_OK) {
                    update = true;
                    Uri contactData = data.getData();
                    getContactInfo(contactData,PICK_CONTACT2);
                    phoneSelected2TextView.setText(EncUtil.decrypt(getApplicationContext(),phoneNumber2));
                    nameSelected2TextView.setText(EncUtil.decrypt(getApplicationContext(),contact2));
                }
                break;
        }
    }

    public void getContactInfo(Uri contactData,int btnNumber) {
        Cursor c = getContentResolver().query(contactData, null, null, null, null);
        if (c.moveToFirst()) {
            String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
            String hasPhone = c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
            try {

                String cNumber = null;
                String name = null;
                if (hasPhone.equalsIgnoreCase("1")) {
                    Cursor phones = getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                            null, null);
                    phones.moveToFirst();
                    cNumber = phones.getString(phones.getColumnIndex("data1"));
                    name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                }
                if (name != null && cNumber != null) {
                    switch (btnNumber) {
                        case PICK_CONTACT1:
                            //contact1 = name;
                            contact1 = EncUtil.encrypt(getApplicationContext(),name);
                            //phoneNumber1 = cNumber;
                            phoneNumber1 =  EncUtil.encrypt(getApplicationContext(),cNumber);
                            break;
                        case (PICK_CONTACT2):
                            //contact2 = name;
                            contact2 = EncUtil.encrypt(getApplicationContext(),name);
                            //phoneNumber2 = cNumber;
                            phoneNumber2 =  EncUtil.encrypt(getApplicationContext(),cNumber);
                            break;
                        default:
                            break;
                    }
                    if (contact1 != null && contact2 != null) {
                        myContacts = new Contact(contact1, phoneNumber1, contact2, phoneNumber2);
                        saveToDatabase(myContacts);
                        StyleableToast.makeText(SecondActivityContacts.this,"Contacts Saved",R.style.myToastPositive).show();
                        setFormsCompleted(true);
                    }
                } else {
                    update = false;
                    StyleableToast.makeText(SecondActivityContacts.this,"Invalid Contact \n Please Try Again",R.style.myToastNegative).show();
                }
            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            } finally {
                c.close();
            }
        }
    }

    /**
     * Save information to Firebase Database
     * @param contacts a contact object
     */
    public void saveToDatabase(Contact contacts){
        myRef.child(Objects.requireNonNull(mFireBaseAuth.getUid())).setValue(contacts);
        sendTextMessage(SentMessage);
    }
    /**
     * Send Messages to saved Contacts
     *
     * @param mySMS
     */
    public void sendTextMessage(String mySMS) {
        if (myContacts == null) return;
        String phoneNum1 = EncUtil.decrypt(getApplicationContext(),myContacts.getPhoneNumber1());
        String phoneNum2 = EncUtil.decrypt(getApplicationContext(),myContacts.getPhoneNumber2());
        if (!TextUtils.isEmpty(mySMS) && !TextUtils.isEmpty(phoneNum1) && !TextUtils.isEmpty(phoneNum2)) {
            if (checkPermission()) {
                //Get the default SmsManager//
                SmsManager smsManager = SmsManager.getDefault();
                //Send the SMS//
                smsManager.sendTextMessage(phoneNum1, null, mySMS, null, null);
                smsManager.sendTextMessage(phoneNum2, null, mySMS, null, null);
            } else {
                Toast.makeText(SecondActivityContacts.this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Get information from database
     */
    private void loadUserDetails() {
        DatabaseReference userReference = myRef.child(mFireBaseAuth.getUid());
        userReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //myContact = new Contact();
                myContacts = dataSnapshot.getValue(Contact.class);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
//                Toast.makeText(ThreadActivity.this, R.string.error_loading_user, Toast.LENGTH_SHORT).show();
//                finish();
            }
        });
    }
    /**
     * Check if user is login onStart
     */
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        if (mFireBaseAuth.getCurrentUser() == null) {
            Intent inToMain = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(inToMain);
            finish();
        }else{
            new MyAsyncTask().execute();
        }
    }
    private class MyAsyncTask extends AsyncTask<Void, Void, Void>
    {
        @Override
        protected Void doInBackground(Void... params) {
            // Read from database
            database = FirebaseDatabase.getInstance();
            myRef = database.getReference("Contacts/");
            DatabaseReference userReference = myRef.child(Objects.requireNonNull(mFireBaseAuth.getUid()));
            userReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    myContacts = dataSnapshot.getValue(Contact.class);
                    if(myContacts != null && !update){
                        nameSelected1TextView.setText(EncUtil.decrypt(getApplicationContext(),myContacts.getContactName1()));
                        phoneSelected1TextView.setText(EncUtil.decrypt(getApplicationContext(),myContacts.getPhoneNumber1()));
                        nameSelected2TextView.setText(EncUtil.decrypt(getApplicationContext(),myContacts.getContactName2()));
                        phoneSelected2TextView.setText(EncUtil.decrypt(getApplicationContext(),myContacts.getPhoneNumber2()));
                        setFormsCompleted(true);
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    //Toast.makeText(ThreadActivity.this, R.string.error_loading_user, Toast.LENGTH_SHORT).show();
                }
            });
            return null;
        }
    }

}//EOF CLASS