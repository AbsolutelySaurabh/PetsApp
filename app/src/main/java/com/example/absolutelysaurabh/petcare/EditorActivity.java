package com.example.absolutelysaurabh.petcare;

import android.app.LoaderManager;
import android.content.ContentValues;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.absolutelysaurabh.petcare.data.PetContract;


//In this activity we implemented the user inputs
//Also inserted the new rows created into the database
//And implemented the Spinner for selection of gender
public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    //Identfier for the pet data loader
    private static final int EXISTING_PET_LOADER = 0;

    //Content URI for the existing pet ( null if it's a new pet)
    private Uri mCurrentPetUri;

    //EditText fields to edit the pets detains
    private EditText mNameEditText;
    private EditText mBreedEditText;
    private EditText mWeightEditText;

    private Spinner mGenderSpinner;

    private int mGender = PetContract.PetEntry.GENDER_UNKNOWN;

    //Boolean flag that keeps track of whether the pet has been edited
    //or not
    private boolean mPetHasChanged = false;

    //OnTouchListener that listens for any user touches on a View,
    //implying that they are modifying trhe View, and we chnage the
    //mPetHasChanged boolean to true

    private View.OnTouchListener mTouchListener = new View.OnTouchListener(){


        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {

            mPetHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        //Examine the intent was used to launch this activity
        //in order to figure out if we're creating a new pet or
        //editing an existing one
        Intent intent = getIntent();

        mCurrentPetUri = intent.getData();

        Uri currentPetUri = intent.getData();

        //If the intent DOES NOT contain a pet content URI,
        //then we know that we are
        //creating a new pet
        if(mCurrentPetUri == null){

            //This is a new pet
            setTitle("Add a Pet");

        }else{

            setTitle("Edit Pet");

            //Initialize a loader to read the pet data from the database
            //and display the current values in the editor
            getLoaderManager().initLoader(EXISTING_PET_LOADER, null, this);
        }

        //Find all the relavant views we will need to read user input
        mNameEditText = (EditText)findViewById(R.id.edit_pet_name);
        mBreedEditText = (EditText)findViewById(R.id.edit_pet_breed);
        mWeightEditText = (EditText)findViewById(R.id.edit_pet_weight);

        mGenderSpinner = (Spinner)findViewById(R.id.spinner_gender);

        //SetUp OnTouchListeners on all the input fields, so we can determineif the
        //user has touched or modified them. This will let us know of there
        //are unsaved changes
        //or not, if the user tries to leave the editor without saving

        mNameEditText.setOnTouchListener(mTouchListener);
        mBreedEditText.setOnTouchListener(mTouchListener);
        mWeightEditText.setOnTouchListener(mTouchListener);
        mGenderSpinner.setOnTouchListener(mTouchListener);


        setupSpinner();

    }

    private void setupSpinner(){

        //Create adapter for spinner . The list options are from the String
        //array it'll use
        //The Spinner will use the default layout
        ArrayAdapter genderSpinnerAdapter = ArrayAdapter.createFromResource(this,
                R.array.array_gender_options,android.R.layout.simple_spinner_item);

        //Specify the dropdown layout style -simple list view with 1 item per line
        genderSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);

        //Apply the adapter for the spinner
        mGenderSpinner.setAdapter(genderSpinnerAdapter);


        //Set the integer mSelected to the constant values
        mGenderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                String selection = (String)parent.getItemAtPosition(position);

                //TextUtils is a bunch od utility function for string objects
                //same as string.indexOf(char c);
                if(!TextUtils.isEmpty(selection)){

                    if(selection.equals(getString(R.string.gender_male))){

                        mGender = PetContract.PetEntry.GENDER_MALE;
                    }else
                    if(selection.equals(getString(R.string.gender_female))){

                        mGender = PetContract.PetEntry.GENDER_FEMALE;
                    }else{

                        mGender = PetContract.PetEntry.GENDER_UNKNOWN;
                    }
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

                mGender = PetContract.PetEntry.GENDER_UNKNOWN;

            }

        });
    }

    /**
     * Get user input from editor and save new pet into database.
     */
    private void savePet() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String nameString = mNameEditText.getText().toString().trim();
        String breedString = mBreedEditText.getText().toString().trim();
        String weightString = mWeightEditText.getText().toString().trim();

        //check if this is supposed to be a new pet
        //and check if all the fields in the editor are blank
        if(mCurrentPetUri==null && TextUtils.isEmpty(nameString) && TextUtils.isEmpty(breedString) &&
                TextUtils.isEmpty(weightString) && mGender== PetContract.PetEntry.GENDER_UNKNOWN){

            //SInce no fields were modified, we can return early without creating a new pet
            //No need to create ContentValues and no need to do any
            //ContentProvider operations
            return;
        }

        // Create a ContentValues object where column names are the keys,
        // and pet attributes from the editor are the values.
        ContentValues values = new ContentValues();
        values.put(PetContract.PetEntry.COLUMN_PET_NAME, nameString);
        values.put(PetContract.PetEntry.COLUMN_PET_BREED, breedString);
        values.put(PetContract.PetEntry.COLUMN_PET_GENDER, mGender);

        //If weight is not provided by the user, don'ttry to parse the string into an
        //integer value. Use 0 by default
        int weight = 0;
        if(!TextUtils.isEmpty(weightString)){

            weight = Integer.parseInt(weightString);

        }

        values.put(PetContract.PetEntry.COLUMN_PET_WEIGHT, weight);


        if(mCurrentPetUri==null){

            //This is a NEW pet, so insert a new pet into the provider,
            //returning the context Uri for the new pet
            // Insert a new pet into the provider, returning the content URI for the new pet.
            Uri newUri = getContentResolver().insert(PetContract.PetEntry.CONTENT_URI, values);

            // Show a toast message depending on whether or not the insertion was successful
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText(this, getString(R.string.editor_insert_pet_failed),
                        Toast.LENGTH_SHORT).show();
            } else {

                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_insert_pet_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }else{

            //Otherwise this is an EXISTING pet, so update the pet with content Uri: mCurrentPetUri
            //and pass in the new ContentValues. Pass in null for the selection and selection args
            //because mCurrentUri will already identify the correct row in the database that
            //we want to modify

            int rowsAffected = getContentResolver().update(mCurrentPetUri, values, null, null);

            //Show a toast message depenmding on whether or not the update was successfull
            if(rowsAffected==0){
                //If no rows were affected then there was an error with the update

                Toast.makeText(this,getString(R.string.editor_insert_pet_failed),Toast.LENGTH_SHORT).show();

            }else{

                //otherwise, the update was successfull and we can display a toast

                Toast.makeText(this,getString(R.string.editor_insert_pet_successful),Toast.LENGTH_SHORT).show();

            }
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_editor, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save pet to database
                savePet();
                // Exit activity
                //This is library defined
                finish();
                return true;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:

                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();

                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:


                //If the pet hasn't changed, continue with navigating upto
                //parent activity
                //whgich is the CatalogActivity
                if(!mPetHasChanged){

                    NavUtils.navigateUpFromSameTask(EditorActivity.this);
                    return true;

                }

                //Otherwise if there are unsaved changes, setup a dialog to warn the user
                //Create a click listener to handle the user confirming that
                //changes should be discarded
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener(){


                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {

                                //User clicked "Discard" button, navigate to parent activity
                                NavUtils.navigateUpFromSameTask(EditorActivity.this);
                            }
                        };

                //Show a Dialog that notifies the user thay have unsaved the changes

                showUnsavedChangesDialog(discardButtonClickListener);

//                // Navigate back to parent activity (CatalogActivity)
//                NavUtils.navigateUpFromSameTask(this);

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // This method is called when the back button is pressed

    @Override
    public void onBackPressed(){

        //If the pet hasn't changed, continue with handling back button
        //press
        if(!mPetHasChanged){

            super.onBackPressed();
            return;
        }

        //Otherwise if there are unsaved changes, setup a dialog to warn
        //the user
        //Create a click listener to handle the user confirming that changes
        //should be discarded

        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener(){

            @Override
                    public void onClick(DialogInterface dialogInterface, int i){

                //User clicked "Discard button", close the current activity
                finish();
            }

        };

        //Show dialog that there are unsaved changes
        showUnsavedChangesDialog(discardButtonClickListener);

    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        //SInce the editor shows all pet attributes, define a projection that
        //contains all columns from the pet table
        String[] projection = {

                PetContract.PetEntry._ID,
                PetContract.PetEntry.COLUMN_PET_NAME,
                PetContract.PetEntry.COLUMN_PET_BREED,
                PetContract.PetEntry.COLUMN_PET_GENDER,
                PetContract.PetEntry.COLUMN_PET_WEIGHT
        };

        //This loader will execute the contentProvider's query methos on a background
        //thread
        return new CursorLoader(this, //Parent activity context
                mCurrentPetUri, //Query the context URI for the current pet
                projection, //columns to include in the resulting cursor

                null, //No selection clause
                null, //No selection arguement
                null //default sort order
        );

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

        //Bail early is the cursor is null or there is less than 1 row in the cursor
        if(cursor==null || cursor.getCount()<1){

            return;
        }

        //Proceed with moving in the first row of the cursor and reding data from it
        //This should be the only row in the cursor
        if(cursor.moveToFirst()){

            //Find the column of pet attributes that we are interested in
            int nameColumnIndex = cursor.getColumnIndex(PetContract.PetEntry.COLUMN_PET_NAME);

            int breedColumnIndex = cursor.getColumnIndex(PetContract.PetEntry.COLUMN_PET_BREED);

            int genderColumnIndex = cursor.getColumnIndex(PetContract.PetEntry.COLUMN_PET_GENDER);

            int weightColumnIndex = cursor.getColumnIndex(PetContract.PetEntry.COLUMN_PET_WEIGHT);

            //Extract out the value from the cursor from the given colunmn index

            String name = cursor.getString(nameColumnIndex);
            String breed = cursor.getString(breedColumnIndex);
            int gender = cursor.getInt(genderColumnIndex);
            int weight = cursor.getInt(weightColumnIndex);

            //Now update the views on the screen with the cvalues from the data base
            //into one of the dropdwn options
            //Then call setSelection() so that option is displyed on the screen as the current selection

            switch(gender){

                case PetContract.PetEntry.GENDER_MALE:

                    mGenderSpinner.setSelection(1);
                    break;
                case PetContract.PetEntry.GENDER_FEMALE:

                    mGenderSpinner.setSelection(2);
                    break;
                default:

                    mGenderSpinner.setSelection(0);
                    break;
            }

        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        // If the loader is invalidated, clear out all the data from the input fields

        mNameEditText.setText("");
        mBreedEditText.setText("");
        mWeightEditText.setText("");
        mGenderSpinner.setSelection(0);//Select Unknown gender

    }

    /**
     * Prompt the user to confirm that they want to delete this pet.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the pet.
                deletePet();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the pet.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the pet in the database.
     */
    private void deletePet() {
        // Only perform the delete if this is an existing pet.

        if (mCurrentPetUri != null) {
            // Call the ContentResolver to delete the pet at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentPetUri
            // content URI already identifies the pet that we want.
            int rowsDeleted = getContentResolver().delete(mCurrentPetUri, null, null);

            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText(this, getString(R.string.editor_delete_pet_failed),
                        Toast.LENGTH_SHORT).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText(this, getString(R.string.editor_delete_pet_successful),
                        Toast.LENGTH_SHORT).show();
            }
        }

        // Close the activity
        finish();
    }

    //Show a dialog taht warns the user there are unsaved changes that will
    //be lost
    //if they continue leaving the editor
    //the discardButtonClickListener is the click listener for what to do
    ///when the user confirms they want to discard their changes

    private void showUnsavedChangesDialog(DialogInterface.OnClickListener discardButtonClickListener){

        //create an AlertDialog.Builder and set the meassage, and click listeners
        //for the positive and negative buttons on the dialog

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.unsaved_changes_dialog_msg);
        builder.setPositiveButton(R.string.discard, discardButtonClickListener);

        builder.setNegativeButton(R.string.keep_editing, new DialogInterface.OnClickListener(){

            public void onClick(DialogInterface dialog, int id){

                //User clicked the "keep editing" butoon, so dismiss the dialog
                //and continue editing the pet
                if(dialog!=null){

                    dialog.dismiss();
                }
            }
        });

        //Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

}
