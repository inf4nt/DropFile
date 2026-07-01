@echo off
docker run --rm ^
  -v dropfile-daemon-conf:/daemon-conf ^
  -e DROPFILE_DAEMON_HOST=host.docker.internal ^
  -e DROPFILE_DAEMON_PORT=28282 ^
  -e DROPFILE_DAEMON_DAEMON-SECRETS_DIRECTORY=/daemon-conf ^
  -e DROPFILE_DAEMON_INSTALLATION-SEED_DIRECTORY=/daemon-conf ^
  dropfile-cli %*
