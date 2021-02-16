package gjz.ControlPC;

//import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.SocketChannel;

public class mySocketServer extends SocketServer
{
    ControlPCActivity mContext;
    public mySocketServer(ControlPCActivity context,int port)
    {
        super(port);
        mContext = context;
    }

    @Override public boolean startServer()
    {
        boolean ret;
        ret = super.startServer();
        if(ret)
        {
            mContext.createServerButton.setText("停止服务");
            mContext.serverMessage("服务器已经启动");
            //mContext.getLocalIpAddress();
        }
        return ret;
    }

    @Override public boolean stopServer()
    {
        boolean ret;
        ret= super.stopServer();
        if(ret)
        {
            mContext.createServerButton.setText("创建服务");
            mContext.serverMessage("服务器停止运行");
        }
        else
        {
            mContext.serverMessage("无法停止服务器");
        }
        return ret;
    }

    @Override public boolean socket_def(SocketChannel s, String c)
    {
        final String x=c;
        Boolean running=true;
        mContext.serverMessage("receive :"+x);    // 刷新

        return true;
    }

    @Override public boolean sentMessage(String data)
    {
        mContext.serverMessage("sent :"+ data);
        return super.sentMessage(data);
    }

    @Override protected void errMessage(String s)
    {
        mContext.serverMessage(s);
    }
}
