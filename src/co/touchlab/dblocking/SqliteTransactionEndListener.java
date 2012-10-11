package co.touchlab.dblocking;

import android.database.sqlite.SQLiteTransactionListener;

/**
 * Created with IntelliJ IDEA.
 * User: qui
 * Date: 10/10/12
 * Time: 5:34 PM
 * To change this template use File | Settings | File Templates.
 */
public interface SqliteTransactionEndListener extends SQLiteTransactionListener {

    public static final int BEGIN = 0;
    public static final int COMMIT = BEGIN + 1;
    public static final int ROLLBACK = COMMIT + 1;
    public static final int END = ROLLBACK + 1;

    public void onEnd();
}
