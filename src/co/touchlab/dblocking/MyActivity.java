package co.touchlab.dblocking;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.example.R;

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
