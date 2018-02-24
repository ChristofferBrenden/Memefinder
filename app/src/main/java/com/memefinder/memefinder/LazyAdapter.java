package com.memefinder.memefinder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.ArrayList;

/**
 * Custom adapter for a RecyclerView, used to load images into imageviews and show them in the RecyclerView
 */
public class LazyAdapter extends RecyclerView.Adapter<LazyAdapter.ImageViewHolder> {
    private static LayoutInflater inflater = null;
    private ArrayList<Image> data;
    private Context context;
    private int position;

    LazyAdapter(Context c, ArrayList<Image> d) {
        context = c;
        data = d;
        inflater = LayoutInflater.from(context);
    }

    /**
     * Creates the ImageViewHolder which holds the image and description for each of our images
     *
     * @param viewGroup The viewgroup
     * @param i         i
     * @return An ImageViewHolder containing an image and its corresponding description to be shown in the app
     */
    @Override
    public ImageViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View view = inflater.inflate(R.layout.row_listview_item, viewGroup, false);
        LinearLayout layout = view.findViewById(R.id.image_row);

        //Set the height to 1200 to produce a cleaner look in the app
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1200);
        layout.setLayoutParams(params);
        return new ImageViewHolder(view);
    }

    /**
     * Loads an image into an ImageViewHolder which is used to display the image and its description in the app
     *
     * @param viewHolder The ImageViewHolder to be used
     * @param i          The index in the "data" ArrayList which holds the image to be loaded
     */
    @Override
    public void onBindViewHolder(final ImageViewHolder viewHolder, int i) {
        Image img = data.get(i);

        Picasso.with(context)
                .load(img.getUrl())
                .error(R.drawable.ic_action_failed)
                .into(viewHolder.image);

        viewHolder.description.setText(img.getTitle());

        //Set listener for longclick, this sets the position of the clicked item
        //which is then used in the {@MainActivity.onCreateContextMenu} function
        //to get the position of the related item
        viewHolder.itemView.setOnLongClickListener(new View.OnLongClickListener() {

            @Override
            public boolean onLongClick(View v) {
                setPosition(viewHolder.getLayoutPosition());
                return false;
            }
        });
    }

    /**
     * Restores the item after its been swiped away when swiping right on an item.
     * <p>
     * Also calls the "onSwipeRight" function which handles the showing of the menu and selection etc.
     *
     * @param viewHolder The item that was longpressed upon
     */
    void onItemRemove(final RecyclerView.ViewHolder viewHolder) {
        final int position = viewHolder.getAdapterPosition();
        notifyItemChanged(position); //Notify changed to restore it to view
        onSwipeRight(position);
    }

    /**
     * Holds an image and its description and also creates its listener for the context menu
     * This is used when showing the images in the app
     */
    static class ImageViewHolder extends RecyclerView.ViewHolder
            implements View.OnCreateContextMenuListener {

        ImageView image;
        TextView description;

        ImageViewHolder(View view) {
            super(view);
            this.image = view.findViewById(R.id.image_row_image);
            this.description = view.findViewById(R.id.image_row_title);
            view.setOnCreateContextMenuListener(this);
        }

        @Override
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        }
    }

    /**
     * On swipe left to right, show an alert asking if you want to save the image
     *
     * @param pos Position of the image that was swiped on in the list
     */
    private void onSwipeRight(int pos) {
        showAlert("Save Image?", "Would you like to save this image to your device?", pos);
    }

    /**
     * Shows an alert, if the user clicks save, it saves the selected image to the device
     *
     * @param title   The title of the alert
     * @param message The message for the alert
     * @param pos     The position of the selected image in the list
     */
    private void showAlert(String title, String message, final int pos) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
                .setTitle(title)
                .setNegativeButton("Save", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        final ImageView saveimgvw = new ImageView(context);

                        Target savetarget = new Target() {

                            @Override
                            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                                saveimgvw.setImageBitmap(bitmap);
                            }

                            @Override
                            public void onBitmapFailed(Drawable errorDrawable) {

                            }

                            @Override
                            public void onPrepareLoad(Drawable placeHolderDrawable) {

                            }
                        };

                        Picasso.with(context).load(data.get(pos).getUrl()).into(savetarget);
                        Bitmap savebmp = ((BitmapDrawable) saveimgvw.getDrawable()).getBitmap();

                        boolean added = MainActivity.insertImage(context, savebmp);

                        if (added)
                            Toast.makeText(context, "Image saved!", Toast.LENGTH_SHORT).show();
                        else
                            Toast.makeText(context, "Image could not be saved!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setPositiveButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Returns the position of an item
     *
     * @return The position of an item
     */
    int getPosition() {
        return position;
    }

    /**
     * Sets the position of an item
     *
     * @param position The position to be set to an item
     */
    private void setPosition(int position) {
        this.position = position;
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public void onViewRecycled(ImageViewHolder holder) {
        holder.itemView.setOnLongClickListener(null);
        super.onViewRecycled(holder);
    }
}