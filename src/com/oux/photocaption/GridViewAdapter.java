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
import android.media.ExifInterface;
import android.widget.Toast;

/**
 *
 * @author javatechig {@link http://javatechig.com}
 *
 */
public class GridViewAdapter extends ArrayAdapter {
    private Context mContext;
    private int layoutResourceId;
    private ArrayList data = new ArrayList();
    private Cursor externalCursor;
    private Uri externalContentUri;
    private int externalColumnIndex;
    static final String TAG = "photoCaptionGridViewAdapter";

    public GridViewAdapter(Context context, int layoutResourceId,
            ArrayList data) {
        super(context, layoutResourceId, data);
        this.layoutResourceId = layoutResourceId;
        this.mContext = context;
        this.data = data;
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ViewHolder holder = null;
        ExifInterface exif = null;

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
        externalCursor.moveToPosition(getCount()-(position+1));
        int imageID = externalCursor.getInt( externalColumnIndex );
        Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,Integer.toString(imageID));

        Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null); 
        cursor.moveToFirst(); 
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA); 
        try {
            exif = new ExifInterface(cursor.getString(idx));
        } catch (IOException e) {
            e.printStackTrace();
        }
        holder.imageTitle.setText(exif.getAttribute("UserComment"));
        holder.image.setImageBitmap(loadThumbnailImage(uri.toString()));
        return row;
    }

    /*
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ImageView imageView;

        if (convertView == null) {  // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
        } else {
            imageView = (ImageView) convertView;
        }
        externalCursor.moveToPosition(position);
        int imageID = externalCursor.getInt( externalColumnIndex );
        Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,Integer.toString(imageID));
        imageView.setImageBitmap(loadThumbnailImage(uri.toString()));   
        return imageView;
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

        ImageItem item = (ImageItem) data.get(position);
        holder.imageTitle.setText(item.getTitle());
        holder.image.setImageBitmap(item.getImage());
        return row;
    }
    */

    static class ViewHolder {
        TextView imageTitle;
        ImageView image;
    }

    public int getCount() {
        return externalCursor.getCount();
    }

    protected Bitmap loadThumbnailImage( String url ) {
        // Get original image ID
        int originalImageId = Integer.parseInt(url.substring(url.lastIndexOf("/") + 1, url.length()));

        // Get (or create upon demand) the micro thumbnail for the original image.
        return MediaStore.Images.Thumbnails.getThumbnail(mContext.getContentResolver(),
                originalImageId, MediaStore.Images.Thumbnails.MINI_KIND, null);
    }
}

