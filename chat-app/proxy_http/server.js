
const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const WebSocket = require('ws');
const { v4: uuidv4 } = require('uuid');
const path = require('path');

const app = express();
app.use(cors());
app.use(bodyParser.json());

let groups = [];
let users = []; // array of usernames
let messages = [];

const BACKEND_WS = process.env.BACKEND_WS || 'ws://localhost:8887';
const ws = new WebSocket(BACKEND_WS);

ws.on('open', () => console.log('Connected to backend ws at', BACKEND_WS));
ws.on('message', (data) => {
  try {
    const msg = JSON.parse(data);
    // if backend sends info about users or groups, update caches
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
    } else if (msg.type === 'USER_CONNECTED' || msg.type === 'USER_CONNECTED'.toLowerCase()) {
      // backend informs about connected user
      const username = msg.sender && msg.sender.username;
      if (username && !users.includes(username)) users.push(username);
    } else if (msg.type === 'TEXT' || !msg.type) {
      // store messages
      messages.push({ id: msg.id || uuidv4(), sender: msg.sender || { username: 'unknown' }, content: msg.content || msg.text || '', nombreGrupo: msg.nombreGrupo || null, dateTime: msg.dateTime || new Date().toISOString(), raw: msg });
      if (msg.sender && msg.sender.username && !users.includes(msg.sender.username)) users.push(msg.sender.username);
    }
  } catch (e) {
    console.error('invalid backend message', e);
  }
});

// register endpoint: called by frontend when user opens app
app.post('/api/register', (req,res) => {
  const { username } = req.body;
  if (!username) return res.status(400).json({ error: 'username required' });
  if (!users.includes(username)) users.push(username);
  // notify backend of connected user (optional)
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
  const createMsg = { id: g.id, sender: { username: username||'webclient' }, content: name, type: 'CREATE_GROUP', nombreGrupo: name, listaUsuarios: memNorm };
  if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(createMsg));
  res.json({ ok:true, group: g });
});

app.get('/api/messages/:type/:name', (req,res) => {
  const { type, name } = req.params;
  let filtered = [];
  if (type === 'group') filtered = messages.filter(m => m.nombreGrupo === name);
  else if (type === 'user') filtered = messages.filter(m => (m.sender && m.sender.username === name) || (m.raw && m.raw.target === name));
  res.json(filtered || []);
});

app.post('/api/send', (req,res) => {
  const { from, to, type, text } = req.body;
  if (!from || !to || !text) return res.status(400).json({ error:'from,to,text required' });
  const msg = { id: uuidv4(), sender: { username: from }, content: text, type: 'TEXT', nombreGrupo: type==='group'?to:null, target: type==='user'?to:null, dateTime: new Date().toISOString() };
  if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(msg));
  messages.push(msg);
  if (!users.includes(from)) users.push(from);
  if (type==='user' && !users.includes(to)) users.push(to);
  res.json({ ok:true, sent: msg });
});

app.use('/', express.static(path.join(__dirname, 'public')));

const PORT = process.env.PORT || 3000;
app.listen(PORT, ()=>console.log('Proxy listening on', PORT, 'backend ws', BACKEND_WS));
