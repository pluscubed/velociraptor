package com.pluscubed.velociraptor.cache;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LimitCacheSqlHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "cache.db";
    public static final int DB_VERSION = 7;

    public LimitCacheSqlHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(LimitCacheWay.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + LimitCacheWay.TABLE_NAME);
        onCreate(db);
    }
}
