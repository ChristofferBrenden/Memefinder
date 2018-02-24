package com.memefinder.memefinder;

/**
 * Used for connecting a subreddit (source) to an "after" variable
 * this after variable is used when making a request and if included the response contains everything after
 * that variable. The variable is a post-id and by doing this you effectively get the next page in the response
 */
public class SubReddit {
    private final String name;
    private String after;

    SubReddit(String name, String after) {
        this.name = name;
        this.setAfter(after);
    }

    public String getName() {
        return name;
    }

    String getAfter() {
        return after;
    }

    void setAfter(String after) {
        this.after = after;
    }
}
