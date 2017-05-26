package com.c.aka009.listener;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{

    public MyService myService;
    private ListUnitsAdapter _listUnitsAdapter;

    private boolean _isLoop_B_On;
    private boolean _isPause_B_On;
    private boolean _isRandom_B_On;

    private int _currentMusicIndex = 0;

    private List<ListUnits> _MLOLU; // Main list of ListUnits

    private ImageButton P_B_list ;
    private ImageButton P_B_pre ;
    private ImageButton P_B_start ;
    private ImageButton P_B_next ;
    private ImageButton P_B_repeat ;

    private ListView P_LV_1;

    private TextView P_TV_currentMusicName;
    private TextView P_TV_currentMusicPlayerName;

    private Uri _mediaUriExternal = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
    private Uri _mediaUriInternal = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;

    private RemoteViews _remoteContentView;
    private NotificationManager _notificationManager;
    private Notification myNotification;
    private BroadcastReceiver _notificationBarClickReceiver;

    public static final String ACTION_N2S_START = "ACTION_N2S_START";
    public static final String ACTION_N2S_NEXT = "ACTION_N2S_NEXT";
    public static final String ACTION_N2P_WAKEUP = "ACTION_N2P_WAKEUP";
    public static final String ACTION_N2P_CLOSE = "ACTION_N2P_CLOSE";


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        P_B_list    = (ImageButton) findViewById(R.id.P_B_list);
        P_B_pre     = (ImageButton) findViewById(R.id.P_B_pre);
        P_B_start   = (ImageButton) findViewById(R.id.P_B_start);
        P_B_next    = (ImageButton) findViewById(R.id.P_B_next);
        P_B_repeat  = (ImageButton) findViewById(R.id.P_B_repeat);

        P_TV_currentMusicName       = (TextView) findViewById(R.id.P_TV_currentMusicName);
        P_TV_currentMusicPlayerName = (TextView) findViewById(R.id.P_TV_currentMusicPlayerName);

        P_LV_1 = (ListView) findViewById(R.id.P_LV_1);

        P_B_list.   setOnClickListener(this);
        P_B_pre.    setOnClickListener(this);
        P_B_start.  setOnClickListener(this);
        P_B_next.   setOnClickListener(this);
        P_B_repeat. setOnClickListener(this);

        _isLoop_B_On    = false;
        _isPause_B_On   = false;
        _isRandom_B_On  = false;

        _bindServiceConnection();
        _startNewThreadToGetData();
        _initializeNotificationBar();

        P_LV_1.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                on_P_LVIC_Jump_To(position);
            }
        });

        _notificationBarClickReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (intent.getAction().equals(ACTION_N2S_START))
                {
                    on_P_B_Start_Click();
                }
                else if (intent.getAction().equals(ACTION_N2S_NEXT))
                {
                    on_P_B_Next_Click();
                }
                else if (intent.getAction().equals(ACTION_N2P_WAKEUP))
                {
                    Intent start = new Intent(getApplicationContext(),MainActivity.class);
                    start.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(start);
                    ST_CollapseNotification(getApplicationContext());
                }
                else if (intent.getAction().equals(ACTION_N2P_CLOSE))
                {
                    on_P_B_Close_Click();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_N2S_START);
        filter.addAction(ACTION_N2S_NEXT);
        filter.addAction(ACTION_N2P_WAKEUP);
        filter.addAction(ACTION_N2P_CLOSE);

        registerReceiver(_notificationBarClickReceiver, filter);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        _initializeNotificationBar();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(_notificationBarClickReceiver);  //解除广播接收器
        _notificationManager.cancelAll();                   //清除所有通知
    }

    @Override
    public void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.P_B_list:
                on_P_B_List_Click();
                break;
            case R.id.P_B_pre:
                on_P_B_Pre_Click();
                break;
            case R.id.P_B_start:
                on_P_B_Start_Click();
                break;
            case R.id.P_B_next:
                on_P_B_Next_Click();
                break;
            case R.id.P_B_repeat:
                on_P_B_Repeat_Click();
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == event.KEYCODE_BACK)
        {
            moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void _bindServiceConnection()
    {
        Intent intent = new Intent(MainActivity.this,MyService.class);
        startService(intent);
        bindService(intent,_serviceConnection,this.BIND_AUTO_CREATE);
    }

    private ServiceConnection _serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            myService = ((MyService.MyBinder)(service)).getService();

            Log.w("onServiceConnected","<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
            if (myService == null)
            {
                Log.w("+++++++++++++++","NULL!!!!!");
            }

            //注册自定义接口来接收需要同步的指令
            myService.setMyOnNeedSyncListener(new myIOnSyncListener()
            {
                @Override
                public void NeedSync()
                {
                    _syncUI();
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            Log.w("onServiceDisconnected",">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            myService = null;
        }
    };

//    public Handler handler = new Handler();
//    public Runnable runnable = new Runnable()
//    {
//        @Override
//        public void run()
//        {
//            _syncUI();
//            handler.post(runnable);
//        }
//    };



    private void on_P_B_Start_Click()
    {
        if ( _isPause_B_On == false)                            //按钮显示为播放时（按下就能播放）
        {
            myService.s_start();                                //调用播放服务
            myService.s_setLoop(_isLoop_B_On);                  //设置单曲循环标记
            P_B_start.setImageResource(R.drawable.p_pause);     //切换为暂停键
            _isPause_B_On = true;                               //更新按钮状态
        }
        else if (_isPause_B_On == true)                         //按钮显示为暂停时（按下就能暂停）
        {
            myService.s_pause();                                //调用暂停服务

            P_B_start.setImageResource(R.drawable.p_start);     //切换为播放键
            _isPause_B_On = false;                              //更新按钮状态
        }
        _syncUI();                                              //更新UI
    }

    private void on_P_B_Repeat_Click()
    {
        if (_isLoop_B_On == false)  //如果单曲循环关闭
        {
            P_B_repeat.setImageResource(R.drawable.p_onrepeat);     //切换为单曲循环状态
            _isLoop_B_On = true;
            myService.s_setLoop(_isLoop_B_On);
        }
        else //如果单曲循环开启
        {
            P_B_repeat.setImageResource(R.drawable.p_offrepeat);     //切换为关闭状态
            _isLoop_B_On = false;
            myService.s_setLoop(_isLoop_B_On);
        }
    }

    private void on_P_B_Next_Click()
    {
        myService.s_next();
        _syncUI();
    }

    private void on_P_B_Pre_Click()
    {
        myService.s_pre();
        _syncUI();
    }

    private void on_P_B_List_Click()
    {
        if (_isRandom_B_On)     //如果已经处于随机状态
        {
            _isRandom_B_On = false;
            P_B_list.setImageResource(R.drawable.p_list);//设置为顺序状态
            myService.s_setRandom(_isRandom_B_On);
        }
        else                    //如果处于顺序状态
        {
            _isRandom_B_On = true;
            P_B_list.setImageResource(R.drawable.p_random);//设置为随机状态
            myService.s_setRandom(_isRandom_B_On);
        }
    }

    private void on_P_B_Close_Click()
    {
        myService.s_stop();
        myService.s_quit();

        //handler.removeCallbacks(runnable);
        unbindService(_serviceConnection);
        Intent intent = new Intent(MainActivity.this,MyService.class);
        stopService(intent);
        try
        {
            _notificationManager.cancelAll();                   //清除所有通知
            MainActivity.this.finish();
            System.exit(0);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private void on_P_LVIC_Jump_To(int index)
    {
        myService.s_jumpTo(index);
        _syncUI();
    }

    private void _startNewThreadToGetData()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }

                List<ListUnits> _TLOLU = new ArrayList<>();

                _TLOLU = _getUriData(_mediaUriInternal, _TLOLU);
                _TLOLU = _getUriData(_mediaUriExternal, _TLOLU);

                final List<ListUnits> final_TLOLU = _TLOLU;
                final String s = "Ready";
                //耗时操作，完成之后提交任务更新UI
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        _MLOLU = final_TLOLU;
                        Log.d("=========final_TLOLU : ",final_TLOLU.toString());
                        if (myService == null)
                        {
                            Log.w("+++++++++++++++","NULL!!!!!");
                        }
                        Log.d("==========myService : ",myService.toString());
                        myService.s_setList(final_TLOLU);
                        _listUnitsAdapter = new ListUnitsAdapter(MainActivity.this, R.layout.list_items_layout, _MLOLU);
                        P_LV_1.setAdapter(_listUnitsAdapter);

                        myService.s_initialize();
                        _syncUI();
                    }
                });
            }
        }).start();
    }

    private List<ListUnits> _getUriData (Uri uri ,List<ListUnits> _TLOLU)
    {
        String[] projection = {"_data", "_display_name", "duration", "artist"};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst())
        {
            cursor.moveToFirst();
            do
            {
                ListUnits tempListUnit = new ListUnits();

                tempListUnit.SetData(cursor.getString(cursor.getColumnIndex("_data")));
                tempListUnit.SetDisplay_name(cursor.getString(cursor.getColumnIndex("_display_name")));
                tempListUnit.SetArtist(cursor.getString(cursor.getColumnIndex("artist")));
                tempListUnit.SetDuration(cursor.getString(cursor.getColumnIndex("duration")));
                tempListUnit.SetTitleHighLight(false,this);

                _TLOLU.add(tempListUnit);
            }
            while (cursor.moveToNext());
            cursor.close();
        }
        return _TLOLU;
    }

    private void _syncUI()
    {
        __syncList(_currentMusicIndex , true);

        //更新主UI
        _currentMusicIndex = myService.s_getCurrentMusicIndex();
        P_TV_currentMusicName.setText(_MLOLU.get(_currentMusicIndex).GetName());
        P_TV_currentMusicPlayerName.setText(_MLOLU.get(_currentMusicIndex).GetPlayerName());

        __syncNotificationBar();

        //ConstraintLayout tempConstraintLayout = (ConstraintLayout) P_LV_1.getAdapter().getView(_currentMusicIndex, null, null);
        //TextView tempInItem_L_TV_Name = (TextView) tempConstraintLayout.getChildAt(0);
        //tempInItem_L_TV_Name.setText("Fuck!!!!!!!!!!!!!!!!!");

        __syncList(_currentMusicIndex,false);

    }

    /**
     * 二级内部函数，控制歌曲列表对应项的文字高亮同步
     * @param index 需要修改的项的索引
     * @param isOld 需要修改的项是不是需要去掉高亮
     */
    private void __syncList(int index , boolean isOld)
    {
        ListUnits tempListUnit = (ListUnits) P_LV_1.getItemAtPosition(index);

        if (isOld)
        {
            tempListUnit.SetTitleHighLight(false , this);
        }
        else
        {
            tempListUnit.SetTitleHighLight(true , this);
        }

        _listUnitsAdapter.notifyDataSetChanged();
        P_LV_1.smoothScrollToPosition(index);
    }

    /**
     * 二级内部函数，控制通知栏控制面板的UI同步
     */
    private void __syncNotificationBar()
    {
        if (_remoteContentView != null && myNotification!= null)
        {
            //更新两个文本
            _remoteContentView.setTextViewText(R.id.N_TV_name ,_MLOLU.get(_currentMusicIndex).GetName());
            _remoteContentView.setTextViewText(R.id.N_TV_playerName ,_MLOLU.get(_currentMusicIndex).GetPlayerName());

            //更新通知栏的播放按钮
            if ( _isPause_B_On == false)        //按钮应显示为播放时
            {
                _remoteContentView.setImageViewResource(R.id.N_B_start ,R.drawable.t_start); //将通知栏按钮图标切换为播放
            }
            else if (_isPause_B_On == true)     //按钮应显示为暂停时
            {
                _remoteContentView.setImageViewResource(R.id.N_B_start ,R.drawable.t_pause); //将通知栏按钮图标切换为暂停
            }

            //重新发送通知
            _notificationManager.notify(0, myNotification);
        }
    }

    private void _initializeNotificationBar()
    {
        _remoteContentView = new RemoteViews(getPackageName(), R.layout.nc_layout);

        _remoteContentView.setImageViewResource(R.id.N_B_close ,R.drawable.t_close);
        _remoteContentView.setImageViewResource(R.id.N_B_main ,R.drawable.main);
        _remoteContentView.setImageViewResource(R.id.N_B_next ,R.drawable.t_next);
        _remoteContentView.setImageViewResource(R.id.N_IV_BG ,R.drawable.bg_n_1);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setSmallIcon(R.mipmap.ic_listener);
        builder.setCustomContentView(_remoteContentView);
        builder.setCustomBigContentView(_remoteContentView);
        builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        builder.setPriority(Notification.PRIORITY_MAX);

        PendingIntent pendingIntent_default= PendingIntent.getActivity(this, 1, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent_default);
        myNotification = builder.build();
//        myNotification.bigContentView = _remoteContentView;
//        myNotification.contentView = _remoteContentView;

//        notification = new Notification();
//        //初始化通知
//        notification.icon = R.mipmap.ic_listener;

//        notification.contentView = contentView;
//
//        Intent intentPlay = new Intent("ACTION_N2S_START");//新建意图，并设置action标记为"play"，用于接收广播时过滤意图信息
//        PendingIntent pIntentPlay = PendingIntent.getBroadcast(this, 0, intentPlay,0);//PendingIntent.FLAG_UPDATE_CURRENT
//        _remoteContentView.setOnClickPendingIntent(R.id.N_B_start, pIntentPlay);//为播放按钮注册事件
//
//        Intent intentPause = new Intent("ACTION_N2S_PAUSE");
//        PendingIntent pIntentPause = PendingIntent.getBroadcast(this, 0, intentPause, 0);
//        _remoteContentView.setOnClickPendingIntent(R.id.bt_notic_pause, pIntentPause);
//
//        Intent intentNext = new Intent("ACTION_N2S_NEXT");
//        PendingIntent pIntentNext = PendingIntent.getBroadcast(this, 0, intentNext, 0);
//        _remoteContentView.setOnClickPendingIntent(R.id.bt_notic_next, pIntentNext);
//
//        Intent intentWakeUp = new Intent("ACTION_N2P_WAKEUP");
//        PendingIntent pIntentLast = PendingIntent.getBroadcast(this, 0, intentWakeUp, 0);
//        _remoteContentView.setOnClickPendingIntent(R.id.bt_notic_last, pIntentLast);
//
//        Intent intentCancel = new Intent("ACTION_N2P_CLOSE");
//        PendingIntent pIntentCancel = PendingIntent.getBroadcast(this, 0, intentCancel, 0);
//        _remoteContentView.setOnClickPendingIntent(R.id.bt_notic_cancel, pIntentCancel);

        __setPendingIntent(R.id.N_B_start,ACTION_N2S_START);
        __setPendingIntent(R.id.N_B_next, ACTION_N2S_NEXT);
        __setPendingIntent(R.id.N_B_main, ACTION_N2P_WAKEUP);
        __setPendingIntent(R.id.N_B_close, ACTION_N2P_CLOSE);

        myNotification.flags = myNotification.FLAG_NO_CLEAR;                                        //设置通知点击或滑动时不被清除
        _notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        _notificationManager.notify(0, myNotification);                                             //开启通知
    }

    /**
     * 二级内部函数，控制通知栏中不同按钮的广播发送
     * @param id 按钮的ID
     * @param actionName 广播的动作名称
     */
    private void __setPendingIntent(int id, String actionName)
    {
        Intent tempIntent = new Intent(actionName);                                             //新建意图，并设置action动作名称为传入的名称，用于接收广播时过滤意图信息
        PendingIntent tempPendingIntent = PendingIntent.getBroadcast(this, 10000, tempIntent, PendingIntent.FLAG_UPDATE_CURRENT);   //PendingIntent.FLAG_UPDATE_CURRENT //创建临时变量，用来发送广播
        _remoteContentView.setOnClickPendingIntent(id, tempPendingIntent);                      //为指定的按钮绑定意图
    }

    /**
     * 静态函数，用于折叠通知栏
     * @param context 当前上下文
     */
    public static void ST_CollapseNotification(Context context)
    {

//        查看了Android源码后发现在android.app包下个被隐藏的类：android.app.StatusBarManager，
//        该类提供了折叠和展开通知栏的相应方法。
//        使用该类前需要获得com.android.serevier.StatusBarManager的一个系统服务对象，
//        使用context.getSystemService(“statusbar”);就可以获取了。这个服务也是被隐藏的。
//        获取服务的Key值常量也是被隐藏的。
//        详见android.content.Context.STATUS_BAR_SERVICE常量定义。
//
//        因展开和折叠通知栏的类和接口都被隐藏了，所以只能通过反射的方式才能调用。

        //使用@SuppressWarnings("WrongConstant")特性来忽略“语法错误”，强行获取"statusbar"
        @SuppressWarnings("WrongConstant")
        Object service = context.getSystemService("statusbar");
        if (null == service)
        {
            return;
        }
        try
        {
            Class<?> clazz = Class.forName("android.app.StatusBarManager");
            int sdkVersion = android.os.Build.VERSION.SDK_INT;
            Method collapse = null;

            if (sdkVersion <= 16)
            {
                collapse = clazz.getMethod("collapse");
            }
            else
            {
                collapse = clazz.getMethod("collapsePanels");
            }

            collapse.setAccessible(true);
            collapse.invoke(service);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

}

