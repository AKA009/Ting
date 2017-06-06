package com.c.aka009.Ting;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 作为列表项的数据类，用来存储歌曲相关信息
 */
public class ListUnits
{
    //region 声明数据
    private String _name;                   //歌曲名称
    private String _path;                   //歌曲路径
    private String _duration;               //歌曲持续时间
    private String _playerName;             //歌手名称
    private String _type;                   //文件类型

    private CharSequence _cs_Name;          //带背景的名称
    private CharSequence _cs_PlayerName;    //带背景的歌手名
    //endregion

    //region Set方法
    /**
     * 输入扫描得到的“data”字段
     * @param data 输入字段
     */
    public void SetData(String data)
    {
        this._path = data;
    }

    /**
     * 输入扫描得到的“display_name”字段
     * @param display_name 输入字段
     */
    public void SetDisplay_name(String display_name)
    {
        String temp[] = display_name.split("\\.");  //将输入的“display_name”字段从“.”字符切分

        //使用StringBuilder还原文件名
        StringBuilder tempStringBuilder = new StringBuilder("");
        for (int i=0 ; i<temp.length-1;i++)
        {
            //如果索引超过0则在开始前追加一个【.】字符
            if (i>0)
            {
                tempStringBuilder.append(".");
            }
            tempStringBuilder.append(temp[i]);
        }
        _name = tempStringBuilder.toString();       //将构造成的字符串存入“name”
        _type = "."+temp[temp.length -1];           //切分后的最后一个字符串（扩展名）存入“type”
    }

    /**
     * 输入扫描得到的“duration”字段的值
     * @param d 输入的长度值（毫秒）
     */
    public void SetDuration(long d)
    {
        //声明分钟和秒钟两个空字符串
        String min_s = "";
        String sec_s = "";

        //将毫秒转换为分和秒的值并存入两个long变量（少于1秒的值被舍去）
        long min_l = d/(60*1000);
        long sec_l = d%(60*1000);

        //region 格式化时间值
        //将分钟的数值转换为MM:SS的格式，并存入字符串变量
        if (min_l< 10L)
        {
            min_s = "0"+min_l+"";
        }
        else if (min_l>= 10L)
        {
            min_s = min_l+"";
        }
        else if (min_l == 0L)
        {
            min_s = "00";
        }

        //将秒钟的数值转换为MM:SS的格式，并存入字符串变量
        sec_l = sec_l/1000;
        if (sec_l< 10L)
        {
            sec_s = "0"+sec_l+"";
        }
        else if (sec_l>= 10L)
        {
            sec_s = sec_l+"";
        }
        else if (sec_l == 0L)
        {
            sec_s = "00";
        }
        //endregion

        //输出到长度字符串
        _duration = min_s+"："+sec_s;
    }

    /**
     * 输入扫描得到的“artist”字段
     * @param artist 输入字段
     */
    public void SetArtist(String artist)
    {
        _playerName = artist;
    }

    /**
     * 设置列表项中歌曲名和歌手名的高亮与取消
     * @param setOn 是否要设置为高亮
     * @param context 当前上下文
     */
    public void SetTitleHighLight(boolean setOn , Context context)
    {
        //如果需要高亮就把cs数据设置为带背景色的字符串
        if (setOn)
        {
            _cs_Name = GetSpannableName(context);
            _cs_PlayerName = GetSpannablePlayerName(context);
        }
        //如果不需要高亮就把cs数据设置为普通的字符串
        else
        {
            _cs_Name = GetName();
            _cs_PlayerName = GetPlayerName();
        }
    }
    //endregion


    //region Get方法
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

    public CharSequence GetCSName()
    {
        return _cs_Name;
    }
    public CharSequence GetCSPlayerName()
    {
        return _cs_PlayerName;
    }

    /**
     * 获取带有浅绿色高亮背景的歌曲名
     * @param context 上下文
     * @return 返回高亮歌曲名
     */
    public SpannableString GetSpannableName(Context context)
    {
        return this._span(this.GetName(),R.color.my_green_L,context);
    }

    /**
     * 获取带有浅绿色高亮背景的歌手名
     * @param context 上下文
     * @return 返回高亮歌手名
     */
    public SpannableString GetSpannablePlayerName(Context context)
    {
        return this._span(this.GetPlayerName(),R.color.my_green_L,context);
    }

    /**
     * 获取带有各种不同颜色高亮背景的歌曲扩展名
     * @param context 上下文
     * @return 返回高亮扩展名
     */
    public SpannableString GetSpannableType(Context context)
    {
        int tempColorID = 0;
        String tempString = this.GetType();

        //判断字符串是否相等，为不同的字符串设置不同颜色
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
            case ".m4a":
                tempColorID = R.color.my_yellow_L;
                break;
            case ".flac":
                tempColorID = R.color.my_purple_L;
                break;
            case ".wav":
                tempColorID = R.color.my_orange_L;
                break;
            case ".3gp":
                tempColorID = R.color.my_orange_L;
                break;
            case ".wma":
                tempColorID = R.color.my_green_L;
                break;
            case ".mid":
                tempColorID = R.color.my_green_L;
                break;
            default:
                tempColorID = R.color.my_white;
                break;
        }
        return this._span(tempString,tempColorID,context);
    }
    //endregion

    //region 工具方法
    /**
     * 将指定的字符串设置为带有背景色的字符串
     * @param string_in 输入字符串
     * @param colorID 要指定的背景色的ID
     * @param context 上下文
     * @return 返回加工过的字符串
     */
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

    /**
     * 静态方法，将列表映射成一个装有散列图的列表
     * @param list_in 存有原始数据的列表
     * @return 返回内含散列图的列表结构
     */
    public static List<HashMap<String, String>> ST_ListToListOfMaps (List<ListUnits> list_in)
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
    //endregion
}
