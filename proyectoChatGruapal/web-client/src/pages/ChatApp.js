const ChatUI = {
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
        this.selectedGroup = null;


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
            const proxyString = "ChatService:ws -h localhost -p 10001 -r /";
            console.log(" Conectando a:", proxyString);

            const base = this.communicator.stringToProxy(proxyString);

            // Casteo obligatorio al tipo generado por Slice
            this.chatService = Chat.ChatServicePrx.uncheckedCast(base);

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

                console.log(" Enviando petición joinChat al servidor...");

                const response = await this.chatService.joinChat(username);

                console.log(" Respuesta recibida:", response);

                // Validación correcta: UserDTO directo
                if (!response || typeof response.id !== "string") {
                    throw new Error("Respuesta no válida del servidor");
                }

                // Asignar el usuario recibido
                this.currentUser = response;

                console.log(" Usuario registrado exitosamente:", this.currentUser);

                this.updateChatHeader("Chat General", "Conectado como " + this.currentUser.username);

                modal.classList.remove('active');

                this.startPolling();

                this.showNotification("¡Bienvenido " + this.currentUser.username + "!", "success");

            } catch (error) {
                console.error(" Error al unirse al chat:", error);

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
             //actualiza grupos
            await this.updateGroups(); 
        }, 1000);

        // Primera actualización inmediata
        this.updateMessages();
        this.updateUsers();
        this.updateGroups(); 

    }

    async updateMessages() {
        try {
            let messages;
            if (this.selectedContact) {
                // Obtener mensajes privados del chat seleccionado
                messages = await this.chatService.getPrivateMessages(
                    this.currentUser.id,
                    this.selectedContact.id
                );
            } else {
                // Obtener mensajes del chat general
                messages = await this.chatService.getMessages();
            }
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
    //metodo para actualizar grupos 
    async updateGroups() {
        try {
            const groups = await this.chatService.getGroups();
            this.renderGroups(groups);
        } catch (error) {
            console.error("Error obteniendo grupos:", error);
        }
    }

     //metodo para renderizar lista de grupos
     renderGroups(groups) {

        const groupsContainer = document.getElementById('groupsList') || this.createGroupsSection();

        groupsContainer.innerHTML = '';

        if (groups.length === 0) {
            groupsContainer.innerHTML = `
                <div class="contact" style="justify-content: center; padding: 20px;">
                    <span style="color: var(--text-muted);">No hay grupos creados</span>
                </div>
            `;
            return;
        }

        groups.forEach(group => {
            const groupElement = this.createGroupElement(group);
            groupsContainer.appendChild(groupElement);
        });
    }
     //metodo Crear elemento HTML para grupo
     createGroupElement(group) {
        const groupDiv = document.createElement('div');
        groupDiv.className = 'contact group';
        groupDiv.onclick = () => this.selectGroup(group);

        const memberCount = group.memberIds ? group.memberIds.length : 0;
        const initials = group.name.split(' ').map(word => word[0]).join('').toUpperCase().substring(0, 2);

        groupDiv.innerHTML = `
            <div class="avatar group-avatar">${initials}</div>
            <div class="contact-info">
                <div class="contact-name">${this.escapeHtml(group.name)}</div>
                <div class="contact-preview group-preview">
                    <i class="fas fa-users"></i> ${memberCount} miembros
                </div>
            </div>
        `;

        return groupDiv;
    }

    //metodo Seleccionar grupo
    selectGroup(group) {
        this.selectedGroup = group;
        this.selectedContact = null;

        this.updateChatHeader(
            group.name, 
            `Grupo • ${group.memberIds ? group.memberIds.length : 0} miembros`
        );

        // Resaltar grupo seleccionado
        document.querySelectorAll('.contact').forEach(c => c.classList.remove('active'));
        document.querySelectorAll('.group').forEach(g => g.classList.remove('active'));
        event.currentTarget.classList.add('active');

        // TODO: Cargar mensajes específicos del grupo (si los implementas)
        console.log("Grupo seleccionado:", group.name);
    }

    // metodo Crear sección de grupos en el sidebar
    createGroupsSection() {
        const sidebar = document.querySelector('.sidebar');

        // Crear contenedor de grupos
        const groupsSection = document.createElement('div');
        groupsSection.id = 'groupsSection';
        groupsSection.className = 'groups-section';

        groupsSection.innerHTML = `
            <div class="section-header">
                <h3>Grupos</h3>
            </div>
            <div id="groupsList" class="groups-list"></div>
        `;

        // Insertar después de la lista de contactos
        const contactsList = document.getElementById('contacts');
        sidebar.insertBefore(groupsSection, contactsList.nextSibling);

        return document.getElementById('groupsList');
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

        if (msg.type.value === ChatUI.MessageTypeEnum.SYSTEM.value) {
            messageDiv.className = 'system-message';
            messageDiv.innerHTML = `
                <div class="system-message-content">
                    <i class="fas fa-info-circle"></i>
                    ${this.escapeHtml(msg.content)}
                </div>
            `;
        } else if (msg.type.value === ChatUI.MessageTypeEnum.VOICECALL.value) {
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

            if (this.selectedContact) {
                // Enviar mensaje privado
                await this.chatService.sendPrivateMessage(
                    this.currentUser.id,
                    this.selectedContact.id,
                    content,
                    Chat.MessageTypeEnum.TEXT
                );
                console.log(" Mensaje privado enviado exitosamente");
            } else {
                // Enviar mensaje al chat general
                await this.chatService.sendMessage(
                    this.currentUser.id,
                    content,
                    Chat.MessageTypeEnum.TEXT
                );
                console.log(" Mensaje enviado exitosamente");
            }

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
        this.selectedGroup = null; // ← Deseleccionar grupo
        this.updateChatHeader(user.username, user.isOnline ? 'En línea' : 'Desconectado');

        // Marcar el contacto como activo
        document.querySelectorAll('.contact').forEach(c => c.classList.remove('active'));
        const contacts = Array.from(document.querySelectorAll('.contact'));
        const selectedContactElement = contacts.find(c => {
            const nameElement = c.querySelector('.contact-name');
            return nameElement && nameElement.textContent === user.username;
        });
        if (selectedContactElement) {
            selectedContactElement.classList.add('active');
        }

        // Cargar mensajes privados inmediatamente
        this.updateMessages();
    }

    updateChatHeader(title, subtitle) {
        const headerInfo = this.chatHeader.querySelector('.chat-header-info');
        
        // Si hay un contacto seleccionado, agregar botón para volver al chat general
        const backButton = this.selectedContact ? `
            <button class="icon-btn back-btn" title="Volver al chat general" onclick="window.chatApp.backToGeneralChat()">
                <i class="fas fa-arrow-left"></i>
            </button>
        ` : '';
        
        headerInfo.innerHTML = `
            ${backButton}
            <div class="chat-header-text">
                <h2 class="chat-title">${title}</h2>
                <span class="chat-subtitle">${subtitle}</span>
            </div>
        `;
    }

    backToGeneralChat() {
        this.selectedContact = null;
        this.updateChatHeader("Chat General", "Conectado como " + this.currentUser.username);
        
        // Desmarcar todos los contactos
        document.querySelectorAll('.contact').forEach(c => c.classList.remove('active'));
        
        // Cargar mensajes del chat general
        this.updateMessages();
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
                await this.chatService.leaveChat(this.currentUser.id);
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