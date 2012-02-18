package co.touchlab.dblocking;

import android.app.Activity;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
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

public class MyActivity extends Activity
{
    private AtomicInteger allCount = new AtomicInteger();
    private Handler uiHandler;


    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        uiHandler = new Handler();

        findViewById(R.id.oneHelper)
                .setOnClickListener(new View.OnClickListener()
                {

                    public void onClick(View view)
                    {
                        allCount.set(0);
                        final List<DbInsertThread> allThreads = new ArrayList<DbInsertThread>();

                        DatabaseHelper helper = new DatabaseHelper(MyActivity.this);

                        for(int i=0; i<4; i++)
                        {
                            allThreads.add(new DbInsertThread(helper, 100));
                        }

                        runAllThreads(allThreads);
                    }
                });

        findViewById(R.id.manyHelpers)
                .setOnClickListener(new View.OnClickListener()
                {

                    public void onClick(View view)
                    {
                        allCount.set(0);
                        final List<DbInsertThread> allThreads = new ArrayList<DbInsertThread>();

                        for(int i=0; i<4; i++)
                        {
                            DatabaseHelper helper = new DatabaseHelper(MyActivity.this);
                            allThreads.add(new DbInsertThread(helper, 100));
                        }

                        runAllThreads(allThreads);
                    }
                });
        
        findViewById(R.id.testTransactionIsolation).setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                DatabaseHelper helper = new DatabaseHelper(MyActivity.this);
                new SlowInsertThread(helper).start();
                new FastSelectThread(helper).start();
            }
        });
        
        findViewById(R.id.noTrans).setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                runNoTrans();
            }
        });

        findViewById(R.id.trans).setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View view)
            {
                runTrans();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void runTrans()
    {
        new AsyncTask(){

            @Override
            protected Object doInBackground(Object... objects)
            {
                DatabaseHelper instance = DatabaseHelper.getInstance(MyActivity.this);

                SQLiteDatabase writableDatabase = instance.getWritableDatabase();

                long start = System.currentTimeMillis();
                writableDatabase.beginTransaction();
                insertTest(instance);
                writableDatabase.setTransactionSuccessful();
                writableDatabase.endTransaction();

                return start;
            }

            @Override
            protected void onPostExecute(Object o)
            {
                showTime((Long)o, DatabaseHelper.getInstance(MyActivity.this));
            }
        }.execute();


    }
    
    @SuppressWarnings("unchecked")
    private void runNoTrans()
    {
        new AsyncTask(){

            @Override
            protected Object doInBackground(Object... objects)
            {
                DatabaseHelper instance = DatabaseHelper.getInstance(MyActivity.this);
                long start = System.currentTimeMillis();
                insertTest(instance);

                return start;
            }

            @Override
            protected void onPostExecute(Object o)
            {
                showTime((Long)o, DatabaseHelper.getInstance(MyActivity.this));
            }
        }.execute();
    }

    private void showTime(long start, DatabaseHelper instance)
    {
        long time = System.currentTimeMillis() - start;
        ((TextView)findViewById(R.id.timeOut)).setText("Total rows: "+ instance.countSessions() +"/time: "+ Long.toString(time));
    }

    private void insertTest(DatabaseHelper helper)
    {
        
        int numberOfInserts = Integer.parseInt(((EditText) findViewById(R.id.numberOfInserts)).getText().toString());
        while(numberOfInserts > 0)
        {
            helper.createSession("Count: "+ numberOfInserts);
            numberOfInserts--;
        }
        
    }

    class SlowInsertThread extends Thread
    {
        private DatabaseHelper helper;
        private Handler handler;

        SlowInsertThread(DatabaseHelper helper)
        {
            this.helper = helper;
            handler = new Handler();
        }

        @Override
        public void run()
        {
            int count = 0;
            while(count < 10)
            {
                SQLiteDatabase db = helper.getWritableDatabase();
                db.beginTransaction();
                final ContentValues contentValues = new ContentValues();

                contentValues.put("description", "asdlkfj");

                Log.i(getClass().getName(), "insert");
                db.insertOrThrow("session", null, contentValues);
                Log.i(getClass().getName(), "start wait");
                try
                {
                    Thread.sleep(5000);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                Log.i(getClass().getName(), "end wait");
                db.endTransaction();

                count++;
            }
        }
    }

    class FastSelectThread extends Thread
    {
        private DatabaseHelper helper;
        private Handler handler;

        FastSelectThread(DatabaseHelper helper)
        {
            this.helper = helper;
            handler = new Handler();
        }

        @Override
        public void run()
        {
            int count = 0;
            while(count < 100)
            {
                helper.loadAllSessions();
                Log.i(getClass().getName(), "selected");

                Log.i(getClass().getName(), "start wait");
                try
                {
                    Thread.sleep(500);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                Log.i(getClass().getName(), "end wait");

                count++;
            }
        }
    }

    private void runAllThreads(final List<DbInsertThread> allThreads)
    {
        new Thread(new Runnable()
        {
            public void run()
            {
                for (DbInsertThread allThread : allThreads)
                {
                    allThread.start();
                }
                for (DbInsertThread thread : allThreads)
                {
                    try
                    {
                        thread.join();
                    }
                    catch (InterruptedException e)
                    {
                    }
                }

                uiHandler.post(new Runnable()
                {
                    public void run()
                    {
                        ((TextView)findViewById(R.id.results)).setText("Inserted "+ allCount.get());
                    }
                });
            }
        }).start();
    }

    class DbInsertThread extends Thread
    {
        private DatabaseHelper helper;
        private int runCount;

        DbInsertThread(DatabaseHelper helper, int runCount)
        {
            this.helper = helper;
            this.runCount = runCount;
        }

        @Override
        public void run()
        {
            Random random = new Random();
            for(int i=0; i<runCount; i++)
            {
                if(i % 10 == 0)
                    Log.i("asdf", "Writing");

                try
                {
                    helper.createSession("heyo - "+ random.nextInt(12345678));
                    allCount.incrementAndGet();
                }
                catch (Exception e)
                {
                    //Insert failed!!!
                    Log.e("asdf", "Insert failed!!!");
                }
            }
        }
    }
}
