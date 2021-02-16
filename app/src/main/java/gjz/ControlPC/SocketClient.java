package gjz.ControlPC;

import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

public class SocketClient
{
    SocketChannel mClientChannel=null;
    Selector mSelector=null;
    private Thread mClientThread = null;
    private boolean isRuning = false;
    private ArrayList<SocketChannel> mServers;
    public ByteBuffer buffer = ByteBuffer.allocate(256);

    protected int mPort;
    protected String mIP;
    // one object connect to one server
    public SocketClient(String ip, int port)
    {
        mIP = ip;
        mPort=port;
    }

    public boolean connectSever()
    {
        SocketAddress address = new InetSocketAddress(mIP, mPort);
        try
        {
            isRuning = true;
            if (mSelector == null)
            {
                mSelector = Selector.open();
            }
            mClientChannel =  SocketChannel.open(address);
            mClientChannel.configureBlocking(false);
            mClientChannel.register(mSelector,SelectionKey.OP_READ,this);
            mClientThread = new Thread(mClientRunnable);
            mClientThread.start();
         } catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean disconnectServer()
    {
        try
        {
            isRuning = false;
            mClientThread.interrupt();
            mClientThread = null;
            mClientChannel.close();
            mClientChannel=null;
        } catch (IOException e)
        {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private Runnable mClientRunnable = new Runnable()
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
                    //Log.d("SocketServer",String.format("keysIterator.hasNext() - %b" ,keysIterator.hasNext()) );
                    Log.d("SocketServer",String.format("START selectors %d - selected number %d",mSelector.keys().size() ,mSelector.selectedKeys().size()) );
                    while (keysIterator.hasNext())
                    {
                        key = keysIterator.next();
                        //keysIterator.remove();  // important to remove,

                        Log.d("SocketClient",String.format("key is acceptalbe(%b), readable(%b), valid(%b),readyOps(%b)" ,key.isAcceptable(),key.isReadable(),key.isValid(),key.readyOps() ));
                        if ( key.isReadable())
                        {
                            try
                            {
                                read(mSelector, key);
                                //key.interestOps( );
                            }
                            catch (IOException e)
                            {
                                //keysIterator.remove();
                                SocketChannel channel = (SocketChannel) key.channel();
                                if(channel.isOpen())
                                {
                                    /*
                                    try
                                    {
                                        //channel.close();

                                    } catch (IOException ex)
                                    {
                                        ex.printStackTrace();
                                    }*/
                                }
                            }
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

    private static void read(Selector selector, SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        SocketClient ss = (SocketClient) key.attachment();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        socketChannel.read(buffer); // can be non-blocking
        buffer.flip();

        if(buffer.array()[0]!=0)
            ss.processReceiveData(socketChannel,new String(buffer.array(), StandardCharsets.US_ASCII));
        else
            key.cancel();
        //socketChannel.register(selector, SelectionKey.OP_WRITE, buffer);
        //ss.errMessage(buffer.toString());
        buffer.clear();
    }

    protected void errMessage(String s)
    {

    }

    public boolean isConnected()
    {
        if(mClientChannel == null)
            return false;
        else
            return mClientChannel.isConnected();
    }


    public boolean sentMessage(String data)
    {
        ByteBuffer buffer = ByteBuffer.wrap(data.getBytes( StandardCharsets.US_ASCII));
        if(mClientChannel !=null && mClientChannel.isConnected())
        {
            //buffer.flip();
            try
            {
                //mClientChannel.write(ByteBuffer.wrap(new String("How about this").getBytes(StandardCharsets.US_ASCII)));
                mClientChannel.write(buffer);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        return true;
    }


    public boolean processReceiveData(SocketChannel c,String b)
    {
        try
        {
            Class cls = Class.forName("gjz.ControlPC.SocketClient");
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

    public boolean socket_def(SocketChannel s, String c)
    {
        return true;
    }

    public boolean startClient()
    {
        return true;
    }
    public boolean stopClient()
    {
        return true;
    }
}
