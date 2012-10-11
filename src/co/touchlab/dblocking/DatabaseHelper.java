package co.touchlab.dblocking;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

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
    static final String DATABASE_NAME = "dblocking.db";
    private static final int DATABASE_VERSION = 1;

    private static DatabaseHelper helper;

    public static synchronized DatabaseHelper getInstance(Context context)
    {
        if(helper == null)
        {
            helper = new DatabaseHelper(context);
        }

        return helper;
    }

    public DatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    static final class SessionTable implements BaseColumns {
        static final String NAME = "session";

        static final String DESCRIPTION = "description";

        static final String[] COLS = new String[] {
                _ID
                ,DESCRIPTION
        };

        static final String CREATE = String.format(
                "CREATE TABLE %1$s (" +
                        " %2$s INTEGER PRIMARY KEY AUTOINCREMENT" +
                        ",%3$s TEXT" +
                        ")"
                ,NAME
                ,_ID
                ,DESCRIPTION
        );

    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase)
    {
        sqLiteDatabase.execSQL(SessionTable.CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1)
    {
        sqLiteDatabase.execSQL(String.format("DROP TABLE %1$s", SessionTable.NAME));
        onCreate(sqLiteDatabase);
    }

    /**
     * Tries to insert a session with description
     * @param desc
     * @throws SQLException on insert error
     */
    public void insertSession(String desc) throws SQLException
    {
        final SQLiteDatabase writableDatabase = getWritableDatabase();
        final ContentValues contentValues = new ContentValues();

        contentValues.put(SessionTable.DESCRIPTION, desc);

        writableDatabase.insertOrThrow(SessionTable.NAME, null, contentValues);
    }

    /**
     *
     * @return Count of session records or -1 on error
     */
    public int getSessionCount()
    {
        int count = -1;

        Cursor cursor = getReadableDatabase().rawQuery(String.format("SELECT COUNT(*) FROM %1$s", SessionTable.NAME), null);

        if(cursor.moveToFirst())
            count = cursor.getInt(0);

        cursor.close();;

        return count;
    }

    /**
     * Update record by ID with description
     * @param id
     * @param desc
     */
    public void updateSession(int id, String desc)
    {
        final SQLiteDatabase writableDatabase = getWritableDatabase();
        try
        {
            final ContentValues contentValues = new ContentValues();

            contentValues.put(SessionTable.DESCRIPTION, desc);

            writableDatabase.update(
                    SessionTable.NAME, contentValues, SessionTable._ID+"=?", new String[]{String.valueOf(id)});
        }
        finally
        {
        }
    }

    /**
     *
     * @return All db records
     */
    public List<Session> loadAllSessions()
    {
        List<Session> sessions = new ArrayList<Session>();

        final SQLiteDatabase readableDatabase = getReadableDatabase();
        final Cursor cursor = readableDatabase.query(SessionTable.NAME, SessionTable.COLS
                , null, null, null, null, SessionTable._ID, null);

        try
        {
            while (cursor.moveToNext())
            {
                final Session session = sessionFromCursor(cursor);

                sessions.add(session);
            }
        }
        finally
        {
            cursor.close();
        }

        return sessions;
    }

    public static class Session
    {
        public final int id;
        public final String description;

        public Session(int id, String description)
        {
            this.description = description;
            this.id = id;
        }

    }

    /**
     * Gets a single session from the DB
     * If record doesn't exist or error, empty session is returned
     * @param id
     * @return
     */
    public Session getSession(int id)
    {
        final SQLiteDatabase readableDatabase = getReadableDatabase();

        final Session session;
        final Cursor cursor = readableDatabase.query(SessionTable.NAME,
                SessionTable.COLS,
                SessionTable._ID+"=?",
                new String[]{String.valueOf(id)},
                null,null,null,null
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

    /**
     * Record should be [ID, DESCRIPTION]
     * @param cursor
     * @return
     */
    private Session sessionFromCursor(Cursor cursor)
    {
        final Session session = new Session(cursor.getInt(0), cursor.getString(1));
        return session;
    }

}
