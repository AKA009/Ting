package com.c.aka009.Ting;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * 自定义的列表适配器类，用于使用自定义的视图填充播放列表
 */
public class ListUnitsAdapter extends ArrayAdapter<ListUnits>
{
    /*
      【引用】最常用和最难用的控件——ListView
       http://blog.csdn.net/u013678930/article/details/50824645
     */

    private int _resourceId;

    /**
     * 重写了父类的一组构造函数,
     * 用于将上下文,ListView子项布局的id和数据都传递进来.
     */
    public ListUnitsAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<ListUnits> objects)
    {
        super(context, resource, objects);
        _resourceId = resource;
    }

//    提升ListView的运行效率
//
//    因为在FruitAdapter的getView()方法中每次都将布局重新加载了一次,
//    当ListView快速滚动的时候就会成为性能的阻碍.
//
//    我们新建了一个内部类ViewHolder,用于对控件的实例进行缓存.
//    当convertView为空的时候,创建一个ViewHolder对象,并将控件的实例都存放在ViewHolder里,
//    然后调用View的SetTag()方法,将ViewHolder对象存储在View中.
//    当convertView不为空的时候则调用View的getTag()方法,把ViewHolder重新取出.
//    这样所有控件的实例都缓存在ViewHolder里,
//    就没有必要每次都通过findViewById()方法来获取控件实例了.

    /**
     * 新建了一个内部类ViewHolder,用于对控件的实例进行缓存
     */
    private class ViewHolder
    {
        TextView L_TV_Name;
        TextView L_TV_PlayerName;
        TextView L_TV_Duration;
        TextView L_TV_Type;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        ListUnits listUnit = getItem(position);                                     //通过getItem()方法得到当前项的Fruit的实例

        View view;
        ViewHolder viewHolder;

        if (convertView == null)                                                    //当convertView为空的时候,创建一个ViewHolder对象,并将控件的实例都存放在ViewHolder里
        {
            view = LayoutInflater.from(getContext()).inflate(_resourceId,null);     //使用LayoutInflater来为这个子项加载我们传入的布局

            viewHolder = new ViewHolder();

                                                                                    //接着调用View的fndViewById()方法分别获取到ImageView和TextView的实例
            viewHolder.L_TV_Name        = (TextView) view.findViewById(R.id.L_TV_Name);
            viewHolder.L_TV_PlayerName  = (TextView) view.findViewById(R.id.L_TV_PlayerName);
            viewHolder.L_TV_Duration    = (TextView) view.findViewById(R.id.L_TV_Duration);
            viewHolder.L_TV_Type        = (TextView) view.findViewById(R.id.L_TV_Type);

            view.setTag(viewHolder);                                                //调用View的SetTag()方法,将ViewHolder对象存储在View中
        }
        else                                                                        //当convertView不为空的时候则调用View的getTag()方法,把ViewHolder重新取出
        {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }
                                                                                    //分别调用它们的setImageResource和setText方法来设置显示的图片和文字
        if (listUnit != null)
        {
            viewHolder.L_TV_Name.setText(listUnit.GetCSName());                     //将要输入的文字设置为带背景色的CharSequence对象，方便更改数据源以更改背景色
            viewHolder.L_TV_PlayerName.setText(listUnit.GetCSPlayerName());
            viewHolder.L_TV_Duration.setText(listUnit.GetDuration());
            viewHolder.L_TV_Type.setText(listUnit.GetSpannableType(this.getContext()));
        }
                                                                                    //这样所有控件的实例都缓存在ViewHolder里,
                                                                                    //就没有必要每次都通过findViewById()方法来获取控件实例了.
        return view;                                                                //最好将布局返回
    }
}
