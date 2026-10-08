package arnav.issuelite;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.IExitCodeGenerator;
import picocli.CommandLine.ITypeConverter;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

enum Operation
{
    INVALID,
    CREATE_PROJECT,
    SELECT_PROJECT,
    NEW_ISSUE,
}

class OperationConverter implements ITypeConverter<Operation>
{
    public Operation convert(String value) {
        switch (value)
        {
            case "create":
                return Operation.CREATE_PROJECT;
            case "select":
                return Operation.SELECT_PROJECT;
            case "new":
                return Operation.NEW_ISSUE;
            default:
                return Operation.INVALID;
        }
    }
}

@Command(name = "issuelite")
class IssueLite implements Runnable, IExitCodeGenerator
{
    String projectName;
    int exitCode = 1;
    static final String dbDir = "jdbc:sqlite:~/.config/issuelite/";

    @Option(names = { "-i", "--interactive" }, description = "use in interactive mode")
    boolean interactive = false;

    @Parameters(index = "0", description = "operation type", converter = OperationConverter.class)
    Operation operationType = Operation.INVALID;

    @Parameters(index = "1", description = "name of item to perform operation on")
    String identifier;

    @Override
    public void run()
    {
        projectName = System.getenv("ISSUELITE_SELECTED_PROJECT");
        switch (operationType)
        {
            case INVALID:
                exitCode = 1;
                break;
            case CREATE_PROJECT:
                projectName = "identifier";
                try
                {
                    Connection conn = DriverManager.getConnection(dbDir+identifier+".db");
                    conn.getMetaData();
                }
                catch (SQLException e)
                {
                    System.out.println("Failed to connect to issue database: " + e.getMessage());
                    exitCode = 1;
                    break;
                }
                exitCode = 0;
                break;
            case SELECT_PROJECT:
                ProcessBuilder pb = new ProcessBuilder();
                pb.command("ISSUELITE_SELECTED_PROJECT=%s".formatted(identifier));
                pb.command("PROMPT=(%s) $".formatted(identifier));
                exitCode = 0;
                break;
            case NEW_ISSUE:
                try (Connection dbConnection = DriverManager.getConnection(dbDir+projectName+".db");
                     Statement stmt = dbConnection.createStatement())
                {
                    if (projectName == null)
                    {
                        System.out.println("A project needs to be selected.");
                        exitCode = 1;
                        break;
                    }
                    exitCode = 0;
                }
                catch (SQLException e)
                {
                    System.out.println("Failed to connect to issue database: " + e.getMessage());
                    exitCode = 1;
                }
                break;
        }
    }

    @Override
    public int getExitCode()
    {
        return exitCode;
    }
}

public class App
{
    public static void main(String[] args)
    {
        System.exit(new CommandLine(new IssueLite()).execute(args));
    }
}
