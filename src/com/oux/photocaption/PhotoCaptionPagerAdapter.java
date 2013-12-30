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
import android.webkit.MimeTypeMap;

class PhotoCaptionPagerAdapter extends PagerAdapter {

    private PhotoCaptionView mContext;
    private int layoutResourceId;
    private Cursor externalCursor;
    private Uri externalContentUri;
    private int externalColumnIndex;
    static final String TAG = "photoCaptionPagerAdapter";
    private Uri mImageUriForced;

    public void setContext(PhotoCaptionView context) {
        mContext = context;
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
    }

    public Uri getUri(int position) {
        externalCursor.moveToPosition(position);
        // externalCursor.moveToPosition(getCount()-(position+1));
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
        // TODO This will break if we have no matching item in the MediaStore.
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

        Uri imageUri = null;
        if (mImageUriForced == null)
        {
            externalCursor.moveToPosition(position);
            // externalCursor.moveToPosition(getCount()-(position+1));
            int imageID = externalCursor.getInt( externalColumnIndex );
            Log.d(TAG,"Id:" + imageID);
            imageUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,Integer.toString(imageID));
        }
        else
        {
            imageUri = mImageUriForced;
        }

        Log.d(TAG,"imageUri:" + imageUri);
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
