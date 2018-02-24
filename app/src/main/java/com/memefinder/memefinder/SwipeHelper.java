package com.memefinder.memefinder;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

/**
 * Used to enable right swiping on an item
 */
public class SwipeHelper extends ItemTouchHelper.Callback {

    private final LazyAdapter adapter;

    SwipeHelper(LazyAdapter adapter) {
        this.adapter = adapter;
    }

    /**
     * Checks whether longPressDrag is enable or not
     * <p>
     * This is disabled since we don't need that functionality
     *
     * @return false
     */
    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    /**
     * Checks whether swiping on items is enables or not
     * <p>
     * Always enables since we need that functionality
     *
     * @return true
     */
    @Override
    public boolean isItemViewSwipeEnabled() {
        return true;
    }

    /**
     * Gets the flags for allowed movement when doing longPressDrag or swipe
     * <p>
     * Always set to 0 for longPressDrag since we dont use that
     * Always set to RIGHT for swiping since we only allow that
     *
     * @param recyclerView recyclerView
     * @param viewHolder   viewHolder
     * @return 0 for longPressDrag and RIGHT for swiping
     */
    @Override
    public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0, ItemTouchHelper.RIGHT);
    }

    /**
     * Called when after longPressDrag
     * <p>
     * Always returns false since we dont use longPressDrag
     *
     * @param recyclerView recyclerView
     * @param source       ViewHolder source
     * @param target       ViewHolder target
     * @return false
     */
    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
        return false;
    }

    /**
     * Called after a swipe has been performed
     * <p>
     * Calls "adapter.onItemRemove" to properly restore the item etc.
     *
     * @param viewHolder The swiped upon item
     * @param i          i
     */
    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int i) {
        adapter.onItemRemove(viewHolder);
    }
}