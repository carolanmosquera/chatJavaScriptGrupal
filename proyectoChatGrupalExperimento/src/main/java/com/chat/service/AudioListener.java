package com.chat.service;

/**
 * Interfaz para notificar a un objeto cuando un fragmento de audio ha sido grabado.
 */
public interface AudioListener {
    /**
     * Se llama cuando un fragmento de datos de audio está disponible.
     * @param audioData El fragmento de audio grabado.
     */
    void onAudioData(byte[] audioData);
}