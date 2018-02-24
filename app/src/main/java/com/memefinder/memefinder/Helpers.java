package com.memefinder.memefinder;

import android.content.Context;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Helpers class. This class contains various static functions and enums useful during the program.
 * It's used in starting the app as well as making requests to reddit to retrieve the links. Basically it contains the core of what is needed for the program to function.
 */
@SuppressWarnings("StatementWithEmptyBody")
class Helpers {

    /**
     * Used for sorting, also contains a description which is used in our requests to reddit
     */
    public enum SortByType {
        HOT("hot"),
        NEW("new"),
        RISING("rising"),
        TOP("top");

        private final String fieldDescription;

        SortByType(String value) {
            fieldDescription = value;
        }

        public String getFieldDescription() {
            return fieldDescription;
        }
    }

    /**
     * Also used for sorting, however only when you are sorting by top, since top has another qualifier as well
     */
    public enum SortByTopValues {
        All("all"),
        YEAR("year"),
        MONTH("month");

        private final String fieldDescription;

        SortByTopValues(String value) {
            fieldDescription = value;
        }

        public String getFieldDescription() {
            return fieldDescription;
        }
    }

    //The default sorting and the variable which contains the current one. Default is used to set the current one if no previous value existed
    private final static SortByType sortByDefault = SortByType.HOT;
    private static SortByType sortBy;

    //Same principle as above
    private final static SortByTopValues sortByTopValueDefault = SortByTopValues.All;
    private static SortByTopValues sortByTopValue;

    //Same principle as above
    private final static ArrayList<String> sourcesDefault = new ArrayList<String>() {{
        add("meirl");
        add("dankmemes");
        add("memes");
    }};
    private static ArrayList<String> sourcesToUse = new ArrayList<>();

    //filenames for the files that contain the various configuration information such as whata sources to use etc..
    private final static String sortByFilename = "sortby.txt";
    private final static String sortByTopValueFilename = "sortbytopvalue.txt";
    private final static String sourcesFilename = "sources.txt";

    //Contains a lists of SubReddit objects. Each one corresponds to a subreddit in use by the program.
    private static ArrayList<SubReddit> subReddits = new ArrayList<>();

    /**
     * Writes a given ArrayList of strings to a file, the file exists in the programs local data folder, which only exists as long as the app is installed
     * and is local to the app in question
     *
     * @param filename Name of the file to be written to
     * @param data     ArrayList of String to be written
     * @param context  Program context
     */
    static void writeArrayListToFile(String filename, ArrayList<String> data, Context context) {
        FileOutputStream outputStream;

        //Tries to first write an empty line, essentially clearing the file.
        //After that it writes each line of the arraylist to the file
        //If the given ArrayList is empty it writes a newline
        try {
            outputStream = context.openFileOutput(filename, Context.MODE_PRIVATE);
            String line = "";
            outputStream.write(line.getBytes());
            outputStream.close();

            outputStream = context.openFileOutput(filename, Context.MODE_APPEND);

            for (int i = 0; i < data.size(); i++) {
                line = data.get(i) + System.getProperty("line.separator");
                outputStream.write(line.getBytes());
            }

            if (data.size() <= 0) {
                line = "" + System.getProperty("line.separator");
                outputStream.write(line.getBytes());
            }

            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Reads a file form local storage to an ArrayList of String
     *
     * @param file    File to be read from
     * @param context Program context
     * @return ArrayList<String> of all lines found
     */
    private static ArrayList<String> readArrayListFromFile(String file, Context context) {
        //if file doesnt exist simply return null
        if (!fileExists(file, context))
            return null;

        ArrayList<String> ret = new ArrayList<>();

        //tries to read all lines of the file
        try {
            InputStream inputStream = context.openFileInput(file);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    ret.add(line);
                }

                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }

    /**
     * Checks whether a file exists or not in local app storage
     *
     * @param filename File to check
     * @param context  Program context
     * @return true if file exists, otherwise false
     */
    private static boolean fileExists(String filename, Context context) {
        File file = new File(context.getFilesDir(), filename);

        return file.exists();
    }

    /**
     * Initializes the application. It checks if files for what sources to use and what sorting to use exists
     * If they exist it reads them to the global variables to be used during runtime. If the sorting method is TOP, it tries to read its other qualifier from file as well
     * <p>
     * It also validates that all of the input is correct and matches all of the criteria, such as allowed characters for a source etc.
     * If any invalid part is found it's skipped or removed and after all of the validation has been done
     * it then saves the variables back to the device to ensure that the correct version exists on disk
     * <p>
     * If any of the files don't exist it then sets them to their default values
     *
     * @param context The context
     */
    static void initApp(Context context) {

        /*
          Tries to read what way to sort
         */
        if (Helpers.fileExists(Helpers.getSortByFilename(), context)) {
            ArrayList<String> file = Helpers.readArrayListFromFile(Helpers.getSortByFilename(), context);

            //loop through all of the sorting options and set sorting to the one read from file
            //unless it doesn't exist, then it sets the value to the default value
            boolean correctSort = false;
            for (SortByType s : SortByType.values()) {
                if (file != null && s.name().equals(file.get(0))) {
                    correctSort = true;
                    Helpers.setSortBy(s);
                    break;
                }
            }

            if (!correctSort) {
                Helpers.setSortBy(Helpers.getSortByDefault());
            }

        } else {
            Helpers.setSortBy(Helpers.getSortByDefault());
        }

        //Same principle as above
        if (Helpers.getSortBy().equals(SortByType.TOP)) {
            if (Helpers.fileExists(Helpers.getSortByTopValueFilename(), context)) {
                ArrayList<String> file = Helpers.readArrayListFromFile(Helpers.getSortByTopValueFilename(), context);

                boolean correctSortTime = false;
                for (SortByTopValues s : SortByTopValues.values()) {
                    if (file != null && s.name().equals(file.get(0))) {
                        correctSortTime = true;
                        Helpers.setSortByTopValue(s);
                        break;
                    }
                }

                if (!correctSortTime) {
                    Helpers.setSortByTopValue(Helpers.getSortByTopValueDefault());
                }
            } else {
                Helpers.setSortByTopValue(Helpers.getSortByTopValueDefault());
            }
        }

        //Tries to read the sources to use from file, while also checking that it doesnt read anything twice
        //it also adds it as lowercase since sources are not case-sensitive
        if (Helpers.fileExists(Helpers.getSourcesFilename(), context)) {
            ArrayList<String> file = Helpers.readArrayListFromFile(Helpers.getSourcesFilename(), context);

            if (file != null && file.size() > 0 && !file.get(0).equals("")) {
                for (int i = 0; i < file.size(); i++) {
                    if (!Helpers.getSourcesToUse().contains(file.get(i).toLowerCase())) {
                        Helpers.getSourcesToUse().add(file.get(i).toLowerCase());
                    }
                }
            }
        } else {
            Helpers.writeArrayListToFile(Helpers.getSourcesFilename(), Helpers.getSourcesDefault(), context);
            Helpers.setSourcesToUse(Helpers.getSourcesDefault());
        }


        ArrayList<String> toRemove = new ArrayList<>();

        Helpers.getSourcesToUse().add("invalid-source()=/;:_-");
        //checks all sources and adds the invalid ones to list for removal
        for (int i = 0; i < Helpers.getSourcesToUse().size(); i++) {
            if (Helpers.getSourcesToUse().get(i).length() > 21 ||
                    Helpers.getSourcesToUse().get(i).length() <= 0 ||
                    !Helpers.getSourcesToUse().get(i).matches("^[a-zA-Z0-9_]{1,21}$")) {
                toRemove.add(Helpers.getSourcesToUse().get(i));
            }
        }

        //if we have sources to remove, remove them
        if (toRemove.size() > 0)
            Helpers.getSourcesToUse().removeAll(toRemove);

        ArrayList<String> temp = new ArrayList<String>() {{
            add(Helpers.getSortBy().name());
        }};

        Helpers.writeArrayListToFile(Helpers.getSortByFilename(), temp, context);
        Helpers.writeArrayListToFile(Helpers.getSourcesFilename(), Helpers.getSourcesToUse(), context);

        if (Helpers.getSortBy().equals(SortByType.TOP)) {
            ArrayList<String> tempp = new ArrayList<String>() {{
                add(Helpers.getSortByTopValue().name());
            }};
            Helpers.writeArrayListToFile(Helpers.getSortByTopValueFilename(), tempp, context);
        }
    }

    /**
     * Gets a JSONObject from a reddit subreddit url using reddits JSON API
     *
     * @param urlString The url
     * @return A JSONObject containing all of the content of one page from a subreddit
     * @throws IOException
     * @throws JSONException
     */
    @Nullable
    private static JSONObject getJSONObjectFromURL(String urlString) throws IOException, JSONException {
        HttpURLConnection urlConnection;
        URL url = new URL(urlString);
        urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setReadTimeout(20000 /* milliseconds */);
        urlConnection.setConnectTimeout(20000 /* milliseconds */);
        urlConnection.setDoOutput(true);
        urlConnection.connect();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();

            String jsonString = sb.toString();

            return new JSONObject(jsonString);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Reads all of the sources from the getSourcesToUse() function. Which is just a list of String's
     * Then parses each subreddit as a url, gets the JSONObject from the url, and then filters out non-images, duplicates etc.
     * While adding the image-urls to a list
     * <p>
     * After gathering all of the images it sorts them concurrently instead of block-wise
     * i.e instead of A1 A2 A3 B1 B2 B3
     * it sorts it as A1 B1 A2 B2 A3 B3
     *
     * @return The final list of images to be shown as the "first page" of the app
     */
    static ArrayList<Image> populateImages() {
        ArrayList<ArrayList<Image>> array = new ArrayList<>();
        ArrayList<Image> images = new ArrayList<>();
        try {
            //loop through all sources, adding each source as an ArrayList<Image> to the ArrayList array
            //which then gets sorted later on
            for (int i = 0; i < Helpers.getSourcesToUse().size(); i++) {
                String urlString = "https://www.reddit.com/r/" + Helpers.getSourcesToUse().get(i) + "/" + getSortByString();
                JSONObject baseObject = getJSONObjectFromURL(urlString);

                if (baseObject != null) {
                    JSONObject jsonObject = baseObject.getJSONObject("data");
                    JSONArray posts = jsonObject.getJSONArray("children");

                    getSubReddits().add(new SubReddit(Helpers.getSourcesToUse().get(i), jsonObject.getString("after")));

                    //loop through all posts of the result, filtering out non-images and url's that have already been loaded
                    for (int j = 0; j < posts.length(); j++) {
                        if (posts.getJSONObject(j).getString("kind").equals("t3")) {
                            String url = posts.getJSONObject(j).getJSONObject("data").getString("url");
                            boolean skip = false;

                            for (int k = 0; k < images.size(); k++) {
                                if (images.get(k).getUrl().equals(url)) {
                                    skip = true;
                                }
                            }

                            if (url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png") || url.endsWith(".gif")) {
                            } else {
                                skip = true;
                            }

                            if (!skip) {
                                Image img = new Image();
                                img.setTitle(posts.getJSONObject(j).getJSONObject("data").getString("title"));
                                img.setUrl(getFinalRedirectedUrl(url));

                                images.add(img);
                            }
                        }
                    }
                }
                array.add(new ArrayList<>(images));
                images.clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //gets the index of the ArrayList with the biggest size
        int biggestIndex = 0;
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i).size() > array.get(biggestIndex).size()) {
                biggestIndex = i;
            }
        }

        //adds all of the different ArrayList's to one final list in the interweaved order
        if (array.size() > 0) {
            for (int i = 0; i < array.get(biggestIndex).size(); i++) {
                for (int j = 0; j < array.size(); j++) {
                    if (i < array.get(j).size())
                        images.add(array.get(j).get(i));
                }
            }
        }

        return images;
    }

    /**
     * Takes an ArrayList of images and stores it as a variable to be appended to later on
     * <p>
     * It then reads all of the sources from the getSourcesToUse() function. Which is just a list of String's
     * For each source it gets its associated "after" variable. Which is used in the request to show all of the results after the post with the id matching "after"
     * Then parses each subreddit as a url, gets the JSONObject from the url, and then filters out non-images, duplicates etc.
     * While adding the image-urls to a list
     * <p>
     * After gathering all of the images it sorts them concurrently instead of block-wise
     * i.e instead of A1 A2 A3 B1 B2 B3
     * it sorts it as A1 B1 A2 B2 A3 B3
     *
     * @param images The list of images to append the gathered images to, so as to extend the list
     * @return The final list of images to be shown after loading the "next page"
     */
    static ArrayList<Image> populateImagesWithAfter(ArrayList<Image> images) {
        ArrayList<ArrayList<Image>> array = new ArrayList<>();
        ArrayList<Image> finalList = new ArrayList<>(images);
        ArrayList<Image> tmpImages = new ArrayList<>();
        try {
            for (int i = 0; i < Helpers.getSourcesToUse().size(); i++) {
                String after = null;
                int x;
                for (x = 0; x < getSubReddits().size(); x++) {
                    if (getSubReddits().get(x).getName().equals(Helpers.getSourcesToUse().get(i))) {
                        after = getSubReddits().get(x).getAfter();
                        break;
                    }
                }

                if (after != null && !after.equals("null")) {
                    JSONObject baseObject = getJSONObjectFromURL("https://www.reddit.com/r/" + Helpers.getSourcesToUse().get(i) + "/" + getSortByString(after));

                    if (baseObject != null) {
                        JSONObject jsonObject = baseObject.getJSONObject("data");

                        ArrayList<SubReddit> subs = Helpers.getSubReddits();
                        subs.get(x).setAfter(jsonObject.getString("after"));
                        setSubReddits(subs);

                        JSONArray posts = jsonObject.getJSONArray("children");

                        for (int j = 0; j < posts.length(); j++) {
                            if (posts.getJSONObject(j).getString("kind").equals("t3")) {
                                String url = posts.getJSONObject(j).getJSONObject("data").getString("url");
                                boolean skip = false;

                                for (int k = 0; k < tmpImages.size(); k++) {
                                    if (tmpImages.get(k).getUrl().equals(url)) {
                                        skip = true;
                                    }
                                }

                                for (int k = 0; k < finalList.size(); k++) {
                                    if (finalList.get(k).getUrl().equals(url)) {
                                        skip = true;
                                    }
                                }

                                if (url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".png") || url.endsWith(".gif")) {
                                } else {
                                    skip = true;
                                }

                                if (!skip) {
                                    Image img = new Image();
                                    img.setTitle(posts.getJSONObject(j).getJSONObject("data").getString("title"));
                                    img.setUrl(getFinalRedirectedUrl(url));

                                    tmpImages.add(img);
                                }
                            }
                        }
                    }
                    array.add(new ArrayList<>(tmpImages));
                    tmpImages.clear();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        int biggestIndex = 0;
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i).size() > array.get(biggestIndex).size()) {
                biggestIndex = i;
            }
        }

        if (array.size() > 0) {
            for (int i = 0; i < array.get(biggestIndex).size(); i++) {
                for (int j = 0; j < array.size(); j++) {
                    if (i < array.get(j).size())
                        tmpImages.add(array.get(j).get(i));
                }
            }

            finalList.addAll(tmpImages);
        }
        return finalList;
    }

    /**
     * Gets the string for sorting used in the query to reddit. For example new.json if we are sorting by new, or top.json?t=all
     * if we are sorting by top of all time
     *
     * @return The string to be used in the query for reddit to get the posts in the desired order
     */
    private static String getSortByString() {
        String ret;
        if (getSortBy().equals(SortByType.TOP)) {
            ret = getSortBy().getFieldDescription() + ".json?t=" + getSortByTopValue().getFieldDescription();
        } else {
            ret = getSortBy().getFieldDescription() + ".json";
        }

        return ret;
    }

    /**
     * Gets the string for sorting used in the query to reddit. For example new.json if we are sorting by new, or top.json?t=all
     * if we are sorting by top of all time
     * <p>
     * Also adds the "after" variable used for pagination. When supplied it loads the posts starting with the next post after
     * the post with the if of the after-variable
     *
     * @param after Id of the last post currently shown. Used for reference to load posts after said post-id
     * @return The string to be used in the query for reddit to get the posts in the desired order while starting from the last loaded post
     */
    private static String getSortByString(String after) {
        String ret;
        if (getSortBy().equals(SortByType.TOP)) {
            ret = getSortBy().getFieldDescription() + ".json?t=" + getSortByTopValue().getFieldDescription() + "&after=" + after;
        } else {
            ret = getSortBy().getFieldDescription() + ".json?after=" + after;
        }

        return ret;
    }

    /**
     * Checks if a subreddit exists and if it matches the allowed pattern of a subreddit
     *
     * @param s The subreddit to test
     * @return True or false depending on if the subreddit existed or not
     */
    static boolean subredditExists(String s) {
        boolean ret;

        if (!s.matches("^[a-zA-Z0-9_]{1,21}$")) {
            ret = false;
        } else {
            String urlString = "https://www.reddit.com/r/" + s + "/new.json";
            try {
                JSONObject baseObject = getJSONObjectFromURL(urlString);

                if (baseObject != null) {
                    JSONObject jsonObject = baseObject.getJSONObject("data");

                    //Is left empty to assure that the array exists, if it doesnt it fails and we catch the exception and return false
                    JSONArray posts = jsonObject.getJSONArray("children");

                    ret = true;
                } else {
                    ret = false;
                }
            } catch (Exception e) {
                ret = false;
            }
        }
        return ret;
    }

    /**
     * Takes a given url and resolves it N amount of times in order to get the final url after
     * being potentially redirected
     * <p>
     * For example: m.imgur.com -> imgur.com -> i.imgur.com
     *
     * @param url The url to be resolved
     * @return The final resolved url
     */
    private static String getFinalRedirectedUrl(String url) {

        HttpURLConnection connection;
        String finalUrl = url;

        //Loops through the url until the response code is 200, meaning the the url is correct
        try {
            do {
                connection = (HttpURLConnection) new URL(finalUrl)
                        .openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setUseCaches(false);
                connection.setRequestMethod("GET");

                //User agent of chrome. Used to ensure the request is properly made and redirected
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36");
                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode >= 300 && responseCode < 400) {
                    String redirectedUrl = connection.getHeaderField("Location");
                    if (null == redirectedUrl)
                        break;
                    finalUrl = redirectedUrl;
                } else
                    break;
            } while (connection.getResponseCode() != HttpURLConnection.HTTP_OK);
            connection.disconnect();
        } catch (Exception ignored) {
        }
        return finalUrl;
    }

    /**
     * Various getters and setters
     */

    private static SortByType getSortByDefault() {
        return sortByDefault;
    }

    static SortByType getSortBy() {
        return sortBy;
    }

    static void setSortBy(SortByType sortBy) {
        Helpers.sortBy = sortBy;
    }

    private static ArrayList<String> getSourcesDefault() {
        return sourcesDefault;
    }

    static ArrayList<String> getSourcesToUse() {
        return sourcesToUse;
    }

    private static void setSourcesToUse(ArrayList<String> sourcesToUse) {
        Helpers.sourcesToUse = sourcesToUse;
    }

    static String getSortByFilename() {
        return sortByFilename;
    }

    static String getSourcesFilename() {
        return sourcesFilename;
    }

    private static ArrayList<SubReddit> getSubReddits() {
        return subReddits;
    }

    private static void setSubReddits(ArrayList<SubReddit> subReddits) {
        Helpers.subReddits = subReddits;
    }

    static SortByTopValues getSortByTopValue() {
        return sortByTopValue;
    }

    static void setSortByTopValue(SortByTopValues sortByTopValue) {
        Helpers.sortByTopValue = sortByTopValue;
    }

    static String getSortByTopValueFilename() {
        return sortByTopValueFilename;
    }

    private static SortByTopValues getSortByTopValueDefault() {
        return sortByTopValueDefault;
    }
}
