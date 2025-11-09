const API = '/api';
let me = null;
let contacts = [];
let current = null;
let isSending = false;
let lastMessageId = null;
let pollingInterval = null;
let lastMessagesHash = null;

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

async function apiGet(path) {
  const r = await fetch(API + path);
  if (!r.ok) throw new Error(`HTTP ${r.status}: ${r.statusText}`);
  return r.json();
}

async function apiPost(path, body) {
  const r = await fetch(API + path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  });
  const data = await r.json();
  if (!r.ok) {
    const errorMsg = data.error || `HTTP ${r.status}: ${r.statusText}`;
    throw new Error(errorMsg);
  }
  return data;
}

async function checkForUpdates() {
  if (!me) return;

  try {
    if (current) {
      const result = await apiGet(`/check-updates?user=${encodeURIComponent(me)}&lastMessageId=${lastMessageId || ''}&chatType=${current.type}&chatName=${encodeURIComponent(current.name)}`);
      
      if (result.hasUpdates) {
        console.log(`🔄 ${result.newMessagesCount} mensaje(s) nuevo(s) detectado(s) en el chat actual`);
        await loadMessages();
      }
    } else {
      await apiGet(`/check-updates?user=${encodeURIComponent(me)}`);
    }
  } catch (error) {
    console.error('❌ Error verificando actualizaciones:', error);
  }
}

function startPolling() {
  if (pollingInterval) {
    clearInterval(pollingInterval);
  }
  
  pollingInterval = setInterval(checkForUpdates, 2000);
  console.log('✅ Polling HTTP iniciado (cada 2 segundos)');
}

function stopPolling() {
  if (pollingInterval) {
    clearInterval(pollingInterval);
    pollingInterval = null;
    console.log('⏸️ Polling HTTP detenido');
  }
}

async function start() {
  me = prompt('Tu nombre de usuario:');
  if (!me) {
    alert('Se requiere un nombre de usuario');
    return;
  }
  
  try {
    await apiPost('/register', { username: me });
    console.log(`✅ Usuario registrado: ${me}`);

    document.querySelector('.sidebar-header h2').innerHTML = `ChatApp<br><small style="font-size:12px; font-weight:400; color:#65676b;">👤 ${me}</small>`;

    startPolling();

    await loadContacts();

    setInterval(loadContacts, 5000);
  } catch (error) {
    console.error('❌ Error al iniciar:', error);
    alert('Error al conectar con el servidor');
  }
}

async function loadContacts() {
  try {
    const users = await apiGet('/users?user=' + encodeURIComponent(me));
    const groups = await apiGet('/groups?user=' + encodeURIComponent(me));
    
    contacts = [];
    users.forEach(u => contacts.push({ type: 'user', name: u }));
    groups.forEach(g => contacts.push({ 
      type: 'group', 
      name: g.name, 
      members: g.members || [] 
    }));
    
    renderContacts();
  } catch (error) {
    console.error('❌ Error cargando contactos:', error);
  }
}

function renderContacts() {
  const container = document.getElementById('contacts');
  container.innerHTML = '';
  contacts.forEach(c => {
    const el = document.createElement('div');
    el.className = 'contact' + (current && current.type === c.type && current.name === c.name ? ' active' : '');
    el.textContent = c.type === 'group' ? '👥 ' + c.name : '💬 ' + c.name;
    el.onclick = () => selectContact(c);
    container.appendChild(el);
  });
}

async function selectContact(c) {
  current = c;
  renderContacts();

  const header = document.getElementById('chatHeader');
  if (c.type === 'group') {
    const memberCount = c.members ? c.members.length : 0;
    const memberNames = c.members ? c.members.map(m => {
      const username = m.username || m;
      return username === me ? 'Me' : username;
    }).join(', ') : '';
    header.innerHTML = `
      <div class="chat-title">👥 ${c.name}</div>
      <div class="chat-subtitle">${memberCount} miembros: ${memberNames}</div>
    `;
  } else {
    header.innerHTML = `
      <div class="chat-title">💬 ${c.name}</div>
      <div class="chat-subtitle">Chat privado</div>
    `;
  }

  lastMessagesHash = null;
  await loadMessages();
}

async function loadMessages() {
  if (!current) return;

  try {
    const msgs = await apiGet(`/messages/${current.type}/${encodeURIComponent(current.name)}?user=${encodeURIComponent(me)}`);
    await renderMessages(msgs);
    
    if (msgs.length > 0) {
      lastMessageId = msgs[msgs.length - 1].id;
    }
  } catch (error) {
    console.error('❌ Error cargando mensajes:', error);
  }
}

async function renderMessages(msgs) {
  const box = document.getElementById('messages');
  if (!box) return;

  const currentHash = JSON.stringify(msgs.map(m => ({
    id: m.id,
    content: m.content || m.text || '',
    sender: m.sender?.username || m.sender,
    dateTime: m.dateTime
  })));

  if (currentHash === lastMessagesHash) {
    return;
  }

  lastMessagesHash = currentHash;

  const wasAtBottom = box.scrollHeight - box.scrollTop <= box.clientHeight + 50;
  const previousScrollHeight = box.scrollHeight;

  box.innerHTML = '';

  msgs.forEach(m => {
    const d = document.createElement('div');
    const senderName = m.sender?.username || m.sender || 'unknown';
    const isMe = senderName === me;

    d.className = 'message' + (isMe ? ' my-message' : ' other-message');

    const showSender = !isMe || (current && current.type === 'group');

    d.innerHTML = `
      ${showSender ? '<div class="message-sender">' + escapeHtml(senderName) + '</div>' : ''}
      <div class="message-content">${escapeHtml(m.content || m.text || '')}</div>
    `;
    box.appendChild(d);
  });

  if (wasAtBottom) {
    box.scrollTop = box.scrollHeight;
  } else {
    const newScrollHeight = box.scrollHeight;
    const scrollDifference = newScrollHeight - previousScrollHeight;
    box.scrollTop = box.scrollTop + scrollDifference;
  }
}

document.getElementById('send').onclick = async () => {
  if (isSending) {
    console.log('⏳ Mensaje ya enviándose, espera...');
    return;
  }

  const text = document.getElementById('message').value.trim();
  if (!current || !text) {
    alert('Selecciona un contacto y escribe un mensaje');
    return;
  }

  isSending = true;
  document.getElementById('message').value = '';

  try {
    console.log('📤 Enviando mensaje...');
    await apiPost('/send', { 
      from: me, 
      to: current.name, 
      type: current.type, 
      text 
    });
    console.log('✅ Mensaje enviado via HTTP');

    await loadMessages();
  } catch (error) {
    console.error('❌ Error enviando mensaje:', error);
    alert('Error al enviar el mensaje');
  } finally {
    setTimeout(() => {
      isSending = false;
    }, 1000);
  }
};

document.getElementById('message').addEventListener('keypress', (e) => {
  if (e.key === 'Enter') {
    e.preventDefault();
    if (!isSending) {
      document.getElementById('send').click();
    }
  }
});

document.getElementById('createGroup').onclick = async () => {
  try {
    const name = prompt('Nombre del grupo:');
    if (!name || !name.trim()) return;
    
    const availableUsers = await apiGet('/users?user=' + encodeURIComponent(me));
    
    if (availableUsers.length === 0) {
      alert('No hay otros usuarios conectados para agregar al grupo');
      return;
    }

    let membersList = '';
    availableUsers.forEach((user, index) => {
      membersList += `${index + 1}. ${user}\n`;
    });
    
    const pick = prompt(
      `Selecciona miembros escribiendo los números separados por comas (ej: 1,2,3)\n\nUsuarios disponibles:\n${membersList}`
    );
    
    if (!pick || !pick.trim()) {
      alert('Debes seleccionar al menos un miembro para el grupo');
      return;
    }

    const selectedNumbers = pick.split(',').map(s => parseInt(s.trim())).filter(n => !isNaN(n) && n > 0 && n <= availableUsers.length);
    
    if (selectedNumbers.length === 0) {
      alert('No se seleccionaron miembros válidos');
      return;
    }

    const members = selectedNumbers.map(num => availableUsers[num - 1]);
    
    const otherMembers = members.filter(m => m !== me);
    if (otherMembers.length === 0) {
      alert('Debes seleccionar al menos un miembro diferente a ti mismo');
      return;
    }
    
    const result = await apiPost('/groups', { name: name.trim(), members, username: me });
    
    if (result.ok) {
      await loadContacts();
    }
  } catch (error) {
    console.error('❌ Error creando grupo:', error);
    const errorMsg = error.message || 'Error al crear el grupo';
    if (errorMsg.includes('Debe haber al menos')) {
      alert('Debe haber al menos un miembro diferente al creador del grupo');
    } else if (errorMsg.includes('Ya existe')) {
      alert('Ya existe un grupo con ese nombre');
    } else if (errorMsg.includes('conectado')) {
      alert('Todos los miembros deben estar conectados');
    } else {
      alert('Error al crear el grupo: ' + errorMsg);
    }
  }
};

window.addEventListener('beforeunload', async () => {
  stopPolling();
  if (me) {
    try {
      await fetch(API + '/disconnect', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: me })
      });
    } catch (error) {
      console.error('Error al desconectar:', error);
    }
  }
});

window.addEventListener('unload', async () => {
  if (me) {
    try {
      await fetch(API + '/disconnect', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: me }),
        keepalive: true
      });
    } catch (error) {
      console.error('Error al desconectar:', error);
    }
  }
});

start();
