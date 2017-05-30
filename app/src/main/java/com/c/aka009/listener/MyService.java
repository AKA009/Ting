package com.c.aka009.listener;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;

public class MyService extends Service
{
    //region 定义播放控制变量
    private List<ListUnits> _MLOLU; // Main list of ListUnits
    private int _currentMusicIndex = 0;
    private boolean _isLooping = false;
    private boolean _isRandom = false;
    //endregion

    //region 声明其他对象
    public MyBinder binder = new MyBinder();
    private MediaPlayer _mediaPlayer;
    private myIOnSyncListener _myOnSyncListener;
    //endregion

    /**
     * 为自定义的监听器声明注册器方法
     * @param onSyncListener
     */
    public void setMyOnNeedSyncListener(myIOnSyncListener onSyncListener)
    {
        this._myOnSyncListener = onSyncListener;
    }

    //通过绑定结合主活动与服务
    public class MyBinder extends Binder
    {
        MyService getService()
        {
            return MyService.this;
        }
    }

    //在服务类的构造函数中实例化对象
    public MyService()
    {
        _mediaPlayer = new MediaPlayer();
        _MLOLU = new ArrayList<>();
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        //初始化控制变量
        _isLooping  = false;
        _isRandom   = false;
        _mediaPlayer.setVolume(1f,1f);  //将左右声道的声音都设置为1（最大）

        //region 注册播放完成监听器
        _mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
        {
            @Override
            public void onCompletion(MediaPlayer mp)
            {
                _myOnSyncListener.NeedSync();   //触发自定义的需要更新事件，同步当前状态（结束时）

                //手动控制歌曲是否单曲循环
                if (_isLooping)
                {
                    s_stop();
                }
                else
                {
                    s_next();
                }

                s_start();                      //如果是自动播放完成的，那么肯定处于停止状态。要连续播放，就要调用开始方法
                _myOnSyncListener.NeedSync();   //再次同步当前（开始播放时）的状态
            }
        });
        //endregion
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }

    //region 按钮接口方法
    /**
     * 播放服务的停止方法，在“停止键”中调用，用于停止当前播放（停止播放并将歌曲进度归零）
     */
    public void s_stop()
    {
        if (_mediaPlayer != null)
        {
            _mediaPlayer.stop();            //调用自带的停止方法
            try
            {
                _mediaPlayer.reset();       //重置播放器
                _mediaPlayer.setDataSource(_MLOLU.get(_currentMusicIndex).GetPath());   //重新将数据源设置为当前曲目
                _mediaPlayer.prepare();     //预加载数据
                _mediaPlayer.seekTo(0);     //将进度值归零
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    /**
     * 播放服务的暂停方法，在“暂停键”中调用，用于暂停当前播放（停止播放并保留歌曲进度）
     */
    public void s_pause()
    {
        if (_mediaPlayer != null)
        {
            _mediaPlayer.pause();
        }
    }

    /**
     * 播放服务的开始播放方法，在“开始键”中调用，用于开始播放当前歌曲
     */
    public void s_start()
    {
        if (_mediaPlayer != null)
        {
            _mediaPlayer.start();
        }
    }

    /**
     * 播放服务的下一曲方法，在“下一首键”中调用，用于跳转到下一首歌曲（如果当前正在播放则连播，停止或暂停状态则不连播）
     */
    public void s_next()
    {
        //如果开启随机，则随机挑选一首设置为当前曲目
        if (_isRandom)
        {
            this.s_setCurrentMusicRandom();
        }

        //否则就是顺序循环播放，如果达到表尾则跳转到表头
        else if (_currentMusicIndex == _MLOLU.size()-1)
        {
            _setCurrentMusic(0);
        }
        else
        {
            _currentMusicIndex ++;
        }

        //如果当前正在播放则连播，停止或暂停状态则不连播
        if (this.s_getIsPlaying())
        {
            //先停止后播放相当于从头开始
            this.s_stop();
            this.s_start();
        }
        else
        {
            this.s_stop();
        }
    }

    /**
     * 播放服务的上一曲方法，在“上一首键”中调用，用于跳转到上一首歌曲（如果当前正在播放则连播，停止或暂停状态则不连播）
     */
    public void s_pre()
    {
        //如果开启随机，则随机挑选一首设置为当前曲目
        if (_isRandom)
        {
            this.s_setCurrentMusicRandom();
        }

        //否则就是倒序循环播放，如果达到表头则跳转到表尾
        else if (_currentMusicIndex == 0)
        {
            _setCurrentMusic(_MLOLU.size()-1);
        }
        else
        {
            _currentMusicIndex --;
        }

        //如果当前正在播放则连播，停止或暂停状态则不连播
        if (this.s_getIsPlaying())
        {
            //先停止后播放相当于从头开始
            this.s_stop();
            this.s_start();
        }
        else
        {
            this.s_stop();
        }
    }

    /**
     * 播放服务的设置随机方法，在“随机按钮”中调用，用于设置播放模式开启或关闭随机
     * @param bool 是否开启随机
     */
    public void s_setRandom(boolean bool)
    {
        this._isRandom = bool;
    }

    /**
     * 播放服务的设置单曲循环方法，在“循环按钮”中调用，用于设置播放模式开启或关闭单曲循环
     * @param loop 是否开启单曲循环
     */
    public void s_setLoop(boolean loop)
    {
        this._isLooping = loop;
    }

    /**
     * 播放服务的跳转到方法，在“歌曲列表被按下选择时”调用，用于跳转到选定歌曲（如果当前正在播放则连播，停止或暂停状态则不连播）
     * @param index 选定歌曲索引
     */
    public void s_jumpTo(int index)
    {
        this._setCurrentMusic(index);

        if (this.s_getIsPlaying())
        {
            this.s_stop();
            this.s_start();
        }
        else
        {
            this.s_stop();
        }
    }
    //endregion

    //region GET、SET方法
    /**
     * 播放服务的获取当前播放状态方法
     * @return 是否正在播放
     */
    public boolean s_getIsPlaying()
    {
        return this._mediaPlayer.isPlaying();
    }

    /**
     * 播放服务的获取当前曲目方法
     * @return 当前曲目索引值
     */
    public int s_getCurrentMusicIndex()
    {
        return this._currentMusicIndex;
    }

    /**
     * 播放服务的随机取值方法，随机取出一个索引值输入到当前播放索引
     */
    public void s_setCurrentMusicRandom()
    {
        this._currentMusicIndex = this._getRandomIndex(_MLOLU.size());
    }

    /**
     * 播放服务的列表设置方法，用于将播放列表输入到当前播放服务对象
     * @param _TLOLU 要传入的列表
     */
    public void s_setList(List<ListUnits> _TLOLU)
    {
        this._MLOLU = _TLOLU;
    }
    //endregion

    //region 流程控制方法
    /**
     * 播放服务的退出方法，用于退出播放状态并且释放播放器资源（退出后将无法执行播放逻辑）
     */
    public void s_quit()
    {
        if (_mediaPlayer != null)
        {
            _mediaPlayer.stop();    //停止
            _mediaPlayer.reset();   //充值
            _mediaPlayer.release(); //释放资源
        }
    }

    /**
     * 播放服务的退出方法，在“需要初始化播放器时”调用，用于初始化组件并进入准备播放状态（默认播放列表第一首歌）
     */
    public void s_initialize()
    {
        if (_mediaPlayer != null)
        {
            _mediaPlayer.setLooping(false);
            try
            {
                _currentMusicIndex = 0;
                _mediaPlayer.setDataSource(_MLOLU.get(_currentMusicIndex).GetPath());
                _mediaPlayer.prepare();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
    //endregion

    //region 内部方法
    /**
     * 内部函数(安全的)，用于设置当前歌曲索引值为指定值
     * @param index 指定值
     */
    private void _setCurrentMusic(int index)
    {
        if (index>=0&&index<=(_MLOLU.size()-1))
        {
            this._currentMusicIndex = index;
        }
    }

    /**
     * 内部函数，用于获取一个指定范围内的随机值
     * @param range 随机值的上限（下限为0）
     * @return 生成的随机值
     */
    private int _getRandomIndex(int range)
    {
        return (int) (Math.random() * range);
    }
    //endregion
}//MyService 结束
