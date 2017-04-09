/*
 * Copyright (C) 2017 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.GridLayoutManager.SpanSizeLookup;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ChooseIconActivity extends Activity {

    private String mCurrentPackageLabel;
    private String mCurrentPackageName;
    private String mIconPackPackageName;

    private GridLayoutManager mGridLayout;
    private ProgressBar mProgressBar;
    private RecyclerView mIconsGrid;
    private IconCache mIconCache;
    private IconsHandler mIconsHandler;

    private static ItemInfo sItemInfo;

    private float mIconMargin;
    private int mIconSize;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.all_icons_view);

        mCurrentPackageName = getIntent().getStringExtra("app_package");
        mCurrentPackageLabel = getIntent().getStringExtra("app_label");
        mIconPackPackageName = getIntent().getStringExtra("icon_pack_package");

        mIconCache = LauncherAppState.getInstance().getIconCache();
        mIconsHandler = IconCache.getIconsHandler(this);

        int itemSpacing = getResources().getDimensionPixelSize(R.dimen.grid_item_spacing);
        mIconsGrid = (RecyclerView) findViewById(R.id.icons_grid);
        mIconsGrid.setHasFixedSize(true);
        mIconsGrid.addItemDecoration(new GridItemSpacer(itemSpacing));

        mProgressBar = (ProgressBar) findViewById(R.id.icons_grid_progress);

        mIconSize = getResources().getDimensionPixelSize(R.dimen.icon_pack_icon_size);
        mIconMargin = getResources().getDimensionPixelSize(R.dimen.icon_margin);

        new IconLoader().execute();
    }

    public static void setItemInfo(ItemInfo info) {
        sItemInfo = info;
    }

    private class IconLoader extends AsyncTask<Void, Void, Void> {

        private List<String> mAllDrawables;
        private List<String> mMatchingDrawables;
        private GridAdapter mAllIconsGridAdapter;

        @Override
        protected Void doInBackground(Void... voids) {
            mAllDrawables = mIconsHandler.getAllDrawables(mIconPackPackageName);
            mMatchingDrawables = mIconsHandler.getMatchingDrawables(mCurrentPackageName);
            cleanList(mAllDrawables, mMatchingDrawables);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Activity activity = ChooseIconActivity.this;
            mGridLayout = new GridLayoutManager(activity, 4);
            mAllIconsGridAdapter = new GridAdapter(mAllDrawables, mMatchingDrawables);
            mIconsGrid.setLayoutManager(mGridLayout);
            mIconsGrid.setAlpha(0.0f);
            mIconsGrid.animate().alpha(1.0f);
            mIconsGrid.setAdapter(mAllIconsGridAdapter);
            mProgressBar.setVisibility(View.GONE);
            super.onPostExecute(aVoid);
        }

        private void cleanList(List<String>... lists) {
            for (List<String> list : lists) {
                for (Iterator<String> it = list.iterator(); it.hasNext();) {
                    String drawable = it.next();
                    int iconId = 0;
                    try {
                        iconId = mIconsHandler.getIdentifier(drawable);
                    } catch (OutOfMemoryError e) {
                        // time for a new device ?
                    }
                    if (iconId == 0) {
                        it.remove();
                    }
                }
            }
        }
    }

    private class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> {

        private static final int TYPE_MATCHING_HEADER = 0;
        private static final int TYPE_MATCHING_ICONS = 1;
        private static final int TYPE_ALL_HEADER = 2;
        private static final int TYPE_ALL_ICONS = 3;

        private List<String> mAllDrawables = new ArrayList();
        private List<String> mMatchingDrawables = new ArrayList();

        private GridAdapter(List<String> allDrawables, List<String> matchingDrawables) {
            mAllDrawables.add(null);
            mAllDrawables.addAll(allDrawables);
            mMatchingDrawables.add(null);
            mMatchingDrawables.addAll(matchingDrawables);
            mGridLayout.setSpanSizeLookup(mSpanSizeLookup);
        }

        @Override
        public int getItemViewType(int position) {
            if (position < mMatchingDrawables.size() && mMatchingDrawables.get(position) == null) {
                return TYPE_MATCHING_HEADER;
            }

            if (position > TYPE_MATCHING_HEADER && position < mMatchingDrawables.size()) {
                return TYPE_MATCHING_ICONS;
            }

            if (position == mMatchingDrawables.size()) {
                return TYPE_ALL_HEADER;
            }
            return TYPE_ALL_ICONS;
        }

        @Override
        public int getItemCount() {
            return mAllDrawables.size() + 1;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Activity activity = ChooseIconActivity.this;
            if (viewType == TYPE_MATCHING_HEADER) {
                TextView text = (TextView) activity.getLayoutInflater().inflate(
                        R.layout.all_icons_view_header, null);
                text.setText(R.string.similar_icons);
                return new ViewHolder(text);
            }
            if (viewType == TYPE_ALL_HEADER) {
                TextView text = (TextView) activity.getLayoutInflater().inflate(
                        R.layout.all_icons_view_header, null);
                text.setText(R.string.all_icons);
                return new ViewHolder(text);
            }
            ImageView view = new ImageView(activity);
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                    RecyclerView.LayoutParams.MATCH_PARENT, mIconSize);
            view.setLayoutParams(params);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            if (holder.getItemViewType() != TYPE_MATCHING_HEADER
                    && holder.getItemViewType() != TYPE_ALL_HEADER) {
                boolean drawablesMatching = holder.getItemViewType() == TYPE_MATCHING_ICONS;
                List<String> drawables = drawablesMatching ? mMatchingDrawables : mAllDrawables;
                if (position >= drawables.size()) {
                    return;
                }
                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Drawable icon = mIconsHandler.loadDrawable(drawables.get(position));
                        if (icon != null) {
                            mIconCache.addCustomInfoToDataBase(icon, sItemInfo, mCurrentPackageLabel);
                        }
                        ChooseIconActivity.this.finish();
                    }
                });
                Drawable icon = null;
                String drawable = drawables.get(position);
                try {
                    icon = mIconsHandler.loadDrawable(drawable);
                } catch (OutOfMemoryError e) {
                    // time for a new device?
                    e.printStackTrace();
                }
                if (icon != null) {
                    ((ImageView)holder.itemView).setImageDrawable(icon);
                }
            }
        }
        private final SpanSizeLookup mSpanSizeLookup = new SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return getItemViewType(position) == TYPE_MATCHING_HEADER
                        || getItemViewType(position) == TYPE_ALL_HEADER ?
                        4 : 1;
            }
        };

        private class ViewHolder extends RecyclerView.ViewHolder {
            private ViewHolder(View v) {
                super(v);
            }
        }
    }

    private class GridItemSpacer extends RecyclerView.ItemDecoration {
        private int spacing;

        private GridItemSpacer(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                 RecyclerView.State state) {
            outRect.top = spacing;
        }
    }
}
