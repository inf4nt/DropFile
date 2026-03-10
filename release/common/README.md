Supports only x64. Windows, Linux, MacOS

Installation example. Linux/MacOS
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