package com.ammar.sharing.activities.AddAppsAndFilesActivity.adaptersR;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.ammar.sharing.R;
import com.ammar.sharing.activities.AddAppsAndFilesActivity.adaptersR.viewHolders.AppViewHolder;
import com.ammar.sharing.activities.AddAppsAndFilesActivity.adaptersR.viewHolders.SearchBarViewHolder;
import com.ammar.sharing.activities.AddAppsAndFilesActivity.adaptersR.viewHolders.SpaceViewHolder;
import com.ammar.sharing.activities.AddAppsAndFilesActivity.data.AppData;
import com.ammar.sharing.common.utils.Utils;

import java.util.ArrayList;

public class AppsRecyclerAdapter extends SearchableRecyclerAdapter<RecyclerView.ViewHolder> {
    private static final int TYPE_SEARCH_BAR = 0;
    private static final int TYPE_APP = 1;
    private static final int TYPE_SPACE = 2;
    private static final int TYPE_EMPTY = 3;

    private final AppData[] mAppsData;
    private int visibleAppCount;

    // if null all apps are listed
    @Nullable
    private ArrayList<Integer> mListedAppsIndices;
    public AppsRecyclerAdapter(AppData[] appsData) {
        this.mAppsData = appsData;
        visibleAppCount = appsData.length;
    }

    @Override
    public int getItemViewType(int position) {
        if( position == 0 ) {
            return TYPE_SEARCH_BAR;
        } else if (position == getItemCount() - 1){
            return visibleAppCount == 0 ? TYPE_EMPTY : TYPE_SPACE;
        } else {
            return TYPE_APP;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view;
        switch (viewType) {
            case TYPE_SEARCH_BAR:
                view = inflater.inflate(R.layout.view_search, parent, false);
                return new SearchBarViewHolder(view, this);
            case TYPE_APP:
                view = inflater.inflate(R.layout.view_add_apps_app, parent, false);
                return new AppViewHolder(view);
            case TYPE_EMPTY:
                android.widget.TextView empty = new android.widget.TextView(parent.getContext());
                empty.setText(R.string.no_apps_found);
                empty.setTextColor(parent.getResources().getColor(R.color.ui_muted));
                empty.setGravity(android.view.Gravity.CENTER);
                empty.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) Utils.dpToPx(120)));
                com.ammar.sharing.common.utils.AppAppearance.apply(empty);
                return new RecyclerView.ViewHolder(empty) {};
            case TYPE_SPACE:
                return SpaceViewHolder.MakeSpaceViewHolder(parent.getContext(), (int) Utils.dpToPx(140));
        }
        // It's impossible to reach this
        throw new RuntimeException("");
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int positionR) {
        int type = getItemViewType(positionR);
        if (type == TYPE_APP) {

            int position = positionR - 1;
            if( mListedAppsIndices != null ) {
                position = mListedAppsIndices.get(position);
            }

            AppViewHolder appHolder = (AppViewHolder) holder;
            com.ammar.sharing.common.utils.AppIcons.bind(appHolder.getImageView(), mAppsData[position].packageName);
            holder.itemView.findViewById(R.id.TV_AppSplits).setVisibility(mAppsData[position].hasSplits ? View.VISIBLE : View.GONE);
            appHolder.getAppNameTV().setText(mAppsData[position].label);
            appHolder.getCheckBox().setChecked(mAppsData[position].isChecked);

            final int posCopy = position;
            appHolder.itemView.setOnClickListener((view) -> {
                boolean isChecked = appHolder.getCheckBox().isChecked();
                appHolder.getCheckBox().setChecked(!isChecked);
                this.mAppsData[posCopy].isChecked = !isChecked;
            });
        }
    }

    @Override
    public int getItemCount() {
        if( mListedAppsIndices != null ) return mListedAppsIndices.size() + 2;
        else return mAppsData.length + 2;
    }

    private final android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable pendingSearch;

    @Override
    public void searchItems(String text) {
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        final String query = com.ammar.sharing.common.utils.AppSearch.normalize(text);
        pendingSearch = () -> {
            if (query.isEmpty()) mListedAppsIndices = null;
            else {
                mListedAppsIndices = new ArrayList<>();
                for (int i = 0; i < mAppsData.length; i++) if (mAppsData[i].searchKey.contains(query)) mListedAppsIndices.add(i);
            }
            // Leave the search row attached to preserve keyboard, cursor and focus.
            notifyItemRangeRemoved(1, visibleAppCount);
            visibleAppCount = mListedAppsIndices == null ? mAppsData.length : mListedAppsIndices.size();
            notifyItemRangeInserted(1, visibleAppCount);
            notifyItemChanged(getItemCount() - 1);
        };
        searchHandler.postDelayed(pendingSearch, 150);
    }

    @Override public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof AppViewHolder app) com.ammar.sharing.common.utils.AppIcons.clear(app.getImageView());
        super.onViewRecycled(holder);
    }

    @Override public void onDetachedFromRecyclerView(@NonNull RecyclerView recycler) {
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        super.onDetachedFromRecyclerView(recycler);
    }
}
