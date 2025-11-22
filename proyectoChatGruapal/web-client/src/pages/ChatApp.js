const Chat = {
    MessageTypeEnum: {
        TEXT: { value: 0 },
        SYSTEM: { value: 1 },
        AUDIO: { value: 2 },
        VOICECALL: { value: 3 }
    }
};

class ChatApp {
    constructor() {
        this.communicator = null;
        this.chatService = null;
        this.currentUser = null;
        this.selectedContact = null;
        this.pollInterval = null;
        
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
            console.log(" Iniciando conexión con Ice...");
            
            // Inicializar Ice communicator
            const initData = new Ice.InitializationData();
            initData.properties = Ice.createProperties();
            
            this.communicator = Ice.initialize(initData);
            
            console.log(" Communicator inicializado");
            
            // Crear proxy al servidor usando WebSocket
            const proxyString = "ChatService:ws -h localhost -p 10001";
            console.log(" Conectando a:", proxyString);
            
            const base = this.communicator.stringToProxy(proxyString);
            this.chatService = base;
            
            console.log(" Conectado al servidor Ice");
            
            // Configurar el modal de username
            this.setupUsernameModal();
            
        } catch (error) {
            console.error(" Error conectando:", error);
            alert("No se pudo conectar al servidor: " + error.message);
            
            // Retry después de 3 segundos
            setTimeout(() => this.connect(), 3000);
        }
    }

    setupUsernameModal() {
        const modal = document.getElementById('usernameModal');
        const input = document.getElementById('usernameInput');
        const joinBtn = document.getElementById('joinChatBtn');
        
        // Mostrar el modal
        modal.classList.add('active');
        input.focus();
        
        const handleJoin = async () => {
            const username = input.value.trim();
            
            if (!username || username.length < 2) {
                input.style.borderColor = 'var(--danger)';
                input.placeholder = 'Por favor ingresa un nombre válido (mínimo 2 caracteres)';
                input.value = '';
                input.focus();
                return;
            }
            
            // Deshabilitar botón mientras se conecta
            joinBtn.disabled = true;
            joinBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Conectando...';
            
            try {
                console.log(" Intentando unirse con usuario:", username);
                
                // Llamar a joinChat usando Ice
                const request = new Ice.OutputStream(this.communicator);
                request.startEncapsulation();
                request.writeString(username);
                request.endEncapsulation();
                
                console.log(" Enviando petición joinChat al servidor...");
                
                const response = await this.chatService.ice_invoke(
                    "joinChat", 
                    Ice.OperationMode.Normal, 
                    request.finished()
                );
                
                console.log(" Respuesta recibida:", response);
                
                if (response.ok) {
                    const reply = new Ice.InputStream(this.communicator, response.outParams);
                    reply.startEncapsulation();
                    
                    // Leer UserDTO
                    this.currentUser = {
                        id: reply.readString(),
                        username: reply.readString(),
                        isOnline: reply.readBool(),
                        connectedAt: reply.readLong()
                    };
                    
                    reply.endEncapsulation();
                    
                    console.log(" Usuario registrado exitosamente:", this.currentUser);
                    this.updateChatHeader("Chat General", "Conectado como " + this.currentUser.username);
                    
                    // Ocultar modal
                    modal.classList.remove('active');
                    
                    // Iniciar polling
                    this.startPolling();
                    
                    // Mensaje de bienvenida
                    this.showNotification("¡Bienvenido " + this.currentUser.username + "!", "success");
                    
                } else {
                    throw new Error("Respuesta no válida del servidor");
                }
            } catch (error) {
                console.error(" Error al unirse al chat:", error);
                
                // Restaurar botón
                joinBtn.disabled = false;
                joinBtn.innerHTML = '<i class="fas fa-sign-in-alt"></i> Unirse al Chat';
                input.style.borderColor = 'var(--danger)';
                input.value = '';
                input.placeholder = 'Error al conectar. Intenta nuevamente';
                input.focus();
                
                alert("Error al conectar con el servidor:\n" + error.message + "\n\n¿Está el servidor corriendo?");
            }
        };
        
        // Manejar click en botón
        joinBtn.onclick = handleJoin;
        
        // Manejar Enter en input
        input.onkeypress = (e) => {
            if (e.key === 'Enter') {
                handleJoin();
            }
        };
        
        // Evitar cerrar el modal clickeando fuera
        modal.onclick = (e) => {
            if (e.target === modal) {
                input.focus();
            }
        };
    }

    startPolling() {
        console.log(" Iniciando polling de mensajes y usuarios...");
        
        // Actualizar cada segundo
        this.pollInterval = setInterval(async () => {
            await this.updateMessages();
            await this.updateUsers();
        }, 1000);
        
        // Primera actualización inmediata
        this.updateMessages();
        this.updateUsers();
    }

    async updateMessages() {
        try {
            const request = new Ice.OutputStream(this.communicator);
            request.startEncapsulation();
            request.endEncapsulation();
            
            const response = await this.chatService.ice_invoke("getMessages", Ice.OperationMode.Normal, request.finished());
            
            if (response.ok) {
                const reply = new Ice.InputStream(this.communicator, response.outParams);
                reply.startEncapsulation();
                
                const messages = this.readMessageArray(reply);
                reply.endEncapsulation();
                
                this.renderMessages(messages);
            }
        } catch (error) {
            console.error("Error obteniendo mensajes:", error);
        }
    }

    async updateUsers() {
        try {
            const request = new Ice.OutputStream(this.communicator);
            request.startEncapsulation();
            request.endEncapsulation();
            
            const response = await this.chatService.ice_invoke("getUsers", Ice.OperationMode.Normal, request.finished());
            
            if (response.ok) {
                const reply = new Ice.InputStream(this.communicator, response.outParams);
                reply.startEncapsulation();
                
                const users = this.readUserArray(reply);
                reply.endEncapsulation();
                
                this.renderContacts(users);
            }
        } catch (error) {
            console.error("Error obteniendo usuarios:", error);
        }
    }

    readMessageArray(stream) {
        const size = stream.readSize();
        const messages = [];
        
        for (let i = 0; i < size; i++) {
            messages.push({
                id: stream.readString(),
                senderId: stream.readString(),
                senderName: stream.readString(),
                content: stream.readString(),
                timestamp: stream.readLong(),
                type: { value: stream.readEnum(3) }
            });
        }
        
        return messages;
    }

    readUserArray(stream) {
        const size = stream.readSize();
        const users = [];
        
        for (let i = 0; i < size; i++) {
            users.push({
                id: stream.readString(),
                username: stream.readString(),
                isOnline: stream.readBool(),
                connectedAt: stream.readLong()
            });
        }
        
        return users;
    }

    renderContacts(users) {
        this.contactsList.innerHTML = '';
        
        const onlineUsers = users.filter(user => 
            user.id !== this.currentUser.id && user.isOnline
        );
        
        if (onlineUsers.length === 0) {
            this.contactsList.innerHTML = `
                <div class="contact" style="justify-content: center; padding: 20px;">
                    <span style="color: var(--text-muted);">No hay otros usuarios conectados</span>
                </div>
            `;
            return;
        }
        
        onlineUsers.forEach(user => {
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
        const placeholder = this.messagesContainer.querySelector('.messages-placeholder');
        if (placeholder && messages.length > 0) {
            placeholder.remove();
        }
        
        const shouldScrollDown = this.isScrolledToBottom();
        
        this.messagesContainer.innerHTML = '';
        
        if (messages.length === 0) {
            this.messagesContainer.innerHTML = `
                <div class="messages-placeholder">
                    <i class="fas fa-comment-dots"></i>
                    <p>No hay mensajes aún. ¡Sé el primero en escribir!</p>
                </div>
            `;
            return;
        }
        
        messages.forEach(msg => {
            const messageDiv = this.createMessageElement(msg);
            this.messagesContainer.appendChild(messageDiv);
        });
        
        if (shouldScrollDown) {
            this.scrollToBottom();
        }
    }

    createMessageElement(msg) {
        const isMyMessage = msg.senderId === this.currentUser.id;
        const messageDiv = document.createElement('div');
        
        if (msg.type.value === Chat.MessageTypeEnum.SYSTEM.value) {
            messageDiv.className = 'system-message';
            messageDiv.innerHTML = `
                <div class="system-message-content">
                    <i class="fas fa-info-circle"></i>
                    ${this.escapeHtml(msg.content)}
                </div>
            `;
        } else if (msg.type.value === Chat.MessageTypeEnum.VOICECALL.value) {
            messageDiv.className = isMyMessage ? 'message my-message' : 'message other-message';
            messageDiv.innerHTML = `
                <div class="message-content">
                    <i class="fas fa-phone"></i> ${this.escapeHtml(msg.content)}
                </div>
                ${!isMyMessage ? `<div class="message-sender">${this.escapeHtml(msg.senderName)}</div>` : ''}
            `;
        } else {
            messageDiv.className = isMyMessage ? 'message my-message' : 'message other-message';
            messageDiv.innerHTML = `
                <div class="message-content">${this.escapeHtml(msg.content)}</div>
                ${!isMyMessage ? `<div class="message-sender">${this.escapeHtml(msg.senderName)}</div>` : ''}
            `;
        }
        
        return messageDiv;
    }

    async sendMessage() {
        const content = this.messageInput.value.trim();
        if (!content || !this.currentUser) return;
        
        try {
            console.log(" Enviando mensaje:", content);
            
            const request = new Ice.OutputStream(this.communicator);
            request.startEncapsulation();
            request.writeString(this.currentUser.id);
            request.writeString(content);
            request.writeEnum(Chat.MessageTypeEnum.TEXT.value, 3);
            request.endEncapsulation();
            
            await this.chatService.ice_invoke("sendMessage", Ice.OperationMode.Normal, request.finished());
            
            console.log(" Mensaje enviado exitosamente");
            
            this.messageInput.value = '';
            this.messageInput.focus();
            
            // Actualizar inmediatamente
            await this.updateMessages();
        } catch (error) {
            console.error(" Error enviando mensaje:", error);
            alert("Error al enviar mensaje: " + error.message);
        }
    }

    selectContact(user) {
        this.selectedContact = user;
        this.updateChatHeader(user.username, user.isOnline ? 'En línea' : 'Desconectado');
        
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

    showNotification(message, type = 'info') {
        console.log(`[${type.toUpperCase()}] ${message}`);
    }

    async disconnect() {
        if (this.pollInterval) {
            clearInterval(this.pollInterval);
        }
        
        if (this.currentUser && this.chatService) {
            try {
                const request = new Ice.OutputStream(this.communicator);
                request.startEncapsulation();
                request.writeString(this.currentUser.id);
                request.endEncapsulation();
                
                await this.chatService.ice_invoke("leaveChat", Ice.OperationMode.Normal, request.finished());
                console.log(" Desconectado correctamente");
            } catch (error) {
                console.error("Error al desconectar:", error);
            }
        }
        
        if (this.communicator) {
            await this.communicator.destroy();
        }
    }
}

// Añadir estilos para mensajes del sistema
const style = document.createElement('style');
style.textContent = `
    .system-message {
        align-self: center;
        margin: 10px 0;
    }
    .system-message-content {
        background: var(--bg-card);
        color: var(--text-muted);
        padding: 8px 16px;
        border-radius: 20px;
        font-size: 0.85rem;
        display: flex;
        align-items: center;
        gap: 8px;
    }
    .system-message-content i {
        color: var(--secondary);
    }
`;
document.head.appendChild(style);

// Inicializar la aplicación
let app;

window.addEventListener('DOMContentLoaded', async () => {
    console.log(" Iniciando ChatApp...");
    app = new ChatApp();
    window.chatApp = app;
    
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
