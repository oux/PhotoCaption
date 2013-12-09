package com.oux.photocaption;

import android.support.v4.view.PagerAdapter;
import android.provider.MediaStore;
import android.content.Context;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.Charset;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.LayoutInflater;
import android.view.View;
import android.util.Log;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.TextView;
import android.widget.ImageView;

import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.exif.ExifTag;
import com.android.gallery3d.exif.IfdId;
import uk.co.senab.photoview.PhotoView;

class PhotoCaptionPagerAdapter extends PagerAdapter {

    private PhotoCaptionView mContext;
    private int layoutResourceId;
    private Cursor externalCursor;
    private Uri externalContentUri;
    private int externalColumnIndex;
    static final String TAG = "photoCaptionPagerAdapter";

    public void setContext(PhotoCaptionView context) {
        mContext = context;
        //Do the query
        externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {MediaStore.Images.Media._ID};
        String selection = "";
        String [] selectionArgs = null;
        externalCursor = mContext.getContentResolver().query(
                externalContentUri,projection,
                selection,selectionArgs,MediaStore.Images.Media._ID+" desc");
                // selection,selectionArgs,null);
        externalColumnIndex = externalCursor.getColumnIndex(MediaStore.Images.Media._ID);
    }

    public Uri getUri(int position) {
        externalCursor.moveToPosition(position);
        // externalCursor.moveToPosition(getCount()-(position+1));
        int imageID = externalCursor.getInt( externalColumnIndex );
        return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,Integer.toString(imageID));
    }

    public int getPosition(int id) {
        //Do the query
        String[] projection = {MediaStore.Images.Media._ID};
        String selection = "_ID = ?";
        String [] selectionArgs = {String.valueOf(id)};
        externalCursor.moveToFirst();
        while (externalCursor.getInt(externalColumnIndex) != id)
        {
            externalCursor.moveToNext();
        }
        return externalCursor.getPosition();
        // return getCount() - externalCursor.getPosition() - 1;
    }

    @Override
    public int getCount() {
        return externalCursor.getCount();
    }

    @Override
    public View instantiateItem(ViewGroup container, int position) {
        Log.i(TAG,"VIEW:" + position + ", container:" + container
                + ", context:" + container.getContext() + ", mContext:" + mContext);
        // PhotoViewCaption photoView = new PhotoViewCaption(container.getContext(),null);
        LayoutInflater inflater = (LayoutInflater) container.getContext()
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // imageView = (MyImageView) 
        View view = inflater.inflate(R.layout.view, null);
        PhotoView photoView = (PhotoView) view.findViewById(R.id.ImageView);
        TextView descriptionView = (TextView) view.findViewById(R.id.Description);
        // PhotoViewCaption photoView = new PhotoViewCaption(container.getContext(),descriptionView);

        ExifInterface exifInterface = new ExifInterface();
        Bitmap preview_bitmap = null;

        externalCursor.moveToPosition(position);
        // externalCursor.moveToPosition(getCount()-(position+1));
        int imageID = externalCursor.getInt( externalColumnIndex );
        Uri imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,Integer.toString(imageID));

        Log.i(TAG,"Id:" + imageID + ", imageUri:" + imageUri);
        try {
            exifInterface.readExif(mContext.getContentResolver().openInputStream(imageUri));
        } catch (Exception e) {
            e.printStackTrace();
        }

        ExifTag tag = exifInterface.getTag(ExifInterface.TAG_USER_COMMENT);
        String description = null;
        if (tag != null)
        {
            description = tag.getValueAsString();
            CharsetEncoder encoder =
                Charset.forName("ISO-8859-1").newEncoder();

            if (! encoder.canEncode(description)) {
                description="<BINARY DATA>";
            }
        }
        try {
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inSampleSize = 8;
            if (imageUri.getScheme().equals("content"))
            {
                Log.i(TAG,"Content");
                preview_bitmap=BitmapFactory.decodeFile(getRealPathFromURI(imageUri),options);
            }
            else
                preview_bitmap=BitmapFactory.decodeFile(imageUri.getPath(),options);
            photoView.setImageBitmap(preview_bitmap);
            if (description != "" && description != null && description.length() != 0)
            {
                Log.i(TAG,"setText: <"+description + ">("+description.length()+")");
                descriptionView.setText(description);
            }
            else
            {
                Log.i(TAG,"Hidding");
                descriptionView.setVisibility(View.INVISIBLE);
            }

            // Now just add PhotoView to ViewPager and return it
            container.addView(view, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return view;
    }

    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        String ret = cursor.getString(idx);
        cursor.close();
        return ret;
    }


    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

}
