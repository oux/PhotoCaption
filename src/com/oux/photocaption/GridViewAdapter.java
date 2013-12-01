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

        // Make a first query
        notifyDataSetChanged();
    }


    public Uri getUri(int position) {
        // externalCursor.moveToPosition(position);
        externalCursor.moveToPosition(getCount()-(position+1));
        int imageID = externalCursor.getInt( externalColumnIndex );
        return  Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,Integer.toString(imageID));
    }

    @Override
    public void notifyDataSetChanged() {
        // Make again the query to take into account adds and deletes
        externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        // String[] projection = {MediaStore.Images.Media._ID,MediaStore.MediaColumns.DATE_ADDED};
        String[] projection = {MediaStore.Images.Media._ID};
        String selection = "";
        String [] selectionArgs = null;
        // externalCursor = MediaStore.Images.Media.query(mContext.getContentResolver(),
        externalCursor = mContext.getContentResolver().query(
            externalContentUri,projection,selection,selectionArgs,null);
            // externalContentUri,projection,selection,selectionArgs,MediaStore.MediaColumns.DATE_ADDED+" asc");
        externalColumnIndex = externalCursor.getColumnIndex(MediaStore.Images.Media._ID);

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
            holder.imageTitle = (TextView) row.findViewById(R.id.caption_preview);
            holder.image = (ImageView) row.findViewById(R.id.image);
            row.setTag(holder);
        } else {
            holder = (ViewHolder) row.getTag();
        }
		// mLoader.DisplayImage(position, holder);
		mLoader.DisplayImage(getCount()-(position+1), holder);
        return row;
    }

    public int getCount() {
        return externalCursor.getCount();
    }
}

