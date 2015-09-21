package com.avik.chatroom;
import android.app.Activity;
import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.*;
import io.socket.emitter.Emitter;

public class MainActivity extends Activity {
    private Socket socket;
    private EditText nickField;
    private Button join;
    private EditText messField;
    private TextView content;
    private String styledMsgs;
    private ScrollView scrollView;
    private ImageButton sendButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        styledMsgs="";
        setContentView(R.layout.activity_main);
        nickField = (EditText)findViewById(R.id.userField);
        messField = (EditText)findViewById(R.id.messField);
        content = (TextView)findViewById(R.id.content);
        join = (Button)findViewById(R.id.join);
        scrollView = (ScrollView)findViewById(R.id.scrollView);
        sendButton=(ImageButton)findViewById(R.id.send);
        messField.setVisibility(View.GONE);
        sendButton.setVisibility(View.GONE);
        content.setVisibility(View.GONE);
        messField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    if (messField.getText().toString().length() > 0) {
                        scrollView.post(new Runnable() {
                            @Override
                            public void run() {
                                scrollView.fullScroll(View.FOCUS_DOWN);
                            }
                        });
                        socket.emit("send message", messField.getText().toString(), new Ack() {
                            @Override
                            public void call(Object... args) {//only called if there is an error
                                String msg = (String) args[0];
                                styledMsgs += "<font color = #ff0000>" + msg + "</font><br/>";
                                content.setText(Html.fromHtml(styledMsgs));
                            }
                        });
                        /*socket.emit('send message', $messageBox.val(), function(data){
						    $chat.append('<span class="error">'+data+"<span><br/>");
					    });*/
                    }
                    messField.setText("");
                    handled = true;
                }
                return handled;
            }
        });
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (messField.getText().toString().length() > 0) {
                    scrollView.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.fullScroll(View.FOCUS_DOWN);
                        }
                    });
                    socket.emit("send message", messField.getText().toString(), new Ack() {
                        @Override
                        public void call(Object... args) {//only called if there is an error
                            String msg = (String) args[0];
                            styledMsgs += "<font color = #ff0000>" + msg + "</font><br/>";
                            content.setText(Html.fromHtml(styledMsgs));
                        }
                    });
                        /*socket.emit('send message', $messageBox.val(), function(data){
						    $chat.append('<span class="error">'+data+"<span><br/>");
					    });*/
                }
                messField.setText("");
            }
        });
        try{
            socket = IO.socket("http://basicnodechat.herokuapp.com/");
            socket.connect();
        }catch(Exception e) {
            Log.d("error", "couldnt create socket");
            e.printStackTrace();
        }
        socket.on("load old msgs", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONArray docs = (JSONArray)args[0];
                for(int i = 0; i < docs.length(); i++){
                    try {
                        displayMsg((JSONObject)docs.get(i));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        socket.on("new message", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject o = (JSONObject)args[0];
                try {
                    displayMsg(o);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        socket.on("whisper", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject o = (JSONObject)args[0];
                try {
                    displayWhisper(o);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void displayMsg(JSONObject msg) throws JSONException{
        styledMsgs+="<b><font color = "+msg.getString("color")+">"+msg.getString("nick")+": </font></b>"+msg.getString("msg")+"<br/>";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                content.setText(Html.fromHtml(styledMsgs));
            }
        });
    }

    public void displayWhisper(JSONObject msg) throws JSONException{
        styledMsgs+="<b><font color = >"+msg.getString("nick")+": </font></b>"+msg.getString("msg")+"<br/>";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                content.setText(Html.fromHtml(styledMsgs));
            }
        });
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
                JSONObject data = (JSONObject)args[0];
                boolean valid = false;
                try {
                    valid = data.getBoolean("valid");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(valid){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            nickField.setVisibility(View.GONE);
                            join.setVisibility(View.GONE);
                            sendButton.setVisibility(View.VISIBLE);
                            content.setVisibility(View.VISIBLE);
                            messField.setVisibility(View.VISIBLE);
                        }
                    });
                }else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            nickField.setText("");
                            nickField.setHint("That username is taken. Try again.");
                        }
                    });
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
