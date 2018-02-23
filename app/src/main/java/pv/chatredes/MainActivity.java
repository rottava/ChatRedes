package pv.chatredes;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import pv.chatredes.chat.ChatFragment;
import pv.chatredes.local.Armazenamento;
import pv.chatredes.login.AuthActivity;
import pv.chatredes.usuario.User;
import pv.chatredes.usuario.UsuarioRecyclerAdapter;
import pv.chatredes.usuario.UsuarioRecyclerListener;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, UsuarioRecyclerListener.OnItemClickListener, SwipeRefreshLayout.OnRefreshListener {

    private ArrayList<User> usuarios;
    private boolean p2p;
    private EditText emailDest;
    private EditText ipDest;
    private FirebaseAuth firebaseUsuario;
    private FirebaseDatabase database;
    //private ProgressDialog progressDialog;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout layout;
    private TextView meuIP;
    UsuarioRecyclerAdapter recyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Getters layout
        setContentView(R.layout.activity_main);
        findViewById(R.id.botao_ok).setOnClickListener(this);
        findViewById(R.id.botao_meuip).setOnClickListener(this);
        findViewById(R.id.botao_dest_ip).setOnClickListener(this);
        findViewById(R.id.popup).setVisibility(GONE);
        findViewById(R.id.meuip).setVisibility(GONE);
        findViewById(R.id.ipdest).setVisibility(GONE);
        findViewById(R.id.p2p_enable).setVisibility(GONE);
        emailDest = findViewById(R.id.email_dest);
        ipDest = findViewById(R.id.new_ip_dest);
        meuIP = findViewById(R.id.txt_ip);
        //Inicializa dados do Firebase
        firebaseUsuario = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        //Inicializa toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //Inicializa FAB
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (findViewById(R.id.popup).getVisibility() == GONE) {
                    findViewById(R.id.popup).setVisibility(VISIBLE);
                } else {
                    findViewById(R.id.popup).setVisibility(GONE);
                }
            }
        });
        //Janela de espera
        /*
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.carregando));
        progressDialog.setMessage(getString(R.string.esperando));
        progressDialog.setIndeterminate(true);
        */
        //Seta modo de operação inicial para FIREBASE
        p2p = false;
        //Atualiza usuarios
        getUsuarios();
        //Inicia recycle view das conversas
        recyclerView = findViewById(R.id.recycler_conversas);
        recyclerAdapter = new UsuarioRecyclerAdapter(usuarios);
        recyclerView.setAdapter(recyclerAdapter);
        layout = findViewById(R.id.swipe_refresh_layout);
        //Preenche recycle view das conversas
        layout.post(new Runnable() {
            @Override
            public void run() {
                layout.setRefreshing(true);
            }
        });
        UsuarioRecyclerListener.addTo(recyclerView).setOnItemClickListener(this);
        layout.setOnRefreshListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Verifica se usuario está logado
        FirebaseAuth.AuthStateListener firebaseListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser == null) {
                    //Vai para tela de registro
                    Intent intent = new Intent(MainActivity.this, AuthActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                    String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
                    FirebaseUser user = firebaseUsuario.getCurrentUser();
                    database.getReference().child("usuarios").child(user.getEmail().replace(".","")).setValue(new User(user.getEmail(), user.getUid(), ip));
                    Log.d("IP", "Novo IP == " + ip);
                }
            }
        };
        //Listener do Firebase
        firebaseUsuario.addAuthStateListener(firebaseListener);
        p2p=true;
        findViewById(R.id.p2p_enable).setVisibility(VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        MenuItem firebaseMenu = menu.findItem(R.id.firebase_enable);
        firebaseMenu.setEnabled(true);
        MenuItem p2pMenu = menu.findItem(R.id.p2p_enable);
        p2pMenu.setEnabled(true);
        MenuItem logoutMenu = menu.findItem(R.id.logout);
        logoutMenu.setEnabled(true);
        MenuItem meuIp = menu.findItem(R.id.meu_ip);
        meuIp.setEnabled(true);
        MenuItem destIp = menu.findItem(R.id.dest_ip);
        destIp.setEnabled(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.firebase_enable:
                p2p = false;
                findViewById(R.id.p2p_enable).setVisibility(GONE);
                //Toast.makeText(this, "P2P Desabilitado", Toast.LENGTH_SHORT).show();
                break;
            case R.id.p2p_enable:
                p2p = true;
                findViewById(R.id.p2p_enable).setVisibility(VISIBLE);
                //Toast.makeText(this, "P2P Habilitado", Toast.LENGTH_SHORT).show();
                break;
            case R.id.logout:
                new AlertDialog.Builder(this).setTitle(R.string.sair).setMessage(R.string.desconectar).setPositiveButton(R.string.sair, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        firebaseUsuario.signOut();
                        //Vai para tela de registro
                        Intent intent = new Intent(MainActivity.this, AuthActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }).setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
                break;
            case R.id.meu_ip:
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
                meuIP.setText(Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress()));
                findViewById(R.id.meuip).setVisibility(VISIBLE);
                break;
            case R.id.dest_ip:
                findViewById(R.id.ipdest).setVisibility(VISIBLE);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        switch (i) {
            case R.id.botao_ok:
                abrirChat(emailDest.getText().toString());
                break;
            case R.id.botao_meuip:
                findViewById(R.id.meuip).setVisibility(GONE);
                break;
            case R.id.botao_dest_ip:
                int pos;
                if (ipDest.getText().toString().equals("")){
                    findViewById(R.id.ipdest).setVisibility(GONE);
                } else {
                    if (validate(ipDest.getText().toString())) {
                        pos = contem(emailDest.getText().toString());
                        usuarios.get(pos).ip = ipDest.getText().toString();
                        Fragment f = getSupportFragmentManager().findFragmentById(R.id.frame_layout_content_chat);
                        FragmentManager fragmentManager = getSupportFragmentManager();
                        fragmentManager.beginTransaction().remove(f).commit();
                        abrirChat(emailDest.getText().toString());
                    } else {
                        Toast.makeText(this, "email não registrado!", Toast.LENGTH_SHORT).show();
                    }
                }
                break;
        }
    }

    @Override
    public void onItemClicked(RecyclerView recyclerView, int position, View v) {
        abrirChat(usuarios.get(position).email);
        //Log.d("Clicked:", " " + position);
    }

    @Override
    public void onRefresh() {
        getUsuarios();
    }

    @Override
    public void onResume(){
        if(getSupportFragmentManager().getBackStackEntryCount() == 0) {
            //Torna popup invisivel
            findViewById(R.id.popup).setVisibility(GONE);
            findViewById(R.id.meuip).setVisibility(GONE);
            findViewById(R.id.ipdest).setVisibility(GONE);
            //Torna FAB visivel
            findViewById(R.id.fab).setVisibility(VISIBLE);
        }
        super.onResume();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.frame_layout_content_chat);
        FragmentManager fragmentManager = getSupportFragmentManager();
        if(findViewById(R.id.popup).getVisibility() == VISIBLE) {
            findViewById(R.id.popup).setVisibility(GONE);
        } else {
            if (findViewById(R.id.meuip).getVisibility() == VISIBLE) {
                findViewById(R.id.meuip).setVisibility(GONE);
            } else {
                if (findViewById(R.id.ipdest).getVisibility() == VISIBLE) {
                    findViewById(R.id.ipdest).setVisibility(GONE);
                } else {
                    if (f != null){
                        fragmentManager.beginTransaction().remove(f).commit();
                        findViewById(R.id.fab).setVisibility(VISIBLE);
                        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                        getSupportActionBar().setDisplayShowHomeEnabled(false);
                    } else {
                        super.onBackPressed();
                    }
                }
            }
        }
    }

    //Carrega usuarios do banco de dados escolhido
    private void getUsuarios() {
        if (p2p) {
            lerDadosP2P();
        } else {
            lerDadosFirebase();
        }
    }

    //Salva dados local
    private void salvarDados(){
        try {
            Armazenamento.salvarDados(this, "usuarios", usuarios);
        } catch (IOException e) {
            Log.e("MainActivity", e.getMessage());
        }

    }

    //Carrega dados local
    private void lerDadosP2P(){
        usuarios = new ArrayList<>();

        try {
            usuarios = (ArrayList<User>) Armazenamento.lerDados(this, "usuarios");
            recebidoSucesso();
        } catch (IOException e) {
            recebidoFalha(e.getMessage());
            Log.e("MainActivity", e.getMessage());
        } catch (ClassNotFoundException e) {
            recebidoFalha(e.getMessage());
            Log.e("MainActivity_a", e.getMessage());
        }

    }

    //Carrega dados do Firebase
    private void lerDadosFirebase(){
        usuarios = new ArrayList<>();
        //showProgressDialog();
        FirebaseDatabase.getInstance().getReference().child("usuarios").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> dataSnapshots = dataSnapshot.getChildren().iterator();
                usuarios = new ArrayList<>();
                while (dataSnapshots.hasNext()) {
                    DataSnapshot dataSnapshotChild = dataSnapshots.next();
                    User usuario = dataSnapshotChild.getValue(User.class);
                    //Log.d("email: ", usuario.email);
                    //Log.d("ui: ", usuario.uid);
                    if (!TextUtils.equals(usuario.uid, firebaseUsuario.getCurrentUser().getUid())) {
                       usuarios.add(usuario);
                    }
                }
                //hideProgressDialog();
                recebidoSucesso();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                recebidoFalha(databaseError.getMessage());
            }
        });
    }

    //Usuario recebido com sucesso atualiza recycle view e salva
    public void recebidoSucesso() {
        //Atualiza recycle view
        layout.post(new Runnable() {
            @Override
            public void run() {
                layout.setRefreshing(false);
            }
        });
        UsuarioRecyclerAdapter recyclerAdapter = new UsuarioRecyclerAdapter(usuarios);
        recyclerView.setAdapter(recyclerAdapter);
        recyclerAdapter.notifyDataSetChanged();
        //Atualiza
        salvarDados();
    }

    //Usuario recebido com falha mostra aviso
    public void recebidoFalha(String mensagem) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
    }

    //Faz aparecer a janela de espera
    /*
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.carregando));
            progressDialog.setIndeterminate(true);
        }

        progressDialog.show();
    }*/

    //Faz desaparecer a janela de espera
    /*
    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }*/

    //Inicia o fragment ChatFragment
    private void abrirChat(String destinatario){
        int pos;
        pos = contem(destinatario);
        //Verifica se a conversa existe no banco de dados local
        if( pos >= usuarios.size()) {
            //Não existe, erro
            Toast.makeText(this, "email não registrado!", Toast.LENGTH_SHORT).show();
        } else {
            emailDest.setText(destinatario);
            //Iniciar fragmento
            Fragment fragment = null;
            try {
                fragment = ChatFragment.class.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //Argumentos passados para o fragmento
            Bundle args = new Bundle();
            args.putString("destinatario", destinatario);
            args.putString("destinatarioUID", usuarios.get(pos).uid);
            args.putString("destinatarioIP", usuarios.get(pos).ip);
            args.putString("remetente", firebaseUsuario.getCurrentUser().getEmail());
            args.putString("remetenteUID", firebaseUsuario.getCurrentUser().getUid());
            args.putBoolean("p2p", p2p);
            fragment.setArguments(args);
            //Mudar foco para fragmento
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.frame_layout_content_chat, fragment);
            //fragmentTransaction.replace(R.id.frame_layout_content_chat, fragment);
            fragmentTransaction.commit();
        }
    }

    //Retorna posição da conversa no banco de dados local
    private int contem(String destinatario) {
        int i;

        for(i = 0; i < usuarios.size(); i++) {
            if (usuarios.get(i).email.equals(destinatario)) {
                return i;
            }
        }

        return i;
    }

    public static boolean validate(final String ip) {
        String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

        return ip.matches(PATTERN);
    }
}