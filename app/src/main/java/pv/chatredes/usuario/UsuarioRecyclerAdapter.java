package pv.chatredes.usuario;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import pv.chatredes.R;

public class UsuarioRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<User> usuarios;

    public UsuarioRecyclerAdapter(List<User> usuario) {
        usuarios = usuario;
    }

    //Adiciona nova mensagem ao recycle view
    public void add(User usuario) {
        usuarios.add(usuario);
        notifyItemInserted(usuarios.size() - 1);
    }

    //Define view holder da usuario
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View usuario = layoutInflater.inflate(R.layout.usuario, parent, false);
        return new Usuario(usuario);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        User usuario = usuarios.get(position);

        if (usuario != null){
            usuarioViewHolder((Usuario) holder, position);
        }
    }

    //Total de itens no recycle view
    @Override
    public int getItemCount() {
        if (usuarios != null) {
            return usuarios.size();
        }
        return 0;
    }

    //Retorna posição da usuario
    public User getUsuario(int position) {
        return usuarios.get(position);
    }

    //View holder da usuario
    public static class Usuario extends RecyclerView.ViewHolder {
        private TextView inicialView, usuarioView;

        Usuario(View itemView) {
            super(itemView);
            inicialView = itemView.findViewById(R.id.text_view_inicial);
            usuarioView = itemView.findViewById(R.id.text_view_usuario);
        }
    }

    //Remetente view holder
    private void usuarioViewHolder(Usuario usuarioViewHolder, int position) {
        User usuario = usuarios.get(position);
        usuarioViewHolder.usuarioView.setText(usuario.email);
        usuarioViewHolder.inicialView.setText(usuario.email.substring(0,1));
    }

}
