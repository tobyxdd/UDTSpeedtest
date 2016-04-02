package org.toby.toys.udtspeedtest;

import com.barchart.udt.net.NetServerSocketUDT;
import com.barchart.udt.net.NetSocketUDT;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

public class UDTSpeedtest {

    public static final int dataSizeMB = 50;

    public static void main(String[] args) throws IOException, InterruptedException {
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

    public static void client() throws IOException, InterruptedException {
        Scanner scanner = new Scanner(System.in);
        out("Target address -");
        String addr = scanner.next();
        out("Target port -");
        int port = scanner.nextInt();
        Socket socket = new NetSocketUDT();
        socket.connect(new InetSocketAddress(addr, port));
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        out("connected, start receiving...");
        ClientReceiver receiver = new ClientReceiver(socket, new AtomicInteger(0));
        new Thread(receiver).start();
        while (receiver.isAlive()) {
            Thread.sleep(1000);
            out(((float) receiver.getReceiveCounter().get() / 1024 / 1024) + " MB/s");
            receiver.getReceiveCounter().set(0);
        }
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

class ClientReceiver implements Runnable {

    private Socket socket;
    private AtomicInteger receiveCounter;
    private boolean alive = true;

    public boolean isAlive() {
        return alive;
    }

    public ClientReceiver(Socket socket, AtomicInteger receiveCounter) {
        this.socket = socket;
        this.receiveCounter = receiveCounter;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public AtomicInteger getReceiveCounter() {
        return receiveCounter;
    }

    public void setReceiveCounter(AtomicInteger receiveCounter) {
        this.receiveCounter = receiveCounter;
    }

    @Override
    public void run() {
        try {
            InputStream stream = socket.getInputStream();
            byte[] bytes = new byte[1024 * 1024];
            int rc;
            while ((!socket.isClosed()) && ((rc = stream.read(bytes)) != -1)) {
                receiveCounter.addAndGet(rc);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            alive = false;
            UDTSpeedtest.out("disconnected.");
        }
    }


}