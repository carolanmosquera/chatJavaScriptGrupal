# Sistema de Chat

Este proyecto consiste en un sistema de mensajería en tiempo real desarrollado con una arquitectura Cliente-Servidor. Permite la creación de salas de chat, mensajería privada y grupal, envío de notas de voz y realización de llamadas de audio directamente desde el navegador.

##  Características Principales

*   **Gestión de Grupos:** Creación y participación en grupos de chat.
*   **Mensajería en Tiempo Real:** Envío de mensajes de texto a usuarios específicos o grupos con actualización dinámica.
*   **Historial Multimedia:** Visualización del historial de conversaciones, incluyendo texto y notas de voz.
*   **Comunicación de Voz:**
    *   Envío de notas de voz.
    *   Llamadas de voz en tiempo real utilizando procesamiento de audio en el navegador y transmisión vía WebSockets/RPC.

## 👥 Autores y Contribuyentes

| Nombre | Código |
| :--- | :--- |
| Camilo Andres Martinez Moreno | A00405205 |
| Martin Borrero Herrera | A00403871 |
| Daniel Santiago Fajardo | A00405139 |
| Carol Andrea Mosquera | A00403934 |

---

##  Requisitos del Sistema

Para ejecutar el servidor y compilar el proyecto, asegúrese de tener instaladas las siguientes herramientas:

*   **Java:** Versión 21 o superior.
*   **Gradle:** Versión 9 o superior.
*   **Node.js:** Versión 14 o superior (incluye npm).
*   **Python 3:** Para desplegar el cliente web localmente.

---

##  Instrucciones de Ejecución

Siga estos pasos para poner en marcha el sistema.

### 1. Iniciar el Servidor (Backend)
Una sola persona (el host) debe ejecutar el servidor. Desde la raíz del proyecto, ejecute los siguientes comandos en su terminal:

1.  Generar el ejecutable:
    ```bash
    .\gradlew jar
    ```
2.  Ejecutar el servidor:
    ```bash
    java -jar build\libs\proyectoChatGruapal.jar
    ```

### 2. Iniciar el Cliente (Frontend)
Tanto el host (si desea usar la app) como el resto de los integrantes deben ejecutar el cliente.( y estos deben de estar conectados a la misma red del host)

1.  Navegue a la carpeta del cliente web:
    ```bash
    cd web-client
    ```
2.  Levante el servidor HTTP con Python:
    ```bash
    python3 -m http.server 8080
    ```
3.  Abra su navegador web e ingrese a:
    *   `http://localhost:8080`

---

##  Flujo de Comunicación Cliente-Servidor

### Arquitectura General
El sistema utiliza **ZeroC Ice** como middleware para la comunicación, operando sobre **WebSockets** en el puerto 8080.
*   **Cliente:** JavaScript ejecutándose en el navegador.
*   **Servidor:** Lógica de negocio en Java.

### Modelo de Comunicación
1.  **Cliente → Servidor (RPC):** El cliente realiza llamadas directas a métodos del servidor (como funciones locales). Ice serializa los datos y gestiona la respuesta.
2.  **Servidor → Cliente (Polling):** El servidor **no** envía datos automáticamente (Push). El cliente consulta periódicamente (cada segundo) si hay novedades.

### Flujos por Funcionalidad

#### 1️ Mensajes de Texto
*   **Envío:** El usuario envía el mensaje → Cliente llama al método RPC → Servidor valida, asigna ID y Timestamp → Guarda en memoria RAM → Confirma recepción.
*   **Recepción:** El cliente hace *polling* cada segundo → Servidor entrega lista de mensajes → Cliente filtra y renderiza solo los nuevos.

#### 2️ Notas de Voz (Audios)
*   **Grabación:** El navegador captura audio (WebM/Opus) y lo convierte a **Base64**.
*   **Envío:** Se envía al servidor como un mensaje de texto especial (tipo `AUDIO`).
*   **Reproducción:** El cliente receptor detecta el tipo `AUDIO`, decodifica el Base64 y genera un elemento HTML de audio para reproducir.

#### 3️ Llamadas de Voz 
El servidor actúa como un **Relay** (retransmisor) y no guarda el audio en disco.

*   **Establecimiento:**
    *   Cliente A notifica intención de llamada.
    *   Servidor crea registro en memoria y buffer.
    *   Cliente B detecta la llamada (vía polling) y acepta.

*   **Transmisión:**
    *   **Captura:** Ambos clientes capturan audio y lo fragmentan en trozos pequeños (~50ms) formato PCM convertidos a Base64.
    *   **Envío:** Cada fragmento se envía al servidor identificando al destinatario.
    *   **Recepción:** Ambos clientes consultan al servidor cada **400ms**. El servidor entrega el último fragmento disponible, lo elimina del buffer, y el cliente lo reproduce inmediatamente.

*   **Finalización:** Al terminar la llamada, el servidor limpia los registros y buffers de memoria.
