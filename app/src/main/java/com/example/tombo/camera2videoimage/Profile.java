package com.example.tombo.camera2videoimage;

import android.graphics.Bitmap;

/**
 * Created by TomBo on 03/08/2017.
 */

public class Profile {
    private String mName;
    private String mEmail;
    private String mDateOfBirth;
    private int mProfilePicture;
    private boolean mActive;

    public Profile(String name, String email, String dateOfBirth){
        mName = name;
        mEmail = email;
        mDateOfBirth = dateOfBirth;
    }


}
