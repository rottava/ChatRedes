package pv.chatredes.local;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public final class Armazenamento{

    private void Armazenamento() {}

    public static void salvarDados(Context contexto,String nome, Object objeto) throws IOException {
        FileOutputStream fos = contexto.openFileOutput(nome, Context.MODE_PRIVATE);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(objeto);
        oos.close();
        fos.close();
    }

    public static Object lerDados(Context contexto, String nome) throws IOException, ClassNotFoundException {
        FileInputStream fis = contexto.openFileInput(nome);
        ObjectInputStream ois = new ObjectInputStream(fis);
        Object objeto = ois.readObject();
        return objeto;
    }
}