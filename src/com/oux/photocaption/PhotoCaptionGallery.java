package com.oux.photocaption;

import java.util.ArrayList;
import android.app.Activity;
import android.app.ActionBar;
import android.content.res.TypedArray;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
// import com.origamilabs.library.views.StaggeredGridView.OnItemClickListener;
// import com.origamilabs.library.views.StaggeredGridView.OnItemLongClickListener;
import android.widget.Toast;
import android.widget.TextView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.util.Log;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.GridView;
import android.graphics.Point;
import android.graphics.Rect;
import com.origamilabs.library.views.StaggeredGridView;
import android.os.Parcelable;
 
/**
 *
 * @author javatechig {@link http://javatechig.com}
 * @author sebastien michel
 *
 */
public class PhotoCaptionGallery extends Activity implements AdapterView.OnItemClickListener,
       AdapterView.OnItemLongClickListener,
       StaggeredGridView.OnItemClickListener,
       StaggeredGridView.OnItemLongClickListener {
    static final String TAG = "photoCaptionGallery";
    private StaggeredGridView sGridView;
    private GridView gridView;
    private GridViewAdapter customGridAdapter;
    ActionBar actionBar;
    private boolean mEntireCaption;
    Parcelable gridState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: try to navigate by album (NavigationList).
        Log.i(TAG,"onCreate");
        super.onCreate(savedInstanceState);

        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.app_name);
        actionBar.setSubtitle(R.string.mode_gallery);

        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mEntireCaption = sharedPrefs.getBoolean("pref_gallery_whole_caption", false);
        setGridView();
    }

    private void setGridView()
    {
        if (mEntireCaption)
        {
            if (sGridView == null) {
                gridView = null;
                customGridAdapter = null;
                setContentView(R.layout.staggered_gallery);
                customGridAdapter = new GridViewAdapter(this, R.layout.staggered_row_grid);
                sGridView = (StaggeredGridView) this.findViewById(R.id.staggeredGridView);
                if (sGridView != null)
                {
                    int margin = getResources().getDimensionPixelSize(R.dimen.margin);
                    sGridView.setAdapter(customGridAdapter);
                    sGridView.setItemMargin(margin); // set the GridView margin
                    sGridView.setPadding(margin, 0, margin, 0); // have the margin on the sides as well 
                    sGridView.setOnItemClickListener((StaggeredGridView.OnItemClickListener)this);
                    sGridView.setOnItemLongClickListener( (StaggeredGridView.OnItemLongClickListener)this);
                }
            }
        }
        else
        {
            if (gridView == null) {
                sGridView = null;
                customGridAdapter = null;
                setContentView(R.layout.gallery);
                customGridAdapter = new GridViewAdapter(this, R.layout.row_grid);
                gridView = (GridView) this.findViewById(R.id.gridView);
                gridView.setAdapter(customGridAdapter);
                gridView.setOnItemClickListener((OnItemClickListener)this);
                gridView.setOnItemLongClickListener((OnItemLongClickListener)this);
            }
        }
        customGridAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onPause() {
        Log.i(TAG,"onPause");
        if (mEntireCaption)
            gridState = sGridView.onSaveInstanceState();
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.i(TAG,"onResume");
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mEntireCaption = sharedPrefs.getBoolean("pref_gallery_whole_caption", false);
        setGridView();
        customGridAdapter.notifyDataSetChanged();
        if (mEntireCaption)
            if (gridState != null) sGridView.onRestoreInstanceState(gridState);
        super.onResume();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        Log.i(TAG, "onItemClick:" + v + " pos=" + position + " id=" + id);
        Intent intent = new Intent(this,PhotoCaptionView.class);
        intent.putExtra("position",position);
        startActivity(intent);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
        Log.i(TAG, "onItemLongClick:" + v + " pos=" + position + " id=" + id);
        Intent intent = new Intent(Intent.ACTION_EDIT,
                customGridAdapter.getUri(position), this,PhotoCaptionEdit.class);
        startActivity(intent);
        return true;
    }

    @Override
    public void onItemClick(StaggeredGridView parent, View v, int position, long id) {
        Log.i(TAG, "onItemClick:" + v + " pos=" + position + " id=" + id);
        Intent intent = new Intent(this,PhotoCaptionView.class);
        intent.putExtra("position",position);
        startActivity(intent);
    }

    @Override
    public boolean onItemLongClick(StaggeredGridView parent, View v, int position, long id) {
        Log.i(TAG, "onItemLongClick:" + v + " pos=" + position + " id=" + id);
        Intent intent = new Intent(Intent.ACTION_EDIT,
                customGridAdapter.getUri(position), this,PhotoCaptionEdit.class);
        startActivity(intent);
        return true;
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
            case R.id.action_settings:
                intent = new Intent(this,PhotoCaptionSettings.class);
                startActivity(intent);
                return true;
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
