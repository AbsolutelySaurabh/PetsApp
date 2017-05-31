package com.example.absolutelysaurabh.petcare.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * Created by absolutelysaurabh on 23/4/17.
 */
//A URI matcher matches the URI with the integer code and ensures that the contentprovider
//doesn't try to handle unexpected contentURIs like the contents://blag/blah


public class PetProvider extends ContentProvider {

    private static final int PETS = 100;
    private static final int PET_ID = 101;

    /**
     * Tag for the log messages
     */
    public static final String LOG_TAG = PetProvider.class.getSimpleName();

    private PetDbHelper mDbHelper;

    //The sUriMatcher's "s" denotes that it is a static variable
    private static UriMatcher sUriMathcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {

        sUriMathcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS, PETS);

        //# pound is the wild card which will be replaced by id
        sUriMathcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS + "/#", PET_ID);
    }

    @Override
    public boolean onCreate() {

        //getContext() returns the view of the current Activity
        //Using "this" will cause error cz we have used context in constructor of PetDbHelper
        mDbHelper = new PetDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        Cursor cursor;

        int match = sUriMathcher.match(uri);
        switch (match) {

            case PETS:

                cursor = database.query(PetContract.PetEntry.TABLE_NAME, projection
                        , selection, selectionArgs, null, null, sortOrder);
                break;

            case PET_ID:
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                cursor = database.query(PetContract.PetEntry.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);

                break;

            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);

        }
        //Set notifications URI on the cursor,
        //so we know what content UR the cursor was created for.
        //If the data at thus URI changes, then we know a need to upadate the cursor
        cursor.setNotificationUri(getContext().getContentResolver(),uri);

        //return the cursor
        return cursor;
    }


    @Override
    public String getType(Uri uri) {
        final int match = sUriMathcher.match(uri);
        switch (match) {
            case PETS:
                return PetContract.PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetContract.PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    //CatalogActvity  -> ContentResolver -> ContentProvider -> SQLiteDatabase
    //Translating into SQLiteDatabse in last helps to decide what should be inserted and where in the db
    //It'll return the Uri telling us eactly where the pet has been inserted
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {

        //The Uri tells where to inserted ie which table eg.
        //The contentValues tells what to be inserted

        final int match = sUriMathcher.match(uri);
        switch (match) {

            //No PETS_ID here as it doesn't make sense to insert a new pet in a row where a pet already exists
            case PETS:
                return insertPet(uri, contentValues);

            default:
                throw new IllegalArgumentException("Insertion not supported for given uri " + uri);
        }

    }

//    private Uri insertPet(Uri uri , ContentValues contentValues){
//
//        //Once we know the id of the new row created return it;
//        return ContentUris.withAppendedId(uri,id);
//    }

    private Uri insertPet(Uri uri, ContentValues values) {

        //Doing a sanity check to alert if nothing has been entered so to reject such data from inserting into the database
        // Check that the name is not null
        String name = values.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Pet requires a name");
        }

        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Insert the new pet with the given values
        long id = database.insert(PetContract.PetEntry.TABLE_NAME, null, values);
        // If the ID is -1, then the insertion failed. Log an error and return null.
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        //Notift all listeners that the data has changed for the pet content uri
        getContext().getContentResolver().notifyChange(uri,null);

        // Return the new URI with the ID (of the newly inserted row) appended at the end
        //like contents://com.example.absolutelysaurabh.pets/3
        //3 is the row id eg.
        return ContentUris.withAppendedId(uri, id);

    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // Get writeable database
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // Track the number of rows that were deleted
        int rowsDeleted;

        final int match = sUriMathcher.match(uri);
        switch (match) {
            // Delete all rows that match the selection and selection args
            case PETS:

                rowsDeleted = database.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
                break;
            // Delete all rows that match the selection and selection args
            //return database.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
            case PET_ID:

                // Delete a single row given by the ID in the URI
                rowsDeleted = database.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
                break;
            // Delete a single row given by the ID in the URI
//                selection = PetContract.PetEntry._ID + "=?";
//                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
//                return database.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);

            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);

        }

                // If 1 or more rows were deleted, then notify all listeners that the data at the
                // given URI has changed
                if (rowsDeleted != 0) {
                           getContext().getContentResolver().notifyChange(uri, null);

                      }

        // Return the number of rows deleted
        return rowsDeleted;

    }


        @Override
    public int update(Uri uri, ContentValues contentValues, String selection,
                      String[] selectionArgs) {
        final int match = sUriMathcher.match(uri);
        switch (match) {
            case PETS:
                return updatePet(uri, contentValues, selection, selectionArgs);
            case PET_ID:
                // For the PET_ID code, extract out the ID from the URI,
                // so we know which row to update. Selection will be "_id=?" and selection
                // arguments will be a String array containing the actual ID.
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[]{String.valueOf(ContentUris.parseId(uri))};
                return updatePet(uri, contentValues, selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    /**
     * Update pets in the database with the given content values. Apply the changes to the rows
     * specified in the selection and selection arguments (which could be 0 or 1 or more pets).
     * Return the number of rows that were successfully updated.
     */
    private int updatePet(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

        // Otherwise, get writeable database to update the data
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        // TODO: Update the selected pets in the pets database table with the given ContentValues
        // Perform the update on the database and get the number of rows affected

        int rowsUpdated = database.update(PetContract.PetEntry.TABLE_NAME, values, selection, selectionArgs);

        // If 1 or more rows were updated, then notify all listeners that the data at the
        // given URI has changed
        if (rowsUpdated != 0) {
                 getContext().getContentResolver().notifyChange(uri, null);

                }
        // Return the number of rows updated
        return rowsUpdated;
    }
}
