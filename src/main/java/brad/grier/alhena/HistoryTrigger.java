

package brad.grier.alhena;



import java.sql.Connection;
import java.sql.SQLException;

import org.h2.api.Trigger;

public class HistoryTrigger implements Trigger {

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {

        String insertedUrl = (String) newRow[1];
        DB.markUrlRead(insertedUrl, true);
    }

    @Override
    public void init(Connection conn, String schemaName, String triggerName,
                     String tableName, boolean before, int type) {}

    @Override
    public void close() {}

    @Override
    public void remove() {}
}