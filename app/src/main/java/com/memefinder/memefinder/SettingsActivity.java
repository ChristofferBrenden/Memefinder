package com.memefinder.memefinder;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;

@SuppressWarnings({"WeakerAccess", "ConstantConditions", "UseBulkOperation"})
public class SettingsActivity extends AppCompatActivity {
    private ListView list_sources;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> itemList = new ArrayList<>();
    private String subredditToAdd;
    private boolean subredditToAddOk = false;
    private Context context = this;

    ArrayList<String> selectedSources = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent data = new Intent();
                if (subredditToAddOk)
                    setResult(RESULT_OK, data);
                else
                    setResult(RESULT_CANCELED, data);

                finish();
            }
        });


        list_sources = findViewById(R.id.settings_sources_list);

        for (int i = 0; i < Helpers.getSourcesToUse().size(); i++) {
            itemList.add(Helpers.getSourcesToUse().get(i));
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_multiple_choice, itemList);

        list_sources.setAdapter(adapter);
        list_sources.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        list_sources.setSoundEffectsEnabled(false);

        list_sources.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (selectedSources.contains(Integer.toString(position))) {
                    selectedSources.remove(Integer.toString(position));
                } else {
                    selectedSources.add(Integer.toString(position));
                }
            }
        });

        Button addSubredditButton = findViewById(R.id.settings_button_addsource);

        addSubredditButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                EditText text = findViewById(R.id.settings_text_subreddit);
                subredditToAdd = text.getText().toString();
                addSubreddit();
            }
        });

        Button removeSubredditButton = findViewById(R.id.settings_button_removesources);

        removeSubredditButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                removeSubreddits();
            }
        });
    }

    private void removeSubreddits() {
        new RemoveSubredditsTask().execute();
    }

    private void addSubreddit() {
        new AddSubredditTask().execute();
    }

    private void showAlert(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle(title)
                .setPositiveButton("Ok", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onBackPressed() {
        Intent data = new Intent();
        if (subredditToAddOk)
            setResult(RESULT_OK, data);
        else
            setResult(RESULT_CANCELED, data);

        finish();
    }

    @SuppressLint("StaticFieldLeak")
    private class AddSubredditTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            if (Helpers.subredditExists(subredditToAdd)) {
                if (Helpers.getSourcesToUse().contains(subredditToAdd.toLowerCase())) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            showAlert("Subreddit already exists!", "The subreddit is already added");
                        }
                    });
                } else {
                    Helpers.getSourcesToUse().add(subredditToAdd.toLowerCase());
                    Helpers.writeArrayListToFile(Helpers.getSourcesFilename(), Helpers.getSourcesToUse(), context);

                    subredditToAddOk = true;

                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            EditText text = findViewById(R.id.settings_text_subreddit);
                            text.setText("");
                            selectedSources.clear();
                            list_sources.clearChoices();
                            itemList.add(subredditToAdd.toLowerCase());
                            adapter.notifyDataSetChanged();
                            showAlert("Subreddit added!", "The subreddit was added");
                        }
                    });
                }
            } else {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        showAlert("Subreddit not found!", "The subreddit does not exist, has the wrong format, or is not accesible");
                    }
                });
            }
            return true;
        }

    }

    @SuppressLint("StaticFieldLeak")
    private class RemoveSubredditsTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... voids) {
            if (android.os.Debug.isDebuggerConnected())
                android.os.Debug.waitForDebugger();
            final ArrayList<String> toRemove = new ArrayList<>();

            for (int i = 0; i < selectedSources.size(); i++) {
                int index = Integer.parseInt(selectedSources.get(i));
                toRemove.add(Helpers.getSourcesToUse().get(index));
            }

            Helpers.getSourcesToUse().removeAll(toRemove);
            if (toRemove.size() == 0) {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        showAlert("No subreddit(s) selected!", "No subreddit(s) were selected!");
                    }
                });
                subredditToAddOk = false;
            } else {
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        selectedSources.clear();
                        list_sources.clearChoices();
                        itemList.removeAll(toRemove);
                        adapter.notifyDataSetChanged();

                        showAlert("Subreddit(s) removed!", "The subreddit(s) have been removed!");
                    }
                });

                Helpers.writeArrayListToFile(Helpers.getSourcesFilename(), Helpers.getSourcesToUse(), context);

                subredditToAddOk = true;
            }

            return true;
        }

    }
}
