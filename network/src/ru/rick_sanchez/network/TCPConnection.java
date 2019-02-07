package ru.rick_sanchez.network;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.Charset;

public class TCPConnection {
    private final Socket socket;
    private final Thread rxTxread;
    private final TCPConnectionListener eventListener;
    private final BufferedReader in;
    private final BufferedWriter out;

    public TCPConnection(TCPConnectionListener eventListener, String ipAddr, int port) throws IOException {
        this(eventListener, new Socket(ipAddr, port));
    }

    public TCPConnection(TCPConnectionListener eventListener,Socket socket) throws IOException {
        this.eventListener = eventListener;
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), Charset.forName("UTF-8")));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), Charset.forName("UTF-8")));
        rxTxread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    eventListener.onConnectionReady(TCPConnection.this);
                    while(!rxTxread.isInterrupted()){
//                        String msg = in.readLine();
                        eventListener.onReceiveString(TCPConnection.this, in.readLine());
                    }
                    String msg = in.readLine();
                }catch(IOException e){
                    eventListener.onException(TCPConnection.this, e);
                }finally {
                    eventListener.onDisconnect(TCPConnection.this);
                }
            }
        });
        rxTxread.start();
    }

    public synchronized void sendString(String value){
        try {
            out.write(value+"\r\n");
            out.flush();
        }catch(IOException e){
            eventListener.onException(TCPConnection.this,e);
            disconnect();
        }
    }

    public synchronized void disconnect(){
        rxTxread.interrupt();
        try {
            socket.close();
        }catch(IOException e){
            eventListener.onException(TCPConnection.this, e );
        }
    }

    @Override
    public String toString() {
        return "TCPConnection: "+socket.getInetAddress()+ ": "+socket.getPort();
    }
}
