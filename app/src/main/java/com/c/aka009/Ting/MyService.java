package com.c.aka009.Ting;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;

/**
 * 音乐播放服务类，用于实现后台播放功能，处理播放逻辑
 */
public class MyService extends Service
{
    //region 定义播放控制变量
    private List<ListUnits> _MLOLU; // Main list of ListUnits
    private int _currentMusicIndex = 0;
    private boolean _isLooping = false;
    private boolean _isRandom = false;
    private boolean _isPrepared = false;
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
                Log.d("onCompletion>>>>>>>>>","<<<<<<<<<<<<<<<<<<<< "+_currentMusicIndex+" }}}}}}}} "+_isPrepared);
                //如果进入播放完成阶段时还没准备好，说明播放器还处在准备中，等待之后直接将本回调函数返回，迎接OnPrepared
                if (_isPrepared == false)
                {
                    try
                    {
                        Thread.sleep(200);
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    return;
                }
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

        //region 注册错误检测监听器
        _mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener()
        {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra)
            {
                //region 输出错误信息
                Log.d(TAG, "OnError - Error code: " + what + " Extra code: " + extra);
                switch (what)
                {
                    case -1004:
                        Log.d(TAG, "MEDIA_ERROR_IO");
                        break;
                    case -1007:
                        Log.d(TAG, "MEDIA_ERROR_MALFORMED");
                        break;
                    case 200:
                        Log.d(TAG, "MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK");
                        break;
                    case 100:
                        Log.d(TAG, "MEDIA_ERROR_SERVER_DIED");
                        break;
                    case -110:
                        Log.d(TAG, "MEDIA_ERROR_TIMED_OUT");
                        break;
                    case 1:
                        Log.d(TAG, "MEDIA_ERROR_UNKNOWN");
                        break;
                    case -1010:
                        Log.d(TAG, "MEDIA_ERROR_UNSUPPORTED");
                        break;
                }
                switch (extra)
                {
                    case 800:
                        Log.d(TAG, "MEDIA_INFO_BAD_INTERLEAVING");
                        break;
                    case 702:
                        Log.d(TAG, "MEDIA_INFO_BUFFERING_END");
                        break;
                    case 701:
                        Log.d(TAG, "MEDIA_INFO_METADATA_UPDATE");
                        break;
                    case 802:
                        Log.d(TAG, "MEDIA_INFO_METADATA_UPDATE");
                        break;
                    case 801:
                        Log.d(TAG, "MEDIA_INFO_NOT_SEEKABLE");
                        break;
                    case 1:
                        Log.d(TAG, "MEDIA_INFO_UNKNOWN");
                        break;
                    case 3:
                        Log.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START");
                        break;
                    case 700:
                        Log.d(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING");
                        break;
                }
                //endregion

                Log.d("onErrorStart>>>>>>>>>","<<<<<<<<<<<<<<<<<<<< "+_currentMusicIndex);
                _mediaPlayer.reset();   //重置为初始状态解除错误

                //如果开启随机，则随机挑选一首设置为当前曲目
                if (_isRandom)
                {
                    s_setCurrentMusicRandom();
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

                s_initialize(false);
                s_start();
                Log.d("onErrorEnd>>>>>>>>>","<<<<<<<<<<<<<<<<<<<< "+_currentMusicIndex);
                _myOnSyncListener.NeedSync();
                return false;
            }
        });
        //endregion

        //region 注册准备完成监听器
        _mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener()
        {
            @Override
            public void onPrepared(MediaPlayer mp)
            {
                Log.d("onPrepared>>>>>>>>>","<<<<<<<<<<<<<<<<<<<< "+_currentMusicIndex);

                //准备完成后更新准备标记并且开始播放
                _isPrepared = true;
                _mediaPlayer.start();
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
            _mediaPlayer.stop();                        //调用自带的停止方法
            _mediaPlayer.reset();                       //重置播放器
            _setMusicDataSource(_currentMusicIndex);    //重新将数据源设置为当前曲目
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
        Log.d("SStart>>>>>>>>>","<<<<<<<<<<<<<<<<<<<< "+_currentMusicIndex+" }}}}}}}}}}} "+_isPrepared);
        if (_mediaPlayer != null)
        {
            if (_isPrepared)
            {
                _mediaPlayer.start();           //直接开始
            }
            else
            {
                _mediaPlayer.prepareAsync();    //异步预加载，预加载完成后通过回调函数开始播放
            }
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
     * 播放服务的初始化方法，在“需要初始化播放器时”调用，用于初始化组件并进入准备播放状态（首次初始化播放列表第一首歌）
     * @param isFirst 是否为首次初始化
     */
    public void s_initialize(boolean isFirst)
    {
        if (_mediaPlayer != null)
        {
            if (isFirst)
            {
                _mediaPlayer.setLooping(false);
                _currentMusicIndex = 0;
            }
            _setMusicDataSource(_currentMusicIndex);
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
        //如果索引值不超出范围
        if (index>=0&&index<=(_MLOLU.size()-1))
        {
            this._currentMusicIndex = index;
        }
    }

    /**
     * 内部函数(安全的)，用于为播放器设置数据源（数据源设置完成后进入未准备状态）
     * @param index 目标曲目索引
     */
    private void _setMusicDataSource(int index)
    {
        //如果索引值不超出范围
        if (index>=0&&index<=(_MLOLU.size()-1))
        {
            try
            {
                _mediaPlayer.setDataSource(_MLOLU.get(index).GetPath());
                _isPrepared = false;
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
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
