package com.avik.chatroom;
import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import io.socket.client.*;
public class MainActivity extends Activity {
    private Socket socket;
    private EditText nickField;
    private Button join;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        nickField = (EditText)findViewById(R.id.userField);
        join = (Button)findViewById(R.id.join);
        try{
            socket = IO.socket("http://10.0.0.33:3000");
            socket.connect();
        }catch(Exception e){
            Log.d("error", "couldnt create socket");
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socket.disconnect();
    }

    public void tryToJoin(View v){
        Log.d("Log", "trying to join");
        String name = nickField.getText().toString();
        socket.emit("new user", name, new Ack() {
            @Override
            public void call(Object... args) {
                Log.d("Log", "Callback");
                for (int i = 0; i < args.length; i++) {
                    Toast.makeText(MainActivity.this, args[i].toString(), Toast.LENGTH_SHORT).show();
                    Log.d("arg", args[i].toString());
                }
            }
        });
       /* socket.emit("new user", name, function(data){
            if(data.valid){
                $('#nickWrap').hide();
                $('#contentWrap').show();
                $('#yourname').html('You are logged in as <b>'+name+'</b>');
            }else{
                $nickError.html('That username is taken! Try again.');
            }
        });
        $nickBox.val('');*/
    }
}
