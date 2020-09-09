package com.example.chattest.tools;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.chattest.R;

public class CustomToolbar {

    private TextView toolbarTitle, toolbarSubtitle;
    private ImageView toolbarLogo, toolbarEncrypt;
    private LinearLayout clickableSection;

    public CustomToolbar(Context context) {
        toolbarTitle = ((Activity) context).findViewById(R.id.text_toolbar_title);
        toolbarSubtitle = ((Activity) context).findViewById(R.id.text_toolbar_subtitle);
        toolbarLogo = ((Activity) context).findViewById(R.id.icon_toolbar_left);
        toolbarEncrypt = ((Activity) context).findViewById(R.id.icon_toolbar_right);
        clickableSection = ((Activity) context).findViewById(R.id.toolbar_clickable_section);
    }

    public CustomToolbar(TextView toolbarTitle, TextView toolbarSubtitle, ImageView toolbarLogo, ImageView toolbarEncrypt) {
        this.toolbarTitle = toolbarTitle;
        this.toolbarSubtitle = toolbarSubtitle;
        this.toolbarLogo = toolbarLogo;
        this.toolbarEncrypt = toolbarEncrypt;
    }

    public void setTitle(String title) {
        if (toolbarTitle.getVisibility() == View.VISIBLE)
            toolbarTitle.setText(title == null ? "" : title);
        else {
            toolbarTitle.setVisibility(View.VISIBLE);
            toolbarTitle.setText(title == null ? "" : title);
        }
    }

    public void setSubtitle(String subtitle) {
        if (subtitle != null) {
            if (!subtitle.equals("")) {
                if (toolbarSubtitle.getVisibility() != View.VISIBLE) {
                    toolbarSubtitle.setVisibility(View.VISIBLE);
                }
                toolbarSubtitle.setText(subtitle);
            } else {
                if (toolbarSubtitle.getVisibility() != View.GONE) {
                    toolbarSubtitle.setVisibility(View.GONE);
                }
                toolbarSubtitle.setText("");
            }
        } else {
            if (toolbarSubtitle.getVisibility() != View.GONE) {
                toolbarSubtitle.setVisibility(View.GONE);
            }
            toolbarSubtitle.setText("");
        }
    }

    public ImageView getLogoView() {
        if (toolbarLogo.getVisibility() == View.GONE) {
            toolbarLogo.setVisibility(View.VISIBLE);
        }
        return toolbarLogo;
    }

    public ImageView getEncryptView() {
        if (toolbarEncrypt.getVisibility() == View.GONE) {
            toolbarEncrypt.setVisibility(View.VISIBLE);
        }
        return toolbarEncrypt;
    }

    public ImageView getEncryptViewInfo() {
        return toolbarEncrypt;
    }

    public void clearLogo() {
        if (toolbarLogo.getVisibility() == View.VISIBLE) {
            toolbarLogo.setVisibility(View.GONE);
            toolbarLogo.setImageResource(android.R.color.transparent);
        }
    }

    public void clearEncrypt() {
        if (toolbarEncrypt.getVisibility() == View.VISIBLE) {
            toolbarEncrypt.setVisibility(View.GONE);
            toolbarEncrypt.setImageResource(android.R.color.transparent);
        }
    }

    public LinearLayout getClickableSection() {
        return clickableSection;
    }
}
