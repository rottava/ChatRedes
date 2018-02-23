package pv.chatredes.chat;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

import pv.chatredes.R;

public class ChatRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_ME = 1;
    private static final int VIEW_TYPE_OTHER = 2;
    private List<Mensagem> mensagens;

    ChatRecyclerAdapter(List<Mensagem> mensagem) {
        mensagens = mensagem;
    }

    //Adiciona nova mensagem ao recycle view
    void add(Mensagem mensagem) {
        mensagens.add(mensagem);
        notifyItemInserted(mensagens.size() - 1);
    }

    //Define view holder da mensagem
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        RecyclerView.ViewHolder viewHolder = null;
        switch (viewType) {
            //Mensagem do remetente
            case VIEW_TYPE_ME:
                View mensagensRemetente = layoutInflater.inflate(R.layout.mensagem_remetente, parent, false);
                viewHolder = new MensagensRemetente(mensagensRemetente);
                break;
            //Mensagem do destinatario
            case VIEW_TYPE_OTHER:
                View mensagensDestinatario = layoutInflater.inflate(R.layout.mensagem_destinatario, parent, false);
                viewHolder = new MensagensDestinatario(mensagensDestinatario);
                break;
        }
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (TextUtils.equals(mensagens.get(position).remetenteUID,
                FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            remetenteViewHolder((MensagensRemetente) holder, position);
        } else {
            destinatarioViewHolder((MensagensDestinatario) holder, position);
        }
    }

    //Remetente view holder
    private void remetenteViewHolder(MensagensRemetente myChatViewHolder, int position) {
        Mensagem mensagem = mensagens.get(position);
        myChatViewHolder.mensagemRemetente.setText(mensagem.mensagem);
    }

    //Destinatario view holder
    private void destinatarioViewHolder(MensagensDestinatario otherChatViewHolder, int position) {
        Mensagem mensagem = mensagens.get(position);
        otherChatViewHolder.mensagemDestinatario.setText(mensagem.mensagem);
    }

    //Total de itens no recycle view
    @Override
    public int getItemCount() {
        if (mensagens != null) {
            return mensagens.size();
        }
        return 0;
    }

    //Retorna 1 para remetente, 2 para destinatario
    @Override
    public int getItemViewType(int position) {
        if (TextUtils.equals(mensagens.get(position).remetenteUID,
                FirebaseAuth.getInstance().getCurrentUser().getUid())) {
            return VIEW_TYPE_ME;
        } else {
            return VIEW_TYPE_OTHER;
        }
    }

    //Remetente fica do lado direito
    private static class MensagensRemetente extends RecyclerView.ViewHolder {
        private TextView mensagemRemetente;

        MensagensRemetente(View itemView) {
            super(itemView);
            mensagemRemetente = itemView.findViewById(R.id.mensagem_remetente);
        }
    }

    //Destinatario fica do lado esquerdo
    private static class MensagensDestinatario extends RecyclerView.ViewHolder {
        private TextView mensagemDestinatario;

        MensagensDestinatario(View itemView) {
            super(itemView);
            mensagemDestinatario = itemView.findViewById(R.id.mensagem_destinario);
        }
    }
}
