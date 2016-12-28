//////////////////////////////////////////////////////////////////////////////////
//  Copyright (c) 2016-2036  TRANSSION HOLDINGS
//
//  PROPRIETARY RIGHTS of TRANSSION HOLDINGS are involved in the
//  subject matter of this material.  All manufacturing, reproduction, use,
//  and sales rights pertaining to this subject matter are governed by the
//  license agreement.  The recipient of this software implicitly accepts
//  the terms of the license.
//
//  Description: For support ripple image view
//  Author:      xieweiwei(IB-02533)
//  Version:     V1.0
//  Date:        2016.12.13
//  Modification:
//////////////////////////////////////////////////////////////////////////////////

package com.transsion.gallery3d.ui;

import com.android.gallery3d.R;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.transsion.gallery3d.ui.RippleView;

public class FlexImageView extends RippleView {
    private ImageView imageView;
    private TextView textView;
    private CharSequence text;
    private Drawable drawable;
    private float textSize;
    private int textColor;
    private int showType;
    private int SHOW_TYPE_ICON = 0;
    private int SHOW_TYPE_TEXT = 1;

    public FlexImageView(Context context) {
        super(context);
    }

    public FlexImageView(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FlexImageView);
        text = a.getText(R.styleable.FlexImageView_text);
        if (text == null) {
            text = "";
        }
        Drawable d = a.getDrawable(R.styleable.FlexImageView_src);
        if (d != null) {
            drawable = d;
        } else {
            throw new RuntimeException("image src is empty.");
        }
        textSize = a.getDimension(R.styleable.FlexImageView_textSize, getResources().getDimension(R.dimen.floating_actionbar_standant_textsize));
        textColor = a.getColor(R.styleable.FlexImageView_textColor, getResources().getColor(R.color.actionbar_standant_textcolor));
        showType = a.getInt(R.styleable.FlexImageView_showType, SHOW_TYPE_ICON);

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.flex_image_layout, this);
        imageView = (ImageView) findViewById(R.id.icon);
        if (imageView != null) {
            if (showType == SHOW_TYPE_ICON) {
                imageView.setImageDrawable(drawable);
                imageView.setVisibility(View.VISIBLE);
            } else {
                imageView.setVisibility(View.GONE);
            }
        }

        textView = (TextView) findViewById(R.id.text);
        if (textView != null) {
            if (showType == SHOW_TYPE_TEXT) {
                if (text.equals("") || text == null) {
                    textView.setVisibility(View.GONE);
                } else {
                    textView.setTextSize((float) textSize);
                    textView.setTextColor(textColor);
                    textView.setText(text);
                    textView.setVisibility(View.VISIBLE);
                }
            } else {
                textView.setVisibility(View.GONE);
            }
        }
        a.recycle();
    }

    public void setImageResource(int resId) {
        imageView.setImageResource(resId);
    }

    public void setTextViewText(String text) {
        textView.setText(text);
    }
}