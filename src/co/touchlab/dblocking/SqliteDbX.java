package co.touchlab.dblocking;

import android.app.Activity;
import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import co.touchlab.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SqliteDbX = SqliteDbExample
 * This app is just a collection of examples written to illustrate simple concepts
 * Testing is minimal and this app is prone to many failures not related to the illustrated concepts
 */
public class SqliteDbX extends Activity
{
    private static final String TAG = SqliteDbX.class.getSimpleName();

    private static final int DEFAULT_INSERTS = 20;

    private TextView mTvRunTime;
    private EditText mEtInsertCount;
    private TextView mTvResults;
    private TickerBar mTbThreadTicker;

    private AtomicInteger allCount = new AtomicInteger();
    private Handler uiHandler;

    private static int sThreadCounter = 0;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mTvRunTime = (TextView) findViewById(R.id.timeOut);
        mEtInsertCount = (EditText) findViewById(R.id.numberOfInserts);
        mTvResults = (TextView) findViewById(R.id.results);
        mTbThreadTicker = (TickerBar) findViewById(R.id.tickerBar);

        mEtInsertCount.setHint(String.valueOf(DEFAULT_INSERTS));

        allCount.set(0);
        uiHandler = new Handler();

        // One instance of a SqliteDatabaseHelper will not crash
        findViewById(R.id.oneWrite).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view)
            {
                runHelperInstances(true);
            }
        });

        // More than one instance of SqliteDatabaseHelper will eventually conflict and crash
        findViewById(R.id.manyWrite).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view)
            {
                runHelperInstances(false);
            }
        });

        // One write many reads can crash on conflict (but won't if no conflict)
        findViewById(R.id.oneWriteManyRead).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view)
            {
                runWriteRead(1);
            }
        });

        // All reads can crash on conflict (but won't if no conflict)
        findViewById(R.id.manyRead).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view)
            {
                runWriteRead(0);
            }
        });

        // Read and writes will take turns
        findViewById(R.id.testTransactionIsolation).setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                clearFeedback();
                DatabaseHelper helper = DatabaseHelper.getInstance(SqliteDbX.this);
                new FastSelectThread(helper, sThreadCounter++).start();
                new SlowInsertThread(helper, sThreadCounter++).start();
            }
        });

        // Individual inserts take time
        findViewById(R.id.noTrans).setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                runNoTrans();
            }
        });

        // Inserts in a transaction are faster
        findViewById(R.id.trans).setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                runTrans();
            }
        });

        // No yield in a long running transaction will block database writes until the transaction is complete
        findViewById(R.id.no_yielding).setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view) {
                runYield(false);
            }
        });

        // Yielding allows openings in long running transactions for other operations to perform
        findViewById(R.id.yielding).setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view) {
                runYield(true);
            }
        });

        // Reads aren't performed when a transaction is exclusive
        findViewById(R.id.no_yield_read).setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view) {
                runYieldRead(false);
            }
        });

        // Yielding in a transaction allows other operations to perform at yield points
        findViewById(R.id.yield_and_read).setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view) {
                runYieldRead(true);
            }
        });

    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "Deleting database");
        deleteDatabase(DatabaseHelper.DATABASE_NAME);
        super.onDestroy();
    }

    private void clearFeedback() {
        mTbThreadTicker.clear();
        mTbThreadTicker.drawTick(Color.RED, TickerBar.THICK);
        mTvResults.setText("");
        mTvRunTime.setText("");
    }

    private void runHelperInstances(boolean singleInstance) {

        // Draw a tick when the example ends
        final TickerBar tickerBar = mTbThreadTicker;
        EventTimer yieldEventTimer = new EventTimer(4);
        yieldEventTimer.setOnEventListener(new EventTimer.OnEventListener() {
            @Override
            public void onStop(long delta) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tickerBar.drawTick(Color.RED, TickerBar.THICK);
                    }
                });

            }
        });

        TransactionEndListener endListener = new TransactionEndListener(yieldEventTimer);

        allCount.set(0);

        final int threadCount = 4;
        final List<Thread> allThreads = new ArrayList<Thread>(threadCount);

        final int runCount = 25;

        DatabaseHelper helper = new DatabaseHelper(SqliteDbX.this);

        int[] colors = new int[] {Color.BLUE, Color.WHITE, Color.RED, Color.YELLOW};

        for(int i=0; i<threadCount; i++)
        {
            if(!singleInstance) {
                helper = new DatabaseHelper(SqliteDbX.this);
            }
            allThreads.add(new DbInsertThread(helper, runCount, sThreadCounter++, endListener, colors[i]));
        }

        clearFeedback();

        runAllThreads(allThreads);

    }

    private void runWriteRead(int writeCount) {

        final int threadCount = 8;

        if(writeCount < 0)
            writeCount = 0;
        else if(writeCount > threadCount)
            writeCount = threadCount;

        allCount.set(0);

        final List<Thread> allThreads = new ArrayList<Thread>(threadCount);

        final int insertCount = 50;

        for(int i=0; i<writeCount; i++)
        {
            allThreads.add(new DbInsertThread(new DatabaseHelper(SqliteDbX.this), insertCount, sThreadCounter++));
        }

        final int selectCount = 15;
        for(int i=writeCount; i<threadCount; i++)
        {
            allThreads.add(new FastSelectThread(new DatabaseHelper(SqliteDbX.this), sThreadCounter++, selectCount));
        }

        runAllThreads(allThreads);

    }

    @SuppressWarnings("unchecked")
    private void runTrans()
    {
        new AsyncTask(){

            @Override
            protected Object doInBackground(Object... objects)
            {
                DatabaseHelper instance = DatabaseHelper.getInstance(SqliteDbX.this);

                SQLiteDatabase writableDatabase = instance.getWritableDatabase();

                long start = System.currentTimeMillis();

                writableDatabase.beginTransaction();
                performInserts(instance);
                writableDatabase.setTransactionSuccessful();
                writableDatabase.endTransaction();

                return start;
            }

            @Override
            protected void onPostExecute(Object o)
            {
                showTime((Long)o, DatabaseHelper.getInstance(SqliteDbX.this));
            }
        }.execute();

    }
    
    @SuppressWarnings("unchecked")
    private void runNoTrans()
    {
        new AsyncTask(){

            @Override
            protected Object doInBackground(Object... o)
            {
                DatabaseHelper instance = DatabaseHelper.getInstance(SqliteDbX.this);
                long start = System.currentTimeMillis();

                performInserts(instance);

                return start;
            }

            @Override
            protected void onPostExecute(Object o)
            {
                showTime((Long)o, DatabaseHelper.getInstance(SqliteDbX.this));
            }
        }.execute();
    }

    /**
     * Illustrate no yield/safe yielding during transactions
     *  No yield: Transactions block and the order of completion is as listed
     *  Yielding: Shorter inserts will complete by slipping in during yields before the long transaction finishes
     *  @param doYield Flag to enable yielding (true) or keep transactions exclusive (false)
     */
    @SuppressWarnings("unchecked")
    private void runYield(boolean doYield) {

        DatabaseHelper helper = DatabaseHelper.getInstance(this);

        Log.i(TAG, String.format("runYield\n\t%1$s\n\trecords: %2$d"
                , (doYield ? "Yielding" : "No yield")
                , helper.getSessionCount()));

        clearFeedback();

        final TickerBar tickerBar = mTbThreadTicker;
        final TextView timeDisplay = mTvRunTime;

        EventTimer yieldEventTimer = new EventTimer(6);
        yieldEventTimer.setOnEventListener(new EventTimer.OnEventListener() {
            @Override
            public void onStop(long delta) {

                final String executionTime = String.format("Execution time: %1$d", delta);

                Log.i(TAG, executionTime);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tickerBar.drawTick(Color.RED, TickerBar.THICK);
                        timeDisplay.setText(executionTime);
                    }
                });

            }
        });

        TransactionEndListener endListener = new TransactionEndListener(yieldEventTimer);

        SingleTransactionRepeatedInserts long1 = new SingleTransactionRepeatedInserts(helper, sThreadCounter++, 1000
                , endListener, Color.GRAY, 0, doYield);
        SingleTransactionRepeatedInserts short1 = new SingleTransactionRepeatedInserts(helper, sThreadCounter++, 20
                , endListener);
        SingleTransactionRepeatedInserts short2 = new SingleTransactionRepeatedInserts(helper, sThreadCounter++, 20
                , endListener, 150);
        SingleTransactionRepeatedInserts short3 = new SingleTransactionRepeatedInserts(helper, sThreadCounter++, 20
                , endListener, 300);
        SingleTransactionRepeatedInserts short4 = new SingleTransactionRepeatedInserts(helper, sThreadCounter++, 20
                , endListener, 500);
        SingleTransactionRepeatedInserts short5 = new SingleTransactionRepeatedInserts(helper, sThreadCounter++, 20
                , endListener, 800);

        yieldEventTimer.start();

        short1.start();

        long1.start();

        short2.start();
        short3.start();
        short4.start();
        short5.start();

    }

    @SuppressWarnings("unchecked")
    private void runYieldRead(boolean doYield) {

        DatabaseHelper helper = DatabaseHelper.getInstance(this);

        Log.i(TAG, String.format("runYieldRead\n\t%1$s\n\trecords: %2$d"
                , (doYield ? "Yielding" : "No yield")
                , helper.getSessionCount()));

        List<Thread> threads = new ArrayList<Thread>(9);

        threads.add(new SingleTransactionRepeatedInserts(helper, sThreadCounter++, 1000, doYield));

        for(int i=0; i<5; i++)
            threads.add(new FastSelectThread(helper, sThreadCounter++, 5));

        runAllThreads(threads);

    }

    private void showTime(long start, DatabaseHelper helper)
    {
        long delta = System.currentTimeMillis() - start;
        mTvRunTime.setText(String.format("Execution time: %2$d, total rows: %1$d", helper.getSessionCount(), delta));
    }

    /**
     * Inserts number of records specified by UI
     * @param helper
     * @throws android.database.SQLException
     */
    private void performInserts(DatabaseHelper helper) throws SQLException
    {
        // @reminder UI query is ok but never perform UI updates
        String sInsert = mEtInsertCount.getText().toString();
        int numberOfInserts = Integer.getInteger(sInsert, DEFAULT_INSERTS);
        while(numberOfInserts > 0)
        {
            helper.insertSession("Count: " + numberOfInserts);
            numberOfInserts--;
        }
    }

    class SingleTransactionRepeatedInserts extends Thread
    {
        private final String TAG = SingleTransactionRepeatedInserts.class.getSimpleName();

        private final SQLiteDatabase mDb;

        private final SqliteTransactionEndListener mTransactionListener;

        private final int mCount;

        private static final int DEFAULT_TICK_COLOR = Color.YELLOW;

        private final int mTickColor;

        private final long mDelay;

        private final boolean mYield;

        SingleTransactionRepeatedInserts(DatabaseHelper helper, int id, int insertCount
                , SqliteTransactionEndListener sqliteTransactionListener) {
            this(helper, id, insertCount, sqliteTransactionListener, 0);
        }
        SingleTransactionRepeatedInserts(DatabaseHelper helper, int id, int insertCount, boolean enableYield) {
            this(helper, id, insertCount, null, DEFAULT_TICK_COLOR, 0, enableYield);
        }
        SingleTransactionRepeatedInserts(DatabaseHelper helper, int id, int insertCount
                , SqliteTransactionEndListener sqLiteTransactionListener, long delay) {
            this(helper, id, insertCount, sqLiteTransactionListener, DEFAULT_TICK_COLOR, delay, false);
        }
        SingleTransactionRepeatedInserts(DatabaseHelper helper, int id, int insertCount
                , SqliteTransactionEndListener sqLiteTransactionListener, int tickColor, long delay
                , boolean enableYield) {

            setName(String.format("%1$s-%2$d", TAG, id));

            mDb = helper.getWritableDatabase();
            mTransactionListener = sqLiteTransactionListener;
            mCount = insertCount;
            mTickColor = tickColor;
            mDelay = delay;
            mYield = enableYield;

            Log.i(getName(), helper.toString());

        }

        @Override
        public void run()
        {
            if(mDelay > 0) {
                try {
                    sleep(mDelay);
                } catch (InterruptedException e) {}
            }

            final ContentValues contentValues = new ContentValues();

            if(mTransactionListener != null)
                mDb.beginTransactionWithListener(mTransactionListener);
            else
                mDb.beginTransaction();

            Log.i(getName(), String.format("After beginTransaction, starting insert of %1$d records, total records %2$d"
                    , mCount, DatabaseHelper.getInstance(SqliteDbX.this).getSessionCount()));

            for(int i=0;i<mCount;i++) {

                contentValues.put(DatabaseHelper.SessionTable.DESCRIPTION, getName() + i);

                mDb.insertOrThrow(DatabaseHelper.SessionTable.NAME, null, contentValues);

                // Doesn't have to run on UI thread since tick doesn't draw
                mTbThreadTicker.tick(mTickColor, TickerBar.THIN);

                if(mYield && i % 50 == 0) {
                    mDb.yieldIfContendedSafely();
                }

            }

            Log.i(getName(), String.format("Before transaction commit, inserted %1$d records, total records %2$d"
                    , mCount, DatabaseHelper.getInstance(SqliteDbX.this).getSessionCount()));

            mDb.setTransactionSuccessful();

            if(mTransactionListener != null)
                mTransactionListener.onEnd();

            mDb.endTransaction();

            Log.i(getName(), "finished!");
        }

    }

    /**
     * Inserts records a single transaction at a time with a pause during transactions
     */
    class SlowInsertThread extends Thread
    {
        private final String TAG = SlowInsertThread.class.getSimpleName();

        private final SQLiteDatabase mDb;
        private final Handler mHandler;
        private final int mCount;

        SlowInsertThread(DatabaseHelper helper, int id) {
            this(helper, id, -1);
        }
        SlowInsertThread(DatabaseHelper helper, int id, int insertCount)
        {
            setName(String.format("%1$s-%2$d", TAG, id));

            mDb = helper.getWritableDatabase();
            mHandler = new Handler();
            mCount = insertCount > 0 ? insertCount : 10;
        }

        @Override
        public void run()
        {
            int count = 0;
            while(count++ < mCount)
            {
                final ContentValues contentValues = new ContentValues();

                mDb.beginTransaction();

                contentValues.put(DatabaseHelper.SessionTable.DESCRIPTION, getName() + count);

                Log.i(getName(), "inserting record");
                mDb.insertOrThrow(DatabaseHelper.SessionTable.NAME, null, contentValues);

                Log.i(getName(), "start wait");
                try
                {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                Log.i(getName(), "end wait");

                mDb.setTransactionSuccessful();
                mDb.endTransaction();

                count++;
            }

            Log.i(getName(), "finished!");
        }
    }

    /**
     * Performs a query on all DB records with a pause between selections
     */
    class FastSelectThread extends Thread
    {
        private final String TAG = FastSelectThread.class.getSimpleName();

        private final DatabaseHelper mHelper;
        private final Handler mHandler;
        private final int mCount;

        FastSelectThread(DatabaseHelper helper, int id) {
            this(helper, id, -1);
        }
        FastSelectThread(DatabaseHelper helper, int id, int selectCount)
        {
            setName(String.format("%1$s-%2$d", TAG, id));

            mHelper = helper;
            mHandler = new Handler();
            mCount = selectCount > 0 ? selectCount : 50;

            Log.i(getName(), helper.toString());
        }

        @Override
        public void run()
        {
            int count = 0;
            while(count < mCount)
            {
                mHelper.loadAllSessions();
                Log.i(getName(), "selected records");

                Log.i(getName(), "start wait");
                try
                {
                    Thread.sleep(200);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }

                Log.i(getName(), "end wait");

                count++;
            }

            Log.i(getName(), "finished!");
        }
    }

    /**
     * Executes all threads from another thread printing summary when all threads are complete
     * @param allThreads
     */
    private void runAllThreads(final List<Thread> allThreads)
    {
        new Thread(new Runnable()
        {
            public void run()
            {

            for (Thread thread : allThreads)
            {
                thread.start();
            }

            // Wait for all threads to complete before running
            for (Thread thread : allThreads)
            {
                try
                {
                    thread.join();
                    Log.i(thread.getName(), "collected");
                }
                catch (InterruptedException e)
                {
                    Log.e(TAG, "Interrupted", e);
                }
            }

            uiHandler.post(new Runnable()
            {
                public void run()
                {
                mTvResults.setText(String.format("Inserted %1$d", allCount.get()));
                }
            });

            Log.i(TAG, "All threads finished!");

            }
        }).start();
    }

    class DbInsertThread extends Thread
    {
        private final String TAG = DbInsertThread.class.getSimpleName();

        private final DatabaseHelper mDbHelper;
        private final TransactionEndListener mEndListener;
        private int mRunCount;

        private final int mColor;

        DbInsertThread(DatabaseHelper helper, int runCount, int id) {
            this(helper, runCount, id, null, Color.TRANSPARENT);
        }
        DbInsertThread(DatabaseHelper helper, int runCount, int id, TransactionEndListener endListener, int color)
        {
            setName(String.format("%1$s-%2$d", TAG, id));

            mDbHelper = helper;
            mEndListener = endListener;
            mRunCount = runCount;
            mColor = color;

            Log.i(getName(), helper.toString());
        }

        @Override
        public void run()
        {
            Random random = new Random();

            for(int i=0; i< mRunCount; i++)
            {
                if(i % 10 == 0)
                    Log.i(getName(), "writing...");

                try
                {
                    mDbHelper.insertSession("heyo - " + random.nextInt(12345678));
                    allCount.incrementAndGet();
                    mTbThreadTicker.tick(mColor);
                }
                catch (Exception e)
                {
                    Log.e(getName(), "Insert failed!!!, stopping writes", e);
                    break;
                }
            }

            if(mEndListener != null) {
                mEndListener.onEnd();
            }

            Log.i(getName(), "finished!");

        }
    }
}
