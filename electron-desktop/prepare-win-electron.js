const { downloadArtifact } = require('@electron/get');
const fs = require('node:fs/promises');
const path = require('node:path');

async function prepareWindowsElectron() {
  const version = require('electron/package.json').version;
  const archive = await downloadArtifact({
    version,
    platform: 'win32',
    arch: 'x64',
    artifactName: 'electron'
  });

  const distDirectory = path.join(__dirname, 'electron-dist');
  const archiveName = `electron-v${version}-win32-x64.zip`;
  await fs.mkdir(distDirectory, { recursive: true });
  await fs.copyFile(archive, path.join(distDirectory, archiveName));
}

prepareWindowsElectron().catch(error => {
  console.error('Failed to download the verified Windows Electron runtime:', error);
  process.exitCode = 1;
});
