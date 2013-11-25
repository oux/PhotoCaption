package com.oux.photocaption;

import java.util.ArrayList;
import android.app.Activity;
import android.app.ActionBar;
import android.content.res.TypedArray;
import android.content.Intent;
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
    private int mIndex;
    private int mTop;
    private StaggeredGridView sGridView;
    private GridView gridView;
    private GridViewAdapter customGridAdapter;
    ActionBar actionBar;
    // zoom animation
    private Animator mCurrentAnimator;
    private int mShortAnimationDuration;
    private boolean mEntireComment;
    Parcelable gridState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG,"onCreate");
        super.onCreate(savedInstanceState);

        mEntireComment = true;

        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.app_name);
        actionBar.setSubtitle(R.string.mode_gallery);
        int margin = getResources().getDimensionPixelSize(R.dimen.margin);

        // TODO: try to navigate by album (NavigationList).

        if (mEntireComment)
        {
            setContentView(R.layout.staggered_gallery);
            customGridAdapter = new GridViewAdapter(this, R.layout.staggered_row_grid);
            sGridView = (StaggeredGridView) this.findViewById(R.id.staggeredGridView);
            if (sGridView != null)
            {
                sGridView.setAdapter(customGridAdapter);
                sGridView.setItemMargin(margin); // set the GridView margin
                sGridView.setPadding(margin, 0, margin, 0); // have the margin on the sides as well 
                sGridView.setOnItemClickListener((StaggeredGridView.OnItemClickListener)this);
                sGridView.setOnItemLongClickListener( (StaggeredGridView.OnItemLongClickListener)this);
            }
        }
        else
        {
            setContentView(R.layout.gallery);
            customGridAdapter = new GridViewAdapter(this, R.layout.row_grid);
            gridView = (GridView) this.findViewById(R.id.gridView);
            gridView.setAdapter(customGridAdapter);
            gridView.setOnItemClickListener((OnItemClickListener)this);
            gridView.setOnItemLongClickListener((OnItemLongClickListener)this);
        }
        customGridAdapter.notifyDataSetChanged();
        // mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void onPause() {
        Log.i(TAG,"onPause");
        if (mEntireComment)
        {
            gridState = sGridView.onSaveInstanceState();
        }
        else
        {
            mIndex = gridView.getFirstVisiblePosition();
        }
        customGridAdapter.clearCache();
        super.onPause();
    }

    @Override
    public void onResume() {
        Log.i(TAG,"onResume");
        customGridAdapter.notifyDataSetChanged();
        if (mEntireComment)
            if (gridState != null) sGridView.onRestoreInstanceState(gridState);
        else
            if (gridView != null) gridView.smoothScrollToPosition(mIndex);
        super.onResume();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
        Log.i(TAG, "onItemClick:" + v + " pos=" + position + " id=" + id);
            mIndex = parent.getFirstVisiblePosition();
            View vv = parent.getChildAt(0);
            mTop = (vv == null) ? 0 : vv.getTop();
        Log.i(TAG, "onItemClick:" + mTop + " pos=" + mIndex );
        Intent intent = new Intent(Intent.ACTION_VIEW,
                customGridAdapter.getUri(position), this,PhotoCaptionView.class);
        // Bundle b = new Bundle();
        // b.putParcelable(getResources().getString(R.string.adapter), gridView);
        // intent.putExtras(b);
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
        Intent intent = new Intent(Intent.ACTION_VIEW,
                customGridAdapter.getUri(position), this,PhotoCaptionView.class);
        // Bundle b = new Bundle();
        // b.putParcelable(getResources().getString(R.string.adapter), gridView);
        // intent.putExtras(b);
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
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
