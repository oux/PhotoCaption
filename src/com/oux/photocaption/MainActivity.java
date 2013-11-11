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
 
/**
 *
 * @author javatechig {@link http://javatechig.com}
 * @author sebastien michel
 *
 */
public class MainActivity extends Activity {
    private GridView gridView;
    private GridViewAdapter customGridAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gridView = (GridView) findViewById(R.id.gridView);
        customGridAdapter = new GridViewAdapter(this, R.layout.row_grid, getData());
        gridView.setAdapter(customGridAdapter);
        gridView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                int position, long id) {
                Toast.makeText(MainActivity.this, position + "#Selected",
                    Toast.LENGTH_SHORT).show();
            }

        });
    }

    private ArrayList getData() {
        final ArrayList imageItems = new ArrayList();
        // retrieve String drawable array
        TypedArray imgs = getResources().obtainTypedArray(R.array.image_ids);
        for (int i = 0; i < imgs.length(); i++) {
            Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(),
                    imgs.getResourceId(i, -1));
            imageItems.add(new ImageItem(bitmap, "Image#" + i));
        }

        return imageItems;

    }

}
