package co.touchlab.dblocking;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: kgalligan
 * Date: 10/9/11
 * Time: 6:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class DatabaseHelper extends SQLiteOpenHelper
{
    private static final String DATABASE_NAME = "dblocking.db";
    private static final int DATABASE_VERSION = 1;
    private static DatabaseHelper helper;
    public static final String[] SESSION_COLS = new String[]
            {
                    "id",
                    "description"
            };


    public DatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase)
    {
        String createSessionTable =
                "CREATE TABLE `session` " +
                        "(" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "`description` VARCHAR" +
                        ") ";
        sqLiteDatabase.execSQL(createSessionTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1)
    {
        sqLiteDatabase.execSQL("drop table session");
        onCreate(sqLiteDatabase);
    }

    public void createSession(String desc)
    {
        final SQLiteDatabase writableDatabase = getWritableDatabase();
        final ContentValues contentValues = new ContentValues();

        contentValues.put("description", desc);

        writableDatabase.insertOrThrow("session", null, contentValues);
    }

    public void updateSession(Integer id, String desc)
    {
        final SQLiteDatabase writableDatabase = getWritableDatabase();
        try
        {
            final ContentValues contentValues = new ContentValues();

            contentValues.put("description", desc);

            writableDatabase.update("session", contentValues, "id = ?", new String[]{id.toString()});
        }
        finally
        {
        }
    }

    public List<Session> loadAllSessions()
    {
        final SQLiteDatabase readableDatabase = getReadableDatabase();
        List<Session> sessions;
        final Cursor sessionCursor = readableDatabase.query("session", SESSION_COLS, null, null, null, null, "id", null);

        try
        {
            sessions = new ArrayList<Session>();

            while (sessionCursor.moveToNext())
            {
                final Session session = sessionFromCursor(sessionCursor);

                sessions.add(session);
            }
        }
        finally
        {
            sessionCursor.close();
        }

        return sessions;
    }

    public static class Session
    {
        private Integer id;
        private String description;

        public Session(Integer id, String description)
        {
            this.description = description;
            this.id = id;
        }

        public String getDescription()
        {
            return description;
        }

        public Integer getId()
        {
            return id;
        }
    }

    public Session loadSession(Integer id)
    {
        final SQLiteDatabase readableDatabase = getReadableDatabase();

        final Session session;
        final Cursor cursor = readableDatabase.query("session",
                SESSION_COLS,
                "id = ?",
                new String[]{id.toString()},
                null,
                null,
                null,
                null
        );
        try
        {


            cursor.moveToNext();

            session = sessionFromCursor(cursor);
        }
        finally
        {
            cursor.close();
        }

        return session;
    }

    private Session sessionFromCursor(Cursor cursor)
    {
        final Session session = new Session(cursor.getInt(0), cursor.getString(1));
        return session;
    }


}
