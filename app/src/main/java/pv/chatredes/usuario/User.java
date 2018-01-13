package pv.chatredes.usuario;

import java.io.Serializable;

public class User implements Serializable {

    public String ip;
    public String email;
    public String uid;

    public User(){

    }

    public User(String email, String uid, String ip){
        this.email = email;
        this.uid = uid;
        this.ip = ip;

    }





}
