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
    private MediaPlayer _mediaPlayer;
    private List<ListUnits> _MLOLU; // Main list of ListUnits

    private int _currentMusicIndex = 0;

    private boolean _isLooping = false;
    private boolean _isRandom = false;

    public MyBinder binder = new MyBinder();

    private myIOnSyncListener _myOnSyncListener;
    public void setMyOnNeedSyncListener(myIOnSyncListener onSyncListener)
    {
        this._myOnSyncListener = onSyncListener;
    }


    public class MyBinder extends Binder
    {
        MyService getService()
        {
            return MyService.this;
        }
    }


    public MyService()
    {
        _mediaPlayer = new MediaPlayer();
        _MLOLU = new ArrayList<>();
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        _isLooping  = false;
        _isRandom   = false;
        //_MLOLU = new ArrayList<>();
        _mediaPlayer.setVolume(1f,1f);

        _mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
        {
            @Override
            public void onCompletion(MediaPlayer mp)
            {
                _myOnSyncListener.NeedSync();
                if (_isLooping)
                {
                    s_stop();
                }
                else
                {
                    s_next();
                }
                s_start();
                _myOnSyncListener.NeedSync();
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }

    public void s_stop()
    {
        if (_mediaPlayer != null)
        {
            _mediaPlayer.stop();
            try
            {
                _mediaPlayer.reset();
                _mediaPlayer.setDataSource(_MLOLU.get(_currentMusicIndex).GetPath());
                _mediaPlayer.prepare();
                _mediaPlayer.seekTo(0);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    public void s_pause()
    {
        if (_mediaPlayer != null)
        {
            _mediaPlayer.pause();
        }
    }

    public void s_start()
    {
        if (_mediaPlayer != null)
        {
            _mediaPlayer.start();
        }
    }

    public void s_next()
    {
        if (_isRandom)
        {
            this.s_setCurrentMusicRandom();
        }
        else if (_currentMusicIndex == _MLOLU.size()-1)
        {
            _currentMusicIndex  = 0;
        }
        else
        {
            _currentMusicIndex ++;
        }

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

    public void s_pre()
    {
        if (_isRandom)
        {
            this.s_setCurrentMusicRandom();
        }
        else if (_currentMusicIndex == 0)
        {
            _currentMusicIndex  = _MLOLU.size()-1;
        }
        else
        {
            _currentMusicIndex --;
        }
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
    public void s_jumpTo(int index)
    {
        this.s_setCurrentMusic(index);

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

    public void s_setCurrentMusic(int index)
    {
        this._currentMusicIndex = index;
    }

    public void s_setCurrentMusicRandom()
    {
        this._currentMusicIndex = this._getRandomIndex(_MLOLU.size());
    }

    public void s_setRandom(boolean bool)
    {
        this._isRandom = bool;
    }

    public void s_setLoop(boolean loop)
    {
        this._isLooping = loop;
    }

    public boolean s_getIsPlaying()
    {
        return this._mediaPlayer.isPlaying();
    }

    public int s_getCurrentMusicIndex()
    {
        return this._currentMusicIndex;
    }

    public void s_quit()
    {
        if (_mediaPlayer != null)
        {
            _mediaPlayer.stop();
            _mediaPlayer.reset();
            _mediaPlayer.release();
        }
    }

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

    public void s_setList(List<ListUnits> _TLOLU)
    {
        this._MLOLU = _TLOLU;
    }

    private int _getRandomIndex(int range)
    {
        return (int) (Math.random() * range);
    }
}
