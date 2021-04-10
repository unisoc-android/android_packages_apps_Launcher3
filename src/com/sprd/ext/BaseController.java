package com.sprd.ext;

import android.content.Context;

import java.io.FileDescriptor;
import java.io.PrintWriter;


/**
 * Created by SPRD on 2019/04/09.
 */
public class BaseController {
    private static final String TAG = "BaseController";

    protected final Context mContext;

    public BaseController(Context context) {
        mContext = context;
    }

    public void dumpState(String prefix, FileDescriptor fd, PrintWriter writer, boolean dumpAll) {

    }
}
