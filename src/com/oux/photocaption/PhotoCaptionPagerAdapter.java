package com.oux.photocaption;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

import android.support.v4.view.PagerAdapter;
import android.provider.MediaStore;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.Charset;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.LayoutInflater;
import android.view.View;
import android.view.MotionEvent;
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

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;
import uk.co.senab.photoview.DefaultOnDoubleTapListener;
import android.view.View.OnLongClickListener;

import static android.view.MotionEvent.ACTION_UP;

class PhotoCaptionPagerAdapter extends PagerAdapter {

    private static final boolean DEBUG = false;
    private PhotoCaptionView mContext;
    private int layoutResourceId;
    private Cursor externalCursor;
    private Uri externalContentUri;
    private int externalColumnIndex;
    static final String TAG = "photoCaptionPagerAdapter";
    private Uri mImageUriForced;
    Point mSize;
    SharedPreferences mSharedPrefs;

    public void setContext(PhotoCaptionView context) {
        mContext = context;

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        mSize = new Point();
        display.getSize(mSize);
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

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

            if (DEBUG)
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
        if (DEBUG)
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
            if (DEBUG)
                Log.d(TAG,"Id:" + imageID);
            imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,Integer.toString(imageID));
        }
        else
        {
            imageUri = mImageUriForced;
        }
        if (DEBUG)
            Log.d(TAG,"imageUri:" + imageUri);

        String description = getDescription(imageUri);
        try {
            String image;
            if (imageUri.getScheme().equals("content"))
            {
                if (DEBUG)
                    Log.d(TAG,"Content");
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
                if (DEBUG)
                    Log.d(TAG,"setText: <"+description + ">("+description.length()+")");
                descriptionView.setText(description);
            }
            else
            {
                if (DEBUG)
                    Log.d(TAG,"Hidding");
                descriptionView.setVisibility(View.INVISIBLE);
            }
            if (DEBUG)
                Log.d(TAG,"photoView: " + photoView);
            PhotoViewAttacher pvAttacher = photoView.getPhotoViewAttacher();
            PhotoEditListener photoEditListener = new PhotoEditListener(pvAttacher);
            photoEditListener.setUri(imageUri);
            photoView.setOnLongClickListener(photoEditListener);
            pvAttacher.setOnDoubleTapListener(photoEditListener);
            photoView.setOnPhotoTapListener(new PhotoTapListener());

            // Now just add PhotoView to ViewPager and return it
            container.addView(view, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return view;
    }

    private class PhotoEditListener extends DefaultOnDoubleTapListener implements
        OnLongClickListener
        {

        Uri mUri;
        public boolean mDisableEdit;

        public PhotoEditListener(PhotoViewAttacher photoViewAttacher) {
            super(photoViewAttacher);
        }


        public void setUri(Uri uri) {
            mUri = uri;
            if (DEBUG)
                Log.d(TAG,"mUri:" + mUri);
        }

        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            if (e.getAction() == ACTION_UP)
                mDisableEdit = false;
            return true;
        }


        @Override
        public boolean onDoubleTap(MotionEvent ev) {
            mDisableEdit = true;
            super.onDoubleTap(ev);
            return true;
        }

        @Override
        public boolean onLongClick(View view) {
            if (mDisableEdit)
            {
                return false;
            }
            Intent intent = new Intent(mContext,PhotoCaptionEdit.class);
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("image/jpeg");
            intent.putExtra(Intent.EXTRA_STREAM, mUri);
            if (DEBUG)
                Log.d(TAG,"Extra:" + mUri);
            mContext.startActivity(intent);
            mContext.finish();
            return true;
        }

    }

    private class PhotoTapListener implements OnPhotoTapListener {
        @Override
        public void onPhotoTap(View view, float x, float y) {
            mContext.toggleActionBar();
        }
    }

    public String getDescription(Uri imageUri)
    {
        ExifInterface exifInterface = new ExifInterface();
        try {
            exifInterface.readExif(mContext.getContentResolver().openInputStream(imageUri));
        } catch (Exception e) {
            e.printStackTrace();
        }
        String description = "";
        Set<String> set = mSharedPrefs.getStringSet("pref_view_exif_field",
                new HashSet<String>(Arrays.asList(Integer.toString(ExifInterface.TAG_USER_COMMENT))));

        for (String tagId: set)
        {
            ExifTag tag = exifInterface.getTag(Integer.parseInt(tagId));
            if (tag == null)
                continue;

            String desc = tag.getValueAsString();
            CharsetEncoder encoder =
                Charset.forName("ISO-8859-1").newEncoder();

            if (!encoder.canEncode(desc)) {
                if (mSharedPrefs.getBoolean("pref_binary_info_signalisation", false))
                    desc = "";
                else
                    desc = "<BINARY DATA>";
            }

            if (!desc.equals("")
                    && desc.charAt(0) != '\0'
                    && !desc.equals("Jpeg\0")
                    && !desc.equals("Jpeg\0\0\0\0")
                    && !desc.equals("ASCII\0\0\0\0")
               )
                if (description.equals(""))
                    description = desc;
                else
                    description += "\n" + desc;
        }
        return description;
    }

    public int getOrientation(Uri photoUri) {
        /* it's on the external media. */
        Cursor cursor = mContext.getContentResolver().query(photoUri,
                new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);

        if (cursor == null || cursor.getCount() != 1) {
            return -1;
        }

        cursor.moveToFirst();
        int orientation = cursor.getInt(0);
        cursor.close();
        return orientation;
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
