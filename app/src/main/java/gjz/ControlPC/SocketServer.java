package gjz.ControlPC;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_READ;

public class SocketServer
{
    //ServerSocket mServerSocket;
    //AsynchronousServerSocketChannel mServerChannel;
    ServerSocketChannel mServerChannel;
    Selector mSelector;
    int mPort;
    private Thread mServerThread = null;
    private boolean isRuning = false;
    private ArrayList< SocketChannel> mClients;
    public ByteBuffer buffer = ByteBuffer.allocate(256);

    public SocketServer(int port)
    {
        mPort = port;
        mClients = new ArrayList<SocketChannel>();
     }

    public boolean startServer()
    {
        try {
            // server : create a server on a port
            mServerChannel= ServerSocketChannel.open();
            mSelector = Selector.open();
            mServerChannel.configureBlocking(false);

            InetSocketAddress hostAddress = new InetSocketAddress((InetAddress) null, mPort);
            mServerChannel.bind(hostAddress);

            //mServerChannel.register(mSelector,OP_READ,this);
            mServerChannel.register(mSelector,OP_ACCEPT,this);

            //Log.d("SocketServer",String.format("mServerChannel.register() size - %d" ,mSelector.keys().size()) );
            //Log.d("SocketServer",String.format("mServerChannel.register() - %b" ,mSelector.keys().iterator().hasNext()) );

            //mServerChannel.accept(null,mAcceptCompletionHandler);
            mServerThread = new Thread(mServerRunnable);
            mServerThread.start();
            isRuning =true;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
            errMessage(e.getMessage());
            return false;
        }
        return true;
    }

    public boolean stopServer()
    {
        if( mServerChannel != null)
        {
            try
            {
                //mServerThread.
                isRuning = false;
                mServerThread.interrupt();
                mServerChannel.socket().close();
                //mServerChannel.close();
                mServerChannel = null;
                //mSelector.close();
                //mSelector = null;
            }
            catch (IOException e)
            {
                // e.printStackTrace();
                errMessage(e.getMessage());
                return false;
            }
        }
        return true;
    }

    public boolean processReceiveData(SocketChannel c,String b)
    {
        try
        {
            Class cls = Class.forName("gjz.ControlPC.SocketServer");
            //Method [] x =  cls.getMethods();
            //Method m = SocketServer.class.getMethod("socket_" + "def", new Class[]{AsynchronousSocketChannel.class, String.class});
            Method m = cls.getMethod("socket_" + "def", new Class[]{SocketChannel.class, String.class});
            return (boolean) m.invoke(this,c,b);
        }
        catch (IllegalAccessException | NoSuchMethodException | ClassNotFoundException | InvocationTargetException e)
        {
            errMessage(e.getMessage());
        }
        return true;
    }
    public boolean processAcceptData(SocketChannel c)
    {
        mClients.add(c);
        return true;
    }

    public boolean socket_def(SocketChannel s, String c)
    {
        return true;
    }

    public boolean sentMessage(String data)
    {
        Iterator i;
        SocketChannel clientSocket;
        BufferedWriter bufferedWriter;
        i = mClients.iterator();
        ByteBuffer buffer ;

        while (i.hasNext())
        {
            clientSocket =(SocketChannel) i.next();
            //i.remove();
            try
            {
                buffer=ByteBuffer.wrap(data.getBytes());
                clientSocket.write(buffer);
                //clientSocket.write("\4");
            }
            catch (Exception e)
            {
            }

        }
        return true;
    }

    private Runnable mServerRunnable = new Runnable()
    {
        public void run()
        {
            // TODO : check input from a client (ip)
            Socket clientSocket = null;
            BufferedReader bufferedReader	= null;
            char[] buffer = new char[256];
            int count = 0;
            Iterator i;
            while (isRuning && mSelector.isOpen())
            {
                SelectionKey key = null;
                // accept
                try
                {
                    //Log.d("SocketServer",String.format("mSelector.Keys() - %d" ,mSelector.keys().size()) );
                    mSelector.select(1000);
                    Iterator<SelectionKey> keysIterator = mSelector.selectedKeys().iterator();
                    Log.d("SocketServer",String.format("keysIterator.hasNext() - %b" ,keysIterator.hasNext()) );
                    Log.d("SocketServer",String.format("START selectors %d - selected number %d",mSelector.keys().size() ,mSelector.selectedKeys().size()) );
                    while (keysIterator.hasNext())
                    {
                        key = keysIterator.next();
                        //keysIterator.remove();  // important to remove,

                        Log.d("SocketServer",String.format("key is acceptalbe(%b), readable(%b), valid(%b),readyOps(%b)" ,key.isAcceptable(),key.isReadable(),key.isValid(),key.readyOps() ));
                        if (key.isAcceptable()) {
                            accept(mSelector, key);
                        }
                        if ( key.isReadable()) {
                            read(mSelector, key);
                            //key.interestOps( );
                        }
                        if (key.isWritable()) {
                            //keysIterator.remove();
                            //write(key);
                        }
                    }
                    mSelector.selectedKeys().clear();
                    //Log.d("SocketServer",String.format("END   selectors %d - selected number %d",mSelector.keys().size() ,mSelector.selectedKeys().size()) );
                }
                catch (IOException e)
                {
                    errMessage(e.getMessage());
                }
                catch(CancelledKeyException e)
                {
                    if(key !=null) key.cancel();
                }
            }
        }
    };

    private static void accept(Selector selector, SelectionKey key) throws IOException
    {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketServer ss = (SocketServer) key.attachment();
        SocketChannel socketChannel = serverSocketChannel.accept(); // can be non-blocking
        if (socketChannel != null) {
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, OP_READ,ss);
            ss.mClients.add(socketChannel);
            ss.errMessage("client("+socketChannel.getRemoteAddress().toString().substring(1)+") connected.");
        }
    }
    private static void read(Selector selector, SelectionKey key)
    {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        SocketServer ss = (SocketServer) key.attachment();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        try
        {
            socketChannel.read(buffer); // can be non-blocking
            //socketChannel.
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        buffer.flip();

        if(buffer.array()[0]!=0)
            ss.processReceiveData(socketChannel,new String(buffer.array(), StandardCharsets.US_ASCII));
        else
            key.cancel();
        //socketChannel.register(selector, SelectionKey.OP_WRITE, buffer);
        //ss.errMessage(buffer.toString());
        buffer.clear();
    }

    private static void write(SelectionKey key) throws IOException
    {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        socketChannel.write(buffer); // can be non-blocking
        socketChannel.close();
    }
    protected void errMessage(String s)
    {

    }
}