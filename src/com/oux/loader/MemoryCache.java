package com.oux.loader;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

public class MemoryCache {

    private static final String TAG = "photoCaptionMemoryCache";
    private Map<Integer, Pair<Bitmap,String>> cache=Collections.synchronizedMap(
            new LinkedHashMap<Integer, Pair<Bitmap,String>>(10,1.5f,true));//Last argument true for LRU ordering
    private long size=0;//current allocated size
    private long limit=1000000;//max memory in bytes

    public MemoryCache(){
        //use 25% of available heap size
        // TODO: tweak values
        setLimit(Runtime.getRuntime().maxMemory()/4);
    }
    
    public void setLimit(long new_limit){
        limit=new_limit;
        // Log.i(TAG, "MemoryCache will use up to "+limit/1024./1024.+"MB");
    }

    public Pair<Bitmap,String> get(int id){
        try{
            if(!cache.containsKey(id))
                return null;
            //NullPointerException sometimes happen here http://code.google.com/p/osmdroid/issues/detail?id=78 
            return cache.get(id);
        }catch(NullPointerException ex){
            ex.printStackTrace();
            return null;
        }
    }

    public void put(int id, Pair<Bitmap,String> item){
        try{
            if(cache.containsKey(id))
                size-=getSizeInBytes(cache.get(id));
            cache.put(id, item);
            size+=getSizeInBytes(item);
            checkSize();
        }catch(Throwable th){
            th.printStackTrace();
        }
    }
    
    private void checkSize() {
        // Log.i(TAG, "cache size="+size+" length="+cache.size());
        if(size>limit){
            //least recently accessed item will be the first one iterated  
            Iterator<Entry<Integer, Pair<Bitmap,String>>> iter=cache.entrySet().iterator();
            while(iter.hasNext()){
                Entry<Integer, Pair<Bitmap,String>> entry=iter.next();
                size-=getSizeInBytes(entry.getValue());
                iter.remove();
                if(size<=limit)
                    break;
            }
            // Log.i(TAG, "Clean cache. New size "+cache.size());
        }
    }

    public void clear() {
        try{
            //NullPointerException sometimes happen here http://code.google.com/p/osmdroid/issues/detail?id=78 
            cache.clear();
            size=0;
        }catch(NullPointerException ex){
            ex.printStackTrace();
        }
    }

    long getSizeInBytes(Pair<Bitmap,String> item) {
        if(item == null)
            return 0;
        if(item.first == null)
            return 0;
        long size = item.first.getRowBytes() * item.first.getHeight();
        if (item.second != null)
            size += item.second.length();
        return size;
    }
}
