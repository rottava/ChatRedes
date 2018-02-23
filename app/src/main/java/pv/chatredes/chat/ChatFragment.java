package pv.chatredes.chat;

import android.app.ProgressDialog;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import pv.chatredes.MainActivity;
import pv.chatredes.R;
import pv.chatredes.local.Armazenamento;

import static android.content.Context.WIFI_SERVICE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class ChatFragment extends Fragment implements View.OnClickListener {

    private ArrayList<Mensagem> mensagens;
    private boolean p2p;
    private ChatRecyclerAdapter recyclerAdapter;
    private DatabaseReference databaseReference;
    private EditText novaMensagem;
    private int respServidor;
    static final int porta = 9001;
    private Mensagem mensagem;
    private Mensagem mensagemRecebida;
    private ProgressDialog progressDialog;
    private RecyclerView recyclerView;
    private String remetenteUID;
    private String destinatario;
    private String destinatarioUID;
    private String destinatarioIP;
    private String id;
    private String rid;

    public ChatFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Inicialização de dados
        if (getArguments() != null) {
            destinatario = getArguments().getString("destinatario");
            Log.d("email: ", destinatario);
            destinatarioUID = getArguments().getString("destinatarioUID");
            Log.d("uid: ", destinatarioUID);
            destinatarioIP = getArguments().getString("destinatarioIP");
            Log.d("ip: ", destinatarioIP);
            remetenteUID = getArguments().getString("remetenteUID");
            p2p = getArguments().getBoolean("p2p");
        }
        mensagens = new ArrayList<>();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View fragmentView = inflater.inflate(R.layout.fragment_chat, container, false);
        //Torna popup invisivel
        getActivity().findViewById(R.id.popup).setVisibility(GONE);
        getActivity().findViewById(R.id.meuip).setVisibility(GONE);
        getActivity().findViewById(R.id.ipdest).setVisibility(GONE);
        //Torna FAB invisivel
        getActivity().findViewById(R.id.fab).setVisibility(GONE);
        //Ajusta toolbar
        ((MainActivity) getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ((MainActivity) getActivity()).getSupportActionBar().setDisplayShowHomeEnabled(true);
        //Acesso ao banco de dados Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference();
        //Recycle view
        recyclerView = fragmentView.findViewById(R.id.recycler_view_chat);
        recyclerAdapter = new ChatRecyclerAdapter(mensagens);
        recyclerView.setAdapter(recyclerAdapter);
        //Botão enviar
        Button enviarMensagem = fragmentView.findViewById(R.id.enviar_mensagem);
        enviarMensagem.setOnClickListener(ChatFragment.this);
        novaMensagem = fragmentView.findViewById(R.id.nova_mensagem);
        //Janela de espera
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setTitle(getString(R.string.carregando));
        progressDialog.setMessage(getString(R.string.esperando));
        progressDialog.setIndeterminate(true);
        //ID para sala de conversa Firebase
        id = remetenteUID + '&' + destinatarioUID;
        rid = destinatarioUID + '&' + remetenteUID;
        //Inicializa conversas
        if(p2p){
            //Inicializa conversas do Firebase
            lerDadosP2P();
            //Inicializacao P2P, ouve porta
            iniP2P();
        } else {
            //Inicializa conversas p2p
            recebeMensagemFirebase();
        }
        //mensagens.add(new Mensagem("123456","Left",System.currentTimeMillis()));
        return fragmentView;
    }

    @Override
    public void onStart() {
        super.onStart();
        //EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        //EventBus.getDefault().unregister(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (p2p) {
            p2p = false;
        }
    }

    @Override
    public void onClick(View v) {
        //Enviar mensagem pelo metodo
        if (p2p) {
            enviaMensagemP2P();
        } else {
            enviaMensagemFirebase();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case R.id.firebase_enable:
                p2p = false;
                getActivity().findViewById(R.id.p2p_enable).setVisibility(GONE);
                //Toast.makeText(getActivity(), "P2P Desabilitado", Toast.LENGTH_SHORT).show();
                break;
            case R.id.p2p_enable:
                p2p = true;
                getActivity().findViewById(R.id.p2p_enable).setVisibility(VISIBLE);
                //Toast.makeText(getActivity(), "P2P Habilitado", Toast.LENGTH_SHORT).show();
                iniP2P();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void meuIP(){
        WifiManager wm = (WifiManager) getContext().getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    }

    private void iniP2P(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    //Cria uma socket servidor (recebimento) e atribui porta
                    ServerSocket sServidor = new ServerSocket(porta);
                    //Cria socket de cliente (retorno)
                    Socket sCliente;
                    //Aqui fica ouvindo a porta
                    while (p2p) {
                        //Aceita conexao com cliente
                        sCliente = sServidor.accept();
                        //Pra cada cliente uma nova porta
                        recebeTCP serverAsyncTask = new recebeTCP();
                        //Inicia tarefa e passa porta cliente
                        serverAsyncTask.execute(sCliente);
                    }
                    sServidor.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    //Carrega dados local
    private void lerDadosP2P(){
        ArrayList<Mensagem> mensagensTemp;
        int i;
        try {
            mensagensTemp = (ArrayList<Mensagem>) Armazenamento.lerDados(getActivity(), id);
            for (i = 0; i < mensagensTemp.size(); i++) {
                recebidoSucesso(mensagensTemp.get(i));
            }
        } catch (IOException e) {
            Log.e("ChatFragment", e.getMessage());
        } catch (ClassNotFoundException e) {
            Log.e("ChatFragment_a", e.getMessage());
        }
        Log.d("lerDadosP2P", "Mensagens:" + mensagens.size());
    }

    //Salva dados local
    private void salvarDadosP2P(){
        try {
            Armazenamento.salvarDados(getActivity(), id, mensagens);
        } catch (IOException e) {
            Log.e("ChatFragment", e.getMessage());
        }

    }

    //Mensagem enviada com sucesso salva mensagem
    public void enviadoSucesso() {
        Log.d("enviadoSucesso", "entrou");
        //Cria recycle view
        if (recyclerAdapter == null) {
            recyclerAdapter = new ChatRecyclerAdapter(new ArrayList<Mensagem>());
            recyclerView.setAdapter(recyclerAdapter);
        }
        //Atualiza
        mensagens.add(mensagem);
        if (p2p) {
            //Dados local
            salvarDadosP2P();
        }
        recyclerAdapter.notifyDataSetChanged();
        //Apaga mensagem na caixa de texto
        novaMensagem.setText("");
        Log.d("enviadoSucesso", "saiu");
        //Toast.makeText(getActivity(), "Message sent", Toast.LENGTH_SHORT).show();
    }

    //Mensagem não enviada mostra aviso
    public void enviadoFalha(String mensagem) {
        Toast.makeText(getActivity(), mensagem, Toast.LENGTH_SHORT).show();
    }

    //Mensagem recebida com sucesso atualiza recycle view e salva mensagem
    public void recebidoSucesso(Mensagem mensagem) {
        Log.d("recebidoSucesso", "entrou");
        //Cria recycle view
        if (recyclerAdapter == null) {
            recyclerAdapter = new ChatRecyclerAdapter(new ArrayList<Mensagem>());
            recyclerView.setAdapter(recyclerAdapter);
        }
        //Atualiza
        if(mensagens.size() == 0){
            mensagens.add(mensagem);
        }else if (mensagem.timestamp != mensagens.get(mensagens.size()-1).timestamp){
            mensagens.add(mensagem);
        }
        if (p2p){
            //Dados local
            salvarDadosP2P();
        }
        recyclerAdapter.notifyDataSetChanged();
        //Rola a tela até a ultima mensagem
        recyclerView.smoothScrollToPosition(recyclerAdapter.getItemCount() - 1);
        Log.d("recebidoSucesso", "Total de mensagens =" + mensagens.size());
        Log.d("recebidoSucesso", "saiu");
    }

    //Mensagem recebida com falha mostra aviso
    public void recebidoFalha(String mensagem) {
        Toast.makeText(getActivity(), mensagem, Toast.LENGTH_SHORT).show();
    }

    //Envia mensagens através do metodo P2P
    private void enviaMensagemP2P() {
        //Mostra tela de espera
        showProgressDialog();
        //Inicializa retorno do destinatario
        mensagem = new Mensagem(remetenteUID, novaMensagem.getText().toString(), System.currentTimeMillis());
        //Envia mensagem para o destinatario
        enviaTCP tcp = new enviaTCP();
        tcp.execute(mensagem);

        hideProgressDialog();

        if (respServidor == 1) {
            enviadoSucesso();
        } else {
            enviadoFalha("Mensagem não enviada: ");
        }

    }

    //Envia mensagens através do Firebase
    private void enviaMensagemFirebase() {
        //Mostra janela de espera
        showProgressDialog();
        //Atualiza mensagem
        mensagem = new Mensagem(remetenteUID, novaMensagem.getText().toString(), System.currentTimeMillis());
        //Manda mensagem para o Firebase
        databaseReference.child("conversas").getRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(id)) {
                    databaseReference.child("conversas").child(id).child(String.valueOf(mensagem.timestamp)).setValue(mensagem);
                } else if (dataSnapshot.hasChild(rid)) {
                    databaseReference.child("conversas").child(rid).child(String.valueOf(mensagem.timestamp)).setValue(mensagem);
                } else {
                    databaseReference.child("conversas").child(id).child(String.valueOf(mensagem.timestamp)).setValue(mensagem);
                    recebeMensagemFirebase();
                }
                //Envia notificação para o outro usuario
                //enviarNotificacao(remetente, mensagem.mensagem, remetenteUID, remetenteToken, destinatarioToken);
                //Oculta janela de espera
                hideProgressDialog();
                //Atualiza dados
                enviadoSucesso();
            }
            //Falha no envio
            @Override
            public void onCancelled(DatabaseError databaseError) {
                hideProgressDialog();
                enviadoFalha("Mensagem não enviada: " + databaseError.getMessage());
            }
        });
    }

    //Recebe mensagens através do Firebse
    public void recebeMensagemFirebase() {
        Log.d("recebeMensagemFirebase", "entrou");
        //Mostra janela de espera
        showProgressDialog();
        //Recebe mensagem do Firebase
         databaseReference.child("conversas").getRef().addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(id)) {
                    FirebaseDatabase.getInstance().getReference().child("conversas").child(id).addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                            Log.d("recebeMensagem.Added", "entrou");
                            //Oculta janela de espera
                            hideProgressDialog();
                            mensagemRecebida = dataSnapshot.getValue(Mensagem.class);
                            recebidoSucesso(mensagemRecebida);
                            Log.d("recebeMensagem.Added", "saiu");

                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {

                        }

                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.d("recebeMensagem.Error", "entrou");
                            hideProgressDialog();
                            recebidoFalha("Mensagem não recebida: " + databaseError.getMessage());
                            Log.d("recebeMensagem.Error", "saiu");
                        }
                    });
                } else if (dataSnapshot.hasChild(rid)) {
                    FirebaseDatabase.getInstance().getReference().child("conversas").child(rid).addChildEventListener(new ChildEventListener() {
                        @Override
                        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                            Log.d("recebeMensagem.Added", "entrou");
                            //Oculta janela de espera
                            hideProgressDialog();
                            mensagemRecebida = dataSnapshot.getValue(Mensagem.class);
                            recebidoSucesso(mensagemRecebida);
                            Log.d("recebeMensagem.Added", "saiu");
                        }

                        @Override
                        public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onChildRemoved(DataSnapshot dataSnapshot) {

                        }

                        @Override
                        public void onChildMoved(DataSnapshot dataSnapshot, String s) {

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.d("recebeMensagem.Error", "entrou");
                            hideProgressDialog();
                            recebidoFalha("Mensagem não recebida: " + databaseError.getMessage());
                            Log.d("recebeMensagem.Error", "saiu");
                        }
                    });
                } else {
                    hideProgressDialog();
                    Log.e("recebeMensagem.Error", "getMessageFromFirebaseUser: no such room available");
                }
            }
            //Falhou em receber mensagem do firebase
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("recebeMensagem.Error", "entrou");
                hideProgressDialog();
                recebidoFalha("Mensagem não recebida: " + databaseError.getMessage());
                Log.d("recebeMensagem.Error", "saiu");
            }
        });
    }

    //Faz aparecer a janela de espera
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage(getString(R.string.carregando));
            progressDialog.setIndeterminate(true);
        }

        progressDialog.show();
    }

    //Faz desaparecer a janela de espera
    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    class enviaTCP extends AsyncTask<Mensagem, Void, Void> {
        @Override
        protected Void doInBackground(Mensagem... param) {
            int cont;
            Mensagem novaMensagem;
            PrintWriter escritor;
            Socket socket;
            String msg;
            try {
                Log.d("enviaTCP", "enviando");
                novaMensagem = param[0];
                msg = novaMensagem.remetenteUID + '\n' + String.valueOf(novaMensagem.timestamp) + '\n' + novaMensagem.mensagem + '\n';
                socket = new Socket(destinatarioIP, porta);
                escritor = new PrintWriter(socket.getOutputStream());
                escritor.write(msg);
                escritor.flush();
                escritor.close();
                //Espera resposta do destinatario
                respServidor = 0;
                cont = 0;
                while (respServidor == 0 && cont < 10){
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    cont++;
                }

                if (cont == 10) {
                    respServidor = 2;
                }

            } catch (IOException e) {
                Log.d("enviaTCP", "IOexception");
                e.printStackTrace();
            }

            Log.d("enviaTCP", "enviado: " + respServidor);
            return null;
        }
    }

    class recebeTCP extends AsyncTask<Socket, Void, Void> {
        @Override
        protected Void doInBackground(Socket... params) {
            long timestamp;
            Socket socket = params[0];
            String msg;
            String uid;
            try {
                //Recebe data do remetente
                InputStream recebido = socket.getInputStream();
                Log.d("recebeTCP", "recebido");
                BufferedReader bufferCliente = new BufferedReader(new InputStreamReader(recebido));
                //Le buffer
                uid = bufferCliente.readLine();
                timestamp = Long.parseLong(bufferCliente.readLine(), 10);
                msg = bufferCliente.readLine();
                //Envia data para o remetente
                Log.d("recebeTCP", "respondendo");
                PrintWriter cliente = new PrintWriter(
                        socket.getOutputStream(), true);
                if (destinatarioUID.equals(uid)) {
                    cliente.println(1);
                    mensagemRecebida = new Mensagem(uid, msg, timestamp);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d("RecebidoSucesso: ", mensagemRecebida.mensagem);
                            recebidoSucesso(mensagemRecebida);
                        }
                    });

                } else {
                    //Recebeu resposta de envio
                    if (uid.equals("1")) {
                        respServidor = 1;
                    } else {
                        respServidor = 2;
                        cliente.println(2);
                    }
                }
                Log.d("recebeTCP", "saindo");
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    //TODO
    //Envia notificações
    /*
    private void sendPushNotificationToReceiver() {
        FcmNotificationBuilder.initialize();
        FcmNotificationBuilder.title(remetente);
        FcmNotificationBuilder.username(remetente);
        FcmNotificationBuilder.uid(remetenteUID);
        FcmNotificationBuilder.message(mensagem.mensagem);
        FcmNotificationBuilder.firebaseToken(firebaseToken);
        FcmNotificationBuilder.receiverFirebaseToken(receiverFirebaseToken);
        FcmNotificationBuilder.send();
    }*/

    //TODO
    //Recebe notificações
    /*
    @Subscribe
    public void onPushNotificationEvent(PushNotificationEvent pushNotificationEvent) {
        if (recyclerAdapter == null || recyclerAdapter.getItemCount() == 0) {
            mChatPresenter.getMessage(FirebaseAuth.getInstance().getCurrentUser().getUid(), pushNotificationEvent.getUid());
        }
    }*/


}
