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
// import android.widget.AdapterView.OnItemClickListener;
// import android.widget.AdapterView.OnItemLongClickListener;
import com.origamilabs.library.views.StaggeredGridView.OnItemClickListener;
import com.origamilabs.library.views.StaggeredGridView.OnItemLongClickListener;
import android.widget.Toast;
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
public class PhotoCaptionGallery extends Activity implements OnItemClickListener,OnItemLongClickListener {
    static final String TAG = "photoCaptionGallery";
    private StaggeredGridView gridView;
    // private AutoMeasureGridView gridView;
    private GridViewAdapter customGridAdapter;
    ActionBar actionBar;
    // zoom animation
    private Animator mCurrentAnimator;
    private int mShortAnimationDuration;
    Parcelable gridState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gallery);

        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.app_name);
        actionBar.setSubtitle(R.string.mode_gallery);
        int margin = getResources().getDimensionPixelSize(R.dimen.margin);

		gridView = (StaggeredGridView) this.findViewById(R.id.gridView);
        gridView.setItemMargin(margin); // set the GridView margin
        gridView.setPadding(margin, 0, margin, 0); // have the margin on the sides as well 
        
        //StaggeredAdapter adapter = new StaggeredAdapter(this, R.id.row_grid, getData());
        
        // gridView = (GridView) findViewById(R.id.gridView);
        // gridView = (AutoMeasureGridView) findViewById(R.id.gridView);

        // TODO: try to navigate by album (NavigationList).
        customGridAdapter = new GridViewAdapter(this, R.layout.row_grid, getData());
        gridView.setAdapter(customGridAdapter);
        customGridAdapter.notifyDataSetChanged();

        Log.i(TAG, "Creating setOnItemClickListener");
        gridView.setOnItemClickListener((OnItemClickListener)this);
        gridView.setOnItemLongClickListener((OnItemLongClickListener)this);

        // mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
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

    private ArrayList getData() {
        final ArrayList imageItems = new ArrayList();

        return imageItems;

    }
}
