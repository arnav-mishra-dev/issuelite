package arnav.issuelite;

import java.io.File;
import java.util.UUID;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.IExitCodeGenerator;
import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@Command(name = "issuelite")
class IssueLite implements Runnable, IExitCodeGenerator
{
    int exitCode = 1;
    static final String dbDir = "%s/.config/issuelite/".formatted(System.getProperty("user.home"));

    // @Option(names = { "-i", "--interactive" }, description = "use in interactive mode")
    // boolean interactive = false;

    @Option(names = { "-n", "--new" }, description = "Create new issue")
    String issueName;

    @Option(names = { "-t", "--target" }, description = "Select a target project")
    String targetProject;

    @Option(names = { "-c", "--create" }, description = "Create new project")
    boolean createProject;

    @Option(names = { "-f", "--finish" }, description = "Mark issue as finished")
    String finishedIssue;

    @Option(names = { "-s", "--show" }, description = "Show project issues")
    boolean showIssues;

    @Override
    public void run()
    {
        String databaseUrl = "jdbc:sqlite:"+dbDir+targetProject+".db";

        if (createProject)
        {
            new File(dbDir).mkdirs();
            try
            {
                DriverManager.getConnection(databaseUrl);
            }
            catch (SQLException e)
            {
                System.out.println(databaseUrl);
                System.out.println("Failed to connect to issue database: " + e.getMessage());
                exitCode = 1;
                return;
            }
            exitCode = 0;
        }
        if (issueName != null)
        {
            try (Connection dbConnection = DriverManager.getConnection(databaseUrl);
                 Statement stmt = dbConnection.createStatement())
            {
                stmt.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS issues (
                        id TEXT PRIMARY KEY,
                        issue_name TEXT NOT NULL,
                        completed BOOLEAN NOT NULL DEFAULT 0
                    )
                """);

                try (PreparedStatement pstmt = dbConnection.prepareStatement(
                        "INSERT INTO issues (id, issue_name, completed) VALUES (?, ?, 0)"))
                {
                    pstmt.setString(1, UUID.randomUUID().toString());
                    pstmt.setString(2, issueName);
                    pstmt.executeUpdate();
                }
                exitCode = 0;
            }
            catch (SQLException e)
            {
                System.out.println("Failed to connect to issue database: " + e.getMessage());
                exitCode = 1;
                return;
            }
        }

        if (finishedIssue != null)
        {
            try (Connection dbConnection = DriverManager.getConnection(databaseUrl);
                 PreparedStatement pstmt = dbConnection.prepareStatement(
                     "UPDATE issues SET completed = 1 WHERE id = ?"))
            {
                pstmt.setString(1, finishedIssue);

                int updated = pstmt.executeUpdate();

                if (updated == 0)
                {
                    System.out.println("Issue not found: " + finishedIssue);
                    exitCode = 1;
                    return;
                }

                exitCode = 0;
            }
            catch (SQLException e)
            {
                System.out.println("Failed to connect to issue database: " + e.getMessage());
                exitCode = 1;
                return;
            }
        }

        if (showIssues)
        {
            if (targetProject == null)
            {
                System.out.println("No target project specified");
                exitCode = 1;
                return;
            }
            try (Connection dbConnection = DriverManager.getConnection(databaseUrl);
                 PreparedStatement pstmt = dbConnection.prepareStatement(
                     "SELECT id, issue_name, completed FROM issues");
                 ResultSet rs = pstmt.executeQuery())
            {
                while (rs.next())
                {
                    String id = rs.getString("id");
                    String name = rs.getString("issue_name");
                    boolean completed = rs.getBoolean("completed");

                    System.out.printf("%s [%s] %s%n",
                            id,
                            completed ? "finished" : "open",
                            name);
                }

                exitCode = 0;
            }
            catch (SQLException e)
            {
                System.out.println("Failed to connect to issue database: " + e.getMessage());
                exitCode = 1;
                return;
            }
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
