package pv.chatredes.chat;

import java.io.Serializable;

public class Mensagem implements Serializable {
    String remetenteUID;
    String mensagem;
    long timestamp;

    public Mensagem(){

    }

    Mensagem(String remetenteUID, String mensagem, long timestamp){
        this.remetenteUID = remetenteUID;
        this.mensagem = mensagem;
        this.timestamp = timestamp;
    }





}
