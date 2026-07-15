# Installation
```
There are 4 versions: x64 windows, macos, linux and portable version.
1. The portable version has no runtime and 
requires installed java 25 or higher on the host machine.
2. X64 versions include runtime and can go without any preparation
```

# Support
The application can run on any platform where Java 25 or higher exists.
Tested on: Windows 11-10 x64, WSL2, DebianX64, MacosX64, Termux(Android)

#### Windows
Add DROPFILE_HOME env var and update the PATH. See Linux/MacOS

#### Linux/MacOS
The executable files ``/bin/dropfile and /bin/dropfile-daemon`` may ask the permissions to execute
1. Unzip project ``/home/user/dropfile-linux``
2. ``tar -xzvf dropfile-linux.tar``
3. There are three ways how to work with the application: Add environment variable, symlink, execute executable script from the bin directory
4. Environment variable. Add env var and update the path. The process is similar to maven
```
#1 Environment variable
export DROPFILE_HOME=~/dropfile-linux
export PATH=$PATH:$DROPFILE_HOME/bin
```
```
#2 Symlink
Create symlink to ~/.local/bin
$ ln -sf "$HOME/dropfile-linux/bin/dropfile" "$HOME/.local/bin/dropfile"
```
```
#3 Direct execution
$ ./dropfile/bin/dropfile
```
5. Go to the command line ``$ dropfile``
6. Result 
```
░███████                                      ░████ ░██░██
░██   ░██                                    ░██       ░██
░██    ░██ ░██░████  ░███████  ░████████  ░████████ ░██░██  ░███████
░██    ░██ ░███     ░██    ░██ ░██    ░██    ░██    ░██░██ ░██    ░██
░██    ░██ ░██      ░██    ░██ ░██    ░██    ░██    ░██░██ ░█████████
░██   ░██  ░██      ░██    ░██ ░███   ░██    ░██    ░██░██ ░██
░███████   ░██       ░███████  ░██░█████     ░██    ░██░██  ░███████
                               ░██
                               ░██

Daemon host: 127.0.0.1
Daemon port: 18181
Usage: <main class> [-hV] [COMMAND]
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  connections, -c, --c  Connections
  daemon                Daemon commands
  share, -s, --s        Share commands
  download, -d, --d     Download commands
  link, -l, --l         Link commands
```
# Termux(Android)
The Termux installation requires java-25 or higher on the host(termux) machine.
Build the portable version
1. use ``$ ./full-install.sh`` which installs java-25
2. use ``$ ./nano-install.sh`` in case you already have java-25
3. Finally, you will get symlink ``$ dropfile``

# Examples
First of all it's necessary to start its daemon
```
$ dropfile daemon start
$ dropfile daemon status
```
#### Daemon commands
```
dropfile daemon
Usage: <main class> daemon [COMMAND]
Daemon commands
Commands:
  shutdown     Daemon shutdown
  status       Daemon status
  start        Daemon start
  cache-reset  Daemon cache reset
```

#### Connections
```
dropfile connections
Usage: <main class> connections [COMMAND]
Connections
Commands:
  connect, -c, --c                   Connect
  current                            Retrieve current connection
  trusted-in, --in, -in, --i, -i     Retrieve trusted-in connections
  trusted-out, --out, -out, --o, -o  Retrieve trusted-out connections
  disconnect                         Disconnect trusted-out connection
  revoke                             Drop trusted-in connection
  access, -a, --a                    Access keys command
  status                             Retrieve status of current connection
  share, -s, --s                     Share operations
  traffic                            Retrieve traffic
```
Generate access token, and use the access key via connect command

```
$ dropfile connections access generate

{
  "id" : "b451ef733318a553",
  "key" : "OVVac3h0eHU5Rzl1MUh5cQ",
  "created" : "2026-03-29 10:39:29"
}

$ dropfile connections connect 192.168.1.5:18181 OVVac3h0eHU5Rzl1MUh5cQ
```

# Docker build
The Docker build is absolutely isolated from the host machine. There is no need to install anything except docker.
Once the sources downloaded, execute script to build X64 or a portable version.
The application archive will be appeared in "DropFile/release" directory. 
dropfile-windows.tar.gz, dropfile-linux.tar.gz, dropfile-macos.tar.gz, dropfile-portable.tar.gz
Windows
```
$ ./DropFile/release/release.windows.x64.bat
```
Linux/WSL
```
$ ./DropFile/release/release.linux.x64.sh
```
MacOS
```
$ ./DropFile/release/release.macos.x64.sh
```
portable
```
$ ./DropFile/release/release.linux.x64.sh
```
