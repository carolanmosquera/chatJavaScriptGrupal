const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const { v4: uuidv4 } = require('uuid');
const path = require('path');
const http = require('http');
const fs = require('fs');

const app = express();
const server = http.createServer(app);

app.use(cors());
app.use(bodyParser.json());

let groups = [];
let users = [];
let messages = [];
let userActivity = new Map();

const DATA_DIR = path.join(__dirname, 'data');
const HISTORY_FILE = path.join(DATA_DIR, 'history.json');

if (!fs.existsSync(DATA_DIR)) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
}

function loadHistory() {
  try {
    if (fs.existsSync(HISTORY_FILE)) {
      const data = fs.readFileSync(HISTORY_FILE, 'utf8');
      return JSON.parse(data);
    }
  } catch (error) {
    console.error('Error cargando historial:', error);
  }
  return { users: {} };
}

function saveHistory() {
  try {
    const history = { users: {} };
    
    users.forEach(username => {
      const userMessages = messages.filter(m => {
        const senderName = m.sender?.username || m.sender;
        const targetName = m.target;
        const groupName = m.nombreGrupo;
        
        return senderName === username || 
               targetName === username || 
               (groupName && groups.some(g => 
                 g.name === groupName && 
                 g.members.some(mem => (mem.username || mem) === username)
               ));
      });
      
      const userGroups = groups.filter(g => 
        g.members.some(m => (m.username || m) === username)
      );
      
      history.users[username] = {
        messages: userMessages,
        groups: userGroups
      };
    });
    
    fs.writeFileSync(HISTORY_FILE, JSON.stringify(history, null, 2), 'utf8');
  } catch (error) {
    console.error('Error guardando historial:', error);
  }
}

function loadUserHistory(username) {
  try {
    const history = loadHistory();
    if (history.users && history.users[username]) {
      const userData = history.users[username];
      
      userData.messages.forEach(msg => {
        if (!messages.find(m => m.id === msg.id)) {
          messages.push(msg);
        }
      });
      
      userData.groups.forEach(group => {
        if (!groups.find(g => g.id === group.id)) {
          groups.push(group);
        }
      });
      
      console.log(`📚 Historial cargado para ${username}: ${userData.messages.length} mensajes, ${userData.groups.length} grupos`);
    }
  } catch (error) {
    console.error(`Error cargando historial de ${username}:`, error);
  }
}

function getMiembrosGrupo(nombreGrupo) {
  const group = groups.find(g => g.name === nombreGrupo);
  if (!group) return [];
  return group.members.map(m => m.username || m);
}

app.post('/api/register', (req, res) => {
  const { username } = req.body;
  if (!username) return res.status(400).json({ error: 'username required' });

  if (!users.includes(username)) {
    users.push(username);
    console.log(`✅ Usuario registrado: ${username}`);
    loadUserHistory(username);
  }

  userActivity.set(username, Date.now());

  res.json({ ok: true, users });
});

app.get('/api/users', (req, res) => {
  const exclude = req.query.user;
  if (exclude) {
    userActivity.set(exclude, Date.now());
  }
  const list = users.filter(u => u !== exclude);
  res.json(list);
});

app.get('/api/groups', (req, res) => {
  const user = req.query.user;
  if (user) {
    userActivity.set(user, Date.now());
  }
  if (!user) return res.json(groups);

  const filtered = groups.filter(g => {
    const members = g.members || [];
    return members.some(m => {
      const username = typeof m === 'string' ? m : (m.username || m);
      return username === user;
    });
  });
  res.json(filtered);
});

app.post('/api/groups', (req, res) => {
  const { name, members, username } = req.body;
  if (!name) return res.status(400).json({ error: 'name required' });
  if (!username) return res.status(400).json({ error: 'username required' });

  if (!users.includes(username)) {
    return res.status(400).json({ error: 'El creador del grupo debe estar conectado' });
  }

  const memList = (members || []).slice();

  const validMembers = memList.filter(m => {
    const memberName = typeof m === 'string' ? m : (m.username || m);
    return users.includes(memberName);
  });

  const otherMembers = validMembers.filter(m => {
    const memberName = typeof m === 'string' ? m : (m.username || m);
    return memberName !== username;
  });

  if (otherMembers.length === 0) {
    return res.status(400).json({ error: 'Debe haber al menos un miembro diferente al creador del grupo' });
  }

  if (!validMembers.includes(username)) {
    validMembers.push(username);
  }

  const memNorm = validMembers.map(u => ({
    username: typeof u === 'string' ? u : (u.username || u),
    id: uuidv4()
  }));

  if (groups.some(g => g.name === name)) {
    return res.status(400).json({ error: 'Ya existe un grupo con ese nombre' });
  }

  const g = {
    name,
    members: memNorm,
    id: uuidv4()
  };

  groups.push(g);
  console.log(`✅ Grupo creado: ${name} con ${memNorm.length} miembros (${otherMembers.length} diferentes al creador)`);

  saveHistory();

  res.json({ ok: true, group: g });
});

app.get('/api/messages/:type/:name', (req, res) => {
  const { type, name } = req.params;
  const user = req.query.user;
  if (user) {
    userActivity.set(user, Date.now());
  }
  let filtered = [];

  if (type === 'group') {
    filtered = messages.filter(m => m.nombreGrupo === name);
  } else if (type === 'user') {
    filtered = messages.filter(m => {
      const senderName = m.sender?.username || m.sender;
      const targetName = m.target;
      return (
        (senderName === user && targetName === name) ||
        (senderName === name && targetName === user)
      );
    });
  }

  filtered.sort((a, b) => new Date(a.dateTime) - new Date(b.dateTime));

  console.log(`📨 Mensajes para ${type}:${name} (usuario: ${user}): ${filtered.length}`);
  res.json(filtered || []);
});

app.post('/api/send', (req, res) => {
  const { from, to, type, text } = req.body;
  if (!from || !to || !text) {
    return res.status(400).json({ error: 'from, to, text required' });
  }

  userActivity.set(from, Date.now());

  const msg = {
    id: uuidv4(),
    sender: { username: from },
    content: text,
    type: 'TEXT',
    nombreGrupo: type === 'group' ? to : null,
    target: type === 'user' ? to : null,
    dateTime: new Date().toISOString()
  };

  messages.push(msg);
  console.log(`💬 Mensaje enviado: ${from} -> ${to} (${type}): ${text}`);

  if (!users.includes(from)) users.push(from);
  if (type === 'user' && !users.includes(to)) users.push(to);

  saveHistory();

  res.json({ ok: true, sent: msg });
});

app.get('/api/check-updates', (req, res) => {
  const { user, lastMessageId, chatType, chatName } = req.query;

  if (!user) {
    return res.status(400).json({ error: 'user required' });
  }

  userActivity.set(user, Date.now());

  let relevantMessages = [];

  if (chatType && chatName) {
    if (chatType === 'group') {
      relevantMessages = messages.filter(m => m.nombreGrupo === chatName);
    } else if (chatType === 'user') {
      relevantMessages = messages.filter(m => {
        const senderName = m.sender?.username || m.sender;
        const targetName = m.target;
        return (
          (senderName === user && targetName === chatName) ||
          (senderName === chatName && targetName === user)
        );
      });
    }
  } else {
    const privateMessages = messages.filter(m => {
      const senderName = m.sender?.username || m.sender;
      const targetName = m.target;
      return (senderName === user || targetName === user) && m.target;
    });

    const userGroups = groups.filter(g => {
      const members = g.members || [];
      return members.some(m => {
        const username = typeof m === 'string' ? m : (m.username || m);
        return username === user;
      });
    });

    const groupMessages = messages.filter(m => {
      if (!m.nombreGrupo) return false;
      return userGroups.some(g => g.name === m.nombreGrupo);
    });

    relevantMessages = [...privateMessages, ...groupMessages];
  }

  if (lastMessageId) {
    const lastIndex = relevantMessages.findIndex(m => m.id === lastMessageId);
    if (lastIndex >= 0) {
      relevantMessages = relevantMessages.slice(lastIndex + 1);
    } else {
      relevantMessages = [];
    }
  }

  res.json({
    hasUpdates: relevantMessages.length > 0,
    newMessagesCount: relevantMessages.length,
    lastMessageId: messages.length > 0 ? messages[messages.length - 1].id : null
  });
});

app.post('/api/disconnect', (req, res) => {
  const { username } = req.body;
  if (!username) return res.status(400).json({ error: 'username required' });

  const index = users.indexOf(username);
  if (index > -1) {
    users.splice(index, 1);
    userActivity.delete(username);
    console.log(`❌ Usuario desconectado: ${username}`);
    saveHistory();
  }

  res.json({ ok: true });
});

app.use('/', express.static(path.join(__dirname, 'public')));

function cleanupInactiveUsers() {
  const INACTIVE_TIMEOUT = 15000;
  const now = Date.now();

  const usersToRemove = [];
  users.forEach(username => {
    const lastActivity = userActivity.get(username);
    if (!lastActivity || (now - lastActivity > INACTIVE_TIMEOUT)) {
      usersToRemove.push(username);
    }
  });

  if (usersToRemove.length > 0) {
    usersToRemove.forEach(username => {
      const index = users.indexOf(username);
      if (index > -1) {
        users.splice(index, 1);
        userActivity.delete(username);
        console.log(`🧹 Usuario inactivo eliminado: ${username}`);
      }
    });
    saveHistory();
  }
}

setInterval(cleanupInactiveUsers, 5000);

process.on('SIGINT', () => {
  console.log('\n💾 Guardando historial antes de cerrar...');
  saveHistory();
  process.exit(0);
});

process.on('SIGTERM', () => {
  console.log('\n💾 Guardando historial antes de cerrar...');
  saveHistory();
  process.exit(0);
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  console.log(`🚀 Servidor HTTP iniciado en http://localhost:${PORT}`);
  console.log(`💾 Historial guardado en: ${HISTORY_FILE}`);
});
