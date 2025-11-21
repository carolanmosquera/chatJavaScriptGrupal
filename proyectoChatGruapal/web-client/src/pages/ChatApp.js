// app.js - Cliente Web para Chat con Ice.js
// Nota: Necesitarás incluir ice.js en tu HTML

class ChatApp {
    constructor() {
        this.communicator = null;
        this.chatService = null;
        this.currentUser = null;
        this.selectedContact = null;
        
        // Referencias DOM
        this.messagesContainer = document.getElementById('messages');
        this.contactsList = document.getElementById('contacts');
        this.messageInput = document.getElementById('messageInput');
        this.sendBtn = document.getElementById('sendBtn');
        this.chatHeader = document.getElementById('chatHeader');
        
        this.initializeEventListeners();
    }

    initializeEventListeners() {
        this.sendBtn.addEventListener('click', () => this.sendMessage());
        this.messageInput.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.sendMessage();
        });
    }

    async connect() {
        try {
            // Inicializar Ice
            this.communicator = Ice.initialize();
            
            // Conectar al servidor usando WebSocket
            const proxy = this.communicator.stringToProxy(
                "ChatService:ws -h localhost -p 10001"
            );
            
            this.chatService = await Chat.ChatServicePrx.checkedCast(proxy);
            
            if (!this.chatService) {
                throw new Error("Proxy inválido");
            }
            
            console.log("✓ Conectado al servidor Ice");
            
            // Solicitar nombre de usuario
            await this.promptUsername();
            
            // Iniciar polling para actualizar mensajes y usuarios
            this.startPolling();
            
        } catch (error) {
            console.error("Error conectando:", error);
            alert("No se pudo conectar al servidor. Asegúrate de que esté ejecutándose.");
        }
    }

    async promptUsername() {
        const username = prompt("Ingresa tu nombre de usuario:");
        if (!username || username.trim() === '') {
            alert("Debes ingresar un nombre de usuario");
            return this.promptUsername();
        }
        
        try {
            this.currentUser = await this.chatService.joinChat(username.trim());
            console.log("Usuario registrado:", this.currentUser);
            this.updateChatHeader("Chat General", "Conectado como " + this.currentUser.username);
        } catch (error) {
            console.error("Error al unirse al chat:", error);
            alert("Error al unirse al chat");
        }
    }

    startPolling() {
        // Actualizar mensajes y usuarios cada 1 segundo
        setInterval(async () => {
            await this.updateMessages();
            await this.updateUsers();
        }, 1000);
    }

    async updateMessages() {
        try {
            const messages = await this.chatService.getMessages();
            this.renderMessages(messages);
        } catch (error) {
            console.error("Error obteniendo mensajes:", error);
        }
    }

    async updateUsers() {
        try {
            const users = await this.chatService.getUsers();
            this.renderContacts(users);
        } catch (error) {
            console.error("Error obteniendo usuarios:", error);
        }
    }

    renderContacts(users) {
        this.contactsList.innerHTML = '';
        
        users.forEach(user => {
            if (user.id === this.currentUser.id) return; // No mostrar el usuario actual
            
            const contactDiv = document.createElement('div');
            contactDiv.className = 'contact';
            contactDiv.onclick = () => this.selectContact(user);
            
            const initial = user.username.charAt(0).toUpperCase();
            const statusClass = user.isOnline ? 'online' : 'offline';
            
            contactDiv.innerHTML = `
                <div class="avatar">${initial}</div>
                <div class="contact-info">
                    <div class="contact-name">${user.username}</div>
                    <div class="contact-preview ${statusClass}">
                        ${user.isOnline ? '● En línea' : '○ Desconectado'}
                    </div>
                </div>
            `;
            
            this.contactsList.appendChild(contactDiv);
        });
    }

    renderMessages(messages) {
        // Limpiar placeholder si existe
        const placeholder = this.messagesContainer.querySelector('.messages-placeholder');
        if (placeholder && messages.length > 0) {
            placeholder.remove();
        }
        
        // Guardar posición de scroll
        const shouldScrollDown = this.isScrolledToBottom();
        
        this.messagesContainer.innerHTML = '';
        
        messages.forEach(msg => {
            const messageDiv = this.createMessageElement(msg);
            this.messagesContainer.appendChild(messageDiv);
        });
        
        // Auto-scroll si estaba al final
        if (shouldScrollDown) {
            this.scrollToBottom();
        }
    }

    createMessageElement(msg) {
        const isMyMessage = msg.senderId === this.currentUser.id;
        const messageDiv = document.createElement('div');
        
        if (msg.type.value === Chat.MessageTypeEnum.SYSTEM.value) {
            // Mensaje del sistema
            messageDiv.className = 'system-message';
            messageDiv.innerHTML = `
                <div class="system-message-content">
                    <i class="fas fa-info-circle"></i>
                    ${msg.content}
                </div>
            `;
        } else if (msg.type.value === Chat.MessageTypeEnum.VOICECALL.value) {
            // Llamada de voz
            messageDiv.className = isMyMessage ? 'message my-message' : 'message other-message';
            messageDiv.innerHTML = `
                <div class="message-content">
                    <i class="fas fa-phone"></i> ${msg.content}
                </div>
                ${!isMyMessage ? `<div class="message-sender">${msg.senderName}</div>` : ''}
            `;
        } else {
            // Mensaje de texto normal
            messageDiv.className = isMyMessage ? 'message my-message' : 'message other-message';
            messageDiv.innerHTML = `
                <div class="message-content">${this.escapeHtml(msg.content)}</div>
                ${!isMyMessage ? `<div class="message-sender">${msg.senderName}</div>` : ''}
            `;
        }
        
        return messageDiv;
    }

    async sendMessage() {
        const content = this.messageInput.value.trim();
        if (!content || !this.currentUser) return;
        
        try {
            await this.chatService.sendMessage(
                this.currentUser.id,
                content,
                Chat.MessageTypeEnum.TEXT
            );
            
            this.messageInput.value = '';
            this.messageInput.focus();
            
            // Actualizar inmediatamente
            await this.updateMessages();
        } catch (error) {
            console.error("Error enviando mensaje:", error);
            alert("Error al enviar mensaje");
        }
    }

    selectContact(user) {
        this.selectedContact = user;
        this.updateChatHeader(user.username, user.isOnline ? 'En línea' : 'Desconectado');
        
        // Actualizar UI
        document.querySelectorAll('.contact').forEach(c => c.classList.remove('active'));
        event.currentTarget.classList.add('active');
    }

    updateChatHeader(title, subtitle) {
        const headerInfo = this.chatHeader.querySelector('.chat-header-info');
        headerInfo.innerHTML = `
            <div class="chat-header-text">
                <h2 class="chat-title">${title}</h2>
                <span class="chat-subtitle">${subtitle}</span>
            </div>
        `;
    }

    isScrolledToBottom() {
        const threshold = 50;
        return this.messagesContainer.scrollHeight - this.messagesContainer.scrollTop 
               <= this.messagesContainer.clientHeight + threshold;
    }

    scrollToBottom() {
        this.messagesContainer.scrollTop = this.messagesContainer.scrollHeight;
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    async disconnect() {
        if (this.currentUser) {
            try {
                await this.chatService.leaveChat(this.currentUser.id);
            } catch (error) {
                console.error("Error al desconectar:", error);
            }
        }
        
        if (this.communicator) {
            await this.communicator.destroy();
        }
    }
}

// Inicializar la aplicación
let app;

window.addEventListener('DOMContentLoaded', async () => {
    app = new ChatApp();
    
    // Conectar al servidor
    setTimeout(async () => {
        await app.connect();
    }, 500);
});

// Desconectar al cerrar la página
window.addEventListener('beforeunload', () => {
    if (app) {
        app.disconnect();
    }
});

// Funcionalidad del modal de grupos
document.addEventListener('DOMContentLoaded', () => {
    const createGroupBtn = document.getElementById('createGroupBtn');
    const createGroupModal = document.getElementById('createGroupModal');
    const closeModalBtns = document.querySelectorAll('.modal-close');
    const createGroupSubmit = createGroupModal.querySelector('.btn-primary');

    createGroupBtn.addEventListener('click', async () => {
        if (!app || !app.currentUser) {
            alert("Debes estar conectado para crear un grupo");
            return;
        }
        
        // Obtener usuarios para seleccionar
        try {
            const users = await app.chatService.getUsers();
            renderUserSelectList(users);
            createGroupModal.classList.add('active');
        } catch (error) {
            console.error("Error obteniendo usuarios:", error);
        }
    });

    closeModalBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            createGroupModal.classList.remove('active');
        });
    });

    createGroupModal.addEventListener('click', (e) => {
        if (e.target === createGroupModal) {
            createGroupModal.classList.remove('active');
        }
    });

    createGroupSubmit.addEventListener('click', async () => {
        const groupNameInput = createGroupModal.querySelector('.form-input');
        const groupName = groupNameInput.value.trim();
        
        if (!groupName) {
            alert("Debes ingresar un nombre para el grupo");
            return;
        }
        
        try {
            const group = await app.chatService.createGroup(groupName, app.currentUser.id);
            console.log("Grupo creado:", group);
            
            // Agregar usuarios seleccionados
            const selectedUsers = createGroupModal.querySelectorAll('input[type="checkbox"]:checked');
            for (const checkbox of selectedUsers) {
                await app.chatService.joinGroup(group.id, checkbox.value);
            }
            
            alert("Grupo creado exitosamente");
            createGroupModal.classList.remove('active');
            groupNameInput.value = '';
        } catch (error) {
            console.error("Error creando grupo:", error);
            alert("Error al crear el grupo");
        }
    });
});

function renderUserSelectList(users) {
    const userSelectList = document.querySelector('.user-select-list');
    userSelectList.innerHTML = '';
    
    users.forEach(user => {
        if (user.id === app.currentUser.id) return;
        
        const userItem = document.createElement('div');
        userItem.className = 'user-select-item';
        
        const initial = user.username.charAt(0).toUpperCase();
        
        userItem.innerHTML = `
            <input type="checkbox" value="${user.id}" id="user_${user.id}">
            <div class="avatar-small">${initial}</div>
            <label for="user_${user.id}">${user.username}</label>
        `;
        
        userSelectList.appendChild(userItem);
    });
}
