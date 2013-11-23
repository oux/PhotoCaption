package com.oux.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import android.R;
import android.media.ExifInterface;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Pair;
import com.oux.loader.MemoryCache;
import android.net.Uri;
import android.provider.MediaStore;

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
    private Cursor externalCursor;
    private int externalColumnIndex;
    
    public ImageLoader(Context context){
        mContext = context;
        externalContentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;        
        executorService=Executors.newFixedThreadPool(5);
        String[] projection = {MediaStore.Images.Media._ID}; 
        String selection = "";
        String [] selectionArgs = null;
        externalCursor = mContext.getContentResolver().query(
            externalContentUri,projection,selection,selectionArgs,null); 
        externalColumnIndex = externalCursor.getColumnIndex(MediaStore.Images.Media._ID);
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
        ExifInterface exif = null;
        externalCursor.moveToPosition(position);
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
        cursor.close();
        return new Pair<Bitmap, String>(loadThumbnailImage(uri.toString()), exif.getAttribute("UserComment"));
    }

    //decodes image and scales it to reduce memory consumption
    private Bitmap decodeFile(File f){
        try {
            //decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            FileInputStream stream1=new FileInputStream(f);
            BitmapFactory.decodeStream(stream1,null,o);
            stream1.close();
            
            //Find the correct scale value. It should be the power of 2.
            final int REQUIRED_SIZE=70;
            int width_tmp=o.outWidth, height_tmp=o.outHeight;
            int scale=1;
            while(true){
                if(width_tmp/2<REQUIRED_SIZE || height_tmp/2<REQUIRED_SIZE)
                    break;
                width_tmp/=2;
                height_tmp/=2;
                scale*=2;
            }
            
            if(scale>=2){
            	scale/=2;
            }
            
            //decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize=scale;
            FileInputStream stream2=new FileInputStream(f);
            Bitmap bitmap=BitmapFactory.decodeStream(stream2, null, o2);
            stream2.close();
            return bitmap;
        } catch (FileNotFoundException e) {
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
    }

}
