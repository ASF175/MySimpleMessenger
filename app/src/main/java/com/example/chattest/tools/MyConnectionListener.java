package com.example.chattest.tools;

import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;

public class MyConnectionListener {

    private boolean networkAvailable;
    private ArrayList<ConnectionHandler> connectionHandlerList;
    private Thread onlineCheckerThread;
    private int TIME_OUT = 2500;

    public MyConnectionListener(int timeout) {
        connectionHandlerList = new ArrayList<>();
        networkAvailable = false;
        TIME_OUT = timeout;
    }

    public MyConnectionListener() {
        connectionHandlerList = new ArrayList<>();
        networkAvailable = false;
    }

    public boolean isNetworkAvailable() {
        return networkAvailable;
    }

    public void addConnectionListener(@NonNull ConnectionHandler connectionHandler) {
        connectionHandlerList.add(connectionHandler);
    }

    public void removeConnectionListener(@NonNull ConnectionHandler connectionHandler) {
        connectionHandlerList.remove(connectionHandler);
    }

    public void setConnectionListener() {
        if (onlineCheckerThread != null)
            return;

        final Handler handler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                switch (msg.what) {
                    case 1:
                        networkAvailable = true;
                        for (ConnectionHandler connectionHandler : connectionHandlerList)
                            connectionHandler.onNetworkAvailableChange(true);

                        break;
                    case 2:
                        networkAvailable = false;
                        for (ConnectionHandler connectionHandler : connectionHandlerList)
                            connectionHandler.onNetworkAvailableChange(false);

                        break;
                }
            }
        };

        onlineCheckerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        if (connectionHandlerList.size() != 0) {
                            boolean flag = checkConnection();
                            if (flag & !networkAvailable) {
                                handler.sendEmptyMessage(1);
                            } else if (!flag & networkAvailable) {
                                handler.sendEmptyMessage(2);
                            }
                            Thread.sleep(TIME_OUT);
                        } else {
                            Thread.sleep(500);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        });
        onlineCheckerThread.setDaemon(true);
        onlineCheckerThread.start();
    }

    public void removeAllConnectionListeners() {
        if (onlineCheckerThread == null)
            return;

        onlineCheckerThread.interrupt();
        onlineCheckerThread = null;
        connectionHandlerList.clear();
    }

    public boolean checkConnection() {
        try {
            int timeoutMs = TIME_OUT;
            Socket sock = new Socket();
            SocketAddress sockaddr = new InetSocketAddress("8.8.8.8", 53);

            sock.connect(sockaddr, timeoutMs);
            sock.close();

            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public interface ConnectionHandler {
        void onNetworkAvailableChange(boolean networkAvailable);
    }
}