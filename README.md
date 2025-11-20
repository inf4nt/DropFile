# Getting Started

app connect 127.0.0.1:8081

app connection status -> shows current connection

app disconnect

app files /home/dir

app download /home/dir/text.txt

app start

app status -> is daemon running ?

app shutdown

app nodes

v2

app connections connect --id=test 127.0.0.1:8081 -> Connected id=test address=127.0.0.1:8081 

app connections connect 127.0.0.1:8081 -> Connected id=a12wqeq address=127.0.0.1:8081

app connections online -> shows connections and online 

app connections disconnect --all -> disconnect all connections

app connections disconnect 127.0.0.1:8081

app connections disconnect --id=aqwd

[//]: # (app connections list online -> shows online nodes [)

[//]: # (                                                    'online': ['aqwd': '127.0.0.1:8081'],   )

[//]: # (                                                    'online': ['aqwd': '127.0.0.1:8081'],)

[//]: # (                                                    'offline': ['1ghe': '192.168.0.1:8081'])

[//]: # (                                                  ])

app files --id=qa12e --path /home/dir

app download add --id=qa12e --path /home/dir/text.xml -> returns ok immediately

app download stop --all

app download stop --id=1234

app download remove --id=1234

app download list -> shows current downloading  [[Downloading, /home/dir/text.xml, 'aqwd', '127.0.0.1:8081'] ]
[Id(Its id), Status(In progress, Complete), Progess(in percentages), 
                    FileName(test.txt), ConnectionId, ConnectionAddress] 
-> not sure needs to show connection information because it can be downloaded from 2+ nodes.
It must be because app downloads files from the given address

app daemon status -> Online/Offline

app daemon start -> OK

app daemon shutdown -> OK