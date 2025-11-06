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
    // ✅ Enviar mensaje de login al conectarse
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
      
      // ✅ CORRECCIÓN: Manejar mensajes de texto del servidor
      if (data.type === 'TEXT') {
        console.log('📨 Nuevo mensaje de chat recibido:', data.content);
        // Si hay un chat activo, recargar los mensajes
        if (current) {
            loadMessages();
        } else {
            // Si no hay chat activo, recargar contactos para mostrar notificaciones
            loadContacts();
        }
      } else if (data.type === 'UPDATE_GROUPS') {
        console.log('👥 Actualización de grupos recibida');
        loadContacts();
      } else if (data.type === 'CREATE_GROUP' || data.type === 'JOIN_GROUP') {
        console.log('🔄 Actualización de estructura de chat');
        loadContacts();
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
  
  // ✅ Enviar mensaje a través de WebSocket
  const messageData = {
    sender: {
      username: me,
      id: Date.now().toString()
    },
    type: "TEXT",
    content: messageText,
    timestamp: new Date().toISOString()
  };
  
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(messageData));
    console.log('📤 Mensaje enviado via WebSocket:', messageData);
    
    // Recargar mensajes después de un breve delay para ver el mensaje propio
    setTimeout(loadMessages, 300);
  } else {
    console.error('❌ WebSocket no está conectado, usando HTTP como fallback');
    // Fallback a HTTP si WebSocket no está disponible
    try {
      await apiPost('/send', { 
        from: me, 
        to: current.name, 
        type: current.type, 
        text: messageText 
      });
      await loadMessages();
    } catch (error) {
      console.error('❌ Error enviando mensaje via HTTP:', error);
      alert('Error enviando mensaje. Verifica la conexión.');
    }
  }
};

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