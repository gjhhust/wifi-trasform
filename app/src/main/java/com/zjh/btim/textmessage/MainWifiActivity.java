package com.zjh.btim.textmessage;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.zjh.btim.R;
import com.zjh.btim.Util.FileUtils;
import com.zjh.btim.Util.XPermissionUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class MainWifiActivity extends AppCompatActivity {
    private ListView message_list;
    private Button send;
    private EditText text_message;
    private MessageAdapter ma;
    private InetAddress target;
    public static int port = 2333;
    private DatagramSocket udp_socket;
    public ReceiveMessageThread rmt;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            Bundle b = message.getData();
            ma.add(new MessageItem(R.drawable.pc, b.getString("data")));
            message_list.setSelection(message_list.getBottom());
            b.clear();
            b = null;
            System.gc();
            return false;
        }
    });
    private Button fileBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_wifi);
//        获取传来的本机和电脑ip，并给标题栏设置一个，醒目一些
        Setting.main = this;
        Intent i = getIntent();
        String localip = i.getStringExtra("local_ip");
        getSupportActionBar().setTitle("本机:" + localip);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setLogo(R.drawable.ip);
//        pc_ip might be null value
        String pcip = i.getStringExtra("pc_ip");
//        初始化接收信息的服务
        try {
            udp_socket = new DatagramSocket(port);
            udp_socket.setReceiveBufferSize(655360000);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        initialAndStartReceiveMessageService();
//        初始化组件
        message_list = findViewById(R.id.message_list);
        ma = new MessageAdapter(this, R.layout.item_list, new ArrayList<MessageItem>());
        message_list.setAdapter(ma);
//        只有获得了电脑IP才可以发送。正则表达式简单判断IP是否合法
        if (Pattern.matches("\\d+\\.\\d+\\.\\d+\\.\\d+", pcip)) {
//            设置发送的目标地址
            try {
                target = InetAddress.getByName(pcip);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
//            获取发送按钮和文本框
            send = findViewById(R.id.send);
            text_message = findViewById(R.id.text);
//            清空文本框，获取文本信息，发送
            send.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String message_to_send = text_message.getText().toString();
//                    避免空信息刷屏
                    if (message_to_send.equals("")) {
                        return;
                    }
                    text_message.getText().clear();
                    ma.add(new MessageItem(R.drawable.phone, message_to_send));
//                    每次发送消息后自动聚焦到最底部
                    message_list.setSelection(message_list.getBottom());
//                    发送文本信息(明文)
                    new SendMessageThread(message_to_send).start();
                }
            });
        } else {
//            否则不显示底下的发送栏
            findViewById(R.id.bottom_frame).setVisibility(View.INVISIBLE);
        }
//        点击其中某项后复制文本内容
        message_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                MessageItem mi = ma.getItem(i);
                copyMessage(mi.getContent());
            }
        });
        fileBtn = findViewById(R.id.file);
        fileBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showFileChooser();
            }
        });


    }

    private static final int FILE_SELECT_CODE = 0;
    private void showFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), FILE_SELECT_CODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }


    //    初始化接收信息活动
    private void initialAndStartReceiveMessageService() {
        rmt = new ReceiveMessageThread();
        rmt.start();
    }

    //    标题栏右边的设置图标
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater mi = getMenuInflater();
        mi.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //    标题栏右边设置图标的活动。进入端口的设置
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.set_port) {
            startActivity(new Intent(this, Setting.class));
            return true;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        停掉服务，会报的错不用管，不影响正常使用
        udp_socket.close();
    }

    class SendMessageThread extends Thread {
        private String text;

        public SendMessageThread(String t) {
            text = t;
        }

        @Override
        public void run() {
            super.run();
            try {
                byte[] b = text.getBytes();
                DatagramPacket data = new DatagramPacket(b, b.length, target, port);
                udp_socket.send(data);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
    }

    class SendFileMessageThread extends Thread {
        private String filePath;

        public SendFileMessageThread(String t) {
            filePath = t;
        }

        @Override
        public void run() {
            super.run();
            try {

                File file = new File(filePath);

                InputStream input = new FileInputStream(file);
                byte[] b = new byte[input.available()];
                DatagramPacket data = new DatagramPacket(b, b.length, target, port);
                udp_socket.send(data);


//                FileInputStream in = new FileInputStream(filePath);
//                byte[] buf = new byte[1024];
//                int i=0;
//                int len;
//                while((len=in.read(buf))!=-1){
//                    //3,创建数据包对象，因为udp协议是需要将数据封装到指定的数据包中。
//                    DatagramPacket dp = new DatagramPacket(buf,len,target,port);
//                    //4,使用udpsocket服务的send方法。将数据包发出。
//                    udp_socket.send(dp);
//                }
                //5,关闭资源。
                udp_socket.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
    }




    private static final String FILE_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/BTIMBluetooth/";
    private static final int FLAG_MSG = 0;  //消息标记
    private static final int FLAG_FILE = 1; //文件标记
    //接收
    class ReceiveMessageThread extends Thread {
        @Override
        public void run() {
            super.run();
            DatagramPacket pack = null;
            byte b[] = new byte[819002];
            pack = new DatagramPacket(b, b.length);

            while (true) {
                try {
                    if (udp_socket.isClosed()) {
                        break;
                    }
                    udp_socket.receive(pack);

//                    File file = new File("/temp/abc.txt");
//                    FileInputStream fis = new FileInputStream(b);
//                    fis.read(bytesArray); //read file into bytes[]
//                    fis.close();

                    File destDir = new File(FILE_PATH);
                    if (!destDir.exists())
                        destDir.mkdirs();
                    // 读取文件内容
                    long len = 0;
                    int r;


                    String timeName = String.valueOf(System.currentTimeMillis());

                    FileOutputStream out = new FileOutputStream(FILE_PATH + timeName);
                    out.write(b, 0, b.length);



                    Bundle bundle = new Bundle();
                    bundle.putString("data", timeName);
                    Message m = new Message();
                    m.setData(bundle);
                    handler.sendMessage(m);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }
    }

    private void sendMessageToUi(int what, Object s) {
        Message message = handler.obtainMessage();
        message.what = what;
        message.obj = s;
        handler.sendMessage(message);
    }

    private void copyMessage(String s) {
        ClipboardManager clipboard = (ClipboardManager) getApplicationContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(null, s);
        clipboard.setPrimaryClip(clipData);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case FILE_SELECT_CODE:
                if (resultCode == RESULT_OK) {
                    Uri uri = data.getData();
                    final String path = FileUtils.getInstance(this).getChooseFileResultPath(uri);
                    Log.i("-选择文件", "File Path: " + path);
                    if (path != null) {
                        AlertDialog.Builder ad = new AlertDialog.Builder(MainWifiActivity.this);
                        ad.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {




                                XPermissionUtil.requestPermissions(MainWifiActivity.this, 1,
                                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                Manifest.permission.READ_EXTERNAL_STORAGE},
                                        new XPermissionUtil.OnPermissionListener() {
                                            @Override
                                            public void onPermissionGranted() {
                                                //权限获取成功
                                                new SendFileMessageThread(path).start();
                                            }

                                            @Override
                                            public void onPermissionDenied() {
                                                //权限获取失败
                                                Snackbar.make(fileBtn, "请手动到设置界面给予相关权限", Snackbar.LENGTH_LONG).show();
                                            }
                                        });


//                                sendFile( path);
                            }
                        });
                        ad.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        ad.setMessage("你确定要发送" + path + "吗?");
                        ad.setTitle("提示");
                        ad.setCancelable(false);
                        ad.show();
                    }
                }
                break;
        }
    }
}
