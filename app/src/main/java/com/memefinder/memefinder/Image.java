package com.memefinder.memefinder;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Holds a title and url for any given image. Used for storing all of the currently showed images
 * <p>
 * Implements parcelable to allow serializing and sending the objects between actions by using an Intent
 */
public class Image implements Parcelable {
    private String title;
    private String url;

    Image() {
    }

    private Image(Parcel in) {
        title = in.readString();
        url = in.readString();

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(title);
        dest.writeString(url);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Image> CREATOR = new Creator<Image>() {

        @Override
        public Image createFromParcel(Parcel in) {
            return new Image(in);
        }

        @Override
        public Image[] newArray(int size) {
            return new Image[size];
        }
    };


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    String getUrl() {
        return url;
    }

    void setUrl(String url) {
        this.url = url;
    }
}
