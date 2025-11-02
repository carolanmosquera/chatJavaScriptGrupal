const API = '/api';
let me = null;
let contacts = [];
let current = null;
let ws = null; // WebSocket connection

function escapeHtml(text) {
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
}

async function apiGet(path){ const r=await fetch(API+path); return r.json(); }
async function apiPost(path, body){ const r=await fetch(API+path,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)}); return r.json(); }

function connectWebSocket() {
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${wsProtocol}//${window.location.host}`;
  
  console.log('🔌 Conectando WebSocket a:', wsUrl);
  ws = new WebSocket(wsUrl);
  
  ws.onopen = () => {
    console.log('✅ WebSocket conectado exitosamente');
  };
  
  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      console.log('✅ Mensaje WebSocket recibido:', data);
      
      if (data.type === 'NEW_MESSAGE') {
        console.log('📨 Nuevo mensaje detectado, recargando mensajes...');
        // Lógica eliminada: Siempre se recargan los mensajes para reflejar la actualización.
        loadMessages();
      } else if (data.type === 'GROUPS_UPDATE') {
        console.log('👥 Actualización de grupos');
        loadContacts();
      } else if (data.type === 'USERS_UPDATE') {
        console.log('👤 Actualización de usuarios');
        loadContacts();
      }
    } catch (e) {
      console.error('❌ Error procesando mensaje WebSocket:', e);
    }
  };
  
  ws.onerror = (error) => {
    console.error('WebSocket error:', error);
  };
  
  ws.onclose = () => {
    console.log('WebSocket desconectado, reconectando...');
    setTimeout(connectWebSocket, 3000);
  };
}

// La función handleNewMessage() ha sido eliminada ya que no es necesaria.

async function start(){
  me = prompt('Tu nombre de usuario:');
  if(!me){ alert('Se requiere un nombre de usuario'); return; }
  await apiPost('/register',{ username: me });
  
  // Mostrar el nombre del usuario en la barra lateral
  document.querySelector('.sidebar-header h2').innerHTML = `ChatApp<br><small style="font-size:12px; font-weight:400; color:#65676b;">👤 ${me}</small>`;
  
  // Conectar WebSocket para recibir mensajes en tiempo real
  connectWebSocket();
  
  await loadContacts();
  // Actualizar contactos cada 10 segundos (solo para nuevos usuarios/grupos)
  setInterval(loadContacts, 10000);
}

async function loadContacts(){
  const users = await apiGet('/users?user=' + encodeURIComponent(me));
  const groups = await apiGet('/groups?user=' + encodeURIComponent(me));
  contacts = [];
  users.forEach(u=>contacts.push({type:'user', name:u}));
  groups.forEach(g=>contacts.push({type:'group', name:g.name, members:g.members||[] }));
  renderContacts();
}

function renderContacts(){
  const container = document.getElementById('contacts');
  container.innerHTML = '';
  contacts.forEach(c=>{
    const el = document.createElement('div');
    el.className = 'contact' + (current && current.type===c.type && current.name===c.name ? ' active' : '');
    el.textContent = c.type==='group' ? '👥 ' + c.name : c.name;
    el.onclick = ()=> selectContact(c);
    container.appendChild(el);
  });
}

async function selectContact(c){
  current = c;
  renderContacts();
  
  // Actualizar header con información detallada
  const header = document.getElementById('chatHeader');
  if(c.type === 'group'){
    const memberCount = c.members ? c.members.length : 0;
    const memberNames = c.members ? c.members.map(m => m.username || m).join(', ') : '';
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
  
  // Cargar mensajes existentes
  await loadMessages();
}

async function loadMessages(){
  if(!current) return;
  
  const msgs = await apiGet(`/messages/${current.type}/${encodeURIComponent(current.name)}?user=${encodeURIComponent(me)}`);
  const box = document.getElementById('messages');
  box.innerHTML = '';
  
  msgs.forEach(m=>{
    const d = document.createElement('div');
    const senderName = m.sender?.username || m.sender || 'unknown';
    const isMe = senderName === me;
    
    d.className = 'message' + (isMe ? ' my-message' : ' other-message');
    
    // Solo mostrar el nombre del remitente si no es el usuario actual o si es un grupo
    const showSender = !isMe || (current && current.type === 'group');
    
    d.innerHTML = `
      ${showSender ? '<div class="message-sender">' + senderName + '</div>' : ''}
      <div class="message-content">${escapeHtml(m.content || m.text || '')}</div>
    `;
    box.appendChild(d);
  });
  
  box.scrollTop = box.scrollHeight;
}

document.getElementById('send').onclick = async ()=>{
  const text = document.getElementById('message').value.trim();
  if(!current || !text) return alert('Selecciona un contacto y escribe un mensaje');
  
  document.getElementById('message').value='';
  
  await apiPost('/send',{ from: me, to: current.name, type: current.type, text });
  
  // La recarga de mensajes se gestionará a través del WebSocket para consistencia
  // await loadMessages(); // Opcional: se puede quitar para depender 100% de WebSocket
};

// Permitir enviar con Enter
document.getElementById('message').addEventListener('keypress', (e) => {
  if (e.key === 'Enter') {
    e.preventDefault(); // Evitar salto de línea
    document.getElementById('send').click();
  }
});

document.getElementById('createGroup').onclick = async ()=>{
  const name = prompt('Group name');
  if(!name) return;
  const users = await apiGet('/users?user=' + encodeURIComponent(me));
  const pick = prompt('Select members from connected users (comma-separated). Available: ' + users.join(', '));
  const members = pick ? pick.split(',').map(s=>s.trim()).filter(Boolean) : [];
  if (!members.includes(me)) members.push(me);
  await apiPost('/groups', { name, members, username: me });
  await loadContacts();
};

// Limpiar WebSocket cuando se cierra la ventana
window.addEventListener('beforeunload', ()=>{
  if(ws) ws.close();
});

start();