package pv.chatredes.chat;

import java.io.Serializable;

public class Mensagem implements Serializable {
    public String remetenteUID;
    public String mensagem;
    public long timestamp;

    public Mensagem(){

    }

    public Mensagem(String remetenteUID, String mensagem, long timestamp){
        this.remetenteUID = remetenteUID;
        this.mensagem = mensagem;
        this.timestamp = timestamp;
    }





}
