package com.chat.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class UserGroups implements Serializable {

    private List<User> listaUsuarios;
    private String nombreGrupo;

    public UserGroups( String nombreGrupo) {
        this.listaUsuarios = new ArrayList<User>();
        this.nombreGrupo = nombreGrupo;
    }

    public List<User> getListaUsuarios() {
        return listaUsuarios;
    }

    public void setListaUsuarios(List<User> listaUsuarios) {
        this.listaUsuarios = listaUsuarios;
    }

    public String getNombreGrupo() {
        return nombreGrupo;
    }

    public void setNombreGrupo(String nombreGrupo) {
        this.nombreGrupo = nombreGrupo;
    }

    public void addUser(User nuevoUsuario){
        listaUsuarios.add(nuevoUsuario);
    }

    public User buscarUsuario(String name) {
    for (int i = 0; i < listaUsuarios.size(); i++) {
        if (listaUsuarios.get(i).getUsername().equals(name)) {
            return listaUsuarios.get(i);
        }
    }
    return null;
    }

    public void deleteUser(String name){

        listaUsuarios.remove(buscarUsuario(name));

    }

     @Override
    public String toString() {
        return nombreGrupo + " (" + listaUsuarios.size() + " miembros)";
    }
}

    

    

    
    

