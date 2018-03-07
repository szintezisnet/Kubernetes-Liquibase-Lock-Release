package liquibase.lockservice.ext.ext;

import hu.gyuuu.liquibasekubernetes.KubernetesConnector;
import liquibase.database.Database;
import liquibase.datatype.DataTypeFactory;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.sqlgenerator.core.LockDatabaseChangeLogGenerator;
import liquibase.statement.core.LockDatabaseChangeLogStatement;
import liquibase.statement.core.UpdateStatement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Timestamp;

public class LockDatabaseChangeLogGeneratorKubernetes extends LockDatabaseChangeLogGenerator {
    private static final Log LOG = LogFactory.getLog(LockDatabaseChangeLogGeneratorKubernetes.class);

    @Override
    public int getPriority() {
        return 1000;
    }

    @Override
    public boolean supports(LockDatabaseChangeLogStatement statement, Database database) {
        return KubernetesConnector.getInstance().isConnected();
    }

    @Override
    public Sql[] generateSql(LockDatabaseChangeLogStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        String liquibaseSchema = database.getLiquibaseSchemaName();
        String liquibaseCatalog = database.getLiquibaseCatalogName();

        UpdateStatement updateStatement = new UpdateStatement(liquibaseCatalog, liquibaseSchema, database.getDatabaseChangeLogLockTableName());
        updateStatement.addNewColumnValue("LOCKED", true);
        updateStatement.addNewColumnValue("LOCKGRANTED", new Timestamp(new java.util.Date().getTime()));
        updateStatement.addNewColumnValue("LOCKEDBY", String.format("%s:%s", KubernetesConnector.getInstance().getPodNamespace(), KubernetesConnector.getInstance().getPodName()));
        updateStatement.setWhereClause(database.escapeColumnName(liquibaseCatalog, liquibaseSchema, database.getDatabaseChangeLogTableName(), "ID") + " = 1 AND " + database.escapeColumnName(liquibaseCatalog, liquibaseSchema, database.getDatabaseChangeLogTableName(), "LOCKED") + " = " + DataTypeFactory.getInstance().fromDescription("boolean", database).objectToSql(false, database));

        return SqlGeneratorFactory.getInstance().generateSql(updateStatement, database);
    }
}
