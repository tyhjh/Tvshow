package com.example.tyhj.tvshow.utils;

import android.graphics.Bitmap;
import android.util.Log;

import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.textline.TextLineCodecFactory;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;

public class Connect {

    private static String u_id = null;

    private static String init, signUp, signIn, singleTalk, reconnect, getFriends,getNewMsg;

    private static IoSession session;

    private static NioSocketConnector connector;

    private static volatile Connect connect = null;

    IoHandlerAdapter minaClientHandler;
    ConnectFuture cf;

    private static String ip;

    private static int port;


    SendBordCast sendBordCast;

    //初始化
    private Connect(String ip, int port, SendBordCast sendBordCast) {
        this.ip = ip;
        this.port = port;
        this.sendBordCast = sendBordCast;
        try {
            //Create TCP/IP connection
            if (connector == null) {

                //Log.e("Connect","执行了");

                connector = new NioSocketConnector();

                //创建接受数据的过滤器
                DefaultIoFilterChainBuilder chain = connector.getFilterChain();
                TextLineCodecFactory textLineCodecFactory=new TextLineCodecFactory();
                textLineCodecFactory.setDecoderMaxLineLength(100000000);
                textLineCodecFactory.setEncoderMaxLineLength(100000000);
                //设定这个过滤器将一行一行(/r/n)的读取数据
                chain.addLast("myChin", new ProtocolCodecFilter(textLineCodecFactory));
                minaClientHandler = new MinaClientHandler(this, sendBordCast);

                //客户端的消息处理器：一个SamplMinaServerHander对象
                connector.setHandler(minaClientHandler);

                //set connect timeout
                connector.setConnectTimeout(10);
            }
            //连接到服务器：
            cf = connector.connect(new InetSocketAddress(ip, port));

            //Wait for the connection attempt to be finished.
            cf.awaitUninterruptibly();
            try {
                session = cf.getSession();
            } catch (Exception e) {
                e.printStackTrace();
                Connect.setReternMsg("服务器出错", "init");
                System.out.println("Connect+服务器无响应");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //获取实例
    public static Connect getInstance(String ip, int port, SendBordCast sendBordCast) {
        // if already inited, no need to get lock everytime
        if (connect == null || session == null || !session.isConnected()) {
            synchronized (Connect.class) {
                if (connect == null || session == null || !session.isConnected()) {
                    connect = new Connect(ip, port, sendBordCast);
                }
            }
        }
        if (session != null)
            return connect;
        Connect.setReternMsg("服务器出错", "init");
        return null;
    }

    //发送消息
    public void sendMsg(String msg) {
        if (session == null) {
            setReternMsg("服务器出错", "singleTalk");
            return;
        }
        //Log.e("大小：",msg.length()+""+msg);
        session.write(msg);
    }

    //登陆
    public void signIn(String email, String pwd) {
        JSONObject jsonObject = new JSONObject();
        if (session == null) {
            setReternMsg("服务器出错", "signIn");
            return;
        }
        try {
            jsonObject.put("action", "signIn");
            jsonObject.put("email", email);
            jsonObject.put("pwd", pwd);
            session.write(jsonObject.toString());
            u_id = email;
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    //创建用户
    public void signUp(String name, String email, String pwd) {
        JSONObject msg;
        if (session == null) {
            setReternMsg("服务器出错", "signUp");
            return;
        }
        try {
            msg = new JSONObject()
                    .put("action", "signUp")
                    .put("name", name)
                    .put("email", email)
                    .put("pwd", pwd);
            session.write(msg.toString());
        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public String getU_id() {
        return u_id;
    }

    public static String getReternMsg(String action) {
        switch (action) {
            case "init":
                return Connect.init;
            case "signUp":
                return Connect.signUp;
            case "signIn":
                return Connect.signIn;
            case "singleTalk":
                return Connect.singleTalk;
            case "getFriends":
                return Connect.getFriends;
            case "getNewMsg":
                return Connect.getNewMsg;
        }
        return null;
    }

    public static void setReternMsg(String reternMsg, String action) {
        switch (action) {
            case "init":
                Connect.init = reternMsg;
                break;
            case "signUp":
                Connect.signUp = reternMsg;
                break;
            case "signIn":
                Connect.signIn = reternMsg;
                break;
            case "singleTalk":
                Connect.singleTalk = reternMsg;
                break;
            case "getFriends":
                Connect.getFriends = reternMsg;
                break;
            case "getNewMsg":
                Connect.getNewMsg=reternMsg;
                break;
        }
    }

    public IoSession getSession() {
        return session;
    }
}
