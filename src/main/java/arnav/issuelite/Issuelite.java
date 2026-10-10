package arnav.issuelite;

import java.io.File;
import java.util.Scanner;
import java.util.UUID;
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
class Issuelite implements Runnable, IExitCodeGenerator
{
    private int exitCode = 1;
    static final String dbDir = "%s/.config/issuelite/".formatted(System.getProperty("user.home"));

    @Option(names = { "-i", "--interactive" }, description = "use in interactive mode")
    private boolean interactive = false;

    @Option(names = { "-n", "--new" }, description = "Create new issue")
    private String issueName;

    @Option(names = { "-t", "--target" }, description = "Select a target project")
    private String targetProject;

    @Option(names = { "-c", "--create" }, description = "Create new project")
    private boolean createProject;

    @Option(names = { "-f", "--finish" }, description = "Mark issue as finished")
    private String finishedIssue;

    @Option(names = { "-s", "--show" }, description = "Show project issues")
    private boolean showIssues;

    @Override
    public void run()
    {
        if (interactive)
        {
            RunInteractive();
        }
        else
        {
            RunDirect();
        }
    }

    private void RunInteractive()
    {
        String prompt = "[issuelite]$ ";
        Connection dbConnection = null;
        boolean runningInteractive = true;
        Scanner sc = new Scanner(System.in);
        while (runningInteractive)
        {
            System.out.print(prompt);
            String rawCommand = sc.nextLine();
            String[] command = rawCommand.trim().split("\\s+");

            switch (command[0])
            {
                case "select":
                {
                    String databaseUrl = "jdbc:sqlite:"+dbDir+command[1]+".db";
                    try
                    {
                        dbConnection = DriverManager.getConnection(databaseUrl);
                        System.out.printf("Connected to database %s.%n", command[1]);
                        prompt = String.format("(%s) %s", command[1], prompt);
                    }
                    catch (SQLException e)
                    {
                        System.out.printf("Failed to connect to issue database: %s%n", e.getMessage());
                        continue;
                    }
                    break;
                }
                case "create":
                {
                    String databaseUrl = "jdbc:sqlite:"+dbDir+command[1]+".db";
                    new File(dbDir).mkdirs();
                    try
                    {
                        DriverManager.getConnection(databaseUrl);
                        System.out.println("Successfully created project.");
                    }
                    catch (SQLException e)
                    {
                        System.out.println(databaseUrl);
                        System.out.printf("Failed to connect to issue database: %s%n", e.getMessage());
                        continue;
                    }
                    break;
                }
                case "new":
                {
                    if (dbConnection == null)
                    {
                        System.out.println("Please select a project first.");
                        continue;
                    }
                    try (Statement stmt = dbConnection.createStatement())
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
                            pstmt.setString(2, command[1]);
                            pstmt.executeUpdate();
                        }
                    }
                    catch (SQLException e)
                    {
                        System.out.printf("Failed to connect to issue database: %s%n", e.getMessage());
                        continue;
                    }
                    break;
                }
                case "finish":
                {
                    try (PreparedStatement pstmt = dbConnection.prepareStatement(
                             "UPDATE issues SET completed = 1 WHERE id = ?"))
                    {
                        pstmt.setString(1, finishedIssue);

                        int updated = pstmt.executeUpdate();

                        if (updated == 0)
                        {
                            System.out.println("Issue not found: " + finishedIssue);
                            continue;
                        }
                    }
                    catch (SQLException e)
                    {
                        System.out.printf("Failed to connect to issue database: %s%n", e.getMessage());
                        continue;
                    }
                    break;
                }
                case "show":
                {
                    try (PreparedStatement pstmt = dbConnection.prepareStatement(
                             "SELECT id, issue_name, completed FROM issues");
                         ResultSet rs = pstmt.executeQuery())
                    {
                        while (rs.next())
                        {
                            String id = rs.getString("id");
                            String name = rs.getString("issue_name");
                            boolean completed = rs.getBoolean("completed");

                            System.out.printf("%s | %s | [%s]%n",
                                    id,
                                    name,
                                    completed ? "finished" : "open");
                        }
                    }
                    catch (SQLException e)
                    {
                        System.out.printf("Failed to connect to issue database: %s%n", e.getMessage());
                        continue;
                    }
                    break;
                }
                case "exit":
                    exitCode = 0;
                    runningInteractive = false;
                    break;
                default:
                    break;
            }
        }
        sc.close();
    }

    private void RunDirect()
    {
        if (targetProject == null)
        {
            System.out.println("No target project specified");
            exitCode = 1;
            return;
        }

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
                System.out.printf("Failed to connect to issue database: %s%n", e.getMessage());
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
                System.out.printf("Failed to connect to issue database: %s%n", e.getMessage());
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
                System.out.printf("Failed to connect to issue database: %s%n", e.getMessage());
                exitCode = 1;
                return;
            }
        }

        if (showIssues)
        {
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

                    System.out.printf("%s | %s | [%s]%n",
                            id,
                            name,
                            completed ? "finished" : "open");
                }

                exitCode = 0;
            }
            catch (SQLException e)
            {
                System.out.printf("Failed to connect to issue database: %s%n", e.getMessage());
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

