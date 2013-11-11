package com.oux.photocaption;

import java.util.ArrayList;
import android.app.Activity;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.GridView;
import android.widget.AdapterView.OnItemClickListener;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;
import android.util.Log;
import android.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.content.Intent;
 
/**
 *
 * @author javatechig {@link http://javatechig.com}
 * @author sebastien michel
 *
 */
public class PhotoCaptionGallery extends Activity {
    static final String TAG = "photoCaptionGallery";
    private GridView gridView;
    private GridViewAdapter customGridAdapter;
    ActionBar actionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery);

        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.app_name);
        actionBar.setSubtitle(R.string.mode_gallery);

        gridView = (GridView) findViewById(R.id.gridView);
        customGridAdapter = new GridViewAdapter(this, R.layout.row_grid, getData());
        gridView.setAdapter(customGridAdapter);
        gridView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                int position, long id) {
                Log.i(TAG, "Click on position=" + position + ", id=" + id);
                Toast.makeText(PhotoCaptionGallery.this, position + "#Selected",
                    Toast.LENGTH_SHORT).show();
            }

        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.gallery_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.action_capture:
                intent = new Intent(this,PhotoCaptionEdit.class);
                startActivity(intent);
                finish();
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private ArrayList getData() {
        final ArrayList imageItems = new ArrayList();
        // retrieve String drawable array
        TypedArray imgs = getResources().obtainTypedArray(R.array.image_ids);
        for (int i = 0; i < imgs.length(); i++) {
            Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(),
                    imgs.getResourceId(i, -1));
            imageItems.add(new ImageItem(bitmap, " jgfdsgjnds;fg dfgnl gnfdsg nlnd gfk lsng lfdlsngklfndsg,nfdsngfsgfsjngjkflsng j fnjgsn fjds ngjfdsjglfdnsjg lnjImage#" + i));
        }

        return imageItems;

    }

}
