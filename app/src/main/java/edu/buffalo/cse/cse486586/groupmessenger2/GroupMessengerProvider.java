package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {

    private static final String KEY_FIELD = "key";
    private static final String VALUE_FIELD = "value";

    private static ContentResolver mContentResolver;
    private static Context context;

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        String fileName = values.getAsString(KEY_FIELD);
        String valueContent = values.getAsString(VALUE_FIELD);
        try {
            FileOutputStream fos = context.openFileOutput(fileName, context.MODE_PRIVATE);
            fos.write(valueContent.getBytes());
            fos.close();
            Log.v("insert", values.toString());
        }
        catch (FileNotFoundException ex) {
            Log.e("Error FileNot Found Exception ", ex.getMessage());
        }
        catch (IOException ex) {
            Log.e("Error: IO Exception ", ex.getMessage());
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        context = getContext();
        mContentResolver = context.getContentResolver();
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        String[] columns = {KEY_FIELD, VALUE_FIELD};
        MatrixCursor cr = new MatrixCursor(columns);
        Context context = getContext();
        try {
            File fl = new File(selection);
            FileInputStream fis = context.openFileInput(selection);
            StringBuilder sb = new StringBuilder();
            String valueContent = "";
            int val = fis.read();
            while (val != -1) {
                sb.append((char) val);
                val = fis.read();
            }
            valueContent = sb.toString();
            fis.close();
            if (!valueContent.isEmpty()) {
                String[] row = new String[2];
                row[0] = selection;
                row[1] = valueContent;
                cr.addRow(row);
            }
            Log.v("query", selection);
            return cr;
        } catch (FileNotFoundException ex) {
            Log.e("Exception e : ",  ex.getMessage());
            return cr;
        } catch (IOException ex) {
            Log.e("Exception e : ",  ex.getMessage());
            return cr;
        }
    }
}
