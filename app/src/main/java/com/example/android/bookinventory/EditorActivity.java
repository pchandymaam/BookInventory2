package com.example.android.bookinventory;

import android.app.AlertDialog;
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
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.android.bookinventory.data.BookContract.BookEntry;
import com.example.android.bookinventory.data.BookDbHelper;

/**
 * Allows user to create a new book or edit an existing one.
 */
public class EditorActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    /**
     * Identifier for the book data loader
     */
    private static final int EXISTING_BOOK_LOADER = 0;
    /**
     * The minimum quantity of book inventory accepted
     */
    private final int MIN_QUANTITY = 0;
    /**
     * The maximum quantity of bok inventory accepted
     */
    private final int MAX_QUANTITY = 100;
    public BookDbHelper dbHelper;
    /**
     * Button used for reducing the quantity.
     */
    Button reduceQuantityButton;
    /**
     * Button used for increasing the quantity
     */
    Button increaseQuantityButton;
    /**
     * Supplier contact phone number. Used when calling the supplier
     */
    private String supplierPhone;
    /**
     * Content URI for the existing book (null if it's a new book)
     */
    private Uri currentBookUri;
    /**
     * EditText field to enter the book's title
     */
    private EditText titleEditText;
    /**
     * EditText field to enter the book's price
     */
    private EditText priceEditText;
    /**
     * EditText field to enter the book's quantity
     */
    private EditText quantityEditText;
    /**
     * EditText field to enter the Supplier's name
     */
    private EditText supplierNameEditText;
    /**
     * EditText field to enter the Supplier's phone
     */
    private EditText supplierPhoneEditText;
    /**
     * Boolean flag that keeps track of whether the book has been edited (true) or not (false)
     */
    private boolean bookHasChanged = false;
    // OnTouchListener that listens for any user touches on a View, implying that they are modifying
    // the view, and we change the bookHasChanged boolean to true.
    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            bookHasChanged = true;
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_editor );

        // Examine the intent that was used to launch this activity, in order to figure out if we're
        // creating a new book or editing an existing one.
        Intent intent = getIntent();
        currentBookUri = intent.getData();

        // If the intent DOES NOT contain a book URI, then we know that we are adding a new book.
        if (currentBookUri == null) {
            // This is a new book, so change the app bar to say "Add a Book"
            setTitle( getString( R.string.editor_activity_title_new_book ) );
            // Invalidate the options menu, so the "Delete" option is hidden.
            invalidateOptionsMenu();
        } else {
            // Otherwise this is an existing book, so change app bar to say "Edit Book"
            setTitle( getString( R.string.editor_activity_title_edit_book ) );

            // Initialize a loader to read the book data from the database and display the current
            // values in the editor.
            getLoaderManager().initLoader( EXISTING_BOOK_LOADER, null, this );
        }

        // Find all relevant views that we will need to read user input from
        titleEditText = findViewById( R.id.edit_book_title );
        priceEditText = findViewById( R.id.edit_book_price );
        quantityEditText = findViewById( R.id.edit_book_quantity );
        supplierNameEditText = findViewById( R.id.edit_book_supplier_name );
        supplierPhoneEditText = findViewById( R.id.edit_book_supplier_phone );
        reduceQuantityButton = findViewById( R.id.reduce_quantity );
        increaseQuantityButton = findViewById( R.id.increase_quantity );

        // OnClickListener for the reduce quantity button
        reduceQuantityButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentQuantityString = quantityEditText.getText().toString();
                int currentQuantityInt;
                if (currentQuantityString.length() == 0) {
                    currentQuantityInt = 0;
                    quantityEditText.setText( String.valueOf( currentQuantityInt ) );
                } else {
                    currentQuantityInt = Integer.parseInt( currentQuantityString ) - 1;
                    if (currentQuantityInt >= MIN_QUANTITY) {
                        quantityEditText.setText( String.valueOf( currentQuantityInt ) );
                    }
                }
            }
        } );

        //onClickListener for the increase quantity button
        increaseQuantityButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentQuantityString = quantityEditText.getText().toString();
                int currentQuantityInt;
                if (currentQuantityString.length() == 0) {
                    currentQuantityInt = 1;
                    quantityEditText.setText( String.valueOf( currentQuantityInt ) );
                } else {
                    currentQuantityInt = Integer.parseInt( currentQuantityString ) + 1;
                    if (currentQuantityInt <= MAX_QUANTITY) {
                        quantityEditText.setText( String.valueOf( currentQuantityInt ) );
                    }
                }
            }
        } );

        dbHelper = new BookDbHelper( this );

        // Setup OnTouchListeners on all the input fields, so that we can determine if the user has
        // touched or modified them.  This will let us know if there are unsaved changes or not, if
        // he user tries to leave the editor without saving.
        titleEditText.setOnTouchListener( mTouchListener );
        priceEditText.setOnTouchListener( mTouchListener );
        quantityEditText.setOnTouchListener( mTouchListener );
        reduceQuantityButton.setOnTouchListener( mTouchListener );
        increaseQuantityButton.setOnTouchListener( mTouchListener );
        supplierNameEditText.setOnTouchListener( mTouchListener );
        supplierPhoneEditText.setOnTouchListener( mTouchListener );
    }

    /**
     * Get user input from editor and save new book into database.
     */
    private void saveBook() {
        // Read from input fields
        // Use trim to eliminate leading or trailing white space
        String titleString = titleEditText.getText().toString().trim();
        String priceString = priceEditText.getText().toString().trim();
        String quantityString = quantityEditText.getText().toString().trim();
        String supplierNameString = supplierNameEditText.getText().toString().trim();
        String supplierPhoneString = supplierPhoneEditText.getText().toString().trim();

        // Ensures that the user has entered a book Title
        if (TextUtils.isEmpty( titleString )) {
            titleEditText.setError( "Book Title is required" );
            return;
        }

        // Ensures that the user has entered a book Price
        if (TextUtils.isEmpty( priceString )) {
            priceEditText.setError( "Book price is required" );
            return;
        }

        // Ensures that the user has entered a quantity (not entering a quantity is not allowed by
        // the interface, and this is included to catch future version that may allow a user to
        // enter data manually)
        if (TextUtils.isEmpty( quantityString )) {
            quantityEditText.setError( "Book quantity is required" );
            return;
        }

        // Ensures that a user has entered a supplier name
        if (TextUtils.isEmpty( supplierNameString )) {
            supplierNameEditText.setError( "Supplier name is required" );
            return;
        }

        // Ensures that a user has entered a supplier phone
        if (TextUtils.isEmpty( supplierPhoneString )) {
            supplierPhoneEditText.setError( "Supplier phone is required" );
            return;
        }

        // Ensures that the user has not entered a negative price
        Double bookPrice = Double.parseDouble( priceString );
        if (bookPrice < 0) {
            priceEditText.setError( "Price cannot be negative" );
            return;
        }

        // Ensures that the user has not entered a negative quantity.  (entering a nagative quantity
        // is not allowed by the interface, and this is included to catch future version that may
        // allow a user to enter data manually)
        int bookQuantity = Integer.parseInt( quantityString );
        if (bookQuantity < 0) {
            quantityEditText.setError( "Quantity cannot be negative" );
            return;
        }

        //Create a ContentValues object where column names are the keys, and book attributes from
        // the editor are the values.
        ContentValues values = new ContentValues();
        values.put( BookEntry.COLUMN_BOOK_TITLE, titleString );
        values.put( BookEntry.COLUMN_BOOK_PRICE, bookPrice );
        values.put( BookEntry.COLUMN_QUANTITY, bookQuantity );
        values.put( BookEntry.COLUMN_SUPPLIER_NAME, supplierNameString );
        values.put( BookEntry.COLUMN_SUPPLIER_PHONE, supplierPhoneString );

        // Determine if this is a new or existing book by checking if mCurrentBookUri is null or not
        if (currentBookUri == null) {
            // This is a NEW book, so insert a new book into the provider,
            // returning the content URI for the new book.
            Uri newUri = getContentResolver().insert( BookEntry.CONTENT_URI, values );

            // Show a toast message depending on whether or not the insertion was successful.
            if (newUri == null) {
                // If the new content URI is null, then there was an error with insertion.
                Toast.makeText( this, getString( R.string.editor_insert_book_failed ),
                        Toast.LENGTH_SHORT ).show();
            } else {
                // Otherwise, the insertion was successful and we can display a toast.
                Toast.makeText( this, getString( R.string.editor_insert_book_successful ),
                        Toast.LENGTH_SHORT ).show();
            }
        } else {
            // Otherwise this is an EXISTING book, so update the book with content URI: mCurrentBookUri
            // and pass in the new ContentValues. Pass in null for the selection and selection args
            // because mCurrentBookUri will already identify the correct row in the database that
            // we want to modify.
            int rowsAffected = getContentResolver().update( currentBookUri, values, null, null );

            // Show a toast message depending on whether or not the update was successful.
            if (rowsAffected == 0) {
                // If no rows were affected, then there was an error with the update.
                Toast.makeText( this, getString( R.string.editor_update_book_failed ),
                        Toast.LENGTH_SHORT ).show();
            } else {
                // Otherwise, the update was successful and we can display a toast.
                Toast.makeText( this, getString( R.string.editor_update_book_successful ),
                        Toast.LENGTH_SHORT ).show();
            }
        }
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_editor.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate( R.menu.menu_editor, menu );
        return true;
    }

    /**
     * This method is called after invalidateOptionsMenu(), so that the
     * menu can be updated (some menu items can be hidden or made visible).
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu( menu );
        // If this is a new pet, hide the "Delete" menu item.
        if (currentBookUri == null) {
            MenuItem menuItem;
            menuItem = menu.findItem( R.id.action_delete );
            menuItem.setVisible( false );
            menuItem = menu.findItem( R.id.action_call_supplier );
            menuItem.setVisible( false );
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Save" menu option
            case R.id.action_save:
                // Save book to database
                saveBook();
                return true;
            //  Respond to a click on the "Call Supplier" menu option
            case R.id.action_call_supplier:
                callSupplier();
                break;
            // Respond to a click on the "Delete" menu option
            case R.id.action_delete:
                // Pop up confirmation dialog for deletion
                showDeleteConfirmationDialog();
                return true;
            // Respond to a click on the "Up" arrow button in the app bar
            case android.R.id.home:
                // If the book hasn't changed, continue with navigating up to parent activity
                // which is the {@link CatalogActivity}.
                if (!bookHasChanged) {
                    NavUtils.navigateUpFromSameTask( EditorActivity.this );
                    return true;
                }
                // Otherwise if there are unsaved changes, setup a dialog to warn the user.
                // Create a click listener to handle the user confirming that
                // changes should be discarded.
                DialogInterface.OnClickListener discardButtonClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // User clicked "Discard" button, navigate to parent activity.
                                NavUtils.navigateUpFromSameTask( EditorActivity.this );
                            }
                        };
                // Show a dialog that notifies the user they have unsaved changes
                showUnsavedChangesDialog( discardButtonClickListener );
                return true;
        }
        return super.onOptionsItemSelected( item );
    }

    /**
     * This method is called when the back button is pressed.
     */
    @Override
    public void onBackPressed() {
        // If the book hasn't changed, continue with handling back button press
        if (!bookHasChanged) {
            super.onBackPressed();
            return;
        }

        // Otherwise if there are unsaved changes, setup a dialog to warn the user.
        // Create a click listener to handle the user confirming that changes should be discarded.
        DialogInterface.OnClickListener discardButtonClickListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // User clicked "Discard" button, close the current activity.
                        finish();
                    }
                };
        // Show dialog that there are unsaved changes
        showUnsavedChangesDialog( discardButtonClickListener );
    }

    /**
     * Instantiate and return a new Loader for the given ID.
     *
     * @param i      The ID whose loader is to be created.
     * @param bundle Any arguments supplied by the caller.
     * @return Return a new Loader instance that is ready to start loading.
     */
    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // Since the editor shows all book attributes, define a projection that contains all columns
        // from the book table.
        String[] projection = {
                BookEntry._ID,
                BookEntry.COLUMN_BOOK_TITLE,
                BookEntry.COLUMN_BOOK_PRICE,
                BookEntry.COLUMN_QUANTITY,
                BookEntry.COLUMN_SUPPLIER_NAME,
                BookEntry.COLUMN_SUPPLIER_PHONE};

        // This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader( this,
                currentBookUri,
                projection,
                null,
                null,
                null );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Proceed with moving to the first row of the cursor and reading data from it
        // (This should be the only row in the cursor)
        if (cursor.moveToFirst()) {
            // Find the columns of book attributes that we're interested in
            int titleColumnIndex = cursor.getColumnIndex( BookEntry.COLUMN_BOOK_TITLE );
            int priceColumnIndex = cursor.getColumnIndex( BookEntry.COLUMN_BOOK_PRICE );
            int quantityColumnIndex = cursor.getColumnIndex( BookEntry.COLUMN_QUANTITY );
            int supplierNameColumnIndex = cursor.getColumnIndex( BookEntry.COLUMN_SUPPLIER_NAME );
            int supplierPhoneColumnIndex = cursor.getColumnIndex( BookEntry.COLUMN_SUPPLIER_PHONE );

            // Extract out the value from the Cursor for the given column index
            String title = cursor.getString( titleColumnIndex );
            Double price = cursor.getDouble( priceColumnIndex );
            int quantity = cursor.getInt( quantityColumnIndex );
            String supplierName = cursor.getString( supplierNameColumnIndex );
            supplierPhone = cursor.getString( supplierPhoneColumnIndex );

            // Update the views on the screen with the values from the database
            titleEditText.setText( title );
            priceEditText.setText( String.valueOf( price ) );
            quantityEditText.setText( String.valueOf( quantity ) );
            supplierNameEditText.setText( supplierName );
            supplierPhoneEditText.setText( supplierPhone );

        }
    }

    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // If the loader is invalidated, clear out all the data from the input fields.
        titleEditText.setText( "" );
        priceEditText.setText( String.valueOf( "" ) );
        quantityEditText.setText( String.valueOf( "" ) );
        supplierNameEditText.setText( "" );
        supplierPhoneEditText.setText( "" );
    }

    /**
     * Dialog box asking the user to discard changes or keep editing
     */
    private void showUnsavedChangesDialog(
            DialogInterface.OnClickListener discardButtonClickListener) {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( R.string.unsaved_changes_dialog_msg );
        builder.setPositiveButton( R.string.discard, discardButtonClickListener );
        builder.setNegativeButton( R.string.keep_editing, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Keep editing" button, so dismiss the dialog
                // and continue editing the book.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        } );

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Prompt the user to confirm that they want to delete this book.
     */
    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the postivie and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder( this );
        builder.setMessage( R.string.delete_dialog_msg );
        builder.setPositiveButton( R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the book.
                deleteBook();
            }
        } );

        builder.setNegativeButton( R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the book.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        } );

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Perform the deletion of the book in the database.
     */
    private void deleteBook() {
        // Only perform the delete if this is an existing book.
        if (currentBookUri != null) {
            // Call the ContentResolver to delete the book at the given content URI.
            // Pass in null for the selection and selection args because the mCurrentBookUri
            // content URI already identifies the book that we want.
            int rowsDeleted = getContentResolver().delete( currentBookUri, null, null );
            // Show a toast message depending on whether or not the delete was successful.
            if (rowsDeleted == 0) {
                // If no rows were deleted, then there was an error with the delete.
                Toast.makeText( this, getString( R.string.editor_delete_book_failed ),
                        Toast.LENGTH_SHORT ).show();
            } else {
                // Otherwise, the delete was successful and we can display a toast.
                Toast.makeText( this, getString( R.string.editor_delete_book_successful ),
                        Toast.LENGTH_SHORT ).show();
            }
        }
        // Close the activity
        finish();
    }

    /**
     * This method is used to call the supplier.
     */
    private void callSupplier() {
        Intent callSupplierIntent = new Intent( Intent.ACTION_DIAL );
        callSupplierIntent.setData( Uri.parse( "tel:" + supplierPhone ) );
        startActivity( callSupplierIntent );
    }
}
