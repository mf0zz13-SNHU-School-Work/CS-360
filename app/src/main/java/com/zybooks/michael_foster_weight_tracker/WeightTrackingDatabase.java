package com.zybooks.michael_foster_weight_tracker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * SQLite helper for the weight tracking application.
 *
 * <p>This class creates the user, daily weight, and goal weight tables and
 * provides CRUD helper methods used by the controller and model.</p>
 */
public class WeightTrackingDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "weightTracker.db";
    private static final int VERSION = 1;

    /**
     * Creates a database helper using the application context.
     *
     * @param context Android context used to open or create the database
     */
    public WeightTrackingDatabase(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    //region Declaration of Tables
    /**
     * Defines the user table and column names.
     */
    private static final class UserTable {
        private static final String TABLE = "user";
        private static final String COL_USERNAME = "userName";
        private static final String COL_PASSWORD = "password";
        private static final String COL_SMSPREFERENCE = "smsPreference";
        private static final String COL_PHONENUMBER = "phoneNumber";
    }

    /**
     * Defines the daily weight table and column names.
     */
    private static final class DailyWeightTable {
        private static final String TABLE = "dailyWeight";
        private static final String COL_WEIGHTID = "weightID";
        private static final String COL_USERNAME = "userName";
        private static final String COL_DATE = "date";
        private static final String COL_WEIGHT = "weight";
    }

    /**
     * Defines the goal weight table and column names.
     */
    private static final class GoalWeightTable {
        private static final String TABLE = "goalWeight";
        private static final String COL_USERNAME = "userName";
        private static final String COL_GOALWEIGHT = "goalWeight";
    }
    //endregion

    //region database initialization
    /**
     * Enables foreign key checks before the database is created or opened.
     *
     * @param db database connection being configured
     */
    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    /**
     * Creates the application database tables.
     *
     * @param db writable database used to execute table creation SQL
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + UserTable.TABLE + " (" +
                UserTable.COL_USERNAME + " text primary key, " +
                UserTable.COL_PASSWORD + " text, " +
                UserTable.COL_SMSPREFERENCE + " integer," +
                UserTable.COL_PHONENUMBER + " text)");

        db.execSQL("create table " + DailyWeightTable.TABLE + " (" +
                DailyWeightTable.COL_WEIGHTID + " integer primary key autoincrement, " +
                DailyWeightTable.COL_USERNAME + " text, " +
                DailyWeightTable.COL_DATE + " text, " +
                DailyWeightTable.COL_WEIGHT + " real, " +
                "foreign key(" + DailyWeightTable.COL_USERNAME + ") references " +
                UserTable.TABLE + "(" + UserTable.COL_USERNAME + "))");


        db.execSQL("create table " + GoalWeightTable.TABLE + " (" +
                GoalWeightTable.COL_USERNAME + " text primary key, " +
                GoalWeightTable.COL_GOALWEIGHT + " real, " +
                "foreign key(" + GoalWeightTable.COL_USERNAME + ") references " +
                UserTable.TABLE + "(" + UserTable.COL_USERNAME + "))");
    }

    /**
     * Rebuilds the database when the schema version changes.
     *
     * @param db writable database being upgraded
     * @param oldVersion previous database version
     * @param newVersion new database version
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("drop table if exists " + GoalWeightTable.TABLE);
        db.execSQL("drop table if exists " + DailyWeightTable.TABLE);
        db.execSQL("drop table if exists " + UserTable.TABLE);
        onCreate(db);
    }
    //endregion

    //region CRUD for User Table
    /**
     * Adds a new user with SMS notifications disabled by default.
     *
     * @param userName username for the new account
     * @param password password for the new account
     * @return true when the row is inserted, otherwise false
     */
    public boolean addUser(String userName, String password) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(UserTable.COL_USERNAME, userName);
        values.put(UserTable.COL_PASSWORD, password);
        values.put(UserTable.COL_SMSPREFERENCE, 0);
        values.put(UserTable.COL_PHONENUMBER, "");

        long result = db.insert(UserTable.TABLE, null, values);
        return result != -1;
    }

    /**
     * Checks whether a username and password match a stored user record.
     *
     * @param userName username entered on the login screen
     * @param password password entered on the login screen
     * @return true when the credentials match, otherwise false
     */
    public boolean validateUser(String userName, String password) {
        SQLiteDatabase db = getReadableDatabase();

        String sql = "select " + UserTable.COL_PASSWORD +
                " from " + UserTable.TABLE +
                " where " + UserTable.COL_USERNAME + " = ?";
        Cursor cursor = db.rawQuery(sql, new String[] {userName});
        if (cursor.moveToFirst()) {
            String returnedPassword = cursor.getString(0);
            if (returnedPassword.equals(password)) {
                cursor.close();
                return true;
            }
        }

        cursor.close();
        return false;
    }

    /**
     * Gets the user's saved SMS notification preference.
     *
     * @param userName username whose SMS preference should be retrieved
     * @return 1 when SMS is enabled, 0 when disabled, or -1 when no user is found
     */
    public int getSmsPreference(String userName){
        SQLiteDatabase db = getReadableDatabase();
        int preference = -1;

        String sql = "select " + UserTable.COL_SMSPREFERENCE +
                " from " + UserTable.TABLE +
                " where " + UserTable.COL_USERNAME + " = ?";
        Cursor cursor = db.rawQuery(sql, new String[] {userName});
        if (cursor.moveToFirst()) {
            preference = cursor.getInt(0);
        }
        cursor.close();
        return preference;
    }

    /**
     * Gets the saved phone number for a user.
     *
     * @param userName username whose phone number should be retrieved
     * @return stored phone number, or an empty string when no number is found
     */
    public String getPhoneNumber(String userName){
        SQLiteDatabase db = getReadableDatabase();
        String phoneNumber = "";

        String sql = "select " + UserTable.COL_PHONENUMBER +
                " from " + UserTable.TABLE +
                " where " + UserTable.COL_USERNAME + " = ?";
        Cursor cursor = db.rawQuery(sql, new String[] {userName});
        if (cursor.moveToFirst()) {
            phoneNumber = cursor.getString(0);
        }
        cursor.close();
        return phoneNumber;
    }

    /**
     * Updates the stored password for an existing user.
     *
     * @param userName username whose password should be updated
     * @param password new password value
     * @return true when a user row is updated, otherwise false
     */
    public boolean updatePassword(String userName, String password) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(UserTable.COL_PASSWORD, password);

        int rowsUpdated = db.update(UserTable.TABLE, values,
                UserTable.COL_USERNAME + "= ?",
                new String[] {userName});
        return rowsUpdated > 0;
    }

    /**
     * Updates the stored SMS preference for an existing user.
     *
     * @param userName username whose SMS preference should be updated
     * @param preference 1 for enabled SMS notifications, 0 for disabled
     * @return true when a user row is updated, otherwise false
     */
    public boolean updateSmsPreference(String userName, int preference) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(UserTable.COL_SMSPREFERENCE, preference);

        int rowsUpdated = db.update(
                UserTable.TABLE,
                values,
                UserTable.COL_USERNAME + " = ?",
                new String[] { userName }
        );

        return rowsUpdated > 0;
    }

    /**
     * Deletes a user account from the user table.
     *
     * @param userName username identifying the user to delete
     * @return true when a user row is deleted, otherwise false
     */
    public boolean deleteUser(String userName) {
        SQLiteDatabase db = getWritableDatabase();

        int rowsDeleted = db.delete(
                UserTable.TABLE,
                UserTable.COL_USERNAME + " = ?",
                new String[] { userName }
        );

        return rowsDeleted > 0;
    }

    //endregion

    //region CRUD for Daily Weight Table
    /**
     * Adds a daily weigh-in for the current date.
     *
     * @param userName username associated with the weigh-in
     * @param weight weight value entered by the user
     * @return true when the weigh-in is inserted, otherwise false
     */
    public boolean addDailyWeight(String userName, double weight) {
        SQLiteDatabase db = getWritableDatabase();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String formattedDate = formatter.format(new Date());

        ContentValues values = new ContentValues();
        values.put(DailyWeightTable.COL_USERNAME, userName);
        values.put(DailyWeightTable.COL_DATE, formattedDate);
        values.put(DailyWeightTable.COL_WEIGHT, weight);

        long result = db.insert(DailyWeightTable.TABLE, null, values);
        return result != -1;
    }

    /**
     * Gets all stored weigh-ins for a user ordered by date descending.
     *
     * @param userName username whose weigh-ins should be retrieved
     * @return map of date strings to weight values
     */
    public Map<String, Double> getWeighIns(String userName) {
        Map<String, Double> weights = new LinkedHashMap<>();
        SQLiteDatabase db = getReadableDatabase();

        String sql = "SELECT " + DailyWeightTable.COL_DATE + ", " +
                DailyWeightTable.COL_WEIGHT +
                " FROM " + DailyWeightTable.TABLE +
                " WHERE " + DailyWeightTable.COL_USERNAME + " = ?" +
                " ORDER BY " + DailyWeightTable.COL_DATE + " DESC";
        Cursor cursor = db.rawQuery(sql, new String[] {userName});
        if (cursor.moveToFirst()) {
            do {
                String date = cursor.getString(0);
                double weight = cursor.getDouble(1);
                weights.put(date,weight);
            } while(cursor.moveToNext());
        }
        cursor.close();
        return weights;
    }

    /**
     * Gets the user's weight entry for the current date.
     *
     * @param userName username whose current-day weight should be retrieved
     * @return today's weight, or -1.0 when no entry exists
     */
    public Double getTodayWeight(String userName) {
        double weight = -1.0;

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String formattedDate = formatter.format(new Date());

        SQLiteDatabase db = getReadableDatabase();

        String sql = "SELECT " + DailyWeightTable.COL_DATE + ", " +
                DailyWeightTable.COL_WEIGHT +
                " FROM " + DailyWeightTable.TABLE +
                " WHERE " + DailyWeightTable.COL_USERNAME + " = ?" +
                " AND " + DailyWeightTable.COL_DATE + " = ?";
        Cursor cursor = db.rawQuery(sql, new String[] {userName, formattedDate});
        if (cursor.moveToFirst()) {
            weight = cursor.getDouble(1);
        }
        cursor.close();
        return weight;
    }

    /**
     * Updates a user's weigh-in for a specific date.
     *
     * @param userName username associated with the weigh-in
     * @param date date string identifying the weigh-in to update
     * @param weight new weight value
     * @return true when a weigh-in row is updated, otherwise false
     */
    public boolean updateWeight(String userName, String date, double weight) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DailyWeightTable.COL_WEIGHT, weight);

        int rowsUpdated = db.update(DailyWeightTable.TABLE, values,
                DailyWeightTable.COL_DATE + " = ? and " +
                        DailyWeightTable.COL_USERNAME + " = ?",
                new String[] {date, userName});
        return rowsUpdated > 0;
    }

    /**
     * Deletes a user's weigh-in for a specific date.
     *
     * @param userName username associated with the weigh-in
     * @param date date string identifying the weigh-in to delete
     * @return true when a weigh-in row is deleted, otherwise false
     */
    public boolean deleteWeight(String userName, String date) {
        SQLiteDatabase db = getWritableDatabase();

        int rowsDeleted = db.delete(
                DailyWeightTable.TABLE,
                DailyWeightTable.COL_USERNAME + " = ? and " +
                DailyWeightTable.COL_DATE + " = ?",
                new String[] {userName, date}
        );

        return rowsDeleted > 0;
    }
    //endregion

    //region CRUD for Goal Weight Table
    /**
     * Adds the goal weight for a new user.
     *
     * @param userName username associated with the goal weight
     * @param goalWeight goal weight value entered during account creation
     * @return true when the goal row is inserted, otherwise false
     */
    public boolean addGoalWeight(String userName, double goalWeight) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(GoalWeightTable.COL_USERNAME, userName);
        values.put(GoalWeightTable.COL_GOALWEIGHT, goalWeight);

        long result = db.insert(GoalWeightTable.TABLE, null, values);
        return result != -1;
    }
    /**
     * Gets the goal weight for a user.
     *
     * @param userName username whose goal weight should be retrieved
     * @return stored goal weight, or -1 when no goal is found
     */
    public double getGoalWeight(String userName) {
        double weight = -1;
        SQLiteDatabase db = getReadableDatabase();

        String sql = "SELECT " + GoalWeightTable.COL_GOALWEIGHT +
                " FROM " + GoalWeightTable.TABLE +
                " WHERE " + GoalWeightTable.COL_USERNAME + " = ?";
        Cursor cursor = db.rawQuery(sql, new String[] {userName});
        if (cursor.moveToFirst()) {
            weight = cursor.getDouble(0);
        }
        cursor.close();
        return weight;
    }

    /**
     * Updates the goal weight for an existing user.
     *
     * @param userName username whose goal weight should be updated
     * @param weight new goal weight value
     * @return true when a goal row is updated, otherwise false
     */
    public boolean updateGoalWeight(String userName, double weight) {
        SQLiteDatabase db = getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(GoalWeightTable.COL_GOALWEIGHT, weight);

        int rowsUpdated = db.update(GoalWeightTable.TABLE, values,
                GoalWeightTable.COL_USERNAME + " = ?",
                new String[] {userName});
        return rowsUpdated > 0;
    }

    /**
     * Deletes the goal weight record for a user.
     *
     * @param userName username whose goal weight should be deleted
     * @return true when a goal row is deleted, otherwise false
     */
    public boolean deleteGoalWeight(String userName) {
        SQLiteDatabase db = getWritableDatabase();

        int rowsDeleted = db.delete(
                GoalWeightTable.TABLE,
                GoalWeightTable.COL_USERNAME + " = ?",
                new String[] {userName}
        );
        return rowsDeleted > 0;
    }
    //endregion

}
