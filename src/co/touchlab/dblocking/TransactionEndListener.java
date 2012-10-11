package co.touchlab.dblocking;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: qui
 * Date: 10/10/12
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class TransactionEndListener implements SqliteTransactionEndListener {
    private static final String TAG = TransactionEndListener.class.getSimpleName();

    protected final List<Integer> mEvents;

    protected final EventTimer mEventTimer;

    public TransactionEndListener(EventTimer eventTimer) {
        mEvents = new ArrayList<Integer>();
        mEventTimer = eventTimer;
    }

    @Override public void onBegin() {}

    // @debug Counts commits to compare to ends
    private int mCommitCount = 0;

    @Override
    public void onCommit() {
        // @reminder Comparison of commits vs ends
        Log.i(TAG, String.format("onCommit: %1$d, events: %2$d", ++mCommitCount, mEvents.size()));
    }

//    @Override
    public void onEnd() {
        mEvents.add(END);

        if(mEvents.size() == mEventTimer.expectedEvents) {
            mEventTimer.stop();
        }
    }

    @Override public void onRollback() {}

}
