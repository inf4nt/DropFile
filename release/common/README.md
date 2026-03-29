# Installation
The dropfile supports x64 Windows, Linux, Macos. 

#### Windows
Add DROPFILE_HOME env var and update the PATH. See Linux/MacOS

#### Linux/MacOS
The executable files ``/bin/dropfile and /bin/dropfile-daemon`` may ask the permissions to execute
1. Unzip project ``/home/user/dropfile-linux``
2. Add env var
```
export DROPFILE_HOME=/home/user/dropfile-linux
```
3. Add ``DROPFILE_HOME`` to ``PATH``
```
export $PATH=$PATH:DROPFILE_HOME/bin
```
4. Go to the command line
```
$ dropfile
```
5. Result 
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

# Limitations

Supports only x64. Windows, Linux, MacOS

# Examples
First of all it's necessary to start its daemon
```
$ dropfile daemon start
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