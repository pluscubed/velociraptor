package com.pluscubed.velociraptor.appselection;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

public class AppInfo implements Comparable<AppInfo>, Parcelable {
    public static final Parcelable.Creator<AppInfo> CREATOR = new Parcelable.Creator<AppInfo>() {
        @Override
        public AppInfo createFromParcel(Parcel source) {
            return new AppInfo(source);
        }

        @Override
        public AppInfo[] newArray(int size) {
            return new AppInfo[size];
        }
    };
    public boolean enabled;
    public String packageName;
    public String name;

    public AppInfo() {
    }

    protected AppInfo(Parcel in) {
        this.packageName = in.readString();
        this.name = in.readString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof AppInfo && packageName.equals(((AppInfo) o).packageName);
    }

    @Override
    public int compareTo(@NonNull AppInfo another) {
        return name.compareTo(another.name);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.packageName);
        dest.writeString(this.name);
    }

    @Override
    public String toString() {
        return packageName;
    }
}