const statusIndicator = document.getElementById('status-indicator');
const statusTitle = document.getElementById('status-title');
const statusMessage = document.getElementById('status-message');
const lastChecked = document.getElementById('last-checked');
const systemInfo = document.getElementById('system-info');
const configurationInfo = document.getElementById('configuration-info');
const refreshButton = document.getElementById('refresh');

let requestInProgress = false;
let refreshTimer;

function renderMap(container, values) {
  container.replaceChildren();
  const entries = Object.entries(values);

  if (entries.length === 0) {
    const emptyState = document.createElement('div');
    emptyState.className = 'empty-state';
    emptyState.textContent = 'Нет данных.';
    container.append(emptyState);
    return;
  }

  for (const [key, value] of entries) {
    const item = document.createElement('div');
    item.className = 'info-item';

    const label = document.createElement('div');
    label.className = 'info-label';
    label.textContent = key;

    const content = document.createElement('div');
    content.className = 'info-value';
    content.textContent = value ?? '—';

    item.append(label, content);
    container.append(item);
  }
}

async function refreshStatus() {
  if (requestInProgress) {
    return;
  }

  requestInProgress = true;
  refreshButton.disabled = true;
  statusIndicator.className = 'status-indicator checking';
  statusTitle.textContent = 'Проверка daemon...';
  statusMessage.textContent = '127.0.0.1:18181';

  try {
    const result = await window.dropfile.getDaemonInfo();
    lastChecked.textContent = `Обновлено: ${new Date().toLocaleTimeString()}`;

    if (!result.success) {
      statusIndicator.className = 'status-indicator offline';
      statusTitle.textContent = 'Daemon недоступен';
      statusMessage.textContent = result.error;
      return;
    }

    statusIndicator.className = 'status-indicator online';
    statusTitle.textContent = 'Daemon работает';
    statusMessage.textContent = 'Соединение установлено';
    renderMap(systemInfo, result.data.systemInfo);
    renderMap(configurationInfo, result.data.configuration);
  } catch {
    statusIndicator.className = 'status-indicator offline';
    statusTitle.textContent = 'Не удалось получить статус';
    statusMessage.textContent = 'Ошибка связи с локальным приложением.';
    lastChecked.textContent = `Обновлено: ${new Date().toLocaleTimeString()}`;
  } finally {
    requestInProgress = false;
    refreshButton.disabled = false;
  }
}

refreshButton.addEventListener('click', refreshStatus);
refreshStatus();
refreshTimer = window.setInterval(refreshStatus, 5000);

window.addEventListener('beforeunload', () => {
  window.clearInterval(refreshTimer);
});
