const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('dropfile', {
  getDaemonInfo: () => ipcRenderer.invoke('daemon:get-info')
});
