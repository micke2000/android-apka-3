package com.example.watki;

import android.os.Parcel;
import android.os.Parcelable;

public class PostepInfo implements Parcelable {
    public int mPobranychBajtow;
    public int mRozmiar;
    public String mStatus;

    PostepInfo(int pobrane, int totalSize,String status){
        mPobranychBajtow = pobrane;
        mRozmiar = totalSize;
        mStatus = status;
    }
    PostepInfo(Parcel paczka){
        mPobranychBajtow = paczka.readInt();
        mRozmiar = paczka.readInt();
        mStatus = paczka.readString();
    }
    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(mPobranychBajtow);
        parcel.writeInt(mRozmiar);
        parcel.writeString(mStatus);
    }
    public static final Creator<PostepInfo> CREATOR = new Creator<PostepInfo>() {
        @Override
        public PostepInfo createFromParcel(Parcel in) {
            return new PostepInfo(in);
        }

        @Override
        public PostepInfo[] newArray(int size) {
            return new PostepInfo[size];
        }
    };
}
