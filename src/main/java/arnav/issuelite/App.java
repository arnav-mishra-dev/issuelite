package arnav.issuelite;

import picocli.CommandLine;

public class App
{
    public static void main(String[] args)
    {
        System.exit(new CommandLine(new Issuelite()).execute(args));
    }
}
