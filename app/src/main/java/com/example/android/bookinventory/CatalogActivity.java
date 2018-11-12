package com.example.android.bookinventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.android.bookinventory.data.BookContract.BookEntry;

/**
 * Displays list of books that were entered and stored in the app.
 */
public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int BOOK_LOADER = 0;

    // The adapter for the list view
    private BookCursorAdapter cursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = findViewById( R.id.fab );
        fab.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
                startActivity(intent);
            }
        });

        // Find the ListView which will be populated with the book data
        ListView bookListView = findViewById( R.id.list );

        // Find and set empty view on the ListView, so that it only shows when the list has 0 items.
        View emptyView = findViewById( R.id.empty_view );
        bookListView.setEmptyView( emptyView );

        // Setup an adapter to create a list item for each row of book data in the Cursor
        cursorAdapter = new BookCursorAdapter( this, null );
        bookListView.setAdapter( cursorAdapter );

        // Setup an item click listener
        bookListView.setOnItemClickListener( new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // Create a new intent to go to {@link EditorActivity}
                Intent intent = new Intent( CatalogActivity.this, EditorActivity.class );

                // Form the content URI that represents the specific book that was clicked on, by
                // appending the "id" (passed as input to this method) onto the
                // {@link BookEntry#CONTENT_URI}.
                Uri currentBookUri = ContentUris.withAppendedId( BookEntry.CONTENT_URI, id );

                // Set the URI on the data field of the intent.
                intent.setData( currentBookUri );

                // Launch the {@link EditorActivity} to display the data for the current book.
                startActivity( intent );
            }
        } );

        // Start the loader
        getLoaderManager().initLoader( BOOK_LOADER, null, this );
    }

    /**
     * Helper method to insert hardcoded book data into the database.  For debugging purposes only.
     */
    private void insertBook() {

        // Create a ContextValues object where column names are the keys, and the book's attributes
        // are the values.
        ContentValues values = new ContentValues();
        values.put( BookEntry.COLUMN_BOOK_TITLE, "You are a Badass." );
        values.put( BookEntry.COLUMN_BOOK_PRICE, 12.95 );
        values.put( BookEntry.COLUMN_QUANTITY, 5 );
        values.put( BookEntry.COLUMN_SUPPLIER_NAME, "ABC Bookstore" );
        values.put( BookEntry.COLUMN_SUPPLIER_PHONE, "3525551234" );

        // Insert a new row for the book in the provider using the ContentResolver.  Use the {@link
        // BookEntry#CONTENT_URI} to indicate that we want to insert into the books database table.
        getContentResolver().insert( BookEntry.CONTENT_URI, values );
    }

    /**
     * Helper method to delete all pets in the database.
     */
    private void deleteAllBooks() {
        getContentResolver().delete( BookEntry.CONTENT_URI, null, null );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_insert_dummy_data:
                insertBook();
                return true;
            // Respond to a click on the "Delete all entries" menu option
            case R.id.action_delete_all_entries:
                deleteAllBooks();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int i, @Nullable Bundle bundle) {
        // Define a projection that specifies the columns from the table that we care about.
        String[] projection = {
                BookEntry._ID,
                BookEntry.COLUMN_BOOK_TITLE,
                BookEntry.COLUMN_BOOK_PRICE,
                BookEntry.COLUMN_QUANTITY};

        //This loader will execute the ContentProvider's query method on a background thread
        return new CursorLoader( this,
                BookEntry.CONTENT_URI,
                projection,
                null,
                null,
                null );
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        // Update {@link BookCursorAdapter} with this new cursor containing book data
        cursorAdapter.swapCursor( cursor );

    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        // Used when the data needs to be deleted
        cursorAdapter.swapCursor( null );
    }
}

