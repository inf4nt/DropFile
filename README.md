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
                                --id 
                                --alias 
                                --directory=/home/example/test.txt, dir_alias, dir_id
                        cat --remote(optional)=finger_print --id --alias
                        info --remote(optional)=finger_print --id --alias --all
app daemon
                info
                status
                shutdown
                start
app share files 
                ls
                add  --alias=text.txt
                rm   --id --all
                info --id --all
app downloads files
                    ls --directory(optional)=dir_alias,dir_id
                    rm --id
                    cat --id
                    info --id
              directory
                        pin --id --alias=photos
                        current
                        add --alias=photos --path=/home/example/download
                        ls 
                        rm --id --alias=photos