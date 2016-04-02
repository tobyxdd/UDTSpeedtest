package org.toby.toys.udtspeedtest;

import com.barchart.udt.ExceptionUDT;
import com.barchart.udt.SocketUDT;
import com.barchart.udt.TypeUDT;
import com.barchart.udt.net.NetServerSocketUDT;
import com.barchart.udt.net.NetSocketUDT;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;

public class UDTSpeedtest {

    public static final int dataSizeMB = 10;
    public static final int readPerMB = 1;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            out("use -s for server & -c for client");
            return;
        }
        for (String arg : args) {
            if (arg.equals("-s")) {
                server();
            } else if (arg.equals("-c")) {
                client();
            }
        }
    }

    public static void server() throws IOException {
        NetServerSocketUDT server = new NetServerSocketUDT();
        server.bind(new InetSocketAddress(6906));
        out("server started, port 6906");
        while (true) {
            Socket socket = server.accept();
            out(socket.getInetAddress().toString() + " connected, sending " + dataSizeMB + "MB data.");
            new Thread(new ServerSender(socket)).start();
        }
    }

    public static void client() throws IOException {
        Scanner scanner = new Scanner(System.in);
        out("Target address -");
        String addr = scanner.next();
        out("Target port -");
        int port = scanner.nextInt();
        Socket socket = new NetSocketUDT();
        socket.connect(new InetSocketAddress(addr, port));
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        out("connected, start receiving...");
        while (!socket.isClosed()) {
            byte[] bytes = new byte[readPerMB * 1024 * 1024]; //1MB
            long startTime = System.currentTimeMillis();
            inputStream.readFully(bytes);
            long endTime = System.currentTimeMillis();
            out(((bytes.length / 1024 / 1024) / ((endTime - startTime) / (float) 1000)) + " MB/s");
        }
        out("disconnected.");
    }

    public static void out(String str) {
        System.out.println(str);
    }
}

class ServerSender implements Runnable {

    public ServerSender(Socket socket) {
        this.socket = socket;
    }

    private Socket socket;

    @Override
    public void run() {
        byte[] bytes = new byte[UDTSpeedtest.dataSizeMB * 1024 * 1024];
        new Random().nextBytes(bytes);
        try {
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            outputStream.write(bytes);
            outputStream.flush();
            UDTSpeedtest.out(socket.getInetAddress().toString() + " sent.");
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
            UDTSpeedtest.out(socket.getInetAddress().toString() + " failed.");
        }
    }
}