const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('node:path');
const { pathToFileURL } = require('node:url');

const DAEMON_INFO_URL = 'http://127.0.0.1:18181/api/daemon/info';
const REQUEST_TIMEOUT_MS = 1000;

let mainWindow;

function isStringMap(value) {
  return value !== null
    && typeof value === 'object'
    && !Array.isArray(value)
    && Object.values(value).every(item => item === null || typeof item === 'string');
}

function isDaemonInfo(value) {
  return value !== null
    && typeof value === 'object'
    && isStringMap(value.systemInfo)
    && isStringMap(value.configuration);
}

ipcMain.handle('daemon:get-info', async event => {
  if (!mainWindow
      || event.sender !== mainWindow.webContents
      || event.senderFrame !== mainWindow.webContents.mainFrame) {
    throw new Error('Untrusted IPC sender');
  }

  try {
    const response = await fetch(DAEMON_INFO_URL, {
      signal: AbortSignal.timeout(REQUEST_TIMEOUT_MS)
    });

    if (!response.ok) {
      return {
        success: false,
        error: `Daemon вернул HTTP ${response.status}.`
      };
    }

    const daemonInfo = await response.json();
    if (!isDaemonInfo(daemonInfo)) {
      return {
        success: false,
        error: 'Daemon вернул ответ неожиданного формата.'
      };
    }

    return { success: true, data: daemonInfo };
  } catch (error) {
    const timedOut = error && error.name === 'TimeoutError';
    return {
      success: false,
      error: timedOut
        ? 'Daemon не ответил за отведённое время.'
        : 'Не удалось подключиться к daemon на 127.0.0.1:18181.'
    };
  }
});

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 900,
    height: 650,
    minWidth: 640,
    minHeight: 480,
    title: 'DropFile',
    backgroundColor: '#0b1020',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true
    }
  });

  mainWindow.loadFile(path.join(__dirname, 'index.html'));
  mainWindow.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  mainWindow.webContents.on('will-navigate', (event, url) => {
    if (url !== pathToFileURL(path.join(__dirname, 'index.html')).href) {
      event.preventDefault();
    }
  });
  mainWindow.on('closed', () => {
    mainWindow = undefined;
  });
}

app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});
