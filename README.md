# Issuelite
Issuelite is a CLI issue tracker designed for simplicity and speed when working as a solo developer.

## Compilation
1. Clone this repository with `git clone https://github.com/arnav-mishra-dev/issuelite.git`.
2. Ensure you have maven installed and available on `PATH`.
3. Go to the cloned repository path in the terminal.
4. Run `mvn clean compile assembly:single`.
5. Use `java -jar target/issuelite-1.0-jar-with-dependencies.jar` to run the program.
6. I recommend aliasing the command to 'issuelite'. On linux this can be done with `alias issuelite="java -jar target/issuelite-1.0-jar-with-dependencies.jar"`. On windows this can be done with `doskey issuelite="java -jar target/issuelite-1.0-jar-with-dependencies.jar"`

> Note: The program has not been tested on windows, if you experience any issues you can attempt to translate the commands to the windows equivalent yourself.

## Usage

```
Usage: issuelite [-cis] [-f=<finishedIssue>] [-n=<issueName>]
                 [-t=<targetProject>]
  -c, --create                  create new project
  -f, --finish=<finishedIssue>  mark issue as finished
  -i, --interactive             use in interactive mode
  -n, --new=<issueName>         create new issue
  -s, --show                    show project issues
  -t, --target=<targetProject>  select a target project
```

Running `issuelite -i` or `issuelite --interactive` runs issuelite in interactive mode. The following commands are available in interactive mode:
- `select [project name]`: selects a created project by name
- `create [project name]`: creates a project with the given name
- `new [issue name]`: creates a new issue in the selected project
- `finish [issue UUID]`: finds an issue in the selected project by UUID and marks it as finished
- `show`: shows all of the issues, their UUIDs and completion states for the selected project
