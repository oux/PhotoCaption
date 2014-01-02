package com.oux.photocaption;

import android.support.v4.view.PagerAdapter;
import android.provider.MediaStore;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.view.WindowManager;
import android.view.Display;
import android.graphics.Point;
import android.graphics.Matrix;
import android.preference.PreferenceManager;

import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.exif.ExifTag;
import com.android.gallery3d.exif.IfdId;
import uk.co.senab.photoview.PhotoView;
import android.webkit.MimeTypeMap;

class PhotoCaptionPagerAdapter extends PagerAdapter {

    private PhotoCaptionView mContext;
    private int layoutResourceId;
    private Cursor externalCursor;
    private Uri externalContentUri;
    private int externalColumnIndex;
    static final String TAG = "photoCaptionPagerAdapter";
    private Uri mImageUriForced;
    Point mSize;

    public void setContext(PhotoCaptionView context) {
        mContext = context;

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        mSize = new Point();
        display.getSize(mSize);

        //Do the query
        externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        String[] projection = {MediaStore.Images.Media._ID};
        String selection = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("jpeg");
        String [] selectionArgs = new String[]{ mimeType };
        externalCursor = mContext.getContentResolver().query(
                externalContentUri,projection,
                selection,selectionArgs,
                MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC, "
                + MediaStore.Images.ImageColumns._ID + " DESC");
        externalColumnIndex = externalCursor.getColumnIndex(MediaStore.Images.Media._ID);
    }

    public void forceUri(Uri imageUri)
    {
        mImageUriForced = imageUri;
        notifyDataSetChanged();
    }

    public Uri getUri(int position) {
        externalCursor.moveToPosition(position);
        int imageID = externalCursor.getInt( externalColumnIndex );
        return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,Integer.toString(imageID));
    }

    public int getPosition(long id) {
        try {
            externalCursor.moveToFirst();
            while (externalCursor.getInt(externalColumnIndex) != id)
            {
                externalCursor.moveToNext();
            }
            return externalCursor.getPosition();
        } catch (Exception e) {
            return -1;
        }
    }

    public int getPosition(String filePath) {
        String[] projection = {MediaStore.Images.Media._ID};
        String selection = MediaStore.Images.ImageColumns.DATA + " LIKE ?";
        String [] selectionArgs = {filePath};
        Cursor cursor = mContext.getContentResolver().query(
                externalContentUri,projection,
                selection,selectionArgs,
                null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(projection[0]);
        try {
            long Id = cursor.getLong(columnIndex);

            Log.d(TAG,"Photo ID is " + Id);
            cursor.close();
            return getPosition(Id);
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public int getCount() {
        if (mImageUriForced == null)
            return externalCursor.getCount();
        else
            return 1;
    }

    @Override
    public View instantiateItem(ViewGroup container, int position) {
        Log.i(TAG,"VIEW:" + position + ", container:" + container
                + ", context:" + container.getContext() + ", mContext:" + mContext);
        LayoutInflater inflater = (LayoutInflater) container.getContext()
            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.view, null);
        PhotoView photoView = (PhotoView) view.findViewById(R.id.ImageView);
        TextView descriptionView = (TextView) view.findViewById(R.id.Description);

        Bitmap preview_bitmap = null;

        Uri imageUri = null;
        if (mImageUriForced == null)
        {
            externalCursor.moveToPosition(position);
            int imageID = externalCursor.getInt( externalColumnIndex );
            Log.d(TAG,"Id:" + imageID);
            imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,Integer.toString(imageID));
        }
        else
        {
            imageUri = mImageUriForced;
        }
        Log.d(TAG,"imageUri:" + imageUri);

        String description = getDescription(imageUri);
        try {
            String image;
            if (imageUri.getScheme().equals("content"))
            {
                Log.i(TAG,"Content");
                image = getRealPathFromURI(imageUri);
            }
            else
                image = imageUri.getPath();

            int orientation = getOrientation(imageUri);

            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inJustDecodeBounds=true;
            BitmapFactory.decodeFile(image ,options);

            int h=(int) Math.ceil(options.outHeight/(float)mSize.y);
            int w=(int) Math.ceil(options.outWidth/(float)mSize.x);

            if(h>1 || w>1){
                if(h>w){
                    options.inSampleSize=h;

                }else{
                    options.inSampleSize=w;
                }
            }
            options.inJustDecodeBounds=false;

            preview_bitmap=BitmapFactory.decodeFile(image ,options);


            if (orientation > 1) {
                Matrix matrix = new Matrix();
                matrix.postRotate(orientation);

                preview_bitmap = Bitmap.createBitmap(preview_bitmap, 0, 0, preview_bitmap.getWidth(),
                        preview_bitmap.getHeight(), matrix, true);
            }

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

    public String getDescription(Uri imageUri)
    {
        ExifInterface exifInterface = new ExifInterface();
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
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);
                if (sharedPrefs.getBoolean("pref_view_binary_info_signalisation", false))
                    return "";
                else
                    return "<BINARY DATA>";
            }
            return description;
        }
        return "";
    }

    public int getOrientation(Uri photoUri) {
        /* it's on the external media. */
        Cursor cursor = mContext.getContentResolver().query(photoUri,
                new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);

        if (cursor.getCount() != 1) {
            return -1;
        }

        cursor.moveToFirst();
        return cursor.getInt(0);
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
