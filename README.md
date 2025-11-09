# 💬 ChatApp Local con Arquitectura Distribuida (Java Backend + Node.js HTTP Proxy)

Esta es una aplicación de chat de texto que permite la comunicación en **tiempo real** entre múltiples usuarios conectados a la **misma red local (LAN)**. La aplicación utiliza una arquitectura de doble componente: un **Backend** (Java) para la lógica de negocio y gestión de la comunicación, y un **Proxy/Frontend** (Node.js) que utiliza **únicamente HTTP** (mediante **Polling**) para la comunicación con el cliente, sin requerir WebSocket ni Spring Boot.

## ✨ Características Principales

* ✅ Comunicación HTTP mediante Polling para actualización automática.
* ✅ Arquitectura Cliente-Servidor-Backend distribuida.
* ✅ Chat privado entre usuarios.
* ✅ Chat grupal.
* ✅ Interfaz moderna y *responsive* (HTML, CSS y JavaScript *vanilla*).
* ✅ Almacenamiento de datos en memoria (los datos se pierden al reiniciar el servidor).

---

## 🛠️ Requisitos del Sistema (Servidor)

Asegúrese de tener instaladas las siguientes herramientas en el computador que actuará como **servidor**:

* **Java** (versión **21** o superior)
* **Gradle** (versión **9** o superior)
* **Node.js** (versión 14 o superior)
* **npm** (incluido con Node.js)

---

## 🚀 Guía de Inicio Rápido

Para iniciar la aplicación, se requiere abrir **dos terminales** en el computador que actuará como **servidor**.

### 1. Inicio del Backend (Terminal 1)

El Backend es el núcleo de la lógica de negocio y la gestión de la comunicación (Java).

1.  **Navegue al directorio del backend:**
    ```bash
    cd backend
    ```
2.  **Compilación y Construcción (Solo la primera vez o después de cambios):**
    Este paso compila el código y genera el archivo ejecutable (`.jar`).
    ```bash
    ./gradlew clean build
    ```
3.  **Ejecución del Backend:**
    Una vez compilado, inicie el servidor de backend.
    ```bash
    java -jar build/libs/backend-1.0-SNAPSHOT.jar
    ```

### 2. Inicio del Proxy/Frontend (Terminal 2)

El proxy actúa como intermediario HTTP y también aloja el frontend (la interfaz de usuario).

1.  **Navegue al directorio del proxy:**
    ```bash
    cd proxy_http
    ```
2.  **Instale las dependencias (Solo la primera vez):**
    ```bash
    npm install
    ```
3.  **Ejecución del Proxy y Frontend:**
    Este comando inicia el servidor proxy y hace que el frontend esté disponible.
    ```bash
    npm start
    ```
> **Nota Importante:** Al iniciar, el sistema mostrará la **dirección IP y el puerto** en el que está activo el Proxy. Esta dirección es la que deben usar los clientes para conectarse. *Ejemplo: `Proxy activo en 192.168.1.7:3000`*

---

## 🔗 Conexión de Clientes y Uso

Los clientes deben estar en la **misma red local (LAN)** que el servidor.

1.  Abre tu navegador web.
2.  Ve a la dirección del servidor usando su **IP** y el **puerto** (generalmente `3000`) proporcionado por la Terminal 2.
    * **Formato de la URL:** `http://(IP del servidor):(Puerto)`
    * **Ejemplo:** `http://192.168.1.7:3000` (o `http://localhost:3000` si es el servidor)
3.  Ingresa tu nombre de usuario cuando la aplicación lo solicite.

### Uso de la Aplicación

* **Chat Privado (Individual):**
    * Seleccione un usuario de la lista de contactos (lado derecho).
    * Escriba su mensaje y presione Enter o haga clic en "Enviar".
* **Chat Grupal:**
    * Haga clic en el botón **`+ Grupo`** para crear un nuevo grupo.
    * Ingrese el **nombre** del grupo.
    * Ingrese los **nombres de los miembros** a incluir, separados por una **coma** (`,`).
    * Seleccione el grupo de la lista para comenzar a chatear.

---

## 💻 Arquitectura y Endpoints

El proyecto se estructura con dos componentes principales:

| Componente | Tecnología | Rol Principal | Comunicación Cliente-Proxy |
| :--- | :--- | :--- | :--- |
| **Proxy/Frontend** | Node.js/Express, HTML/CSS/JS *Vanilla* | Aloja el Frontend y gestiona la comunicación HTTP con los clientes. | **HTTP REST API** |
| **Backend** | Java 21/Gradle | Lógica de negocio y gestión de la comunicación centralizada. | **Polling HTTP** para actualizaciones |

### Endpoints API del Proxy HTTP (Comunicación Cliente-Proxy)

El Proxy expone los siguientes *endpoints* HTTP para la comunicación con el cliente:

* `POST /api/register` - Registrar un nuevo usuario.
* `GET /api/users` - Obtener lista de usuarios.
* `GET /api/groups` - Obtener lista de grupos.
* `POST /api/groups` - Crear un nuevo grupo.
* `GET /api/messages/:type/:name` - Obtener mensajes (*type*: `'user'` o `'group'`).
* `POST /api/send` - Enviar un mensaje.
* `GET /api/check-updates` - Verificar si hay nuevos mensajes (utilizado para el Polling).

---

## 🧑‍💻 Autores y Contribuyentes

Este proyecto fue desarrollado por los siguientes estudiantes:

| Nombre | Código |
| :--- | :--- |
| **Camilo Andres Martinez Moreno** | A00405205 |
| **Martin Borrero Herrera** | A00403871 |
| **Daniel Santiago Fajardo** | A00405139 |
| **Carol Andrea Mosquera** | A00403934 |
