package com.oux.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.Charset;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.webkit.MimeTypeMap;

import android.R;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Pair;
import com.oux.loader.MemoryCache;
import android.net.Uri;
import android.provider.MediaStore;

import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.exif.ExifTag;
import com.android.gallery3d.exif.IfdId;

/**
 * Using LazyList via https://github.com/thest1/LazyList/tree/master/src/com/fedorvlasov/lazylist
 * for the example since its super lightweight
 * I barely modified this file
 */
public class ImageLoader {

    MemoryCache memoryCache=new MemoryCache();
    private Context mContext;
    private Map<ViewHolder, Integer> imageViews = Collections.synchronizedMap(
            new WeakHashMap<ViewHolder, Integer>());
    ExecutorService executorService;
    Handler handler=new Handler();//handler to display images in UI thread
    private Uri externalContentUri;
    private int externalColumnIndex;
    private int externalColumnDate;
    private Cursor externalCursor;

    public ImageLoader(Context context){
        mContext = context;
        externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        executorService=Executors.newFixedThreadPool(5);
        setCursor();
    }

    public void setCursor() {
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_TAKEN};
        String selection = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("jpeg");
        String [] selectionArgs = new String[]{ mimeType };
        externalCursor = mContext.getContentResolver().query(
            externalContentUri,projection,
            selection,selectionArgs,
            MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC, "
            + MediaStore.Images.ImageColumns._ID + " DESC");
        externalColumnIndex = externalCursor.getColumnIndex(MediaStore.Images.Media._ID);
        externalColumnDate = externalCursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
    }

    public int getImageID(int position) {
        externalCursor.moveToPosition(position);
        return externalCursor.getInt( externalColumnIndex );
    }

    public void DisplayImage(int position, ViewHolder holder)
    {
        imageViews.put(holder, position);
        Pair<Bitmap, String> pair=memoryCache.get(position);
        if(pair!=null)
        {
            holder.image.setImageBitmap(pair.first);
            holder.imageTitle.setText(pair.second);
        }
        else
        {
            queuePhoto(position, holder);
            holder.image.setImageDrawable(null);
        }
    }

    private void queuePhoto(int position, ViewHolder holder)
    {
        PhotoToLoad p=new PhotoToLoad(position, holder);
        executorService.submit(new PhotosLoader(p));
    }

    private Pair<Bitmap, String> getPair(int position)
    {
        ExifInterface exifInterface = new ExifInterface();

        externalCursor.moveToPosition(position);
        int imageID = externalCursor.getInt( externalColumnIndex );
        Uri uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,Integer.toString(imageID));
        Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        try {
            exifInterface.readExif(cursor.getString(idx));
        } catch (Exception e) {
            e.printStackTrace();
        }
        cursor.close();
        ExifTag tag = exifInterface.getTag(ExifInterface.TAG_USER_COMMENT);
        String description = null;
        if (tag != null)
        {
            description = tag.getValueAsString();
            CharsetEncoder encoder =
                Charset.forName("US-ASCII").newEncoder();

            if (! encoder.canEncode(description)) {
                description="<BINARY DATA>";
            }
        }
        return new Pair<Bitmap, String>(loadThumbnailImage(uri.toString()), description);
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

    //Task for the queue
    private class PhotoToLoad
    {
        public int position;
        public ViewHolder holder;
        public PhotoToLoad(int p, ViewHolder i){
            position=p;
            holder=i;
        }
    }

    class PhotosLoader implements Runnable {
        PhotoToLoad photoToLoad;
        PhotosLoader(PhotoToLoad photoToLoad){
            this.photoToLoad=photoToLoad;
        }

        @Override
        public void run() {
            try{
                if(imageViewReused(photoToLoad))
                    return;
                Pair<Bitmap,String> bmp=getPair(photoToLoad.position);
                memoryCache.put(photoToLoad.position, bmp);
                if(imageViewReused(photoToLoad))
                    return;
                BitmapDisplayer bd=new BitmapDisplayer(bmp, photoToLoad);
                handler.post(bd);
            }catch(Throwable th){
                th.printStackTrace();
            }
        }
    }

    boolean imageViewReused(PhotoToLoad photoToLoad){
        Integer tag=imageViews.get(photoToLoad.holder);
        if(tag==null || tag != photoToLoad.position)
            return true;
        return false;
    }

    //Used to display bitmap in the UI thread
    class BitmapDisplayer implements Runnable
    {
        Bitmap bitmap;
        String description;
        PhotoToLoad photoToLoad;
        public BitmapDisplayer(Pair<Bitmap,String> b, PhotoToLoad p){
            bitmap=b.first;description=b.second;photoToLoad=p;}
        public void run()
        {
            if(imageViewReused(photoToLoad))
                return;
            if(bitmap!=null)
            {
                photoToLoad.holder.image.setImageBitmap(bitmap);
                photoToLoad.holder.imageTitle.setText(description);
            }
            else
                photoToLoad.holder.image.setImageDrawable(null);
        }
    }

    public void clearCache() {
        memoryCache.clear();
        externalCursor.close();
        setCursor();
    }

}
