
const API = '/api';
let me = null;
let contacts = [];
let current = null;

async function apiGet(path){ const r=await fetch(API+path); return r.json(); }
async function apiPost(path, body){ const r=await fetch(API+path,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body)}); return r.json(); }

async function start(){
  me = prompt('Your username');
  if(!me){ alert('username required'); return; }
  await apiPost('/register',{ username: me });
  await loadContacts();
  setInterval(loadContacts, 3000);
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
  document.getElementById('chatHeader').textContent = c.type==='group' ? c.name + ' (group)' : c.name;
  const msgs = await apiGet(`/messages/${c.type}/${encodeURIComponent(c.name)}`);
  const box = document.getElementById('messages');
  box.innerHTML = '';
  msgs.forEach(m=>{
    const d = document.createElement('div');
    d.innerHTML = '<b>' + (m.sender?.username || m.sender) + '</b>: ' + (m.content || m.text || '');
    box.appendChild(d);
  });
  box.scrollTop = box.scrollHeight;
}

document.getElementById('send').onclick = async ()=>{
  const text = document.getElementById('message').value.trim();
  if(!current || !text) return alert('Select contact and enter message');
  await apiPost('/send',{ from: me, to: current.name, type: current.type, text });
  document.getElementById('message').value='';
  selectContact(current);
};

document.getElementById('createGroup').onclick = async ()=>{
  const name = prompt('Group name');
  if(!name) return;
  // fetch current connected users and present a selection prompt
  const users = await apiGet('/users?user=' + encodeURIComponent(me));
  const pick = prompt('Select members from connected users (comma-separated). Available: ' + users.join(', '));
  const members = pick ? pick.split(',').map(s=>s.trim()).filter(Boolean) : [];
  if (!members.includes(me)) members.push(me);
  await apiPost('/groups', { name, members, username: me });
  await loadContacts();
};

start();
