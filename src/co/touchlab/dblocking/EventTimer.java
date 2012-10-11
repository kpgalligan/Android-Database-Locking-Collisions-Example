package co.touchlab.dblocking;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: qui
 * Date: 10/10/12
 * Time: 4:51 PM
 * To change this template use File | Settings | File Templates.
 */
public final class EventTimer {

    public final int expectedEvents;

    private long mStartTime;
    private List<Long> mTickTimes;
    private long mEndTime;

    public EventTimer(int expectedEvents) {
        this.expectedEvents = expectedEvents;
        mStartTime = 0;
        mEndTime = 0;
    }

    public long start() {
        mStartTime = System.currentTimeMillis();
        return mStartTime;
    }

    public long tick() {
        long tick = System.currentTimeMillis();
        mTickTimes.add(tick);
        return tick;
    }

    public long stop() {
        mEndTime = System.currentTimeMillis();

        if(mOnEventListener != null) {
            mOnEventListener.onStop(mEndTime - mStartTime);
        }

        return mEndTime;
    }

    public long delta() {
        return mEndTime - mStartTime;
    }

    public long[] tickDeltas() {
        final int S = mTickTimes.size();
        long[] deltas = new long[S];
        for(int i=0; i<S; i++)
            deltas[i] = mTickTimes.get(i) - mStartTime;
        return deltas;
    }

    private OnEventListener mOnEventListener;
    public void setOnEventListener(OnEventListener onEventListener) {
        mOnEventListener = onEventListener;
    }

    public interface OnEventListener {
        public void onStop(long delta);
    }

}