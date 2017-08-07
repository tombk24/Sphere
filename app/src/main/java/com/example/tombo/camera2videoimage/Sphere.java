package com.example.tombo.camera2videoimage;

import android.content.Context;
import android.graphics.Color;
import android.media.Image;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Created by TomBo on 03/08/2017.
 */

public class Sphere {
    RelativeLayout sphereLayout;

    public Sphere (Context screen){
        sphereLayout = new RelativeLayout(screen);
        RelativeLayout actualSphere = new RelativeLayout(screen);

        RelativeLayout.LayoutParams sphereLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        sphereLayoutParams.width = 150;
        sphereLayoutParams.height = 200;

        RelativeLayout.LayoutParams actualSphereLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        actualSphereLayoutParams.height = 150;

        sphereLayout.setLayoutParams(sphereLayoutParams);
        actualSphere.setLayoutParams(actualSphereLayoutParams);

        ImageView profilePicture = new CircleImageView(screen);
        ImageView active = new CircleImageView(screen);

        RelativeLayout.LayoutParams imgViewParams = new RelativeLayout.LayoutParams(150, 150);
        imgViewParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        active.setLayoutParams(imgViewParams);
        active.setImageResource(R.drawable.greensquare);

        RelativeLayout.LayoutParams imgViewParams2 = new RelativeLayout.LayoutParams(130, 130);
        imgViewParams2.addRule(RelativeLayout.CENTER_IN_PARENT);
        profilePicture.setLayoutParams(imgViewParams2);
        profilePicture.setImageResource(R.drawable.redsquare);

        TextView distance = new TextView(screen);
        RelativeLayout.LayoutParams txtViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, 50);
        txtViewParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        distance.setText("50m");
        distance.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        distance.setBackgroundColor(Color.parseColor("#ffffff"));
        distance.setLayoutParams(txtViewParams);

        actualSphere.addView(active);
        actualSphere.addView(profilePicture);

        sphereLayout.addView(actualSphere);
        sphereLayout.addView(distance);

    }

    public RelativeLayout returnLayout(){
        return this.sphereLayout;
    }

}
