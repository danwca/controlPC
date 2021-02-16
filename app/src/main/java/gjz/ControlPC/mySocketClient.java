package gjz.ControlPC;

import java.nio.channels.SocketChannel;

public class mySocketClient extends SocketClient
{
    private ControlPCActivity mContext;
    public mySocketClient(ControlPCActivity context,String ip, int port)
    {
        super(ip, port);
        mContext = context;
    }

    @Override public boolean socket_def(SocketChannel s, String c)
    {
        final String x=c;
        mContext.clientMessage("receive :"+x);    // 刷新
        return true;
    }

    @Override public boolean connectSever()
    {
        boolean ret=super.connectSever();
        if(ret )
        {
            mContext.clientMessage(String.format("connected to %s:%d", mIP, mPort));
        }
        else
        {
            mContext.clientMessage("connect to server(" + mIP + ") failed.");
        }
        return ret;
    }

    @Override public boolean disconnectServer()
    {
        boolean ret= super.disconnectServer();
        if(ret )
            mContext.clientMessage("disconnected from "+ mIP);
        else
            mContext.clientMessage("disconnect from server(" + mIP +") failed.");
        return ret;
    }
}
