package com.oux.photocaption;

import java.util.ArrayList;
import java.io.IOException;

import android.util.Log;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.widget.Toast;
import com.oux.loader.ImageLoader;
import com.oux.loader.ViewHolder;

/**
 *
 * @author javatechig {@link http://javatechig.com}
 *
 */
public class GridViewAdapter extends ArrayAdapter {
    private Context mContext;
    private int layoutResourceId;
    private Cursor externalCursor;
    private Uri externalContentUri;
    private int externalColumnIndex;
    static final String TAG = "photoCaptionGridViewAdapter";
    private ImageLoader mLoader;

    public GridViewAdapter(Context context, int layoutResourceId) {
        super(context, layoutResourceId);
        this.layoutResourceId = layoutResourceId;
        this.mContext = context;
		mLoader = new ImageLoader(context);
        //Do the query
        externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;        

        String[] projection = {MediaStore.Images.Media._ID}; 
        String selection = "";
        String [] selectionArgs = null;
        externalCursor = mContext.getContentResolver().query(
            externalContentUri,projection,selection,selectionArgs,null); 
        externalColumnIndex = externalCursor.getColumnIndex(MediaStore.Images.Media._ID);
    }


    public Uri getUri(int position) {
        externalCursor.moveToPosition(getCount()-(position+1));
        int imageID = externalCursor.getInt( externalColumnIndex );
        return  Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,Integer.toString(imageID));
    }

    public void clearCache() {
        mLoader.clearCache();
    }

    @Override
    public void notifyDataSetChanged() {
        mLoader.clearCache();
        super.notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewHolder holder = null;

        if (row == null) {
            LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new ViewHolder();
            holder.imageTitle = (TextView) row.findViewById(R.id.text);
            holder.image = (ImageView) row.findViewById(R.id.image);
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }
		mLoader.DisplayImage(getCount()-(position+1), holder);
		// mLoader.DisplayImage(getItem(position), holder);

        return row;
    }

    public int getCount() {
        return externalCursor.getCount();
    }
}

