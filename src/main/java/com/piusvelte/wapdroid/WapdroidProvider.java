/*
 * Wapdroid - Android Location based Wifi Manager
 * Copyright (C) 2012 Bryan Emmanuel
 * 
 * This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  Bryan Emmanuel piusvelte@gmail.com
 */
package com.piusvelte.wapdroid;

import java.util.HashMap;

import com.piusvelte.wapdroid.Wapdroid.Cells;
import com.piusvelte.wapdroid.Wapdroid.Locations;
import com.piusvelte.wapdroid.Wapdroid.Networks;
import com.piusvelte.wapdroid.Wapdroid.Pairs;
import com.piusvelte.wapdroid.Wapdroid.Ranges;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;

public class WapdroidProvider extends ContentProvider {

    public static final String AUTHORITY = BuildConfig.PACKAGE_NAME + ".WapdroidProvider";

    private static final UriMatcher sUriMatcher;

    protected static final String DATABASE_NAME = "wapdroid";

    private static final int DATABASE_VERSION = 11;

    public static final String TAG = "WapdroidProvider";

    public static final String TABLE_NETWORKS = "networks";
    private static final int NETWORKS = 0;
    private static HashMap<String, String> networksProjectionMap;

    public static final String TABLE_CELLS = "cells";
    private static final int CELLS = 1;
    private static HashMap<String, String> cellsProjectionMap;

    public static final String TABLE_PAIRS = "pairs";
    private static final int PAIRS = 2;
    private static HashMap<String, String> pairsProjectionMap;

    public static final String TABLE_LOCATIONS = "locations";
    private static final int LOCATIONS = 3;
    private static HashMap<String, String> locationsProjectionMap;

    public static final String VIEW_RANGES = "ranges";
    private static final int RANGES = 4;
    private static HashMap<String, String> rangesProjectionMap;

    private DatabaseHelper mDatabaseHelper;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        sUriMatcher.addURI(AUTHORITY, TABLE_NETWORKS, NETWORKS);
        networksProjectionMap = new HashMap<String, String>();
        networksProjectionMap.put(Networks._ID, Networks._ID);
        networksProjectionMap.put(Networks.SSID, Networks.SSID);
        networksProjectionMap.put(Networks.BSSID, Networks.BSSID);
        networksProjectionMap.put(Networks.MANAGE, Networks.MANAGE);
        networksProjectionMap.put(Networks.LATITUDE, Networks.LATITUDE);
        networksProjectionMap.put(Networks.LONGITUDE, Networks.LONGITUDE);
        networksProjectionMap.put(Networks.RADIUS, Networks.RADIUS);
        networksProjectionMap.put(Networks.COORD_RSSI, Networks.COORD_RSSI);

        sUriMatcher.addURI(AUTHORITY, TABLE_CELLS, CELLS);
        cellsProjectionMap = new HashMap<String, String>();
        cellsProjectionMap.put(Cells._ID, Cells._ID);
        cellsProjectionMap.put(Cells.CID, Cells.CID);
        cellsProjectionMap.put(Cells.LOCATION, Cells.LOCATION);

        sUriMatcher.addURI(AUTHORITY, TABLE_PAIRS, PAIRS);
        pairsProjectionMap = new HashMap<String, String>();
        pairsProjectionMap.put(Pairs._ID, Pairs._ID);
        pairsProjectionMap.put(Pairs.CELL, Pairs.CELL);
        pairsProjectionMap.put(Pairs.NETWORK, Pairs.NETWORK);
        pairsProjectionMap.put(Pairs.RSSI_MAX, Pairs.RSSI_MAX);
        pairsProjectionMap.put(Pairs.RSSI_MIN, Pairs.RSSI_MIN);
        pairsProjectionMap.put(Pairs.MANAGE_CELL, Pairs.MANAGE_CELL);

        sUriMatcher.addURI(AUTHORITY, TABLE_LOCATIONS, LOCATIONS);
        locationsProjectionMap = new HashMap<String, String>();
        locationsProjectionMap.put(Locations._ID, Locations._ID);
        locationsProjectionMap.put(Locations.LAC, Locations.LAC);

        sUriMatcher.addURI(AUTHORITY, VIEW_RANGES, RANGES);
        rangesProjectionMap = new HashMap<String, String>();
        rangesProjectionMap.put(Ranges._ID, Ranges._ID);
        rangesProjectionMap.put(Ranges.BSSID, Ranges.BSSID);
        rangesProjectionMap.put(Ranges.CELL, Ranges.CELL);
        rangesProjectionMap.put(Ranges.CID, Ranges.CID);
        rangesProjectionMap.put(Ranges.LAC, Ranges.LAC);
        rangesProjectionMap.put(Ranges.LOCATION, Ranges.LOCATION);
        rangesProjectionMap.put(Ranges.NETWORK, Ranges.NETWORK);
        rangesProjectionMap.put(Ranges.RSSI_MAX, Ranges.RSSI_MAX);
        rangesProjectionMap.put(Ranges.RSSI_MIN, Ranges.RSSI_MIN);
        rangesProjectionMap.put(Ranges.SSID, Ranges.SSID);
        rangesProjectionMap.put(Ranges.MANAGE, Ranges.MANAGE);
        rangesProjectionMap.put(Ranges.MANAGE_CELL, Ranges.MANAGE_CELL);
    }


    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db;
        synchronized (Wapdroid.sDatabaseLock) {
            db = mDatabaseHelper.getWritableDatabase();
        }
        int count;
        switch (sUriMatcher.match(uri)) {
            case NETWORKS:
                synchronized (Wapdroid.sDatabaseLock) {
                    // the orphaned pairs need to be deleted as well
                    Cursor networksToDelete = db.query(TABLE_NETWORKS, new String[]{Networks._ID}, selection, selectionArgs, null, null, null);
                    long[] networkIds = new long[networksToDelete.getCount()];

                    while (networksToDelete.moveToNext() && !networksToDelete.isAfterLast()) {
                        networkIds[networksToDelete.getPosition()] = networksToDelete.getLong(0);
                    }

                    networksToDelete.close();
                    count = db.delete(TABLE_NETWORKS, selection, selectionArgs);

                    for (long networkId : networkIds) {
                        db.delete(TABLE_PAIRS, Pairs.NETWORK + "=?", new String[]{String.valueOf(networkId)});
                    }

                    cleanupCells(db);
                }
                break;
            case CELLS:
                synchronized (Wapdroid.sDatabaseLock) {
                    count = db.delete(TABLE_CELLS, selection, selectionArgs);
                }
                break;
            case LOCATIONS:
                synchronized (Wapdroid.sDatabaseLock) {
                    count = db.delete(TABLE_LOCATIONS, selection, selectionArgs);
                }
                break;
            case PAIRS:
                synchronized (Wapdroid.sDatabaseLock) {
                    count = db.delete(TABLE_PAIRS, selection, selectionArgs);
                    cleanupCells(db);
                }

                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        // the view needs to be updated also
        if (sUriMatcher.match(uri) == PAIRS) {
            getContext().getContentResolver().notifyChange(Ranges.CONTENT_URI, null);
        }

        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NETWORKS:
                return Networks.CONTENT_TYPE;
            case CELLS:
                return Cells.CONTENT_TYPE;
            case LOCATIONS:
                return Locations.CONTENT_TYPE;
            case PAIRS:
                return Pairs.CONTENT_TYPE;
            case RANGES:
                return Ranges.CONTENT_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db;
        synchronized (Wapdroid.sDatabaseLock) {
            db = mDatabaseHelper.getWritableDatabase();
        }
        long rowId;
        Uri returnUri;
        switch (sUriMatcher.match(uri)) {
            case NETWORKS:
                synchronized (Wapdroid.sDatabaseLock) {
                    rowId = db.insert(TABLE_NETWORKS, Networks._ID, values);
                }
                returnUri = ContentUris.withAppendedId(Networks.CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(returnUri, null);
                break;
            case CELLS:
                synchronized (Wapdroid.sDatabaseLock) {
                    rowId = db.insert(TABLE_CELLS, Cells._ID, values);
                }
                returnUri = ContentUris.withAppendedId(Cells.CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(returnUri, null);
                break;
            case LOCATIONS:
                synchronized (Wapdroid.sDatabaseLock) {
                    rowId = db.insert(TABLE_LOCATIONS, Locations._ID, values);
                }
                returnUri = ContentUris.withAppendedId(Locations.CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(returnUri, null);
                break;
            case PAIRS:
                synchronized (Wapdroid.sDatabaseLock) {
                    rowId = db.insert(TABLE_PAIRS, Pairs._ID, values);
                }
                returnUri = ContentUris.withAppendedId(Pairs.CONTENT_URI, rowId);
                getContext().getContentResolver().notifyChange(returnUri, null);
                getContext().getContentResolver().notifyChange(Ranges.CONTENT_URI, null);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        return returnUri;
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
            case NETWORKS:
                qb.setTables(TABLE_NETWORKS);
                qb.setProjectionMap(networksProjectionMap);
                break;
            case CELLS:
                qb.setTables(TABLE_CELLS);
                qb.setProjectionMap(cellsProjectionMap);
                break;
            case LOCATIONS:
                qb.setTables(TABLE_LOCATIONS);
                qb.setProjectionMap(locationsProjectionMap);
                break;
            case PAIRS:
                qb.setTables(TABLE_PAIRS);
                qb.setProjectionMap(pairsProjectionMap);
                break;
            case RANGES:
                qb.setTables(VIEW_RANGES);
                qb.setProjectionMap(rangesProjectionMap);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        SQLiteDatabase db;
        synchronized (Wapdroid.sDatabaseLock) {
            db = mDatabaseHelper.getReadableDatabase();
        }
        Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        SQLiteDatabase db;
        synchronized (Wapdroid.sDatabaseLock) {
            db = mDatabaseHelper.getWritableDatabase();
        }
        int count;
        switch (sUriMatcher.match(uri)) {
            case NETWORKS:
                synchronized (Wapdroid.sDatabaseLock) {
                    count = db.update(TABLE_NETWORKS, values, selection, selectionArgs);

                    // moving to geofencing, deprecate and cleanup old tables
                    if (count > 0) {
                        if (values.containsKey(Networks.LATITUDE)) {
                            // move network to geofencing
                            Cursor networks = db.query(TABLE_NETWORKS, new String[]{Networks._ID}, Networks.LATITUDE + "!=-999.0", null, null, null, null);

                            if (networks.moveToFirst()) {
                                int idIdx = networks.getColumnIndex(Networks._ID);

                                while (!networks.isAfterLast()) {
                                    db.delete(TABLE_PAIRS, Pairs.NETWORK + "=?", new String[]{String.valueOf(networks.getLong(idIdx))});
                                    networks.moveToNext();
                                }
                            }

                            cleanupCells(db);
                        }
                    }
                }
                break;
            case CELLS:
                synchronized (Wapdroid.sDatabaseLock) {
                    count = db.update(TABLE_CELLS, values, selection, selectionArgs);
                }
                break;
            case LOCATIONS:
                synchronized (Wapdroid.sDatabaseLock) {
                    count = db.update(TABLE_LOCATIONS, values, selection, selectionArgs);
                }
                break;
            case PAIRS:
                synchronized (Wapdroid.sDatabaseLock) {
                    count = db.update(TABLE_PAIRS, values, selection, selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        // the view needs to be updated also
        if (sUriMatcher.match(uri) == PAIRS)
            getContext().getContentResolver().notifyChange(Ranges.CONTENT_URI, null);
        return count;
    }

    private void cleanupCells(SQLiteDatabase db) {
        Cursor c = db.query(TABLE_CELLS, new String[]{Cells._ID, Cells.LOCATION}, null, null, null, null, null);
        if (c.moveToFirst()) {
            final int idIdx = c.getColumnIndex(Cells._ID);
            final int locationIdx = c.getColumnIndex(Cells.LOCATION);

            do {
                String[] args = new String[]{String.valueOf(c.getInt(idIdx))};
                Cursor p = db.query(TABLE_PAIRS, new String[]{Pairs._ID}, Pairs.CELL + "=?", args, null, null, null, null);

                if (p.getCount() == 0) {
                    db.delete(TABLE_CELLS, Cells._ID + "=?", args);
                    String[] locationArgs = new String[]{String.valueOf(c.getInt(locationIdx))};
                    Cursor l = db.query(TABLE_CELLS, new String[]{Cells.LOCATION}, Cells.LOCATION + "=?", locationArgs, null, null, null, null);
                    if (l.getCount() == 0) db.delete(TABLE_LOCATIONS, Locations._ID + "=?", locationArgs);
                    l.close();
                }

                p.close();
            } while (c.moveToNext() && !c.isAfterLast());
        }

        c.close();
    }

    public class DatabaseHelper extends SQLiteOpenHelper {

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Wapdroid.Networks.createTable(db);
            Wapdroid.Cells.createTable(db);
            Wapdroid.Pairs.createTable(db);
            Wapdroid.Locations.createTable(db);
            Wapdroid.Ranges.createView(db);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion < 2) {
                // add BSSID
                db.execSQL("drop table if exists " + TABLE_NETWORKS + "_bkp;");
                db.execSQL("create temporary table " + TABLE_NETWORKS + "_bkp as select * from " + TABLE_NETWORKS + ";");
                db.execSQL("drop table if exists " + TABLE_NETWORKS + ";");
                db.execSQL("create table if not exists " + TABLE_NETWORKS + " (_id  integer primary key autoincrement, "
                        + Networks.SSID + " text not null, "
                        + Networks.BSSID + " text not null);");
                db.execSQL("insert into " + TABLE_NETWORKS + " select " + Networks._ID + ", " + Networks.SSID + ", \"\" from " + TABLE_NETWORKS + "_bkp;");
                db.execSQL("drop table if exists " + TABLE_NETWORKS + "_bkp;");
            }

            if (oldVersion < 3) {
                // add locations
                db.execSQL("create table if not exists " + TABLE_LOCATIONS + " (_id  integer primary key autoincrement, "
                        + Locations.LAC + " integer);");
                // first backup cells to create pairs
                db.execSQL("drop table if exists " + TABLE_CELLS + "_bkp;");
                db.execSQL("create temporary table " + TABLE_CELLS + "_bkp as select * from " + TABLE_CELLS + ";");
                // update cells, dropping network column, making unique
                db.execSQL("drop table if exists " + TABLE_CELLS + ";");
                db.execSQL("create table if not exists " + TABLE_CELLS + " (_id  integer primary key autoincrement, " + Cells.CID + " integer, location integer);");
                db.execSQL("insert into " + TABLE_CELLS + " (" + Cells.CID + ", " + Ranges.LOCATION
                        + ") select " + Cells.CID + ", " + Wapdroid.UNKNOWN_CID + " from " + TABLE_CELLS + "_bkp group by " + Cells.CID + ";");
                // create pairs
                db.execSQL("create table if not exists " + TABLE_PAIRS + " (_id  integer primary key autoincrement, cell integer, network integer, " + Pairs.RSSI_MIN + " integer, " + Pairs.RSSI_MAX + " integer);");
                db.execSQL("insert into " + TABLE_PAIRS
                        + " (" + Pairs.CELL + ", " + Pairs.NETWORK + ", " + Pairs.RSSI_MIN + ", " + Pairs.RSSI_MAX
                        + ") select " + TABLE_CELLS + "." + Cells._ID + ", " + TABLE_CELLS + "_bkp." + Pairs.NETWORK + ", " + Wapdroid.UNKNOWN_RSSI + ", " + Wapdroid.UNKNOWN_RSSI
                        + " from " + TABLE_CELLS + "_bkp"
                        + " left join " + TABLE_CELLS + " on " + TABLE_CELLS + "_bkp." + Cells.CID + "=" + TABLE_CELLS + "." + Cells.CID + ";");
                db.execSQL("drop table if exists " + TABLE_CELLS + "_bkp;");
            }

            if (oldVersion < 4) {
                // clean lac=0 locations
                Cursor locations = db.rawQuery("select " + Locations._ID + " from " + TABLE_LOCATIONS + " where " + Locations.LAC + "=0", null);
                if (locations.getCount() > 0) {
                    locations.moveToFirst();
                    int index = locations.getColumnIndex(Locations._ID);
                    while (!locations.isAfterLast()) {
                        int location = locations.getInt(index);
                        // clean pairs
                        db.execSQL("delete from " + TABLE_PAIRS + " where " + Pairs._ID + " in (select " + TABLE_PAIRS + "." + Pairs._ID + " as " + Pairs._ID + " from " + TABLE_PAIRS
                                + " left join " + TABLE_CELLS + " on " + Pairs.CELL + "=" + TABLE_CELLS + "." + Cells._ID
                                + " where " + Ranges.LOCATION + "=" + location + ");");
                        // clean cells
                        db.execSQL("delete from " + TABLE_CELLS + " where " + Ranges.LOCATION + "=" + location + ";");
                        locations.moveToNext();
                    }
                    // clean locations
                    db.execSQL("delete from " + TABLE_LOCATIONS + " where " + Locations.LAC + "=0;");
                }
            }

            if (oldVersion < 5) {
                // fix bad rssi values
                db.execSQL("update " + TABLE_PAIRS + " set " + Pairs.RSSI_MIN + "=-1*" + Pairs.RSSI_MIN + " where " + Pairs.RSSI_MIN + " >0 and " + Pairs.RSSI_MIN + " !=" + Wapdroid.UNKNOWN_RSSI + ";");
                db.execSQL("update " + TABLE_PAIRS + " set " + Pairs.RSSI_MAX + "=-1*" + Pairs.RSSI_MAX + " where " + Pairs.RSSI_MAX + " >0 and " + Pairs.RSSI_MAX + " !=" + Wapdroid.UNKNOWN_RSSI + ";");
            }

            if (oldVersion < 6) {
                // revert incorrect unknown rssi's
                db.execSQL("update " + TABLE_PAIRS + " set " + Pairs.RSSI_MIN + "=99," + Pairs.RSSI_MAX + "=99 where " + Pairs.RSSI_MAX + "<" + Pairs.RSSI_MIN + " and RSSI_max=-85;");
            }

            if (oldVersion < 7) {
                // need to make all column names lowercase
                db.execSQL("drop table if exists " + TABLE_NETWORKS + "_bkp;");
                db.execSQL("create temporary table " + TABLE_NETWORKS + "_bkp as select * from " + TABLE_NETWORKS + ";");
                db.execSQL("drop table if exists " + TABLE_NETWORKS + ";");
                db.execSQL("create table if not exists " + TABLE_NETWORKS + " ("
                        + Networks._ID + " integer primary key autoincrement, "
                        + Networks.SSID + " text not null, "
                        + Networks.BSSID + " text not null);");
                db.execSQL("insert into " + TABLE_NETWORKS + " (" + Networks._ID + "," + Networks.SSID + "," + Networks.BSSID + ") select " + Networks._ID + ",SSID,BSSID from " + TABLE_NETWORKS + "_bkp;");
                db.execSQL("drop table if exists " + TABLE_NETWORKS + "_bkp;");

                db.execSQL("drop table if exists " + TABLE_CELLS + "_bkp;");
                db.execSQL("create temporary table " + TABLE_CELLS + "_bkp as select * from " + TABLE_CELLS + ";");
                db.execSQL("drop table if exists " + TABLE_CELLS + ";");
                db.execSQL("create table if not exists " + TABLE_CELLS + " ("
                        + Cells._ID + " integer primary key autoincrement, "
                        + Cells.CID + " integer, "
                        + Cells.LOCATION + " integer);");
                db.execSQL("insert into " + TABLE_CELLS + " (" + Cells._ID + "," + Cells.CID + "," + Cells.LOCATION + ") select " + Cells._ID + ",CID," + Cells.LOCATION + " from " + TABLE_CELLS + "_bkp;");
                db.execSQL("drop table if exists " + TABLE_CELLS + "_bkp;");

                db.execSQL("drop table if exists " + TABLE_PAIRS + "_bkp;");
                db.execSQL("create temporary table " + TABLE_PAIRS + "_bkp as select * from " + TABLE_PAIRS + ";");
                db.execSQL("drop table if exists " + TABLE_PAIRS + ";");
                db.execSQL("create table if not exists " + TABLE_PAIRS + " ("
                        + Pairs._ID + " integer primary key autoincrement, "
                        + Pairs.CELL + " integer, "
                        + Pairs.NETWORK + " integer, "
                        + Pairs.RSSI_MIN + " integer, "
                        + Pairs.RSSI_MAX + " integer);");
                db.execSQL("insert into " + TABLE_PAIRS + " (" + Pairs._ID + "," + Pairs.CELL + "," + Pairs.NETWORK + "," + Pairs.RSSI_MIN + "," + Pairs.RSSI_MAX + ") select " + Pairs._ID + "," + Pairs.CELL + "," + Pairs.NETWORK + ",RSSI_min, RSSI_max from " + TABLE_PAIRS + "_bkp;");
                db.execSQL("drop table if exists " + TABLE_PAIRS + "_bkp;");
                db.execSQL("drop table if exists " + TABLE_LOCATIONS + "_bkp;");
                db.execSQL("create temporary table " + TABLE_LOCATIONS + "_bkp as select * from " + TABLE_LOCATIONS + ";");
                db.execSQL("drop table if exists " + TABLE_LOCATIONS + ";");
                db.execSQL("create table if not exists " + TABLE_LOCATIONS + " ("
                        + Locations._ID + " integer primary key autoincrement, "
                        + Locations.LAC + " integer);");
                db.execSQL("insert into " + TABLE_LOCATIONS + " (" + Locations._ID + "," + Locations.LAC + ") select " + Locations._ID + ",LAC from " + TABLE_LOCATIONS + "_bkp;");
                db.execSQL("drop table if exists " + TABLE_LOCATIONS + "_bkp;");
                db.execSQL("drop view if exists " + VIEW_RANGES + ";");
                db.execSQL("create view if not exists " + VIEW_RANGES + " as select "
                        + TABLE_PAIRS + "." + Ranges._ID + " as " + Pairs._ID
                        + "," + Pairs.RSSI_MAX
                        + "," + Pairs.RSSI_MIN
                        + "," + Cells.CID
                        + "," + Locations.LAC
                        + "," + Ranges.LOCATION
                        + "," + Ranges.SSID
                        + "," + Ranges.BSSID
                        + "," + Ranges.CELL
                        + "," + Ranges.NETWORK
                        + " from " + TABLE_PAIRS
                        + " left join " + TABLE_CELLS + " on " + TABLE_CELLS + "." + Cells._ID + "=" + Ranges.CELL
                        + " left join " + TABLE_LOCATIONS + " on " + TABLE_LOCATIONS + "." + Locations._ID + "=" + Ranges.LOCATION
                        + " left join " + TABLE_NETWORKS + " on " + TABLE_NETWORKS + "." + Networks._ID + "=" + Pairs.NETWORK + ";");
            }

            if (oldVersion < 8) {
                // add the manage column for optional network management
                db.execSQL("drop table if exists " + TABLE_NETWORKS + "_bkp;");
                db.execSQL("create temporary table " + TABLE_NETWORKS + "_bkp as select * from " + TABLE_NETWORKS + ";");
                db.execSQL("drop table if exists " + TABLE_NETWORKS + ";");
                db.execSQL("create table if not exists " + TABLE_NETWORKS + " ("
                        + Networks._ID + " integer primary key autoincrement, "
                        + Networks.SSID + " text not null, "
                        + Networks.BSSID + " text not null, "
                        + Networks.MANAGE + " integer);");
                db.execSQL("insert into " + TABLE_NETWORKS + " select " + Networks._ID + ", " + Networks.SSID + ", " + Networks.BSSID + ", 1 from " + TABLE_NETWORKS + "_bkp;");
                db.execSQL("drop table if exists " + TABLE_NETWORKS + "_bkp;");
                db.execSQL("drop view if exists " + VIEW_RANGES + ";");
                db.execSQL("create view if not exists " + VIEW_RANGES + " as select "
                        + TABLE_PAIRS + "." + Ranges._ID + " as " + Ranges._ID
                        + "," + Ranges.RSSI_MAX
                        + "," + Ranges.RSSI_MIN
                        + "," + Ranges.CID
                        + "," + Ranges.LAC
                        + "," + Ranges.LOCATION
                        + "," + Ranges.SSID
                        + "," + Ranges.BSSID
                        + "," + Ranges.CELL
                        + "," + Ranges.NETWORK
                        + "," + Ranges.MANAGE
                        + " from " + TABLE_PAIRS
                        + " left join " + TABLE_CELLS + " on " + TABLE_CELLS + "." + Ranges._ID + "=" + Ranges.CELL
                        + " left join " + TABLE_LOCATIONS + " on " + TABLE_LOCATIONS + "." + Ranges._ID + "=" + Ranges.LOCATION
                        + " left join " + TABLE_NETWORKS + " on " + TABLE_NETWORKS + "." + Ranges._ID + "=" + Ranges.NETWORK + ";");
            }

            if (oldVersion < 9) {
                // add the manage column for optional cell management
                db.execSQL("drop table if exists " + TABLE_PAIRS + "_bkp;");
                db.execSQL("create temporary table " + TABLE_PAIRS + "_bkp as select * from " + TABLE_PAIRS + ";");
                db.execSQL("drop table if exists " + TABLE_PAIRS + ";");
                db.execSQL("create table if not exists " + TABLE_PAIRS + " ("
                        + Pairs._ID + " integer primary key autoincrement, "
                        + Pairs.CELL + " integer, "
                        + Pairs.NETWORK + " integer, "
                        + Pairs.RSSI_MIN + " integer, "
                        + Pairs.RSSI_MAX + " integer, "
                        + Pairs.MANAGE_CELL + " integer);");
                db.execSQL("insert into " + TABLE_PAIRS
                        + " select "
                        + Pairs._ID + ", "
                        + Pairs.CELL + ", "
                        + Pairs.NETWORK + ", "
                        + Pairs.RSSI_MIN + ", "
                        + Pairs.RSSI_MAX + ", 1 from " + TABLE_PAIRS + "_bkp;");
                db.execSQL("drop table if exists " + TABLE_PAIRS + "_bkp;");
                db.execSQL("drop view if exists " + VIEW_RANGES + ";");
                db.execSQL("create view if not exists " + VIEW_RANGES + " as select "
                        + TABLE_PAIRS + "." + Ranges._ID + " as " + Ranges._ID
                        + "," + Ranges.RSSI_MAX
                        + "," + Ranges.RSSI_MIN
                        + "," + Ranges.CID
                        + "," + Ranges.LAC
                        + "," + Ranges.LOCATION
                        + "," + Ranges.SSID
                        + "," + Ranges.BSSID
                        + "," + Ranges.CELL
                        + "," + Ranges.NETWORK
                        + "," + Ranges.MANAGE
                        + "," + Ranges.MANAGE_CELL
                        + " from " + TABLE_PAIRS
                        + " left join " + TABLE_CELLS + " on " + TABLE_CELLS + "." + Ranges._ID + "=" + Ranges.CELL
                        + " left join " + TABLE_LOCATIONS + " on " + TABLE_LOCATIONS + "." + Ranges._ID + "=" + Ranges.LOCATION
                        + " left join " + TABLE_NETWORKS + " on " + TABLE_NETWORKS + "." + Ranges._ID + "=" + Ranges.NETWORK + ";");
            }

            if (oldVersion < 10) {
                // strip quoted SSIDs
                Cursor c = db.rawQuery("select " + Networks._ID + "," + Networks.SSID + " from " + TABLE_NETWORKS + " where " + Networks.SSID + " like '\"%\"';", null);
                if (c.moveToFirst()) {
                    while (!c.isAfterLast()) {
                        db.execSQL("update " + TABLE_NETWORKS + " set " + Networks.SSID + "='" + Wapdroid.stripQuotes(c.getString(1)) + "' where " + Networks._ID + "=" + c.getInt(0));
                        c.moveToNext();
                    }
                }
            }

            if (oldVersion < 11) {
                // geofencing
                db.execSQL("ALTER TABLE " + Networks.TABLE_NAME
                        + " ADD COLUMN " + Networks.LATITUDE + " REAL DEFAULT " + Float.toString(Networks.INVALID_FLOAT) + ";");
                db.execSQL("ALTER TABLE " + Networks.TABLE_NAME
                        + " ADD COLUMN " + Networks.LONGITUDE + " REAL DEFAULT " + Float.toString(Networks.INVALID_FLOAT) + ";");
                db.execSQL("ALTER TABLE " + Networks.TABLE_NAME
                        + " ADD COLUMN " + Networks.RADIUS + " REAL DEFAULT " + Float.toString(Networks.INVALID_FLOAT) + ";");
                db.execSQL("ALTER TABLE " + Networks.TABLE_NAME
                        + " ADD COLUMN " + Networks.COORD_RSSI + " INTEGER DEFAULT 0;");
            }
        }
    }


}
