const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const WebSocket = require('ws');
const { v4: uuidv4 } = require('uuid');
const path = require('path');
const http = require('http');

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

app.use(cors());
app.use(bodyParser.json());

let groups = [];
let users = [];
let messages = [];
let frontendClients = new Set(); // Clientes conectados desde el frontend

const BACKEND_WS = process.env.BACKEND_WS || 'ws://172.20.10.13:8887';

const ws = new WebSocket(BACKEND_WS);

ws.on('open', () => {
  console.log(' Conectado al backend WebSocket en:', BACKEND_WS);
  console.log(' Estado actual:');
  console.log('   - Usuarios:', users.length);
  console.log('   - Grupos:', groups.length);
  console.log('   - Mensajes:', messages.length);
  console.log('   - Clientes frontend:', frontendClients.size);
});
ws.on('message', (data) => {
  try {
    const msg = JSON.parse(data);
    console.log('Backend message:', msg);
    
    if (msg.type === 'UPDATE_GROUPS' || msg.type === 'UPDATE_GROUPS'.toLowerCase()) {
      const list = msg.groupList || msg.listaGrupos || msg.groups || [];
      if (Array.isArray(list)) {
        groups = list.map(g => {
          const name = g.nombreGrupo || g.name || g.groupName;
          const membersRaw = g.listaUsuarios || g.members || [];
          const members = (membersRaw || []).map(u => (typeof u === 'string' ? { username: u, id: uuidv4() } : { username: u.username || u.name || u, id: u.id || uuidv4() }));
          return { name, members, id: g.id || uuidv4() };
        }).filter(g=>g.name);
      }
      // Notificar a todos los clientes frontend sobre actualización de grupos
      broadcastToFrontend({ type: 'GROUPS_UPDATE', groups });
    } else if (msg.type === 'USER_CONNECTED' || msg.type === 'USER_CONNECTED'.toLowerCase()) {
      const username = msg.sender && msg.sender.username;
      if (username && !users.includes(username)) users.push(username);
      broadcastToFrontend({ type: 'USERS_UPDATE', users });
    } else if (msg.type === 'TEXT' || !msg.type) {
      // Almacenar mensajes con información completa
      const msgData = {
        id: msg.id || uuidv4(),
        sender: msg.sender || { username: 'unknown' },
        content: msg.content || msg.text || '',
        nombreGrupo: msg.nombreGrupo || null,
        target: msg.target || null,
        dateTime: msg.dateTime || new Date().toISOString(),
        raw: msg
      };
      messages.push(msgData);
      console.log('Stored message:', msgData);
      
      if (msg.sender && msg.sender.username && !users.includes(msg.sender.username)) {
        users.push(msg.sender.username);
      }
      
      // IMPORTANTE: Notificar a todos los clientes sobre el nuevo mensaje
      broadcastToFrontend({ type: 'NEW_MESSAGE', message: msgData });
    }
  } catch (e) {
    console.error('invalid backend message', e);
  }
});

ws.on('error', (err) => console.error('WebSocket error:', err));
ws.on('close', () => console.log('Disconnected from backend'));

// Manejar conexiones WebSocket desde el frontend
wss.on('connection', (clientWs) => {
  console.log('New frontend client connected');
  frontendClients.add(clientWs);
  
  clientWs.on('close', () => {
    console.log('Frontend client disconnected');
    frontendClients.delete(clientWs);
  });
  
  clientWs.on('error', (err) => {
    console.error('Frontend client error:', err);
    frontendClients.delete(clientWs);
  });
});

// Función para enviar mensajes a todos los clientes frontend
function broadcastToFrontend(data) {
  const message = JSON.stringify(data);
  frontendClients.forEach(client => {
    if (client.readyState === WebSocket.OPEN) {
      client.send(message);
      console.log(` Enviado a cliente ${index + 1}`);
    }else {
      console.log(`   ❌ Cliente ${index + 1} no está conectado, removiendo...`);
      frontendClients.delete(client);
    }
  });
}

app.post('/api/register', (req,res) => {
  const { username } = req.body;
  if (!username) return res.status(400).json({ error: 'username required' });
  if (!users.includes(username)) users.push(username);
  
  const notify = { id: uuidv4(), sender: { username }, type: 'USER_CONNECTED', content: `${username} connected` };
  if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(notify));
  
  res.json({ ok:true, users });
});

app.get('/api/users', (req,res) => {
  const exclude = req.query.user;
  const list = users.filter(u=>u !== exclude);
  res.json(list);
});

app.get('/api/groups', (req,res) => {
  const user = req.query.user;
  if (!user) return res.json(groups);
  const filtered = groups.filter(g => (g.members||[]).some(m => m.username === user));
  res.json(filtered);
});

app.post('/api/groups', (req,res) => {
  const { name, members, username } = req.body;
  if (!name) return res.status(400).json({ error: 'name required' });
  
  const memList = (members || []).slice();
  if (username && !memList.includes(username)) memList.push(username);
  const memNorm = memList.map(u => ({ username: typeof u === 'string' ? u : (u.username||u), id: uuidv4() }));
  
  const g = { name, members: memNorm, id: uuidv4() };
  groups.push(g);
  
  const createMsg = { 
    id: g.id, 
    sender: { username: username||'webclient' }, 
    content: name, 
    type: 'CREATE_GROUP', 
    nombreGrupo: name, 
    listaUsuarios: memNorm 
  };
  
  if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(createMsg));
  
  res.json({ ok:true, group: g });
});

app.get('/api/messages/:type/:name', (req,res) => {
  const { type, name } = req.params;
  const user = req.query.user; // Usuario que está consultando
  let filtered = [];
  
  if (type === 'group') {
    // Mensajes del grupo
    filtered = messages.filter(m => m.nombreGrupo === name);
  } else if (type === 'user') {
    // Mensajes 1-a-1: mostrar conversación entre el usuario actual y el contacto
    filtered = messages.filter(m => {
      const senderName = m.sender?.username || m.sender;
      const targetName = m.target;
      
      // Incluir mensajes donde:
      // 1. El usuario actual es el remitente Y el contacto es el destinatario
      // 2. El contacto es el remitente Y el usuario actual es el destinatario
      return (
        (senderName === user && targetName === name) ||
        (senderName === name && targetName === user) ||
        (senderName === user && targetName === name) ||
        (senderName === name && !targetName && !m.nombreGrupo) // Compatibilidad
      );
    });
  }
  
  // Ordenar por fecha
  filtered.sort((a, b) => new Date(a.dateTime) - new Date(b.dateTime));
  
  console.log(`Messages for ${type}:${name} (user: ${user}):`, filtered.length);
  res.json(filtered || []);
});

app.post('/api/send', (req,res) => {
  const { from, to, type, text } = req.body;
  if (!from || !to || !text) return res.status(400).json({ error:'from,to,text required' });
  
  const msg = { 
    id: uuidv4(), 
    sender: { username: from }, 
    content: text, 
    type: 'TEXT', 
    nombreGrupo: type === 'group' ? to : null,
    target: type === 'user' ? to : null,
    dateTime: new Date().toISOString() 
  };
  
  // Enviar al backend WebSocket
  if (ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(msg));
    console.log('Mensaje enviado al backend:', msg);
  }
  
  // Almacenar localmente también
  messages.push(msg);
  
  // CRÍTICO: Notificar inmediatamente a todos los clientes frontend
  console.log('Broadcasting mensaje a', frontendClients.size, 'clientes');
  broadcastToFrontend({ type: 'NEW_MESSAGE', message: msg });
  
  if (!users.includes(from)) users.push(from);
  if (type === 'user' && !users.includes(to)) users.push(to);
  
  res.json({ ok:true, sent: msg });
});

app.use('/', express.static(path.join(__dirname, 'public')));

const PORT = process.env.PORT || 3000;
server.listen(PORT, ()=>console.log('Proxy listening on', PORT, 'backend ws', BACKEND_WS));