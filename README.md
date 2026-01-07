app connections connect <host>:<port>
                current
                status
                info <host>:<port>
                trusted-out -out -o
                trusted-in  -in  -i
                disconnect <fingerprint>
                revoke     <fingerprint>
                access
                        generate -permanent
                        ls
                        revoke -id=12d1as
                        revoke -all
                files
                        ls --remote(optional)=finger_print
                        download --remote(optional)=finger_print 
                                --name(optional)=text.txt
                                --id 
                                --alias 
                                --directory=/home/example/test.txt, dir_alias, dir_id
                        cat --remote(optional)=finger_print --id --alias
app daemon
            info
            status
            shutdown
            start
app share
            ls
            add  --alias=text.txt
            rm   --id --all
app files
            ls --directory(optional)=dir_alias,dir_id
            rm --id
            cat --id