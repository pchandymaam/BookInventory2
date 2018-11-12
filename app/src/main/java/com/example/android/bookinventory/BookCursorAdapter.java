package com.example.android.bookinventory;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.bookinventory.data.BookContract;
import com.example.android.bookinventory.data.BookContract.BookEntry;

public class BookCursorAdapter extends CursorAdapter {

    /**
     * Constructs a new {@link BookCursorAdapter}.
     *
     * @param context The context
     * @param c       The cursor from which to get the data.
     */
    public BookCursorAdapter(Context context, Cursor c) {
        super( context, c, 0 );
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already
     *                moved to the correct position.
     * @param parent  The parent to which the new view is attached to
     * @return the newly created list item view.
     */
    //  The newView method is used to inflate a new view and return it, you don't bind any data to
    // the view at this point.
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from( context ).inflate( R.layout.list_item, parent, false );
    }

    /**
     * This method binds the book data (in the current row pointed to by cursor) to the given
     * list item layout.
     *
     * @param view    Existing view, returned earlier by newView() method
     * @param context app context
     * @param cursor  The cursor from which to get the data. The cursor is already moved to the
     *                correct row.
     */
    // The bindView method is used to bind all data to a given view, such as setting the text on a
    // TextView.
    @Override
    public void bindView(View view, final Context context, Cursor cursor) {
        // Find individual views that we want to modify in the list item layout.
        // Find fields to populate in inflated template
        TextView titleTextView = view.findViewById( R.id.book_title );
        TextView priceTextView = view.findViewById( R.id.price );
        final TextView quantityTextView = view.findViewById( R.id.quantity );

        //Find the columns of books attributes that we're interested in
        int titleColumnIndex = cursor.getColumnIndex( BookEntry.COLUMN_BOOK_TITLE );
        int priceColumnIndex = cursor.getColumnIndex( BookEntry.COLUMN_BOOK_PRICE );
        int quantityColumnIndex = cursor.getColumnIndex( BookEntry.COLUMN_QUANTITY );

        // Find the columns of book attributes that we're interested in.
        // Extract properties from cursor
        String bookTitle = cursor.getString( titleColumnIndex );
        String bookPrice = String.valueOf( cursor.getDouble( priceColumnIndex ) );
        String bookQuantity = String.valueOf( cursor.getInt( quantityColumnIndex ) );

        // Update the TextViews with the attributes for the current book
        // Populate fields with extracted properties
        titleTextView.setText( bookTitle );
        priceTextView.setText( bookPrice );
        quantityTextView.setText( bookQuantity );

        // column number of "_ID"
        int productIdColumnIndex = cursor.getColumnIndex( BookEntry._ID );

        // Read the book attributes from the Cursor for the current book for "Sale" button
        final long productIdVal = Integer.parseInt( cursor.getString( productIdColumnIndex ) );
        final int currentProductQuantityVal = cursor.getInt( quantityColumnIndex );

        // When the "Buy" button is clicked on, the quantity will be reduced, but not below 0.
        Button buyButton = view.findViewById( R.id.buy_button );
        buyButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Uri currentUri = ContentUris.withAppendedId( BookEntry.CONTENT_URI, productIdVal );

                String updatedQuantity = String.valueOf( currentProductQuantityVal - 1 );

                if (Integer.parseInt( updatedQuantity ) >= 0) {
                    ContentValues values = new ContentValues();
                    values.put( BookEntry.COLUMN_QUANTITY, updatedQuantity );
                    context.getContentResolver().update( currentUri, values, null, null );
                }
            }
        } );
    }
}
