package com.example.chattest.DialogInfo;

import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.chattest.R;
import com.example.chattest.tools.CustomToolbar;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class UserInfoFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.user_info_fragment, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String userName = getArguments().getString("userName");
        String userUri = getArguments().getString("userUri");
        long lastOnline = getArguments().getLong("lastOnline");
        Context context = view.getContext();

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        ImageView img = view.findViewById(R.id.user_info_image);
        img.setLayoutParams(new LinearLayout.LayoutParams((int) (width * 0.8), (int) (width * 0.8)));

        TextView title = view.findViewById(R.id.user_info_title);
        TextView online = view.findViewById(R.id.user_info_online);
        if (userUri.equals(""))
            Picasso.get().load(R.drawable.user_placeholder).fit().into(img);
        else
            Picasso.get().load(userUri).fit().into(img);

        title.setText(userName);

        CustomToolbar toolbar = new CustomToolbar(context);
        String type = getArguments().getString("type", "0");
        if (!type.equals("1")) {

            toolbar.clearEncrypt();
            toolbar.getEncryptViewInfo().setOnClickListener(null);
        }

        Calendar current = new GregorianCalendar();
        Calendar usertime = new GregorianCalendar();
        usertime.setTimeInMillis(lastOnline);
        if (current.get(Calendar.DAY_OF_YEAR) != usertime.get(Calendar.DAY_OF_YEAR) | current.get(Calendar.YEAR) != usertime.get(Calendar.YEAR))
            online.setText("Last seen " + DateFormat.format("d MMM", lastOnline) + " at " + DateFormat.format("HH:mm", lastOnline));
        else
            online.setText(new Date().getTime() - lastOnline < 60000 ? "Online" : "Last seen at " + DateFormat.format("HH:mm", lastOnline));
    }
}
