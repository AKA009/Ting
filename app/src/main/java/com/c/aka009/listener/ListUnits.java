package com.c.aka009.listener;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by A.K.A 009 on 2017/5/20.
 */

public class ListUnits
{
    private String _name;
    private String _path;
    private String _duration;
    private String _playerName;
    private String _type;

    private CharSequence _cs_Name;
    private CharSequence _cs_PlayerName;

    public void SetData(String data)
    {
        this._path = data;
    }
    public void SetDisplay_name(String display_name)
    {
        String temp[] = display_name.split("\\.");
        _name = temp[0];
        _type = "."+temp[1];
    }
    public void SetDuration(String duration)
    {
        String min_s = new String();
        String sec_s = new String();

        long d = Long.parseLong(duration);
        long min_l = d/(60*1000);
        long sec_l = d%(60*1000);

        if (min_l<10l)
        {
            min_s = "0"+min_l+"";
        }
        else if (min_l>10l)
        {
            min_s = min_l+"";
        }
        else if (min_l == 0l)
        {
            min_s = "00";
        }

        sec_l = sec_l/1000;
        if (sec_l<10l)
        {
            sec_s = "0"+sec_l+"";
        }
        else if (sec_l>10l)
        {
            sec_s = sec_l+"";
        }
        else if (sec_l == 0l)
        {
            sec_s = "00";
        }

        _duration = min_s+"："+sec_s;
    }
    public void SetArtist(String artist)
    {
        _playerName = artist;
    }

    public void SetTitleHighLight(boolean setOn , Context context)
    {
        if (setOn)
        {
            _cs_Name = GetSpannableName(context);
            _cs_PlayerName = GetSpannablePlayerName(context);
        }
        else
        {
            _cs_Name = GetName();
            _cs_PlayerName = GetPlayerName();
        }
    }

    public String GetName()
    {
        return _name;
    }
    public String GetPath()
    {
        return _path;
    }
    public String GetDuration()
    {
        return _duration;
    }
    public String GetPlayerName()
    {
        return _playerName;
    }
    public String GetType()
    {
        return _type;
    }

    public static List<HashMap<String, String>> ListToListOfMaps (List<ListUnits> list_in)
    {
        List<HashMap<String, String>> list_out = new ArrayList<>();

        for (ListUnits unit : list_in)
        {
            HashMap<String, String> map = new HashMap<>();

            map.put("Name",unit.GetName() );
            map.put("Path", unit.GetPath());
            map.put("Duration", unit.GetDuration());
            map.put("PlayerName", unit.GetPlayerName());
            map.put("Type", unit.GetType());

            list_out.add(map);
        }
        return list_out;
    }

    public SpannableString GetSpannableName(Context context)
    {
        return this._span(this.GetName(),R.color.my_green_L,context);
    }

    public SpannableString GetSpannablePlayerName(Context context)
    {
        return this._span(this.GetPlayerName(),R.color.my_green_L,context);
    }

    public SpannableString GetSpannableType(Context context)
    {
        int tempColorID = 0;
        String tempString = this.GetType();

        switch (tempString)
        {
            case ".mp3":
                tempColorID = R.color.my_blue_L;
                break;
            case ".ogg":
                tempColorID = R.color.my_red_L;
                break;
            case ".aac":
                tempColorID = R.color.my_yellow_L;
                break;
            case ".flac":
                tempColorID = R.color.my_purple_L;
                break;
            case ".wav":
                tempColorID = R.color.my_orange_L;
                break;
            default:
                tempColorID = R.color.my_white;
                break;
        }
        return this._span(tempString,tempColorID,context);

//        if (tempString .equals(".mp3"))
//        {
//            tempColorID = R.color.my_blue_L;
//        }
//        else if (tempString .equals(".ogg"))
//        {
//            tempColorID = R.color.my_red_L;
//        }
//        else if (tempString .equals(".aac"))
//        {
//            tempColorID = R.color.my_yellow_L;
//        }
//        else if (tempString .equals(".flac"))
//        {
//            tempColorID = R.color.my_purple_L;
//        }
//        else if (tempString .equals(".wav"))
//        {
//            tempColorID = R.color.my_orange_L;
//        }
//        else
//        {
//            tempColorID = R.color.my_white;
//        }
//        return this._span(tempString,tempColorID,context);
    }

    public CharSequence GetCSName()
    {
        return _cs_Name;
    }
    public CharSequence GetCSPlayerName()
    {
        return _cs_PlayerName;
    }
    
    private SpannableString _span (String string_in ,int colorID , Context context)
    {
        //将字符串转换为SpannableString对象
        SpannableString spannableString = new SpannableString(string_in);

        //确定要设置的字符串的start和end
        int start = 0;
        int end = string_in.length();

        //创建BackgroundColorSpan对象，指定背景色
        BackgroundColorSpan backgroundColorSpan = new BackgroundColorSpan(context.getResources().getColor(colorID));

        //使用setSpan方法将指定字符串转换成BackgroundColorSpan对象
        spannableString.setSpan(backgroundColorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableString;
    }
}
