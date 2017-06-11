package com.c.aka009.Ting;

import android.app.ActivityManager;
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
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.NotificationCompat;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener
{
    //region 定义播放控制变量
    //各个按钮的开关状态（是否开启）
    private boolean _isLoop_B_On;
    private boolean _isPause_B_On;
    private boolean _isRandom_B_On;
    private int _currentMusicIndex = 0;     //当前歌曲的索引值
    private List<ListUnits> _MLOLU;         // Main list of ListUnits当前播放列表
    //endregion

    //region 声明各个UI控件对象
    private ImageButton P_B_list ;
    private ImageButton P_B_pre ;
    private ImageButton P_B_start ;
    private ImageButton P_B_next ;
    private ImageButton P_B_repeat ;

    private ListView P_LV_1;

    private TextView P_TV_currentMusicName;
    private TextView P_TV_currentMusicPlayerName;

    private View P_V_splash;
    //endregion

    //region 声明其他对象
    public MyService myService;
    private ListUnitsAdapter _listUnitsAdapter;
    private RemoteViews _remoteContentView;
    private NotificationManager _notificationManager;
    private Notification myNotification;
    private BroadcastReceiver _notificationBarClickReceiver;
    //endregion

    //region 定义静态常量值
    private static final Uri MEDIA_URI_EXTERNAL = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;  //外接SD卡的URI
    private static final Uri MEDIA_URI_INTERNAL = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;  //内部存储的URI

    //定义广播的动作名称
    public static final String ACTION_N2S_START = "ACTION_N2S_START";
    public static final String ACTION_N2S_NEXT = "ACTION_N2S_NEXT";
    public static final String ACTION_N2P_WAKEUP = "ACTION_N2P_WAKEUP";
    public static final String ACTION_N2P_CLOSE = "ACTION_N2P_CLOSE";
    //endregion


    //region 生命周期函数
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //region 持有各个UI控件
        P_B_list    = (ImageButton) findViewById(R.id.P_B_list);
        P_B_pre     = (ImageButton) findViewById(R.id.P_B_pre);
        P_B_start   = (ImageButton) findViewById(R.id.P_B_start);
        P_B_next    = (ImageButton) findViewById(R.id.P_B_next);
        P_B_repeat  = (ImageButton) findViewById(R.id.P_B_repeat);

        P_TV_currentMusicName       = (TextView) findViewById(R.id.P_TV_currentMusicName);
        P_TV_currentMusicPlayerName = (TextView) findViewById(R.id.P_TV_currentMusicPlayerName);

        P_LV_1 = (ListView) findViewById(R.id.P_LV_1);

        P_V_splash = findViewById(R.id.P_CL_splash);
        //endregion

        //region 为主界面的按钮设置监听器
        P_B_list.   setOnClickListener(this);
        P_B_pre.    setOnClickListener(this);
        P_B_start.  setOnClickListener(this);
        P_B_next.   setOnClickListener(this);
        P_B_repeat. setOnClickListener(this);
        //endregion

        //region 初始化按钮状态
        _isLoop_B_On    = false;
        _isPause_B_On   = false;
        _isRandom_B_On  = false;
        //endregion

        _showSplash(3000);                  //显示3秒钟的splash画面
        _bindServiceConnection();           //开启并绑定服务
        _startNewThreadToGetData();         //开启子线程进行媒体探查并将结果输入列表
        _initializeNotificationBar();       //初始化通知栏并调出通知栏里的控制面板

        //region 为UI列表的项添加点击事件
        P_LV_1.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                on_P_LVIC_Jump_To(position);    //调用跳转方法，跳转到按下的项的索引位置
            }
        });
        //endregion

        //region 定义广播接收器，处理通知栏的按钮事件，以及耳机拔下事件
        _notificationBarClickReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                //region 处理通知栏的按钮事件
                if (intent.getAction().equals(ACTION_N2S_START))
                {
                    on_P_B_Start_Click();
                }
                else if (intent.getAction().equals(ACTION_N2S_NEXT))
                {
                    on_P_B_Next_Click();
                }
                else if (intent.getAction().equals(ACTION_N2P_WAKEUP))  //按下图标按钮时，唤醒主界面并收起通知面板
                {

                    //region 【引用】将activity切换到前台
                    /*
                    Bringing the activity to foreground 将activity切换到前台

                    今天遇到这个问题，找了很久，网上一些解决方法不够完全。特做此记录：
                    经测试以下方法不能将在后台运行的activity切换到前台运行！

                    Intent i = new Intent();
                    i.setClass(this, MainActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);

                    必须要将i替换为getApplicationContext()才可以将MainActivity运行，
                    但是并不是将后台运行的MainActivity切换到前台，
                    而是新建了一个新的任务运行MainActivity。

                    所以必须用以下代码：

                    Intent start = new Intent(getApplicationContext(),MainActivity.class);
                    start.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(start);

                    并且在相应的MainActivity需要做如下红色字体部分配置

                    <activity
                    android:name=".MainActivity"
                    android:label="@string/app_name"
                    android:launchMode="singleTop">

                    才能将后台运行的MainActivity重新排序到前台运行。
                    */
                    //endregion

//                    Intent start = new Intent(context,MainActivity.class);
//                    start.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK );//FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
//                    context.startActivity(start);


                    //获取ActivityManager
                    ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
                    //获得当前运行的task
                    List<ActivityManager.RunningTaskInfo> taskList = activityManager.getRunningTasks(100);
                    boolean tempIsOn = false;
                    for (ActivityManager.RunningTaskInfo rti : taskList)
                    {
                        //找到当前应用的task，并启动task的栈顶activity，达到程序切换到前台
                        if (rti.topActivity.getPackageName().equals(context.getPackageName()))
                        {
                            activityManager.moveTaskToFront(rti.id, 0);
                            tempIsOn = true;
                        }
                    }

                    if (!tempIsOn)
                    {
                        //若没有找到运行的task，用户结束了task或被系统释放，则重新启动mainactivity
                        Intent resultIntent = new Intent(context, MainActivity.class);
                        resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        context.startActivity(resultIntent);
                    }


                    ST_CollapseNotification(getApplicationContext());   //收起通知面板
                }
                else if (intent.getAction().equals(ACTION_N2P_CLOSE))
                {
                    on_P_B_Close_Click();
                }
                //endregion

                //region 处理耳机拔下事件
                //注册广播实现拔下耳机停止音乐播放
                else if (intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY))
                {
                    //region 【引用】处理 AUDIO_BECOMING_NOISY Intent
                    /*
                    * 处理 AUDIO_BECOMING_NOISY Intent
                    * 当用户拔下耳机的是否，一些优秀的App会自动停止播放音乐。
                    * （例如QQ音乐在你拔下耳机的时候自动暂停）。
                    * 但是，这种功能并不是制自动的，而是需要你自己去实现。
                    * 如果你不实现这个，可能会导致坏的用户体验。
                    * 比如你的用户带着耳机在教室或者图书馆使用你的app播放多媒体文件，
                    * 不小心拔掉了耳机或者耳机插头松动，那么后果就是导致非常差的用户体验。
                    *
                    */
                    //endregion

                    if (_isPause_B_On)
                    {
                        on_P_B_Start_Click();
                    }
                }
                //endregion

            }//onReceive 结束
        };
        //endregion

        //region 动态添加广播接收过滤器
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_N2S_START);
        filter.addAction(ACTION_N2S_NEXT);
        filter.addAction(ACTION_N2P_WAKEUP);
        filter.addAction(ACTION_N2P_CLOSE);
        filter.addAction(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY);

        registerReceiver(_notificationBarClickReceiver, filter);
        //endregion

    }//onCreate函数结束

    @Override
    protected void onPause()
    {
        super.onPause();
        _initializeNotificationBar();//在没有通知栏的情况下进入应用的暂停状态，调出通知栏
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        unregisterReceiver(_notificationBarClickReceiver);  //解除广播接收器
        _notificationManager.cancelAll();                   //清除所有通知
    }
    //endregion

    //注册主界面的按钮的点击事件
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
        if (keyCode == event.KEYCODE_BACK)//按下返回键时
        {
            moveTaskToBack(true);         //将当前活动移至后台
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 开启并绑定服务
     */
    private void _bindServiceConnection()
    {
        Intent intent = new Intent(MainActivity.this,MyService.class);
        startService(intent);
        bindService(intent,_serviceConnection,this.BIND_AUTO_CREATE);
    }

    //内部服务连接对象，作为服务控制和服务对象的中介
    private ServiceConnection _serviceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            myService = ((MyService.MyBinder)(service)).getService();

            //注册自定义接口来接收需要同步的指令
            myService.setMyOnNeedSyncListener(new myIOnSyncListener()
            {
                @Override
                public void NeedSync()  //回调函数
                {
                    _syncUI();          //接收到指令后
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            myService = null;
        }
    };

    /**
     * 开启子线程进行媒体探查并将结果输入列表，更新UI
     */
    private void _startNewThreadToGetData()
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                //线程等待100毫秒后再执行，防止服务还没开启造成NPE
                try
                {
                    Thread.sleep(200);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }

                List<ListUnits> _TLOLU = new ArrayList<>(); //临时列表变量

                _TLOLU = __getUriData(MEDIA_URI_INTERNAL, _TLOLU, 30000L);
                _TLOLU = __getUriData(MEDIA_URI_EXTERNAL, _TLOLU,0L);

                final List<ListUnits> final_TLOLU = _TLOLU;

                //耗时操作完成之后提交任务更新UI
                //Android也提供的扩展的通讯方式，内部实现是基于Handler消息机制
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        _MLOLU = final_TLOLU;               //将临时列表传入UI部分的列表实例
                        myService.s_setList(final_TLOLU);   //将临时列表传入服务部分的列表实例

                        //配置适配器并构造播放列表视图
                        _listUnitsAdapter = new ListUnitsAdapter(MainActivity.this, R.layout.list_items_layout, _MLOLU);
                        P_LV_1.setAdapter(_listUnitsAdapter);

                        myService.s_initialize(true);           //初始化服务
                        _syncUI();
                    }
                });
            }
        }).start();
    }

    /**
     * 初始化通知栏并调出通知栏里的控制面板
     */
    private void _initializeNotificationBar()
    {
        /*
         【参考】 Android 通知栏Notification的整合 全面学习 （一个DEMO让你完全了解它）
          http://blog.csdn.net/vipzjyno1/article/details/25248021
        */

        /*
         【参考】 Android自定义Notification并没有那么简单
          http://www.tuicool.com/articles/YBzuYv7
        */

        _remoteContentView = new RemoteViews(getPackageName(), R.layout.nc_layout);     //实例化远程视图对象

        //region 初始化远程视图和构造器
        //初始化所以图片资源
        _remoteContentView.setImageViewResource(R.id.N_B_close ,R.drawable.t_close);
        _remoteContentView.setImageViewResource(R.id.N_B_main ,R.drawable.main);
        _remoteContentView.setImageViewResource(R.id.N_B_next ,R.drawable.t_next);
        _remoteContentView.setImageViewResource(R.id.N_IV_BG ,R.drawable.bg_n_1);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        builder.setSmallIcon(R.mipmap.ic_listener);                 //设置显示在状态栏的小图标
        builder.setCustomContentView(_remoteContentView);           //设置小通知视图
        builder.setCustomBigContentView(_remoteContentView);        //设置大通知视图（本应用以此为基础）
        builder.setPriority(Notification.PRIORITY_MAX);             //设置通知的级别为最高级，防止被后来的通知挤压形成小视图
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        {
            builder.setVisibility(Notification.VISIBILITY_PUBLIC);  //设置锁屏显示全部通知需要Android 5 以上系统（显示小视图）
        }

        //如果不进行如下设置会出现“android.app.RemoteServiceException: Bad notification posted from package”异常
        PendingIntent pendingIntent_default= PendingIntent.getActivity(this,1, new Intent(), PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent_default);
        //endregion

        myNotification = builder.build();                                               //通过构造器构造通知

        //通过设置待定意图的方式来实现按钮发送广播
        __setPendingIntent(R.id.N_B_start,ACTION_N2S_START);
        __setPendingIntent(R.id.N_B_next, ACTION_N2S_NEXT);
        __setPendingIntent(R.id.N_B_main, ACTION_N2P_WAKEUP);
        __setPendingIntent(R.id.N_B_close, ACTION_N2P_CLOSE);

        myNotification.flags = Notification.FLAG_NO_CLEAR;                                          //设置通知点击或滑动时不被清除
        _notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        _notificationManager.notify(0, myNotification);                                             //开启通知
    }

    /**
     * 显示splash画面并在指定时间后消失
     * @param sec 持续时间（毫秒）
     */
    private void _showSplash(final int sec)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                //等待3秒钟
                try
                {
                    Thread.sleep(sec);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }

                //让splash画面消失
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        P_V_splash.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    /**
     * 同步UI函数，在需要修改UI视图时调用，用于将UI显示的东西更新为服务中的数据
     */
    private void _syncUI()
    {
        __syncList(_currentMusicIndex , true);                                                 //更新播放列表，取消旧高亮

        _currentMusicIndex = myService.s_getCurrentMusicIndex();                               //同步本地的当前歌曲索引
        P_TV_currentMusicName.setText(_MLOLU.get(_currentMusicIndex).GetName());               //更新主UI的歌曲名
        P_TV_currentMusicPlayerName.setText(_MLOLU.get(_currentMusicIndex).GetPlayerName());   //更新主UI的歌手名

        __syncNotificationBar();                                                               //更新通知栏
        __syncList(_currentMusicIndex,false);                                                  //更新播放列表，添加新高亮
    }

    //region 定义各个按钮的点击事件
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
        if (_isLoop_B_On == false)                                  //如果单曲循环关闭
        {
            P_B_repeat.setImageResource(R.drawable.p_onrepeat);     //切换为单曲循环状态
            _isLoop_B_On = true;
            myService.s_setLoop(_isLoop_B_On);
        }
        else                                                        //如果单曲循环开启
        {
            P_B_repeat.setImageResource(R.drawable.p_offrepeat);    //切换为关闭状态
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
        if (_isRandom_B_On)                                 //如果已经处于随机状态
        {
            _isRandom_B_On = false;
            P_B_list.setImageResource(R.drawable.p_list);   //设置为顺序状态
            myService.s_setRandom(_isRandom_B_On);
        }
        else                                                //如果处于顺序状态
        {
            _isRandom_B_On = true;
            P_B_list.setImageResource(R.drawable.p_random); //设置为随机状态
            myService.s_setRandom(_isRandom_B_On);
        }
    }

    private void on_P_B_Close_Click()
    {
        //先停止播放再执行退出播放
        myService.s_stop();
        myService.s_quit();

        //解绑并停止服务
        unbindService(_serviceConnection);
        Intent intent = new Intent(MainActivity.this,MyService.class);
        stopService(intent);

        try
        {
            _notificationManager.cancelAll();                   //清除所有通知
            MainActivity.this.finish();                         //退出程序
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
    //endregion

    //region 工具函数
    /**
     * 二级内部函数，从指定的位置扫描媒体资源，附加到传入的列表中并将其返回，相当于 I += a
     * @param uri 指定的URI位置
     * @param _TLOLU 需要加工的列表
     * @param timeCullOff 时长小于此值（毫秒）则不将该曲目收入列表
     * @return 返回原来的列表
     */
    private List<ListUnits> __getUriData(Uri uri , List<ListUnits> _TLOLU , long timeCullOff)
    {
        //region 【引用】媒体探查
        /*
        在adb shell中，找到/data/data/com.android.providers.media/databases/下，
        然后找到SD卡的数据库文件(一般是一个.db文件)，
        然后输入命令sqlite3加上这个数据库的名字就可以查询android的多媒体数据库了。
        .table命令可以列出所有多媒体数据库的表，
        .scheme加上表名可以查询表中的所有列名。
        这里可以利用SQL语句来查看你想要的数据，
        记得最后一定要记住每条语句后面都加上分号。

        查询，代码如下所示：

　　    Cursor cursor = resolver.query(_uri, prjs, selections, selectArgs, order);

        ContentResolver的query方法接受几个参数，参数意义如下:

        Uri：这个Uri代表要查询的数据库名称加上表的名称。
        这个Uri一般都直接从MediaStore里取得，
        例如我要取所有歌的信息，就必须利用MediaStore.Audio.Media. EXTERNAL _CONTENT_URI这个Uri。
        专辑信息要利用MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI这个Uri来查询，其他查询也都类似。

        Prjs：这个参数代表要从表中选择的列，用一个String数组来表示。

        Selections：相当于SQL语句中的where子句，就是代表你的查询条件。

        selectArgs：这个参数是说你的Selections里有？这个符号是，这里可以以实际值代替这个问号。
        如果Selections这个没有？的话，那么这个String数组可以为null。

        Order：说明查询结果按什么来排序。

        上面就是各个参数的意义，它返回的查询结果一个Cursor，
        这个Cursor就相当于数据库查询的中Result，用法和它差不多。
        */
        //endregion
        //【引用】MediaStore.Audio >>>>>>>  http://blog.csdn.net/lmrjian/article/details/46620613

        String[] projection = {"_data", "_display_name", "duration", "artist"};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);

        //必须确保游标不为空
        if (cursor != null && cursor.moveToFirst())
        {
            cursor.moveToFirst();
            do
            {
                //如果文件持续时间小于Culloff值则跳过循环不将该曲目收入列表
                if (cursor.getLong(cursor.getColumnIndex("duration")) < timeCullOff)
                {
                    continue;
                }

                ListUnits tempListUnit = new ListUnits();   //创建列表项的临时变量

                //为列表项赋值
                tempListUnit.SetData(cursor.getString(cursor.getColumnIndex("_data")));
                tempListUnit.SetDisplay_name(cursor.getString(cursor.getColumnIndex("_display_name")));
                tempListUnit.SetArtist(cursor.getString(cursor.getColumnIndex("artist")));
                tempListUnit.SetDuration(cursor.getLong(cursor.getColumnIndex("duration")));
                tempListUnit.SetTitleHighLight(false,this); //将绿色的标题高亮设置为否

                _TLOLU.add(tempListUnit);                   //将结果（临时项）追加到列表中
            }
            while (cursor.moveToNext());                    //直到没有下一个的时候结束do-while循环
            cursor.close();                                 //及时关闭游标方便重用
        }
        return _TLOLU;
    }

    /**
     * 二级内部函数，控制歌曲列表对应项的文字高亮同步和焦点跳转
     * @param index 需要修改的项的索引
     * @param isOld 需要修改的项是不是需要去掉高亮
     */
    private void __syncList(int index , boolean isOld)
    {
        ListUnits tempListUnit = (ListUnits) P_LV_1.getItemAtPosition(index);   //通过索引值获取列表项

        if (isOld)                                                              //如果需要去掉高亮
        {
            tempListUnit.SetTitleHighLight(false , this);                       //去掉高亮
        }
        else                                                                    //如果需要添加高亮
        {
            tempListUnit.SetTitleHighLight(true , this);                        //添加高亮
        }

        _listUnitsAdapter.notifyDataSetChanged();                               //通知列表适配器数据源已经修改，重新构建列表
        P_LV_1.smoothScrollToPosition(index);                                   //列表滚动到索引项在屏幕范围内
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
            if ( _isPause_B_On == false)                                                     //按钮应显示为播放时
            {
                _remoteContentView.setImageViewResource(R.id.N_B_start ,R.drawable.t_start); //将通知栏按钮图标切换为播放
            }
            else if (_isPause_B_On == true)                                                  //按钮应显示为暂停时
            {
                _remoteContentView.setImageViewResource(R.id.N_B_start ,R.drawable.t_pause); //将通知栏按钮图标切换为暂停
            }

            //重新发送通知
            //对于更新通知的UI界面时，要记住调用NotificationManager.notify(int id, Notification notification)方法通知一下，否则即使设置了新值，也不会起作用的。
            _notificationManager.notify(0, myNotification);
        }
    }

    /**
     * 二级内部函数，控制通知栏中不同按钮的广播发送
     * @param id 按钮的ID
     * @param actionName 要发送的广播的动作名称
     */
    private void __setPendingIntent(int id, String actionName)
    {
        Intent tempIntent = new Intent(actionName);                                             //新建意图，并设置action动作名称为传入的名称，用于接收广播时过滤意图信息
        PendingIntent tempPendingIntent = PendingIntent.getBroadcast(this, 10000, tempIntent, PendingIntent.FLAG_UPDATE_CURRENT);  //创建临时变量，用来发送广播
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
            Method collapse;

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
    //endregion

}//MainActivity结束

