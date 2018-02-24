package com.memefinder.memefinder;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckedTextView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.ContextMenu;
import android.view.Display;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Main class which does all of the things, such as loading images
 */
@SuppressWarnings({"ConstantConditions", "unchecked", "SameParameterValue", "ResultOfMethodCallIgnored"})
@SuppressLint("ClickableViewAccessibility")
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    //RecyclerView and adapter for it. The RecyclerView is the main view for the program
    private RecyclerView list_images;
    private LazyAdapter adapter;

    //The images to be used in the app
    private ArrayList<Image> images;

    private boolean doubleBackToExitPressedOnce;

    private final static ArrayList<String> quickSettingsItems = new ArrayList<String>() {{
        add("New");
        add("Popular");
        add("Rising");
        add("Top of all time");
        add("Top of the year");
        add("Top of the month");
    }};

    //For the menu and its content
    private DrawerLayout drawer;
    private NavigationView navigationView;

    //Swipe to refresh
    private SwipeRefreshLayout swipeContainer;

    private String quickSettingsClickedOption;

    private int settingsRequestCode = 69;

    private final int REQUEST_PERMISSION_STORAGE = 1;

    private boolean haltRefresh = false;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Initiate all of the views
        Toolbar toolbar = findViewById(R.id.toolbar);
        ImageButton menuIcon = findViewById(R.id.toolbar_quicksettings_button);
        Button quickSettingsSaveButton = findViewById(R.id.quicksettings_savebutton);
        drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        list_images = findViewById(R.id.list_images);
        ListView quickSettingsList = findViewById(R.id.quicksettings_list);
        swipeContainer = findViewById(R.id.swipe_refresh);

        //Setup the toolbar and show what we're currently sorting by
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Exploring Memes");
        getSupportActionBar().setSubtitle("Sorting by: " + getSortingByString());

        //Get images to use, sent from LoadingActivity
        //Set the images to the adapter and then set the adapter to our ListView
        images = (ArrayList<Image>) getIntent().getSerializableExtra("images");
        adapter = new LazyAdapter(this, images);

        //Setup layout manager to enable preloading of images
        PreLoadLayoutManager layoutManager = new PreLoadLayoutManager(getApplicationContext());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        //Get screen height for preloading
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        //The extra space, which basically tells the program that the screen is twice as high
        //meaning that it loads an extra screens worth of images continuously
        layoutManager.setExtraLayoutSpace(size.y);

        list_images.setLayoutManager(layoutManager);
        list_images.setAdapter(adapter);

        //Enable swipe to save
        ItemTouchHelper.Callback callback = new SwipeHelper(adapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(list_images);

        //Initialize options for quicksettings, i.e. our different sorting options
        ArrayAdapter<String> quickSettingsAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice, quickSettingsItems);
        quickSettingsList.setAdapter(quickSettingsAdapter);
        quickSettingsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        quickSettingsList.setSoundEffectsEnabled(false);
        quickSettingsList.setItemChecked(getQuicksettingsItemPositionForActiveSort(), true);

        //register the list for long press which opens our menu for sharing and saving images
        registerForContextMenu(list_images);

        navigationView.setNavigationItemSelectedListener(this);

        toolbar.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                list_images.smoothScrollToPosition(0);
            }
        });

        menuIcon.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (drawer.isDrawerOpen(navigationView)) {
                    drawer.closeDrawer(navigationView);
                } else if (!drawer.isDrawerOpen(navigationView)) {
                    drawer.openDrawer(navigationView);
                }
            }
        });

        quickSettingsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                AppCompatCheckedTextView b = (AppCompatCheckedTextView) view;
                quickSettingsClickedOption = b.getText().toString();
            }
        });

        //when you click save on quicksettings get the values which were selected, update them, write them to file, and then refresh the image-feed
        quickSettingsSaveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                switch (quickSettingsClickedOption) {
                    case "New":
                        Helpers.setSortBy(Helpers.SortByType.NEW);
                        break;
                    case "Popular":
                        Helpers.setSortBy(Helpers.SortByType.HOT);
                        break;
                    case "Rising":
                        Helpers.setSortBy(Helpers.SortByType.RISING);
                        break;
                    case "Top of all time":
                        Helpers.setSortBy(Helpers.SortByType.TOP);
                        Helpers.setSortByTopValue(Helpers.SortByTopValues.All);
                        break;
                    case "Top of the year":
                        Helpers.setSortBy(Helpers.SortByType.TOP);
                        Helpers.setSortByTopValue(Helpers.SortByTopValues.YEAR);
                        break;
                    case "Top of the month":
                        Helpers.setSortBy(Helpers.SortByType.TOP);
                        Helpers.setSortByTopValue(Helpers.SortByTopValues.MONTH);
                        break;
                    default:

                }

                ArrayList<String> temp = new ArrayList<String>() {{
                    add(Helpers.getSortBy().name());
                }};

                Helpers.writeArrayListToFile(Helpers.getSortByFilename(), temp, getApplicationContext());

                if (Helpers.getSortBy().equals(Helpers.SortByType.TOP)) {
                    ArrayList<String> tempp = new ArrayList<String>() {{
                        add(Helpers.getSortByTopValue().name());
                    }};
                    Helpers.writeArrayListToFile(Helpers.getSortByTopValueFilename(), tempp, getApplicationContext());
                }

                refresh();

                drawer.closeDrawer(navigationView);
            }
        });

        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                if (!haltRefresh && Helpers.getSourcesToUse().size() > 0) {
                    refresh();
                }
                swipeContainer.setRefreshing(false);
            }
        });

        list_images.addOnScrollListener(new RecyclerView.OnScrollListener() {

            //start loading new images if the last visible image is the 4th last one
            @Override
            public void onScrollStateChanged(RecyclerView absListView, int i) {
                if (!haltRefresh && Helpers.getSourcesToUse().size() > 0) {
                    LinearLayoutManager man = ((LinearLayoutManager) list_images.getLayoutManager());
                    if (man.findLastVisibleItemPosition() > images.size() - 5) {
                        refreshWithAfter();
                    }
                }
            }

            @Override
            public void onScrolled(RecyclerView absListView, int dx, int dy) {
            }
        });

        //Check if we have permissions, and request permissions if we dont, or show an explanataion saying why we need it if the user has denied it previously
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                showExplanation("Permission Needed", "Permission to write to external storage is needed for the applications sharing to function properly!", Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_PERMISSION_STORAGE);
            } else {
                requestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, REQUEST_PERMISSION_STORAGE);
            }
        }
    }

    /**
     * Fix for errors produced when the list of images is empty and the user swipes
     */
    @Override
    public void onPause() {
        super.onPause();

        if (swipeContainer != null) {
            swipeContainer.setRefreshing(false);
            swipeContainer.destroyDrawingCache();
            swipeContainer.clearAnimation();
        }
    }

    /**
     * Handles the result given after the user is asked to allow a permission
     *
     * @param requestCode  The code given
     * @param permissions  Permisisons
     * @param grantResults Holds the value which details if the permission was granted
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String permissions[],
            @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                    this.finishAffinity();
                }
        }
    }

    /**
     * Shows the explanation for why a permission is needed. After the explanation is shown it asks the user to allow  the permission.
     * This is only called if the user has previously denied the program access to the permission
     *
     * @param title                 The title of the alert
     * @param message               The message of the alert
     * @param permission            The permission name
     * @param permissionRequestCode The permission request code
     */
    private void showExplanation(String title,
                                 String message,
                                 final String permission,
                                 final int permissionRequestCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int id) {
                        requestPermission(permission, permissionRequestCode);
                    }
                });
        builder.create().show();
    }

    /**
     * Requests a permission from the user
     *
     * @param permissionName        The permission name
     * @param permissionRequestCode The permission request code
     */
    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(this,
                new String[]{permissionName}, permissionRequestCode);
    }

    /**
     * Gets the index in the quicksettings menu of the currently chosen sorting method
     * This is used to set the correct selection in the list on program start.
     *
     * @return The index for the quicksettings list which corresponds to the currently selected sorting method
     */
    private int getQuicksettingsItemPositionForActiveSort() {
        int ret = 0;

        switch (Helpers.getSortBy()) {
            case NEW:
                for (int i = 0; i < quickSettingsItems.size(); i++) {
                    if (quickSettingsItems.get(i).equals("New")) {
                        ret = i;
                        quickSettingsClickedOption = "New";
                        break;
                    }
                }
                break;
            case HOT:
                for (int i = 0; i < quickSettingsItems.size(); i++) {
                    if (quickSettingsItems.get(i).equals("Popular")) {
                        ret = i;
                        quickSettingsClickedOption = "Popular";
                        break;
                    }
                }
                break;
            case RISING:
                for (int i = 0; i < quickSettingsItems.size(); i++) {
                    if (quickSettingsItems.get(i).equals("Rising")) {
                        ret = i;
                        quickSettingsClickedOption = "Rising";
                        break;
                    }
                }
                break;
            case TOP:
                switch (Helpers.getSortByTopValue()) {
                    case All:
                        for (int i = 0; i < quickSettingsItems.size(); i++) {
                            if (quickSettingsItems.get(i).equals("Top of all time")) {
                                ret = i;
                                quickSettingsClickedOption = "Top of all time";
                                break;
                            }
                        }
                        break;
                    case YEAR:
                        for (int i = 0; i < quickSettingsItems.size(); i++) {
                            if (quickSettingsItems.get(i).equals("Top of the year")) {
                                ret = i;
                                quickSettingsClickedOption = "Top of the year";
                                break;
                            }
                        }
                        break;
                    case MONTH:
                        for (int i = 0; i < quickSettingsItems.size(); i++) {
                            if (quickSettingsItems.get(i).equals("Top of the month")) {
                                ret = i;
                                quickSettingsClickedOption = "Top of the month";
                                break;
                            }
                        }
                        break;
                }
                break;
        }

        return ret;
    }

    /**
     * Gets the textual representation of the currently selected sorting option
     * This is used for example when showing the different options in the program for the user to select
     *
     * @return The tectual representation of the currently selected sorting option
     */
    private String getSortingByString() {
        String ret = "";

        switch (Helpers.getSortBy()) {
            case NEW:
                ret = "New";
                break;
            case HOT:
                ret = "Popular";
                break;
            case RISING:
                ret = "Rising";
                break;
            case TOP:
                switch (Helpers.getSortByTopValue()) {
                    case All:
                        ret = "Top of all time";
                        break;
                    case YEAR:
                        ret = "Top of the year";
                        break;
                    case MONTH:
                        ret = "Top of the month";
                        break;
                }
                break;
        }

        return ret;
    }

    /**
     * Clears the list of images, updates the text saying what we're currently sorting by to ensure that we're showing the correct one
     * Shows a loading progress bar while the program gets the new items to show
     * Starts the task to run in the background which gets the new images to show
     * <p>
     * Only runs when haltRefresh is false to prevent running refresh while another one is already running
     */
    private void refresh() {
        if (!haltRefresh) {
            getSupportActionBar().setSubtitle("Sorting by: " + getSortingByString());
            list_images.getLayoutManager().scrollToPosition(0);
            images.clear();
            adapter.notifyDataSetChanged();
            ProgressBar p = findViewById(R.id.refresh_progress);
            p.setVisibility(View.VISIBLE);
            TextView t = findViewById(R.id.refresh_text);
            t.setVisibility(View.VISIBLE);
            haltRefresh = true;
            new RefreshTask().execute();
        }
    }

    /**
     * Calls the RefreshWithAfterTask task, which adds the next "page" to the end of our currently shown list
     * <p>
     * Only runs when haltRefresh is false to prevent running refresh while another one is already running
     */
    private void refreshWithAfter() {
        if (!haltRefresh && Helpers.getSourcesToUse().size() > 0) {
            haltRefresh = true;
            new RefreshWithAfterTask().execute();
        }
    }

    /**
     * Handle opening the correct menu when doing a long press on an image
     *
     * @param menu     Menu to bo opened
     * @param v        View
     * @param menuInfo Menu info
     */
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
    }

    /**
     * Handle what happens when you select share or save from the context menu
     *
     * @param item Item selected
     * @return true
     */
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int position = adapter.getPosition();
        switch (item.getItemId()) {
            case R.id.share:
                final ImageView shareimgvw = new ImageView(this);

                Target sharetarget = new Target() {

                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        shareimgvw.setImageBitmap(bitmap);
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {

                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {

                    }
                };

                //Use picasso to load imageview into our target which gets our selected imageview
                //Then get the image from that imageview
                Picasso.with(this).load(images.get(position).getUrl()).into(sharetarget);
                Bitmap sharebmp = ((BitmapDrawable) shareimgvw.getDrawable()).getBitmap();

                //Save the selected image to the apps private storage folder, to later share from
                //Each time you share a new image it gets overwritten
                try {
                    File cachePath = new File(this.getCacheDir(), "images");
                    cachePath.mkdirs(); // don't forget to make the directory
                    FileOutputStream stream = new FileOutputStream(cachePath + "/image.png"); // overwrites this image every time
                    sharebmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //gets the Uri for the file
                File imagePath = new File(this.getCacheDir(), "images");
                File newFile = new File(imagePath, "image.png");
                Uri contentUri = FileProvider.getUriForFile(this, "com.memefinder.memefinder.fileprovider", newFile);

                //Share the file
                if (contentUri != null) {
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // temp permission for receiving app to read this file
                    shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
                    shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                    startActivity(Intent.createChooser(shareIntent, "Share an image"));
                }

                return true;
            case R.id.save:
                final ImageView saveimgvw = new ImageView(this);

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

                Picasso.with(this).load(images.get(position).getUrl()).into(savetarget);
                Bitmap savebmp = ((BitmapDrawable) saveimgvw.getDrawable()).getBitmap();

                //save the image
                boolean added = insertImage(this, savebmp);

                if (added)
                    Toast.makeText(this, "Image saved!", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, "Image could not be saved!", Toast.LENGTH_SHORT).show();

                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    /**
     * Takes a bitmap and saves it to the devices image folder
     *
     * @param context Application context
     * @param source  The bitmap to be saved
     * @return True if the image was saved. Otherwise false
     */
    public static boolean insertImage(Context context, Bitmap source) {
        ContentValues values = new ContentValues();
        boolean added = false;
        // save image
        String photoUriStr = MediaStore.Images.Media.insertImage(
                context.getContentResolver(),
                source,
                "",
                "");
        Uri photoUri = Uri.parse(photoUriStr);

        String asd = getRealPathFromURI(context, photoUri);
        File f = new File(asd);
        long size = f.length(); //real file size on disk

        values.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000);
        values.put(MediaStore.Images.Media.DATE_TAKEN, System.currentTimeMillis());

        values.put(MediaStore.Images.Media.SIZE, size);

        //update the saved image with the extra details
        context.getContentResolver().update(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values,
                MediaStore.Images.Media._ID + "=?",
                new String[]{ContentUris.parseId(photoUri) + ""});

        if (photoUri != null) {
            added = true;
        }

        return added;
    }

    /**
     * Resolves a Uri to the real path on the device from the one given by Mediastore which contains its own id instead of the filepath
     * This id is not usable for the actual filepath, this function queries Mediastore's table for the actual filepath
     *
     * @param context    Application context
     * @param contentUri The Uri to be resolved
     * @return The actual filepath of the Uri
     */
    public static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = {MediaStore.Images.Media.DATA};
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Handle pressing the back button
     * You have to press it twice in quick succession to close the app if no menu is open, in which case that menu is closed instead
     */
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(Gravity.END)) {
            drawer.closeDrawer(Gravity.END);
        } else {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }

            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);
        }
    }

    /**
     * Starts the settings activity if the user has pressed it
     *
     * @param item Item clicked
     * @return false
     */
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_settings) {
            startActivityForResult(new Intent(getApplicationContext(), SettingsActivity.class), settingsRequestCode);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(Gravity.END);
        return false;
    }

    /**
     * Destructor
     */
    @Override
    public void onDestroy() {
        list_images.setAdapter(null);
        super.onDestroy();
    }

    /**
     * Gets the new images to use, in the background
     * <p>
     * After the images are gathered it clears the list to make sure it's empty and adds the new images
     * It notifies the adapter to show the new images and scrolls to the top whilst hiding the loading progress bar
     */
    @SuppressLint("StaticFieldLeak")
    private class RefreshTask extends AsyncTask<Void, Void, Boolean> {
        private ArrayList<Image> tmp;

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (Helpers.getSourcesToUse().size() > 0)
                tmp = Helpers.populateImages();
            return true;
        }

        protected void onPostExecute(Boolean result) {
            if (Helpers.getSourcesToUse().size() > 0) {
                images.clear();
                images.addAll(tmp);
                adapter.notifyDataSetChanged();
                list_images.smoothScrollToPosition(0);
            } else {
                images.clear();
                adapter.notifyDataSetChanged();
            }

            ProgressBar p = findViewById(R.id.refresh_progress);
            p.setVisibility(View.GONE);
            TextView t = findViewById(R.id.refresh_text);
            t.setVisibility(View.GONE);

            haltRefresh = false;

        }
    }

    /**
     * Gets the new images to use while supplying the currently shown images, so as to append the new ones to the current list
     * Effectively adding a new page to the end
     * <p>
     * After the images are gathered it clears the list to make sure it's empty and adds the new images
     * It notifies the adapter to show the new images
     */
    @SuppressLint("StaticFieldLeak")
    private class RefreshWithAfterTask extends AsyncTask<Void, Void, Boolean> {
        private ArrayList<Image> tmp;

        @Override
        protected Boolean doInBackground(Void... voids) {
            if (Helpers.getSourcesToUse().size() > 0)
                tmp = Helpers.populateImagesWithAfter(images);
            return true;
        }

        protected void onPostExecute(Boolean result) {
            if (Helpers.getSourcesToUse().size() > 0) {
                images.clear();
                images.addAll(tmp);
                adapter.notifyDataSetChanged();
            } else {
                images.clear();
                adapter.notifyDataSetChanged();
            }

            haltRefresh = false;
        }
    }

    /**
     * Handles the result from the settings activity
     * If the result if RESULT_OK then something has been added or removed then it calls refresh to update the content shown
     *
     * @param requestCode Request code, activity specific and usermade
     * @param resultCode  Result
     * @param data        Data
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == settingsRequestCode) {
            if (resultCode == RESULT_OK) {
                list_images.getLayoutManager().scrollToPosition(0);
                refresh();
            }
        }
    }
}