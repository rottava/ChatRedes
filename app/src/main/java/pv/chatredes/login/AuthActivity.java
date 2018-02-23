package pv.chatredes.login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import pv.chatredes.MainActivity;
import pv.chatredes.R;
import pv.chatredes.usuario.User;

public class AuthActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "AuthActivity";
    private FirebaseAuth firebaseUsuario;
    private FirebaseDatabase database;
    private TextView detalheView;
    private EditText emailView;
    private EditText passView;
    private ProgressDialog progressDialog;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Views
        emailView = findViewById(R.id.campo_email);
        passView = findViewById(R.id.campo_senha);
        //estadoView = findViewById(R.id.texto_titulo);
        detalheView = findViewById(R.id.texto_conectado);

        // Buttons
        findViewById(R.id.botao_entrar).setOnClickListener(this);
        findViewById(R.id.botao_criar_conta).setOnClickListener(this);
        //findViewById(R.id.botao_sair).setOnClickListener(this);
        findViewById(R.id.botao_prosseguir).setOnClickListener(this);
        findViewById(R.id.botao_verificar_email).setOnClickListener(this);

        // Firebase
        firebaseUsuario = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
    }

    @Override
    public void onStart() {
        super.onStart();
        atualizarDados(firebaseUsuario.getCurrentUser());
    }

    @Override
    public void onStop() {
        super.onStop();
        hideProgressDialog();
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.botao_criar_conta) {
            criarConta(emailView.getText().toString(), passView.getText().toString());
        } else if (i == R.id.botao_entrar) {
            entrar(emailView.getText().toString(), passView.getText().toString());
            //} else if (i == R.id.botao_sair) {
            //    sair();
        } else if (i == R.id.botao_verificar_email) {
            validarEmail();
        } else if (i == R.id.botao_prosseguir) {
            prosseguir();
        }
    }

    private void criarConta(String email, String senha) {
        Log.d(TAG, "Registro:" + email);
        if (!validarEntrada()) {
            return;
        }

        showProgressDialog();

        firebaseUsuario.createUserWithEmailAndPassword(email, senha).addOnCompleteListener(AuthActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Estado: Concluido");
                            FirebaseUser user = firebaseUsuario.getCurrentUser();
                            database.getReference().child("usuarios").child(user.getEmail().replace(".","")).setValue(new User(user.getEmail(), user.getUid(), "IP"));
                            atualizarDados(user);
                        } else {
                            Log.w(TAG, "Estado: Falhou", task.getException());
                            Toast.makeText(AuthActivity.this, "Autenticação falhou",
                                    Toast.LENGTH_SHORT).show();
                            atualizarDados(null);
                        }

                        hideProgressDialog();
                    }
                });
    }

    private void entrar(String email, String senha) {
        Log.d(TAG, "Conectando:" + email);
        if (!validarEntrada()) {
            return;
        }

        showProgressDialog();

        firebaseUsuario.signInWithEmailAndPassword(email, senha).addOnCompleteListener(AuthActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Estado: Concluido");
                            FirebaseUser user = firebaseUsuario.getCurrentUser();
                            atualizarDados(user);
                        } else {
                            // If sign in fails, display a mensagem to the user.
                            Log.w(TAG, "Estado: Falhou", task.getException());
                            Toast.makeText(AuthActivity.this, "Autenticação falhou",
                                    Toast.LENGTH_SHORT).show();
                            atualizarDados(null);
                        }

                        if (!task.isSuccessful()) {
                            detalheView.setText(R.string.auth_failed);
                        }

                        hideProgressDialog();
                    }
                });
    }

    private void sair() {
        firebaseUsuario.signOut();
        atualizarDados(null);
    }

    private void prosseguir() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void validarEmail() {
        findViewById(R.id.botao_verificar_email).setEnabled(false);
        final FirebaseUser usuario = firebaseUsuario.getCurrentUser();

        usuario.sendEmailVerification().addOnCompleteListener(AuthActivity.this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        findViewById(R.id.botao_verificar_email).setEnabled(true);

                        if (task.isSuccessful()) {
                            Toast.makeText(AuthActivity.this,
                                    "E-mail de verificação enviado para" + usuario.getEmail(),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Log.e(TAG, "Enviar e-mail: Falhou", task.getException());
                            Toast.makeText(AuthActivity.this,
                                    "E-mail de verificação falhou",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private boolean validarEntrada() {
        boolean valido = true;

        String email = emailView.getText().toString();
        if (TextUtils.isEmpty(email)) {
            emailView.setError("Obrigatório");
            valido = false;
        } else {
            emailView.setError(null);
        }

        String senha = passView.getText().toString();
        if (TextUtils.isEmpty(senha)) {
            passView.setError("Obrigatório");
            valido = false;
        } else {
            passView.setError(null);
        }

        return valido;
    }

    private void atualizarDados(FirebaseUser usuario) {
        if (usuario != null) {
            detalheView.setText(getString(R.string.autenticacao_status_fmt,
                    usuario.getEmail(), usuario.isEmailVerified()));

            findViewById(R.id.texto_titulo).setVisibility(View.GONE);
            findViewById(R.id.botoes_autenticacao).setVisibility(View.GONE);
            findViewById(R.id.campo_autenticacao).setVisibility(View.GONE);
            findViewById(R.id.botoes_autenticado).setVisibility(View.VISIBLE);
            findViewById(R.id.texto_conectado).setVisibility(View.VISIBLE);

            findViewById(R.id.botao_verificar_email).setEnabled(!usuario.isEmailVerified());
        } else {
            detalheView.setText(R.string.desconectado);

            findViewById(R.id.texto_titulo).setVisibility(View.VISIBLE);
            findViewById(R.id.botoes_autenticacao).setVisibility(View.VISIBLE);
            findViewById(R.id.campo_autenticacao).setVisibility(View.VISIBLE);
            findViewById(R.id.botoes_autenticado).setVisibility(View.GONE);
            findViewById(R.id.texto_conectado).setVisibility(View.GONE);
        }
    }

    public void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.carregando));
            progressDialog.setIndeterminate(true);
        }

        progressDialog.show();
    }

    public void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
    
}