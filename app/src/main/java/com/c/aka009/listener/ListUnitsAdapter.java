package com.c.aka009.listener;

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
 * Created by A.K.A 009 on 2017/5/21.
 */

public class ListUnitsAdapter extends ArrayAdapter<ListUnits>
{
    private int _resourceId;
    /**
     * Constructor
     *
     * @param context  The current context.
     * @param resource The resource ID for a layout file containing a TextView to use when
     *                 instantiating views.
     * @param objects  The objects to represent in the ListView.
     */
    public ListUnitsAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<ListUnits> objects)
    {
        super(context, resource, objects);
        _resourceId = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent)
    {
        ListUnits listUnit = getItem(position);
        View view;
        ViewHolder viewHolder;

        if (convertView == null)
        {
            view = LayoutInflater.from(getContext()).inflate(_resourceId,null);
            viewHolder = new ViewHolder();

            viewHolder.L_TV_Name        = (TextView) view.findViewById(R.id.L_TV_Name);
            viewHolder.L_TV_PlayerName  = (TextView) view.findViewById(R.id.L_TV_PlayerName);
            viewHolder.L_TV_Duration    = (TextView) view.findViewById(R.id.L_TV_Duration);
            viewHolder.L_TV_Type        = (TextView) view.findViewById(R.id.L_TV_Type);

            view.setTag(viewHolder);
        }
        else
        {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }

        viewHolder.L_TV_Name.setText(listUnit.GetCSName());
        viewHolder.L_TV_PlayerName.setText(listUnit.GetCSPlayerName());
        viewHolder.L_TV_Duration.setText(listUnit.GetDuration());
        viewHolder.L_TV_Type.setText(listUnit.GetSpannableType(this.getContext()));

        return view;
    }

    class ViewHolder
    {
        TextView L_TV_Name;
        TextView L_TV_PlayerName;
        TextView L_TV_Duration;
        TextView L_TV_Type;
    }
}
