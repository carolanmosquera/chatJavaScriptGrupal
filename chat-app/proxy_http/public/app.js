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

async function apiGet(path){ 
    const r = await fetch(API + path); 
    return r.json(); 
}

async function apiPost(path, body){ 
    const r = await fetch(API + path, {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(body)
    }); 
    return r.json(); 
}

function connectWebSocket() {
  const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${wsProtocol}//${window.location.host}`;
  
  console.log('🔌 Conectando WebSocket a:', wsUrl);
  ws = new WebSocket(wsUrl);
  
  ws.onopen = () => {
    console.log('✅ WebSocket conectado exitosamente');
    // Enviar mensaje de login al conectarse
    if (me) {
        const loginMsg = {
            sender: {
                username: me,
                id: Date.now().toString()
            },
            type: "TEXT",
            content: `${me} se ha conectado al chat`
        };
        ws.send(JSON.stringify(loginMsg));
        console.log('📤 Mensaje de login enviado:', loginMsg);
    }
  };
  
  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      console.log('✅ Mensaje WebSocket recibido:', data);
      
      // ✅ CORRECCIÓN: Manejar TODOS los tipos de mensajes del proxy
      switch(data.type) {
        case 'NEW_MESSAGE':
          console.log('📨 Nuevo mensaje recibido:', data.message?.content);
          // Si el mensaje es relevante para el chat actual, recargar mensajes
          if (shouldDisplayMessage(data.message)) {
            loadMessages();
          }
          break;
          
        case 'GROUPS_UPDATE':
          console.log('👥 Actualización de grupos recibida');
          loadContacts();
          break;
          
        case 'USERS_UPDATE':
          console.log('👤 Actualización de usuarios recibida');
          loadContacts();
          break;
          
        case 'TEXT':
          console.log('💬 Mensaje TEXT recibido:', data.content);
          loadMessages();
          break;
          
        default:
          console.log('📦 Mensaje de tipo desconocido:', data.type);
          // Por seguridad, recargar mensajes si hay un chat activo
          if (current) loadMessages();
      }
    } catch (e) {
      console.error('❌ Error procesando mensaje WebSocket:', e);
      console.error('Mensaje que causó el error:', event.data);
    }
  };
  
  ws.onerror = (error) => {
    console.error('❌ WebSocket error:', error);
  };
  
  ws.onclose = () => {
    console.log('🔌 WebSocket desconectado, reconectando en 3 segundos...');
    setTimeout(connectWebSocket, 3000);
  };
}

// ✅ NUEVA FUNCIÓN: Determinar si un mensaje debe mostrarse en el chat actual
function shouldDisplayMessage(message) {
  if (!current || !message) return false;
  
  const senderName = message.sender?.username || message.sender;
  const targetGroup = message.nombreGrupo;
  const targetUser = message.target;
  
  // Si es un mensaje de grupo y estamos en ese grupo
  if (current.type === 'group' && targetGroup === current.name) {
    return true;
  }
  
  // Si es un mensaje privado y estamos chateando con esa persona
  if (current.type === 'user') {
    return (senderName === current.name && targetUser === me) || 
           (senderName === me && targetUser === current.name);
  }
  
  return false;
}

async function start(){
  me = prompt('Tu nombre de usuario:');
  if(!me){ 
    alert('Se requiere un nombre de usuario'); 
    return; 
  }
  
  try {
    await apiPost('/register',{ username: me });
    console.log('✅ Usuario registrado:', me);
  } catch (error) {
    console.error('❌ Error registrando usuario:', error);
  }
  
  // Mostrar el nombre del usuario en la barra lateral
  document.querySelector('.sidebar-header h2').innerHTML = `ChatApp<br><small style="font-size:12px; font-weight:400; color:#65676b;">👤 ${me}</small>`;
  
  // Conectar WebSocket para recibir mensajes en tiempo real
  connectWebSocket();
  
  await loadContacts();
  // Actualizar contactos cada 10 segundos (solo para nuevos usuarios/grupos)
  setInterval(loadContacts, 10000);
}

async function loadContacts(){
  try {
    const users = await apiGet('/users?user=' + encodeURIComponent(me));
    const groups = await apiGet('/groups?user=' + encodeURIComponent(me));
    contacts = [];
    users.forEach(u => contacts.push({type: 'user', name: u}));
    groups.forEach(g => contacts.push({type: 'group', name: g.name, members: g.members || [] }));
    renderContacts();
    console.log('📞 Contactos cargados:', contacts.length);
  } catch (error) {
    console.error('❌ Error cargando contactos:', error);
  }
}

function renderContacts(){
  const container = document.getElementById('contacts');
  container.innerHTML = '';
  contacts.forEach(c => {
    const el = document.createElement('div');
    el.className = 'contact' + (current && current.type === c.type && current.name === c.name ? ' active' : '');
    el.textContent = c.type === 'group' ? '👥 ' + c.name : c.name;
    el.onclick = () => selectContact(c);
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
  
  try {
    const msgs = await apiGet(`/messages/${current.type}/${encodeURIComponent(current.name)}?user=${encodeURIComponent(me)}`);
    const box = document.getElementById('messages');
    box.innerHTML = '';
    
    console.log(`📨 Cargando ${msgs.length} mensajes para`, current.name);
    
    msgs.forEach(m => {
      const d = document.createElement('div');
      const senderName = m.sender?.username || m.sender || 'unknown';
      const isMe = senderName === me;
      
      d.className = 'message' + (isMe ? ' my-message' : ' other-message');
      
      // Solo mostrar el nombre del remitente si no es el usuario actual o si es un grupo
      const showSender = !isMe || (current && current.type === 'group');
      
      d.innerHTML = `
        ${showSender ? '<div class="message-sender">' + escapeHtml(senderName) + '</div>' : ''}
        <div class="message-content">${escapeHtml(m.content || m.text || '')}</div>
      `;
      box.appendChild(d);
    });
    
    box.scrollTop = box.scrollHeight;
  } catch (error) {
    console.error('❌ Error cargando mensajes:', error);
  }
}

// ✅ CORRECCIÓN: Función mejorada para enviar mensajes
document.getElementById('send').onclick = async () => {
  const text = document.getElementById('message').value.trim();
  if(!current || !text) {
    alert('Selecciona un contacto y escribe un mensaje');
    return;
  }
  
  const messageText = text;
  document.getElementById('message').value = '';
  
  // Preparar el mensaje para el backend
  const messageData = {
    sender: {
      username: me,
      id: Date.now().toString()
    },
    type: "TEXT",
    content: messageText,
    timestamp: new Date().toISOString()
  };
  
  // ✅ AGREGAR información de destino basada en el tipo de chat
  if (current.type === 'group') {
    messageData.nombreGrupo = current.name;
  } else if (current.type === 'user') {
    messageData.target = current.name;
  }
  
  console.log('📤 Enviando mensaje:', messageData);
  
  // Intentar enviar via WebSocket primero
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(messageData));
    console.log('✅ Mensaje enviado via WebSocket');
    
    // Mostrar mensaje localmente inmediatamente (feedback rápido)
    displayLocalMessage(messageText, true);
    
  } else {
    console.error('❌ WebSocket no está conectado, usando HTTP como fallback');
    // Fallback a HTTP
    try {
      await apiPost('/send', { 
        from: me, 
        to: current.name, 
        type: current.type, 
        text: messageText 
      });
      // Recargar mensajes después de enviar
      setTimeout(loadMessages, 300);
    } catch (error) {
      console.error('❌ Error enviando mensaje via HTTP:', error);
      alert('Error enviando mensaje. Verifica la conexión.');
    }
  }
};

// ✅ NUEVA FUNCIÓN: Mostrar mensaje localmente inmediatamente
function displayLocalMessage(text, isOwn = true) {
  if (!current) return;
  
  const box = document.getElementById('messages');
  const d = document.createElement('div');
  d.className = 'message' + (isOwn ? ' my-message' : ' other-message');
  
  const showSender = !isOwn || (current && current.type === 'group');
  
  d.innerHTML = `
    ${showSender && !isOwn ? '<div class="message-sender">' + escapeHtml(me) + '</div>' : ''}
    <div class="message-content">${escapeHtml(text)}</div>
  `;
  
  box.appendChild(d);
  box.scrollTop = box.scrollHeight;
}

// Permitir enviar con Enter
document.getElementById('message').addEventListener('keypress', (e) => {
  if (e.key === 'Enter') {
    e.preventDefault();
    document.getElementById('send').click();
  }
});

document.getElementById('createGroup').onclick = async () => {
  const name = prompt('Nombre del grupo:');
  if(!name) return;
  
  try {
    const users = await apiGet('/users?user=' + encodeURIComponent(me));
    const pick = prompt('Selecciona miembros de los usuarios conectados (separados por comas). Disponibles: ' + users.join(', '));
    const members = pick ? pick.split(',').map(s => s.trim()).filter(Boolean) : [];
    if (!members.includes(me)) members.push(me);
    
    await apiPost('/groups', { name, members, username: me });
    await loadContacts();
    console.log('✅ Grupo creado:', name);
  } catch (error) {
    console.error('❌ Error creando grupo:', error);
    alert('Error creando el grupo');
  }
};

// Limpiar WebSocket cuando se cierra la ventana
window.addEventListener('beforeunload', () => {
  if(ws) {
    ws.close();
  }
});

// Iniciar la aplicación
start();