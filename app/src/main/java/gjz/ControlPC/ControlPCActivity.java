package gjz.ControlPC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ControlPCActivity extends Activity
{
    /** Called when the activity is first created. */

	private Button connectServerButton;
	private EditText IPText;
	private Button sendButtonClient;	
	private Button sendButtonServer;	
	public Button createServerButton;
	private EditText serverMessageText, clientMessageText;
	public TextView recvText;
	private Button StartMouseButton;
	
	private Context mContext;
	private boolean isConnecting = false;
	
	private Thread mThreadClient = null;
	private Socket mSocketServer = null;
	private Socket mSocketClient = null;
	static BufferedReader mBufferedReaderServer	= null;
	static PrintWriter mPrintWriterServer = null;
	static BufferedReader mBufferedReaderClient	= null;
	static PrintWriter mPrintWriterClient = null;
	private  String recvMessageClient = "";
	private  String recvMessageServer = "";

	private mySocketServer mMySocketServer = null;
	private mySocketClient mMySocketClient = null;

	private static final String MSG_KEY = "wifimsg";

	@Override
    public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mContext = this;

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()        
        .detectDiskReads()        
        .detectDiskWrites()        
        .detectNetwork()   // or .detectAll() for all detectable problems       
        .penaltyLog()        
        .build());        
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()        
        .detectLeakedSqlLiteObjects()     
        .penaltyLog()        
        .penaltyDeath()        
        .build()); 

        IPText= (EditText)findViewById(R.id.IPText);
        //IPText.setText("10.0.2.15:");
        IPText.setText("192.168.1.2:59671");

        clientMessageText = (EditText)findViewById(R.id.clientMessageText);
        clientMessageText.setText("up");
		clientMessageText.setEnabled(false);
        
        // client : connect to a server
		connectServerButton = (Button)findViewById(R.id.StartConnect);
		connectServerButton.setOnClickListener(StartClickListener);

		// client : send message to a server
		sendButtonClient= (Button)findViewById(R.id.SendButtonClient);
		sendButtonClient.setEnabled(false);
		sendButtonClient.setOnClickListener(SendClickListenerClient);

        // server : start a server
		createServerButton = (Button)findViewById(R.id.CreateConnect);
		createServerButton.setOnClickListener(CreateServerClickListener);

		serverMessageText = (EditText)findViewById(R.id.MessageText);
		serverMessageText.setText("up");
		serverMessageText.setEnabled(false);

		// server : send message to a client
        sendButtonServer= (Button)findViewById(R.id.SendButtonServer);
		sendButtonServer.setEnabled(false);
		sendButtonServer.setOnClickListener(ServerSendMessageClickListener);

        recvText= (TextView)findViewById(R.id.RecvText);
        recvText.setMovementMethod(ScrollingMovementMethod.getInstance()); 
        
        StartMouseButton = (Button)findViewById(R.id.StartMouse);
        StartMouseButton.setOnClickListener(StartMouseClickListenerServer);

		recvText.setText("信息:\n");

		mMySocketServer = new mySocketServer(this,2412);
    }

    // client : connect to a server
    private OnClickListener StartClickListener = new OnClickListener()
	{
		@Override public void onClick(View arg0)
		{
			// disconnect from the server
			if (mMySocketClient !=null && mMySocketClient.isConnected())
			{				
				mMySocketClient.disconnectServer();
				//mSocketClient = null;
				connectServerButton.setText("开始连接");
				IPText.setEnabled(true);
				clientMessageText.setEnabled(false);
				sendButtonClient.setEnabled(false);
			}
			else //connect to a server
			{
				String msgText =IPText.getText().toString();
				if(msgText.length()<=0)
				{
					//Toast.makeText(mContext, "IP不能为空！", Toast.LENGTH_SHORT).show();
					recvMessageClient = "IP不能为空！\n";//消息换行
					clientMessage(recvMessageClient);
					return;
				}
				int start = msgText.indexOf(":");
				if( (start == -1) ||(start+1 >= msgText.length()) )
				{
					recvMessageClient = "IP地址不合法\n";//消息换行
					clientMessage(recvMessageClient);
					return;
				}
				String sIP = msgText.substring(0, start);
				String sPort = msgText.substring(start+1);
				int port = Integer.parseInt(sPort);

				//if(mMySocketClient == null)
				mMySocketClient = new mySocketClient((ControlPCActivity) mContext,sIP,port);
				if(mMySocketClient.connectSever())
				{
					connectServerButton.setText("停止连接");
					IPText.setEnabled(false);
					clientMessageText.setEnabled(true);
					sendButtonClient.setEnabled(true);
				}else
				{

				}
			}
		}
	};

    // client : send message
	private OnClickListener SendClickListenerClient = new OnClickListener()
	{
		@Override public void onClick(View arg0)
		{
			// TODO Auto-generated method stub				
			if ( mMySocketClient !=null)
			{
				String msgText = clientMessageText.getText().toString();//取得编辑框中我们输入的内容
				if(msgText.length()<=0)
				{
					Toast.makeText(mContext, "发送内容不能为空！", Toast.LENGTH_SHORT).show();
				}
				else
				{
					mMySocketClient.sentMessage(msgText);
					clientMessage(String.format("sent (to %s:%d) : ",mMySocketClient.mIP,mMySocketClient.mPort)+msgText);
				}
			}
			else
			{
				Toast.makeText(mContext, "没有连接", Toast.LENGTH_SHORT).show();
			}
		}
	};

	// server : create a server object
	private OnClickListener CreateServerClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View arg0)
		{
			if(serverRuning)
			{
				serverRuning=false;
				mMySocketServer.stopServer();
				serverMessageText.setEnabled(false);
				sendButtonServer.setEnabled(false);
			}else
			{
				serverRuning=true;
				mMySocketServer.startServer();
				getLocalIpAddress();
				serverMessageText.setEnabled(true);
				sendButtonServer.setEnabled(true);
			}
		}
	};


	//创建服务端ServerSocket对象
     private boolean serverRuning = false;

	// server : send a message to a client
	private OnClickListener ServerSendMessageClickListener = new OnClickListener()
	{
		@Override
		public void onClick(View arg0)
		{
			String msgText = serverMessageText.getText().toString();//取得编辑框中我们输入的内容
			mMySocketServer.sentMessage(msgText);
		}
	};


	public void serverMessage(String ms)
	{
		Message msg = mHandler.obtainMessage();
		Bundle bundle = new Bundle();
		bundle.putString(MSG_KEY, "Server: "+ms+"\n");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	public void clientMessage(String ms)
	{
		Message msg = mHandler.obtainMessage();
		Bundle bundle = new Bundle();
		bundle.putString(MSG_KEY, "Client: "+ms+"\n");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}
	public String getLocalIpAddress()
	{
		String temp="";
		WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int ipAddress = wifiInfo.getIpAddress();
		if(ipAddress==0)return "未连接wifi";
		temp = "请连接IP："+ ((ipAddress & 0xff)+"."+(ipAddress>>8 & 0xff)+"."+(ipAddress>>16 & 0xff)+"."+(ipAddress>>24 & 0xff))+String.format(":%d",mMySocketServer.mPort);
		serverMessage(temp);
		return temp;
	}

	private String getInfoBuff(char[] buff, int count)
	{
		char[] temp = new char[count];
		for(int i=0; i<count; i++)
		{
			temp[i] = buff[i];
		}
		return new String(temp);
	}
		
	private OnClickListener StartMouseClickListenerServer = new OnClickListener()
	{
		@Override
		public void onClick(View arg0)
		{
			// TODO Auto-generated method stub
			if ( (serverRuning && mSocketServer!=null) || (isConnecting && mSocketClient!=null) )
			{
				Intent intent = new Intent();
				intent.setClass(ControlPCActivity.this, mouseActivity.class);
				startActivity(intent);
			}
			else
			{
				Toast.makeText(mContext, "没有连接", Toast.LENGTH_SHORT).show();
			}
		}
	};

	public void onDestroy()
	{
		super.onDestroy();
		if (isConnecting)
		{
			isConnecting = false;
			try {
				if(mSocketClient!=null)
				{
					mSocketClient.close();
					mSocketClient = null;

					mPrintWriterClient.close();
					mPrintWriterClient = null;
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mThreadClient.interrupt();
		}
		if (serverRuning)
		{
			serverRuning = false;
			if(mMySocketServer!=null)
			{
				mMySocketServer.stopServer();
				mMySocketServer = null;
			}
		}
	}

	Handler mHandler = new Handler()
	{
		public void handleMessage(Message msg)
		{
			Bundle bundle = msg.getData();
			String string = bundle.getString(MSG_KEY);
			recvText.append(string);
		}
	};

}