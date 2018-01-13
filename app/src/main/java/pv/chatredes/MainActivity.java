package pv.chatredes;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
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
    private FirebaseAuth firebaseUsuario;
    //private ProgressDialog progressDialog;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout layout;
    UsuarioRecyclerAdapter recyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Getters layout
        setContentView(R.layout.activity_main);
        findViewById(R.id.botao_ok).setOnClickListener(this);
        findViewById(R.id.popup).setVisibility(GONE);
        emailDest = findViewById(R.id.email_dest);
        //Inicializa dados do Firebase
        firebaseUsuario = FirebaseAuth.getInstance();
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
                }
            }
        };
        //Listener do Firebase
        firebaseUsuario.addAuthStateListener(firebaseListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        MenuItem firebaseMenu = menu.findItem(R.id.firebase_enable);
        firebaseMenu.setEnabled(true);
        MenuItem p2pMenu = menu.findItem(R.id.p2p_enable).setEnabled(true);
        p2pMenu.setEnabled(true);
        MenuItem logoutMenu = menu.findItem(R.id.logout);
        logoutMenu.setEnabled(true);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.firebase_enable:
                p2p = false;
                break;
            case R.id.p2p_enable:
                p2p = true;
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.botao_ok) {
            //checar se email e valido
            abrirChat(emailDest.getText().toString());
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
        } else if (f != null){
            fragmentManager.beginTransaction().remove(f).commit();
            findViewById(R.id.fab).setVisibility(VISIBLE);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        } else {
            super.onBackPressed();
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

}