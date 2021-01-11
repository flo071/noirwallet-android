package org.noirofficial.presenter.activities.adapters;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.noirofficial.R;
import org.noirofficial.presenter.activities.BreadActivity;
import org.noirofficial.presenter.entities.VerticalSpaceItemDecoration;
import org.noirofficial.tools.adapter.TransactionListAdapter;
import jp.wasabeef.recyclerview.animators.SlideInDownAnimator;

public class TxAdapter extends PagerAdapter {

    private BreadActivity activity;
    private RecyclerView allRecycler;
    private TransactionListAdapter allAdapter;
    private RecyclerView sentRecycler;
    private TransactionListAdapter sentAdapter;
    private RecyclerView receivedRecycler;
    private TransactionListAdapter receivedAdapter;

    public RecyclerView getAllRecycler() {
        return allRecycler;
    }

    public TransactionListAdapter getAllAdapter() {
        return allAdapter;
    }

    public RecyclerView getSentRecycler() {
        return sentRecycler;
    }

    public TransactionListAdapter getSentAdapter() {
        return sentAdapter;
    }

    public RecyclerView getReceivedRecycler() {
        return receivedRecycler;
    }

    public TransactionListAdapter getReceivedAdapter() {
        return receivedAdapter;
    }

    public TxAdapter(BreadActivity activity) {
        this.activity = activity;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup collection, int position) {
        RecyclerView layout = (RecyclerView) LayoutInflater.from(activity).inflate(
                R.layout.activity_bread_recycler, collection, false);
        SlideInDownAnimator slideInDownAnimator = new SlideInDownAnimator();
        slideInDownAnimator.setAddDuration(500);
        slideInDownAnimator.setChangeDuration(0);
        layout.addItemDecoration(new VerticalSpaceItemDecoration(4));
        layout.setItemAnimator(slideInDownAnimator);
        layout.setLayoutManager(new LinearLayoutManager(activity));
        switch (position) {
            case 0:
                allAdapter = new TransactionListAdapter(activity);
                layout.setAdapter(allAdapter);
                allRecycler = layout;
                break;
            case 1:
                sentAdapter = new TransactionListAdapter(activity);
                layout.setAdapter(sentAdapter);
                sentRecycler = layout;
                break;
            case 2:
                receivedAdapter = new TransactionListAdapter(activity);
                layout.setAdapter(receivedAdapter);
                receivedRecycler = layout;
                break;
        }
        collection.addView(layout);
        return layout;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup collection, int position, Object view) {
        collection.removeView((View) view);
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return object == view;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            default:
            case 0:
                return activity.getString(R.string.all);
            case 1:
                return activity.getString(R.string.sent);
            case 2:
                return activity.getString(R.string.received);
        }
    }
}